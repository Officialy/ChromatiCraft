package reika.chromaticraft.client.gui;

import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuCrystalCharger;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;

/** Source-faithful 176x191 Storage Crystal Charger screen on the 26.2 extraction pipeline. */
public final class ScreenCrystalCharger extends AbstractContainerScreen<MenuCrystalCharger> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/crystal_charger.png");

	public ScreenCrystalCharger(MenuCrystalCharger menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 191);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
				imageWidth, imageHeight, 256, 256);
		for (CrystalElement element : CrystalElement.elements) {
			int x = leftPos + barX(element);
			int bottom = topPos + (element.ordinal() < 8 ? 51 : 91);
			int fill = Math.min(34, 34 * menu.energy(element) / TileEntityCrystalCharger.CAPACITY);
			if (fill > 0) graphics.fill(x, bottom - fill, x + 16, bottom, 0xff000000 | element.getColor());
			int outline = menu.isToggled(element) ? 0xff00ff00 : 0xffff0000;
			drawFrame(graphics, x - 1, bottom - 35, 18, 36, outline);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, (imageWidth - font.width(title)) / 2, 5, 0xffffffff, false);
		graphics.text(font, playerInventoryTitle, 8, imageHeight - 94, 0xffffffff, false);
		for (CrystalElement element : CrystalElement.elements) {
			int x = barX(element);
			int bottom = element.ordinal() < 8 ? 51 : 91;
			if (mouseX >= leftPos + x && mouseX < leftPos + x + 16
					&& mouseY >= topPos + bottom - 34 && mouseY < topPos + bottom) {
				Component tooltip = Component.literal(String.format("%s: %d/%d",
						element.displayName, menu.energy(element), TileEntityCrystalCharger.CAPACITY));
				graphics.setTooltipForNextFrame(font, java.util.List.of(tooltip), Optional.empty(),
						mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (CrystalElement element : CrystalElement.elements) {
				int x = leftPos + barX(element) - 1;
				int y = topPos + (element.ordinal() < 8 ? 16 : 56);
				if (event.x() >= x && event.x() < x + 18 && event.y() >= y && event.y() < y + 36) {
					ClientPacketDistributor.sendToServer(new ChromaNetwork.ToggleCrystalCharger(
							menu.charger().getBlockPos(), element.ordinal()));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static int barX(CrystalElement element) {
		int index = element.ordinal();
		return 6 + 18 * (index % 8) + (index % 8 >= 4 ? 22 : 0);
	}

	private static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}
}
