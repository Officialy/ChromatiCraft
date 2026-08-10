package reika.chromaticraft.client.hud;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import org.jspecify.annotations.Nullable;

/**
 * The filled part of V33a's element wheel: sixteen sectors of fixed angle whose <em>radius</em>
 * carries the fill, rather than the other way round.
 *
 * <p>It is a picture-in-picture element for the same reason everything else 3D-or-geometric in this
 * port is: {@code GuiGraphicsExtractor} offers rectangles, blits and text, and nothing that can fill
 * a wedge. The two wheel textures around it are ordinary blits and stay outside this.
 *
 * @param radii   per element, the sector's radius in pixels, already scaled by V33a's 0.675 exponent
 * @param colours per element, the sector colour, already shifted if an upgrade flourish is running
 * @param radius  the wheel's full radius, which the divider lines are drawn to
 */
public record ElementPieRenderState(
		float[] radii,
		int[] colours,
		float radius,
		int x0,
		int y0,
		int x1,
		int y1,
		float scale,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

	public ElementPieRenderState(float[] radii, int[] colours, float radius,
			int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
		// scale 1: one model unit is one GUI pixel, so the geometry below is written in pixels.
		this(radii, colours, radius, x0, y0, x1, y1, 1,
				scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
	}
}
