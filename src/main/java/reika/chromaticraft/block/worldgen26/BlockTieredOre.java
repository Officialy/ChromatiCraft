package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.StatisticalRandom;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

import reika.dragonapi.base.BlockTieredResource;
import net.minecraft.world.level.BlockGetter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * V33a {@code BlockTieredOre} on top of DragonAPI's {@code BlockTieredResource}: an ore that is
 * disguised as the stone it generated in until the miner has reached its {@link ProgressStage}.
 *
 * <p>The disguise is total, not cosmetic. An insufficient player sees the host block's model (see
 * {@code TieredOreModel}), and mining it yields the host block's own drops — with Silk Touch, the
 * host block itself — so the ore is indistinguishable from ordinary stone until its stage is
 * reached. Only a sufficient player gets the real resources plus V33a's 2-6 experience splash.
 *
 * <p>Because these blocks never drop themselves, generated loot tables select DragonAPI's
 * tier-aware dynamic drop. V33a's {@code BlockTieredResource} overrides the vanilla drop
 * path to nothing and re-implements harvesting in {@code removedByPlayer}. The modern shared
 * removal hook evaluates the table before removal and dispatches NeoForge's drop event once.
 */
public class BlockTieredOre extends BlockTieredResource {

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

	@Override
	public boolean isPlayerSufficientTier(BlockGetter level, BlockPos pos, Player player) {
		return this.isPlayerSufficientTier(player);
	}

	@Override
	public Collection<ItemStack> getHarvestResources(Level level, BlockPos pos, int fortune, Player player) {
		List<ItemStack> harvested = new ArrayList<>();
		drops.collect(harvested, fortune, level.getRandom(), player);
		return harvested;
	}

	@Override
	public Collection<ItemStack> getNoHarvestResources(Level level, BlockPos pos, int fortune, Player player) {
		ItemStack tool = player != null ? player.getMainHandItem().copy() : ItemStack.EMPTY;
		// The legacy helper takes an explicit Fortune level, independently of the held tool.
		if (tool.isEmpty() && fortune > 0)
			tool = new ItemStack(Items.STICK);
		if (!tool.isEmpty()) {
			var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
			EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(enchantment, fortune));
		}
		return this.disguisedDrops(level, pos, player, tool);
	}

	@Override
	protected Collection<ItemStack> getNoHarvestResources(Level level, BlockPos pos, ItemInstance tool, Player player) {
		return this.disguisedDrops(level, pos, player, tool);
	}

	/**
	 * V33a getNoHarvestResources: exactly what the host block would have dropped, so a player
	 * without the stage cannot tell the ore apart from the stone around it. Silk Touch yields the
	 * host block itself rather than its broken form, as it would on the real stone.
	 */
	private List<ItemStack> disguisedDrops(Level level, BlockPos pos, Player player, ItemInstance toolStack) {
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

}
