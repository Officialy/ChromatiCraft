package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuStructurePassword;

/** Source-faithful translucent V33a {@code structpass.png} screen. */
public final class ScreenStructurePassword extends AbstractContainerScreen<MenuStructurePassword> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/structure_password.png");

	public ScreenStructurePassword(MenuStructurePassword menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
				imageWidth, imageHeight, 256, 256, 0x80ffffff);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
				imageWidth, imageHeight, 256, 256, 0x40ffffff);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, 0xffffffff, false);
	}
}
