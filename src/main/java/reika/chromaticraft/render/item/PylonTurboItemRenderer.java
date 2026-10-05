package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelTurbo;
import reika.chromaticraft.render.tesr.RenderPylonTurboCharger;

import java.util.function.Consumer;

/** The exact V33a Energy Focal Point model in inventories, hands, frames, and dropped form. */
public final class PylonTurboItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_turbo_charger");
	private final ModelTurbo model;

	private PylonTurboItemRenderer(ModelTurbo model) {
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.08F, 0.5F);
		poseStack.scale(0.84F, -0.84F, -0.84F);
		poseStack.rotate(Axis.YP.rotationDegrees(180));
		poseStack.translate(0, -1.5F, 0);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RenderPylonTurboCharger.TEXTURE),
				(unused, vertices) -> model.render(modelPose, vertices, lightCoords, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0));
		output.accept(new Vector3f(1));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public PylonTurboItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new PylonTurboItemRenderer(new ModelTurbo(
					context.entityModelSet().bakeLayer(RenderPylonTurboCharger.MODEL_LAYER)));
		}
	}
}
