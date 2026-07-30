package reika.chromaticraft.block.crystal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.registry.CrystalElement;

/** One property-free cave-crystal block identity for a single element. */
public final class BlockCaveCrystal extends CrystalBlock {

    private static final VoxelShape[][][][] SHAPES = createShapes();

    private final CrystalElement element;

    public BlockCaveCrystal(BlockBehaviour.Properties properties, CrystalElement element) {
        super(properties);
        this.element = element;
    }

    /** Matches the two discarded booleans and four-bit arm selector in V33a CrystalRenderer. */
    public static int armMask(BlockPos pos) {
        long seed = (pos.getX() * 3129871L) ^ (pos.getZ() * 116129781L) ^ pos.getY();
        RandomSource random = RandomSource.create(seed);
        random.nextBoolean();
        random.nextBoolean();
        return random.nextInt(16);
    }

    public static boolean isCeilingMounted(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockPos above = pos.above();
        return !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean flipped = isCeilingMounted(level, pos);
        boolean above = level.getBlockState(pos.above()).getBlock() instanceof BlockCaveCrystal;
        boolean below = level.getBlockState(pos.below()).getBlock() instanceof CrystalBlock;
        return SHAPES[flipped ? 1 : 0][above ? 1 : 0][below ? 1 : 0][armMask(pos)];
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }

    private static VoxelShape[][][][] createShapes() {
        VoxelShape[][][][] shapes = new VoxelShape[2][2][2][16];
        for (int flipped = 0; flipped < 2; flipped++) {
            for (int above = 0; above < 2; above++) {
                for (int below = 0; below < 2; below++) {
                    for (int mask = 0; mask < 16; mask++) {
                        boolean flip = flipped != 0;
                        VoxelShape shape = centralSpike(flip, above != 0);
                        if ((mask & 8) != 0) shape = Shapes.or(shape, xArm(flip, true, below != 0));
                        if ((mask & 4) != 0) shape = Shapes.or(shape, xArm(flip, false, below != 0));
                        if ((mask & 2) != 0) shape = Shapes.or(shape, zArm(flip, true, below != 0));
                        if ((mask & 1) != 0) shape = Shapes.or(shape, zArm(flip, false, below != 0));
                        shapes[flipped][above][below][mask] = shape.optimize();
                    }
                }
            }
        }
        return shapes;
    }

    private static VoxelShape centralSpike(boolean flip, boolean above) {
        VoxelShape body = box(flip, 5.6, 0, 5.6, 10.4, above ? 16 : 12.8, 10.4);
        if (above) return body;
        return Shapes.or(body,
                box(flip, 6.4, 12.8, 6.4, 9.6, 14.4, 9.6),
                box(flip, 7.2, 14.4, 7.2, 8.8, 16, 8.8));
    }

    private static VoxelShape xArm(boolean flip, boolean positive, boolean below) {
        double shift = below ? 2.4 : 0;
        double x0 = positive ? 9.6 : 4;
        double x1 = positive ? 12 : 6.4;
        double mx0 = positive ? 10 : 1;
        double mx1 = positive ? 15 : 6;
        double tx0 = positive ? 13 : -0.8;
        double tx1 = positive ? 16.8 : 3;
        return Shapes.or(
                box(flip, x0, 0 + shift, 6.08, x1, 5 + shift, 9.92),
                box(flip, mx0, 3 + shift, 6.08, mx1, 10.5 + shift, 9.92),
                box(flip, tx0, 8 + shift, 6.08, tx1, 12.8 + shift, 9.92));
    }

    private static VoxelShape zArm(boolean flip, boolean positive, boolean below) {
        double shift = below ? 1.6 : 0;
        double z0 = positive ? 9.6 : 4;
        double z1 = positive ? 12 : 6.4;
        double mz0 = positive ? 10 : 1;
        double mz1 = positive ? 15 : 6;
        double tz0 = positive ? 13 : -0.8;
        double tz1 = positive ? 16.8 : 3;
        return Shapes.or(
                box(flip, 6.08, 0 + shift, z0, 9.92, 5 + shift, z1),
                box(flip, 6.08, 3 + shift, mz0, 9.92, 10.5 + shift, mz1),
                box(flip, 6.08, 8 + shift, tz0, 9.92, 12.8 + shift, tz1));
    }

    private static VoxelShape box(boolean flip, double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return flip
                ? BlockCaveCrystal.box(minX, 16-maxY, minZ, maxX, 16-minY, maxZ)
                : BlockCaveCrystal.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override public CrystalElement getCrystalElement(BlockState state) { return element; }
    public CrystalElement getCrystalElement() { return element; }
    @Override public boolean shouldMakeNoise() { return true; }
    @Override public boolean shouldGiveEffects(CrystalElement element) { return true; }
    @Override public boolean performEffect(CrystalElement element) {
        return element == CrystalElement.BROWN || element == CrystalElement.BLUE ? rand.nextInt(4) == 0 : true;
    }
    @Override public int getRange() { return 4; }
    @Override public int getDuration(CrystalElement element) { return element == CrystalElement.BROWN ? 5 : 200; }
    @Override public int getPotionLevel(CrystalElement element) { return 0; }
    @Override public boolean renderBase() { return false; }
}