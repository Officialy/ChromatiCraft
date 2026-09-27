/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.maps.CountMap;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.interfaces.blockentity.ConditionBreakDropsInventory;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code TileEntityMassStorage}: a crate holding any number of unstackable items. Its contents live in world data
 * keyed by the crate's id rather than in the tile (so a huge crate does not bloat its chunk); items enter through a
 * single pending slot (slot 0, which automation fills) and each stored item is its own further slot. Breaking it drops
 * everything.
 *
 * <p>CHROMA-PORT: V33a was also an Applied Energistics 2 grid node that pulled matching items out of the ME network
 * (and could be read through a storage bus). AE2 has no 26.2 port on the classpath; the grid node, ME reader and
 * cell scanning are left out and {@link #canPullFromME} is kept for when it lands.
 */
public abstract class TileEntityMassStorage extends TileEntityChromaticBase implements Container, BreakAction, ConditionBreakDropsInventory {

	private static final List<ItemStack> NO_ITEMS = Collections.emptyList();

	private final CountMap<KeyedItemStack> types = new CountMap<>();
	private ItemStack pendingInput = ItemStack.EMPTY;
	private UUID identifier = UUID.randomUUID();
	/** V33a read items still stored in the tile (from before the world data) into the crate; applied on first tick. */
	private List<ItemStack> legacyItems;

	private final StepTimer updateTimer = new StepTimer(200);

	private boolean broken;

	protected TileEntityMassStorage(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** V33a {@code WorldToolCrateData}: every crate's contents, in the overworld's data (1.7.10's shared map storage). */
	public static final class ToolCrateData extends SavedData {

		private static final String IDENTIFIER = "tool_crate_items";
		private static final Codec<ToolCrateData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.CODEC.listOf())
				.xmap(ToolCrateData::new, d -> d.items);
		private static final SavedDataType<ToolCrateData> TYPE = new SavedDataType<>(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, IDENTIFIER),
				ToolCrateData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

		private final HashMap<UUID, List<ItemStack>> items = new HashMap<>();

		private ToolCrateData() {

		}

		private ToolCrateData(Map<UUID, List<ItemStack>> data) {
			for (Map.Entry<UUID, List<ItemStack>> e : data.entrySet())
				items.put(e.getKey(), new ArrayList<>(e.getValue()));
		}

		private static ToolCrateData get(Level level) {
			if (!(level instanceof ServerLevel server))
				return null;
			return server.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
		}
	}

	protected final List<ItemStack> getItems() {
		return this.getOrCreateItemList();
	}

	private List<ItemStack> getOrCreateItemList() {
		ToolCrateData data = level != null ? ToolCrateData.get(level) : null;
		if (data == null)
			return NO_ITEMS;
		return data.items.computeIfAbsent(identifier, k -> new ArrayList<>());
	}

	private void markDataDirty() {
		ToolCrateData data = level != null ? ToolCrateData.get(level) : null;
		if (data != null)
			data.setDirty();
	}

	@Override
	public final int getContainerSize() {
		return this.getItems().size()+1;
	}

	@Override
	public final ItemStack getItem(int slot) {
		List<ItemStack> li = this.getItems();
		if (slot > li.size() || slot < 0) {
			ChromatiCraft.LOGGER.error("Something tried pulling from an off-list slot #"+slot+"!");
			return ItemStack.EMPTY;
		}
		return slot == 0 ? pendingInput : li.get(slot-1);
	}

	@Override
	public final ItemStack removeItem(int slot, int amount) {
		if (slot == 0) {
			ItemStack ret = pendingInput.split(amount);
			this.setChanged();
			return ret;
		}
		List<ItemStack> li = this.getItems();
		if (slot < 1 || slot > li.size())
			return ItemStack.EMPTY;
		ItemStack is = li.get(slot-1);
		if (amount >= is.getCount()) {
			this.removeItemAt(slot-1);
			return is;
		}
		ItemStack ret = is.split(amount);
		types.subtract(this.key(ret), ret.getCount());
		this.markDataDirty();
		return ret;
	}

	@Override
	public final ItemStack removeItemNoUpdate(int slot) {
		ItemStack is = this.getItem(slot);
		return this.removeItem(slot, is.getCount());
	}

	/**
	 * Slot 0 takes the pending input; emptying a stored slot removes that item. V33a threw on putting an item into a
	 * stored slot, which a hopper does when it has to hand an item back; here it replaces the item.
	 */
	@Override
	public final void setItem(int slot, ItemStack is) {
		if (slot == 0) {
			pendingInput = is;
			this.setChanged();
		}
		else if (is.isEmpty()) {
			this.removeItemAt(slot-1);
		}
		else if (slot-1 < this.getItems().size()) {
			ItemStack old = this.getItems().set(slot-1, is);
			types.subtract(this.key(old), old.getCount());
			types.increment(this.key(is), is.getCount());
			this.markDataDirty();
		}
	}

	@Override
	public final int getMaxStackSize() {
		return 1;
	}

	@Override
	public final boolean stillValid(Player ep) {
		return this.isPlayerAccessible(ep);
	}

	@Override
	public final boolean canPlaceItem(int slot, ItemStack is) {
		return slot == 0 && (pendingInput.isEmpty() || ItemStack.isSameItemSameComponents(pendingInput, is)
				&& pendingInput.getCount()+is.getCount() <= is.getMaxStackSize())
				&& this.isItemValid(is) && this.getItems().size() < this.maxItemCount();
	}

	@Override
	public final void clearContent() {
		for (int i = this.getItems().size()-1; i >= 0; i--)
			this.removeItemAt(i);
		pendingInput = ItemStack.EMPTY;
	}

	/** The item the renderer floats above the crate (V33a {@code getFilterItemRender}); empty draws a question mark. */
	public abstract ItemStack getFilterItemRender();

	public abstract boolean isItemValid(ItemStack is);
	public abstract int maxItemCount();

	protected final ItemStack getPendingInput() {
		return pendingInput;
	}

	@Override
	public final void updateEntity(Level world, BlockPos pos) {
		if (!pendingInput.isEmpty() && !world.isClientSide()) {
			this.addItem(pendingInput);
			pendingInput = ItemStack.EMPTY;
		}

		this.onTick(world, pos);

		updateTimer.update();
		if (updateTimer.checkCap() && !world.isClientSide()) {
			this.buildCache();
		}

		// CHROMA-PORT: V33a ticked its ME system reader here and, with a network holding matching items, pulled one
		// random stack of them into the pending slot whenever the network had work.
	}

	protected void onTick(Level world, BlockPos pos) {

	}

	@Override
	protected final void onFirstTick(Level world, BlockPos pos) {
		if (world.isClientSide())
			return;
		if (legacyItems != null) {
			this.getItems().clear();
			types.clear();
			for (ItemStack is : legacyItems)
				this.addItem(is);
			legacyItems = null;
		}
		this.markDataDirty();
		this.initTypeData();
	}

	private void initTypeData() {
		types.clear();
		for (ItemStack is : this.getItems()) {
			KeyedItemStack ks = this.key(is);
			types.increment(ks, is.getCount());
			this.onAddItem(is);
		}
	}

	protected final void resetWorkTimer() {
		updateTimer.setTick(updateTimer.getCap()+2);
	}

	/** CHROMA-PORT: V33a rebuilt its list of matching ME network items here every 200 ticks. */
	private void buildCache() {

	}

	protected boolean canPullFromME() {
		return true;
	}

	private void addItem(ItemStack is) {
		if (level == null || level.isClientSide())
			return;
		this.getItems().add(is);
		this.markDataDirty();
		KeyedItemStack ks = this.key(is);
		types.increment(ks, is.getCount());
		this.onAddItem(is);
		this.syncAllData(false);
	}

	protected void onAddItem(ItemStack is) {

	}

	protected void onRemoveItem(ItemStack is) {

	}

	private void removeItemAt(int slot) {
		if (level == null || level.isClientSide())
			return;
		List<ItemStack> li = this.getItems();
		if (slot < 0 || slot >= li.size())
			return;
		ItemStack is = li.remove(slot);
		this.markDataDirty();
		KeyedItemStack ks = this.key(is);
		types.subtract(ks, is.getCount());
		this.onRemoveItem(is);
		this.syncAllData(false);
	}

	@Override
	public final boolean isEmpty() {
		return this.getItems().isEmpty();
	}

	protected abstract KeyedItemStack key(ItemStack is);

	/** How many of each kind of item it holds (V33a showed this in its WAILA tooltip; no tooltip mod is ported yet). */
	public final Map<KeyedItemStack, Integer> getItemTypes() {
		HashMap<KeyedItemStack, Integer> ret = new HashMap<>();
		for (KeyedItemStack ks : types.keySet())
			ret.put(ks, types.get(ks));
		return Collections.unmodifiableMap(ret);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!pendingInput.isEmpty())
			output.store("pending", ItemStack.CODEC, pendingInput);
		output.store("boxid", UUIDUtil.STRING_CODEC, identifier);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		input.read("boxid", UUIDUtil.STRING_CODEC).ifPresent(id -> identifier = id);
		input.read("Items", ItemStack.CODEC.listOf()).ifPresent(li -> legacyItems = new ArrayList<>(li));
		pendingInput = input.read("pending", ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	/** Drops every stored item and the pending one, then forgets the crate's data. */
	@Override
	public final void breakBlock() {
		if (broken || level == null || level.isClientSide())
			return;
		broken = true;
		BlockPos p = this.getBlockPos();
		ReikaItemHelper.dropItems(level, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, new ArrayList<>(this.getItems()));
		if (!pendingInput.isEmpty())
			ReikaItemHelper.dropItems(level, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, List.of(pendingInput));
		pendingInput = ItemStack.EMPTY;
		ToolCrateData data = ToolCrateData.get(level);
		if (data != null) {
			data.items.remove(identifier);
			data.setDirty();
		}
	}

	/** Any removal, not only a player's break (V33a's BlockChromaTile.breakBlock). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		this.breakBlock();
	}

	/** The contents drop from {@link #breakBlock}, never also as an inventory. */
	@Override
	public final boolean dropsInventoryOnBroken() {
		return false;
	}

	public final ItemStack removeLastItem() {
		List<ItemStack> li = this.getItems();
		if (li.isEmpty())
			return ItemStack.EMPTY;
		ItemStack is = li.get(li.size()-1);
		this.removeItemAt(li.size()-1);
		return is;
	}

}
