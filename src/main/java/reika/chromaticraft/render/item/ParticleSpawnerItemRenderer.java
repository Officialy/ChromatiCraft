package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.tesr.RenderParticleSpawner;

/** V33a drew the Particle Spawner item with its TESR on a render-only tile, which is never faded: full opacity. */
public final class ParticleSpawnerItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "particle_spawner");

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		RenderParticleSpawner.submitFrame(poseStack, collector, 1);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0.3125F, 0.3125F, 0.3125F));
		output.accept(new Vector3f(0.6875F, 0.6875F, 0.6875F));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public ParticleSpawnerItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new ParticleSpawnerItemRenderer();
		}
	}
}
