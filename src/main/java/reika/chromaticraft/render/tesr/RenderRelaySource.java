package reika.chromaticraft.render.tesr;

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
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderer;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;

import java.util.ArrayList;
import java.util.List;

/** Submit-pipeline port of V33a's Relay Source body, stored-energy crystals and enhanced caustics. */
public final class RenderRelaySource
		implements BlockEntityRenderer<TileEntityRelaySource, RenderRelaySource.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "relay_source"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/relay_source.png");
	private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/crystal/crystal_outline");
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
		state.effects = source.hasLevel() || StructureRenderer.isRenderingTiles();
		state.enhanced = source.isEnhanced();
		state.time = source.getTicksExisted() + partialTick;
		state.edgeColor = 0xff000000 | CrystalElement.getBlendedColor(source.getTicksExisted(), 50);
		var crystals = new ArrayList<Crystal>();
		int capacity = source.getRenderedCrystalCapacity();
		if (capacity > 0) for (CrystalElement element : CrystalElement.elements) {
			float fraction = source.getRenderedCrystalEnergy(element) / (float)capacity;
			if (fraction > 0) crystals.add(new Crystal(element.color(), fraction));
		}
        state.crystals = List.copyOf(crystals);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		submitModel(state, poseStack, collector, model, state.lightCoords);
		if (state.effects && !state.crystals.isEmpty()) submitCrystals(state, poseStack, collector);
		if (state.effects && state.enhanced) submitEnhancedArea(poseStack, collector);
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
		collector.submitCustomGeometry(edgePose, ChromaRenderPipelines.SOURCE_EDGES_TYPE,
				(unused, out) -> model.renderEdges(edgePose, out, LightCoordsUtil.FULL_BRIGHT,
						OverlayTexture.NO_OVERLAY, edgeColor));
	}

	private static void submitCrystals(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CRYSTAL);
        float u0=sprite.getU0(),u1=sprite.getU1(),v0=sprite.getV0(),v1=sprite.getV1();
		int count = state.crystals.size();
        double angle=state.time%360;
        int spacing=360/count; // Original Java integer division, including 7/16-element rings.
		for (int i = 0; i < count; i++) {
			Crystal crystal = state.crystals.get(i);
			PoseStack draw = copy(poseStack);
			draw.translate(0.5, 0.5, 0.5);
			draw.rotate(Axis.YP.rotationDegrees((float)(angle + i * spacing)));
			draw.translate(0, 0.1 * Math.sin(state.time / 8D + i), 0);
			draw.translate(0.5, 0.5, 0.5);
			draw.rotate(Axis.YP.rotationDegrees((float)(angle * 4)));
            draw.translate(-.5,-.5,-.5);
            ScreenParticleRendering.submitGeometry(collector,draw,
                    EntityLighting.FULL_BRIGHT.screen(TextureAtlas.LOCATION_BLOCKS,true,true),
                    (captured,out)->crystal(out,captured,u0,u1,v0,v1,crystal));
		}
	}

    private static void crystal(VertexConsumer out,PoseStack.Pose pose,float u,float du,float v,float dv,Crystal crystal) {
        float w=.175F/4,h=.2875F/2*crystal.fraction,h2=.1875F/2;
        int color=0xff000000|crystal.color;
        vertex(out,pose,0.5F-w,0.5F-h,0.5F-w,u,v,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F-w,du,v,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F-w,u,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F+w,u,dv,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F+w,du,v,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F+w,u,v,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F-w,u,v,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F-w,du,v,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F+w,u,dv,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F+w,u,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F-w,du,v,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F-w,u,v,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F-w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F+w,0.5F+h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F,0.5F+h+h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F-w,du,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
        vertex(out,pose,0.5F+w,0.5F-h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F-w,0.5F-h,0.5F+w,du,dv,color);
        vertex(out,pose,0.5F,0.5F-h-h2,0.5F,u,dv,color);
    }

	private static void submitEnhancedArea(PoseStack poseStack, SubmitNodeCollector collector) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CAUSTICS);
        float u=sprite.getU0(),du=sprite.getU1(),v=sprite.getV0(),dv=sprite.getV1();
        ScreenParticleRendering.submitGeometry(collector,poseStack,
                EntityLighting.FULL_BRIGHT.screen(TextureAtlas.LOCATION_BLOCKS,true),
                (captured,out)->enhancedArea(out,captured,u,du,v,dv));
	}

    private static void enhancedArea(VertexConsumer out,PoseStack.Pose pose,float u,float du,float v,float dv) {
        float h=1.25F,o=2;
        vertex(out,pose,0.5F-o,0,o+0.5F,u,v,0xffffffff);
        vertex(out,pose,0.5F+o,0,o+0.5F,du,v,0xffffffff);
        vertex(out,pose,1,h,1,du,dv,0xffffffff);
        vertex(out,pose,0,h,1,u,dv,0xffffffff);
        vertex(out,pose,0,h,0,u,dv,0xffffffff);
        vertex(out,pose,1,h,0,du,dv,0xffffffff);
        vertex(out,pose,0.5F+o,0,-o+0.5F,du,v,0xffffffff);
        vertex(out,pose,0.5F-o,0,-o+0.5F,u,v,0xffffffff);
        vertex(out,pose,1,h,0,u,dv,0xffffffff);
        vertex(out,pose,1,h,1,du,dv,0xffffffff);
        vertex(out,pose,o+0.5F,0,0.5F+o,du,v,0xffffffff);
        vertex(out,pose,o+0.5F,0,0.5F-o,u,v,0xffffffff);
        vertex(out,pose,-o+0.5F,0,0.5F-o,u,v,0xffffffff);
        vertex(out,pose,-o+0.5F,0,0.5F+o,du,v,0xffffffff);
        vertex(out,pose,0,h,1,du,dv,0xffffffff);
        vertex(out,pose,0,h,0,u,dv,0xffffffff);
        vertex(out,pose,0,h,1,u,dv,0xffffffff);
        vertex(out,pose,1,h,1,du,dv,0xffffffff);
        vertex(out,pose,1,h,0,du,v,0xffffffff);
        vertex(out,pose,0,h,0,u,v,0xffffffff);
        vertex(out,pose,0.5F-o-0.5F,-1,o+0.5F,u,dv,0xffffffff);
        vertex(out,pose,0.5F+o+0.5F,-1,o+0.5F,du,dv,0xffffffff);
        vertex(out,pose,0.5F+o+0.5F,0,o+0.5F,du,v,0xffffffff);
        vertex(out,pose,0.5F-o-0.5F,0,o+0.5F,u,v,0xffffffff);
        vertex(out,pose,0.5F-o-0.5F,0,-o+0.5F,u,v,0xffffffff);
        vertex(out,pose,0.5F+o+0.5F,0,-o+0.5F,du,v,0xffffffff);
        vertex(out,pose,0.5F+o+0.5F,-1,-o+0.5F,du,dv,0xffffffff);
        vertex(out,pose,0.5F-o-0.5F,-1,-o+0.5F,u,dv,0xffffffff);
        vertex(out,pose,-o+0.5F,-1,0.5F-o-0.5F,u,dv,0xffffffff);
        vertex(out,pose,-o+0.5F,-1,0.5F+o+0.5F,du,dv,0xffffffff);
        vertex(out,pose,-o+0.5F,0,0.5F+o+0.5F,du,v,0xffffffff);
        vertex(out,pose,-o+0.5F,0,0.5F-o-0.5F,u,v,0xffffffff);
        vertex(out,pose,o+0.5F,0,0.5F-o-0.5F,u,v,0xffffffff);
        vertex(out,pose,o+0.5F,0,0.5F+o+0.5F,du,v,0xffffffff);
        vertex(out,pose,o+0.5F,-1,0.5F+o+0.5F,du,dv,0xffffffff);
        vertex(out,pose,o+0.5F,-1,0.5F-o-0.5F,u,dv,0xffffffff);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float u,float v,int color) {
        out.addVertex(pose,x,y,z).setUv(u,v).setColor(color).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose,0,1,0);
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
		private boolean enhanced, effects;
		private float time;
		private int edgeColor = 0xffffffff;
		private List<Crystal> crystals = List.of();
	}
}
