package reika.chromaticraft.block.worldgen26;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a {@code BlockTieredPlant}: a plant that does not exist for a player who has not reached its
 * {@link reika.chromaticraft.magic.progression.ProgressStage}.
 *
 * <p>The gate is total rather than cosmetic, exactly as upstream. V33a's renderer returns false for
 * an insufficient player so nothing is drawn, and its {@code getSelectedBoundingBoxFromPool} returns
 * a zero box so the plant cannot even be looked at. {@link #getShape} reproduces the second half
 * against the viewing entity, which is the modern equivalent and — unlike V33a's client-only
 * override — also keeps the plant untargetable on a dedicated server.
 *
 * <p>Each plant is its own registered identity; see {@link ChromaTieredPlants}. Support rules come
 * from V33a {@code canPlaceBlockOn}/{@code isValidLocation}: the surface, water and sand plants
 * stand on the block below, while the cave and leaf plants hang from the block above. Glowing
 * Roots use their own dirt-or-grass support rule beside the base of a tree.
 */
public class BlockTieredPlant extends Block {

	/** V33a resets every non-pod plant's selected bounds to the complete block. Collision stays empty. */
	private static final VoxelShape SHAPE = Shapes.block();

	private final ChromaTieredPlants plant;
	private final MapCodec<BlockTieredPlant> codec = MapCodec.unit(this);

	public BlockTieredPlant(BlockBehaviour.Properties properties, ChromaTieredPlants plant) {
		super(properties);
		this.plant = plant;
	}

	@Override
	public MapCodec<? extends BlockTieredPlant> codec() {
		return codec;
	}

	public ChromaTieredPlants getPlant() {
		return plant;
	}

	public boolean isPlayerSufficientTier(Player player) {
		// Creative is the modern inspection/building path. Treating a creative owner as insufficient
		// made every tiered plant visible in inventory yet impossible to target after placement, which
		// also prevented mapmakers from removing one. Survival players retain the exact progression
		// gate and invisible/untargetable behavior.
		return player != null && (player.isCreative() || plant.stage().isPlayerAtStage(player));
	}

	/**
	 * V33a getSelectedBoundingBoxFromPool: an insufficient player gets a zero box, so the plant is
	 * not merely invisible but impossible to target. Anything that is not a player — pathfinding,
	 * light, block updates — sees the ordinary shape.
	 */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		if (context instanceof EntityCollisionContext entity
				&& entity.getEntity() instanceof Player player
				&& !this.isPlayerSufficientTier(player))
			return Shapes.empty();
		if (plant.siting() == ChromaTieredPlants.Siting.TREE_POD)
			return podShape(level, pos);
		return SHAPE;
	}

	/** V33a's neighbour-sensitive Vibrant Pod selection bounds. */
	private static VoxelShape podShape(BlockGetter level, BlockPos pos) {
		double minX = isLog(level, pos.west()) ? 0 : 4;
		double maxX = isLog(level, pos.east()) ? 16 : 12;
		double minY = isLog(level, pos.below()) ? 0 : 4;
		double maxY = isLog(level, pos.above()) ? 16 : 12;
		double minZ = isLog(level, pos.north()) ? 0 : 4;
		double maxZ = isLog(level, pos.south()) ? 16 : 12;
		return box(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static boolean isLog(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).is(BlockTags.LOGS);
	}

	/** V33a getCollisionBoundingBoxFromPool returns null: these never obstruct movement. */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}

	/** V33a isValidLocation: which neighbour has to support this particular plant. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return switch (plant.siting()) {
			case CAVE, LEAVES -> this.canPlaceOn(level, pos.above());
			case TREE_POD -> this.canPlaceOn(level, pos.above())
					|| this.canPlaceOn(level, pos.north()) || this.canPlaceOn(level, pos.south())
					|| this.canPlaceOn(level, pos.west()) || this.canPlaceOn(level, pos.east());
			default -> this.canPlaceOn(level, pos.below());
		};
	}

	/** V33a canPlaceBlockOn, per plant. */
	private boolean canPlaceOn(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return switch (plant.siting()) {
			case SURFACE -> state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
					|| state.is(Blocks.FARMLAND);
			case LEAVES -> state.is(BlockTags.LEAVES);
			case CAVE -> state.is(Blocks.STONE) || state.is(Blocks.BEDROCK)
					|| state.is(Tags.Blocks.ORES)
					|| state.getBlock() instanceof BlockStructureShield && isProxima(level);
			case WATER -> state.getFluidState().is(Fluids.WATER)
					|| state.getFluidState().is(Fluids.FLOWING_WATER);
			case SAND -> state.is(Blocks.SAND);
			case TREE_POD -> state.is(BlockTags.LOGS);
			case TREE_ROOT -> state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
		};
	}

	/** V33a's structure-shield exception applies only inside the ChromatiCraft dimension. */
	private static boolean isProxima(LevelReader level) {
		if (level instanceof Level concrete)
			return concrete.dimension() == ChromaDimensions.PROXIMA;
		if (level instanceof ServerLevelAccessor accessor)
			return accessor.getLevel().dimension() == ChromaDimensions.PROXIMA;
		return false;
	}

	/** V33a randomDisplayTick: the five particle-bearing plants emit every other client tick. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if ((level.getGameTime() & 1) == 0)
			ChromaParticle.spawnTieredPlant(level, pos, plant, random);
	}

	/** Vanilla plant idiom, and V33a's checkAndDropBlock: an unsupported plant is removed. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
			RandomSource random) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	/**
	 * V33a getHarvestResources, reached through BlockTieredResource.removedByPlayer. There is no
	 * insufficient-player branch here as there is for the tiered ores: an insufficient player cannot
	 * target the plant at all, so the only way to break one is to already have the stage. The loot
	 * table is therefore empty and every drop is decided here.
	 */
	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
			ItemStack toolStack, boolean willHarvest, FluidState fluid) {
		List<ItemStack> harvested = new ArrayList<>();
		if (!player.isCreative() && this.isPlayerSufficientTier(player)) {
			// V33a maxes the tool's Fortune against the player's Looting before switching.
			int fortune = Math.max(enchantmentLevel(level, toolStack, Enchantments.FORTUNE),
					enchantmentLevel(level, toolStack, Enchantments.LOOTING));
			plant.collectDrops(harvested, fortune, level.getRandom(), player,
					new ItemStack(ChromaItems.TIERED.get(plant.drop()).get()));
		}
		boolean removed = super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
		if (removed)
			for (ItemStack stack : harvested)
				popResource(level, pos, stack);
		return removed;
	}

	private static int enchantmentLevel(Level level, ItemStack tool, ResourceKey<Enchantment> key) {
		if (tool.isEmpty()) return 0;
		return EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), tool);
	}
}
