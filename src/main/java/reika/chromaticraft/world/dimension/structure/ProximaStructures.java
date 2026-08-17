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

	private ProximaStructures() {}
}
