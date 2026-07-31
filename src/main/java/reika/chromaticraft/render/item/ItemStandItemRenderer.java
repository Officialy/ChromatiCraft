package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.tesr.RenderItemStand;

/** 26.2 special item model for the exact V33a casting-item-stand geometry. */
public final class ItemStandItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "item_stand");

	private final ModelPart model;

	private ItemStandItemRenderer(ModelPart model) {
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		// The in-world BER anchors the model at y=1.5 with a flipped Y, which maps the stand into
		// world y 0..1. Reusing that anchor here put the 0.72-scaled item model at y 0.875..1.595 --
		// entirely above the unit cube, so the icon floated above its slot. 0.14 is the bottom that
		// centres a 0.72-tall model: 0.5 - 0.72/2.
		poseStack.translate(0.5F, 0.14F, 0.5F);
		poseStack.scale(0.72F, -0.72F, -0.72F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180F));
		poseStack.translate(0F, -1.5F, 0F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RenderItemStand.STAND_TEXTURE),
				(pose, vertices) -> model.render(modelPose, vertices, lightCoords, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0F, 0F, 0F));
		output.accept(new Vector3f(1F, 1F, 1F));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public ItemStandItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new ItemStandItemRenderer(context.entityModelSet().bakeLayer(RenderItemStand.MODEL_LAYER));
		}
	}
}