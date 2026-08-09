package reika.chromaticraft.item;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.LexiconData;

/** The V33a Chromic Lexicon, backed by 26.2 custom-data components. */
public final class ItemChromaBook extends Item {

	public ItemChromaBook(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() && hand == InteractionHand.MAIN_HAND)
			reika.chromaticraft.client.ChromaClientScreens.openLexicon(player,
					player.getItemInHand(hand), player.isShiftKeyDown());
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		LexiconData data = LexiconData.read(stack);
		if (data.creative()) {
			tooltip.accept(Component.literal("Creative Spawned").withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		tooltip.accept(Component.literal("Has " + data.pages().size() + " of "
				+ LexiconCatalog.obtainablePages().size() + " pages."));
		tooltip.accept(Component.literal("Has " + data.blanks() + " extra blank fragment"
				+ (data.blanks() == 1 ? "." : "s.")));
	}

	public static ItemStack creativeStack() {
		ItemStack stack = new ItemStack(reika.chromaticraft.registry.ChromaItems.LEXICON.get());
		new LexiconData(LexiconCatalog.obtainablePages().stream().map(LexiconCatalog.Entry::id).toList(),
				true, 0, List.of()).writeTo(stack);
		return stack;
	}

	public static boolean hasPage(ItemStack stack, LexiconCatalog.Entry page) {
		return page.readableWithoutFragment() || LexiconData.read(stack).hasPage(page.id());
	}

	public static boolean hasAllPages(ItemStack stack) {
		for (LexiconCatalog.Entry page : LexiconCatalog.obtainablePages()) {
			if (!hasPage(stack, page))
				return false;
		}
		return true;
	}

	public static boolean addPage(ItemStack stack, LexiconCatalog.Entry page) {
		if (page == null || !page.obtainable() || hasPage(stack, page))
			return false;
		LexiconData.read(stack).withPage(page.id()).writeTo(stack);
		return true;
	}

	/** V33a fragment recovery: one paper plus one black dye is consumed atomically. */
	public static boolean recoverFragment(ServerPlayer player, ItemStack book, LexiconCatalog.Entry page) {
		if (page == null || !page.obtainable() || hasPage(book, page))
			return false;
		Inventory inventory = player.getInventory();
		int paper = find(inventory, stack -> stack.is(Items.PAPER));
		int ink = find(inventory, stack -> stack.is(Tags.Items.DYES_BLACK));
		if (paper < 0 || ink < 0)
			return false;
		inventory.getItem(paper).shrink(1);
		inventory.getItem(ink).shrink(1);
		return addPage(book, page);
	}

	private static int find(Inventory inventory, java.util.function.Predicate<ItemStack> match) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (match.test(inventory.getItem(slot)))
				return slot;
		}
		return -1;
	}
}
