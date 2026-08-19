package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.world.dimension.biome.ProximaBiomes;

/**
 * V33a {@code WorldGenMiniAltar}: the Precursors' roadside shrine, and the buried chest under it.
 *
 * <p>A seven-by-seven shielded platform with a lit centre, four cobble pillars at its corners rising
 * four blocks to a glass canopy, a lamp of a random element on top of that, and a five-by-five burrow
 * dug three deep beneath the platform with a Loot Chest at its floor. Nothing about it is random
 * except the lamp's colour, the chest's facing and what the chest holds — the geometry is fixed, which
 * is what makes an altar recognisable as one from a distance.
 *
 * <p>It refuses unless the whole seven-by-seven is grass with air above it, so it only ever appears on
 * open flat ground; upstream's chance is a low 0.025 per chunk on top of that.
 *
 * <h2>The chest's contents</h2>
 *
 * <p>Upstream fills the chest at generation time from {@code ItemMagicRegistry} — four to twenty-seven
 * items drawn at random from every item that has an elemental value, each in a stack size inversely
 * proportional to that value ({@code min(16, max(1, 24/tag.getMaximumValue()))}) — plus one item
 * chosen by biome: a Multi Crystal in the Glowing Forest and an Iridescent Crystal Shard on the
 * Crystal Plains. That registry is a 548-line table of Reika's own item-to-element values and is not
 * ported yet, so the weighting it drives cannot be reproduced here.
 *
 * <p>The biome item is here and the pool is not. It is placed at generation time, which is upstream's
 * own mechanism — this chest never went through {@code ChestGenHooks}, so the port's loot-table
 * convention for Burrow caches and village chests does not describe it, and a loot table could not
 * express the biome choice anyway: Proxima's biomes are created by the same datagen run that would have
 * to name them, so neither a direct holder nor a tag resolves there. When the registry lands, its pool
 * joins the biome item on this same generation-time path and the altar matches V33a exactly.
 *
 * <p>V33a's {@code ISLANDS} and {@code SKYLANDS} cases return null — commented out there as "maybe
 * generates in oceans?" and "currently cannot generate here" — so the Glowing Forest and the Crystal
 * Plains are the whole of it.
 */
