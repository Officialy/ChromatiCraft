package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Modern submit port of V33a RenderLocusPoint's three-layer Dimension Core glow knot. */
public final class RenderDimensionCore implements
		BlockEntityRenderer<TileEntityDimensionCore, RenderDimensionCore.State> {

	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/effect/aurapoint2-grid.png");

	public RenderDimensionCore(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityDimensionCore core, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPosition,
				breakProgress);
		state.color = core.getRenderColor();
		state.frame = (int)(System.currentTimeMillis() / 250 % 80);
		state.ritual = reika.chromaticraft.client.render.MonumentRitualEffects.isRunning();
		reika.chromaticraft.client.render.LocusPointScreenEffects.addDimensionCore(core);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(camera.orientation);
		// Dimension cores remain supernatural landmarks through terrain outside the monument ritual as
		// well. Their matching DIMCORE post effect is likewise intentionally not LOS-culled.
		submitLayers(poseStack, collector, state.color, state.frame, true, true);
		poseStack.popPose();
	}

	/** Shared by the block and its special item renderer; V33a uses the same three layers for both. */
	public static void submitLayers(PoseStack poseStack, SubmitNodeCollector collector,
			int baseColor, int frame, boolean afterTerrain) {
		submitLayers(poseStack, collector, baseColor, frame, afterTerrain, false);
	}

	private static void submitLayers(PoseStack poseStack, SubmitNodeCollector collector,
			int baseColor, int frame, boolean afterTerrain, boolean throughWall) {
		float u = frame % 8 / 8F;
		float v = frame / 8 / 10F;
		for (int layer = 0; layer < 3; layer++) {
			int alpha = 255 - layer * 64;
			float scale = 1 - layer * 0.25F;
			int rgb = ReikaColorAPI.getColorWithBrightnessMultiplier(baseColor, alpha / 255F);
			int color = alpha << 24 | rgb;
			PoseStack layerPose = copy(poseStack);
			layerPose.scale(scale, scale, scale);
			SubmitNodeCollector.CustomGeometryRenderer geometry = (ignored, out) -> quad(
					layerPose.last(), out, color, u, v, u + 1F / 8F, v + 1F / 10F);
			// The retained sheet is opaque RGB: its black background was transparency under V33a's
			// additive blend. The legacy shader derives coverage from luminance for modern item targets.
			RenderType type = throughWall
					? ChromaRenderPipelines.legacyAdditiveSpriteThroughWall(TEXTURE)
					: ChromaRenderPipelines.legacyAdditiveSprite(TEXTURE);
			if (afterTerrain)
				submitAfterTerrain(layerPose, collector, type, geometry);
			else
				collector.submitCustomGeometry(layerPose, type, geometry);
		}
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out, int color,
			float u, float v, float du, float dv) {
		out.addVertex(pose, -1, -1, 0).setColor(color).setUv(u, v)
				.setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose, 1, -1, 0).setColor(color).setUv(du, v)
				.setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose, 1, 1, 0).setColor(color).setUv(du, dv)
				.setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose, -1, 1, 0).setColor(color).setUv(u, dv)
				.setLight(LightCoordsUtil.FULL_BRIGHT);
	}

	private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
			RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
				poseStack.last().copy(), renderType, renderer);
		((OrderedSubmitNodeCollectorExtension)collector.order(0))
				.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityDimensionCore core) {
		return new AABB(core.getBlockPos()).inflate(1.5);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	public static final class State extends BlockEntityRenderState {
		private int color;
		private int frame;
		private boolean ritual;
	}
}
