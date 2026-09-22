package reika.chromaticraft.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

import reika.chromaticraft.ChromatiCraft;

/** V33a {@code MouseoverOverlayRenderer.renderStorageOverlay}'s coloured pie and dark rim. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class LumenStorageRenderer extends PictureInPictureRenderer<LumenStorageRenderState> {

	@SubscribeEvent
	public static void register(RegisterPictureInPictureRenderersEvent event) {
		event.register(LumenStorageRenderState.class, LumenStorageRenderer::new);
	}

	@Override public Class<LumenStorageRenderState> getRenderStateClass() { return LumenStorageRenderState.class; }
	@Override protected String getTextureLabel() { return "lumen storage"; }
	@Override protected float getTranslateY(int height, int guiScale) { return height / 2F; }

	@Override
	protected void renderToTexture(LumenStorageRenderState state, PoseStack pose,
			SubmitNodeCollector collector) {
		collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, vertices) -> {
			int count = state.colors().length;
			if (count == 0)
				return;
			for (int index = 0; index < count; index++) {
				double start = state.rotation() + index * Math.PI * 2 / count;
				double end = start + Math.PI * 2 / count;
				int color = state.colors()[index];
				int backing = (color & 0xfefefe) >> 2;
				sector(vertices, matrix, start, end, state.radius(), 0xff000000 | backing);
				sector(vertices, matrix, start, end,
						state.radius() * state.fractions()[index], 0xff000000 | color);
			}
			if (count > 1)
				for (int index = 0; index < count; index++) {
					double angle = state.rotation() + index * Math.PI * 2 / count;
					line(vertices, matrix, 0, 0,
							(float)(state.radius() * Math.cos(angle)),
							(float)(state.radius() * Math.sin(angle)), 0xff000000);
				}
			for (int step = 0; step < 72; step++) {
				double angle = step * Math.PI * 2 / 72;
				double next = (step + 1) * Math.PI * 2 / 72;
				line(vertices, matrix,
						(float)(state.radius() * Math.cos(angle)),
						(float)(state.radius() * Math.sin(angle)),
						(float)(state.radius() * Math.cos(next)),
						(float)(state.radius() * Math.sin(next)), 0xff000000);
			}
		});
	}

	private static void sector(com.mojang.blaze3d.vertex.VertexConsumer vertices,
			PoseStack.Pose pose, double start, double end, float radius, int color) {
		if (radius <= 0)
			return;
		for (double angle = start; angle < end; angle += Math.toRadians(2)) {
			double next = Math.min(end, angle + Math.toRadians(2));
			float ax = (float)(radius * Math.cos(angle));
			float ay = (float)(radius * Math.sin(angle));
			float bx = (float)(radius * Math.cos(next));
			float by = (float)(radius * Math.sin(next));
			vertices.addVertex(pose, 0, 0, 0).setColor(color);
			vertices.addVertex(pose, ax, ay, 0).setColor(color);
			vertices.addVertex(pose, bx, by, 0).setColor(color);
			vertices.addVertex(pose, 0, 0, 0).setColor(color);
		}
	}

	private static void line(com.mojang.blaze3d.vertex.VertexConsumer vertices,
			PoseStack.Pose pose, float x0, float y0, float x1, float y1, int color) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float length = (float)Math.sqrt(dx * dx + dy * dy);
		if (length < 1.0E-5F)
			return;
		float offsetX = -dy / length * 0.5F;
		float offsetY = dx / length * 0.5F;
		vertices.addVertex(pose, x0 + offsetX, y0 + offsetY, 0).setColor(color);
		vertices.addVertex(pose, x1 + offsetX, y1 + offsetY, 0).setColor(color);
		vertices.addVertex(pose, x1 - offsetX, y1 - offsetY, 0).setColor(color);
		vertices.addVertex(pose, x0 - offsetX, y0 - offsetY, 0).setColor(color);
	}
}
