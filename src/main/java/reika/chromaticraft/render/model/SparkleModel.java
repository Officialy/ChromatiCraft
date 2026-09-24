package reika.chromaticraft.render.model;

import java.util.List;
import java.util.Random;

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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code SparklingBlockRender}: the proxy block, with a scatter of glinting motes on each face.
 *
 * <p>For every face upstream picks one to six motes. A mote is a single texel-sized square, drawn
 * full-bright just proud of the face, carrying the fade icon (the fast-pulsing one a third of the time)
 * in a random near-white colour. Around it, on the four sides, sit texels of the dye-block texture at
 * half that brightness, which is what makes each mote read as a little plus-shaped glint; a mote centre
 * already drawn on the face is not painted over by a later mote's surround. Motes keep one texel clear
 * of the edge.
 *
 * <p>Upstream drew from an unseeded random on every chunk rebuild, so the motes moved whenever the
 * section re-meshed. Here they are seeded from the position: the same scatter, but stable. Every face's
 * motes are generated, and each face's are culled with that face, which is upstream's
 * {@code shouldSideBeRendered} test.
 */
public final class SparkleModel implements DynamicBlockStateModel {

	/** Upstream's side index {@code s} and the face it actually draws on (the opposite of {@code s}'s direction). */
	private static final Direction[] FACES = {
			Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST,
	};
	/** Upstream's outward offset {@code o}. */
	private static final float OFFSET = 0.005F;

	private final BlockStateModel proxy;
	private final Material.Baked fade;
	private final Material.Baked fadeFast;
	private final Material.Baked surround;
	private final int flags;

	private SparkleModel(BlockStateModel proxy, Material.Baked fade, Material.Baked fadeFast, Material.Baked surround,
			int flags) {
		this.proxy = proxy;
		this.fade = fade;
		this.fadeFast = fadeFast;
		this.surround = surround;
		this.flags = flags;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		proxy.collectParts(level, pos, state, random, parts);

		Random rand = new Random(Mth.getSeed(pos));
		QuadCollection.Builder quads = new QuadCollection.Builder();
		for (Direction face : FACES) {
			int n = 1 + rand.nextInt(6);
			boolean[][] draw = new boolean[16][16];
			for (int i = 0; i < n; i++) {
				int c = 0xFF000000 | ReikaColorAPI.RGBtoHex(192 + rand.nextInt(64), 192 + rand.nextInt(64), 192 + rand.nextInt(64));
				int c2 = 0xFF000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(c, 0.5F);
				Material.Baked icon = rand.nextInt(3) == 0 ? fadeFast : fade;
				int da = ReikaRandomHelper.getRandomBetween(1, 14, rand);
				int db = ReikaRandomHelper.getRandomBetween(1, 14, rand);
				draw[da][db] = true;
				for (int ar = -1; ar <= 1; ar++) {
					for (int br = -1; br <= 1; br++) {
						if (ar != 0 && br != 0)
							continue;
						boolean centre = ar == 0 && br == 0;
						if (centre || !draw[da + ar][db + br])
							quads.addCulledFace(face, texel(face, da + ar, db + br, centre ? icon : surround, centre ? c : c2));
					}
				}
			}
		}
		parts.add(new SimpleModelWrapper(quads.build(), false, proxy.particleMaterial()));
	}

	/**
	 * One texel of a face, {@code a} and {@code b} along the face's two in-plane axes in upstream's
	 * assignment (x/z on top and bottom, x/y on north and south, z/y on east and west).
	 */
	private static BakedQuad texel(Direction face, int a, int b, Material.Baked material, int color) {
		float a0 = a / 16F;
		float a1 = (a + 1) / 16F;
		float b0 = b / 16F;
		float b1 = (b + 1) / 16F;
		MutableQuad quad = new MutableQuad();
		switch (face.getAxis()) {
			case Y -> quad.setCubeFace(face, a0, -OFFSET, b0, a1, 1 + OFFSET, b1);
			case Z -> quad.setCubeFace(face, a0, b0, -OFFSET, a1, b1, 1 + OFFSET);
			case X -> quad.setCubeFace(face, -OFFSET, b0, a0, 1 + OFFSET, b1, a1);
		}
		quad.setSprite(material);
		for (int i = 0; i < 4; i++) {
			float along = switch (face.getAxis()) {
				case Y, Z -> quad.x(i);
				case X -> quad.z(i);
			};
			float across = switch (face.getAxis()) {
				case Y -> quad.z(i);
				case Z, X -> quad.y(i);
			};
			quad.setUvFromSprite(i, along > (a0 + a1) / 2 ? 1 : 0, across > (b0 + b1) / 2 ? 0 : 1);
		}
		quad.setColor(color);
		quad.recomputeNormals(false);
		// Tessellator brightness 240: full-bright, unshaded, unsmoothed.
		quad.setShade(false);
		quad.setAmbientOcclusion(false);
		quad.setLightEmission(15);
		quad.setTintIndex(-1);
		return quad.toBakedQuad();
	}

	@Override public Material.Baked particleMaterial() { return proxy.particleMaterial(); }
	@Override public int materialFlags() { return flags; }

	public record Unbaked(BlockStateModel.Unbaked proxy, Identifier fade, Identifier fadeFast, Identifier surround)
			implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				BlockStateModel.Unbaked.CODEC.fieldOf("proxy").forGetter(Unbaked::proxy),
				Identifier.CODEC.fieldOf("fade").forGetter(Unbaked::fade),
				Identifier.CODEC.fieldOf("fade_fast").forGetter(Unbaked::fadeFast),
				Identifier.CODEC.fieldOf("surround").forGetter(Unbaked::surround)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			BlockStateModel bakedProxy = proxy.bake(baker);
			Material.Baked fadeMat = material(baker, fade);
			Material.Baked fastMat = material(baker, fadeFast);
			Material.Baked surroundMat = material(baker, surround);
			int flags = bakedProxy.materialFlags();
			for (Material.Baked m : new Material.Baked[] {fadeMat, fastMat, surroundMat})
				flags |= texel(Direction.UP, 1, 1, m, -1).materialInfo().flags();
			return new SparkleModel(bakedProxy, fadeMat, fastMat, surroundMat, flags);
		}

		private static Material.Baked material(ModelBaker baker, Identifier id) {
			return baker.materials().get(new Material(id), () -> "chromaticraft:sparkle/" + id);
		}

		@Override public void resolveDependencies(Resolver resolver) { proxy.resolveDependencies(resolver); }
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
