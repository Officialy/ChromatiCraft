package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.event.CrystalGenEvent;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaBlocks;

/**
 * Modern feature form of V33a's {@code CrystalGenerator}. One invocation owns the original sixty
 * per-chunk attempts (scaled by dimension/biome density), so placement modifiers intentionally do
 * not add a second count or height distribution.
 */
public final class CrystalFeature extends Feature<NoneFeatureConfiguration> {

	private static final int PER_CHUNK = 60;
	private static final TagKey<Biome> RAINBOW_FOREST = TagKey.create(Registries.BIOME,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "is_rainbow_forest"));

	public CrystalFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		if (context.chunkGenerator() instanceof FlatLevelSource)
			return false;
		WorldGenLevel world = context.level();
		if (world.getLevel().dimension() == Level.END)
			return false;

		RandomSource random = context.random();
		BlockPos origin = context.origin();
		int attempts = (int)(PER_CHUNK * getDensityFactor(world, origin));
		int maximumY = world.getLevel().dimension() == Level.NETHER ? 128 : 64;
		boolean placedAny = false;
		for (int i = 0; i < attempts; i++) {
			BlockPos pos = new BlockPos(origin.getX() + random.nextInt(16),
					4 + random.nextInt(maximumY - 4), origin.getZ() + random.nextInt(16));
			int color = random.nextInt(16);
			if (placeCrystal(world, pos, color)) {
				placedAny = true;
				NeoForge.EVENT_BUS.post(new CrystalGenEvent(world, pos, random, color));
			}
		}
		return placedAny;
	}

	/** Deterministic placement seam used by worldgen and the focused GameTest. */
	public static boolean placeCrystal(LevelAccessor world, BlockPos pos, int color) {
		if (!canGenerateAt(world, pos))
			return false;
		BlockState crystal = ChromaBlocks.caveCrystal(CrystalElement.elements[Math.floorMod(color, 16)]).get().defaultBlockState();
		return world.setBlock(pos, crystal, 2);
	}

	public static boolean canGenerateAt(LevelAccessor world, BlockPos pos) {
		BlockState target = world.getBlockState(pos);
		if (!(target.isAir() || target.canBeReplaced()) || !target.getFluidState().isEmpty())
			return false;
		if (!canGenerateOn(world.getBlockState(pos.below())))
			return false;
		for (Direction direction : Direction.values()) {
			if (world.getBlockState(pos.relative(direction)).isAir())
				return true;
		}
		return false;
	}

	public static boolean canGenerateOn(BlockState state) {
		return state.is(Blocks.STONE)
				|| state.is(Blocks.DIRT)
				|| state.is(Blocks.GRAVEL)
				|| state.is(BlockTags.PLANKS)
				|| state.is(Blocks.BEDROCK)
				|| state.is(Blocks.OBSIDIAN)
				|| state.is(BlockTags.STONE_BRICKS)
				|| state.is(Blocks.INFESTED_STONE)
				|| state.is(Blocks.COBBLESTONE)
				|| state.is(Blocks.MOSSY_COBBLESTONE)
				|| state.is(Blocks.NETHERRACK)
				|| state.is(BlockTags.BASE_STONE_OVERWORLD)
				|| state.is(BlockTags.BASE_STONE_NETHER)
				|| state.is(BlockTags.STONE_ORE_REPLACEABLES)
				|| state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
				|| isOre(state);
	}

	private static boolean isOre(BlockState state) {
		return state.is(BlockItemTags.COAL_ORES.block()) || state.is(BlockTags.IRON_ORES)
				|| state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES)
				|| state.is(BlockItemTags.REDSTONE_ORES.block()) || state.is(BlockItemTags.EMERALD_ORES.block())
				|| state.is(BlockItemTags.LAPIS_ORES.block()) || state.is(BlockItemTags.DIAMOND_ORES.block());
	}

	public static float getDensityFactor(WorldGenLevel world, BlockPos pos) {
		if (world.getLevel().dimension() == Level.END)
			return 0;
		if (world.getLevel().dimension() == Level.NETHER)
			return 0.25F;
		Holder<Biome> biome = world.getBiome(pos);
		if (biome.is(RAINBOW_FOREST))
			return 1.5F;
		if (biome.is(Biomes.MUSHROOM_FIELDS))
			return 1.375F;
		if (biome.is(BiomeTags.IS_OCEAN))
			return 1.25F;
		if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL))
			return 1.125F;
		return 1F;
	}
}
