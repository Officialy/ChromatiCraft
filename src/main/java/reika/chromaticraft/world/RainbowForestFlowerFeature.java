package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code BiomeRainbowForest.plantFlower}: what bone meal grows on Rainbow Forest grass.
 *
 * <p>Seven times in eight a dye flower of a random colour, where one can stand; otherwise the biome's
 * base flower, which for a plain {@code BiomeGenBase} is a dandelion two times in three and a poppy the
 * rest. As with {@link EnderForestFlowerFeature}, this is the only {@code can_spawn_from_bone_meal}
 * feature the biome lists, and it is listed with a count of zero so world generation never runs it.
 */
public final class RainbowForestFlowerFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<RainbowForestFlowerFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(RainbowForestFlowerFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<RainbowForestFlowerFeature> codec() {
        return CODEC;
    }

	public RainbowForestFlowerFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		RandomSource rand = random;
		BlockPos pos = origin;
		if (!world.getBlockState(pos).isAir())
			return false;
		if (rand.nextInt(8) > 0) {
			BlockState flower = ChromaBlocks.dyeFlower(CrystalElement.elements[rand.nextInt(16)]).get().defaultBlockState();
			if (flower.canSurvive(world, pos)) {
				world.setBlock(pos, flower, 3);
				return true;
			}
		}
		BlockState flower = rand.nextInt(3) > 0 ? Blocks.DANDELION.defaultBlockState() : Blocks.POPPY.defaultBlockState();
		if (!flower.canSurvive(world, pos))
			return false;
		world.setBlock(pos, flower, 3);
		return true;
	}
}
