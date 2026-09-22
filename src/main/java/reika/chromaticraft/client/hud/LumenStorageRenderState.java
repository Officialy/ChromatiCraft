package reika.chromaticraft.client.hud;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import org.jspecify.annotations.Nullable;

/** V33a Manipulator mouseover wheel for a tile's actual stored and maximum lumen. */
public record LumenStorageRenderState(
		int[] colors,
		float[] fractions,
		float radius,
		float rotation,
		int x0,
		int y0,
		int x1,
		int y1,
		float scale,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

	public LumenStorageRenderState(int[] colors, float[] fractions, float radius, float rotation,
			int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
		this(colors, fractions, radius, rotation, x0, y0, x1, y1, 1,
				scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
	}
}
