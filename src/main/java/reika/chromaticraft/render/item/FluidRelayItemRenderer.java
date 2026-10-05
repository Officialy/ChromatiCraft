package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelFluidRelay;
import reika.chromaticraft.render.tesr.RenderFluidRelay;

import java.util.function.Consumer;

/**
 * V33a drew the Fluid Link Anchor item with its TESR on a render-only tile: outside a world only the model (in its
 * default downward mounting, without the spinning cube, which belonged to the second render pass), 3x, raised 5/16.
 */
public final class FluidRelayItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "fluid_relay");

	private final ModelFluidRelay model;

	private FluidRelayItemRenderer(ModelFluidRelay model) {
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		float s = 3F;
		// V33a scaled about the render origin; centred here on the block's footprint.
		poseStack.translate(0.5F, 0, 0.5F);
		poseStack.scale(s, s, s);
		poseStack.translate(-0.5F, 0.3125F, -0.5F);
		RenderFluidRelay.submitModel(model, poseStack, collector, lightCoords);
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0, 0, 0));
		output.accept(new Vector3f(1, 1, 1));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public FluidRelayItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new FluidRelayItemRenderer(new ModelFluidRelay(context.entityModelSet().bakeLayer(RenderFluidRelay.MODEL_LAYER)));
		}
	}
}
