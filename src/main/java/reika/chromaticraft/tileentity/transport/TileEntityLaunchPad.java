/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.*;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.maps.TimerMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * V33a {@code TileEntityLaunchPad}, the Vertical Propulsion Panel. On its crystalline-stone frame it charges for 20 ticks
 * while something stands on it (a lone sneaking player excepted; items and XP orbs never count), then flings everything
 * on it skyward -- players at 3.5-4 blocks a tick, anything else at 1.8 -- scaled by the power option, and cancels the
 * fall of whatever lands on it. Ringed by liquid ender at distance two it is enhanced: 2.5 times the speed, and those it
 * launched keep being pushed for 12-18 more ticks.
 */
public class TileEntityLaunchPad extends TileEntityChromaticBase implements MultiBlockChromaTile {

	public static final int CHARGE_DURATION = 20;

	private int charge;
	private boolean structure;
	private boolean enhanced;

	private boolean needsBigUpdate;

	private final TimerMap<Entity> currentLaunches = new TimerMap();

	public TileEntityLaunchPad(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LAUNCH_PAD.get(), pos, state);
	}

	public float getChargeFraction() {
		return charge/(float)CHARGE_DURATION;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.LAUNCHPAD;
	}

	@Override
	public boolean hasStructure() {
		return structure;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		if (!world.isClientSide() && this.hasStructure()) {
			AABB box = new AABB(x-0.75, y+0.9375, z-0.75, x+1.75, y+2.5, z+1.75);
			List<Entity> li = world.getEntitiesOfClass(Entity.class, box);
			for (Entity e : li) {
				e.fallDistance = 0;
			}

			currentLaunches.tick();
			for (Entity e : currentLaunches.keySet()) {
				this.flingEntity(e, false);
			}

			boolean flag = charge > 0;
			box = new AABB(x-1, y+0.9375, z-1, x+2, y+1.0625, z+2);
			li = new ArrayList(world.getEntitiesOfClass(Entity.class, box));
			Iterator<Entity> it = li.iterator();
			while (it.hasNext()) {
				Entity e = it.next();
				if (e instanceof ItemEntity || e instanceof ExperienceOrb)
					it.remove();
			}
			if (li.size() == 1 && li.get(0).isShiftKeyDown())
				li.clear();
			if (!li.isEmpty()) {
				if (charge == CHARGE_DURATION) {
					for (Entity e : li) {
						this.flingEntity(e, true);
					}
					needsBigUpdate = true;
					ChromaSounds.KILLAURA.playSoundAtBlockNoAttenuation(this, 1, 0.5F, 90);
					ChromaNetwork.sendLaunchFire((ServerLevel)world, pos);
				}
				else {
					charge++;
					ChromaSounds.KILLAURA_CHARGE.playSoundAtBlock(this, 1, 0.5F+this.getChargeFraction()*0.5F);
				}
			}
			else if (charge > 0) {
				charge = Math.max(0, charge-2);
				if (charge == 0) {
					needsBigUpdate = true;
				}
			}
			if (flag || charge > 0) {
				this.syncAllData(false);
			}
		}
	}

	private void flingEntity(Entity e, boolean cache) {
		float v = this.getSpeedFactor();
		Vec3 m = e.getDeltaMovement();
		if (e instanceof Player) {
			e.setDeltaMovement(m.x, Math.max(m.y, (3.5+rand.nextDouble()*0.5)*v), m.z);
		}
		else {
			e.setDeltaMovement(m.x, 1.8*v, m.z);
		}
		e.syncVelocity = true;
		e.fallDistance = -100;

		if (cache && this.isEnhanced()) {
			currentLaunches.put(e, 12+rand.nextInt(7));
		}
	}

	private float getSpeedFactor() {
		float base = this.isEnhanced() ? 2.5F : 1;
		return base*Math.max(0.25F, ChromaOptions.LAUNCHPOWER.getFloat());
	}

	public boolean isEnhanced() {
		return enhanced;
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		this.checkForStructure(world, pos);
	}

	/** V33a re-checked the frame whenever a neighbouring block changed. */
	@Override
	protected void onAdjacentBlockUpdate() {
		this.validateStructure();
	}

	private void checkForStructure(Level world, BlockPos pos) {
        // Structure templates are server-owned; clients retain the synchronized result.
        if (world.isClientSide()) return;
		ChromaStructureBase struct = ChromaStructures.LAUNCHPAD.getStructure();
		struct.resetToDefaults();
		enhanced = false;
		structure = struct.getArray(world, pos.getX(), pos.getY(), pos.getZ()).matchInWorld();
		if (structure) {
			enhanced = this.checkForEnhanced(world, pos);
		}
		if (!world.isClientSide())
			this.syncAllData(true);
	}

	/** Every block of the ring at distance two must be a liquid ender source (V33a: the "ender" fluid, metadata 0). */
	private boolean checkForEnhanced(Level world, BlockPos pos) {
		for (int i = -2; i <= 2; i++) {
			for (int k = -2; k <= 2; k++) {
				if (Math.abs(i) != 2 && Math.abs(k) != 2)
					continue;
				FluidState f = world.getFluidState(pos.offset(i, 0, k));
				if (!f.is(ChromaFluids.ENDER.get()) || !f.isSource()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		NBT.putBoolean("struct", structure);
		NBT.putBoolean("enhanced", enhanced);
		NBT.putInt("charge", charge);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		structure = NBT.getBooleanOr("struct", false);
		enhanced = NBT.getBooleanOr("enhanced", false);
		charge = NBT.getIntOr("charge", 0);
	}

	@Override
	public int getUpdatePacketRadius() {
		return needsBigUpdate ? 128 : 32;
	}

	@Override
	protected void onSync() {
		needsBigUpdate = false;
	}

	@Override
	public void validateStructure() {
		if (this.getLevel() != null)
			this.checkForStructure(this.getLevel(), this.getBlockPos());
	}

	@Override
	public ChromaStructures getPrimaryStructure() {
		return ChromaStructures.LAUNCHPAD;
	}

	@Override
	public Coordinate getStructureOffset() {
		return null;
	}

	@Override
	public boolean canStructureBeInspected() {
		return false;
	}

}
