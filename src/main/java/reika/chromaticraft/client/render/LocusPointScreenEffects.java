package reika.chromaticraft.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.dragonapi.instantiable.RayTracer;

/** Modern live-focus host for V33a's DIMCORE and AURALOC fullscreen shaders. */
public final class LocusPointScreenEffects {

	private static final int MAX_POINTS = 64;
	/** std140: ivec4 count, vec4 Focus[64], vec4 Params[64]. */
	private static final int UBO_SIZE = 16 + MAX_POINTS * 16 * 2;
	private static final int UBO_USAGE = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM;
	private static final Identifier EFFECT_ID = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "locus_points");
	private static final Identifier GRADED_TARGET_ID = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "locus_graded");
	private static final Set<Identifier> ALLOWED_TARGETS = Set.of(
			PostChain.MAIN_TARGET_ID, GRADED_TARGET_ID);

	private static final RayTracer.RayTracerWithCache<?> LOS =
			RayTracer.getVisualLOSForRenderCulling();
	private static final List<Point> POINTS = new ArrayList<>();
	private static @Nullable MappableRingBuffer pointUbo;

	private LocusPointScreenEffects() {}

	public static void addDimensionCore(TileEntityDimensionCore core) {
		Player player = Minecraft.getInstance().player;
		if (player == null || POINTS.size() >= MAX_POINTS)
			return;
		double x = core.getBlockPos().getX() + 0.5;
		double y = core.getBlockPos().getY() + 0.5;
		double z = core.getBlockPos().getZ() + 0.5;
		double distSq = player.distanceToSqr(x, y, z);
		double distance = Math.sqrt(distSq);
		if (distance > 128)
			return;
		// DIMCORE is a world-space locus marker, not a surface-only bloom. Keep the shader emitter
		// alive through terrain just as the core geometry is; Aura Locus deliberately retains V33a's
		// line-of-sight gate below.
		float intensity = distance <= 32 ? 1F : (float)(1 - (distance - 32) / 96D);
		CrystalElement element = core.getColor();
		POINTS.add(new Point(new Vec3(x, y, z), distSq, Math.max(0, intensity),
				MonumentRitualEffects.getDimensionCoreShaderScale(element), element.getColor(), false));
	}

	public static void addAuraPoint(TileEntityAuraPoint point) {
		Player player = Minecraft.getInstance().player;
		if (player == null || POINTS.size() >= MAX_POINTS)
			return;
		double x = point.getBlockPos().getX() + 0.5;
		double y = point.getBlockPos().getY() + 0.5;
		double z = point.getBlockPos().getZ() + 0.5;
		double distance = Math.sqrt(player.distanceToSqr(x, y, z));
		if (distance > 40 || !hasLineOfSight(point, player, x, y, z))
			return;
		float intensity = distance <= 8 ? 1F : (float)(1 - (distance - 8) / 32D);
		if (point.getTicksExisted() < 50)
			intensity *= point.getTicksExisted() / 50F;
		if (intensity > 0)
			POINTS.add(new Point(new Vec3(x, y, z), player.distanceToSqr(x, y, z),
					intensity, 1, point.getRenderColor(), true));
	}

	private static boolean hasLineOfSight(BlockEntity tile, Player player,
			double x, double y, double z) {
		LOS.setOrigins(x, y, z, player.getX(), player.getY(), player.getZ());
		return LOS.isClearLineOfSight(tile);
	}

	/** Called after the level is fully rendered; posted emitters always belong to this one frame. */
	public static void renderAndClear(Matrix4fc modelView, Matrix4fc projection, Vec3 eye) {
		try {
			if (!POINTS.isEmpty())
				render(modelView, projection, eye);
		}
		finally {
			POINTS.clear();
		}
	}

	public static void clear() {
		POINTS.clear();
	}

	private static void render(Matrix4fc modelView, Matrix4fc projection, Vec3 eye) {
		Minecraft mc = Minecraft.getInstance();
		PostChain chain = mc.getShaderManager().getPostChain(EFFECT_ID, ALLOWED_TARGETS);
		if (chain == null)
			return;
		List<ProjectedPoint> projected = project(modelView, projection, eye);
		if (projected.isEmpty())
			return;
		upload(projected);

		RenderTarget main = mc.gameRenderer.mainRenderTarget();
		FrameGraphBuilder frame = new FrameGraphBuilder();
		ResourceHandle<RenderTarget> mainHandle = frame.importExternal("main", main);
		ResourceHandle<RenderTarget> gradedHandle = frame.createInternal("chromaticraft_locus_graded",
				new RenderTargetDescriptor(main.width, main.height, false,
						new Vector4f(0, 0, 0, 0), GpuFormat.RGBA8_UNORM));
		var pass = frame.addPass("chromaticraft_locus_points");
		pass.reads(mainHandle);
		ResourceHandle<RenderTarget> gradedOut = pass.readsAndWrites(gradedHandle);
		pass.executes(() -> draw(mainHandle.get(), gradedOut.get()));
		chain.addToFrame(frame, main.width, main.height,
				new TargetBundle(mainHandle, gradedOut));
		frame.execute(GraphicsResourceAllocator.UNPOOLED);
		pointUbo.rotate();
	}

	private static List<ProjectedPoint> project(Matrix4fc modelView, Matrix4fc projection, Vec3 eye) {
		List<ProjectedPoint> projected = new ArrayList<>(POINTS.size());
		for (Point point : POINTS) {
			Vector4f vector = new Vector4f((float)(point.position.x - eye.x),
					(float)(point.position.y - eye.y), (float)(point.position.z - eye.z), 1);
			vector.mul(modelView);
			vector.mul(projection);
			if (vector.w <= 1.0E-4F)
				continue;
			float u = vector.x / vector.w * 0.5F + 0.5F;
			float v = vector.y / vector.w * 0.5F + 0.5F;
			int rgb = point.color;
			projected.add(new ProjectedPoint(u, v, (float)Math.max(0.01, point.distSq),
					point.intensity, ((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F,
					(rgb & 255) / 255F, point.aura ? -point.scale : point.scale));
		}
		return projected;
	}

	private static void upload(List<ProjectedPoint> points) {
		if (pointUbo == null)
			pointUbo = new MappableRingBuffer(() -> "ChromatiCraft LocusPoints",
					UBO_USAGE, UBO_SIZE);
		try (var view = pointUbo.currentBuffer().map(false, true)) {
			Std140Builder builder = Std140Builder.intoBuffer(view.data());
			builder.putIVec4(points.size(), 0, 0, 0);
			for (ProjectedPoint point : points)
				builder.putVec4(point.u, point.v, point.distSq, point.intensity);
			for (int i = points.size(); i < MAX_POINTS; i++)
				builder.putVec4(0, 0, 0, 0);
			for (ProjectedPoint point : points)
				builder.putVec4(point.red, point.green, point.blue, point.signedScale);
		}
	}

	private static void draw(RenderTarget source, RenderTarget target) {
		var encoder = RenderSystem.getDevice().createCommandEncoder();
		try (var pass = encoder.createRenderPass(() -> "ChromatiCraft locus point shaders",
				target.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(ChromaRenderPipelines.LOCUS_POINTS);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("LocusPoints", pointUbo.currentBuffer());
			pass.bindTexture("InSampler", source.getColorTextureView(),
					RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}

	private record Point(Vec3 position, double distSq, float intensity, float scale,
			int color, boolean aura) {}
	private record ProjectedPoint(float u, float v, float distSq, float intensity,
			float red, float green, float blue, float signedScale) {}

	private record TargetBundle(ResourceHandle<RenderTarget> main,
			ResourceHandle<RenderTarget> graded) implements PostChain.TargetBundle {
		@Override public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {}
		@Override public @Nullable ResourceHandle<RenderTarget> get(Identifier id) {
			if (id.equals(PostChain.MAIN_TARGET_ID))
				return main;
			return id.equals(GRADED_TARGET_ID) ? graded : null;
		}
	}
}
