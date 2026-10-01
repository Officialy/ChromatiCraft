/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuMEDistributor;
import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;
import reika.chromaticraft.network.ChromaNetwork.MEDistributorControl;
import reika.dragonapi.instantiable.gui.ImagedGuiButton;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

/**
 * V33a {@code GuiMEDistributor}: ten rows of query item, threshold box (coloured by the rule's match mode), match-mode
 * button and transfer item, in two columns. Hovering a slot names it ("Query" / "Transfer"), hovering the threshold
 * area describes its match mode.
 */
public final class ScreenMEDistributor extends AbstractContainerScreen<MenuMEDistributor> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/medistributor.png");
	private static final Identifier BUTTONS = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/buttons.png");

	private final TileEntityMEDistributor med;
	private final EditBox[] texts = new EditBox[TileEntityMEDistributor.NSLOTS];

	public ScreenMEDistributor(MenuMEDistributor menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 214, 215);
		med = menu.tile();
		inventoryLabelX = imageWidth - 76;
		inventoryLabelY = imageHeight - 97 + 3;
	}

	@Override
	protected void init() {
		super.init();
		int j = leftPos;
		int k = topPos;

		int w = 45;
		int h = 16;
		for (int i = 0; i < texts.length; i++) {
			int x = j + 27 + (i / 5) * 115;
			int y = k + 16 + (i % 5) * 20;
			EditBox box = new EditBox(font, x, y, w, h, Component.literal("threshold"));
			box.setFocused(false);
			box.setMaxLength(6);
			box.setValue(String.valueOf(med.getThreshold(i)));
			box.setTextColor(0xff000000 | med.getMode(i).color);
			int slot = i;
			box.setResponder(s -> this.send(MEDistributorControl.THRESHOLD, slot, Math.max(0, ReikaJavaLibrary.safeIntParse(s))));
			texts[i] = box;
			this.addRenderableWidget(box);
		}

		for (int i = 0; i < TileEntityMEDistributor.NSLOTS; i++) {
			int x = j + 94 + 16 * (i * 2 / TileEntityMEDistributor.NSLOTS);
			int y = k + 19 + 20 * (i % (TileEntityMEDistributor.NSLOTS / 2));
			int u = 70 + med.getMode(i).ordinal() * 10;
			int slot = i;
			ImagedGuiButton b = new ImagedGuiButton(i, x, y, 10, 10, u, 96, BUTTONS, btn -> {
				med.toggleFuzzy(slot); //V33a toggled locally too, so the button redraws at once
				this.send(MEDistributorControl.MODE, slot, 0);
				this.rebuildWidgets();
			});
			b.setTooltip(Tooltip.create(Component.literal("Match Mode")));
			this.addRenderableWidget(b);
		}
	}

	private void send(int action, int slot, int value) {
		ClientPacketDistributor.sendToServer(new MEDistributorControl(med.getBlockPos(), action, slot, value));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, (imageWidth - font.width(title)) / 2, 4, 0xffffffff, false);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffffffff, false);

		int w = 67;
		int w2 = 115;
		for (int i = 0; i < TileEntityMEDistributor.NSLOTS; i++) {
			int dx = 8;
			if (i >= 5)
				dx += w2;
			int dy = 7 + (i % 5) * 20 + 9;
			ItemStack transfer = med.getMapping(i);
			ItemStack query = med.getMapping(i + TileEntityMEDistributor.NSLOTS);
			if (!transfer.isEmpty())
				g.item(transfer, dx + w, dy);
			if (!query.isEmpty())
				g.item(query, dx, dy);
			int mx = mouseX - leftPos;
			int my = mouseY - topPos;
			String s = null;
			if (mx >= dx - 1 && mx <= dx + 16 && my >= dy - 1 && my <= dy + 17)
				s = "Query";
			else if (mx >= dx + w - 1 && mx <= dx + w + 16 && my >= dy - 1 && my <= dy + 17)
				s = "Transfer";
			else if (mx >= dx + 17 && mx <= dx + w - 2 && my >= dy - 1 && my <= dy + 17)
				s = med.getMode(i).desc;
			if (s != null)
				g.setTooltipForNextFrame(Component.literal(s), mx, my);
		}
	}

}
