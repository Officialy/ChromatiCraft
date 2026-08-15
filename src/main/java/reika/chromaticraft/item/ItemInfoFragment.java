package reika.chromaticraft.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ResearchFragmentData;
import reika.chromaticraft.registry.ChromaBlocks;

/** V33a blank, decoded, and chroma-soaked information fragments as one component-backed item. */
public final class ItemInfoFragment extends Item {

	public ItemInfoFragment(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (!(owner instanceof Player player) || level.getGameTime() % 8 != Math.floorMod(player.getId(), 8))
			return;
		ResearchFragmentData data = ResearchFragmentData.read(stack);
		if (data.blank() && data.random()) {
			LexiconCatalog.Entry page = PlayerResearch.randomNextResearch(player, level.getRandom());
			if (page != null) {
				data.withPage(page).writeTo(stack);
				PlayerResearch.giveFragment(player, page, true);
			}
		}
		else if (!data.blank() && data.page().canPlayerProgressTo(player)
				&& PlayerResearch.nextResearch(player).contains(data.page())) {
			PlayerResearch.giveFragment(player, data.page(), true);
		}
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		if (!entity.level().isClientSide() && ResearchFragmentData.read(stack).blank()
				&& entity.level().getBlockState(entity.blockPosition()).is(ChromaBlocks.CHROMA.get()))
			ResearchFragmentData.read(stack).soaked().writeTo(stack);
		return false;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ResearchFragmentData data = ResearchFragmentData.read(stack);
		if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer server
				&& data.blank() && !data.random() && !PlayerResearch.nextResearch(player).isEmpty())
			server.openMenu(new net.minecraft.world.SimpleMenuProvider(
					(id, inventory, ignored) -> new reika.chromaticraft.container.MenuFragmentSelection(id, inventory),
					Component.literal("Decode Information Fragment")));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		ResearchFragmentData data = ResearchFragmentData.read(stack);
		if (data.blank()) {
			tooltip.accept(Component.literal(data.random() ? "Chroma-Soaked" : "Undeciphered")
					.withStyle(ChatFormatting.ITALIC));
			if (!data.random())
				tooltip.accept(Component.literal("Extract information to learn contents"));
			return;
		}
		LexiconCatalog.Entry page = data.page();
		tooltip.accept(page.section().title().copy().append(": ").append(page.title()));
		if (page.id().equals("FRAGMENT")) {
			tooltip.accept(Component.literal(" Shift-Right-Click with"));
			tooltip.accept(Component.literal(" the lexicon to add"));
		}
	}

	public static ItemStack forPage(LexiconCatalog.Entry page) {
		ItemStack stack = new ItemStack(reika.chromaticraft.registry.ChromaItems.INFO_FRAGMENT.get());
		ResearchFragmentData.BLANK.withPage(page).writeTo(stack);
		return stack;
	}
}
