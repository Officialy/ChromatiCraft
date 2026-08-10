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
import reika.chromaticraft.registry.CrystalElement;

/**
 * Draws {@link ElementPieRenderState}'s sixteen wedges and the divider lines between them.
 *
 * <p>The base pass leaves model +X pointing right and model +Y pointing <em>down</em>, which is the
 * same handedness V33a's GUI space has — so the angles here are written exactly as upstream writes
 * them, {@code cos} across and {@code sin} down, with no flip. That is deliberate: the wheel is flat,
 * so unlike the structure viewer there is nothing to orient.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ElementPieRenderer extends PictureInPictureRenderer<ElementPieRenderState> {

	/** V33a walks each wedge in two-degree steps. */
	private static final double STEP = 2;
	private static final double SECTOR = 360D / CrystalElement.elements.length;

	/** V33a: {@code GL11.glLineWidth(2)} for the spokes. */
	private static final float SPOKE_WIDTH = 2;

	@SubscribeEvent
	public static void register(RegisterPictureInPictureRenderersEvent event) {
		event.register(ElementPieRenderState.class, ElementPieRenderer::new);
	}

	@Override
	public Class<ElementPieRenderState> getRenderStateClass() {
		return ElementPieRenderState.class;
	}

	@Override
	protected String getTextureLabel() {
		return "element pie";
	}

	/** The wheel is centred on itself, not standing on a floor. */
	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2F;
	}

	@Override
	protected void renderToTexture(ElementPieRenderState state, PoseStack pose, SubmitNodeCollector collector) {
		collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) -> {
			for (int i = 0; i < state.radii().length; i++) {
				double min = i * SECTOR;
				double dr = state.radii()[i];
				if (dr <= 0)
					continue;
				int colour = 0xff000000 | state.colours()[i];
				for (double a = min; a < min + SECTOR; a += STEP) {
					double b = Math.min(a + STEP, min + SECTOR);
					float ax = (float)(dr * Math.cos(Math.toRadians(a)));
					float ay = (float)(dr * Math.sin(Math.toRadians(a)));
					float bx = (float)(dr * Math.cos(Math.toRadians(b)));
					float by = (float)(dr * Math.sin(Math.toRadians(b)));
					// A quad with the centre doubled is the triangle upstream's strip produces.
					buffer.addVertex(matrix, 0, 0, 0).setColor(colour);
					buffer.addVertex(matrix, ax, ay, 0).setColor(colour);
					buffer.addVertex(matrix, bx, by, 0).setColor(colour);
					buffer.addVertex(matrix, 0, 0, 0).setColor(colour);
				}
			}
		});

		// V33a's black spokes, at the wheel's full radius rather than each wedge's, drawn two pixels
		// wide as upstream's glLineWidth(2) does.
		//
		// They are quads, not RenderTypes.lines(): that pipeline binds
		// POSITION_COLOR_NORMAL_LINE_WIDTH, so a vertex carrying only position and colour trips
		// "Missing elements in vertex" and takes the frame down. Quads also keep the whole element on
		// one render type.
		collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) -> {
			for (int i = 0; i < CrystalElement.elements.length; i++) {
				double a = Math.toRadians(i * SECTOR);
				float cx = (float)Math.cos(a);
				float cy = (float)Math.sin(a);
				// Perpendicular, half the line width either side.
				float px = -cy * SPOKE_WIDTH / 2;
				float py = cx * SPOKE_WIDTH / 2;
				float ex = cx * state.radius();
				float ey = cy * state.radius();
				buffer.addVertex(matrix, px, py, 0).setColor(0xff000000);
				buffer.addVertex(matrix, ex + px, ey + py, 0).setColor(0xff000000);
				buffer.addVertex(matrix, ex - px, ey - py, 0).setColor(0xff000000);
				buffer.addVertex(matrix, -px, -py, 0).setColor(0xff000000);
			}
		});
	}
}
