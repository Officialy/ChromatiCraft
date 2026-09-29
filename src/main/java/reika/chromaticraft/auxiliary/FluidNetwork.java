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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;

/**
 * V33a {@code FluidNetwork}: the Fluid Link Anchors joined by range. Anchors with positive pressure push the fluids
 * they filter out of their tank into the non-positive ones, highest pressure first; negative ones pull into their
 * tank from the non-negative ones, lowest first. It is transient, rebuilt as anchors tick and merge, as in V33a.
 */
public class FluidNetwork {

	private static final PushSorter pushSorter = new PushSorter();
	private static final SuckSorter suckSorter = new SuckSorter();

	private final ArrayList<PressureData> relays = new ArrayList<>();
	private final ArrayList<PressureData> pushers = new ArrayList<>();
	private final ArrayList<PressureData> suckers = new ArrayList<>();

	private long lastUpdate;

	public void merge(FluidNetwork net, Level world) {
		if (net != null && net != this) {
			for (PressureData p : net.relays) {
				TileEntityFluidRelay te = p.getTile(world);
				if (te == null)
					continue;
				this.add(te);
				te.assignNetwork(this);
			}
			net.clear();
		}
	}

	private void clear() {
		relays.clear();
		pushers.clear();
		suckers.clear();
	}

	public void add(TileEntityFluidRelay te) {
		PressureData p = new PressureData(te);
		if (relays.contains(p))
			return;
		relays.add(p);
		Collections.sort(relays, suckSorter);
		if (p.pressure > 0) {
			pushers.add(p);
			Collections.sort(pushers, pushSorter);
		}
		else if (p.pressure < 0) {
			suckers.add(p);
			Collections.sort(suckers, suckSorter);
		}
	}

	public void remove(TileEntityFluidRelay te) {
		PressureData p = new PressureData(te);
		relays.remove(p);
		pushers.remove(p);
		suckers.remove(p);
	}

	public void updateState(TileEntityFluidRelay te) {
		this.remove(te);
		this.add(te);
	}

	public void update(Level world) {
		if (lastUpdate != world.getGameTime() && !world.isClientSide()) {
			this.doUpdate(world);
		}
		lastUpdate = world.getGameTime();
	}

	private void doUpdate(Level world) {
		Iterator<PressureData> it = pushers.iterator();
		while (it.hasNext()) {
			PressureData p = it.next();
			TileEntityFluidRelay te = p.getTile(world);
			if (te == null) {
				it.remove();
				continue;
			}
			te.pushFluids();
		}
		it = suckers.iterator();
		while (it.hasNext()) {
			PressureData p = it.next();
			TileEntityFluidRelay te = p.getTile(world);
			if (te == null) {
				it.remove();
				continue;
			}
			te.suckFluids();
		}
	}

	/** Drains up to {@code max} of {@code f} from the network's non-suction anchors; returns what it took. */
	public int suck(Level world, Fluid f, int max) {
		int ret = 0;
		Iterator<PressureData> it = relays.iterator();
		while (it.hasNext()) {
			PressureData p = it.next();
			if (p.pressure >= 0) {
				TileEntityFluidRelay te = p.getTile(world);
				if (te == null) {
					it.remove();
					continue;
				}
				int amt = te.suckFluid(f, max);
				if (amt > 0) {
					max -= amt;
					ret += amt;
					if (max == 0)
						break;
				}
			}
		}
		return ret;
	}

	/** Fills up to {@code max} of {@code f} into the network's non-pushing anchors; returns what they took. */
	public int push(Level world, Fluid f, int max) {
		int ret = 0;
		Iterator<PressureData> it = relays.iterator();
		while (it.hasNext()) {
			PressureData p = it.next();
			if (p.pressure <= 0) {
				TileEntityFluidRelay te = p.getTile(world);
				if (te == null) {
					it.remove();
					continue;
				}
				int amt = te.pushFluid(f, max);
				if (amt > 0) {
					max -= amt;
					ret += amt;
					if (max == 0)
						break;
				}
			}
		}
		return ret;
	}

	private static class PressureData {

		private final BlockPos location;
		private final int pressure;

		private PressureData(TileEntityFluidRelay te) {
			location = te.getBlockPos().immutable();
			pressure = te.getCurrentPressure();
		}

		/** Null when the anchor is gone or its chunk is not loaded (V33a: {@code Coordinate.getTileEntity}). */
		private TileEntityFluidRelay getTile(Level world) {
			if (!world.isLoaded(location))
				return null;
			return world.getBlockEntity(location) instanceof TileEntityFluidRelay te ? te : null;
		}

		@Override
		public int hashCode() {
			return location.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof PressureData p && p.location.equals(location);
		}

		@Override
		public String toString() {
			return "P = "+pressure+" @ "+location;
		}

	}

	private static class SuckSorter implements Comparator<PressureData> {

		@Override
		public int compare(PressureData o1, PressureData o2) {
			return Integer.compare(o1.pressure, o2.pressure);
		}

	}

	private static class PushSorter implements Comparator<PressureData> {

		@Override
		public int compare(PressureData o1, PressureData o2) {
			return -Integer.compare(o1.pressure, o2.pressure);
		}

	}

}
