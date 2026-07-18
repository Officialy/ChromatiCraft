/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import reika.chromaticraft.api.interfaces.CustomExcavationStarBehavior;
import reika.chromaticraft.base.ItemBlockChangingWand;
import reika.chromaticraft.block.decoration.blockethereallight.Flags;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.technical.TileEntityStructControl;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker;
import reika.dragonapi.auxiliary.progressiverecursivebreaker.ProgressiveBreaker;
import reika.dragonapi.instantiable.PlayerReference;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.libraries.ReikaEnchantmentHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaTreeHelper;
import reika.dragonapi.modinteract.itemhandlers.TwilightForestHandler;
import reika.dragonapi.modregistry.ModWoodList;

public class ItemExcavationWand extends ItemBlockChangingWand {

	private static final int MAX_DEPTH = 12;
	private static final int MAX_DEPTH_BOOST = 18;

	private static final HashMap<Integer, PlayerReference> breakers = new HashMap();

	public ItemExcavationWand(int index) {
		super(index);
		this.addEnergyCost(CrystalElement.YELLOW, 2);
	}

	@Override
	public float getDigSpeed(ItemStack is, Block b, int meta) {
		return 5;
	}

	@Override
	public boolean canHarvestBlock(Block b, ItemStack is) {
		return true;
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean canSpreadOn(World world, int x, int y, int z, Block b, int meta) {
		if (world.provider.isSurfaceWorld() && b == Blocks.stone)
			return false;
		if (world.provider.isHellWorld && b == Blocks.netherrack)
			return false;
		if (world.provider.dimensionId == 1 && (b == Blocks.end_stone || b == Blocks.obsidian))
			return false;
		if (b == ChromaBlocks.BEDROCKCRACK.getBlockInstance())
			return false;
		TileEntity te = world.getTileEntity(x, y, z);
		return !(te instanceof CrystalNetworkTile) && !(te instanceof TileEntityStructControl);
	}

	@Override
	public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer ep) {
		World world = ep.worldObj;
		if (!this.canSpreadOn(world, x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)))
			return false;
		if (!world.isRemote) {
			ProgressiveBreaker b = ProgressiveRecursiveBreaker.instance.addCoordinateWithReturn(world, x, y, z, this.getDepth(ep, world, x, y, z));
			//b.looseMatches.put(Blocks.redstone_ore, new BlockKey(Blocks.lit_redstone_ore));
			b.call = this;
			b.silkTouch = ReikaEnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch, itemstack) > 0;
			b.drops = !ep.capabilities.isCreativeMode;
			b.fortune = ReikaEnchantmentHelper.getEnchantmentLevel(Enchantment.fortune, itemstack);
			if (ep.isSneaking())
				b.extraSpread = true;
			if (ReikaEnchantmentHelper.getEnchantmentLevel(ChromaEnchants.AUTOCOLLECT.getEnchantment(), itemstack) > 0) {
				b.dropInventory = ep.inventory;
			}
			b.taxiCabDistance = true;
			b.player = ep;
			b.hungerFactor = 0.125F;
			b.setBlocks(false, this.getSpreadBlocks(world, x, y, z));
			breakers.put(b.hashCode(), new PlayerReference(ep));
		}
		return true;
	}

	private HashSet<BlockKey> getSpreadBlocks(World world, int x, int y, int z) {
		HashSet<BlockKey> set = new HashSet();
		Block bk = world.getBlock(x, y, z);
		set.add(new BlockKey(bk, world.getBlockMetadata(x, y, z)));
		if (bk == Blocks.lit_redstone_ore)
			set.add(new BlockKey(Blocks.redstone_ore));
		else if (bk == Blocks.redstone_ore)
			set.add(new BlockKey(Blocks.lit_redstone_ore));
		else if (bk instanceof BlockStairs) {
			for (int i = 0; i < 16; i++) {
				set.add(new BlockKey(bk, i));
			}
		}
		else if (bk == TwilightForestHandler.BlockEntry.NAGASTONE.getBlock()) {
			for (int i = 0; i < 16; i++) {
				set.add(new BlockKey(bk, i));
			}
		}
		else if (bk == TwilightForestHandler.BlockEntry.AURORA.getBlock()) {
			for (int i = 0; i < 16; i++) {
				set.add(new BlockKey(bk, i));
			}
		}
		else if (bk instanceof CustomExcavationStarBehavior) {
			Collection<BlockKey> c = ((CustomExcavationStarBehavior)bk).getSpreadBlocks(world, x, y, z);
			if (c != null && !c.isEmpty())
				set.addAll(c);
		}
		ReikaTreeHelper tree = ReikaTreeHelper.getTree(bk, world.getBlockMetadata(x, y, z));
		if (tree != null) {
			for (int meta : tree.getLogMetadatas()) {
				set.add(new BlockKey(bk, meta));
			}
		}
		tree = ReikaTreeHelper.getTreeFromLeaf(bk, world.getBlockMetadata(x, y, z));
		if (tree != null) {
			for (int meta : tree.getLeafMetadatas()) {
				set.add(new BlockKey(bk, meta));
			}
		}
		ModWoodList mod = ModWoodList.getModWood(bk, world.getBlockMetadata(x, y, z));
		if (mod != null) {
			for (int meta : mod.getLogMetadatas()) {
				set.add(new BlockKey(bk, meta));
			}
		}
		mod = ModWoodList.getModWoodFromLeaf(bk, world.getBlockMetadata(x, y, z));
		if (mod != null) {
			for (int meta : mod.getLeafMetadatas()) {
				set.add(new BlockKey(bk, meta));
			}
		}
		return set;
	}

	@Override
	public int getDepth(EntityPlayer ep, World world, int x, int y, int z) {
		int base = canUseBoostedEffect(ep) ? MAX_DEPTH_BOOST : MAX_DEPTH;
		Block b = world.getBlock(x, y, z);
		if (b instanceof CustomExcavationStarBehavior) {
			base = Math.max(base, ((CustomExcavationStarBehavior)b).getRange(world, x, y, z, ep));
		}
		return base;
	}

	@Override
	public void onPreBreak(ProgressiveBreaker b, World world, int x, int y, int z, Block id, int meta) {

	}

	@Override
	public void onPostBreak(ProgressiveBreaker b, World world, int x, int y, int z, Block id, int meta) {
		PlayerReference p = breakers.get(b.hashCode());
		if (p != null) {
			EntityPlayer ep = p.getPlayer(world);
			if (ReikaEnchantmentHelper.hasEnchantment(Enchantment.flame, p.getHeldItem()))
				this.placeSomeLight(world, x, y, z);
			if (ep != null) {
				this.drainPlayer(ep);
			}
			else {
				b.terminate();
			}
		}
	}

	private void placeSomeLight(World world, int x, int y, int z) {
		if (world.getBlockLightValue(x, y, z) < 8 && !world.canBlockSeeTheSky(x, y, z)) {
			world.setBlock(x, y, z, ChromaBlocks.LIGHT.getBlockInstance(), Flags.PARTICLES.getFlag(), 3);
		}
	}

	@Override
	public boolean canBreak(ProgressiveBreaker b, World world, int x, int y, int z, Block id, int meta) {
		PlayerReference p = breakers.get(b.hashCode());
		if (p != null) {
			EntityPlayer ep = p.getPlayer(world);
			if (ep != null) {
				return this.sufficientEnergy(ep) && this.canBreakBlock(world, x, y, z, id, meta, ep);
			}
		}
		return false;
	}

	private boolean canBreakBlock(World world, int x, int y, int z, Block id, int meta, EntityPlayer ep) {
		return (world.isRemote || ReikaPlayerAPI.playerCanBreakAt((WorldServer)world, x, y, z, (EntityPlayerMP)ep)) && this.canSpreadOn(world, x, y, z, id, meta);
	}

	@Override
	public void onFinish(ProgressiveBreaker b) {
		breakers.remove(b.hashCode());
	}

	@Override
	public void getSpreadBlocks(World world, int x, int y, int z, BlockArray arr, EntityPlayer ep, ItemStack is) {
		Set<BlockKey> set = this.getSpreadBlocks(world, x, y, z);
		if (ep.isSneaking())
			arr.extraSpread = true;
		arr.taxiCabDistance = true;
		arr.recursiveAddMultipleWithBounds(world, x, y, z, set, x-32, y-32, z-32, x+32, y+32, z+32);
	}

}
