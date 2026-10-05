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
import reika.chromaticraft.models.ModelTurret;
import reika.chromaticraft.render.tesr.RenderLumenTurret;

import java.util.function.Consumer;

/**
 * V33a drew the Lumen Turret item with its TESR on a render-only tile: the model alone (the item pass skipped the spark),
 * scaled up 1.75x and raised 3/16 so the small turret fills the slot.
 */
public final class LumenTurretItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "lumen_turret");

	private final ModelTurret model;

	private LumenTurretItemRenderer(ModelTurret model) {
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		double d = 1.75;
		// V33a scaled about the render origin; centred here on the block's footprint.
		poseStack.translate(0.5F, 0, 0.5F);
		poseStack.scale((float)d, (float)d, (float)d);
		poseStack.translate(-0.5F, 0.1875F, -0.5F);
		RenderLumenTurret.submitModel(model, poseStack, collector, lightCoords);
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
		public LumenTurretItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new LumenTurretItemRenderer(new ModelTurret(context.entityModelSet().bakeLayer(RenderLumenTurret.MODEL_LAYER)));
		}
	}
}
