package reika.chromaticraft.client.gui;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * V33a {@code GuiScrollingPage}: the pannable backdrop the guide's navigation and progress pages sit
 * on.
 *
 * <p>Movement is not a key event. V33a polls the held movement keys every frame and slides the page
 * by {@code max(1, 180 / fps)} pixels, so panning is smooth and frame-rate independent rather than
 * stepping once per keypress — holding W drifts the page upward continuously. Shift doubles that
 * step and Ctrl halves it. The player's own movement binds are read rather than hardcoded WASD, so a
 * remapped keyboard still works, with the arrow keys always available alongside them.
 *
 * <p>The backdrop itself is a 256x256 tile drawn at {@code offset % 256}, which is what makes it
 * appear to scroll continuously under the window cut into the frame above it.
 */
public final class LexiconScrollPane {

	/** V33a GuiNavigation: {@code super(BOOKNAV, ep, 256, 220, 242, 206)}. */
	public static final int PANE_WIDTH = 242;
	public static final int PANE_HEIGHT = 206;

	/** The scrolling art is a single 256x256 tile. */
	private static final int TILE = 256;

	/** V33a draws the pane inset one pixel up and seven right of the frame origin. */
	private static final int INSET_X = 7;
	private static final int INSET_Y = -1;

	private int offsetX;
	private int offsetY;
	private int maxX = -1;
	private int maxY = -1;

	/** Negative bounds mean "unbounded on this axis", which is V33a's default. */
	public void setBounds(int maxX, int maxY) {
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public void reset() {
		offsetX = 0;
		offsetY = 0;
	}

	public int offsetX() {
		return offsetX;
	}

	public int offsetY() {
		return offsetY;
	}

	/** Nudges the pane, used by the wheel so it drives the same offset the keys do. */
	public void scrollBy(int dx, int dy) {
		offsetX = Math.max(0, maxX >= 0 ? Math.min(offsetX + dx, maxX) : offsetX + dx);
		offsetY = Math.max(0, maxY >= 0 ? Math.min(offsetY + dy, maxY) : offsetY + dy);
	}

	/**
	 * V33a's per-frame poll. Call once per rendered frame, before drawing, exactly where upstream
	 * does it at the top of {@code drawScreen}.
	 */
	public void pan() {
		Minecraft mc = Minecraft.getInstance();
		com.mojang.blaze3d.platform.Window window = mc.getWindow();
		int step = Math.max(1, 180 / Math.max(1, mc.getFps()));
		if (hasShiftDown(window))
			step *= 2;
		else if (hasControlDown(window))
			step = Math.max(1, step / 2);

		if (down(window, mc.options.keyUp) || down(window, InputConstants.KEY_UP))
			offsetY -= step;
		if (down(window, mc.options.keyDown) || down(window, InputConstants.KEY_DOWN))
			offsetY += step;
		if (down(window, mc.options.keyLeft) || down(window, InputConstants.KEY_LEFT))
			offsetX -= step;
		if (down(window, mc.options.keyRight) || down(window, InputConstants.KEY_RIGHT))
			offsetX += step;

		offsetX = Math.max(0, offsetX);
		offsetY = Math.max(0, offsetY);
		if (maxX >= 0)
			offsetX = Math.min(offsetX, maxX);
		if (maxY >= 0)
			offsetY = Math.min(offsetY, maxY);
	}

	/** Draws the scrolling backdrop; the frame is blitted over this afterwards. */
	public void render(GuiGraphicsExtractor graphics, Identifier texture, int leftX, int topY) {
		this.render(graphics, texture, leftX, topY, PANE_WIDTH, PANE_HEIGHT);
	}

	/** Same V33a tiled pane, with the caller's screen-specific aperture dimensions. */
	public void render(GuiGraphicsExtractor graphics, Identifier texture, int leftX, int topY,
			int paneWidth, int paneHeight) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
				leftX + INSET_X, topY + INSET_Y,
				Math.floorMod(offsetX, TILE), Math.floorMod(offsetY, TILE),
				paneWidth, paneHeight, TILE, TILE);
	}

	private static boolean down(com.mojang.blaze3d.platform.Window window, net.minecraft.client.KeyMapping mapping) {
		InputConstants.Key key = mapping.getKey();
		return key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()
				&& InputConstants.isKeyDown(window, key.getValue());
	}

	private static boolean down(com.mojang.blaze3d.platform.Window window, int keyCode) {
		return InputConstants.isKeyDown(window, keyCode);
	}

	private static boolean hasShiftDown(com.mojang.blaze3d.platform.Window window) {
		return down(window, InputConstants.KEY_LSHIFT) || down(window, InputConstants.KEY_RSHIFT);
	}

	private static boolean hasControlDown(com.mojang.blaze3d.platform.Window window) {
		return down(window, InputConstants.KEY_LCONTROL) || down(window, InputConstants.KEY_RCONTROL);
	}
}
