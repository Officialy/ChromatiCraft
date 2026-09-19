package reika.chromaticraft.render.tesr.dimension;

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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.dimension.TileEntityVoidRift;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** V33a's animated sixteen-block Void Rift aura, using the shipped fallback copy of its atlas. */
public final class RenderVoidRift implements
		BlockEntityRenderer<TileEntityVoidRift, RenderVoidRift.State> {

	private static final Identifier AURA = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/effect/voidaura-strip_page_fallback.png");

	public RenderVoidRift(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityVoidRift rift, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(rift, state, partialTick, cameraPosition,
				breakProgress);
		CrystalElement own = rift.getColor();
		state.worldX = rift.getBlockPos().getX();
		state.worldY = rift.getBlockPos().getY();
		state.worldZ = rift.getBlockPos().getZ();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			CrystalElement adjacent = rift.colorAt(direction);
			state.colors[index(direction)] = adjacent == own ? 0 : adjacent == null ? own.getColor()
					: ReikaColorAPI.mixColors(own.getColor(), adjacent.getColor(), 0.5F);
			for (int height = 1; height < TileEntityVoidRift.HEIGHT; height++) {
				// Do not lay the aura exactly across an opaque terrain face. The animated seam moves
				// through the block boundary, so a depth bias alone only changes which surface flickers;
				// suppressing the covered strip makes solid blocks correctly occlude the beam.
				state.occluded[index(direction)][height] = rift.getLevel().getBlockState(
						rift.getBlockPos().relative(direction).above(height)).canOcclude();
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++)
				state.neighbours[dx + 1][dz + 1] = (dx != 0 || dz != 0) && rift.hasAt(dx, dz);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		PoseStack auraPose = copy(poseStack);
		submitAfterTerrain(auraPose, collector, ChromaRenderPipelines.additiveSprite(AURA),
				(ignored, out) -> renderAura(auraPose.last(), out, state));
	}

	private static void renderAura(PoseStack.Pose pose, VertexConsumer out, State state) {
		long tick = System.currentTimeMillis();
		double time = tick / 200D;
		float pulse = (float)(0.875 + 0.125 * Math.sin(tick / 800D));
		int hx = Math.floorMod(16 + Math.floorMod(state.worldX + state.worldZ, 16), 16);
		int frame = (int)(tick / 32 % 128);
		float u = hx / 256F + (frame % 16) / 16F;
		float du = u + 1F / 256F;

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			int base = state.colors[index(direction)];
			if (base == 0)
				continue;
			int color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(base, pulse);
			for (int k = 1; k < TileEntityVoidRift.HEIGHT; k++) {
				if (state.occluded[index(direction)][k])
					continue;
				float dv = (frame / 16 + 1) / 8F
						+ (1 - k / (float)TileEntityVoidRift.HEIGHT) / 8F;
				float v = dv - 1F / 128F;
				double bottom = k > 1 ? wave(state.worldY + k, time) : 0;
				double top = wave(state.worldY + k + 1, time);
				emitWall(pose, out, state, direction, k, bottom, top, color, u, v, du, dv);
			}
		}
	}

	/** The original seam geometry, including diagonal joins and the flat first segment. */
	private static void emitWall(PoseStack.Pose pose, VertexConsumer out, State state,
			Direction direction, int k, double bottom, double top, int color,
			float u, float v, float du, float dv) {
		boolean northOpen = state.colors[index(Direction.NORTH)] != 0;
		boolean southOpen = state.colors[index(Direction.SOUTH)] != 0;
		boolean westOpen = state.colors[index(Direction.WEST)] != 0;
		boolean eastOpen = state.colors[index(Direction.EAST)] != 0;
		switch (direction) {
			case NORTH -> {
				vertex(pose,out,westOpen ? top : 0,k+1,has(state,-1,-1)?0:top,color,u,v);
				vertex(pose,out,eastOpen ? 1-top : 1,k+1,has(state,1,-1)?0:top,color,du,v);
				vertex(pose,out,eastOpen ? 1-bottom : 1,k,has(state,1,-1)?0:bottom,color,du,dv);
				vertex(pose,out,westOpen ? bottom : 0,k,has(state,-1,-1)?0:bottom,color,u,dv);
			}
			case SOUTH -> {
				vertex(pose,out,westOpen ? bottom : 0,k,has(state,-1,1)?1:1-bottom,color,u,dv);
				vertex(pose,out,eastOpen ? 1-bottom : 1,k,has(state,1,1)?1:1-bottom,color,du,dv);
				vertex(pose,out,eastOpen ? 1-top : 1,k+1,has(state,1,1)?1:1-top,color,du,v);
				vertex(pose,out,westOpen ? top : 0,k+1,has(state,-1,1)?1:1-top,color,u,v);
			}
			case EAST -> {
				vertex(pose,out,has(state,1,-1)?1:1-top,k+1,northOpen?top:0,color,u,v);
				vertex(pose,out,has(state,1,1)?1:1-top,k+1,southOpen?1-top:1,color,du,v);
				vertex(pose,out,has(state,1,1)?1:1-bottom,k,southOpen?1-bottom:1,color,du,dv);
				vertex(pose,out,has(state,1,-1)?1:1-bottom,k,northOpen?bottom:0,color,u,dv);
			}
			case WEST -> {
				vertex(pose,out,has(state,-1,-1)?0:bottom,k,northOpen?bottom:0,color,u,dv);
				vertex(pose,out,has(state,-1,1)?0:bottom,k,southOpen?1-bottom:1,color,du,dv);
				vertex(pose,out,has(state,-1,1)?0:top,k+1,southOpen?1-top:1,color,du,v);
				vertex(pose,out,has(state,-1,-1)?0:top,k+1,northOpen?top:0,color,u,v);
			}
			default -> { }
		}
	}

	private static boolean has(State state, int dx, int dz) {
		return state.neighbours[dx + 1][dz + 1];
	}

	/** V33a's fast sine approximation, retained because it gives the aura its angular shimmer. */
	private static double wave(double y, double time) {
		return 0.03125 * approxSin(y + time) + 0.03125 * approxCos(y + time / 3D);
	}

	private static double approxSin(double angle) {
		angle = angle % (Math.PI * 2) - Math.PI;
		return angle < 0 ? -(1.27323954 * angle + 0.405284735 * angle * angle)
				: -(1.27323954 * angle - 0.405284735 * angle * angle);
	}

	private static double approxCos(double angle) {
		return approxSin(angle + Math.PI / 2);
	}

	private static int index(Direction direction) {
		return switch (direction) {
			case NORTH -> 0;
			case SOUTH -> 1;
			case WEST -> 2;
			case EAST -> 3;
			default -> throw new IllegalArgumentException("Not horizontal: " + direction);
		};
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer out, double x,double y,double z,
			int color,float u,float v) {
		out.addVertex(pose, (float)x, (float)y, (float)z).setUv(u, v).setColor(color);
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
	public AABB getRenderBoundingBox(TileEntityVoidRift rift) {
		return rift.getRenderBoundingBox();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 192;
	}

	public static final class State extends BlockEntityRenderState {
		private final int[] colors = new int[4];
		private final boolean[][] occluded = new boolean[4][TileEntityVoidRift.HEIGHT];
		private final boolean[][] neighbours = new boolean[3][3];
		private int worldX;
		private int worldY;
		private int worldZ;
	}
}
