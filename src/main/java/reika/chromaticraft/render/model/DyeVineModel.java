package reika.chromaticraft.render.model;

import net.minecraft.core.Direction;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import reika.chromaticraft.block.dye26.BlockDyeVine;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code DyeVineRenderer} and {@code BlockDyeVine.render}, as a chunk-mesh model.
 *
 * <p>Upstream's renderer is fully deterministic: it reseeds two {@link Random}s from the block position
 * on every draw — one from {@code chunkXZ2Int(x, z) ^ y}, one from {@code chunkXZ2Int(x, z)} alone — and
 * takes every choice from them in a fixed order. That is replayed here exactly, so each vine gets the
 * sway, texture, width and shade it had in V33a:
 * <ul>
 * <li>from the column seed: a shift of up to a quarter block in X and Z and a lift of 1/16-3/16, shared
 * by every vine in the column so a strand stays straight;</li>
 * <li>from the block seed: one of four strand textures, a shade of 67.5-87.5% of the leaf colour over
 * dark grey, a further 1/32 jitter in X and Z, and how far apart the four planes stand (0-1/8).</li>
 * </ul>
 * The strand is DragonAPI's crop shape: four double-sided planes, two either side of the centre on each
 * axis. A fertile vine draws the same planes a second time with the glowing texture, full-bright, in its
 * crystal colour.
 *
 * <p>The colours are baked into the vertices rather than tinted, since each depends on the position.
 */
public final class DyeVineModel implements DynamicBlockStateModel {

	private final Material.Baked[] strands;
	private final Material.Baked glow;
	private final int flags;

	private DyeVineModel(Material.Baked[] strands, Material.Baked glow, int flags) {
		this.strands = strands;
		this.glow = glow;
		this.flags = flags;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		if (!(state.getBlock() instanceof BlockDyeVine vine))
			return;
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		// DyeVineRenderer.renderWorldBlock
		long column = chunkXZ2Int(x, z);
		Random renderRand = new Random(column ^ y);
		renderRand.nextBoolean();
		renderRand.nextBoolean();
		Random renderRandNoY = new Random(column);
		renderRandNoY.nextBoolean();
		renderRandNoY.nextBoolean();
		double dx = ReikaRandomHelper.getRandomPlusMinus(0, 0.25, renderRandNoY);
		double dz = ReikaRandomHelper.getRandomPlusMinus(0, 0.25, renderRandNoY);
		double dy = ReikaRandomHelper.getRandomPlusMinus(0.125, 0.0625, renderRandNoY);

		// BlockDyeVine.render
		Material.Baked strand = strands[renderRand.nextInt(strands.length)];
		float fc = (float)ReikaRandomHelper.getRandomBetween(0.675, 0.875, renderRand);
		int color = 0xFF000000 | ReikaColorAPI.mixColors(BlockDyeVine.leafColor(vine.getElement()), BlockDyeVine.SHADE, fc);
		dx += ReikaRandomHelper.getRandomPlusMinus(0, 0.03125, renderRand);
		dz += ReikaRandomHelper.getRandomPlusMinus(0, 0.03125, renderRand);
		double w = ReikaRandomHelper.getRandomBetween(0, 0.125, renderRand);

		QuadCollection.Builder quads = new QuadCollection.Builder();
		cropPlanes(quads, strand, color, 0, (float)dx, (float)dy, (float)dz, (float)w);
		if (vine.isFertile())
			cropPlanes(quads, glow, 0xFF000000 | vine.getElement().getColor(), 15, (float)dx, (float)dy, (float)dz, (float)w);
		parts.add(new SimpleModelWrapper(quads.build(), false, strands[0]));
	}

	/** Minecraft 1.7.10 {@code ChunkCoordIntPair.chunkXZ2Int}, which upstream used as a column seed. */
	private static long chunkXZ2Int(int x, int z) {
		return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
	}

	/**
	 * DragonAPI {@code ReikaRenderHelper.renderCropTypeTex(..., space, h = 1, s = 0.5)}: planes at
	 * {@code 0.5 +/- space} on each axis, spanning the whole block, dropped 1/16, each drawn both ways.
	 * Each entry is the pair of plane ends in upstream's vertex order.
	 */
	private static void cropPlanes(QuadCollection.Builder quads, Material.Baked material, int color, int light,
			float dx, float dy, float dz, float space) {
		float a = 0.5F - space;
		float b = 0.5F + space;
		float bottom = -0.0625F + dy;
		float top = bottom + 1;
		float[][] ends = {
				{a, 0, a, 1}, {a, 1, a, 0}, {b, 1, b, 0}, {b, 0, b, 1},
				{0, a, 1, a}, {1, a, 0, a}, {1, b, 0, b}, {0, b, 1, b},
		};
		for (float[] end : ends)
			quads.addUnculledFace(plane(material, color, light,
					end[0] + dx, end[1] + dz, end[2] + dx, end[3] + dz, bottom, top));
	}

	/** Upstream's vertex order: top of end A, bottom of A, bottom of B, top of B. */
	private static BakedQuad plane(Material.Baked material, int color, int light,
			float ax, float az, float bx, float bz, float bottom, float top) {
		MutableQuad quad = new MutableQuad();
		quad.setSprite(material);
		quad.setPosition(0, ax, top, az);
		quad.setPosition(1, ax, bottom, az);
		quad.setPosition(2, bx, bottom, bz);
		quad.setPosition(3, bx, top, bz);
		quad.setUvFromSprite(0, 0, 0);
		quad.setUvFromSprite(1, 0, 1);
		quad.setUvFromSprite(2, 1, 1);
		quad.setUvFromSprite(3, 1, 0);
		quad.setColor(color);
		quad.recomputeNormals(true);
		// The 1.7.10 tessellator path set its colour directly, with no face shading and no smoothing.
		quad.setShadeOverride(Direction.UP);
		quad.setAmbientOcclusion(false);
		quad.setLightEmission(light);
		quad.setTintIndex(-1);
		return quad.toBakedQuad();
	}

	@Override public Material.Baked particleMaterial() { return strands[0]; }
	@Override public int materialFlags() { return flags; }

	public record Unbaked(List<Identifier> strands, Identifier glow) implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.listOf().fieldOf("strands").forGetter(Unbaked::strands),
				Identifier.CODEC.fieldOf("glow").forGetter(Unbaked::glow)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked[] baked = new Material.Baked[strands.size()];
			int flags = 0;
			for (int i = 0; i < baked.length; i++) {
				Identifier id = strands.get(i);
				baked[i] = baker.materials().get(new Material(id), () -> "chromaticraft:dye_vine/" + id);
				flags |= plane(baked[i], -1, 0, 0, 0, 1, 1, 0, 1).materialInfo().flags();
			}
			Material.Baked glowMat = baker.materials().get(new Material(glow), () -> "chromaticraft:dye_vine/" + glow);
			flags |= plane(glowMat, -1, 15, 0, 0, 1, 1, 0, 1).materialInfo().flags();
			return new DyeVineModel(baked, glowMat, flags);
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
