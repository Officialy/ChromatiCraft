package reika.chromaticraft.client.gui;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuCollector;
import reika.chromaticraft.tileentity.acquisition.TileEntityCollector;

/** Source-faithful V33a Collector screen using the original 176x166 GUI and tank positions. */
public final class ScreenCollector extends AbstractContainerScreen<MenuCollector> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/collector.png");
	public ScreenCollector(MenuCollector menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
				imageWidth, imageHeight, 256, 256);
		drawTank(graphics, leftPos + 35, topPos + 70, menu.inputLevel(), menu.inputFluid());
		drawTank(graphics, leftPos + 125, topPos + 70, menu.outputLevel(),
				reika.chromaticraft.registry.ChromaFluids.CHROMA.get());
	}
	private static void drawTank(GuiGraphicsExtractor graphics, int x, int bottom, int level, Fluid fluid) {
		int height = 54 * level / TileEntityCollector.CAPACITY;
		if (height <= 0 || fluid == Fluids.EMPTY) return;
		var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
				.get(fluid.defaultFluidState());
		TextureAtlasSprite sprite = model.stillMaterial().sprite();
		int color = model.tintSource() != null
				? model.tintSource().color(fluid.defaultFluidState().createLegacyBlock()) : 0xffffffff;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, bottom - height, 16, height, color);
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, (imageWidth - font.width(title)) / 2, 5, 0xff404040, false);
		graphics.text(font, playerInventoryTitle, 8, imageHeight - 94, 0xff404040, false);
		if (mouseX >= leftPos + 35 && mouseX < leftPos + 51 && mouseY >= topPos + 16 && mouseY < topPos + 70)
			graphics.setTooltipForNextFrame(font, List.of(Component.literal(menu.inputLevel() + " / "
					+ TileEntityCollector.CAPACITY + " mB XP fluid")), Optional.empty(), mouseX, mouseY);
		if (mouseX >= leftPos + 125 && mouseX < leftPos + 141 && mouseY >= topPos + 16 && mouseY < topPos + 70)
			graphics.setTooltipForNextFrame(font, List.of(Component.literal(menu.outputLevel() + " / "
					+ TileEntityCollector.CAPACITY + " mB Liquid Chroma")), Optional.empty(), mouseX, mouseY);
	}
}
