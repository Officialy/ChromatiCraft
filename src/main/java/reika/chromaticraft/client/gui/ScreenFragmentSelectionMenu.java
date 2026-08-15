package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuFragmentSelection;
import reika.chromaticraft.item.ItemInfoFragment;
import reika.chromaticraft.network.ChromaNetwork;

/** Extracted-render-pipeline port of V33a {@code GuiFragmentSelect}. */
public final class ScreenFragmentSelectionMenu extends AbstractContainerScreen<MenuFragmentSelection> {

	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/fragselect.png");
	private static final Identifier CATEGORIES = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/fragmentcategories.png");
	private static final Identifier FOG = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/squarefog.png");
	private static final long SELECTION_TIME_MS = 1500;
	private int selected = -1;
	private long selectionEnds;

	public ScreenFragmentSelectionMenu(MenuFragmentSelection menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 180, 162);
	}

	@Override
	protected void init() {
		super.init();
		for (int index = 0; index < 3; index++) {
			final int choice = index;
			addRenderableWidget(new ChoiceButton(choice,
					leftPos + 33 + index * 41, topPos + 37));
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (selected >= 0 && System.currentTimeMillis() >= selectionEnds) {
			ClientPacketDistributor.sendToServer(new ChromaNetwork.SelectFragmentChoice(selected));
			selected = -1;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Upstream draws xSize+64/ySize+64 from (-32,-32), leaving its animated fog fringe outside
		// the logical container while preserving the exact slot coordinates.
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos - 32, topPos - 32,
				0, 0, imageWidth + 64, imageHeight + 64, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// The authored texture supplies all labels.
	}

	private final class ChoiceButton extends AbstractButton {
		private final int index;

		private ChoiceButton(int index, int x, int y) {
			super(x, y, 32, 32, Component.literal("Decode fragment"));
			this.index = index;
		}

		@Override
		public void onPress(net.minecraft.client.input.InputWithModifiers input) {
			if (menu.hasFragment() && menu.choice(index) != null) {
				selected = index;
				selectionEnds = System.currentTimeMillis() + SELECTION_TIME_MS;
			}
		}

		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			visible = menu.hasFragment() && menu.choice(index) != null;
			active = visible;
			if (!visible)
				return;
			graphics.blit(RenderPipelines.GUI_TEXTURED, FOG,
					getX() - 6, getY() - 6, 0, 0, 44, 44, 256, 256);
			int category = menu.category(index).ordinal();
			graphics.blit(RenderPipelines.GUI_TEXTURED, CATEGORIES, getX(), getY(),
					(category % 8) * 32, (category / 8) * 32, 32, 32, 256, 256);
			graphics.item(ItemInfoFragment.forPage(menu.choice(index)), getX() + 8, getY() + 8);
			if (selected == index) {
				float progress = 1F - Math.clamp((selectionEnds - System.currentTimeMillis())
						/ (float)SELECTION_TIME_MS, 0, 1);
				int colour = 0xff80ffff;
				graphics.outline(getX() - 2, getY() - 2, 36, 36, colour);
				graphics.fill(getX() - 2, getY() + 35, getX() - 2 + Math.round(36 * progress),
						getY() + 37, colour);
			}
		}
	}
}
