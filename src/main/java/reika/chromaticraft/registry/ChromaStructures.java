/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import net.minecraft.world.level.Level;

import reika.chromaticraft.auxiliary.structure.CompoundRepeaterStructure;
import reika.chromaticraft.auxiliary.structure.CastingStructure;
import reika.chromaticraft.auxiliary.structure.DataTowerStructure;
import reika.chromaticraft.auxiliary.structure.InfusionStructure;
import reika.chromaticraft.auxiliary.structure.PortalStructure;
import reika.chromaticraft.auxiliary.structure.PylonStructure;
import reika.chromaticraft.auxiliary.structure.PlayerInfusionStructure;
import reika.chromaticraft.auxiliary.structure.RepeaterStructure;
import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.base.ColoredStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.interfaces.registry.StructureEnum;

/**
 * ChromatiCraft multiblock structure registry (port-in-progress). The 1.7.10 original had ~40 entries;
 * this grows as each structure class ports. The complete base pylon and crystal repeater layouts are active.
 */
public enum ChromaStructures implements StructureEnum<ChromaStructureBase> {

	PYLON(PylonStructure.class),
	CASTING1(CastingStructure.Tier1.class),
	CASTING2(CastingStructure.Tier2.class),
	CASTING3(CastingStructure.Tier3.class),
	REPEATER(RepeaterStructure.class),
	COMPOUND(CompoundRepeaterStructure.class),
	PYLONBROADCAST(PylonBroadcastStructure.class),
	INFUSION(InfusionStructure.class),
	PLAYERINFUSION(PlayerInfusionStructure.class),
	PORTAL(PortalStructure.class),
	DATANODE(DataTowerStructure.class);

	public final boolean requiresColor;
	private final Class<? extends ChromaStructureBase> structureClass;
	private ChromaStructureBase structureInstance;

	public static final ChromaStructures[] structureList = values();

	ChromaStructures(Class<? extends ChromaStructureBase> type) {
		structureClass = type;
		requiresColor = ColoredStructureBase.class.isAssignableFrom(structureClass);
	}

	public static void buildStructures() {
		for (ChromaStructures s : structureList)
			s.getStructure();
	}

	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return this.getStructure().getArray(world, x, y, z);
	}

	public FilledBlockArray getArray(Level world, int x, int y, int z, CrystalElement e) {
		ChromaStructureBase s = this.getStructure();
		s.resetToDefaults();
		if (e != null && s instanceof ColoredStructureBase)
			return ((ColoredStructureBase)s).getArray(world, x, y, z, e);
		return s.getArray(world, x, y, z);
	}

	@Override
	public synchronized ChromaStructureBase getStructure() {
		if (structureInstance == null) {
			try {
				structureInstance = structureClass.getDeclaredConstructor().newInstance();
			}
			catch (ReflectiveOperationException e) {
				throw new RuntimeException("Could not instantiate structure type " + this, e);
			}
		}
		return structureInstance;
	}

	@Override
	public boolean isNatural() {
		return this == PYLON || this == PYLONBROADCAST || this == DATANODE;
	}
}
