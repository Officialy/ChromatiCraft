/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.magic.CrystalTarget;
import reika.chromaticraft.magic.CrystalTarget.TickingCrystalTarget;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * Base for network tiles that transmit to {@link CrystalTarget}s (pylons, repeaters, sources). Holds
 * the target list + the ley-line render alpha.
 *
 * <p>Deferred: the client ley-line / beam particle FX ({@code ChromaFX.drawLeyLineParticles} +
 * EntityLaserFX/EntityFlareFX) — re-add with the particle-render port.
 */
public abstract class CrystalTransmitterBase extends TileEntityCrystalBase implements CrystalTransmitter {

	private ArrayList<CrystalTarget> targets = new ArrayList<>();
	private ArrayList<TickingCrystalTarget> tickingTargets = new ArrayList<>();

	public int renderAlpha;

	protected CrystalTransmitterBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (renderAlpha > 0)
			renderAlpha -= 4;
		if (renderAlpha < 0)
			renderAlpha = 0;
	}

	@Override
	public final void addTarget(WorldLocation loc, CrystalElement e, double dx, double dy, double dz, double w, double maxW) {
		CrystalTarget tg = new CrystalTarget(this, loc, e, dx, dy, dz, w, maxW);
		if (!this.getLevel().isClientSide()) {
			if (!targets.contains(tg))
				targets.add(tg);
			this.onTargetChanged();
		}
	}

	@Override
	public final void addSelfTickingTarget(WorldLocation loc, CrystalElement e, double dx, double dy, double dz, double w, double maxW, int duration) {
		TickingCrystalTarget tg = new TickingCrystalTarget(this, loc, e, dx, dy, dz, w, maxW, duration);
		if (!this.getLevel().isClientSide()) {
			if (!targets.contains(tg)) {
				targets.add(tg);
				tickingTargets.add(tg);
			}
			this.onTargetChanged();
		}
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		// Deferred: client ley-line particles (ChromaFX.drawLeyLineParticles).
		this.tickTargets();
	}

	private void tickTargets() {
		if (!this.getLevel().isClientSide() && !tickingTargets.isEmpty()) {
			Iterator<TickingCrystalTarget> it = tickingTargets.iterator();
			while (it.hasNext()) {
				TickingCrystalTarget t = it.next();
				if (t.tick()) {
					it.remove();
					targets.remove(t);
					this.syncAllData(true);
				}
			}
		}
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		targets.clear();
	}

	private void onTargetChanged() {
		renderAlpha = 512;
		this.syncAllData(true);
	}

	public final void removeTarget(WorldLocation loc, CrystalElement e) {
		if (!this.getLevel().isClientSide()) {
			targets.remove(new CrystalTarget(this, loc, e, 0));
			this.onTargetChanged();
		}
	}

	public final void clearTargets(boolean unload) {
		if (!this.getLevel().isClientSide()) {
			targets.clear();
			if (!unload)
				this.onTargetChanged();
		}
	}

	public final Collection<CrystalTarget> getTargets() {
		return Collections.unmodifiableCollection(targets);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);

		targets = new ArrayList<>();
		int num = NBT.getIntOr("targetcount", 0);
		for (int i = 0; i < num; i++) {
			CrystalTarget tg = CrystalTarget.readFromNBT("target" + i, NBT);
			if (tg != null)
				targets.add(tg);
		}

		renderAlpha = NBT.getIntOr("alpha", 0);
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);

		NBT.putInt("targetcount", targets.size());
		for (int i = 0; i < targets.size(); i++)
			targets.get(i).writeToNBT("target" + i, NBT);

		NBT.putInt("alpha", renderAlpha);
	}

	@Override
	public AABB getRenderBoundingBox() {
		return !targets.isEmpty() ? AABB.INFINITE : super.getRenderBoundingBox();
	}
}
