package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelTurbo;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger;
import reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger.Location;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/** V33a Energy Focal Point body, ritual ring, lightning, and sky-column rendering. */
public final class RenderPylonTurboCharger
		implements BlockEntityRenderer<TileEntityPylonTurboCharger, RenderPylonTurboCharger.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_turbo_charger"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/icons/turbo.png");
	private static final Identifier BEAM = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/effect/beam.png");

	private final ModelTurbo model;

	public RenderPylonTurboCharger(BlockEntityRendererProvider.Context context) {
		model = new ModelTurbo(context.bakeLayer(MODEL_LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityPylonTurboCharger charger, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(charger, state, partialTick, cameraPosition, breakProgress);
		state.ritualActive = charger.isRitualActive();
		state.skyTick = charger.getSkyTick();
		state.time = charger.getLevel() != null ? charger.getLevel().getGameTime() + partialTick : partialTick;
		state.connection = null;
		Location location = charger.getLocation();
		if (location != null && charger.getLevel() != null
				&& charger.getLevel().getBlockEntity(charger.getBlockPos().offset(location.nextOffset()))
						instanceof TileEntityPylonTurboCharger) {
			state.connection = Vec3.atLowerCornerOf(location.nextOffset());
		}
		state.lightningEnd = location == null
				? new Vec3(0, 8, 0)
				: Vec3.atLowerCornerOf(location.relativePylonOffset());
		TileEntityCrystalPylon pylon = charger.getAssociatedPylon();
		state.color = pylon != null ? pylon.getColor().getColor() : CrystalElement.WHITE.getColor();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		submitModel(state, poseStack, collector);

		poseStack.pushPose();
		poseStack.translate(0.5, 0.4375, 0.5);
		if (state.connection != null)
			submitLine(poseStack, collector, state.connection, 0xc0ffffff, 3F);
		if (state.ritualActive)
			submitLightning(state, poseStack, collector);
		poseStack.popPose();

		if (state.skyTick > 0)
			submitSkyBeam(state, poseStack, collector);
	}

	private void submitModel(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = copy(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (unused, vertices) ->
				model.render(modelPose, vertices, state.lightCoords, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	private static void submitLightning(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		PoseStack renderPose = copy(poseStack);
		submitAfterTerrain(poseStack, collector, RenderTypes.linesTranslucent(), (unused, vertices) -> {
			Vector3f normal = new Vector3f((float)state.lightningEnd.x,
					(float)state.lightningEnd.y, (float)state.lightningEnd.z).normalize();
			for (int bolt = 0; bolt < 3; bolt++) {
				int segments = 4 + bolt * 4;
				Vec3 previous = Vec3.ZERO;
				for (int segment = 1; segment <= segments; segment++) {
					double fraction = segment / (double)segments;
					Vec3 point = state.lightningEnd.scale(fraction);
					if (segment < segments) {
						double envelope = Math.sin(Math.PI * fraction) * 0.375;
						double phase = state.time * (0.0625 + bolt * 0.0175) + segment * 2.399 + bolt * 17.31;
						point = point.add(Math.sin(phase) * envelope,
								Math.sin(phase * 1.37 + 2.1) * envelope,
								Math.cos(phase * 0.83 - 1.7) * envelope);
					}
					lineVertex(vertices, renderPose.last(), previous, normal, 0xc0ffffff, 4F);
					lineVertex(vertices, renderPose.last(), point, normal, 0xc0ffffff, 4F);
					previous = point;
				}
			}
		});
	}

	private static void submitSkyBeam(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.translate(0.5, 8.5, 0.5);
		PoseStack renderPose = copy(poseStack);
		float scroll = (float)((state.time / 30D) % 12D);
		int color = 0xff000000 | state.color;
		submitAfterTerrain(poseStack, collector, ChromaRenderPipelines.energyBeam(BEAM),
				(unused, vertices) -> beamTube(renderPose.last(), vertices, 512, 0.35F, scroll, color));
		poseStack.popPose();
	}

	private static void beamTube(PoseStack.Pose pose, VertexConsumer vertices, double height,
			float radius, float scroll, int color) {
		for (int side = 0; side < 6; side++) {
			double a0 = side * Math.PI * 2 / 6D;
			double a1 = (side + 1) * Math.PI * 2 / 6D;
			float x0 = (float)(Math.sin(a0) * radius);
			float z0 = (float)(Math.cos(a0) * radius);
			float x1 = (float)(Math.sin(a1) * radius);
			float z1 = (float)(Math.cos(a1) * radius);
			vertex(vertices, pose, x0, 0, z0, scroll, scroll + 1, color);
			vertex(vertices, pose, x1, 0, z1, scroll, scroll + 1, color);
			vertex(vertices, pose, x1, height, z1, scroll + 32, scroll, color);
			vertex(vertices, pose, x0, height, z0, scroll + 32, scroll, color);
		}
	}

	private static void submitLine(PoseStack poseStack, SubmitNodeCollector collector,
			Vec3 end, int color, float width) {
		PoseStack renderPose = copy(poseStack);
		submitAfterTerrain(poseStack, collector, RenderTypes.linesTranslucent(), (unused, vertices) -> {
			Vector3f normal = new Vector3f((float)end.x, (float)end.y, (float)end.z).normalize();
			lineVertex(vertices, renderPose.last(), Vec3.ZERO, normal, color, width);
			lineVertex(vertices, renderPose.last(), end, normal, color, width);
		});
	}

	private static void lineVertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point,
			Vector3f normal, int color, float width) {
		vertices.addVertex(pose, (float)point.x, (float)point.y, (float)point.z).setColor(color)
				.setNormal(pose, normal.x, normal.y, normal.z).setLineWidth(width);
	}

	private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
			float x, double y, float z, float u, float v, int color) {
		vertices.addVertex(pose, x, (float)y, z).setUv(u, v).setColor(color);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
			RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
				poseStack.last().copy(), renderType, renderer);
		((OrderedSubmitNodeCollectorExtension)collector.order(0))
				.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityPylonTurboCharger charger) {
		return new AABB(charger.getBlockPos()).inflate(12, 0, 12).expandTowards(0, 520, 0);
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
		private boolean ritualActive;
		private int skyTick;
		private int color;
		private double time;
		private @Nullable Vec3 connection;
		private Vec3 lightningEnd = Vec3.ZERO;
	}
}
