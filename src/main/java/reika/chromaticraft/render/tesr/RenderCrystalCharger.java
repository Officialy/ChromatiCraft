package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelCrystalCharger;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;

/** Submit-pipeline port of V33a's rotating Charger body and counter-rotating stored crystal. */
public final class RenderCrystalCharger
		implements BlockEntityRenderer<TileEntityCrystalCharger, RenderCrystalCharger.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "crystal_charger"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/crystal_charger.png");

	private final ModelCrystalCharger model;
	private final ItemModelResolver itemModelResolver;

	public RenderCrystalCharger(BlockEntityRendererProvider.Context context) {
		model = new ModelCrystalCharger(context.bakeLayer(MODEL_LAYER));
		itemModelResolver = context.itemModelResolver();
	}

	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(TileEntityCrystalCharger charger, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(charger, state, partialTick, cameraPosition, breakProgress);
		state.angle = charger.getAngle(partialTick);
		state.item = null;
		ItemStack stack = charger.getItem(0);
		if (!stack.isEmpty()) {
			state.item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED,
					charger.getLevel(), null, Long.hashCode(charger.getBlockPos().asLong()));
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		float angle = state.angle;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (unused, vertices) -> {
			model.setRotorAngle(angle);
			model.render(modelPose, vertices, state.lightCoords, OverlayTexture.NO_OVERLAY);
		});
		poseStack.popPose();

		if (state.item != null) {
			for (float offset : new float[] {-0.1F, 0.1F}) {
				poseStack.pushPose();
				poseStack.translate(0.5F, 0.65F, 0.5F);
				poseStack.mulPose(Axis.YP.rotationDegrees(-state.angle));
				poseStack.translate(0, 0, offset);
				poseStack.scale(1.35F, 1.3F, 1.35F);
				state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				poseStack.popPose();
			}
		}
	}

	public static final class State extends BlockEntityRenderState {
		private float angle;
		private ItemStackRenderState item;
	}
}
