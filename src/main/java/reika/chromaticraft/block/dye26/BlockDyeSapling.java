package reika.chromaticraft.block.dye26;

import net.minecraft.util.random.WeightedList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code BlockDyeSapling}, one registered block per former metadata colour.
 *
 * <p>Growth is upstream's, not vanilla's. A dye sapling has no intermediate stage: the random tick that
 * would advance a vanilla sapling's stage grows the tree outright (V33a overrides
 * {@code func_149879_c} to grow directly), provided {@link #canGrowAt} allows it with light. Bone meal
 * always works where the tree fits, ignoring light, and is used up either way; upstream did this in
 * {@code onBlockActivated} with white dye, which in 1.7.10 was bone meal. The tree itself is the
 * {@code dye_tree_<colour>} configured feature, a {@code DyeTreeFeature} forced past the site checks,
 * with a 0.25 vine chance and no fertile vines.
 */
public final class BlockDyeSapling extends SaplingBlock {
    private final CrystalElement element;

    public BlockDyeSapling(BlockBehaviour.Properties properties, CrystalElement element) {
        super(new TreeGrower("chromaticraft_dye_" + element.name().toLowerCase(java.util.Locale.ROOT),
                WeightedList.of(ChromaWorldGenProvider.dyeTree(element)), WeightedList.of(),
                WeightedList.of(), ChromaWorldGenProvider.dyeTree(element)), properties);
        this.element = element;
    }

    /** V33a {@code func_149879_c}: no stage, grow as soon as the tick says so. */
    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (canGrowAt(level, pos, false))
            this.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    /** V33a grew on every bone meal, not 45% of them. */
    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (canGrowAt(level, pos, true))
            this.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
    }

    /**
     * V33a {@code canGrowAt}: dirt or grass below, the spot itself not a liquid, five clear non-liquid
     * blocks above, and (unless ignored) a light level of at least 9.
     */
    public static boolean canGrowAt(LevelReader level, BlockPos pos, boolean ignoreLight) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(Blocks.DIRT) && !below.is(Blocks.GRASS_BLOCK) && !below.is(Blocks.COARSE_DIRT)
                && !below.is(Blocks.PODZOL))
            return false;
        if (level.getBlockState(pos).liquid())
            return false;
        for (int i = 1; i < 6; i++) {
            BlockPos up = pos.above(i);
            if (!ReikaWorldHelper.softBlocks(level, up) || level.getBlockState(up).liquid())
                return false;
        }
        return ignoreLight || level.getMaxLocalRawBrightness(pos) >= 9;
    }

    public CrystalElement getElement() {
        return element;
    }

    /** V33a {@code colorMultiplier}: the dye colour brightened, as the leaves are. */
    public int getTintColor() {
        return 0xFF000000 | reika.dragonapi.libraries.registry.ReikaDyeHelper.dyes[element.ordinal()]
                .getJavaColor().brighter().getRGB();
    }
}
