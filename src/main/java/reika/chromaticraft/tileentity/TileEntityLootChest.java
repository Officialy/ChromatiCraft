package reika.chromaticraft.tileentity;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * V33a {@code BlockLootChest.TileEntityLootChest}: a 54-slot chest that remembers who may open it.
 *
 * <p>Two things separate it from a vanilla chest. It records the player who placed or first opened it
 * and can be restricted to them, and it carries a set of progression stages that are granted the
 * first time somebody legitimately gets it open — the chest is a progression trigger as much as it is
 * loot. It also tracks whether it has ever been opened, which is what stops a structure re-rolling
 * its contents.
 *
 * <p>Loot itself is a data-driven loot table on the modern side rather than V33a's
 * {@code ChestGenHooks} population, so {@link RandomizableContainerBlockEntity} supplies the
 * unpack-on-first-open behaviour that {@code populateChest} did by hand.
 */
public class TileEntityLootChest extends RandomizableContainerBlockEntity implements LidBlockEntity {

	private static final int SIZE = 54;

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	private UUID placer;
	private boolean opened;
	/** Modern replacement for V33a's metadata bit 8 structure-puzzle lock. */
	private boolean structureLocked;
	private final Set<ProgressStage> progressTriggers = EnumSet.noneOf(ProgressStage.class);
	private final ChestLidController chestLidController = new ChestLidController();

