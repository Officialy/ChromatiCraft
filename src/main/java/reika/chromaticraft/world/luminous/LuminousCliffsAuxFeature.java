package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import reika.chromaticraft.block.worldgen26.BlockGlowDaisy;
import reika.chromaticraft.block.worldgen26.BlockTieredPlant;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.world.GlowingCliffsColumnShaper;

/**
 * V33a {@code GlowingCliffsAuxGenerator}'s after-population passes over the Luminous Cliffs, in
 * upstream's order.
 *
 * <p>{@code growSaplings}: the oak saplings the column shaper leaves on cavern floors become underground
 * glowing trees — one time in forty a big one, otherwise a small oak, both with one leaf in ten a Glowing
 * Leaf. A tree that does not fit gets one more try; the sapling is removed either way.
 *
 * <p>{@code cleanupBlocks}, from four below sea level to the top of the high plateau: grass under an
 * opaque block becomes dirt, dirt or grass with nothing solid under it and something solid over it
 * becomes stone, and Aura Blooms or Glow Daisies left standing on nothing are removed.
 *
 * <p>The floating islands, the third pass, are {@code LuminousIslandStructure}. The fourth,
 * {@code generateMaterialVeins}, only ever placed Mystcraft crystal and is CHROMA-PORT with Mystcraft.
 */
public final class LuminousCliffsAuxFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<LuminousCliffsAuxFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(LuminousCliffsAuxFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<LuminousCliffsAuxFeature> codec() {
        return CODEC;
    }

	/** V33a {@code getUndergroundTreeGen(rand, true, 40)}. */
	private static final int BIG_TREE_CHANCE = 40;

	public LuminousCliffsAuxFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		WorldGenLevel level = world;
		ChunkPos chunk = ChunkPos.containing(origin);
		GlowingCliffsColumnShaper.Sampler sampler = LuminousCliffsTerrainFeature.sampler(level);
		var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
		Feature small = features.getValue(ChromaWorldGenProvider.UNDERGROUND_GLOWING_TREE_SMALL);
		Feature big = features.getValue(ChromaWorldGenProvider.UNDERGROUND_GLOWING_TREE_BIG);
		boolean changed = false;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				int x = chunk.getMinBlockX() + i;
				int z = chunk.getMinBlockZ() + k;
				if (!sampler.biome(x, z).isGlowingCliffs())
					continue;
				for (int y = GlowingCliffsColumnShaper.SEA_LEVEL; y <= GlowingCliffsColumnShaper.MAX_MIDDLE_TOP_Y + 1; y++) {
					pos.set(x, y, z);
					if (!level.getBlockState(pos).is(Blocks.OAK_SAPLING))
						continue;
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					Feature tree = random.nextInt(BIG_TREE_CHANCE) == 0 ? big : small;
					BlockPos at = pos.immutable();
					if (tree != null && !tree.place(level, generator, random, at))
						tree.place(level, generator, random, at);
					changed = true;
				}
			}
		}

		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				int x = chunk.getMinBlockX() + i;
				int z = chunk.getMinBlockZ() + k;
				if (!sampler.biome(x, z).isGlowingCliffs())
					continue;
				for (int y = GlowingCliffsColumnShaper.SEA_LEVEL - 4; y <= GlowingCliffsColumnShaper.MAX_UPPER_TOP_Y; y++) {
					pos.set(x, y, z);
					changed |= this.cleanup(level, pos);
				}
			}
		}
		return changed;
	}

	/** V33a {@code cleanupBlocks} for one position. */
	private boolean cleanup(WorldGenLevel level, BlockPos pos) {
		BlockState b = level.getBlockState(pos);
		BlockState b2 = b;
		BlockState above = level.getBlockState(pos.above());
		BlockState below = level.getBlockState(pos.below());
		if (b.is(Blocks.GRASS_BLOCK) && above.isSolidRender())
			b2 = Blocks.DIRT.defaultBlockState();
		if ((b.is(Blocks.DIRT) || b.is(Blocks.GRASS_BLOCK)) && !below.isSolid() && above.isSolid())
			b2 = Blocks.STONE.defaultBlockState();
		boolean auraBloom = b.getBlock() instanceof BlockTieredPlant plant && plant.getPlant() == ChromaTieredPlants.AURA_BLOOM;
		if ((auraBloom || b.getBlock() instanceof BlockGlowDaisy) && !below.isSolid())
			b2 = Blocks.AIR.defaultBlockState();
		if (b2 != b) {
			level.setBlock(pos, b2, 2);
			return true;
		}
		return false;
	}
}
