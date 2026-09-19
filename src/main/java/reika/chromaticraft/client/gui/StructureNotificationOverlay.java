package reika.chromaticraft.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.ElementEncodedNumber;
import reika.chromaticraft.client.CrystalRuneTextures;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.DimensionStructureType;

/**
 * Modern render-state port of V33a's Proxima structure overlays: the short name announcement at an
 * entrance and the seven-second, progressively revealed rune password recovered from a core.
 */
public final class StructureNotificationOverlay implements GuiLayer {

	private static final StructureNotificationOverlay INSTANCE = new StructureNotificationOverlay();
	private static final Identifier ID = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "structure_notifications");
	private static final int ENTRY_LIFESPAN = 100;
	private static final int PASSWORD_LIFESPAN = 700;

	private int entryType = -1;
	private long entryStarted = Long.MIN_VALUE;
	private ElementEncodedNumber password;
	private long passwordStarted = Long.MIN_VALUE;

	private StructureNotificationOverlay() {}

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.TITLE, ID, INSTANCE);
	}

	public static void enter(int structureType) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || structureType < 0 || structureType >= DimensionStructureType.types.length)
			return;
		INSTANCE.entryType = structureType;
		INSTANCE.entryStarted = mc.level.getGameTime();
	}

	public static void showPassword(int value) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		INSTANCE.password = new ElementEncodedNumber(value, 8);
		INSTANCE.passwordStarted = mc.level.getGameTime();
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		long now = mc.level.getGameTime();
		renderEntry(graphics, mc, (int)(now - entryStarted));
		renderPassword(graphics, (int)(now - passwordStarted));
	}

	private void renderEntry(GuiGraphicsExtractor graphics, Minecraft mc, int age) {
		if (entryType < 0 || age < 0 || age >= ENTRY_LIFESPAN)
			return;
		String name = DimensionStructureType.types[entryType].getDisplayText();
		if (name == null || name.isBlank())
			return;
		int color = ((int)(255 * fade(age, ENTRY_LIFESPAN)) << 24) | 0xffffff;
		int width = mc.font.width(name);
		graphics.text(mc.font, name, (graphics.guiWidth() - width) / 2,
				graphics.guiHeight() / 3, color, true);
	}

	private void renderPassword(GuiGraphicsExtractor graphics, int age) {
		if (password == null || age < 0 || age >= PASSWORD_LIFESPAN)
			return;
		float alpha = fade(age, PASSWORD_LIFESPAN);
		// FullScreenOverlayRenderer used a slightly lighter bottom edge and doubled the group alpha
		// for the backdrop. Preserve its exact C0101010 -> D0101010 gradient instead of a flat black
		// veil; this is part of the core-recovery presentation, not generic HUD dimming.
		float backgroundAlpha = Math.min(alpha * 2, 1);
		int top = Mth.clamp((int)(0xC0 * backgroundAlpha), 0, 0xC0) << 24 | 0x101010;
		int bottom = Mth.clamp((int)(0xD0 * backgroundAlpha), 0, 0xD0) << 24 | 0x101010;
		graphics.fillGradient(0, 0, graphics.guiWidth(), graphics.guiHeight(), top, bottom);

		int shown = Math.min(password.getLength(), age / 5);
		// V33a translated around a fifteen-pixel lore cell, then scaled the complete rune group by two.
		// Express that transform directly in GUI coordinates so the submit renderer receives final bounds.
		int runeSize = 32;
		int spacing = 48;
		int startX = graphics.guiWidth() / 2 - password.getLength() * spacing / 2 + 9;
		int y = graphics.guiHeight() / 2 - 13;
		int runeAlpha = Mth.clamp((int)(alpha * 255), 0, 255);
		int frameAlpha = age < PASSWORD_LIFESPAN - 100 ? 255
				: Mth.clamp((PASSWORD_LIFESPAN - age + 1) * 255 / 100, 0, 255);
		for (int i = 0; i < shown; i++) {
			CrystalElement element = password.getSlot(i);
			int x = startX + i * spacing;
			Identifier rune = CrystalRuneTextures.outline(element);
			float brightness = Math.min(1F,
					1F + (float)(0.625 * Math.sin(System.currentTimeMillis() / 300D - i)));
			int channel = Mth.clamp((int)(brightness * 255), 0, 255);
			int tint = runeAlpha << 24 | channel << 16 | channel << 8 | channel;
			graphics.blit(RenderPipelines.GUI_TEXTURED, rune, x, y, 0, 0,
					runeSize, runeSize, 16, 16, 16, 16, tint);
			for (int d = 0; d <= 2; d++) {
				float frameBrightness = 1 - brightness * d / 3F;
				int frameRgb = reika.dragonapi.libraries.rendering.ReikaColorAPI
						.getColorWithBrightnessMultiplier(element.getColor(), frameBrightness);
				int color = frameAlpha << 24 | frameRgb & 0xffffff;
				int offset = d * 2;
				graphics.outline(x - offset, y - offset,
						runeSize + offset * 2, runeSize + offset * 2, color);
			}
		}
	}

	/** V33a FullElementRender: fade in for the first eighth, fade out over the latter half. */
	private static float fade(int age, int lifespan) {
		if (age < lifespan / 8F)
			return age / (lifespan / 8F);
		if (age >= lifespan / 2F)
			return 2F * (lifespan - age) / lifespan;
		return 1;
	}
}
