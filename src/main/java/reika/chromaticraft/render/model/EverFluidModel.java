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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import reika.chromaticraft.block.dimension.structure.water.BlockEverFluid26;

/**
 * Chunk-mesh port of V33a's {@code EverFluidRenderer} ISBRH for Channeled Flow's fluid.
 *
 * <p>A source stands 7/8 high, a flowing cell 3/4, and a cell under any liquid is full; each top corner averages
 * the cell and its three neighbours there (solid neighbours count as the cell's own height, open ones as zero,
 * and any neighbour at 7/8 or more wins), exactly as the old renderer did. The top and the sides are drawn from
 * both faces, the sides sampling half the still-water icon, with V33a's 1.0 / 0.5 / 0.8 / 0.6 face brightness.
 * Upstream's still-water icon already carried blue; its white multiplier left that blue intact. The modern
 * icon is greyscale, so tint slot zero supplies normal water blue while these vertex colours retain the
 * original face brightness. This block never used biome-dependent colouring upstream.
 */
public final class EverFluidModel implements DynamicBlockStateModel {

	private static final float LIGHT_Y_NEG = 0.5F;
	private static final float LIGHT_Y_POS = 1.0F;
	private static final float LIGHT_XZ_NEG = 0.8F;
	private static final float LIGHT_XZ_POS = 0.6F;
	private static final double RENDER_OFFSET = 0.0010000000474974513D;

	private final Material.Baked still;

	private EverFluidModel(Material.Baked still) {
		this.still = still;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		Block block = state.getBlock();
		if (!(block instanceof BlockEverFluid26))
			return;
		boolean renderTop = !level.getBlockState(pos.above()).is(block);
		boolean renderBottom = shouldSideRender(level, pos.below(), block) && !level.getBlockState(pos.below()).is(block);
		boolean[] renderSides = {
				shouldSideRender(level, pos.north(), block),
				shouldSideRender(level, pos.south(), block),
				shouldSideRender(level, pos.west(), block),
				shouldSideRender(level, pos.east(), block)
		};
		if (!renderTop && !renderBottom && !renderSides[0] && !renderSides[1] && !renderSides[2] && !renderSides[3])
			return;

		double hNW, hSW, hSE, hNE;
		float f11 = height(level, pos, block, 0);
		if (f11 != 1) {
			float f00 = height(level, pos.offset(-1, 0, -1), block, f11);
			float f01 = height(level, pos.offset(-1, 0, 0), block, f11);
			float f02 = height(level, pos.offset(-1, 0, 1), block, f11);
			float f10 = height(level, pos.offset(0, 0, -1), block, f11);
			float f12 = height(level, pos.offset(0, 0, 1), block, f11);
			float f20 = height(level, pos.offset(1, 0, -1), block, f11);
			float f21 = height(level, pos.offset(1, 0, 0), block, f11);
			float f22 = height(level, pos.offset(1, 0, 1), block, f11);
			hNW = average(new float[] {f00, f01, f10, f11});
			hSW = average(new float[] {f01, f02, f12, f11});
			hSE = average(new float[] {f12, f21, f22, f11});
			hNE = average(new float[] {f10, f20, f21, f11});
		}
		else {
			hNW = hSW = hSE = hNE = f11;
		}

		QuadCollection.Builder quads = new QuadCollection.Builder();
		if (renderTop) {
			hNW -= RENDER_OFFSET;
			hSW -= RENDER_OFFSET;
			hSE -= RENDER_OFFSET;
			hNE -= RENDER_OFFSET;
			int c = grey(LIGHT_Y_POS);
			quad(quads, c, 0, hNW, 0, 0, 0, 0, hSW, 1, 0, 1, 1, hSE, 1, 1, 1, 1, hNE, 0, 1, 0);
			quad(quads, c, 0, hNW, 0, 0, 0, 1, hNE, 0, 1, 0, 1, hSE, 1, 1, 1, 0, hSW, 1, 0, 1);
		}
		if (renderBottom) {
			int c = grey(LIGHT_Y_NEG);
			double y = RENDER_OFFSET;
			quad(quads, c, 0, y, 1, 0, 1, 0, y, 0, 0, 0, 1, y, 0, 1, 0, 1, y, 1, 1, 1);
		}
		for (int side = 0; side < 4; side++) {
			if (!renderSides[side])
				continue;
			double ty1, ty2, tx1, tx2, tz1, tz2;
			if (side == 0) {
				ty1 = hNW; ty2 = hNE; tx1 = 0; tx2 = 1; tz1 = RENDER_OFFSET; tz2 = RENDER_OFFSET;
			}
			else if (side == 1) {
				ty1 = hSE; ty2 = hSW; tx1 = 1; tx2 = 0; tz1 = 1 - RENDER_OFFSET; tz2 = 1 - RENDER_OFFSET;
			}
			else if (side == 2) {
				ty1 = hSW; ty2 = hNW; tx1 = RENDER_OFFSET; tx2 = RENDER_OFFSET; tz1 = 1; tz2 = 0;
			}
			else {
				ty1 = hNE; ty2 = hSE; tx1 = 1 - RENDER_OFFSET; tx2 = 1 - RENDER_OFFSET; tz1 = 0; tz2 = 1;
			}
			float u1 = 0;
			float u2 = 0.5F;
			float v1 = (float)((1 - ty1) * 0.5);
			float v2 = (float)((1 - ty2) * 0.5);
			float v3 = 0.5F;
			int c = grey(LIGHT_Y_POS * (side < 2 ? LIGHT_XZ_NEG : LIGHT_XZ_POS));
			quad(quads, c, tx1, ty1, tz1, u1, v1, tx2, ty2, tz2, u2, v2, tx2, 0, tz2, u2, v3, tx1, 0, tz1, u1, v3);
			quad(quads, c, tx1, ty1, tz1, u1, v1, tx1, 0, tz1, u1, v3, tx2, 0, tz2, u2, v3, tx2, ty2, tz2, u2, v2);
		}
		parts.add(new SimpleModelWrapper(quads.build(), false, still));
	}

