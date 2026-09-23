package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDecoFlowers;

/**
 * V33a {@code BiomeEnderForest.plantFlower}: what bone meal grows on Ender Forest grass.
 *
 * <p>Three times in four it is one of the biome's own flowers, Enderflower or Resonant Clover at even
 * odds, placed only if that flower can stand there; otherwise it falls through to the forest's ordinary
 * flower, which in 1.7.10 was a dandelion two times in three and a poppy the rest.
 *
 * <p>In 1.7.10 bone meal asked the biome to {@code plantFlower} on one try in eight. 26.2 does the same
 * thing on the same one-in-eight try, but by picking one of the biome's configured features tagged
 * {@code minecraft:can_spawn_from_bone_meal} and placing it at the spot. This feature is that tagged
 * feature, and the Ender Forest carries no other — its world-generation flowers use an untagged copy of
 * vanilla's — so bone meal's choice in this biome is exactly this and nothing else. It is listed on the
 * biome with a count of zero, because bone meal only finds features the biome lists and upstream never
 * used this path during world generation.
 */
public final class EnderForestFlowerFeature extends Feature<NoneFeatureConfiguration> {

	public EnderForestFlowerFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource rand = context.random();
		BlockPos pos = context.origin();
		BlockState flower;
		if (rand.nextInt(4) > 0) {
			ChromaDecoFlowers type = rand.nextInt(2) == 0
					? ChromaDecoFlowers.ENDERFLOWER : ChromaDecoFlowers.RESONANT_CLOVER;
			flower = ChromaBlocks.decoFlower(type).get().defaultBlockState();
		}
		else {
			// V33a BiomeGenBase.func_150572_a for a type-0 forest.
			flower = rand.nextInt(3) > 0 ? Blocks.DANDELION.defaultBlockState() : Blocks.POPPY.defaultBlockState();
		}
		if (!world.getBlockState(pos).isAir() || !flower.canSurvive(world, pos))
			return false;
		world.setBlock(pos, flower, 3);
		return true;
	}
}
