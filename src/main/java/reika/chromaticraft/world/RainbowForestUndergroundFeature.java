package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.block.worldgen26.BlockSparkle;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code RainbowForestDecorator.generateUndergroundLight}: what lies under the Rainbow Forest.
 *
 * <p>Half the chunks get one ellipsoid of sparkling ground: a random sparkle type, radii of 1-6 on each
 * axis, centred somewhere between y 4 and 67, turning every block of that type's proxy inside it into
 * the sparkling version, so a stone ellipsoid glitters through the rock while a clay one only catches
 * whatever clay it happens to cross. Every chunk then scatters 8-31 floating Ethereal Lights into
 * whatever air (caves) happens to be at y 4-67.
 *
 * <p>The heights are upstream's absolute ones; 26.2's deeper world below them is left untouched, as it
 * did not exist.
 */
public final class RainbowForestUndergroundFeature extends Feature<NoneFeatureConfiguration> {

	public RainbowForestUndergroundFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		ChunkPos chunk = ChunkPos.containing(context.origin());
		if (random.nextInt(2) == 0) {
			int y = 4 + random.nextInt(64);
			int x = chunk.getMinBlockX() + random.nextInt(16);
			int z = chunk.getMinBlockZ() + random.nextInt(16);
			BlockSparkle.Type type = BlockSparkle.Type.list[random.nextInt(BlockSparkle.Type.list.length)];
			BlockState sparkle = ChromaBlocks.sparkle(type).get().defaultBlockState();
			double r1 = 1 + random.nextDouble() * 5;
			double r2 = 1 + random.nextDouble() * 5;
			double r3 = 1 + random.nextDouble() * 5;
			for (int i = -(int)r1 - 1; i <= r1 + 1; i++) {
				for (int j = -(int)r2 - 1; j <= r2 + 1; j++) {
					for (int k = -(int)r3 - 1; k <= r3 + 1; k++) {
						if (ReikaMathLibrary.isPointInsideEllipse(i, j, k, r1, r2, r3)) {
							BlockPos pos = new BlockPos(x + i, y + j, z + k);
							if (world.getBlockState(pos).is(type.getBlockProxy()))
								world.setBlock(pos, sparkle, 2);
						}
					}
				}
			}
		}
		BlockState light = ChromaBlocks.ETHEREAL_LIGHT.get().defaultBlockState();
		int n = 8 + random.nextInt(24);
		for (int i = 0; i < n; i++) {
			int y = 4 + random.nextInt(64);
			int x = chunk.getMinBlockX() + random.nextInt(16);
			int z = chunk.getMinBlockZ() + random.nextInt(16);
			BlockPos pos = new BlockPos(x, y, z);
			if (world.getBlockState(pos).isAir())
				world.setBlock(pos, light, 2);
		}
		return true;
	}
}
