package reika.chromaticraft.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.network.ChromaNetwork;

/** V33a blank-fragment selection with server-authoritative candidate validation. */
public final class ScreenFragmentSelection extends Screen {

	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lexicon/fragments.png");
	private static final int WIDTH = 256;
	private static final int HEIGHT = 220;
	private final Player player;
	@SuppressWarnings("unused")
	private final ItemStack fragment;
	private int offset;

	public ScreenFragmentSelection(Player player, ItemStack fragment) {
		super(Component.literal("Decode Information Fragment"));
		this.player = player;
		this.fragment = fragment;
	}

	@Override
	protected void init() {
		clearWidgets();
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		List<LexiconCatalog.Entry> pages = PlayerResearch.nextResearch(player);
		int end = Math.min(offset + 8, pages.size());
		for (int i = offset; i < end; i++) {
			LexiconCatalog.Entry page = pages.get(i);
			addRenderableWidget(Button.builder(page.title(), button -> {
				ClientPacketDistributor.sendToServer(new ChromaNetwork.SelectResearchFragment(page.id()));
				onClose();
			}).bounds(left + 28, top + 22 + (i - offset) * 22, 200, 18).build());
		}
		if (offset > 0)
			addRenderableWidget(Button.builder(Component.literal("↑"), button -> {
				offset = Math.max(0, offset - 8);
				rebuildWidgets();
			}).bounds(left + 8, top + 196, 28, 18).build());
		if (end < pages.size())
			addRenderableWidget(Button.builder(Component.literal("↓"), button -> {
				offset += 8;
				rebuildWidgets();
			}).bounds(left + 220, top + 196, 28, 18).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 0, 0,
				WIDTH, HEIGHT, 256, 256);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 7, 0xffffffff);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}
}
