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

	public static final ResourceKey<Structure> MONUMENT = ResourceKey.create(Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "monument"));

	public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet>
			MONUMENT_SET = ResourceKey.create(Registries.STRUCTURE_SET,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "monument"));

	private ProximaStructures() {}
}
