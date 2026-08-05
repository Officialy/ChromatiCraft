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
import reika.chromaticraft.registry.ChromaTieredPlants;

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
 * stand on the block below, while the cave and leaf plants hang from the block above.
 */
public class BlockTieredPlant extends Block {

	/** V33a drawCrossedSquares: a vanilla-style cross, so the selection box is the plant's own bounds. */
	private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

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
		return player != null && plant.stage().isPlayerAtStage(player);
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
		return SHAPE;
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
			// CHROMA-PORT: V33a also accepts ChromaBlocks.STRUCTSHIELD inside the ChromatiCraft
			// pocket dimension. Both the block and the dimension are still pristine 1.7.10, so that
			// branch cannot be expressed yet; it is not a normal-world case.
			case CAVE -> state.is(Blocks.STONE) || state.is(Blocks.BEDROCK)
					|| state.is(Tags.Blocks.ORES);
			case WATER -> state.getFluidState().is(Fluids.WATER)
					|| state.getFluidState().is(Fluids.FLOWING_WATER);
			case SAND -> state.is(Blocks.SAND);
		};
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
