package reika.chromaticraft.client.render;

import java.util.Optional;
import java.util.Set;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code ChromaShaders.UAZONE}, driven by {@code ArtefactSpawner}: while the player stands in a lore
 * tower's artefact ring, the world's edges pick up a faint moving tracery of acid green, stronger in the
 * dark. Each zone signal from the server keeps it alive for ten ticks; while alive its intensity climbs
 * ({@code x*1.005 + 0.008} per tick, capped at 1), and afterwards falls ({@code x*0.994 - 0.06}).
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ArtefactZoneScreenEffect {

	private static final Identifier EFFECT_ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "ua_zone");
	private static final Identifier GRADED_TARGET_ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "ua_graded");
	private static final Set<Identifier> ALLOWED_TARGETS = Set.of(PostChain.MAIN_TARGET_ID, GRADED_TARGET_ID);
	private static final int UBO_USAGE = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM;

	private static long lastShaderTime = -1;
	private static float intensity;
	private static @Nullable MappableRingBuffer ubo;

	private ArtefactZoneScreenEffect() {}

	/** V33a {@code refreshShader}. */
	public static void refresh(Level world) {
		lastShaderTime = world.getGameTime();
	}

	/** V33a {@code resetShader}. */
	public static void reset() {
		lastShaderTime = -1;
	}

	private static boolean isShaderActive(Level world) {
		return lastShaderTime >= 0 && world.getGameTime() - lastShaderTime <= 10;
	}

	/** V33a {@code updateShader}, once per client player tick. */
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			intensity = 0;
			reset();
			return;
		}
		if (isShaderActive(mc.level))
			intensity = Math.min(1, intensity * 1.005F + 0.008F);
		else
			intensity = Math.max(0, intensity * 0.994F - 0.06F);
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterLevel event) {
		Minecraft mc = Minecraft.getInstance();
		if (intensity <= 0 || mc.level == null || mc.player == null)
			return;
		PostChain chain = mc.getShaderManager().getPostChain(EFFECT_ID, ALLOWED_TARGETS);
		if (chain == null)
			return;
		float ptick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float skyBright = ReikaWorldHelper.getSunIntensity(mc.level, true, ptick);
		var camera = mc.gameRenderer.mainCamera();
		float head = camera.xRot() + camera.yRot();
		upload(intensity, mc.player.tickCount, skyBright, head);

		RenderTarget main = mc.gameRenderer.mainRenderTarget();
		FrameGraphBuilder frame = new FrameGraphBuilder();
		ResourceHandle<RenderTarget> mainHandle = frame.importExternal("main", main);
		ResourceHandle<RenderTarget> gradedHandle = frame.createInternal("chromaticraft_ua_graded",
				new com.mojang.blaze3d.resource.RenderTargetDescriptor(main.width, main.height, new com.mojang.blaze3d.resource.RenderTargetDescriptor.TextureProperties(new org.joml.Vector4f(0, 0, 0, 0), com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM), null));
		var pass = frame.addPass("chromaticraft_ua_zone");
		pass.reads(mainHandle);
		ResourceHandle<RenderTarget> gradedOut = pass.readsAndWrites(gradedHandle);
		pass.executes(() -> draw(mainHandle.get(), gradedOut.get()));
		chain.addToFrame(frame, main.width, main.height, new TargetBundle(mainHandle, gradedOut));
		frame.execute(GraphicsResourceAllocator.UNPOOLED);
		ubo.rotate();
	}

	private static void upload(float intensity, int time, float skyBright, float head) {
		if (ubo == null)
			ubo = new MappableRingBuffer(() -> "ChromatiCraft UaZone", UBO_USAGE, 16);
		try (var view = ubo.currentBuffer().map(false, true)) {
			Std140Builder.intoBuffer(view.data()).putVec4(intensity, time, skyBright, head);
		}
	}

	private static void draw(RenderTarget source, RenderTarget target) {
		var encoder = RenderSystem.getDevice().createCommandEncoder();
		try (var pass = encoder.createRenderPass(() -> "ChromatiCraft UA zone shader",
				target.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(ChromaRenderPipelines.UA_ZONE));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("UaZone", ubo.currentBuffer());
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
}
