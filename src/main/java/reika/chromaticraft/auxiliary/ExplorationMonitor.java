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

import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.interfaces.ProgressionTrigger;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.modinterface.ModInteraction;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.world.BiomeGlowingCliffs;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.trackers.tickregistry.TickHandler;
import reika.dragonapi.auxiliary.trackers.tickregistry.TickType;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.modinteract.deepinteract.ReikaMystcraftHelper;
import reika.dragonapi.modinteract.itemhandlers.ThaumItemHelper;

import cpw.mods.fml.common.gameevent.TickEvent.Phase;

public class ExplorationMonitor implements TickHandler {

	public static final ExplorationMonitor instance = new ExplorationMonitor();

	private ExplorationMonitor() {

	}

	@Override
	public void tick(TickType type, Object... tickData) {
		EntityPlayer ep = (EntityPlayer)tickData[0];
		World world = ep.worldObj;
		if (!world.isRemote) {
			if (ModList.MYSTCRAFT.isLoaded() && ReikaMystcraftHelper.isMystAge(world)) {
				ProgressStage.MYST.stepPlayerTo(ep);
			}
			//ProgressionManager.instance.setPlayerDiscoveredColor(ep, CrystalElement.RED, true);
			int x0 = MathHelper.floor_double(ep.posX);
			int y0 = MathHelper.floor_double(ep.posY)+1;
			int z0 = MathHelper.floor_double(ep.posZ);
			MovingObjectPosition mov = ReikaPlayerAPI.getLookedAtBlock(ep, 4, true);
			if (mov != null) {
				int x = mov.blockX;
				int y = mov.blockY;
				int z = mov.blockZ;

				if (ChromaTiles.getTile(world, x, y, z) == ChromaTiles.PYLON) {
					TileEntityCrystalPylon te = (TileEntityCrystalPylon)world.getTileEntity(x, y, z);
					if (te.hasStructure() && te.getEnergy(te.getColor()) >= te.getMaxStorage(te.getColor())/10) {
						ProgressionManager.instance.setPlayerDiscoveredColor(ep, te.getColor(), true, true);
						if (ModList.THAUMCRAFT.isLoaded() && ReikaItemHelper.matchStacks(ep.getCurrentEquippedItem(), ThaumItemHelper.ItemEntry.THAUMOMETER.getItem())) {
							if (ep.isUsingItem() && ep.itemInUseCount <= 5)
								if (!ModInteraction.triggerPylonScanProgress(ep, te))
									ep.clearItemInUse();
						}
					}
				}

				Block b = world.getBlock(x, y, z);
				if (b instanceof ProgressionTrigger) {
					ProgressStage[] ps = ((ProgressionTrigger)b).getTriggers(ep, world, x, y, z);
					if (ps != null) {
						for (int i = 0; i < ps.length; i++) {
							ProgressStage p = ps[i];
							p.stepPlayerTo(ep);
						}
					}
				}
				else if (b == Blocks.bedrock && y < 6) {
					ProgressStage.BEDROCK.stepPlayerTo(ep);
				}
				else if (b == Blocks.mob_spawner) {
					ProgressStage.FINDSPAWNER.stepPlayerTo(ep);
				}
				else if (ModList.THAUMCRAFT.isLoaded() && b == ThaumItemHelper.BlockEntry.NODE.getBlock() && world.getBlockMetadata(x, y, z) == ThaumItemHelper.BlockEntry.NODE.metadata) {
					ProgressStage.NODE.stepPlayerTo(ep);
				}
			}

			if (world.provider.dimensionId == -1 && ep.posY > 128) {
				ProgressStage.NETHERROOF.stepPlayerTo(ep);
			}

			if (world.provider.dimensionId == 0 && ep.posY < 18 && world.getSavedLightValue(EnumSkyBlock.Sky, x0, y0, z0) == 0) {
				ProgressStage.DEEPCAVE.stepPlayerTo(ep);
			}

			if (world.provider.dimensionId == 0 && ChromatiCraft.isRainbowForest(world.getBiomeGenForCoords(x0, z0))) {
				ProgressStage.RAINBOWFOREST.stepPlayerTo(ep);
			}

			if (world.provider.dimensionId == 0 && BiomeGlowingCliffs.isGlowingCliffs(world.getBiomeGenForCoords(x0, z0))) {
				ProgressStage.GLOWCLIFFS.stepPlayerTo(ep);
			}
		}
	}

	@Override
	public EnumSet<TickType> getType() {
		return EnumSet.of(TickType.PLAYER);
	}

	@Override
	public boolean canFire(Phase p) {
		return p == Phase.START;
	}

	@Override
	public String getLabel() {
		return "ChromatiCraft Exploration Monitor";
	}

}
