package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.registry.ChromaBlocks;

/**
 * Places one canonical casting temple from its NBT template, so each tier can be spawned with
 * {@code /place feature chromaticraft:casting_temple_l1} (…{@code _l2}, {@code _l3}).
 *
 * <p>These are command-only: no biome modifier references them, exactly as the turbocharged and
 * booster-pylon variants are. Casting temples are player-built in V33a and must never generate.
 *
 * <p>Placement goes through {@link NBTStructureLoader#place} rather than the {@code FilledBlockArray}
 * matcher, because that reads existing state through the backing level and deadlocks a worldgen
 * worker; the same reason natural pylon placement uses it.
 */
public final class CastingTempleFeature extends Feature<NoneFeatureConfiguration> {

	private final int tier;

	public CastingTempleFeature(int tier) {
		super(NoneFeatureConfiguration.CODEC);
		this.tier = tier;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		Identifier template = NBTStructureLoader.chromaTemplate("multiblock/casting_l" + tier);
		// The template anchor is the table-minus-one cell, matching CastingStructure.
		BlockPos anchor = new BlockPos(tier == 3 ? 8 : 6, 0, tier == 3 ? 8 : 6);
		NBTStructureLoader.place(level, template, origin, anchor, state -> state, 2);
		// The template deliberately omits the table cell itself so the structure can be matched
		// around an existing table; spawning a temple for testing should come with one.
		level.setBlock(origin.above(), ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 2);
		if (level.getBlockState(origin).isAir())
			level.setBlock(origin, Blocks.STONE.defaultBlockState(), 2);
		return true;
	}
}
