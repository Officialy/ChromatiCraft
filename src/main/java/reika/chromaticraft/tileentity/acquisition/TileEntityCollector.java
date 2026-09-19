/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.acquisition;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import reika.chromaticraft.auxiliary.interfaces.ChromaExtractable;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.container.MenuCollector;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.modinteract.ReikaXPFluidHelper;

/** Complete V33a Chroma Collector on the 26.2 inventory, persistence, and transfer APIs. */
public final class TileEntityCollector extends TileEntityChromaticBase
		implements WorldlyContainer, MenuProvider, OwnedTile, ChromaExtractable {

	public static final int XP_PER_CHROMA = 1;
	public static final int XP_PER_BOTTLE = 300;
	public static final int CAPACITY = 3000;
	private static final int CONVERSION_SPEED = 5;
	private static final int INPUT_TANK = 0;
	private static final int OUTPUT_TANK = 1;

	private final NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
	private final CollectorFluidHandler fluidHandler = new CollectorFluidHandler();
	private FluidResource inputFluid = FluidResource.EMPTY;
	private int inputLevel;
	private int outputLevel;

	public TileEntityCollector(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.COLLECTOR.get(), pos, state);
	}

	@Override public ChromaTiles getTile() { return ChromaTiles.COLLECTOR; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) return;
		this.internalizeXPBottle();
		// V33a redstone pauses only piped-fluid conversion, not bottles or direct owner intake.
		if (!this.hasRedstoneSignal()) this.convertInputFluid();
		this.tryIntakeXPFromPlayer(this.getPlacer(), true);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (world != null && world.isClientSide() && world.getRandom().nextInt(4) == 0)
			ChromaParticle.spawnCollectorRune(world, pos, world.getRandom());
	}

	private void internalizeXPBottle() {
		ItemStack input = inventory.get(0);
		ItemStack output = inventory.get(1);
		if (!input.is(Items.EXPERIENCE_BOTTLE) || outputLevel + XP_PER_BOTTLE > CAPACITY
				|| (!output.isEmpty() && (!output.is(Items.GLASS_BOTTLE)
						|| output.getCount() >= output.getMaxStackSize()))) return;
		input.shrink(1);
		if (input.isEmpty()) inventory.set(0, ItemStack.EMPTY);
		if (output.isEmpty()) inventory.set(1, new ItemStack(Items.GLASS_BOTTLE));
		else output.grow(1);
		outputLevel += XP_PER_BOTTLE;
		this.contentsChanged();
	}

	private void convertInputFluid() {
		if (inputLevel <= 0 || outputLevel >= CAPACITY || inputFluid.isEmpty()) return;
		int milliBucketsPerXP = milliBucketsPerXP(inputFluid.getFluid());
		if (milliBucketsPerXP <= 0) return;
		int produced = Math.min(CONVERSION_SPEED,
				Math.min(CAPACITY - outputLevel, inputLevel / (milliBucketsPerXP * XP_PER_CHROMA)));
		if (produced <= 0) return;
		inputLevel -= produced * milliBucketsPerXP * XP_PER_CHROMA;
		outputLevel += produced;
		if (inputLevel == 0) inputFluid = FluidResource.EMPTY;
		this.contentsChanged();
	}

	public void tryIntakeXPFromPlayer(Player player, boolean requireContact) {
		if (player == null || player instanceof FakePlayer || player.isSpectator()
				|| outputLevel + CONVERSION_SPEED > CAPACITY
				|| player.totalExperience < XP_PER_CHROMA * CONVERSION_SPEED) return;
		if (requireContact && !player.getBoundingBox().intersects(new AABB(this.getBlockPos().above()))) return;
		outputLevel += CONVERSION_SPEED;
		player.giveExperiencePoints(-CONVERSION_SPEED * XP_PER_CHROMA);
		ProgressStage.MAKECHROMA.giveToPlayer(player, true);
		this.contentsChanged();
	}

	private static int milliBucketsPerXP(Fluid fluid) {
		if (ReikaXPFluidHelper.fluidsExist() && fluid == ReikaXPFluidHelper.getFluidType()) {
			var stack = ReikaXPFluidHelper.getFluid();
			return stack != null ? Math.max(1, stack.getAmount()) : 0;
		}
		Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
		String path = id.getPath().replace("_", "");
		return path.equals("xp") || path.equals("experience") || path.equals("xpjuice") ? 1 : 0;
	}

	public ResourceHandler<FluidResource> fluidHandler() { return fluidHandler; }
	public int getInputLevel() { return inputLevel; }
	public int getOutputLevel() { return outputLevel; }
	public FluidResource getInputFluid() { return inputFluid; }
	public int getCapacity() { return CAPACITY; }
	@Override public int getChromaLevel() { return outputLevel; }
	@Override public void removeLiquid(int amount) {
		if (amount > 0) { outputLevel = Math.max(0, outputLevel - amount); this.contentsChanged(); }
	}
	private void contentsChanged() { this.setChanged(); }

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		inventory.clear();
		ContainerHelper.loadAllItems(input, inventory);
		inputFluid = input.read("inputFluid", FluidResource.OPTIONAL_CODEC).orElse(FluidResource.EMPTY);
		inputLevel = Math.clamp(input.getIntOr("inputLevel", 0), 0, CAPACITY);
		outputLevel = Math.clamp(input.getIntOr("outputLevel", 0), 0, CAPACITY);
		if (inputLevel == 0 || inputFluid.isEmpty()) { inputLevel = 0; inputFluid = FluidResource.EMPTY; }
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, inventory);
		output.store("inputFluid", FluidResource.OPTIONAL_CODEC, inputFluid);
		output.putInt("inputLevel", inputLevel);
		output.putInt("outputLevel", outputLevel);
	}

	@Override public int getContainerSize() { return inventory.size(); }
	@Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
	@Override public ItemStack getItem(int slot) { return inventory.get(slot); }
	@Override public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(inventory, slot, amount);
		if (!removed.isEmpty()) this.contentsChanged();
		return removed;
	}
	@Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(inventory, slot); }
	@Override public void setItem(int slot, ItemStack stack) {
		inventory.set(slot, stack.copyWithCount(Math.min(stack.getCount(), stack.getMaxStackSize())));
		this.contentsChanged();
	}
	@Override public void clearContent() { inventory.clear(); this.contentsChanged(); }
	@Override public boolean stillValid(Player player) { return this.isPlayerAccessible(player); }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.is(Items.EXPERIENCE_BOTTLE); }
	@Override public int[] getSlotsForFace(Direction side) { return new int[] {0, 1}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return this.canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

	@Override public Component getDisplayName() { return Component.translatable("block.chromaticraft.collector"); }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuCollector(id, inventory, this);
	}

	@Override public boolean onlyAllowOwnersToMine() { return true; }
	@Override public boolean onlyAllowOwnersToUse() { return false; }
	@Override public boolean isOwnedByPlayer(Player player) { return placerUUID == null || placerUUID.equals(player.getUUID()); }
	@Override public void getTagsToWriteToStack(CompoundTag tag) {
		if (placer != null && !placer.isEmpty()) tag.putString("place", placer);
		if (placerUUID != null) tag.putString("placeUUID", placerUUID.toString());
	}
	@Override public void setDataFromItemStackTag(ItemStack stack) {
		var custom = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag = custom != null ? custom.copyTag() : new CompoundTag();
		placer = tag.getStringOr("place", "");
		String owner = tag.getStringOr("placeUUID", "");
		placerUUID = owner.isEmpty() ? null : UUID.fromString(owner);
	}
	@Override public void addTooltipInfo(List list, boolean shift) {
		if (placer != null && !placer.isEmpty()) list.add(Component.literal("Owner: " + placer));
	}

	/** XP input at index zero and all-sided Liquid Chroma output at index one. */
	private final class CollectorFluidHandler extends SnapshotJournal<TankSnapshot>
			implements ResourceHandler<FluidResource> {
		@Override public int size() { return 2; }
		@Override public FluidResource getResource(int index) {
			return switch (index) {
				case INPUT_TANK -> inputLevel > 0 ? inputFluid : FluidResource.EMPTY;
				case OUTPUT_TANK -> outputLevel > 0 ? FluidResource.of(ChromaFluids.CHROMA.get()) : FluidResource.EMPTY;
				default -> FluidResource.EMPTY;
			};
		}
		@Override public long getAmountAsLong(int index) {
			return switch (index) { case INPUT_TANK -> inputLevel; case OUTPUT_TANK -> outputLevel; default -> 0; };
		}
		@Override public long getCapacityAsLong(int index, FluidResource resource) {
			return index >= 0 && index < 2 && (resource.isEmpty() || this.isValid(index, resource)) ? CAPACITY : 0;
		}
		@Override public boolean isValid(int index, FluidResource resource) {
			if (resource.isEmpty()) return false;
			return index == INPUT_TANK ? milliBucketsPerXP(resource.getFluid()) > 0
					: index == OUTPUT_TANK && resource.getFluid() == ChromaFluids.CHROMA.get();
		}
		@Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			if (index != INPUT_TANK || !this.isValid(index, resource)
					|| (inputLevel > 0 && !inputFluid.equals(resource))) return 0;
			int accepted = Math.min(amount, CAPACITY - inputLevel);
			if (accepted <= 0) return 0;
			this.updateSnapshots(transaction);
			inputFluid = resource;
			inputLevel += accepted;
			return accepted;
		}
		@Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			if (index != OUTPUT_TANK || resource.getFluid() != ChromaFluids.CHROMA.get()) return 0;
			int extracted = Math.min(amount, outputLevel);
			if (extracted <= 0) return 0;
			this.updateSnapshots(transaction);
			outputLevel -= extracted;
			return extracted;
		}
		@Override protected TankSnapshot createSnapshot() { return new TankSnapshot(inputFluid, inputLevel, outputLevel); }
		@Override protected void revertToSnapshot(TankSnapshot snapshot) {
			inputFluid = snapshot.inputFluid(); inputLevel = snapshot.inputLevel(); outputLevel = snapshot.outputLevel();
		}
		@Override protected void onRootCommit(TankSnapshot originalState) { contentsChanged(); }
	}
	private record TankSnapshot(FluidResource inputFluid, int inputLevel, int outputLevel) {}
}
