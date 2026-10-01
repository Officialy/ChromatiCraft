package reika.chromaticraft.render.item;

import java.util.function.Consumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelMEDistributor;
import reika.chromaticraft.render.tesr.RenderMEDistributor;

/**
 * V33a drew the ME Item Distributor item with its TESR on a render-only tile: the upward-mounted model with its
 * posts squashed ({@code isInWorld() == false}).
 */
public final class MEDistributorItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "me_distributor");

	private final ModelMEDistributor model;

	private MEDistributorItemRenderer(ModelMEDistributor model) {
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		RenderMEDistributor.submitModel(model, poseStack, collector, lightCoords, false);
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
		public MEDistributorItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new MEDistributorItemRenderer(new ModelMEDistributor(context.entityModelSet().bakeLayer(RenderMEDistributor.MODEL_LAYER)));
		}
	}
}
