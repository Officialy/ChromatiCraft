package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuAuraPouch;
import reika.chromaticraft.items.tools.ItemAuraPouch;

/**
 * V33a {@code GuiAuraPouch}: the basic small storage panel (drawn twice, as V33a's alpha-then-default passes did),
 * only the inventory label, and each pouch slot tinted green (active) or red (inactive) with a slowly pulsing,
 * per-slot-phased opacity. It does not pause the game.
 */
public final class ScreenAuraPouch extends AbstractContainerScreen<MenuAuraPouch> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/basicstorage_small.png");

	public ScreenAuraPouch(MenuAuraPouch menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, playerInventoryTitle, 8, imageHeight-96+2, 0xffffffff, false);
		ItemStack is = minecraft.player.getMainHandItem();
		if (is.getItem() instanceof ItemAuraPouch iap) {
			boolean[] active = iap.getActiveSlots(is);
			for (int i = 0; i < ItemAuraPouch.SIZE; i++) {
				int a = 96+(int)(48*Math.sin(System.currentTimeMillis()/400D+System.identityHashCode(menu.getSlot(i))));
				int c = (a << 24) | (active[i] ? 0x00ff00 : 0xff0000);
				int x = 8+(i%9)*18;
				int y = 17+(i/9)*18;
				g.fill(x, y, x+16, y+16, c);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

}
