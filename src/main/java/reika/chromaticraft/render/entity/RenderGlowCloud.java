package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import reika.chromaticraft.entity.EntityGlowCloud;

/**
 * V33a RenderGlowCloud draws no mesh at all: outside the ChromatiCraft pocket dimension it is a
 * complete no-op, and inside that dimension it feeds the entity's position/distance into a dedicated
 * post-process glow shader ({@code ChromaShaders.DIMGLOWCLOUD}) gated by a line-of-sight check to the
 * camera ({@code RayTracer.getVisualLOSForRenderCulling()}). RayTracer itself is fully ported, but
 * two other V33a dependencies for that dimension-only branch are not:
 * {@code reika.dragonapi.io.shaders.*} (ShaderHook/ShaderRegistry/ShaderProgram/ShaderDomain) was
 * replaced wholesale by the modern PostChain-based {@code reika.dragonapi.extras.shader} package and
 * was never re-created there, and {@code ExtraChromaIDs.DIMID} (the pocket dimension itself) is not
 * registered in 26.2 yet either.
 *
 * <p>Since Glow Cloud currently only spawns in Luminous Cliffs (an overworld biome), this renderer is
 * legitimately empty for every world it can appear in right now — exactly matching V33a's own
 * behaviour there. The cloud's visible presence comes entirely from its particles
 * ({@code ChromaParticle.spawnGlowCloudAmbient/Attack/Death}, driven from {@link EntityGlowCloud}
 * itself) and, once ported, its placed ethereal light block.
 *
 * <p>CHROMA-PORT: original dimension-gated body, preserved here for when the pocket dimension and
 * shader package are ready:
 * <pre>
 * if (level.dimension() == CHROMATICRAFT_DIMENSION) {
 *     LOS.setOrigins(entity.getX(), entity.getY(), entity.getZ(), camera.getX(), camera.getY(), camera.getZ());
 *     if (LOS.isClearLineOfSight(entity)) {
 *         ChromaShaders.DIMGLOWCLOUD.setIntensity(1);
 *         ChromaShaders.DIMGLOWCLOUD.clearOnRender = true;
 *         ChromaShaders.DIMGLOWCLOUD.getShader().addFocus(entity);
 *         // distance/factor falloff exactly as pristine RenderGlowCloud.doRender()
 *         ChromaShaders.DIMGLOWCLOUD.getShader().modifyLastCompoundFocus(f, vars);
 *     }
 * }
 * </pre>
 */
public class RenderGlowCloud extends EntityRenderer<EntityGlowCloud, EntityRenderState> {

	public RenderGlowCloud(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		// Intentionally empty; see class javadoc.
	}
}
