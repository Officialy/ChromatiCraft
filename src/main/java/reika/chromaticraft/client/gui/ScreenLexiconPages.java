package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuLexiconPages;
import reika.chromaticraft.network.ChromaNetwork;

/** V33a {@code GuiBookPages}: the actual fragment inventory stored inside a Chromic Lexicon. */
public final class ScreenLexiconPages extends AbstractContainerScreen<MenuLexiconPages> {

	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/basicstorage_small.png");

	public ScreenLexiconPages(MenuLexiconPages menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new LexiconImageButton(leftPos + 148, topPos + 4, 10, 10,
				90, 16, Component.literal("Previous row"), () -> scroll(-1)));
		addRenderableWidget(new LexiconImageButton(leftPos + 158, topPos + 4, 10, 10,
				90, 26, Component.literal("Next row"), () -> scroll(1)));
	}

	private void scroll(int direction) {
		ClientPacketDistributor.sendToServer(new ChromaNetwork.ScrollLexiconPages(direction));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
				0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Component count = Component.literal("Pages: " + menu.pageCount() + "/" + menu.totalPages());
		graphics.text(font, count, (imageWidth - font.width(count)) / 2, 6, 0xffffffff, false);
		graphics.text(font, playerInventoryTitle, 8, imageHeight - 94, 0xffffffff, false);
	}
}
