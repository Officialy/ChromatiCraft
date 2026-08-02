package reika.chromaticraft.tileentity.recipe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.interfaces.blockentity.InertIInv;

/** V33a casting stand: one inert slot, owner access, locking, table linking, and spread-fill. */
public class TileEntityItemStand extends TileEntityChromaticBase
		implements WorldlyContainer, OwnedTile, NBTTile, InertIInv {

	private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
	private BlockPos table;
	private boolean locked;
	private static final Map<UUID, Set<TileEntityItemStand>> SPREAD_SET = new HashMap<>();

	public TileEntityItemStand(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ITEM_STAND.get(), pos, state);
	}

	public boolean interact(Player player, InteractionHand hand) {
		if (locked || !this.isOwnedByPlayer(player)) return false;
		ItemStack held = player.getItemInHand(hand);
		Set<TileEntityItemStand> spread = SPREAD_SET.remove(player.getUUID());
		if (spread != null && !spread.isEmpty() && !held.isEmpty()) {
			spreadItems(spread, held, player);
			return true;
		}
		ItemStack present = inventory.getFirst();
		if (present.isEmpty()) {
			if (held.isEmpty()) return true;
			inventory.set(0, held.copyWithCount(1));
			if (!player.isCreative()) held.shrink(1);
		}
		else if (!held.isEmpty() && ItemStack.isSameItemSameComponents(present, held)) {
			int moved = Math.min(held.getCount(), present.getMaxStackSize() - present.getCount());
			present.grow(moved);
			if (!player.isCreative()) held.shrink(moved);
		}
		else {
			this.giveOrDrop(player, present.copy());
			inventory.set(0, ItemStack.EMPTY);
			if (!held.isEmpty()) {
				inventory.set(0, held.copyWithCount(1));
				if (!player.isCreative()) held.shrink(1);
			}
		}
		this.inventoryChanged();
		ChromaSounds.ITEMSTAND.playSoundAtBlock(this);
		return true;
	}

	private void giveOrDrop(Player player, ItemStack stack) {
		if (!player.addItem(stack) && this.getLevel() != null)
			net.minecraft.world.level.block.Block.popResource(this.getLevel(), this.getBlockPos().above(), stack);
	}

	public void queueSpread(Player player) {
		if (inventory.getFirst().isEmpty())
			SPREAD_SET.computeIfAbsent(player.getUUID(), key -> new HashSet<>()).add(this);
	}

	/** Drops and clears the stand's complete stack, as used by the table's V33a mass-empty action. */
	public void dropSlot() {
		ItemStack present = inventory.getFirst();
		if (present.isEmpty() || this.getLevel() == null) return;
		net.minecraft.world.level.block.Block.popResource(this.getLevel(), this.getBlockPos().above(), present.copy());
		inventory.set(0, ItemStack.EMPTY);
		this.inventoryChanged();
	}

	private static void spreadItems(Set<TileEntityItemStand> locations, ItemStack held, Player player) {
		java.util.ArrayList<TileEntityItemStand> stands = new java.util.ArrayList<>();
		for (TileEntityItemStand stand : locations) {
			if (!stand.isRemoved() && stand.getLevel() == player.level() && stand.isOwnedByPlayer(player) && !stand.locked)
				stands.add(stand);
		}
		if (stands.isEmpty()) return;
		int total = held.getCount();
		for (TileEntityItemStand stand : stands) total += stand.getItem(0).getCount();
		int each = Math.min(held.getMaxStackSize(), total / stands.size());
		int placed = 0;
		for (TileEntityItemStand stand : stands) {
			stand.setItem(0, held.copyWithCount(each));
			placed += each;
		}
		if (!player.isCreative()) held.setCount(Math.max(0, total - placed));
	}

	private void inventoryChanged() {
		this.setChanged();
		if (this.getLevel() != null && table != null) {
			BlockEntity blockEntity = this.getLevel().getBlockEntity(table);
			if (blockEntity != null) blockEntity.setChanged();
		}
		if (this.getLevel() != null && !this.getLevel().isClientSide())
			this.syncAllData(true);
	}

	public void setTable(BlockPos table) { this.table = table != null ? table.immutable() : null; this.setChanged(); }
	public BlockPos getTable() { return table; }
	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!world.isClientSide() && table != null && world.hasChunkAt(table) && world.getBlockEntity(table) == null) {
			table = null;
			this.setChanged();
		}
	}

	public void lock(boolean lock) { locked = lock; this.setChanged(); }
	public boolean isLocked() { return locked; }

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (world == null || !world.isClientSide() || inventory.getFirst().isEmpty()) return;
		RandomSource random = world.getRandom();
		if (random.nextBoolean()) {
			double x = pos.getX()+0.5+(random.nextDouble()-0.5)*0.75;
			double y = pos.getY()+0.5+(random.nextDouble()-0.5)*0.25;
			double z = pos.getZ()+0.5+(random.nextDouble()-0.5)*0.75;
			world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.01+random.nextDouble()*0.025, 0);
		}
		if (table != null && random.nextInt(32) == 0)
			world.addParticle(ParticleTypes.END_ROD, pos.getX()+0.5, pos.getY()+0.25, pos.getZ()+0.5, 0, 0.08, 0);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.STAND;
	}

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		inventory.set(0, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, inventory);
		table = input.read("table", BlockPos.CODEC).orElse(null);
		locked = input.getBooleanOr("locked", false);
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, inventory);
		if (table != null) output.store("table", BlockPos.CODEC, table);
		output.putBoolean("locked", locked);
	}

	@Override public int getContainerSize() { return 1; }
	@Override public boolean isEmpty() { return inventory.getFirst().isEmpty(); }
	@Override public ItemStack getItem(int slot) { return inventory.get(slot); }
	@Override public ItemStack removeItem(int slot, int amount) { ItemStack out = ContainerHelper.removeItem(inventory, slot, amount); if (!out.isEmpty()) inventoryChanged(); return out; }
	@Override public ItemStack removeItemNoUpdate(int slot) { ItemStack out = ContainerHelper.takeItem(inventory, slot); if (!out.isEmpty()) inventoryChanged(); return out; }
	@Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); stack.limitSize(getMaxStackSize()); inventoryChanged(); }
	@Override public boolean stillValid(Player player) { return this.isPlayerAccessible(player) && this.isOwnedByPlayer(player); }
	@Override public void clearContent() { inventory.set(0, ItemStack.EMPTY); inventoryChanged(); }
	/** Pushes the post-craft item change to every client tracking this chunk. */
	public void syncAfterCraft() {
		if (this.getLevel() != null && !this.getLevel().isClientSide()) this.syncAllData(true);
	}

	// V33a's BuildCraft IPipeConnection returned DISCONNECT. With no 26.2 BuildCraft target, the
	// same invariant is enforced natively: no sided slots are exposed and all automation is rejected.
	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

	@Override public boolean onlyAllowOwnersToMine() { return true; }
	@Override public boolean onlyAllowOwnersToUse() { return true; }
	@Override public boolean isOwnedByPlayer(Player player) { return placerUUID == null || placerUUID.equals(player.getUUID()); }
	@Override public void getTagsToWriteToStack(CompoundTag tag) {
		if (placer != null && !placer.isEmpty()) tag.putString("place", placer);
		if (placerUUID != null) tag.putString("placeUUID", placerUUID.toString());
	}
	@Override public void setDataFromItemStackTag(ItemStack stack) {
		CompoundTag tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) != null
				? stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
		placer = tag.getStringOr("place", "");
		String ownerId = tag.getStringOr("placeUUID", "");
		placerUUID = ownerId.isEmpty() ? null : UUID.fromString(ownerId);
	}
	@Override public void addTooltipInfo(java.util.List list, boolean shift) {
		if (placer != null && !placer.isEmpty()) list.add(Component.literal("Owner: "+placer));
		if (locked) list.add(Component.literal("Locked"));
	}
}
