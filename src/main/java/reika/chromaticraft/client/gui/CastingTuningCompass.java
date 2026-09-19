package reika.chromaticraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;

import java.util.List;

/**
 * Filled square compass used by V33a's Personalized Casting lexicon page.
 *
 * <p>The tuning key deliberately has no cardinal entries. Each of the other twelve directions owns
 * one 22.5-degree annular sector, leaving the four gaps drawn into {@code handbook_casttune.png}
 * clear. Both radii are square radii: at a diagonal the ray reaches a corner at
 * {@code radius * sqrt(2)}, exactly as DragonAPI's old {@code Compass.squareRender} did.
 */
public final class CastingTuningCompass {

	private static final double HALF_SECTOR = 11.25;
	private static final double STEP = 0.25;

	private CastingTuningCompass() {}

	public record Sector(double angle, int colour) {}

	public record State(
			List<Sector> sectors,
			float outerRadius,
			float innerRadius,
			int x0,
			int y0,
			int x1,
			int y1,
			float scale,
			@Nullable ScreenRectangle scissorArea,
			@Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

		public State(List<Sector> sectors, float outerRadius, float innerRadius,
				int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
			this(List.copyOf(sectors), outerRadius, innerRadius, x0, y0, x1, y1, 1,
					scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
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
			return "casting tuning compass";
		}

		@Override
		protected float getTranslateY(int height, int guiScale) {
			return height / 2F;
		}

		@Override
		protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
			collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) -> {
				for (Sector sector : state.sectors()) {
					double end = sector.angle() + HALF_SECTOR;
					for (double angle = sector.angle() - HALF_SECTOR; angle < end; angle += STEP) {
						double next = Math.min(angle + STEP, end);
						double a = Math.toRadians(angle);
						double b = Math.toRadians(next);
						float ai = (float)squareRadius(state.innerRadius(), a);
						float ao = (float)squareRadius(state.outerRadius(), a);
						float bi = (float)squareRadius(state.innerRadius(), b);
						float bo = (float)squareRadius(state.outerRadius(), b);
						int colour = 0xff000000 | sector.colour();
						buffer.addVertex(matrix, ai * (float)Math.cos(a), ai * (float)Math.sin(a), 0)
								.setColor(colour);
						buffer.addVertex(matrix, ao * (float)Math.cos(a), ao * (float)Math.sin(a), 0)
								.setColor(colour);
						buffer.addVertex(matrix, bo * (float)Math.cos(b), bo * (float)Math.sin(b), 0)
								.setColor(colour);
						buffer.addVertex(matrix, bi * (float)Math.cos(b), bi * (float)Math.sin(b), 0)
								.setColor(colour);
					}
				}
			});
		}

		private static double squareRadius(double radius, double angle) {
			return radius * Math.min(Math.abs(1D / Math.cos(angle)), Math.abs(1D / Math.sin(angle)));
		}
	}
}
