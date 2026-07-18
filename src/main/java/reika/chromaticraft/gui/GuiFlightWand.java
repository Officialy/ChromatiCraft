/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.render.ChromaFontRenderer;
import reika.chromaticraft.base.GuiChromaTool;
import reika.chromaticraft.block.blockhoverblock.HoverType;
import reika.chromaticraft.items.tools.wands.ItemFlightWand;
import reika.chromaticraft.registry.ChromaPackets;
import reika.dragonapi.instantiable.gui.customsoundguibutton.CustomSoundImagedGuiButton;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaTextureHelper;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;

public class GuiFlightWand extends GuiChromaTool {

	private final int xSize = 111;
	private final int ySize = 166;

	public GuiFlightWand(EntityPlayer ep) {
		super(ep);
	}

	@Override
	public void initGui() {
		super.initGui();
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		String tex = "Textures/GUIs/buttons.png";

		int l = HoverType.list.length;
		HoverType md = this.getCurrentMode();
		for (int i = 0; i < l; i++) {
			int u = md.ordinal() == i ? 18 : 0;
			GuiButton b = new CustomSoundImagedGuiButton(i, j+8, k+14+i*40, 18, 18, u, 0, tex, ChromatiCraft.class, this);
			b.packedFGColour = md.renderColor;
			buttonList.add(b);
		}
	}

	@Override
	protected void actionPerformed(GuiButton b) {
		ItemFlightWand.setMode(player.getCurrentEquippedItem(), HoverType.list[b.id]);
		ReikaPacketHelper.sendDataPacket(ChromatiCraft.packetChannel, ChromaPackets.HOVERWAND.ordinal(), PacketTarget.server, b.id);
		this.initGui();
	}

	@Override
	public void drawScreen(int x, int y, float ptick) {
		super.drawScreen(x, y, ptick);
		GL11.glPushMatrix();
		GL11.glTranslated(0, 0, -500);
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		ReikaTextureHelper.bindTexture(ChromatiCraft.class, "Textures/GUIs/hoverwand.png");
		ReikaGuiAPI.instance.drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
		int tx = j+xSize/2;
		int ty = k+4;
		ReikaGuiAPI.instance.drawCenteredStringNoShadow(ChromaFontRenderer.FontType.GUI.renderer, "Hover Selection", tx, ty, 0xffffff);

		int l = HoverType.list.length;
		HoverType md = this.getCurrentMode();
		for (int i = 0; i < l; i++) {
			int color = HoverType.list[i].renderColor;//md.ordinal() == i ? 0x00ff00 : 0xffffff;
			if (md.ordinal() == i) {
				color = 0xffffff;
			}
			tx = j+30;
			ty = k+19+i*40;
			ChromaFontRenderer.FontType.GUI.renderer.drawString(HoverType.list[i].desc, tx, ty, color);
		}
		GL11.glPopMatrix();
	}

	private HoverType getCurrentMode() {
		return ItemFlightWand.getMode(player.getCurrentEquippedItem());
	}

}
