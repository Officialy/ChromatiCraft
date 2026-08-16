package reika.chromaticraft.world;

import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.dragonapi.instantiable.data.maps.ThresholdMapping;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code LavaRiverGenerator}: the rivers of lava that wind across the Nether roof.
 *
 * <p>Three simplex fields decide everything, all seeded off the world seed the way upstream seeds
 * them — the placement field from the seed, the height field from its negation, the fluid field from
 * its complement — so a given world lays its rivers down the same way it always did. The placement
 * field is sampled per column at a scale of 32 blocks and taken as an absolute value, which turns the
 * noise's zero crossings into the ribbons the rivers run along: within {@code 0.1} of a crossing is the
 * channel, a single course of Stone Shielding with the fluid resting on top of it; out to {@code 0.2}
 * is the bank, three courses of Shielding and no fluid. The height field is sampled eight times
 * broader and mapped across y 127 to 240, so a river climbs and falls over long distances rather than
 * per column.
 *
 * <p>Every column this writes belongs to the chunk being generated, so unlike the roof structures
 * there is no question of reaching outside the region a feature may write to.
 */
public final class NetherLavaRiverFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a RIVER_THRESH: how far from a noise zero crossing still counts as river at all. */
	private static final double RIVER_THRESH = 0.2;
	/** V33a RIVER_CENTER_THRESH: within this, the column is channel rather than bank. */
	private static final double RIVER_CENTER_THRESH = 0.1;
	/** V33a MIN_HEIGHT/MAX_HEIGHT: the band of the roof a river may sit in. */
	private static final double MIN_HEIGHT = 127;
	private static final double MAX_HEIGHT = 240;
	/** V33a divides world coordinates by 32 for placement, and by a further 8 for height. */
	private static final double PLACEMENT_SCALE = 32;
	private static final double HEIGHT_SCALE = 8;

	/**
	 * V33a's weighted fluid table. Upstream looks each entry up by its 1.7.10 fluid name and simply
	 * skips the ones no installed mod provides, so the table is written out in full and resolved
	 * against whatever is actually present — a pack with Thermal gets Pyrotheum at its proper weight,
	 * one without gets lava, and neither needs this code to change.
	 *
	 * <p>Two of upstream's entries are absent rather than merely unresolved: {@code poison} at weight
	 * 10 and {@code fluiddeath} at weight 1 belong to mods whose modern fluid ids are not known here,
	 * and inventing an id would silently point the table at the wrong fluid or at nothing while looking
	 * correct. They are recorded here so they can be restored once identified.
	 */
	private static final List<FluidEntry> FLUIDS = List.of(
			new FluidEntry(80, "minecraft:lava"),
			new FluidEntry(20, "thermal:pyrotheum"),
			new FluidEntry(40, "ic2:pahoehoe_lava"),
			new FluidEntry(5, "tconstruct:molten_iron"),
			new FluidEntry(3, "tconstruct:molten_gold"),
			new FluidEntry(2, "tconstruct:molten_ardite"),
			new FluidEntry(2, "tconstruct:molten_cobalt"),
			new FluidEntry(4, "tconstruct:molten_obsidian"));

	private record FluidEntry(double weight, String fluid) {}

	/** Rebuilt whenever the world seed changes, exactly as V33a rebuilds its generator. */
	private record Fields(long seed, SimplexNoiseGenerator placement, SimplexNoiseGenerator height,
			SimplexNoiseGenerator fluid, ThresholdMapping<Block> fluids) {}

	/**
	 * Volatile because worldgen runs {@link #place} on a worker pool, concurrently for different
	 * chunks. Two threads racing to build the same fields is harmless, but publishing the reference
	 * without a happens-before is not: a reader could see a non-null {@code fields} while the
	 * threshold table's backing map was still only half visible to it. The table is complete before
	 * the assignment, so safe publication is all this needs. V33a had no such hazard; 1.7.10 worldgen
	 * was single-threaded.
	 */
	private volatile Fields fields;

	public NetherLavaRiverFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		if (world.getLevel().dimension() != Level.NETHER)
			return false;
		Fields active = fieldsFor(world.getSeed());
		BlockState shielding = ChromaBlocks.shielding(ChromaShieldTypes.STONE).get().defaultBlockState();
		ChunkPos chunk = ChunkPos.containing(context.origin());
		boolean placed = false;
		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				int x = chunk.getMinBlockX() + i;
				int z = chunk.getMinBlockZ() + k;
				Column column = classify(active, x, z);
				if (column == null)
					continue;
				int y = column.y();
				if (column.channel()) {
					// The channel: one course of Shielding carrying the fluid.
					world.setBlock(new BlockPos(x, y, z), shielding, Block.UPDATE_CLIENTS);
					world.setBlock(new BlockPos(x, y + 1, z),
							liquid(active, x / PLACEMENT_SCALE, z / PLACEMENT_SCALE),
							Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
				else {
					// The bank: three courses, standing proud of the channel beside it.
					for (int j = 0; j < 3; j++)
						world.setBlock(new BlockPos(x, y + j, z), shielding, Block.UPDATE_CLIENTS);
				}
				placed = true;
			}
		}
		return placed;
	}

	/** What a single column is: the channel a river runs down, or the bank standing beside it. */
	public record Column(boolean channel, int y) {}

	/**
	 * The whole of V33a's per-column decision, separated from the writing so it can be checked without
	 * a Nether to write into.
	 *
	 * @return null where there is no river at all
	 */
	public Column classify(long seed, int x, int z) {
		return classify(fieldsFor(seed), x, z);
	}

	private static Column classify(Fields active, int x, int z) {
		double rx = x / PLACEMENT_SCALE;
		double rz = z / PLACEMENT_SCALE;
		double value = Math.abs(active.placement().getValue(rx, rz));
		if (value > RIVER_THRESH)
			return null;
		int y = (int)ReikaMathLibrary.normalizeToBounds(
				active.height().getValue(rx / HEIGHT_SCALE, rz / HEIGHT_SCALE), MIN_HEIGHT, MAX_HEIGHT);
		return new Column(value < RIVER_CENTER_THRESH, y);
	}

	private BlockState liquid(Fields active, double rx, double rz) {
		double value = ReikaMathLibrary.normalizeToBounds(active.fluid().getValue(rx, rz),
				0, active.fluids().lastValue());
		Block block = active.fluids().getForValue(value, true);
		return block == null ? Blocks.LAVA.defaultBlockState() : block.defaultBlockState();
	}

	private Fields fieldsFor(long seed) {
		Fields active = fields;
		if (active != null && active.seed() == seed)
			return active;
		ThresholdMapping<Block> table = new ThresholdMapping<>();
		for (FluidEntry entry : FLUIDS) {
			Block block = fluidBlock(entry.fluid());
			if (block != null)
				table.addMapping(entry.weight() + table.lastValue(), block);
		}
		// V33a seeds the three fields from the world seed, its negation and its complement.
		active = new Fields(seed, new SimplexNoiseGenerator(seed), new SimplexNoiseGenerator(-seed),
				new SimplexNoiseGenerator(~seed), table);
		fields = active;
		return active;
	}

	/** V33a's {@code addFluid(weight, name)}: an absent mod simply leaves its entry out of the table. */
	private static Block fluidBlock(String id) {
		Identifier identifier = Identifier.tryParse(id.toLowerCase(Locale.ROOT));
		if (identifier == null)
			return null;
		return BuiltInRegistries.FLUID.getOptional(identifier)
				.map(fluid -> fluid.defaultFluidState().createLegacyBlock().getBlock())
				.filter(block -> block != Blocks.AIR)
				.orElse(null);
	}
}
