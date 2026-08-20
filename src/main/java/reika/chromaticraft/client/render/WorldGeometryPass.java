package reika.chromaticraft.client.render;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

/**
 * Draws arbitrary textured geometry into the world, from a level-render stage.
 *
 * <h2>Why this exists</h2>
 *
 * <p>26.2 has two ways to get geometry on screen and neither covers this case. The submit pipeline —
 * {@code SubmitNodeCollector.submitCustomGeometry}, which the aurora curtain uses — is reached through
 * an entity or block-entity renderer, and every collector inside {@code LevelRenderer} is private, so
 * a {@code RenderLevelStageEvent} handler cannot get one. Post-effect chains, which RotaryCraft's heat
 * ripple uses, operate on the finished frame rather than on world geometry. What is left is what
 * vanilla itself does for weather: build a mesh, upload it, and issue one indexed draw against the
 * main target. This wraps that so a caller only has to emit vertices.
 *
 * <p>The buffer is retained between frames and only grown, because this runs every frame and
 * allocating a fresh GPU buffer each time would churn badly for geometry that is often large.
 *
 * <p>The pass targets the main colour and depth textures with {@code OptionalDouble.empty()} for both,
 * meaning neither is cleared — this composites over the scene rather than replacing it.
 */
public final class WorldGeometryPass {

	private @Nullable GpuBuffer vertexBuffer;
	private final String label;
	/**
	 * Retained across frames, and the reason matters: a fresh {@link ByteBufferBuilder} per frame means
	 * a malloc per frame and then a chain of reallocs as it fills, which showed up in a profile as
	 * {@code ByteBufferBuilder.resize} -> {@code JEmallocAllocator.realloc} on both the sky and the sky
	 * rivers. Keeping one lets it settle at the size this pass actually needs and never grow again.
	 * Vanilla does the same thing with the builders in {@code RenderBuffers}.
	 */
	private final ByteBufferBuilder scratch = new ByteBufferBuilder(
			INITIAL_VERTICES * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());

	/** Enough for the sky field, which is the largest thing drawn through this. */
	private static final int INITIAL_VERTICES = 64 * 1024;

	public WorldGeometryPass(String label) {
		this.label = label;
	}

	/**
	 * Emits and draws one batch.
	 *
	 * @param pipeline the pipeline to draw with; it must declare {@link DefaultVertexFormat#POSITION_TEX_COLOR}
	 *                 and the {@code GLOBALS}, {@code MATRICES_PROJECTION} and {@code SAMPLER0} bind groups
	 * @param texture  bound as {@code Sampler0}
	 * @param builder  fills the mesh; called once, and may emit nothing
	 */
	public void draw(RenderPipeline pipeline, Identifier texture, org.joml.Matrix4f modelView,
			Consumer<BufferBuilder> builder) {
		Minecraft minecraft = Minecraft.getInstance();
		GpuBuffer vertices;
		GpuBuffer indices;
		IndexType indexType;
		int indexCount;
		// Deliberately not ByteBufferBuilder.exactlySized: that sets the maximum capacity equal to the
		// initial one, so the buffer cannot grow and overflowing it throws rather than reallocating.
		// A caller here does not know its vertex count up front -- the sky field alone runs to tens of
		// thousands of vertices -- so this is a starting size on a buffer that is free to grow, and it
		// is the same buffer every frame so that growth happens once rather than continually.
		{
			BufferBuilder buffer = new BufferBuilder(scratch, PrimitiveTopology.QUADS,
					DefaultVertexFormat.POSITION_TEX_COLOR);
			builder.accept(buffer);
			MeshData mesh = buffer.build();
			// Nothing was emitted; there is no mesh to draw and buildOrThrow would have thrown.
			if (mesh == null)
				return;
			try (mesh) {
				vertices = this.upload(mesh.vertexBuffer());
				RenderSystem.AutoStorageIndexBuffer sequential =
						RenderSystem.getSequentialBuffer(mesh.drawState().primitiveTopology());
				indexCount = mesh.drawState().indexCount();
				indices = sequential.getBuffer(indexCount);
				indexType = sequential.type();
			}
		}

		AbstractTexture bound = minecraft.getTextureManager().getTexture(texture);
		var target = minecraft.gameRenderer.mainRenderTarget();
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> label, target.getColorTextureView(), Optional.empty(),
				target.getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.bindTexture("Sampler0", bound.getTextureView(),
					RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.setIndexBuffer(indices, indexType);
			pass.setVertexBuffer(0, vertices.slice());
			pass.drawIndexed(indexCount, 1, 0, 0, 0);
		}
	}

	/** Reuses the buffer unless this batch needs a bigger one. */
	private GpuBuffer upload(ByteBuffer data) {
		var device = RenderSystem.getDevice();
		if (vertexBuffer == null || vertexBuffer.size() < data.remaining()) {
			if (vertexBuffer != null)
				vertexBuffer.close();
			vertexBuffer = device.createBuffer(() -> label + " vertices",
					GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, data.remaining());
		}
		device.createCommandEncoder().writeToBuffer(vertexBuffer.slice(), data);
		return vertexBuffer;
	}
}
