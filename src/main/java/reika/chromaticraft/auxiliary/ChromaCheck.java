/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import reika.chromaticraft.block.blockactivechroma.TileEntityChroma;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;
import reika.dragonapi.interfaces.blockcheck.TileEntityCheck;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class ChromaCheck implements TileEntityCheck {

	public final CrystalElement color;

	public ChromaCheck(CrystalElement e) {
		color = e;
	}

	@Override
	public boolean matchInWorld(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		if (b == ChromaBlocks.CHROMA.getBlockInstance()) {
			TileEntityChroma te = (TileEntityChroma)world.getTileEntity(x, y, z);
			return te != null && te.getElement() == color && te.getBerryCount() == TileEntityChroma.BERRY_SATURATION;
		}
		return false;
	}

	@Override
	public boolean match(Block b, int meta) {
		return b == ChromaBlocks.CHROMA.getBlockInstance();
	}

	@Override
	public void place(World world, int x, int y, int z, int flags) {
		world.setBlock(x, y, z, ChromaBlocks.CHROMA.getBlockInstance());
		TileEntityChroma te = (TileEntityChroma)world.getTileEntity(x, y, z);
		te.activate(color, TileEntityChroma.BERRY_SATURATION);
	}

	@Override
	public ItemStack asItemStack() {
		return new ItemStack(ChromaBlocks.CHROMA.getBlockInstance());
	}

	@Override
	public ItemStack getDisplay() {
		return this.asItemStack();
	}

	@Override
	public BlockKey asBlockKey() {
		return new BlockKey(ChromaBlocks.CHROMA.getBlockInstance());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public TileEntity getTileEntity() {
		TileEntityChroma te = new TileEntityChroma();
		te.worldObj = Minecraft.getMinecraft().theWorld;
		te.activate(color, TileEntityChroma.BERRY_SATURATION);
		return te;
	}

	@Override
	public boolean match(BlockCheck bc) {
		return bc instanceof ChromaCheck && ((ChromaCheck)bc).color == color;
	}

}
