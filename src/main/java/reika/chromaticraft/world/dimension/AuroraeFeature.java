package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.entity.AuroraData;
import reika.chromaticraft.entity.EntityAurora;
import reika.chromaticraft.registry.ChromaEntityTypes;

/**
 * V33a {@code WorldGenAurorae}: the ribbons of light hanging over Proxima's Skylands.
 *
 * <p>One placement makes a whole display, not one ribbon: between one and twelve are laid out parallel
 * to each other along a random bearing, spaced eight to thirty-two blocks apart and sixty to a hundred
 * and eighty long. Every ribbon in a display shares the same two colours, so a display reads as one
 * thing rather than a dozen unrelated streaks.
 *
 * <p>Heights are the part that repays attention. Each end is put forty blocks above whatever terrain is
 * under it, or at a hundred and twenty to two hundred and twenty, whichever is higher — so a display
 * clears mountains but never sinks into a valley. Then both ends are pulled to the higher of the two and
 * jittered five blocks either way, which is what stops a ribbon looking tilted while still keeping it
 * from being perfectly level.
 *
 * <h2>The colour pairs</h2>
 *
 * <p>Colours are drawn from a weighted table — red, green and blue at 100 down to pink at 10 — and the
 * second is redrawn while the pair is one of three upstream forbids: Argon with Pink, Apple with Pink,
 * and Green with Pink. Those three are the pairs whose gradients muddy into each other, and dropping the
 * rule would be invisible in code review and obvious in the sky.
 */
public final class AuroraeFeature extends Feature<NoneFeatureConfiguration> {

	/**
	 * V33a's weighted aurora palette. The weights matter as much as the colours: the three primaries
	 * dominate, and pink is a tenth as likely as red, so a pink display is a rarity.
	 */
	private enum AuroraColour {
		RED(0xFF0000, 100),
		GREEN(0x00FF00, 100),
		BLUE(0x0000FF, 100),
		WHITE(0xFFFFFF, 60),
		YELLOW(0xFFFF00, 40),
		CYAN(0x00FFFF, 40),
		MAGENTA(0xFF00FF, 40),
		ARGON(0x50BEFF, 30),
		ORANGE(0xFF8C00, 20),
		APPLE(0x9BFF00, 20),
		PURPLE(0x8930FF, 20),
		PINK(0xFF97AE, 10);

		static final AuroraColour[] list = values();

		final int colour;
		final int weight;

		AuroraColour(int colour, int weight) {
			this.colour = colour;
			this.weight = weight;
		}
	}

	/** V33a's three disallowed pairs, tested unordered. */
	private static boolean isForbiddenPair(AuroraColour a, AuroraColour b) {
		return pair(a, b, AuroraColour.ARGON, AuroraColour.PINK)
				|| pair(a, b, AuroraColour.APPLE, AuroraColour.PINK)
				|| pair(a, b, AuroraColour.GREEN, AuroraColour.PINK);
	}

	private static boolean pair(AuroraColour a, AuroraColour b, AuroraColour x, AuroraColour y) {
		return (a == x && b == y) || (a == y && b == x);
	}

	public AuroraeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		int ribbons = 1 + random.nextInt(12);
		double bearing = Math.toRadians(random.nextDouble() * 360);
		double separation = 8 + random.nextDouble() * 24;
		double length = 60 + random.nextDouble() * 120;
		// The ribbons are laid out along the perpendicular, so they run parallel rather than fanning.
		double offsetX = separation * Math.cos(bearing + Math.toRadians(90));
		double offsetZ = separation * Math.sin(bearing + Math.toRadians(90));

		AuroraColour first = drawColour(random);
		AuroraColour second = drawColour(random);
		// V33a redraws only the second colour, so the forbidden pairs bias away from pink rather than
		// away from whatever it was first paired with.
		while (isForbiddenPair(first, second))
			second = drawColour(random);

		boolean placed = false;
		for (int i = 0; i < ribbons; i++) {
			double lateral = i - ribbons / 2D;
			double x1 = origin.getX() + 0.5 + length / 2 * Math.cos(bearing) + offsetX * lateral;
			double z1 = origin.getZ() + 0.5 + length / 2 * Math.sin(bearing) + offsetZ * lateral;
			double x2 = origin.getX() + 0.5 - length / 2 * Math.cos(bearing) + offsetX * lateral;
			double z2 = origin.getZ() + 0.5 - length / 2 * Math.sin(bearing) + offsetZ * lateral;

			// Forty blocks clear of the terrain beneath each end, or high enough regardless.
			double y1 = Math.max(surfaceAt(world, x1, z1) + 40, 120 + random.nextDouble() * 100);
			double y2 = Math.max(surfaceAt(world, x2, z2) + 40, 120 + random.nextDouble() * 100);
			// Both ends then take the higher of the two, so a ribbon hangs level rather than sloped,
			// with five blocks of jitter left either way.
			double top = Math.max(y1, y2);
			y1 = top + random.nextDouble() * 10 - 5;
			y2 = top + random.nextDouble() * 10 - 5;

			double speed = 0.125 + random.nextDouble() * 2.375;
			EntityAurora aurora = ChromaEntityTypes.AURORA.get().create(world.getLevel(),
					net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
			if (aurora == null)
				continue;
			aurora.setAuroraData(new AuroraData(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2),
					first.colour, second.colour, speed));
			world.addFreshEntity(aurora);
			placed = true;
		}
		return placed;
	}

	/**
	 * The terrain height under one end. Read through the heightmap rather than by probing blocks: an end
	 * can be ninety blocks from the origin, and a block read that far out could ask for a chunk that is
	 * not there yet.
	 */
	private static int surfaceAt(WorldGenLevel world, double x, double z) {
		return world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
	}

	private static AuroraColour drawColour(RandomSource random) {
		int total = 0;
		for (AuroraColour colour : AuroraColour.list)
			total += colour.weight;
		int roll = random.nextInt(total);
		for (AuroraColour colour : AuroraColour.list) {
			roll -= colour.weight;
			if (roll < 0)
				return colour;
		}
		return AuroraColour.RED;
	}
}
