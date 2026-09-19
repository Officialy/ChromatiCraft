package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityStructureController;

/**
 * V33a {@code RenderStructControl}: the fragment structure's root is not a solid block but a glowing
 * flare hanging in the air, additively blended and turning on its own.
 *
 * <p>Upstream's flare is {@code ChromaIcons.SPINFLARE}, a 64x11520 strip of 180 frames stepped one
 * per tick. The strip is used directly here rather than through the block atlas: it is referenced by
 * no model, so it is not stitched, and walking the V offset by hand reproduces the animation without
 * needing an atlas source registered for one texture.
 *
 * <p>CHROMA-PORT: upstream also draws the monument line ring over this (the
 * {@code monument_lines_big.png} pass and the {@code structcontrol} shader), and gates the whole
 * thing on {@code isVisible}/{@code isMonument}/{@code isInWorld}. None of those flags exist on the
 * ported block entity, so the flare draws unconditionally for now; restore the gating and the
 * monument pass with the monument ritual.
 */
public final class RenderStructureController
		implements BlockEntityRenderer<TileEntityStructureController, RenderStructureController.State> {

	private static final Identifier FLARE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/icons/rotating_flare_pulse.png");

	/** The strip is 64 wide and 11520 tall: 180 square frames, one tick each. */
	private static final int FRAMES = 180;
	private static final float FRAME_V = 1F / FRAMES;

	/** V33a draws the flare a little over a block across, centred on the block. */
	private static final float RADIUS = 0.75F;

	public RenderStructureController(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityStructureController controller, State state,
			float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(controller, state, partialTick, cameraPosition, breakProgress);
		CrystalElement colour = controller.getColor();
		state.colour = colour == null ? 0xffffff : colour.getColor();
		// Wall clock, not a tick counter: the controller is not ticked client-side in every state, and
		// upstream's flare spins regardless of whether the structure is doing anything.
		state.frame = (int)(System.currentTimeMillis() / 50 % FRAMES);
		// Face the camera, so the flare reads as a light source rather than a flat decal.
		state.yaw = (float)Math.toDegrees(Math.atan2(
				cameraPosition.x - (controller.getBlockPos().getX() + 0.5),
				cameraPosition.z - (controller.getBlockPos().getZ() + 0.5)));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(state.yaw));
		PoseStack.Pose pose = poseStack.last();
		float v0 = state.frame * FRAME_V;
		float v1 = v0 + FRAME_V;
		int colour = 0xffffffff & (0xff000000 | state.colour);
		// Emissive translucent: additive-looking against the world without needing a pipeline modifier,
		// and one render type for the whole element.
		submitAfterTerrain(poseStack, collector, ChromaRenderPipelines.additiveSprite(FLARE),
				(matrix, buffer) -> {
					buffer.addVertex(pose, -RADIUS, -RADIUS, 0).setColor(colour).setUv(0, v1)
							.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
							.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
					buffer.addVertex(pose, RADIUS, -RADIUS, 0).setColor(colour).setUv(1, v1)
							.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
							.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
					buffer.addVertex(pose, RADIUS, RADIUS, 0).setColor(colour).setUv(1, v0)
							.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
							.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
					buffer.addVertex(pose, -RADIUS, RADIUS, 0).setColor(colour).setUv(0, v0)
							.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
							.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
				});
		poseStack.popPose();
	}

	private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
			RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
				poseStack.last().copy(), renderType, renderer);
		((OrderedSubmitNodeCollectorExtension)collector.order(0))
				.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	public static final class State extends BlockEntityRenderState {
		int colour = 0xffffff;
		int frame;
		float yaw;
	}
}
