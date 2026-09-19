package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.models.ModelRelaySource;
import reika.chromaticraft.render.tesr.RenderRelaySource;
import reika.chromaticraft.ChromatiCraft;

/** The V33a Relay Source Techne model in inventory, hand, frame and dropped-item contexts. */
public final class RelaySourceItemRenderer implements NoDataSpecialModelRenderer {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "relay_source");
	private final ModelRelaySource model;
	private RelaySourceItemRenderer(ModelRelaySource model) { this.model = model; }

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.05F, 0.5F);
		poseStack.scale(0.88F, -0.88F, -0.88F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		poseStack.translate(0, -1.5F, 0);
		RenderRelaySource.submitItemModel(poseStack, collector, model, lightCoords, 0xffffffff);
		poseStack.popPose();
	}

	@Override public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0)); output.accept(new Vector3f(1));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public RelaySourceItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new RelaySourceItemRenderer(new ModelRelaySource(
					context.entityModelSet().bakeLayer(RenderRelaySource.MODEL_LAYER)));
		}
	}
}
