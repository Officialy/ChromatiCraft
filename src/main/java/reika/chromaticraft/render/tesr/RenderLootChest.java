package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.models.ModelLootChest;
import reika.chromaticraft.tileentity.TileEntityLootChest;

/** Submit-pipeline port of V33a's animated loot-chest TESR. */
public final class RenderLootChest implements BlockEntityRenderer<TileEntityLootChest, RenderLootChest.State> {
	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "loot_chest"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/loot_chest.png");
	private final ModelLootChest model;

	public RenderLootChest(BlockEntityRendererProvider.Context context) {
		model = new ModelLootChest(context.bakeLayer(MODEL_LAYER));
	}

	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(TileEntityLootChest chest, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(chest, state, partialTick, cameraPosition, breakProgress);
		state.facing = chest.getBlockState().getValue(BlockLootChest.FACING);
		float open = chest.getOpenNess(partialTick);
		state.open = 1F - (float)Math.pow(1F - open, 3);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0, 0.5F);
		poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
		poseStack.translate(-0.5F, 0, -0.5F);
		model.setLidRotation(-state.open * ((float)Math.PI / 2F));
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
				(unused, vertices) -> model.render(modelPose, vertices, state.lightCoords, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	public static final class State extends BlockEntityRenderState {
		private Direction facing = Direction.NORTH;
		private float open;
	}
}
