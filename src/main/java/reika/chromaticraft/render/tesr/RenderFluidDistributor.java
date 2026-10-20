package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import reika.chromaticraft.tileentity.transport.TileEntityFluidDistributor;

/** V33a's blue, pulsing ADDITIVEDARK lattice halo; the source renderer did not draw a solid model. */
public final class RenderFluidDistributor implements BlockEntityRenderer<TileEntityFluidDistributor, RenderFluidDistributor.State> {
	private static final Identifier LATTICE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/block/icons/lattice.png");

	public RenderFluidDistributor(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityFluidDistributor tile, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
		state.ticks = tile.getTicksExisted() + partialTick;
		state.inWorld = tile.hasLevel();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		PoseStack.Pose pose = poseStack.last().copy();
        float ticks = state.ticks;
        var type=EntityLighting.LIGHTMAP_ONLY.screen(LATTICE,false);
        SubmitNodeCollector.CustomGeometryRenderer draw=(ignored, vertices)->drawHalo(pose,vertices,ticks);
        if(state.inWorld) ScreenParticleRendering.submitGeometry(collector,poseStack,type,draw);
        else collector.submitCustomGeometry(poseStack,type,draw);
	}

	private static void drawHalo(PoseStack.Pose pose, VertexConsumer v, float ticks) {
		float[] expansion = new float[6];
		for (int side = 0; side < 6; side++)
			expansion[side] = (float)(0.0125 + 0.0125 * Math.sin(Math.toRadians(ticks * 3 + side * 60)));
		float west = -expansion[4], east = 1 + expansion[5];
		float down = -expansion[0], up = 1 + expansion[1];
		float north = -expansion[2], south = 1 + expansion[3];
		quad(pose, v, west, up, north, east, up, north, east, down, north, west, down, north);
		quad(pose, v, west, up, south, east, up, south, east, down, south, west, down, south);
		quad(pose, v, west, up, north, west, up, south, west, down, south, west, down, north);
		quad(pose, v, east, up, north, east, up, south, east, down, south, east, down, north);
		quad(pose, v, west, up, south, east, up, south, east, up, north, west, up, north);
		quad(pose, v, west, down, south, east, down, south, east, down, north, west, down, north);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer v,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		vertex(pose, v, ax, ay, az, 0, 1);
		vertex(pose, v, bx, by, bz, 1, 1);
		vertex(pose, v, cx, cy, cz, 1, 0);
		vertex(pose, v, dx, dy, dz, 0, 0);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer v, float x, float y, float z, float u, float texV) {
		v.addVertex(pose, x, y, z).setUv(u, texV).setColor(0xff2255ff).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose,0,1,0);
	}

	public static final class State extends BlockEntityRenderState {
		private boolean inWorld;
		private float ticks;
	}
}
