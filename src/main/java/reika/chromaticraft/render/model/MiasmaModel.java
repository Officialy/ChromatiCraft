package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * V33a {@code DimensionDecoRenderer.renderEffect} for {@code MIASMA}: the gas cloud, which is not a
 * cube.
 *
 * <p>Upstream draws three quads, each emitted twice in opposite winding so both faces show: one
 * horizontal sheet through the middle of the block, and two vertical sheets crossed on the diagonals.
 * All three are <em>larger than the block</em> — the horizontal one spans -0.5 to 1.5 on both axes and
 * the crossed pair stand a full block above and below — so a field of Miasma reads as one continuous
 * fog rather than a grid of cubes. That overhang is the whole look, and it is why a cube model was
 * never going to be close: what was shipping was {@code cube_all} on a texture that is white at alpha
 * 22, which is to say very nearly nothing.
 *
 * <p>The blue is not in the texture either. Upstream tints it per position with
 * {@code getModifiedHue(0x0000ff, 220 + 80*sin((x*x*2 + y*y + z*z*8)/2000000))}, so the cloud shifts
 * slowly through blues across a landscape; that lives on the tint source in {@code ChromaBlockColors},
 * reached from here by tint index 0. Upstream's {@code setBrightness(240)} is full-bright, and
 * {@code renderIconInPass} puts Miasma in pass 1 alone, so this is translucent and unlit.
 *
 * <p>The texture's own animation — forty-eight frames at two ticks each — comes from the sprite, so
 * nothing here has to drive it.
 */
public final class MiasmaModel implements DynamicBlockStateModel {

	/** V33a's {@code s}: the sheets reach a full block past the block's own bounds. */
	private static final float S = 1;
	/** V33a insets the crossed pair to three quarters on the horizontal axes. */
	private static final float D = 0.75F;

	private final BlockStateModelPart part;
	private final Material.Baked material;

	private MiasmaModel(BlockStateModelPart part, Material.Baked material) {
		this.part = part;
		this.material = material;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> parts) {
		parts.add(part);
	}

	@Override public Material.Baked particleMaterial() { return material; }
	@Override public int materialFlags() { return part.materialFlags() | BakedQuad.FLAG_TRANSLUCENT; }

	public record Unbaked(Identifier texture) implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance ->
				instance.group(Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture))
						.apply(instance, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked baked = baker.materials().get(new Material(texture, true),
					() -> "chromaticraft:miasma/" + texture);
			QuadCollection.Builder quads = new QuadCollection.Builder();
			// The horizontal sheet, then the two crossed vertical ones, in upstream's own order.
			emitBothFaces(baker, quads, baked, new float[][] {
					{0.5F - S, 0.5F, 0.5F - S}, {0.5F + S, 0.5F, 0.5F - S},
					{0.5F + S, 0.5F, 0.5F + S}, {0.5F - S, 0.5F, 0.5F + S}});
			emitBothFaces(baker, quads, baked, new float[][] {
					{0.5F - S * D, 0.5F - S, 0.5F - S * D}, {0.5F + S * D, 0.5F - S, 0.5F + S * D},
					{0.5F + S * D, 0.5F + S, 0.5F + S * D}, {0.5F - S * D, 0.5F + S, 0.5F - S * D}});
			emitBothFaces(baker, quads, baked, new float[][] {
					{0.5F + S * D, 0.5F - S, 0.5F - S * D}, {0.5F - S * D, 0.5F - S, 0.5F + S * D},
					{0.5F - S * D, 0.5F + S, 0.5F + S * D}, {0.5F + S * D, 0.5F + S, 0.5F - S * D}});
			return new MiasmaModel(new SimpleModelWrapper(quads.build(), false, baked), baked);
		}

		/**
		 * One sheet, emitted twice in opposite winding. Upstream does this by writing the same four
		 * vertices out forwards and then backwards rather than by disabling face culling, and the
		 * distinction matters here: these are baked into the chunk mesh, where there is no render state
		 * to turn culling off in.
		 */
		private static void emitBothFaces(ModelBaker baker, QuadCollection.Builder quads,
				Material.Baked material, float[][] corners) {
			// V33a's UV corners: (u,v), (du,v), (du,dv), (u,dv) over the whole sprite. These are
			// fractions, not texels -- TextureAtlasSprite.getU interpolates u0..u1 by its argument.
			float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
			emit(baker, quads, material, corners, uv, false);
			// Reversed, and the UVs travel with their vertices so the back face is not mirrored.
			emit(baker, quads, material, corners, uv, true);
		}

		private static void emit(ModelBaker baker, QuadCollection.Builder quads,
				Material.Baked material, float[][] corners, float[][] uv, boolean reversed) {
			QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
			vertex.setSprite(material, Transparency.TRANSLUCENT);
			// Tint index 0: the blue comes from the position-dependent tint source, not the texture.
			vertex.setTintIndex(0);
			vertex.setShade(false);
			// V33a setBrightness(240): the cloud lights itself.
			vertex.setLightEmission(15);
			vertex.setAmbientOcclusion(false);
			float[] normal = normalOf(corners, reversed);
			for (int i = 0; i < 4; i++) {
				int index = reversed ? 3 - i : i;
				float[] corner = corners[index];
				vertex.addVertex(corner[0], corner[1], corner[2])
						.setColor(0xFFFFFFFF)
						.setUv(material.sprite().getU(uv[index][0]), material.sprite().getV(uv[index][1]))
						.setNormal(normal[0], normal[1], normal[2]);
			}
			quads.addUnculledFace(vertex.bakeQuad(baker.interner()));
		}

		/** The sheet's own normal, flipped for the reversed copy so each face is lit as it faces. */
		private static float[] normalOf(float[][] corners, boolean reversed) {
			float[] a = {corners[1][0] - corners[0][0], corners[1][1] - corners[0][1],
					corners[1][2] - corners[0][2]};
			float[] b = {corners[3][0] - corners[0][0], corners[3][1] - corners[0][1],
					corners[3][2] - corners[0][2]};
			float x = a[1] * b[2] - a[2] * b[1];
			float y = a[2] * b[0] - a[0] * b[2];
			float z = a[0] * b[1] - a[1] * b[0];
			float length = (float)Math.sqrt(x * x + y * y + z * z);
			if (length == 0)
				return new float[] {0, 1, 0};
			float sign = reversed ? -1 : 1;
			return new float[] {sign * x / length, sign * y / length, sign * z / length};
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
