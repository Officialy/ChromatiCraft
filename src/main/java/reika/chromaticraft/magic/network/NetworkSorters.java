/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import java.util.Comparator;

import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

class NetworkSorters {

	static final SourcePrioritizer[] prioritizer = new SourcePrioritizer[16];

	static {
		for (int i = 0; i < 16; i++)
			prioritizer[i] = new SourcePrioritizer(CrystalElement.elements[i]);
	}

	static class TransmitterDistanceSorter implements Comparator<CrystalTransmitter> {

		private final WorldLocation location;

		TransmitterDistanceSorter(CrystalReceiver r) {
			location = new WorldLocation(r.getWorld(), r.getX(), r.getY(), r.getZ());
		}

		@Override
		public int compare(CrystalTransmitter o1, CrystalTransmitter o2) {
			return Double.compare(
					o1.getDistanceSqTo(location.pos.getX(), location.pos.getY(), location.pos.getZ()),
					o2.getDistanceSqTo(location.pos.getX(), location.pos.getY(), location.pos.getZ()));
		}

	}

	static class SourcePrioritizer implements Comparator<CrystalTransmitter> {

		private final CrystalElement color;

		private SourcePrioritizer(CrystalElement e) {
			color = e;
		}

		@Override
		public int compare(CrystalTransmitter o1, CrystalTransmitter o2) {
			if (o1 instanceof CrystalSource && o2 instanceof CrystalSource) {
				return -Integer.compare(PylonFinder.getSourcePriority((CrystalSource)o1, color), PylonFinder.getSourcePriority((CrystalSource)o2, color));
			}
			else if (o1 instanceof CrystalSource) {
				return Integer.MIN_VALUE;
			}
			else if (o2 instanceof CrystalSource) {
				return Integer.MAX_VALUE;
			}
			else {
				return -Integer.compare(o1.getPathPriority(), o2.getPathPriority());
			}
		}

	}

}
