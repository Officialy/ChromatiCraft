package reika.chromaticraft.container;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.item.ItemInfoFragment;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.LexiconData;
import reika.chromaticraft.magic.progression.ResearchFragmentData;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaMenus;

/** V33a's scrollable 9x3 lexicon fragment inventory, backed directly by the held book. */
public final class MenuLexiconPages extends AbstractContainerMenu {

	public static final int PAGE_SLOTS = 27;
	private static final int COLUMNS = 9;
	private final Inventory playerInventory;
	private final SimpleContainer pageInventory = new SimpleContainer(PAGE_SLOTS);
	private int scroll;
	private boolean loading;

	public MenuLexiconPages(int id, Inventory inventory, FriendlyByteBuf ignored) {
		this(id, inventory);
	}

	public MenuLexiconPages(int id, Inventory inventory) {
		super(ChromaMenus.LEXICON_PAGES.get(), id);
		playerInventory = inventory;
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < COLUMNS; column++)
				addSlot(new PageSlot(column + row * COLUMNS, 8 + column * 18, 17 + row * 18));
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < COLUMNS; column++)
				addSlot(new Slot(inventory, column + row * COLUMNS + 9,
						8 + column * 18, 84 + row * 18));
		for (int column = 0; column < COLUMNS; column++) {
			int inventorySlot = column;
			Slot slot = inventorySlot == inventory.getSelectedSlot()
					? new HeldBookSlot(inventory, inventorySlot, 8 + column * 18, 142)
					: new Slot(inventory, inventorySlot, 8 + column * 18, 142);
			addSlot(slot);
		}
		loadSlice();
	}

	public int scroll() {
		return scroll;
	}

	public int maxScroll() {
		return Math.max(0, (LexiconCatalog.obtainablePages().size() - 1) / COLUMNS - 2);
	}

	public int pageCount() {
		return LexiconData.read(book()).pages().size();
	}

	public int totalPages() {
		return LexiconCatalog.obtainablePages().size();
	}

	public void scroll(int direction) {
		if (playerInventory.player.level().isClientSide())
			return;
		int next = Math.clamp(scroll + Integer.signum(direction), 0, maxScroll());
		if (next == scroll)
			return;
		commitSlice();
		scroll = next;
		loadSlice();
		broadcastChanges();
	}

	@Override
	public void slotsChanged(net.minecraft.world.Container container) {
		super.slotsChanged(container);
		if (container == pageInventory && !loading)
			commitSlice();
	}

	private void loadSlice() {
		loading = true;
		pageInventory.clearContent();
		ItemStack book = book();
		if (book.is(ChromaItems.LEXICON.get())) {
			LexiconData data = LexiconData.read(book);
			List<LexiconCatalog.Entry> pages = LexiconCatalog.obtainablePages();
			int offset = scroll * COLUMNS;
			for (int slot = 0; slot < PAGE_SLOTS && offset + slot < pages.size(); slot++) {
				LexiconCatalog.Entry page = pages.get(offset + slot);
				if (data.pages().contains(page.id()))
					pageInventory.setItem(slot, ItemInfoFragment.forPage(page));
			}
		}
		loading = false;
	}

	private void commitSlice() {
		if (playerInventory.player.level().isClientSide())
			return;
		ItemStack book = book();
		if (!book.is(ChromaItems.LEXICON.get()))
			return;
		LexiconData data = LexiconData.read(book);
		LinkedHashSet<String> stored = new LinkedHashSet<>(data.pages());
		List<LexiconCatalog.Entry> pages = LexiconCatalog.obtainablePages();
		int offset = scroll * COLUMNS;
		for (int slot = 0; slot < PAGE_SLOTS && offset + slot < pages.size(); slot++)
			stored.remove(pages.get(offset + slot).id());
		for (int slot = 0; slot < PAGE_SLOTS; slot++) {
			ResearchFragmentData fragment = ResearchFragmentData.read(pageInventory.getItem(slot));
			if (!fragment.blank())
				stored.add(fragment.pageId());
		}
		data.withPages(stored).writeTo(book);
	}

	private ItemStack book() {
		return playerInventory.getSelectedItem();
	}

	private boolean containsPage(String pageId, int exceptSlot) {
		for (int slot = 0; slot < PAGE_SLOTS; slot++) {
			if (slot == exceptSlot)
				continue;
			ResearchFragmentData data = ResearchFragmentData.read(pageInventory.getItem(slot));
			if (!data.blank() && data.pageId().equals(pageId))
				return true;
		}
		return LexiconData.read(book()).pages().stream().anyMatch(pageId::equals)
				&& !isPageInCurrentSlice(pageId);
	}

	private boolean isPageInCurrentSlice(String pageId) {
		List<LexiconCatalog.Entry> pages = LexiconCatalog.obtainablePages();
		int index = -1;
		for (int i = 0; i < pages.size(); i++)
			if (pages.get(i).id().equals(pageId)) {
				index = i;
				break;
			}
		int offset = scroll * COLUMNS;
		return index >= offset && index < offset + PAGE_SLOTS;
	}

	@Override
	public boolean stillValid(Player player) {
		return book().is(ChromaItems.LEXICON.get());
	}

	@Override
	public void removed(Player player) {
		commitSlice();
		super.removed(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < PAGE_SLOTS) {
			if (!moveItemStackTo(stack, PAGE_SLOTS, slots.size(), true))
				return ItemStack.EMPTY;
		}
		else {
			ResearchFragmentData fragment = ResearchFragmentData.read(stack);
			if (!stack.is(ChromaItems.INFO_FRAGMENT.get()) || fragment.blank()
					|| containsPage(fragment.pageId(), -1)
					|| !moveItemStackTo(stack, 0, PAGE_SLOTS, false))
				return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}

	private final class PageSlot extends Slot {
		private PageSlot(int index, int x, int y) {
			super(pageInventory, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (!stack.is(ChromaItems.INFO_FRAGMENT.get()))
				return false;
			ResearchFragmentData data = ResearchFragmentData.read(stack);
			return !data.blank() && !containsPage(data.pageId(), getContainerSlot());
		}
	}

	private static final class HeldBookSlot extends Slot {
		private HeldBookSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}
		@Override public boolean mayPickup(Player player) { return false; }
		@Override public boolean mayPlace(ItemStack stack) { return false; }
	}
}
