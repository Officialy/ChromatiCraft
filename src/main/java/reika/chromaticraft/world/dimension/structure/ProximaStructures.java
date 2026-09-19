package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;

/**
 * Proxima's structure types, and the datapack keys their instances are registered under.
 *
 * <p>The oversized decoration generators live here rather than among the features: a structure's
 * pieces are laid out once and then written chunk by chunk with a clipped box, which is the only way a
 * shape wider than a chunk's write window can be built. See {@link GlassCliffPiece}.
 */
public final class ProximaStructures {

	public static final DeferredRegister<StructureType<?>> TYPES =
			DeferredRegister.create(Registries.STRUCTURE_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<StructureType<?>, StructureType<GlassCliffStructure>>
			GLASS_CLIFF_TYPE = TYPES.register("glass_cliff", () -> () -> GlassCliffStructure.CODEC);

	/** The datapack entry, which is what a biome's structure set points at. */
	public static final ResourceKey<Structure> GLASS_CLIFF = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "glass_cliff"));

	/** The structure set that decides how often and how far apart the cliffs appear. */
	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			GLASS_CLIFF_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "glass_cliff"));

	public static final DeferredHolder<StructureType<?>, StructureType<GlowCaveStructure>>
			GLOW_CAVE_TYPE = TYPES.register("glow_cave", () -> () -> GlowCaveStructure.CODEC);

	public static final ResourceKey<Structure> GLOW_CAVE = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "glow_cave"));

	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			GLOW_CAVE_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "glow_cave"));

	public static final DeferredHolder<StructureType<?>, StructureType<ProximaMonumentStructure>>
			MONUMENT_TYPE = TYPES.register("monument", () -> () -> ProximaMonumentStructure.CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<ProximaLightPanelStructure>>
			LIGHT_PANEL_TYPE = TYPES.register("light_panel", () -> () -> ProximaLightPanelStructure.CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<ProximaThreeDMazeStructure>>
			THREE_D_MAZE_TYPE = TYPES.register("three_d_maze", () -> () -> ProximaThreeDMazeStructure.CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<ProximaMusicStructure>>
			MUSIC_TYPE = TYPES.register("music", () -> () -> ProximaMusicStructure.CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<ProximaGOLStructure>>
			GOL_TYPE = TYPES.register("gol", () -> () -> ProximaGOLStructure.CODEC);

	public static final DeferredHolder<StructureType<?>, StructureType<PortalStructureCommand>>
			PORTAL_TYPE = TYPES.register("portal", () -> () -> PortalStructureCommand.CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<CastingTempleStructure>>
			CASTING_TEMPLE_L1_TYPE = TYPES.register("casting_temple_l1",
					() -> () -> CastingTempleStructure.L1_CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<CastingTempleStructure>>
			CASTING_TEMPLE_L2_TYPE = TYPES.register("casting_temple_l2",
					() -> () -> CastingTempleStructure.L2_CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<CastingTempleStructure>>
			CASTING_TEMPLE_L3_TYPE = TYPES.register("casting_temple_l3",
					() -> () -> CastingTempleStructure.L3_CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<BurrowCommandStructure>>
			BURROW_TYPE = TYPES.register("burrow", () -> () -> BurrowCommandStructure.CODEC);

	/** Command-only player multiblock; intentionally absent from every natural structure set. */
	public static final ResourceKey<Structure> PORTAL = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "portal"));
	public static final ResourceKey<Structure> CASTING_TEMPLE_L1 = commandKey("casting_temple_l1");
	public static final ResourceKey<Structure> CASTING_TEMPLE_L2 = commandKey("casting_temple_l2");
	public static final ResourceKey<Structure> CASTING_TEMPLE_L3 = commandKey("casting_temple_l3");
	/** Command-placeable complete Burrow; natural Burrows remain the rarity-controlled feature. */
	public static final ResourceKey<Structure> BURROW = commandKey("burrow");

	private static ResourceKey<Structure> commandKey(String path) {
		return ResourceKey.create(Registries.STRUCTURE,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
	}

	/**
	 * The monument's placement is its own type because it names one chunk rather than a spread; see
	 * {@link MonumentPlacement}.
	 */
	public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>>
			PLACEMENTS = DeferredRegister.create(
					net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_PLACEMENT, ChromatiCraft.MODID);

	public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
			net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<MonumentPlacement>>
			MONUMENT_PLACEMENT = PLACEMENTS.register("monument", () -> () -> MonumentPlacement.CODEC);
	public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
			net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<LightPanelPlacement>>
			LIGHT_PANEL_PLACEMENT = PLACEMENTS.register("light_panel", () -> () -> LightPanelPlacement.CODEC);
	public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
			net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<ThreeDMazePlacement>>
			THREE_D_MAZE_PLACEMENT = PLACEMENTS.register("three_d_maze", () -> () -> ThreeDMazePlacement.CODEC);
	public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
			net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<MusicPlacement>>
			MUSIC_PLACEMENT = PLACEMENTS.register("music", () -> () -> MusicPlacement.CODEC);
	public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
			net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<GOLPlacement>>
			GOL_PLACEMENT = PLACEMENTS.register("gol", () -> () -> GOLPlacement.CODEC);

	public static final ResourceKey<Structure> MONUMENT = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "monument"));

	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			MONUMENT_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "monument"));

	public static final ResourceKey<Structure> LIGHT_PANEL = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "light_panel"));
	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			LIGHT_PANEL_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "light_panel"));
	public static final ResourceKey<Structure> THREE_D_MAZE = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "three_d_maze"));
	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			THREE_D_MAZE_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "three_d_maze"));
	public static final ResourceKey<Structure> MUSIC = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "music"));
	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			MUSIC_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "music"));
	public static final ResourceKey<Structure> GOL = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "gol"));
	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			GOL_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "gol"));

	private ProximaStructures() {}
}
