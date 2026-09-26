package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import reika.chromaticraft.container.MenuCrystalBrewer;
import reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer;

/**
 * V33a {@code GuiCrystalBrewer}: the vanilla brewing stand screen, its brew arrow and bubbles driven by the Brewery's
 * 400-tick timer. It is drawn from 26.2's brewing stand art (V33a used 1.7.10's, which is Mojang's to ship, not ours),
 * so the stand's blaze-fuel slot and gauge show empty: the Brewery has no fuel.
 */
public final class ScreenCrystalBrewer extends AbstractContainerScreen<MenuCrystalBrewer> {

	private static final Identifier BREW_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
	private static final Identifier BUBBLES_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
	private static final Identifier BREWING_STAND_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/brewing_stand.png");
	private static final int[] BUBBLELENGTHS = new int[]{29, 24, 20, 16, 11, 6, 0};

	public ScreenCrystalBrewer(MenuCrystalBrewer menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int k = (this.width - this.imageWidth) / 2;
		int l = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BREWING_STAND_LOCATION, k, l, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		int i1 = this.menu.getBrewTime();

		if (i1 > 0) {
			int j1 = (int)(28.0F * (1.0F - i1 / (float)TileEntityCrystalBrewer.BREW_TIME));

			if (j1 > 0) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BREW_PROGRESS_SPRITE, 9, 28, 0, 0, k + 97, l + 16, 9, j1);
			}

			j1 = BUBBLELENGTHS[i1 / 2 % 7];

			if (j1 > 0) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUBBLES_SPRITE, 12, 29, 0, 29 - j1, k + 63, l + 14 + 29 - j1, 12, j1);
			}
		}
	}

}
