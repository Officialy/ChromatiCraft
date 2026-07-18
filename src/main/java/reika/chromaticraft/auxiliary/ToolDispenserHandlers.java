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

import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.interfaces.ProjectileFiringTool;
import reika.chromaticraft.items.tools.ItemChromaBucket;
import reika.chromaticraft.tileentity.recipe.TileEntityAuraInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.dragonapi.libraries.ReikaDirectionHelper;
import reika.dragonapi.libraries.ReikaDispenserHelper;
import reika.dragonapi.libraries.reikadispenserhelper.DispenserAction;

public class ToolDispenserHandlers {

	public static class ManipulatorDispenserAction extends DispenserAction {

		@Override
		public ItemStack doAction(World world, int x, int y, int z, ItemStack is, ForgeDirection dir, IBlockSource ref) {
			ChromatiCraft.logger.debug("Manipulator used in dispenser, IBS="+ref);
			TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof TileEntityItemStand) {
				((TileEntityItemStand)te).onRightClickWith(null, ReikaDispenserHelper.getDispenserPlayer(ref, is));
			}
			if (te instanceof TileEntityAuraInfuser) {
				((TileEntityAuraInfuser)te).onRightClickWith(null, ReikaDispenserHelper.getDispenserPlayer(ref, is));
			}
			return is;
		}

	}

	public static class BucketDispenserAction extends DispenserAction {

		@Override
		public ItemStack doAction(World world, int x, int y, int z, ItemStack is, ForgeDirection dir, IBlockSource ref) {
			if (ItemChromaBucket.operateAt(world, x, y, z, is)) {
				is.func_150996_a(Items.bucket);
				is.stackSize = 1;
			}
			return is;
		}

	}

	public static class ProjectileToolDispenserAction extends DispenserAction {

		@Override
		public ItemStack doAction(World world, int x, int y, int z, ItemStack is, ForgeDirection dir, IBlockSource ref) {
			ChromatiCraft.logger.debug("Projectile tool used in dispenser, IBS="+ref);
			Item i = is.getItem();
			if (i instanceof ProjectileFiringTool) {
				EntityPlayer ep = ReikaDispenserHelper.getDispenserPlayer(ref, is);
				ep.setLocationAndAngles(ref.getX(), ref.getY()-1.62, ref.getZ(), -ReikaDirectionHelper.getRelativeAngle(dir, ForgeDirection.SOUTH), 0);
				((ProjectileFiringTool)i).fire(is, world, ep, false);
			}
			else {
				ChromatiCraft.logger.logError("Invalid item "+is.getDisplayName()+" for projectile-tool dispenser action!");
			}
			return is;
		}

	}
}
