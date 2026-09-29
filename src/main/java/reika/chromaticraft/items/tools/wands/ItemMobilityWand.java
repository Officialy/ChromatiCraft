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

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code ItemMobilityWand} (TELEPORT): teleports the user onto the face of the block they look at, up to 192
 * blocks away, for Black, Lime and Purple energy scaled by how far up and how far away the target is. The target
 * must have two free blocks unless the Ore Clip ability is on.
 */
public class ItemMobilityWand extends ItemWandBase {

	public ItemMobilityWand(Properties properties) {
		super(properties);
		this.addEnergyCost(CrystalElement.BLACK, 100);
		this.addEnergyCost(CrystalElement.LIME, 250);
		this.addEnergyCost(CrystalElement.PURPLE, 50);
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		BlockHitResult mov = ReikaPlayerAPI.getLookedAtBlock(ep, 192, true);
		if (mov != null) {
			BlockPos target = mov.getBlockPos().relative(mov.getDirection());
			int x = target.getX();
			int y = target.getY();
			int z = target.getZ();
			if (Chromabilities.ORECLIP.enabledOn(ep) || this.isValidTarget(world, target)) {
				float f = this.getUsageFactor(ep.getX(), ep.getY(), ep.getZ(), x, y, z);
				if (this.sufficientEnergy(ep, f)) {
					if (!world.isClientSide()) {
						this.drainPlayer(ep, f);
						ep.teleportTo(x+0.5, y+0.25, z+0.5);
					}
					ep.playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
					if (world.isClientSide()) {
						for (int i = 0; i < 128; i++) {
							double rx = x+0.5+(ep.getRandom().nextDouble()*2-1)*0.75;
							double ry = y+0.25-1+(ep.getRandom().nextDouble()*2-1)*0.5;
							double rz = z+0.5+(ep.getRandom().nextDouble()*2-1)*0.75;
							world.addParticle(ParticleTypes.PORTAL, rx, ry, rz, 0, 0, 0);
						}
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	private float getUsageFactor(double posX, double posY, double posZ, int x, int y, int z) {
		float num = 1;
		if (y >= posY) {
			num *= 1.5F;
		}
		if (y >= posY+32) {
			num *= 2F;
		}
		if (y >= posY+64) {
			num *= 2.5F;
		}
		if (y >= posY+128) {
			num *= 4F;
		}
		// V33a measured this "horizontal" distance from x and y, not x and z; kept as written.
		double dd = ReikaMathLibrary.py3d(x-posX, 0, y-posY);
		if (dd >= 16) {
			num *= 1.25F;
		}
		if (dd >= 32) {
			num *= 1.5F;
		}
		if (dd >= 64) {
			num *= 2.5F;
		}
		if (dd >= 128) {
			num *= 4F;
		}
		return num;
	}

	/** V33a {@code softBlocks} at the target and the block above it. */
	private boolean isValidTarget(Level world, BlockPos pos) {
		return world.getBlockState(pos).canBeReplaced() && world.getBlockState(pos.above()).canBeReplaced();
	}

}
