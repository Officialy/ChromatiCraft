package reika.chromaticraft.client.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.auxiliary.trackers.TickScheduler;
import reika.dragonapi.instantiable.event.ScheduledTickEvent;
import reika.dragonapi.instantiable.event.scheduledtickevent.ScheduledEvent;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

import java.util.Optional;
import java.util.Set;

/**
 * Client half of V33a {@code ArtefactWithDataCrystalAlloyingEffect}: the rising rays, the shockwave burst, and
 * the {@code ARTEALLOY$SHOCK} and {@code ARTEALLOY$GLOW} screen shaders focused on the orange Memory Crystal.
 *
 * <p>GLOW: each frame the crystal is drawn it adds 0.2 intensity (capped at 1); after every frame the
 * intensity falls ({@code x*0.99 - 0.008}), so it fades out once the crystal is gone. SHOCK: while a burst's
 * {@code fireTick} is in (0, 35] a ring of radius {@code 1 - fire/35} pulls the image toward the crystal and
 * bleaches it; it is cleared after each frame. V33a ran them as two registry passes, SHOCK first; GLOW only
 * recolours, so one pass doing both in that order is the same image.
 *
 * <p>V33a projected the focus with the matrices captured when the crystal was drawn, which went stale while
 * GLOW faded; the focus is reprojected with the current camera every frame instead.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ArtealloyEffects {

	private static final Identifier EFFECT_ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "artealloy");
	private static final Identifier GRADED_TARGET_ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "artealloy_graded");
	private static final Set<Identifier> ALLOWED_TARGETS = Set.of(PostChain.MAIN_TARGET_ID, GRADED_TARGET_ID);
	private static final int UBO_USAGE = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM;
	/** std140: vec4 Glow, vec4 Shock, vec4 Params. */
	private static final int UBO_SIZE = 48;

	private static final double SHOCK_THICKNESS = 0.125;

	private static int fireTick;

	private static float glowIntensity;
	private static @Nullable Vec3 glowFocus;
	private static double glowDistance;

	private static float shockIntensity;
	private static float shockSize;

	private static @Nullable MappableRingBuffer ubo;

	private ArtealloyEffects() {}

	/** V33a {@code doClientFX} and the client {@code fireTick} countdown, once per alloying tick. */
	public static void doClientFX(ItemEntity ei) {
		ChromaParticle.spawnArtealloyRays(ei);
		if (fireTick > 0)
			fireTick--;
	}

	/** V33a {@code doBurst}, on the ARTEALLOYBURST packet. */
	public static void doBurst(ItemEntity ei) {
		ReikaSoundHelper.playClientSound(ChromaSounds.ARTEALLOYHIT, ei, 2, 1, false);
		fireTick = 50;
		TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(new ParticleBurst(ei.getX(), ei.getY(), ei.getZ())), 10);
	}

	public static int getFireTick() {
		return fireTick;
	}

	/**
	 * Called each frame an artealloyed Memory Crystal is drawn, with its shader focus and squared distance to
	 * the viewer.
	 */
	public static void markCrystal(Vec3 focus, double distSq) {
		glowIntensity = Math.min(1, glowIntensity + 0.2F);
		glowFocus = focus;
		glowDistance = distSq;
		int fire = fireTick;
		shockIntensity = fire > 0 && fire <= 35 ? (float)Math.pow(fire / 35F, 2) : 0;
		shockSize = 1 - fire / 35F;
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterLevel event) {
		try {
			if (glowIntensity > 0 && glowFocus != null) {
				var camera = event.getLevelRenderState().cameraRenderState;
				render(event.getModelViewMatrix(), camera.projectionMatrix, camera.pos);
			}
		}
		finally {
			// ChromaShaders.onPostRender: SHOCK clearOnRender, GLOW rampDown 0.008 / 0.99.
			shockIntensity = 0;
			glowIntensity = Math.max(0, glowIntensity * 0.99F - 0.008F);
			if (glowIntensity <= 0)
				glowFocus = null;
		}
	}

	private static void render(Matrix4fc modelView, Matrix4fc projection, Vec3 eye) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		PostChain chain = mc.getShaderManager().getPostChain(EFFECT_ID, ALLOWED_TARGETS);
		if (chain == null)
			return;
		Vector4f v = new Vector4f((float)(glowFocus.x - eye.x), (float)(glowFocus.y - eye.y), (float)(glowFocus.z - eye.z), 1);
		v.mul(modelView);
		v.mul(projection);
		if (v.w <= 1.0E-4F)
			return;
		float u = v.x / v.w * 0.5F + 0.5F;
		float w = v.y / v.w * 0.5F + 0.5F;
		upload(u, w, mc.player.tickCount);

		RenderTarget main = mc.gameRenderer.mainRenderTarget();
		FrameGraphBuilder frame = new FrameGraphBuilder();
		ResourceHandle<RenderTarget> mainHandle = frame.importExternal("main", main);
		ResourceHandle<RenderTarget> gradedHandle = frame.createInternal("chromaticraft_artealloy_graded",
				new com.mojang.blaze3d.resource.RenderTargetDescriptor(main.width, main.height, new com.mojang.blaze3d.resource.RenderTargetDescriptor.TextureProperties(new org.joml.Vector4f(0, 0, 0, 0), com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM), null));
		var pass = frame.addPass("chromaticraft_artealloy");
		pass.reads(mainHandle);
		ResourceHandle<RenderTarget> gradedOut = pass.readsAndWrites(gradedHandle);
		pass.executes(() -> draw(mainHandle.get(), gradedOut.get()));
		chain.addToFrame(frame, main.width, main.height, new TargetBundle(mainHandle, gradedOut));
		frame.execute(GraphicsResourceAllocator.UNPOOLED);
		ubo.rotate();
	}

	private static void upload(float u, float v, int time) {
		if (ubo == null)
			ubo = new MappableRingBuffer(() -> "ChromatiCraft Artealloy", UBO_USAGE, UBO_SIZE);
		try (var view = ubo.currentBuffer().map(false, true)) {
			Std140Builder.intoBuffer(view.data())
					.putVec4(u, v, glowIntensity, (float)glowDistance)
					.putVec4(u, v, shockIntensity, shockSize)
					.putVec4(time, (float)SHOCK_THICKNESS, 0, 0);
		}
	}

	private static void draw(RenderTarget source, RenderTarget target) {
		var encoder = RenderSystem.getDevice().createCommandEncoder();
		try (var pass = encoder.createRenderPass(() -> "ChromatiCraft artealloy shaders",
				target.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(ChromaRenderPipelines.ARTEALLOY));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("Artealloy", ubo.currentBuffer());
			pass.setUniform("InSampler", source.getColorTextureView(),
					RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}

	private record TargetBundle(ResourceHandle<RenderTarget> main, ResourceHandle<RenderTarget> graded)
			implements PostChain.TargetBundle {
		@Override public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {}
		@Override public @Nullable ResourceHandle<RenderTarget> get(Identifier id) {
			if (id.equals(PostChain.MAIN_TARGET_ID))
				return main;
			return id.equals(GRADED_TARGET_ID) ? graded : null;
		}
	}

	/** V33a {@code ArtefactWithDataCrystalAlloyingEffect.ParticleBurst}. */
	private record ParticleBurst(double x, double y, double z) implements ScheduledEvent {

		@Override
		public void fire() {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null)
				ChromaParticle.spawnArtealloyBurst(mc.level, x, y, z);
		}

		@Override
		public boolean runOnSide(LogicalSide s) {
			return s == LogicalSide.CLIENT;
		}

	}

}
