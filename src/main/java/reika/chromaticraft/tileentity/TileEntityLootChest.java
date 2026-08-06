package reika.chromaticraft.tileentity;

import java.util.UUID;

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
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import reika.chromaticraft.registry.ChromaBlockEntities;

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
public class TileEntityLootChest extends RandomizableContainerBlockEntity {

	private static final int SIZE = 54;

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	private UUID placer;
	private boolean opened;

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
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int from, int to) {}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu
					&& menu.getContainer() == TileEntityLootChest.this;
		}
	};

	public TileEntityLootChest(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LOOT_CHEST.get(), pos, state);
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

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output))
			ContainerHelper.saveAllItems(output, items);
		output.putBoolean("opened", opened);
		output.putDouble("reach", maxReachAccess);
		if (placer != null)
			output.putString("placer", placer.toString());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input))
			ContainerHelper.loadAllItems(input, items);
		opened = input.getBooleanOr("opened", false);
		maxReachAccess = input.getDoubleOr("reach", 8);
		placer = input.getString("placer").map(UUID::fromString).orElse(null);
	}
}
