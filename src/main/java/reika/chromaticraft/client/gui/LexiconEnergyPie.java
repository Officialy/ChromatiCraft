package reika.chromaticraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.Proportionality;

/**
 * The proportional wheel V33a's machine pages draw for a construct's energy cost.
 *
 * <p>Unlike the HUD's element wheel, this one really is a pie: each element's <em>angle</em> is its
 * share of the total. The drawing is DragonAPI's {@link Proportionality}, used as designed —
 * {@code setGeometry} then {@code render} — which needs a {@link SubmitNodeCollector}, so it rides a
 * picture-in-picture element like every other piece of filled geometry in GUI space.
 *
 * <p>The geometry is set in this element's own pixel space, centred on zero, because the
 * picture-in-picture pass already places the viewport for us.
 */
public final class LexiconEnergyPie {

	private LexiconEnergyPie() {}

	/**
	 * @param radius the wheel's radius in pixels; V33a uses 32 on the machine page
	 * @param spin   V33a re-seeds the zero angle per frame so the wedges rotate slowly
	 */
	public record State(
			Proportionality<CrystalElement> data,
			float radius,
			float spin,
			int x0,
			int y0,
			int x1,
			int y1,
			float scale,
			@Nullable ScreenRectangle scissorArea,
			@Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

		public State(Proportionality<CrystalElement> data, float radius, float spin,
				int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
			// scale 1: one model unit is one GUI pixel, so the radius below is in pixels.
			this(data, radius, spin, x0, y0, x1, y1, 1, scissorArea,
					PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
		}
	}

	@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
	public static final class Renderer extends PictureInPictureRenderer<State> {

		@SubscribeEvent
		public static void register(RegisterPictureInPictureRenderersEvent event) {
			event.register(State.class, Renderer::new);
		}

		@Override
		public Class<State> getRenderStateClass() {
			return State.class;
		}

		@Override
		protected String getTextureLabel() {
			return "energy pie";
		}

		@Override
		protected float getTranslateY(int height, int guiScale) {
			return height / 2F;
		}

		@Override
		protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
			state.data().setGeometry(0, 0, state.radius(), state.spin());
			state.data().render(collector, pose, CrystalElement.getColorMap());
		}
	}
}
