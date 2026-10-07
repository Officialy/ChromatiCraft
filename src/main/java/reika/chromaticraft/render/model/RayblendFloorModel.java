package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import reika.chromaticraft.block.dimension.structure.rayblend.BlockRayblendFloor26;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Chunk-mesh port of V33a's {@code RayBlendFloorRenderer}: full-bright, translucent pass.
 *
 * <p>Every face not against another floor tile draws {@code dimstruct/rayblendfloor} tinted with the cell's cage
 * colour (white when blocked or uncaged). The top face also draws, 0.01 above, the X icon on a blocked cell or the
 * cage colour's engraved rune. Every drawn face then draws DragonAPI's {@code EdgeDetectionRenderer} glints (with
 * {@code renderOnOcclusion} off) along each edge whose neighbour is not a floor tile of the same region, which
 * outlines the puzzle's regions.
 */
public final class RayblendFloorModel implements DynamicBlockStateModel {

	private static final double EDGE_LIFT = 0.005;
	private static final double OVERLAY_LIFT = 0.01;

	private final Material.Baked base;
	private final Material.Baked cross;
	private final Material.Baked[] runes;
	private final Material.Baked[] edges;
	private final int flags;

	private RayblendFloorModel(Material.Baked base, Material.Baked cross, Material.Baked[] runes, Material.Baked[] edges) {
		this.base = base;
		this.cross = cross;
		this.runes = runes;
		this.edges = edges;
		QuadCollection.Builder sample = new QuadCollection.Builder();
		face(sample, base, Direction.UP, 0, 0xFFFFFFFF);
		face(sample, cross, Direction.UP, OVERLAY_LIFT, 0xFFFFFFFF);
		for (Material.Baked m : edges)
			SpecialShieldModel.quad(sample, m, Direction.UP, EDGE_LIFT, 0xFFFFFFFF, true, true);
		flags = new SimpleModelWrapper(sample.build(), false, base).materialFlags();
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		boolean blocked = state.getValue(BlockRayblendFloor26.BLOCKED);
		CrystalElement cage = BlockRayblendFloor26.cage(state);
		int tint = blocked || cage == null ? 0xFFFFFFFF : 0xFF000000 | cage.getColor();
		int region = state.getValue(BlockRayblendFloor26.REGION);
		QuadCollection.Builder quads = new QuadCollection.Builder();
		for (Direction f : Direction.values()) {
			if (level.getBlockState(pos.relative(f)).getBlock() instanceof BlockRayblendFloor26)
				continue;
			face(quads, base, f, 0, tint);
			if (f == Direction.UP) {
				if (blocked)
					face(quads, cross, f, OVERLAY_LIFT, 0xFFFFFFFF);
				else if (cage != null)
					face(quads, runes[cage.ordinal()], f, OVERLAY_LIFT, 0xFFFFFFFF);
			}
			for (Direction edge : Direction.values()) {
				if (edge.getAxis() == f.getAxis())
					continue;
				BlockState side = level.getBlockState(pos.relative(edge));
				boolean match = side.getBlock() instanceof BlockRayblendFloor26
						&& side.getValue(BlockRayblendFloor26.REGION) == region;
				if (!match)
					SpecialShieldModel.quad(quads, edges[SpecialShieldModel.edgeIcon(f, edge)], f, EDGE_LIFT,
							0xFFFFFFFF, true, true);
			}
		}
		parts.add(new SimpleModelWrapper(quads.build(), false, base));
	}

	/** One face in RenderBlocks' corner and UV layout, pushed out by {@code lift} (V33a {@code renderFaceYPos(y+0.01)}). */
	private static void face(QuadCollection.Builder quads, Material.Baked material, Direction face, double lift, int color) {
		double lo = -lift, hi = 1 + lift;
		double[][] v = switch (face) {
			case DOWN -> new double[][] {{0, lo, 1, 0, 1}, {0, lo, 0, 0, 0}, {1, lo, 0, 1, 0}, {1, lo, 1, 1, 1}};
			case UP -> new double[][] {{1, hi, 1, 1, 1}, {1, hi, 0, 1, 0}, {0, hi, 0, 0, 0}, {0, hi, 1, 0, 1}};
			case NORTH -> new double[][] {{0, 1, lo, 1, 0}, {1, 1, lo, 0, 0}, {1, 0, lo, 0, 1}, {0, 0, lo, 1, 1}};
			case SOUTH -> new double[][] {{0, 1, hi, 0, 0}, {0, 0, hi, 0, 1}, {1, 0, hi, 1, 1}, {1, 1, hi, 1, 0}};
			case WEST -> new double[][] {{lo, 1, 1, 1, 0}, {lo, 1, 0, 0, 0}, {lo, 0, 0, 0, 1}, {lo, 0, 1, 1, 1}};
			default -> new double[][] {{hi, 0, 1, 1, 1}, {hi, 0, 0, 0, 1}, {hi, 1, 0, 0, 0}, {hi, 1, 1, 1, 0}};
		};
		QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
		vertex.setSprite(material);
		vertex.setTintIndex(-1);
		vertex.setShadeOverride(Direction.UP);
		vertex.setAmbientOcclusion(false);
		vertex.setLightEmission(15);
		vertex.setDirection(face);
		for (double[] p : v)
			vertex.addVertex((float)p[0], (float)p[1], (float)p[2])
					.setColor(color)
					.setUv(material.sprite().getU((float)p[3]), material.sprite().getV((float)p[4]))
					.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
		if (lift == 0)
			quads.addCulledFace(face, vertex.bakeQuad());
		else
			quads.addUnculledFace(vertex.bakeQuad());
	}

	@Override public Material.Baked particleMaterial() { return base; }
	@Override public int materialFlags() { return flags; }

	public record Unbaked(Identifier texture, Identifier cross, List<Identifier> runes, List<Identifier> edges)
			implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
				Identifier.CODEC.fieldOf("cross").forGetter(Unbaked::cross),
				Identifier.CODEC.listOf().fieldOf("runes").forGetter(Unbaked::runes),
				Identifier.CODEC.listOf().fieldOf("edges").forGetter(Unbaked::edges)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new RayblendFloorModel(get(baker, texture), get(baker, cross), all(baker, runes), all(baker, edges));
		}

		private static Material.Baked get(ModelBaker baker, Identifier id) {
			return baker.materials().get(new Material(id), () -> "chromaticraft:rayblend_floor");
		}

		private static Material.Baked[] all(ModelBaker baker, List<Identifier> ids) {
			Material.Baked[] out = new Material.Baked[ids.size()];
			for (int i = 0; i < out.length; i++)
				out[i] = get(baker, ids.get(i));
			return out;
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
