package reika.chromaticraft.render.tesr;

import java.util.Map;
import java.util.WeakHashMap;

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
import reika.chromaticraft.render.GlowKnot;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Full 26.2 submit port of V33a's Aura Locus renderer. */
public final class RenderAuraPoint implements
		BlockEntityRenderer<TileEntityAuraPoint, RenderAuraPoint.State> {

	private static final Identifier STAR_FLARE = sprite("starflare");
	private static final Identifier FADE = sprite("fade");
	private final Map<TileEntityAuraPoint, KnotState> knots = new WeakHashMap<>();

	public RenderAuraPoint(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityAuraPoint point, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(point, state, partialTick, cameraPosition,
				breakProgress);
		state.color = 0xff000000 | ReikaColorAPI.getModifiedSat(point.getRenderColor(), 0.875F);
		state.pvp = point.doPvP();
		state.age = point.getTicksExisted() + partialTick;
		state.frame = (int)(System.currentTimeMillis() / 250 % 80);
		reika.chromaticraft.client.render.LocusPointScreenEffects.addAuraPoint(point);
		KnotState knot = knots.computeIfAbsent(point, ignored -> new KnotState());
		knot.advance(point.getTicksExisted());
		state.knot = knot.knot;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		// RenderLocusPoint's inherited three animated ADDITIVE2 layers.
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(camera.orientation);
		RenderDimensionCore.submitLayers(poseStack, collector, state.color & 0xffffff,
				state.frame, true);
		poseStack.popPose();

		if (state.pvp) {
			float scale = (float)(3.5 + Math.sin(state.age / 32D));
			this.submitBillboard(poseStack, collector, camera, STAR_FLARE, scale, state.color, 0);
		}

		if (state.knot != null) {
			state.knot.submit(collector, poseStack, state.color, true);
			int alpha = 160;
			int faded = alpha << 24 | ReikaColorAPI.getColorWithBrightnessMultiplier(
					state.color & 0xffffff, alpha / 255F);
			this.submitBillboard(poseStack, collector, camera, FADE, 1.25F, faded, 0.05F);
		}
	}

	private void submitBillboard(PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, Identifier spriteId, float scale, int color, float z) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(spriteId);
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(camera.orientation);
		PoseStack renderPose = copy(poseStack);
		submitAfterTerrain(poseStack, collector,
				ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(ignored, vertices) -> quad(renderPose.last(), vertices, sprite, scale, z, color));
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vertices,
			TextureAtlasSprite sprite, float scale, float z, int color) {
		vertices.addVertex(pose, -scale, -scale, z).setUv(sprite.getU0(), sprite.getV1())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		vertices.addVertex(pose, scale, -scale, z).setUv(sprite.getU1(), sprite.getV1())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		vertices.addVertex(pose, scale, scale, z).setUv(sprite.getU1(), sprite.getV0())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		vertices.addVertex(pose, -scale, scale, z).setUv(sprite.getU0(), sprite.getV0())
				.setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
	}

	private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
			net.minecraft.client.renderer.rendertype.RenderType renderType,
			SubmitNodeCollector.CustomGeometryRenderer renderer) {
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

	private static Identifier sprite(String name) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/" + name);
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityAuraPoint point) {
		return new AABB(point.getBlockPos()).inflate(5);
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
		private int color = 0xffffffff;
		private int frame;
		private float age;
		private boolean pvp;
		private @Nullable GlowKnot knot;
	}

	private static final class KnotState {
		private final GlowKnot knot = new GlowKnot(0.875);
		private int lastTick = Integer.MIN_VALUE;

		private void advance(int tick) {
			if (lastTick == Integer.MIN_VALUE) {
				lastTick = tick;
				return;
			}
			int elapsed = Math.clamp(tick - lastTick, 0, 20);
			for (int i = 0; i < elapsed * 6; i++)
				knot.update();
			lastTick = tick;
		}
	}
}
