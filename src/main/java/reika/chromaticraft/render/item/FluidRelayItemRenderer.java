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
        RenderFluidRelay.submitItemModel(model,poseStack,collector,lightCoords);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		var pose=new PoseStack();RenderFluidRelay.itemTransform(pose);
        pose.translate(.5F,1.5F,.5F);pose.scale(1,-1,-1);model.getExtents(pose,output);
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
