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

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelLootChest;
import reika.chromaticraft.render.tesr.RenderLootChest;

/** V33a loot chest's real model in GUI, hand, frame and dropped-item contexts. */
public final class LootChestItemRenderer implements NoDataSpecialModelRenderer {
	public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "loot_chest");
	private final ModelLootChest model;
	private LootChestItemRenderer(ModelLootChest model) { this.model = model; }

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0, 0.5F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		poseStack.translate(-0.5F, 0, -0.5F);
		model.setLidRotation(0);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RenderLootChest.TEXTURE),
				(unused, vertices) -> model.render(modelPose, vertices, lightCoords, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}
	@Override public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0)); output.accept(new Vector3f(1));
	}
	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public LootChestItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new LootChestItemRenderer(new ModelLootChest(
					context.entityModelSet().bakeLayer(RenderLootChest.MODEL_LAYER)));
		}
	}
}
