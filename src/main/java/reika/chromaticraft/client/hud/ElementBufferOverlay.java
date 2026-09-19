package reika.chromaticraft.client.hud;

import java.awt.Color;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.CrystalRuneTextures;
import reika.chromaticraft.item.ItemManipulator;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ChromaOverlays.renderElementPie}: the sixteen-wedge wheel showing what the player is
 * carrying, drawn while an Elemental Manipulator is in hand.
 *
 * <p>The wheel is deliberately not a pie chart. Every element owns a fixed 22.5 degree wedge and the
 * <em>radius</em> carries how full it is, so a colour never moves — you learn where each element sits
 * and read the shape at a glance. The radius uses {@code pow(amt/cap, 0.675)} rather than a linear
 * fraction, which makes a nearly-empty element still visible.
 *
 * <p>Holding shift enlarges it from 32 to 48, as upstream does.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ElementBufferOverlay implements GuiLayer {

	private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "element_buffer");
	private static final Identifier BACK = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/hud/wheelback_2.png");
	private static final Identifier FRONT = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/hud/wheelfront2.png");

	private static final ElementBufferOverlay INSTANCE = new ElementBufferOverlay();

	/** V33a: {@code int sp = 4} between the wheel and the screen edge. */
	private static final int MARGIN = 4;

	@SubscribeEvent
	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.CROSSHAIR, ID, INSTANCE);
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		// No hideGui check: a layer registered through RegisterGuiLayersEvent is already suppressed
		// with the rest of the HUD when the player hides it.
		if (player == null)
			return;
		ItemStack held = player.getMainHandItem();
		if (!(held.getItem() instanceof ItemManipulator))
			return;

		int r = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_LSHIFT) ? 48 : 32;
		int ox = pieX(graphics, r);
		int oy = pieY(graphics, r);

		// V33a draws both wheel plates at twice the radius, so the art frames the wedges.
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACK, ox - r * 2, oy - r * 2,
				0, 0, r * 4, r * 4, r * 4, r * 4);

		float flag = PlayerElementBuffer.instance.getAndDecrUpgradeTick(player);
		int cap = PlayerElementBuffer.instance.getElementCap(player);
		float[] radii = new float[CrystalElement.elements.length];
		int[] colours = new int[CrystalElement.elements.length];
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			int amt = PlayerElementBuffer.instance.getPlayerContent(player, e);
			radii[i] = (float)(r * Math.pow(cap == 0 ? 0 : (double)amt / cap, 0.675));
			colours[i] = flag > 0 ? pulse(e.getColor()) : e.getColor();
		}
		graphics.submitPictureInPictureRenderState(new ElementPieRenderState(radii, colours, r,
				ox - r, oy - r, ox + r, oy + r, graphics.peekScissorStack()));

		// V33a places each eight-pixel outline rune at the centre of its fixed 22.5-degree wedge.
		// Keep these below wheelfront2, whose rim and spokes deliberately finish the composition.
		int runeSize = 8;
		double runeRadius = 0.8125 * r;
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			double angle = 11.125 + i * 22.5;
			int x = (int)Math.round(ox - runeSize / 2D
					+ runeRadius * Math.cos(Math.toRadians(angle)));
			int y = (int)Math.round(oy - runeSize / 2D
					+ runeRadius * Math.sin(Math.toRadians(angle)));
			graphics.blit(RenderPipelines.GUI_TEXTURED,
					CrystalRuneTextures.outline(CrystalElement.elements[i]), x, y,
					0, 0, runeSize, runeSize, 16, 16);
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED, FRONT, ox - r * 2, oy - r * 2,
				0, 0, r * 4, r * 4, r * 4, r * 4);

		graphics.centeredText(mc.font, Component.literal("Cap: " + cap),
				ox, oy + r + mc.font.lineHeight - 4, 0xffffffff);
	}

	/** V33a brightens the whole wheel while a capacity upgrade flourish is running. */
	private static int pulse(int color) {
		float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
		int deg = (int)(System.currentTimeMillis() / 2 % 360);
		hsb[2] *= 0.75F + 0.25F * (float)Math.sin(Math.toRadians(deg));
		return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff;
	}

	/** V33a getPieX/getPieY: PIELOC picks which corner the wheel sits in. */
	private static int pieX(GuiGraphicsExtractor graphics, int r) {
		return ChromaOptions.PIELOC.getValue() < 2 ? r + MARGIN : graphics.guiWidth() - r - MARGIN;
	}

	private static int pieY(GuiGraphicsExtractor graphics, int r) {
		return ChromaOptions.PIELOC.getValue() % 2 == 0 ? r + MARGIN
				: graphics.guiHeight() - r - MARGIN - 16;
	}
}
