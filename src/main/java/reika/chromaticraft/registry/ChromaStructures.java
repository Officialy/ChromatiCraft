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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import net.minecraft.util.StatCollector;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.structure.BoostedLumenTreeStructure;
import reika.chromaticraft.auxiliary.structure.BoostedRelayStructure;
import reika.chromaticraft.auxiliary.structure.BroadcasterStructure;
import reika.chromaticraft.auxiliary.structure.CastingL1Structure;
import reika.chromaticraft.auxiliary.structure.CastingL2Structure;
import reika.chromaticraft.auxiliary.structure.CastingL3Structure;
import reika.chromaticraft.auxiliary.structure.CloakingTowerStructure;
import reika.chromaticraft.auxiliary.structure.CompoundRepeaterStructure;
import reika.chromaticraft.auxiliary.structure.GateStructure;
import reika.chromaticraft.auxiliary.structure.InfusionStructure;
import reika.chromaticraft.auxiliary.structure.LaunchPadStructure;
import reika.chromaticraft.auxiliary.structure.LumenTreeStructure;
import reika.chromaticraft.auxiliary.structure.MeteorTowerStructure;
import reika.chromaticraft.auxiliary.structure.NetworkOptimizerStructureV2;
import reika.chromaticraft.auxiliary.structure.PersonalChargerStructure;
import reika.chromaticraft.auxiliary.structure.PlayerInfusionStructure;
import reika.chromaticraft.auxiliary.structure.PortalStructure;
import reika.chromaticraft.auxiliary.structure.ProgressionLinkerStructure;
import reika.chromaticraft.auxiliary.structure.ProtectionBeaconStructure;
import reika.chromaticraft.auxiliary.structure.PylonTurboStructure;
import reika.chromaticraft.auxiliary.structure.RepeaterStructure;
import reika.chromaticraft.auxiliary.structure.RitualStructure;
import reika.chromaticraft.auxiliary.structure.TreeSendFocusStructure;
import reika.chromaticraft.auxiliary.structure.WeakRepeaterStructure;
import reika.chromaticraft.auxiliary.structure.WirelessPedestalL2Structure;
import reika.chromaticraft.auxiliary.structure.WirelessPedestalStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.BiomeStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.BurrowStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.CavernStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.DataTowerStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.DesertStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.OceanStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.PylonStructure;
import reika.chromaticraft.auxiliary.structure.worldgen.SnowStructure;
import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.base.FragmentStructureBase;
import reika.chromaticraft.modinterface.voidritual.VoidMonsterNetherStructure;
import reika.chromaticraft.modinterface.voidritual.VoidMonsterRitualStructure;
import reika.dragonapi.DragonAPICore;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.interfaces.registry.StructureEnum;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum ChromaStructures implements StructureEnum<ChromaStructureBase> {

	PYLON(PylonStructure.class),
	CASTING1(CastingL1Structure.class),
	CASTING2(CastingL2Structure.class),
	CASTING3(CastingL3Structure.class),
	RITUAL(RitualStructure.class,	false),
	RITUAL2(RitualStructure.class,	true),
	INFUSION(InfusionStructure.class),
	PLAYERINFUSION(PlayerInfusionStructure.class),
	TREE(LumenTreeStructure.class),
	TREE_SENDER(TreeSendFocusStructure.class),
	TREE_BOOSTED(BoostedLumenTreeStructure.class),
	REPEATER(RepeaterStructure.class),
	COMPOUND(CompoundRepeaterStructure.class),
	CAVERN(CavernStructure.class),
	BURROW(BurrowStructure.class),
	OCEAN(OceanStructure.class),
	DESERT(DesertStructure.class),
	SNOWSTRUCT(SnowStructure.class),
	BIOMEFRAG(BiomeStructure.class),
	PORTAL(PortalStructure.class),
	PERSONAL(PersonalChargerStructure.class),
	BROADCAST(BroadcasterStructure.class),
	CLOAKTOWER(CloakingTowerStructure.class),
	PROTECT(ProtectionBeaconStructure.class),
	WEAKREPEATER(WeakRepeaterStructure.class),
	METEOR1(MeteorTowerStructure.class,	0),
	METEOR2(MeteorTowerStructure.class,	1),
	METEOR3(MeteorTowerStructure.class,	2),
	TELEGATE(GateStructure.class),
	RELAY(BoostedRelayStructure.class),
	PYLONBROADCAST(PylonBroadcastStructure.class),
	PYLONTURBO(PylonTurboStructure.class),
	DATANODE(DataTowerStructure.class),
	WIRELESSPEDESTAL(WirelessPedestalStructure.class),
	WIRELESSPEDESTAL2(WirelessPedestalL2Structure.class),
	PROGRESSLINK(ProgressionLinkerStructure.class),
	OPTIMIZER(NetworkOptimizerStructureV2.class),
	VOIDRITUAL(VoidMonsterRitualStructure.class),
	NETHERTRAP(VoidMonsterNetherStructure.class),
	LAUNCHPAD(LaunchPadStructure.class);

	public final boolean requiresColor;
	private final Class<? extends ChromaStructureBase> structureClass;
	private final Object[] constructorData;

	private ChromaStructureBase structureInstance;

	public static final ChromaStructures[] structureList = values();

	private ChromaStructures(Class<? extends ChromaStructureBase> type, Object... data) {
		structureClass = type;
		requiresColor = ColoredStructureBase.class.isAssignableFrom(structureClass);
		constructorData = data;
	}

	public static void buildStructures() {
		for (int i = 0; i < structureList.length; i++) {
			structureList[i].construct();
		}
	}

	private void construct() {
		try {
			structureInstance = this.instantiate(constructorData);
		}
		catch (Exception e) {
			throw new RegistrationException(ChromatiCraft.instance, "Could not instantiate structure type "+this+"!", e);
		}
	}

	private ChromaStructureBase instantiate(Object... data) throws Exception {
		if (structureClass == MeteorTowerStructure.class) {
			Constructor c = structureClass.getConstructor(int.class);
			return (ChromaStructureBase)c.newInstance(data[0]);
		}
		else if (structureClass == RitualStructure.class) {
			Constructor c = structureClass.getConstructor(boolean.class);
			return (ChromaStructureBase)c.newInstance(data[0]);
		}
		return structureClass.newInstance();
	}

	public synchronized FilledBlockArray getArray(World world, int x, int y, int z) {
		return this.getStructure().getArray(world, x, y, z);
	}

	public synchronized FilledBlockArray getArray(World world, int x, int y, int z, Random r) {
		ChromaStructureBase s = this.getStructure();
		s.resetToDefaults();
		if (r != null)
			s.setRand(r);
		FilledBlockArray ret = s.getArray(world, x, y, z);
		return ret;
	}

	public synchronized FilledBlockArray getArray(World world, int x, int y, int z, CrystalElement e) {
		return this.getArray(world, x, y, z, null, e);
	}

	public synchronized FilledBlockArray getArray(World world, int x, int y, int z, Random r, CrystalElement e) {
		if (e == null)
			return this.getArray(world, x, y, z, r);
		ColoredStructureBase s = (ColoredStructureBase)this.getStructure();
		s.resetToDefaults();
		if (r != null)
			s.setRand(r);
		return s.getArray(world, x, y, z, e);
	}

	@SideOnly(Side.CLIENT)
	public synchronized FilledBlockArray getStructureForDisplay() {
		ChromaStructureBase s = this.getStructure();
		s.setRand(DragonAPICore.rand);
		return this.getStructure().getStructureForDisplay();
	}

	public String getDisplayName() {
		return StatCollector.translateToLocal("chromastruct."+this.name().toLowerCase(Locale.ENGLISH));
	}

	public boolean isNatural() {
		switch(this) {
			case PYLON:
			case CAVERN:
			case BURROW:
			case OCEAN:
			case DESERT:
			case SNOWSTRUCT:
			case BIOMEFRAG:
			case DATANODE:
				return true;
			default:
				return false;
		}
	}

	@Override
	public ChromaStructureBase getStructure() {
		return structureInstance;
	}

	public WeightedRandomChestContent[] getModifiedLootSet(WeightedRandomChestContent[] items) {
		if (structureInstance instanceof FragmentStructureBase) {
			ArrayList<WeightedRandomChestContent> li = ReikaJavaLibrary.makeListFromArray(items);
			((FragmentStructureBase)structureInstance).modifyLootSet(li);
			items = li.toArray(new WeightedRandomChestContent[li.size()]);
		}
		return items;
	}

	public ChromaResearch getFragment() {
		return ChromaResearch.getPageFor(this);
	}

}