	/** V33a maxReachAccess: some structures widen how far away the chest stays usable. */
	private double maxReachAccess = 8;

	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHEST_OPEN,
					net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.25F);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHEST_CLOSE,
					net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.25F);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int from, int to) {
			level.blockEvent(pos, state.getBlock(), 1, to);
			if (from == to)
				return;
			// V33a openInventory/closeInventory follow the count change with causeAdjacentUpdates, so
			// the trap circuit re-reads the chest's power. Without this the signal from
			// BlockLootChest.ownSignal is correct but nothing ever asks for it again, and the TNT
			// beside or beneath the chest never fires. pos.below() is the strong-power case, matching
			// getDirectSignal's UP-only rule.
			Block block = state.getBlock();
			level.updateNeighborsAt(pos, block, null);
			level.updateNeighborsAt(pos.below(), block, null);
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu
					&& menu.getContainer() == TileEntityLootChest.this;
		}
	};

	public TileEntityLootChest(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LOOT_CHEST.get(), pos, state);
	}

	/** V33a's Burrow cache collates equal drops, then keeps blocks and items in separate halves. */
	@Override
	public void unpackLootTable(Player player) {
		boolean burrowCache = reika.chromaticraft.world.OverworldStructureFeature.BURROW_CACHE_LOOT
				.equals(this.getLootTable());
		super.unpackLootTable(player);
		if (!burrowCache) return;
		java.util.ArrayList<ItemStack> collated = new java.util.ArrayList<>();
		for (ItemStack source : items) {
			if (source.isEmpty()) continue;
			ItemStack remaining = source.copy();
			for (ItemStack existing : collated) {
				if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;
				int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
				existing.grow(moved);
				remaining.shrink(moved);
				if (remaining.isEmpty()) break;
			}
			if (!remaining.isEmpty()) collated.add(remaining);
		}
		collated.sort(java.util.Comparator.comparing(stack ->
				net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		int blockSlot = 0;
		int itemSlot = 27;
		for (ItemStack stack : collated) {
			boolean block = net.minecraft.world.level.block.Block.byItem(stack.getItem())
					!= net.minecraft.world.level.block.Blocks.AIR;
			int slot = block ? blockSlot++ : itemSlot++;
			if (slot < SIZE) items.set(slot, stack);
		}
		setChanged();
	}

	public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, TileEntityLootChest chest) {
		chest.chestLidController.tickLid();
	}

	@Override
	public boolean triggerEvent(int id, int value) {
		if (id == 1) {
			chestLidController.shouldBeOpen(value > 0);
			return true;
		}
		return super.triggerEvent(id, value);
	}

	@Override
	public float getOpenNess(float partialTick) {
		return chestLidController.getOpenness(partialTick);
	}

	public void recheckOpen() {
		if (!this.isRemoved())
			openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> list) {
		items = list;
	}

	@Override
	protected Component getDefaultName() {
		return Component.literal("Loot Chest");
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.sixRows(id, inventory, this);
	}

	/** V33a isOwnedBy. */
	public boolean isOwnedBy(Player player) {
		return placer != null && player.getUUID().equals(placer);
	}

	/** V33a isAccessibleBy: an unclaimed chest is open to anyone. */
	public boolean isAccessibleBy(Player player) {
		return placer == null || this.isOwnedBy(player);
	}

	public void setPlacer(UUID id) {
		placer = id;
		this.setChanged();
	}

	public boolean wasOpened() {
		return opened;
	}

	public void markOpened() {
		opened = true;
		this.setChanged();
	}

	public boolean isStructureLocked() {
		return structureLocked;
	}

	public void setStructureLocked(boolean locked) {
		if (structureLocked != locked) {
			structureLocked = locked;
			setChanged();
		}
	}

	/** Adds one of V33a's persistent progression rewards to this generated chest. */
	public void addProgress(ProgressStage stage) {
		if (stage != null && progressTriggers.add(stage))
			this.setChanged();
	}

	/**
	 * V33a grants every attached trigger whenever a player legitimately accesses the chest. The
	 * progression manager is idempotent, so retaining the triggers also preserves cooperative and
	 * reloadable-stage behavior instead of making the first opener consume them.
	 */
	public void grantProgress(Player player) {
		for (ProgressStage stage : progressTriggers)
			stage.giveToPlayer(player, true);
	}

	public void setMaxReach(double max) {
		maxReachAccess = max;
		this.setChanged();
	}

	/** V33a isUseableByPlayer, with the structure-supplied reach rather than vanilla's fixed 8. */
	@Override
	public boolean stillValid(Player player) {
		return Container_stillValid(this, player, maxReachAccess);
	}

	private static boolean Container_stillValid(BlockEntity be, Player player, double reach) {
		Level level = be.getLevel();
		BlockPos pos = be.getBlockPos();
		if (level == null || level.getBlockEntity(pos) != be)
			return false;
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
				<= reach * reach;
	}

	@Override
	public void startOpen(net.minecraft.world.entity.ContainerUser user) {
		if (!this.isRemoved() && !user.getLivingEntity().isSpectator())
			openersCounter.incrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(),
					this.getBlockState(), user.getContainerInteractionRange());
	}

	@Override
	public void stopOpen(net.minecraft.world.entity.ContainerUser user) {
		if (!this.isRemoved() && !user.getLivingEntity().isSpectator())
			openersCounter.decrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(),
					this.getBlockState());
	}

	@Override
	public java.util.List<net.minecraft.world.entity.ContainerUser> getEntitiesWithContainerOpen() {
		return openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
	}

	/** V33a {@code numPlayersUsing > 0}: what arms the chest's redstone output. */
	public boolean isOpenedByAnyone() {
		return openersCounter.getOpenerCount() > 0;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output))
			ContainerHelper.saveAllItems(output, items);
		output.putBoolean("opened", opened);
		output.putBoolean("structureLocked", structureLocked);
		output.putDouble("reach", maxReachAccess);
		if (placer != null)
			output.putString("placer", placer.toString());
		ValueOutput.TypedOutputList<String> triggers = output.list("triggers", Codec.STRING);
		for (ProgressStage stage : progressTriggers)
			triggers.add(stage.name());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input))
			ContainerHelper.loadAllItems(input, items);
		opened = input.getBooleanOr("opened", false);
		structureLocked = input.getBooleanOr("structureLocked", false);
		maxReachAccess = input.getDoubleOr("reach", 8);
		placer = input.getString("placer").map(UUID::fromString).orElse(null);
		progressTriggers.clear();
		for (String name : input.listOrEmpty("triggers", Codec.STRING)) {
			try {
				progressTriggers.add(ProgressStage.valueOf(name));
			}
			catch (IllegalArgumentException ignored) {
				// A removed or renamed stage must not make an old world unloadable.
			}
		}
	}
}
