package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import reika.chromaticraft.block.dye26.BlockDyeFlower;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code RainbowForestDecorator.generateDyeFlowers}: the Rainbow Forest's carpet of dye flowers.
 *
 * <p>Each chunk makes 8, 16, ... or 64 attempts (eight times one to eight). Each picks a column, stands
 * on its top solid block, and plants a flower of a random colour where the spot is soft, not liquid, and
 * on dirt or grass. Below y 128 it also has to pass a roll that thins flowers out the lower the ground
 * is, {@code rand(1 + (128-y)/16) > 0}: nearly every attempt succeeds in the hills, four in five at sea
 * level. At 128 and above the roll's range is 1 and it always fails, so upstream grew no dye flowers
 * on high ground; that is kept.
 */
public final class DyeFlowerPatchFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<DyeFlowerPatchFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(DyeFlowerPatchFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<DyeFlowerPatchFeature> codec() {
        return CODEC;
    }

	public DyeFlowerPatchFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		ChunkPos chunk = ChunkPos.containing(origin);
		int num = 8 * (1 + random.nextInt(8));
		boolean any = false;
		for (int i = 0; i < num; i++) {
			int x = chunk.getMinBlockX() + random.nextInt(16);
			int z = chunk.getMinBlockZ() + random.nextInt(16);
			int y = ReikaWorldHelper.getTopSolidOrLiquidBlock(world, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (!canGenerateFlower(world, pos))
				continue;
			if (y < 128 && random.nextInt(1 + (128 - y) / 16) > 0) {
				BlockState flower = ChromaBlocks.dyeFlower(CrystalElement.elements[random.nextInt(16)]).get().defaultBlockState();
				if (flower.canSurvive(world, pos)) {
					world.setBlock(pos, flower, 2);
					any = true;
				}
			}
		}
		return any;
	}

	/** V33a {@code canGenerateFlower}: not a liquid, dirt or grass below, and soft. */
	private static boolean canGenerateFlower(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.liquid() || !state.getFluidState().isEmpty())
			return false;
		return BlockDyeFlower.isPlainsSoil(world.getBlockState(pos.below())) && ReikaWorldHelper.softBlocks(world, pos);
	}
}
