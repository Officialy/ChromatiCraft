package reika.chromaticraft.tileentity;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.ElementEncodedNumber;
import reika.chromaticraft.container.MenuStructurePassword;
import reika.chromaticraft.item.ItemChromaBerry;
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.item.ItemElementalStone;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/** Persistent 26.2 port of V33a {@code TileEntityStructurePassword}. */
public final class TileEntityStructurePassword extends BlockEntity implements Container, MenuProvider {

	private static final int SIZE = 8;
	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private CrystalElement color = CrystalElement.WHITE;
	private DimensionStructureType structureType = DimensionStructureType.LIGHTPANEL;
	private int generationIndex;
	private boolean unlocked;

	public TileEntityStructurePassword(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.STRUCTURE_PASSWORD.get(), pos, state);
	}

	public void setStructure(CrystalElement color, DimensionStructureType type, int generationIndex) {
		this.color = color;
		this.structureType = type;
		this.generationIndex = generationIndex;
		setChanged();
	}

	public boolean isUnlocked() { return unlocked; }

	public StructureGeneratorBase getStructure() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == structureType
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase generator)
				return generator;
		return null;
	}

	/** V33a checks after every slot click and returns all eight crystals on a successful bypass. */
	public boolean checkPassword(Player player) {
		if (level == null || level.isClientSide()) return unlocked;
		StructureGeneratorBase generator = this.getStructure();
		if (generator == null) {
			ChromaSounds.ERROR.playSoundAtBlock(this);
			return false;
		}
		if (unlocked || generator.forcedOpen()) {
			ChromaSounds.CRAFTDONE.playSoundAtBlock(this);
			return true;
		}
		if (ReikaPlayerAPI.isReika(player)) return unlock(player, generator);

		byte[] entered = new byte[SIZE];
		for (int slot = 0; slot < SIZE; slot++) {
			CrystalElement element = elementOf(items.get(slot));
			if (element == null) return false;
			entered[slot] = (byte)element.ordinal();
		}
		int password = generator.getPassword(player, SharedConstants.getCurrentVersion().name());
		if (new ElementEncodedNumber(password).match(entered))
			return unlock(player, generator);
		ChromaSounds.ERROR.playSoundAtBlock(this);
		return false;
	}

	private boolean unlock(Player player, StructureGeneratorBase generator) {
		unlocked = true;
		generator.forceOpen(level);
		for (int slot = 0; slot < SIZE; slot++) {
			ItemStack stack = items.get(slot);
			if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack.copy());
			items.set(slot, ItemStack.EMPTY);
		}
		setChanged();
		ChromaSounds.CRAFTDONE.playSoundAtBlock(this);
		return true;
	}

	public static CrystalElement elementOf(ItemStack stack) {
		if (stack.isEmpty()) return null;
		Item item = stack.getItem();
		if (item instanceof ItemCrystalShard shard) return shard.element();
		if (item instanceof ItemChromaBerry berry) return berry.element();
		if (item instanceof ItemElementalStone stone) return stone.element();
		for (CrystalElement element : CrystalElement.elements) {
			if (item == ChromaItems.SHARDS.get(element).get()
					|| item == ChromaItems.BOOSTED_SHARDS.get(element).get()
					|| item == ChromaBlocks.caveCrystal(element).get().asItem()
					|| item == ChromaBlocks.crystalLamp(element).get().asItem()
					|| item == ChromaBlocks.superCrystal(element).get().asItem()
					|| item == ChromaBlocks.rune(element).get().asItem())
				return element;
		}
		return null;
	}

	@Override public int getContainerSize() { return SIZE; }
	@Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
	@Override public ItemStack getItem(int slot) { return items.get(slot); }
	@Override public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		if (!removed.isEmpty()) setChanged();
		return removed;
	}
	@Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
	@Override public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack.copyWithCount(Math.min(1, stack.getCount())));
		setChanged();
	}
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return elementOf(stack) != null; }
	@Override public int getMaxStackSize() { return 1; }
	@Override public void clearContent() { items.clear(); setChanged(); }
	@Override public boolean stillValid(Player player) {
		return level != null && level.getBlockEntity(worldPosition) == this
				&& player.distanceToSqr(worldPosition.getX() + 0.5,
						worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
	}

	@Override public Component getDisplayName() {
		return Component.literal(structureType.getDisplayText() + " " + (generationIndex + 1) + " Bypass");
	}
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuStructurePassword(id, inventory, this);
	}

	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("color", color.ordinal());
		output.putInt("structure", structureType.ordinal());
		output.putInt("generation", generationIndex);
		output.putBoolean("unlocked", unlocked);
	}
	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		color = CrystalElement.elements[Math.floorMod(input.getIntOr("color", 0), CrystalElement.elements.length)];
		structureType = DimensionStructureType.types[Math.floorMod(input.getIntOr("structure",
				DimensionStructureType.LIGHTPANEL.ordinal()), DimensionStructureType.types.length)];
		generationIndex = Math.max(0, input.getIntOr("generation", 0));
		unlocked = input.getBooleanOr("unlocked", false);
	}
}
