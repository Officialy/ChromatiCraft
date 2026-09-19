package reika.chromaticraft.render.tesr;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelRelaySource;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;

/** Submit-pipeline port of V33a's Relay Source body, stored-energy crystals and enhanced caustics. */
public final class RenderRelaySource
		implements BlockEntityRenderer<TileEntityRelaySource, RenderRelaySource.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "relay_source"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/relay_source.png");
	private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/crystal/crystal");
	private static final Identifier CAUSTICS = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/icons/caustics-g");

	private final ModelRelaySource model;

	public RenderRelaySource(BlockEntityRendererProvider.Context context) {
		model = new ModelRelaySource(context.bakeLayer(MODEL_LAYER));
	}

	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(TileEntityRelaySource source, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(source, state, partialTick, cameraPosition, breakProgress);
		state.enhanced = source.isEnhanced();
		state.time = source.getTicksExisted() + partialTick;
		state.edgeColor = 0xff000000 | CrystalElement.getBlendedColor(source.getTicksExisted(), 50);
		state.crystals.clear();
		int capacity = source.getRenderedCrystalCapacity();
		if (capacity > 0) for (CrystalElement element : CrystalElement.elements) {
			float fraction = source.getRenderedCrystalEnergy(element) / (float)capacity;
			if (fraction > 0) state.crystals.add(new Crystal(element.getColor(), fraction));
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		submitModel(state, poseStack, collector, model, state.lightCoords);
		if (!state.crystals.isEmpty()) submitCrystals(state, poseStack, collector);
		if (state.enhanced) submitEnhancedArea(poseStack, collector);
	}

	public static void submitModel(State state, PoseStack poseStack, SubmitNodeCollector collector,
			ModelRelaySource model, int light) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		submitItemModel(poseStack, collector, model, light, state.edgeColor);
		poseStack.popPose();
	}

	/** Renders just the source model around the caller's already-established item/model origin. */
	public static void submitItemModel(PoseStack poseStack, SubmitNodeCollector collector,
			ModelRelaySource model, int light, int edgeColor) {
		PoseStack bodyPose = copy(poseStack);
		collector.submitCustomGeometry(bodyPose, RenderTypes.entityCutout(TEXTURE),
				(unused, out) -> model.renderBody(bodyPose, out, light, OverlayTexture.NO_OVERLAY));
		PoseStack edgePose = copy(poseStack);
		collector.submitCustomGeometry(edgePose, ChromaRenderPipelines.legacyAdditiveSprite(TEXTURE),
				(unused, out) -> model.renderEdges(edgePose, out, LightCoordsUtil.FULL_BRIGHT,
						OverlayTexture.NO_OVERLAY, edgeColor));
	}

	private static void submitCrystals(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CRYSTAL);
		int count = state.crystals.size();
		for (int i = 0; i < count; i++) {
			Crystal crystal = state.crystals.get(i);
			PoseStack draw = copy(poseStack);
			draw.translate(0.5, 0.5, 0.5);
			draw.mulPose(Axis.YP.rotationDegrees((float)(state.time + i * 360D / count)));
			draw.translate(0, 0.1 * Math.sin(state.time / 8D + i), 0);
			draw.translate(0.5, 0.5, 0.5);
			draw.mulPose(Axis.YP.rotationDegrees(state.time * 4));
			collector.submitCustomGeometry(draw,
					ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
					(unused, out) -> crystal(out, draw.last(), sprite, crystal));
		}
	}

	private static void crystal(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite,
			Crystal crystal) {
		float w = 0.175F / 4;
		float h = 0.2875F / 2 * crystal.fraction;
		float tip = 0.1875F / 2;
		int color = 0xff000000 | crystal.color;
		float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
		quad(out, pose, -w,-h,-w, -w,h,-w, w,h,-w, w,-h,-w, u0,u1,v0,v1,color);
		quad(out, pose, w,-h,w, w,h,w, -w,h,w, -w,-h,w, u0,u1,v0,v1,color);
		quad(out, pose, w,-h,-w, w,h,-w, w,h,w, w,-h,w, u0,u1,v0,v1,color);
		quad(out, pose, -w,-h,w, -w,h,w, -w,h,-w, -w,-h,-w, u0,u1,v0,v1,color);
		triangleQuad(out, pose, 0,h+tip,0, w,h,w, w,h,-w, color, u0,u1,v0,v1);
		triangleQuad(out, pose, 0,h+tip,0, -w,h,-w, -w,h,w, color, u0,u1,v0,v1);
		triangleQuad(out, pose, 0,-h-tip,0, w,-h,-w, w,-h,w, color, u0,u1,v0,v1);
		triangleQuad(out, pose, 0,-h-tip,0, -w,-h,w, -w,-h,-w, color, u0,u1,v0,v1);
	}

	private static void submitEnhancedArea(PoseStack poseStack, SubmitNodeCollector collector) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CAUSTICS);
		PoseStack draw = copy(poseStack);
		collector.submitCustomGeometry(draw,
				ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(unused, out) -> enhancedArea(out, draw.last(), sprite));
	}

	private static void enhancedArea(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite s) {
		float u0=s.getU0(), u1=s.getU1(), v0=s.getV0(), v1=s.getV1(); int c=0xffffffff;
		quad(out,pose,-1.5F,0,2.5F,2.5F,0,2.5F,1,1.25F,1,0,1.25F,1,u0,u1,v0,v1,c);
		quad(out,pose,0,1.25F,0,1,1.25F,0,2.5F,0,-1.5F,-1.5F,0,-1.5F,u0,u1,v0,v1,c);
		quad(out,pose,1,1.25F,0,1,1.25F,1,2.5F,0,2.5F,2.5F,0,-1.5F,u0,u1,v0,v1,c);
		quad(out,pose,-1.5F,0,-1.5F,-1.5F,0,2.5F,0,1.25F,1,0,1.25F,0,u0,u1,v0,v1,c);
		quad(out,pose,0,1.25F,1,1,1.25F,1,1,1.25F,0,0,1.25F,0,u0,u1,v0,v1,c);
		quad(out,pose,-2,-1,2.5F,3,-1,2.5F,3,0,2.5F,-2,0,2.5F,u0,u1,v0,v1,c);
		quad(out,pose,-2,0,-1.5F,3,0,-1.5F,3,-1,-1.5F,-2,-1,-1.5F,u0,u1,v0,v1,c);
		quad(out,pose,-1.5F,-1,-2,-1.5F,-1,3,-1.5F,0,3,-1.5F,0,-2,u0,u1,v0,v1,c);
		quad(out,pose,2.5F,0,-2,2.5F,0,3,2.5F,-1,3,2.5F,-1,-2,u0,u1,v0,v1,c);
	}

	private static void triangleQuad(VertexConsumer out, PoseStack.Pose pose,
			float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,
			int color,float u0,float u1,float v0,float v1) {
		quad(out,pose,ax,ay,az,bx,by,bz,cx,cy,cz,ax,ay,az,u0,u1,v0,v1,color);
	}

	private static void quad(VertexConsumer out, PoseStack.Pose pose,
			float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,
			float x4,float y4,float z4,float u0,float u1,float v0,float v1,int color) {
		out.addVertex(pose,x1,y1,z1).setUv(u0,v1).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose,x2,y2,z2).setUv(u0,v0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose,x3,y3,z3).setUv(u1,v0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		out.addVertex(pose,x4,y4,z4).setUv(u1,v1).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack(); copy.last().set(source.last()); return copy;
	}

	@Override public AABB getRenderBoundingBox(TileEntityRelaySource source) {
		return new AABB(source.getBlockPos()).inflate(source.isEnhanced() ? 3 : 1);
	}
	@Override public boolean shouldRenderOffScreen() { return true; }
	@Override public int getViewDistance() { return 128; }

	private record Crystal(int color, float fraction) {}
	public static final class State extends BlockEntityRenderState {
		private boolean enhanced;
		private float time;
		private int edgeColor = 0xffffffff;
		private final List<Crystal> crystals = new ArrayList<>();
	}
}