public final class MiniAltarFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a {@code r}: the platform reaches three blocks from centre, so seven by seven. */
	private static final int PLATFORM_RADIUS = 3;
	/** V33a {@code t}: the canopy sits four blocks above the platform. */
	private static final int ARCH_HEIGHT = 4;
	/** V33a {@code yc}: the chest floor is three blocks below the platform. */
	private static final int BURROW_DEPTH = 3;

	public MiniAltarFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource rand = context.random();
		// V33a's first statement is `y--`: the placement hands it the first air block and the altar is
		// built on the ground under it.
		BlockPos origin = context.origin().below();
		if (!canGenerate(world, origin))
			return false;

		BlockState stone = shield(ChromaShieldTypes.STONE);
		BlockState light = shield(ChromaShieldTypes.LIGHT);
		for (int i = -PLATFORM_RADIUS; i <= PLATFORM_RADIUS; i++)
			for (int k = -PLATFORM_RADIUS; k <= PLATFORM_RADIUS; k++)
				world.setBlock(origin.offset(i, 0, k), i == 0 && k == 0 ? light : stone, 3);

		generateArches(world, origin, rand);
		generateBurrow(world, origin);
		generateChest(world, origin, rand);
		// V33a returns false even on success -- nothing reads it, since its decorator calls each
		// generator once per chosen chunk regardless. A modern Feature's answer is read, so this
		// reports what actually happened.
		return true;
	}

	/** V33a generateArches: four pillars, a rim at both ends of them, a glass canopy, and the lamp. */
	private static void generateArches(WorldGenLevel world, BlockPos origin, RandomSource rand) {
		BlockState cobble = shield(ChromaShieldTypes.COBBLE);
		BlockState glass = shield(ChromaShieldTypes.GLASS);
		BlockState light = shield(ChromaShieldTypes.LIGHT);

		for (int h = 1; h <= ARCH_HEIGHT; h++) {
			world.setBlock(origin.offset(2, h, 2), cobble, 3);
			world.setBlock(origin.offset(-2, h, 2), cobble, 3);
			world.setBlock(origin.offset(2, h, -2), cobble, 3);
			world.setBlock(origin.offset(-2, h, -2), cobble, 3);
		}

		// The rim runs between the pillars at both the canopy and the platform, so the altar reads as
		// four arches rather than four posts.
		for (int n = -1; n <= 1; n++) {
			world.setBlock(origin.offset(-2, ARCH_HEIGHT, n), cobble, 3);
			world.setBlock(origin.offset(2, ARCH_HEIGHT, n), cobble, 3);
			world.setBlock(origin.offset(n, ARCH_HEIGHT, -2), cobble, 3);
			world.setBlock(origin.offset(n, ARCH_HEIGHT, 2), cobble, 3);

			world.setBlock(origin.offset(-2, 0, n), cobble, 3);
			world.setBlock(origin.offset(2, 0, n), cobble, 3);
			world.setBlock(origin.offset(n, 0, -2), cobble, 3);
			world.setBlock(origin.offset(n, 0, 2), cobble, 3);
		}

		for (int a = -1; a <= 1; a++)
			for (int b = -1; b <= 1; b++)
				world.setBlock(origin.offset(a, ARCH_HEIGHT, b), glass, 3);
		world.setBlock(origin.offset(0, ARCH_HEIGHT, 0), light, 3);

		CrystalElement element = CrystalElement.elements[rand.nextInt(CrystalElement.elements.length)];
		world.setBlock(origin.offset(0, ARCH_HEIGHT + 1, 0),
				ChromaBlocks.crystalLamp(element).get().defaultBlockState(), 3);
	}

	/**
	 * V33a generateBurrow: a five-by-five shaft dug {@code yc+1} deep, walled in Stone Shielding on its
	 * sides and floor and hollow inside. The extra layer past {@code yc} is the floor, which is why the
	 * loop runs one deeper than the chest.
	 */
	private static void generateBurrow(WorldGenLevel world, BlockPos origin) {
		BlockState stone = shield(ChromaShieldTypes.STONE);
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int d = 1; d <= BURROW_DEPTH + 1; d++)
			for (int a = -2; a <= 2; a++)
				for (int b = -2; b <= 2; b++) {
					boolean wall = Math.abs(a) == 2 || Math.abs(b) == 2 || d == BURROW_DEPTH + 1;
					world.setBlock(origin.offset(a, -d, b), wall ? stone : air, 3);
				}
	}

	private static void generateChest(WorldGenLevel world, BlockPos origin, RandomSource rand) {
		BlockPos at = origin.below(BURROW_DEPTH);
		// V33a's `rand.nextInt(4)` metadata is the chest's facing.
		world.setBlock(at, ChromaBlocks.LOOT_CHEST.get().defaultBlockState()
				.setValue(BlockLootChest.FACING, Direction.from2DDataValue(rand.nextInt(4))), 3);
		if (!(world.getBlockEntity(at) instanceof TileEntityLootChest chest))
			return;
		ItemStack biomeItem = biomeItem(world, at);
		if (!biomeItem.isEmpty())
			chest.setItem(rand.nextInt(chest.getContainerSize()), biomeItem);
	}

	/** V33a getRandomBiomeItem: one guaranteed item chosen by the biome the altar stands in. */
	private static ItemStack biomeItem(WorldGenLevel world, BlockPos pos) {
		Holder<Biome> biome = world.getBiome(pos);
		if (biome.is(ProximaBiomes.FOREST.biomeKey()))
			return new ItemStack(ChromaItems.CLUSTERS.get(ChromaClusterItems.MULTI_CRYSTAL).get());
		if (biome.is(ProximaBiomes.PLAINS.biomeKey()))
			return new ItemStack(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get());
		return ItemStack.EMPTY;
	}

	/**
	 * V33a canGenerate: every cell of the platform must be grass with air directly above it. Upstream's
	 * loop is the asymmetric {@code -r..3}, which for {@code r == 3} is the same seven-by-seven the
	 * platform covers.
	 */
	private static boolean canGenerate(WorldGenLevel world, BlockPos origin) {
		for (int i = -PLATFORM_RADIUS; i <= PLATFORM_RADIUS; i++)
			for (int k = -PLATFORM_RADIUS; k <= PLATFORM_RADIUS; k++) {
				BlockPos at = origin.offset(i, 0, k);
				if (!world.getBlockState(at).is(Blocks.GRASS_BLOCK)
						|| !world.getBlockState(at.above()).isAir())
					return false;
			}
		return true;
	}

	private static BlockState shield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState();
	}
}
