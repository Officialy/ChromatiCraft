package reika.chromaticraft.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/** Java's 48-bit random stream, saved between post-population retry batches. */
public final class SavedWorldgenRandom extends LegacyRandomSource {
    private long state;
    private boolean hasGaussian;
    private double gaussian;

    public SavedWorldgenRandom(long seed) { super(seed); }

    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
        state = (seed ^ 25214903917L) & 281474976710655L;
        hasGaussian = false;
    }

    @Override
    public int next(int bits) {
        state = (state * 25214903917L + 11L) & 281474976710655L;
        return (int)(state >>> (48 - bits));
    }

    @Override
    public double nextDouble() {
        // BitRandomSource's float multiplication loses precision; V33a used Java's 53-bit draw.
        return (((long)next(26) << 27) + next(27)) * 0x1.0p-53;
    }

    @Override
    public double nextGaussian() {
        if (hasGaussian) {
            hasGaussian = false;
            return gaussian;
        }
        double x, y, distance;
        do {
            x = 2 * nextDouble() - 1;
            y = 2 * nextDouble() - 1;
            distance = x * x + y * y;
        } while (distance >= 1 || distance == 0);
        double factor = StrictMath.sqrt(-2 * StrictMath.log(distance) / distance);
        gaussian = y * factor;
        hasGaussian = true;
        return x * factor;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("State", state);
        tag.putBoolean("HasGaussian", hasGaussian);
        tag.putDouble("Gaussian", gaussian);
        return tag;
    }

    public static SavedWorldgenRandom load(CompoundTag tag) {
        SavedWorldgenRandom random = new SavedWorldgenRandom(0);
        random.state = tag.getLongOr("State", 0);
        random.hasGaussian = tag.getBooleanOr("HasGaussian", false);
        random.gaussian = tag.getDoubleOr("Gaussian", 0);
        return random;
    }
}
