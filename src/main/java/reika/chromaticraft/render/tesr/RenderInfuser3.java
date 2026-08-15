package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelInfuser2;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.recipe.TileEntityAuraInfuser;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Submit-pipeline port of V33a's exact infuser body, floating item, and eight cycling rays. */
public final class RenderInfuser3 implements BlockEntityRenderer<TileEntityAuraInfuser, RenderInfuser3.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "item_aura_infuser"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/item_aura_infuser.png");

	private final ModelInfuser2 model;
	private final ItemModelResolver itemResolver;

	public RenderInfuser3(BlockEntityRendererProvider.Context context) {
		model = new ModelInfuser2(context.bakeLayer(MODEL_LAYER));
		itemResolver = context.itemModelResolver();
	}

	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(TileEntityAuraInfuser infuser, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(infuser, state, partialTick, cameraPosition, breakProgress);
		state.tick = infuser.getTicksExisted() + partialTick;
		state.item = null;
		ItemStack stack = infuser.getRenderItem();
		if (!stack.isEmpty()) {
			state.item = new ItemStackRenderState();
			itemResolver.updateForTopItem(state.item, stack, ItemDisplayContext.GROUND,
					infuser.getLevel(), null, Long.hashCode(infuser.getBlockPos().asLong()));
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack bodyPose = copy(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
				(unused, vertices) -> model.render(bodyPose, vertices, state.lightCoords,
						OverlayTexture.NO_OVERLAY));
		poseStack.popPose();

		if (state.item == null) return;
		float angle = state.tick * 3F % 360F;
		double bob = 0.0625 * Math.sin(Math.toRadians(angle * 2));
		poseStack.pushPose();
		poseStack.translate(0.5, 0.6875 + bob, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(angle));
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();

		float mix = 1F - ((int)state.tick & 3) / 4F;
		PoseStack linesPose = copy(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (unused, vertices) -> {
			for (int degrees = 0; degrees < 360; degrees += 45) {
				int index = (((int)state.tick / 4) + degrees / 45) & 15;
				int next = (index + 1) & 15;
				int rgb = ReikaColorAPI.mixColors(CrystalElement.elements[index].getColor(),
						CrystalElement.elements[next].getColor(), mix);
				double radians = Math.toRadians(degrees);
				Vector3f normal = new Vector3f((float)(0.5 * Math.cos(radians)), -0.3375F,
						(float)(0.5 * Math.sin(radians))).normalize();
				line(vertices, linesPose.last(), radians, normal, 6, (70 << 24) | rgb);
				line(vertices, linesPose.last(), radians, normal, 3, (150 << 24) | rgb);
				line(vertices, linesPose.last(), radians, normal, 1, 0xff000000 | rgb);
			}
		});
	}

	private static void line(VertexConsumer out, PoseStack.Pose pose, double angle, Vector3f normal,
			float width, int color) {
		out.addVertex(pose, 0.5F, 0.6875F, 0.5F).setColor(color)
				.setNormal(pose, normal.x, normal.y, normal.z).setLineWidth(width);
		out.addVertex(pose, (float)(0.5 + 0.5 * Math.cos(angle)), 0.35F,
				(float)(0.5 + 0.5 * Math.sin(angle))).setColor(color)
				.setNormal(pose, normal.x, normal.y, normal.z).setLineWidth(width);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	public static final class State extends BlockEntityRenderState {
		private float tick;
		private ItemStackRenderState item;
	}
}