	/** V33a {@code shouldSideBeRendered}: never against itself, otherwise against anything not opaque. */
	private static boolean shouldSideRender(BlockAndTintGetter level, BlockPos pos, Block block) {
		BlockState other = level.getBlockState(pos);
		if (other.is(block))
			return false;
		return !other.canOcclude();
	}

	/** V33a {@code getFluidHeightForRender}. */
	private static float height(BlockAndTintGetter level, BlockPos pos, Block block, float base) {
		BlockState state = level.getBlockState(pos);
		if (state.is(block)) {
			BlockState above = level.getBlockState(pos.above());
			if (above.is(block) || !above.getFluidState().isEmpty())
				return 1;
			return state.getValue(BlockEverFluid26.SOURCE) ? 0.875F : 0.75F;
		}
		return state.isSolid() ? base : 0;
	}

	/** V33a {@code getFluidHeightAverage}. */
	private static float average(float[] flow) {
		float total = 0;
		int count = 0;
		float end = 0;
		for (float f : flow) {
			if (f >= 0.875F && end != 1F)
				end = f;
			if (f >= 0) {
				total += f;
				count++;
			}
		}
		if (end == 0)
			end = total / count;
		return end;
	}

	private static int grey(float f) {
		int g = (int)(f * 255);
		return 0xFF000000 | g << 16 | g << 8 | g;
	}

	private void quad(QuadCollection.Builder quads, int color, double... d) {
		double[] p = new double[12];
		float[] uv = new float[8];
		for (int i = 0; i < 4; i++) {
			p[i * 3] = d[i * 5];
			p[i * 3 + 1] = d[i * 5 + 1];
			p[i * 3 + 2] = d[i * 5 + 2];
			uv[i * 2] = (float)d[i * 5 + 3];
			uv[i * 2 + 1] = (float)d[i * 5 + 4];
		}
		double nx = 0, ny = 0, nz = 0;
		for (int i = 0; i < 4; i++) {
			int j = (i + 1) % 4;
			nx += (p[i * 3 + 1] - p[j * 3 + 1]) * (p[i * 3 + 2] + p[j * 3 + 2]);
			ny += (p[i * 3 + 2] - p[j * 3 + 2]) * (p[i * 3] + p[j * 3]);
			nz += (p[i * 3] - p[j * 3]) * (p[i * 3 + 1] + p[j * 3 + 1]);
		}
		double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
		float fx = len < 1.0E-9 ? 0 : (float)(nx / len);
		float fy = len < 1.0E-9 ? 1 : (float)(ny / len);
		float fz = len < 1.0E-9 ? 0 : (float)(nz / len);
		QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
		vertex.setSprite(still, Transparency.TRANSLUCENT);
		vertex.setTintIndex(0);
		vertex.setShadeOverride(Direction.UP);
		vertex.setAmbientOcclusion(false);
		vertex.setDirection(Direction.getApproximateNearest(fx, fy, fz));
		for (int i = 0; i < 4; i++)
			vertex.addVertex((float)p[i * 3], (float)p[i * 3 + 1], (float)p[i * 3 + 2])
					.setColor(color)
					.setUv(still.sprite().getU(uv[i * 2]), still.sprite().getV(uv[i * 2 + 1]))
					.setNormal(fx, fy, fz);
		quads.addUnculledFace(vertex.bakeQuad());
	}

	@Override public Material.Baked particleMaterial() { return still; }
	@Override public int materialFlags() { return BakedQuad.FLAG_TRANSLUCENT; }

	public record Unbaked(Identifier still) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("still").forGetter(Unbaked::still)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new EverFluidModel(baker.materials().get(new Material(still), () -> "chromaticraft:ever_fluid"));
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
