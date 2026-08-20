package reika.chromaticraft.block.dimension;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a's cracked bedrock: the only thing in Proxima that gives up Proximal Essence, and so the only way
 * to tune a Portal Rift at all.
 *
 * <p>A crack has a depth from zero to nine, which is upstream's metadata and is here an integer
 * property. Everything about the block reads from it. A deeper crack is <b>softer</b> — the hardness
 * curve is {@code (1 - (9-depth)/9 * 0.5)} of the base twelve, so a fresh crack is twice the work of a
 * fully opened one — it <b>drops more</b>, and at depth nine it also gives up the pure form. And
 * breaking one does not destroy it: it comes back one depth shallower, so a crack is mined down rather
 * than mined out, and only reverts to plain bedrock when a depth-zero crack is finally taken.
 *
 * <p>The drop count is scaled by the breaking player's dimension tuning, which is what makes tuning
 * compound: essence tunes the rift, the rift tunes the player, the player takes more essence.
 */
public class BlockBedrockCrack extends Block {

	public static final MapCodec<BlockBedrockCrack> CODEC = simpleCodec(BlockBedrockCrack::new);

	/** V33a metadata 0-9: how far the crack has been opened. */
	public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 0, 9);

	public BlockBedrockCrack(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(DEPTH, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DEPTH);
	}

	/**
	 * V33a getBlockHardness: the base hardness scaled by {@code 1-(9-meta)/9*0.5}, so a depth-nine crack
	 * is half the work of a fresh one.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		float scale = 1F - (9 - state.getValue(DEPTH)) / 9F * 0.5F;
		// Destroy progress is the reciprocal of hardness, so a softer block is a larger number.
		return super.getDestroyProgress(state, player, level, pos) / scale;
	}

	/**
	 * V33a getBlockReplacedWith/getMetaReplacedWith: a harvested crack of any depth above zero comes
	 * back one shallower, and only a depth-zero crack reverts to plain bedrock. The crack is a seam that
	 * is worked, not a block that is removed.
	 */
	public static BlockState afterHarvest(BlockState state) {
		int depth = state.getValue(DEPTH);
		return depth > 0 ? state.setValue(DEPTH, depth - 1) : Blocks.BEDROCK.defaultBlockState();
	}

	/**
	 * V33a getBlockReplacedWith in practice: a harvested crack leaves a shallower crack behind rather
	 * than air. Done here rather than through a loot table because the block that survives is the point
	 * -- a crack is a seam you work down, and vanilla's destroy path would take the whole thing.
	 */
	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
			ItemStack tool, boolean willHarvest,
			net.minecraft.world.level.material.FluidState fluid) {
		if (level.isClientSide())
			return level.setBlock(pos, fluid.createLegacyBlock(), 11);
		level.levelEvent(2001, pos, Block.getId(state));
		return level.setBlock(pos, willHarvest ? afterHarvest(state) : Blocks.BEDROCK.defaultBlockState(),
				3);
	}

	/** The essence a crack gives up, read through the loot context so fortune and the breaker apply. */
	@Override
	protected List<ItemStack> getDrops(BlockState state,
			net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		net.minecraft.world.entity.Entity breaker = params.getOptionalParameter(
				net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
		net.minecraft.world.item.ItemInstance tool = params.getOptionalParameter(
				net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
		int fortune = 0;
		if (tool != null && tool.count() > 0) {
			var lookup = params.getLevel().registryAccess()
					.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
			fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
					lookup.getOrThrow(Enchantments.FORTUNE), tool);
		}
		return drops(state, params.getLevel(), breaker instanceof Player p ? p : null, fortune);
	}

	/**
	 * V33a getDrops: one to {@code 1+depth+fortune} Proximal Essence, scaled again by the breaker's
	 * dimension tuning, and at full depth up to three of the pure form as well.
	 */
	public static List<ItemStack> drops(BlockState state, Level level, Player player, int fortune) {
		int depth = state.getValue(DEPTH);
		List<ItemStack> drops = new ArrayList<>();
		int n = 1 + Math.min(depth, level.getRandom().nextInt(1 + depth + fortune));
		if (player != null)
			n = DimensionTuningManager.instance.getTunedDropCount(player, n, 1, Integer.MAX_VALUE);
		for (int i = 0; i < n; i++)
			drops.add(new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.PROXIMAL_ESSENCE).get()));
		if (depth == 9) {
			// Upstream reuses `n` here, so the pure count starts from the essence count and is then
			// clamped to three -- a detail worth keeping, since it means a well-tuned player gets the
			// cap and an untuned one does not.
			int pure = player != null
					? DimensionTuningManager.instance.getTunedDropCount(player, n, 1, 3) : 1;
			for (int i = 0; i < pure; i++)
				drops.add(new ItemStack(
						ChromaItems.TIERED.get(ChromaTieredItems.PURE_PROXIMAL_ESSENCE).get()));
		}
		return drops;
	}
}
