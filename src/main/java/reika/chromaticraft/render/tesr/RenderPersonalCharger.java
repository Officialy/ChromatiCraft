package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityPersonalCharger;

/** Submit-pipeline port of V33a's layered, camera-facing Personal Charger glow. */
public final class RenderPersonalCharger implements
		BlockEntityRenderer<TileEntityPersonalCharger, RenderPersonalCharger.State> {

	private static final Identifier CENTER = sprite("centerblur3");
	private static final Identifier ROSES_WHITE = sprite("roses_w");
	private static final Identifier BIG_FLARE = sprite("bigflare");
	private static final Identifier ROSES = sprite("roses");

	public RenderPersonalCharger(BlockEntityRendererProvider.Context context) {}

	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(TileEntityPersonalCharger charger, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(charger, state, partialTick, cameraPosition,
				breakProgress);
		state.active = charger.canConduct();
		state.color = charger.getRenderColor();
		state.phase = charger.getBlockPos().hashCode() + System.currentTimeMillis() / 5000D;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(camera.orientation);
		if (!state.active) {
			submitLayer(poseStack, collector, CENTER, 0.5F, 0xffffffff, 0, true);
		}
		else {
			float pulse = (float)(0.875 + 0.25 * Math.sin(state.phase)
					+ 0.125 * Math.sin(state.phase * 4) + 0.0625 * Math.sin(state.phase * 16));
			submitLayer(poseStack, collector, CENTER, pulse, 0xff000000 | state.color, 0, true);
			submitLayer(poseStack, collector, ROSES_WHITE, pulse, 0xff000000 | state.color, -0.005F, true);
			submitLayer(poseStack, collector, BIG_FLARE, pulse * 0.75F, 0xffffffff, -0.01F, true);
		}
		poseStack.popPose();
	}

	/** V33a's one-layer inventory presentation, shared with the special item model. */
	public static void submitItem(PoseStack poseStack, SubmitNodeCollector collector) {
		submitLayer(poseStack, collector, ROSES, 0.875F, 0xffffffff, 0, false);
	}

	private static void submitLayer(PoseStack poseStack, SubmitNodeCollector collector,
			Identifier spriteId, float scale, int color, float z, boolean afterTerrain) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(spriteId);
		PoseStack drawPose = copy(poseStack);
		SubmitNodeCollector.CustomGeometryRenderer geometry = (ignored, vertices) ->
				quad(drawPose.last(), vertices, sprite, scale, z, color);
		var type = ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS);
		if (afterTerrain) {
			CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
					drawPose.last().copy(), type, geometry);
			((OrderedSubmitNodeCollectorExtension)collector.order(0))
					.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
		}
		else collector.submitCustomGeometry(drawPose, type, geometry);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite,
			float scale, float z, int color) {
		out.addVertex(pose, -scale, -scale, z).setUv(sprite.getU0(), sprite.getV1())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose, scale, -scale, z).setUv(sprite.getU1(), sprite.getV1())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose, scale, scale, z).setUv(sprite.getU1(), sprite.getV0())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose, -scale, scale, z).setUv(sprite.getU0(), sprite.getV0())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	private static Identifier sprite(String name) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/" + name);
	}

	@Override public AABB getRenderBoundingBox(TileEntityPersonalCharger charger) {
		return new AABB(charger.getBlockPos()).inflate(2);
	}
	@Override public boolean shouldRenderOffScreen() { return true; }
	@Override public int getViewDistance() { return 96; }

	public static final class State extends BlockEntityRenderState {
		private boolean active;
		private int color;
		private double phase;
	}
}
