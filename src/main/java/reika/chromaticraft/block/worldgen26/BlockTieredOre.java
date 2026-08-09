package reika.chromaticraft.block.worldgen26;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.nbt.CompoundTag;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.StatisticalRandom;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/**
 * V33a {@code BlockTieredOre} on top of DragonAPI's {@code BlockTieredResource}: an ore that is
 * disguised as the stone it generated in until the miner has reached its {@link ProgressStage}.
 *
 * <p>The disguise is total, not cosmetic. An insufficient player sees the host block's model (see
 * {@code TieredOreModel}), and mining it yields the host block's own drops — with Silk Touch, the
 * host block itself — so the ore is indistinguishable from ordinary stone until its stage is
 * reached. Only a sufficient player gets the real resources plus V33a's 2-6 experience splash.
 *
 * <p>Because these blocks never drop themselves, the loot table is empty and every drop is decided
 * here; that mirrors V33a, where {@code BlockTieredResource} hard-overrides the whole vanilla drop
 * path to nothing and re-implements harvesting in {@code removedByPlayer}.
 */
public class BlockTieredOre extends Block {

	private final ProgressStage stage;
	private final Block host;
	private final HarvestDrops drops;

	/** V33a getHarvestResources: the count formula is per-ore, so each identity supplies its own. */
	@FunctionalInterface
	public interface HarvestDrops {
		void collect(List<ItemStack> into, int fortune, RandomSource random, Player player);
	}

	/**
	 * V33a STONES: min(4, 1 + fortune/2) Elemental Stones, drawn from a {@link StatisticalRandom}
	 * persisted per player under {@code elementalstones}. That is not a plain uniform roll — it
	 * biases toward the colours that player has seen least, so mining eventually completes the set
	 * instead of leaving someone permanently short one colour.
	 */
	public static final HarvestDrops ELEMENTAL_STONE_DROPS = (into, fortune, random, player) -> {
		int n = Math.min(4, 1 + fortune / 2);
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(player);
		CompoundTag tag = NBTCompat.getCompound(root, "elementalstones");
		root.put("elementalstones", tag);
		StatisticalRandom<CrystalElement> roller = new StatisticalRandom<>(CrystalElement.class);
		roller.load(tag);
		for (int i = 0; i < n; i++) {
			CrystalElement element = roller.roll();
			into.add(new ItemStack(ChromaItems.ELEMENTAL_STONES.get(element).get()));
		}
		roller.saveAdditional(tag);
	};

	public BlockTieredOre(BlockBehaviour.Properties properties, ProgressStage stage, Block host, HarvestDrops drops) {
		super(properties);
		this.stage = stage;
		this.host = host;
		this.drops = drops;
	}

	public ProgressStage getProgressStage() {
		return stage;
	}

	/** The block this ore generated in, and therefore what an insufficient player sees and mines. */
	public Block getDisguise() {
		return host;
	}

	public boolean isPlayerSufficientTier(Player player) {
		return player != null && (player.isCreative() || stage.isPlayerAtStage(player));
	}

	/**
	 * V33a BlockTieredResource.removedByPlayer: the drop set is chosen by tier before the block is
	 * removed, and a sufficient harvest additionally splits 2-6 experience over the broken block.
	 */
	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
			ItemStack toolStack, boolean willHarvest, FluidState fluid) {
		boolean sufficient = this.isPlayerSufficientTier(player);
		List<ItemStack> harvested = new ArrayList<>();
		if (!player.isCreative()) {
			int fortune = enchantmentLevel(level, toolStack, Enchantments.FORTUNE);
			if (sufficient)
				drops.collect(harvested, fortune, level.getRandom(), player);
			else
				harvested.addAll(this.disguisedDrops(level, pos, player, toolStack));
		}
		boolean removed = super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
		if (removed && !harvested.isEmpty()) {
			for (ItemStack stack : harvested)
				popResource(level, pos, stack);
		}
		if (removed && sufficient && !player.isCreative() && level instanceof ServerLevel server)
			popExperience(server, pos, 2 + level.getRandom().nextInt(5));
		return removed;
	}

	/**
	 * V33a getNoHarvestResources: exactly what the host block would have dropped, so a player
	 * without the stage cannot tell the ore apart from the stone around it. Silk Touch yields the
	 * host block itself rather than its broken form, as it would on the real stone.
	 */
	private List<ItemStack> disguisedDrops(Level level, BlockPos pos, Player player, ItemStack toolStack) {
		if (enchantmentLevel(level, toolStack, Enchantments.SILK_TOUCH) > 0)
			return List.of(new ItemStack(host));
		if (!(level instanceof ServerLevel server))
			return List.of();
		LootParams.Builder params = new LootParams.Builder(server)
				.withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
				.withParameter(LootContextParams.TOOL, toolStack)
				.withOptionalParameter(LootContextParams.THIS_ENTITY, player);
		return host.defaultBlockState().getDrops(params);
	}

	private static int enchantmentLevel(Level level, ItemStack tool, ResourceKey<Enchantment> key) {
		if (tool.isEmpty()) return 0;
		return EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), tool);
	}

	/**
	 * V33a BlockTieredResource.onBlockPlacedBy: placing an ore you cannot see removes it with the
	 * host block's break effects, so the disguise cannot be defeated by placing one from creative
	 * and watching what happens.
	 */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && !this.isPlayerSufficientTier(player)) {
			level.levelEvent(2001, pos, Block.getId(state));
			level.removeBlock(pos, false);
		}
	}
}
