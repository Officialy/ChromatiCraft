package reika.chromaticraft.render.item;

import java.util.function.Consumer;

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
import reika.chromaticraft.models.ModelInfuser2;
import reika.chromaticraft.render.tesr.RenderInfuser3;

/** The authored Item Aura Infuser model in inventory, hand, frame, and dropped-item contexts. */
public final class ItemAuraInfuserRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "item_aura_infuser");
	private final ModelInfuser2 model;

	private ItemAuraInfuserRenderer(ModelInfuser2 model) { this.model = model; }

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.06F, 0.5F);
		poseStack.scale(0.88F, -0.88F, -0.88F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		poseStack.translate(0, -1.5F, 0);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RenderInfuser3.TEXTURE),
				(unused, vertices) -> model.render(modelPose, vertices, lightCoords,
						OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	@Override public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0)); output.accept(new Vector3f(1));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public ItemAuraInfuserRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new ItemAuraInfuserRenderer(new ModelInfuser2(
					context.entityModelSet().bakeLayer(RenderInfuser3.MODEL_LAYER)));
		}
	}
}
