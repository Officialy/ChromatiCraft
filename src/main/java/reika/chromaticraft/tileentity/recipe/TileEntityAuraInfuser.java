package reika.chromaticraft.tileentity.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import reika.chromaticraft.auxiliary.interfaces.FocusAcceleratable;
import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal.CrystalTier;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.interfaces.blockentity.InertIInv;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * Full 26.2 port of V33a's owned one-slot aura-infusion pedestal. Despite its historical name this
 * machine does not consume crystal-network aura: the PURPLE/BLACK requirement was commented out in
 * V33a. Its consumable is the source Liquid Chroma ring encoded by {@link ChromaStructures#INFUSION}.
 */
public abstract class TileEntityAuraInfuser extends TileEntityChromaticBase implements WorldlyContainer,
		OwnedTile, OperationInterval, FocusAcceleratable, NBTTile, BreakAction, InertIInv {

	public static final int DURATION = 608;
	private static final Set<WorldLocation> CACHE = Collections.synchronizedSet(new HashSet<>());

	protected final NonNullList<ItemStack> inv = NonNullList.withSize(1, ItemStack.EMPTY);
	protected final Set<BlockPos> focusCrystalSpots = new HashSet<>();
	private final List<BlockPos> chromaLocations = new ArrayList<>();

	private int craftingTick;
	private boolean hasStructure = true;
	private UUID craftingPlayer;
	private int focusCrystalTotal;
	private boolean allExquisite;
	private int fluidCooldown;
	private boolean clientWasCrafting;
	private final ChromaRingFluidHandler fluidHandler = new ChromaRingFluidHandler();

	protected TileEntityAuraInfuser(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
			BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) {
			if (hasStructure && craftingTick > 0) {
				clientWasCrafting = true;
				this.spawnCraftingParticles(world, pos);
				craftingTick--;
			}
			else if (clientWasCrafting) {
				clientWasCrafting = false;
				this.spawnCompletionParticles(world, pos);
			}
			else if (hasStructure) {
				this.spawnAmbientParticles(world, pos);
			}
			return;
		}
		if (fluidCooldown > 0) fluidCooldown--;
		if (this.getTicksExisted() == 1) {
			this.validateStructure();
			CACHE.add(new WorldLocation(this));
		}
		else if (this.getTicksExisted() % 40 == 0) {
			this.validateStructure();
		}
		if (!hasStructure) {
			if (craftingTick > 0) this.killCrafting();
			craftingTick = 0;
			return;
		}
		if (craftingTick > 0) this.tickCrafting();
	}

	@Override protected final void animateWithTick(Level world, BlockPos pos) {}

	protected abstract ChromaStructures getStructure();
	protected abstract void collectFocusCrystalLocations(FilledBlockArray array);
	protected abstract boolean isReady();
	protected abstract void onCraft();
	protected abstract void spawnCraftingParticles(Level world, BlockPos pos);
	protected abstract void spawnCompletionParticles(Level world, BlockPos pos);
	protected void spawnAmbientParticles(Level world, BlockPos pos) {}
	protected void onCraftingTick(Level world, BlockPos pos) {}

	public final void validateStructure() {
		if (this.getLevel() == null || this.getLevel().isClientSide()) return;
		boolean previous = hasStructure;
		focusCrystalTotal = 0;
		allExquisite = true;
		focusCrystalSpots.clear();
		chromaLocations.clear();
		FilledBlockArray array = this.getStructure().getArray(this.getLevel(),
				this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
		hasStructure = array.matchInWorld();
		for (BlockPos cell : array.keySet()) {
			if (array.getBlockAt(cell.getX(), cell.getY(), cell.getZ()) == ChromaBlocks.CHROMA.get())
				chromaLocations.add(cell.immutable());
		}
		if (hasStructure) {
			this.collectFocusCrystalLocations(array);
			this.countFocusCrystals();
		}
		else if (craftingTick > 0) {
			this.killCrafting();
			craftingTick = 0;
		}
		this.setChanged();
		if (previous != hasStructure) this.syncAllData(false);
	}

	private void countFocusCrystals() {
		for (BlockPos location : focusCrystalSpots) {
			if (!(this.getLevel().getBlockEntity(location) instanceof TileEntityFocusCrystal focus)) continue;
			CrystalTier tier = focus.getTier();
			if (tier == CrystalTier.FLAWED) {
				focusCrystalTotal = 0;
				allExquisite = false;
				break;
			}
			focusCrystalTotal += 1 << Math.max(0, tier.effectiveOrdinal() - 1);
			if (!tier.isMaximumPower()) allExquisite = false;
			focus.addConnection(this, true);
		}
	}

	private void tickCrafting() {
		if (!this.canCraft()) {
			craftingTick = 0;
			this.killCrafting();
			return;
		}
		int speed = this.getCraftSpeed();
		if (speed == 4 && craftingTick % 152 == 0) ChromaSounds.INFUSION_SHORT.playSoundAtBlock(this);
		else if (craftingTick % 304 == 0) ChromaSounds.INFUSION.playSoundAtBlock(this);
		craftingTick--;
		this.onCraftingTick(this.getLevel(), this.getBlockPos());
		if (craftingTick == 0) this.craft();
	}

	protected final void craft() {
		ChromaSounds.INFUSE.playSoundAtBlock(this);
		this.onCraft();
		for (BlockPos location : chromaLocations) {
			BlockState state = this.getLevel().getBlockState(location);
			if (state.is(ChromaBlocks.CHROMA.get()) && state.getFluidState().isSource())
				this.getLevel().setBlock(location, Blocks.AIR.defaultBlockState(), 3);
		}
		craftingPlayer = null;
		this.validateStructure();
		this.setChanged();
		this.syncAllData(true);
	}

	private void killCrafting() {
		ChromaSounds.ERROR.playSoundAtBlock(this);
	}

	protected final boolean canCraft() {
		Player player = this.getCraftingPlayer();
		return player != null && ProgressStage.ALLOY.isPlayerAtStage(player) && this.isReady();
	}

	protected final Player getCraftingPlayer() {
		return craftingPlayer != null && this.getLevel() != null
				? this.getLevel().getPlayerByUUID(craftingPlayer) : null;
	}

	private int getCraftSpeed() {
		if (allExquisite && focusCrystalTotal >= 16) return 4;
		return focusCrystalTotal >= 8 ? 2 : 1;
	}

	public final ItemStack interact(ItemStack held, Player player) {
		if (!this.isOwnedByPlayer(player)) return held;
		this.validateStructure();
		if (!hasStructure) {
			if (inv.get(0).isEmpty() || !held.isEmpty()) return held;
			this.dropItem();
			return held;
		}
		if (!held.isEmpty() && !this.canPlaceItem(0, held)) return held;
		ItemStack stored = inv.get(0);
		if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stored)) {
			if (stored.getCount() + held.getCount() <= this.getMaxStackSize()) {
				stored.grow(held.getCount());
				held.setCount(0);
			}
			else if (stored.getCount() < this.getMaxStackSize()) {
				// V33a intentionally moved one item per click when the combined stacks overflowed.
				stored.grow(1);
				held.shrink(1);
			}
		}
		else if (!stored.isEmpty()) {
			this.dropItem();
		}
		if (!held.isEmpty() && inv.get(0).isEmpty()) {
			int add = held.getCount() <= this.getMaxStackSize() ? held.getCount() : 1;
			inv.set(0, held.copyWithCount(add));
			held.shrink(add);
		}
		craftingPlayer = player.getUUID();
		this.inventoryChanged();
		return held;
	}

	public final boolean onItemCollision(ItemEntity entity) {
		if (this.getLevel() == null || this.getLevel().isClientSide() || entity.hasPickUpDelay()) return false;
		ItemStack dropped = entity.getItem();
		if (!this.canPlaceItem(0, dropped)) return false;
		ItemStack stored = inv.get(0);
		if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, dropped)) return false;
		int present = stored.getCount();
		int add = Math.min(dropped.getCount(), dropped.getMaxStackSize() - present);
		if (add <= 0) return false;
		craftingTick = 0;
		inv.set(0, dropped.copyWithCount(present + add));
		dropped.shrink(add);
		if (entity.getOwner() instanceof Player player) craftingPlayer = player.getUUID();
		this.inventoryChanged();
		if (dropped.isEmpty()) entity.discard();
		return dropped.isEmpty();
	}

	protected ItemEntity dropItem() {
		if (this.getLevel() == null || this.getLevel().isClientSide() || inv.get(0).isEmpty()) return null;
		ItemStack stack = inv.get(0);
		inv.set(0, ItemStack.EMPTY);
		ItemEntity entity = new ItemEntity(this.getLevel(), this.getBlockPos().getX() + 0.5,
				this.getBlockPos().getY() + 0.5, this.getBlockPos().getZ() + 0.5, stack);
		this.getLevel().addFreshEntity(entity);
		this.inventoryChanged();
		return entity;
	}

	private void inventoryChanged() {
		if (this.canCraft()) {
			if (craftingTick == 0) craftingTick = DURATION / this.getCraftSpeed();
		}
		else {
			if (craftingTick > 0) this.killCrafting();
			craftingTick = 0;
		}
		this.setChanged();
		if (this.getLevel() != null && !this.getLevel().isClientSide()) this.syncAllData(true);
	}

	public final boolean hasStructure() { return hasStructure; }
	public final int getCraftingTick() { return craftingTick; }
	public final Collection<BlockPos> getChromaLocations() { return Collections.unmodifiableList(chromaLocations); }
	public final ItemStack getRenderItem() { return inv.get(0); }
	public final boolean canAcceptFluid() { return fluidCooldown == 0 && this.getLevel() != null && !this.getLevel().isClientSide(); }
	public final void onFluidInserted() { fluidCooldown = 2; this.validateStructure(); }
	public final ResourceHandler<FluidResource> fluidHandler() { return fluidHandler; }

	/** The V33a IFluidHandler's virtual tanks: one 1000 mB tank for each authored chroma-ring cell. */
	private final class ChromaRingFluidHandler extends SnapshotJournal<List<BlockState>>
			implements ResourceHandler<FluidResource> {
		@Override public int size() { return chromaLocations.size(); }
		@Override public FluidResource getResource(int index) {
			if (!validIndex(index) || getLevel() == null) return FluidResource.EMPTY;
			BlockState state = getLevel().getBlockState(chromaLocations.get(index));
			return state.is(ChromaBlocks.CHROMA.get()) && state.getFluidState().isSource()
					? FluidResource.of(ChromaFluids.CHROMA.get()) : FluidResource.EMPTY;
		}
		@Override public long getAmountAsLong(int index) {
			return this.getResource(index).isEmpty() ? 0 : FluidType.BUCKET_VOLUME;
		}
		@Override public long getCapacityAsLong(int index, FluidResource resource) {
			return validIndex(index) && (resource.isEmpty() || this.isValid(index, resource))
					? FluidType.BUCKET_VOLUME : 0;
		}
		@Override public boolean isValid(int index, FluidResource resource) {
			return validIndex(index) && resource.getFluid() == ChromaFluids.CHROMA.get();
		}
		@Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			if (!this.isValid(index, resource) || amount < FluidType.BUCKET_VOLUME || !canAcceptFluid()) return 0;
			BlockPos cell = chromaLocations.get(index);
			BlockState current = getLevel().getBlockState(cell);
			if (!current.isAir() && !(current.is(ChromaBlocks.CHROMA.get()) && !current.getFluidState().isSource())) return 0;
			this.updateSnapshots(transaction);
			getLevel().setBlock(cell, ChromaBlocks.CHROMA.get().defaultBlockState(), 3);
			return FluidType.BUCKET_VOLUME;
		}
		@Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			return 0;
		}
		private boolean validIndex(int index) { return index >= 0 && index < chromaLocations.size(); }
		@Override protected List<BlockState> createSnapshot() {
			return chromaLocations.stream().map(getLevel()::getBlockState).toList();
		}
		@Override protected void revertToSnapshot(List<BlockState> snapshot) {
			for (int i = 0; i < Math.min(snapshot.size(), chromaLocations.size()); i++)
				getLevel().setBlock(chromaLocations.get(i), snapshot.get(i), 3);
		}
		@Override protected void onRootCommit(List<BlockState> originalState) {
			fluidCooldown = 2;
			setChanged();
			validateStructure();
			getLevel().playSound(null, getBlockPos(), SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS,
					1F, 0.5F + rand.nextFloat());
		}
	}

	@Override public final float getOperationFraction() {
		return 1F - craftingTick / (float)(DURATION / this.getCraftSpeed());
	}
	@Override public final OperationState getState() {
		return this.canCraft() ? hasStructure ? OperationState.RUNNING : OperationState.PENDING : OperationState.INVALID;
	}
	@Override public final float getAccelerationFactor() {
		int speed = this.getCraftSpeed();
		return speed == 1 ? 0 : speed;
	}
	@Override public final float getMaximumAcceleratability() { return 4; }
	@Override public final float getProgressToNextStep() {
		if (focusCrystalTotal < 8) return focusCrystalTotal / 8F;
		if (focusCrystalTotal >= 16 || !allExquisite) return 0;
		return (focusCrystalTotal - 8) / 8F;
	}
	@Override public final void recountFocusCrystals() { this.validateStructure(); }
	@Override public final Collection<BlockPos> getRelativeFocusCrystalLocations() {
		return focusCrystalSpots.stream().map(pos -> pos.subtract(this.getBlockPos())).toList();
	}

	@Override public final int getContainerSize() { return 1; }
	@Override public final boolean isEmpty() { return inv.get(0).isEmpty(); }
	@Override public final ItemStack getItem(int slot) { return slot == 0 ? inv.get(0) : ItemStack.EMPTY; }
	@Override public final ItemStack removeItem(int slot, int amount) {
		ItemStack removed = slot == 0 ? ContainerHelper.removeItem(inv, slot, amount) : ItemStack.EMPTY;
		if (!removed.isEmpty()) this.inventoryChanged();
		return removed;
	}
	@Override public final ItemStack removeItemNoUpdate(int slot) {
		return slot == 0 ? ContainerHelper.takeItem(inv, 0) : ItemStack.EMPTY;
	}
	@Override public final void setItem(int slot, ItemStack stack) {
		if (slot != 0) return;
		inv.set(0, stack.copyWithCount(Math.min(stack.getCount(), this.getMaxStackSize())));
		this.inventoryChanged();
	}
	@Override public final void clearContent() { inv.set(0, ItemStack.EMPTY); this.inventoryChanged(); }
	@Override public final boolean stillValid(Player player) { return this.isPlayerAccessible(player); }
	@Override public final int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public final boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public final boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

	@Override public final boolean onlyAllowOwnersToMine() { return true; }
	@Override public final boolean onlyAllowOwnersToUse() { return true; }
	@Override public final boolean isOwnedByPlayer(Player player) {
		return placerUUID == null || placerUUID.equals(player.getUUID());
	}
	@Override public void getTagsToWriteToStack(CompoundTag tag) {
		if (this.getPlacerName() != null && !this.getPlacerName().isEmpty()) tag.putString("place", this.getPlacerName());
		if (placerUUID != null) tag.putString("placeUUID", placerUUID.toString());
	}
	@Override public void setDataFromItemStackTag(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		if (tag == null) return;
		placer = tag.getStringOr("place", "");
		if (tag.contains("placeUUID")) placerUUID = UUID.fromString(tag.getStringOr("placeUUID", ""));
	}
	@Override public void addTooltipInfo(List list, boolean shift) {}

	@Override public void breakBlock() {
		this.dropItem();
		if (this.getLevel() != null) CACHE.remove(new WorldLocation(this));
	}

	public static void clearCache() { CACHE.clear(); }
	public static WorldLocation searchForMatch(Predicate<WorldLocation> check) {
		synchronized (CACHE) { return CACHE.stream().filter(check).findFirst().orElse(null); }
	}

	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, inv);
		output.putBoolean("struct", hasStructure);
		output.putInt("craft", craftingTick);
		output.putInt("focus", focusCrystalTotal);
		output.putBoolean("exq", allExquisite);
	}
	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		inv.clear();
		ContainerHelper.loadAllItems(input, inv);
		hasStructure = input.getBooleanOr("struct", true);
		craftingTick = input.getIntOr("craft", 0);
		focusCrystalTotal = input.getIntOr("focus", 0);
		allExquisite = input.getBooleanOr("exq", false);
		craftingPlayer = null;
	}
	@Override protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putBoolean("struct", hasStructure);
		tag.putInt("craft", craftingTick);
		tag.putInt("focus", focusCrystalTotal);
		tag.putBoolean("exq", allExquisite);
		RegistryAccess access = this.getLevel() == null ? RegistryAccess.EMPTY : this.getLevel().registryAccess();
		ItemStack.OPTIONAL_CODEC.encodeStart(access.createSerializationContext(NbtOps.INSTANCE), inv.get(0))
				.result().ifPresent(encoded -> tag.put("renderItem", encoded));
	}
	@Override protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		hasStructure = tag.getBooleanOr("struct", true);
		craftingTick = tag.getIntOr("craft", 0);
		focusCrystalTotal = tag.getIntOr("focus", 0);
		allExquisite = tag.getBooleanOr("exq", false);
		RegistryAccess access = this.getLevel() == null ? RegistryAccess.EMPTY : this.getLevel().registryAccess();
		net.minecraft.nbt.Tag itemTag = tag.get("renderItem");
		inv.set(0, itemTag == null ? ItemStack.EMPTY : ItemStack.OPTIONAL_CODEC
				.parse(access.createSerializationContext(NbtOps.INSTANCE), itemTag)
				.result().orElse(ItemStack.EMPTY));
	}
}
