/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic;

import net.minecraft.nbt.CompoundTag;

import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.network.PylonFinder;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

public class CrystalTarget {

	public final WorldLocation source;
	public final WorldLocation location;
	public final CrystalElement color;
	public final double endWidth;
	public final double offsetX;
	public final double offsetY;
	public final double offsetZ;
	public final double widthLimit;

	public CrystalTarget(WorldLocation src, WorldLocation target, CrystalElement color, double w) {
		this(src, target, color, 0, 0, 0, w, w);
	}

	public CrystalTarget(CrystalNetworkTile src, WorldLocation target, CrystalElement color, double w) {
		this(PylonFinder.getLocation(src), target, color, 0, 0, 0, w, w);
	}

	public CrystalTarget(CrystalNetworkTile src, WorldLocation target, CrystalElement color, double dx, double dy, double dz, double w, double maxW) {
		this(PylonFinder.getLocation(src), target, color, dx, dy, dz, w, maxW);
	}

	public CrystalTarget(WorldLocation src, WorldLocation target, CrystalElement color, double dx, double dy, double dz, double w, double maxW) {
		if (src == null)
			throw new IllegalArgumentException("Cannot supply null source!");
		if (target == null)
			throw new IllegalArgumentException("Cannot supply null target!");
		if (color == null)
			throw new IllegalArgumentException("Cannot supply null color!");
		source = src;
		this.color = color;
		location = target;
		offsetX = dx;
		offsetY = dy;
		offsetZ = dz;
		endWidth = w;
		widthLimit = maxW;
	}

	public void writeToNBT(String name, CompoundTag NBT) {
		if (location == null || color == null)
			return;
		CompoundTag tag = new CompoundTag();
		tag.putInt("color", color.ordinal());
		tag.putDouble("dx", offsetX);
		tag.putDouble("dy", offsetY);
		tag.putDouble("dz", offsetZ);
		tag.putDouble("width", endWidth);
		tag.putDouble("maxw", widthLimit);
		location.saveAdditional("loc", tag);
		source.saveAdditional("src", tag);
		NBT.put(name, tag);
	}

	public static CrystalTarget readFromNBT(String name, CompoundTag NBT) {
		if (!NBT.contains(name))
			return null;
		CompoundTag tag = NBT.getCompoundOrEmpty(name);
		if (tag == null)
			return null;
		WorldLocation loc = WorldLocation.load("loc", tag);
		WorldLocation src = WorldLocation.load("src", tag);
		CrystalElement e = CrystalElement.elements[tag.getIntOr("color", 0)];
		double dx = tag.getDoubleOr("dx", 0);
		double dy = tag.getDoubleOr("dy", 0);
		double dz = tag.getDoubleOr("dz", 0);
		double w = tag.getDoubleOr("width", 0);
		double maxw = tag.getDoubleOr("maxw", 0);
		return loc != null && src != null && e != null ? new CrystalTarget(src, loc, e, dx, dy, dz, w, maxw) : null;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CrystalTarget) {
			CrystalTarget t = (CrystalTarget)o;
			return t.location.equals(location) && t.color == color; //ignore render width
		}
		return false;
	}

	@Override
	public int hashCode() {
		return location.hashCode()+color.ordinal(); //ignore render width
	}

	@Override
	public String toString() {
		return color.name()+": "+location.getBlockEntity()+" {"+offsetX+","+offsetY+","+offsetZ+"}";
	}

	public static class TickingCrystalTarget extends CrystalTarget {

		private final int lifespan;
		private int tick;

		public TickingCrystalTarget(CrystalNetworkTile src, WorldLocation target, CrystalElement color, double w, int l) {
			super(src, target, color, w);
			lifespan = l;
		}

		public TickingCrystalTarget(CrystalNetworkTile src, WorldLocation target, CrystalElement color, double dx, double dy, double dz, double w, double maxW, int l) {
			super(src, target, color, dx, dy, dz, w, maxW);
			lifespan = l;
		}

		public boolean tick() {
			tick++;
			return tick >= lifespan;
		}

	}

}
