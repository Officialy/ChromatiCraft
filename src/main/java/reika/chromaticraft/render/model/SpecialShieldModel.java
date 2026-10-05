package reika.chromaticraft.render.model;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
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

import reika.chromaticraft.block.dimension.structure.BlockSpecialShield26;

/**
 * Chunk-mesh port of V33a's {@code SpecialShieldRenderer} with DragonAPI's {@code EdgeDetectionRenderer}.
 *
 * <p>Each face that is not against the identical shield draws the material's icon with V33a's 0.5 / 1 / 0.8 /
 * 0.65 face shading, unlit for Cloak, Stone and Cracks. Cobble adds one of its two overlays chosen by
 * {@code (x+z) % 2}, unlit. Cloak and Stone add the edge glints: on each visible face, every in-plane edge whose
 * neighbour is not a special shield (any material: the upstream key carried no metadata), or whose neighbour's
 * same face is hidden by the shield beyond it, draws the matching {@code basic/sideN} icon 0.005 outside the face.
 * That occlusion probe reproduces V33a exactly, including its quirk of always passing side 0, so the neighbour
 * compares the probed block with the one above it.
 */
public final class SpecialShieldModel implements DynamicBlockStateModel {

	private static final double O = 0.005;

	private final Material.Baked base;
	private final Optional<Material.Baked[]> overlays;
	private final Optional<Material.Baked[]> edges;
	private final boolean unlit;

	private final int flags;

	private SpecialShieldModel(Material.Baked base, Optional<Material.Baked[]> overlays,
			Optional<Material.Baked[]> edges, boolean unlit) {
		this.base = base;
		this.overlays = overlays;
		this.edges = edges;
		this.unlit = unlit;
		QuadCollection.Builder sample = new QuadCollection.Builder();
		quad(sample, base, Direction.UP, 0, 0xFFFFFFFF, unlit, false);
		overlays.ifPresent(o -> { for (Material.Baked m : o) quad(sample, m, Direction.UP, 0, 0xFFFFFFFF, true, false); });
		edges.ifPresent(o -> { for (Material.Baked m : o) quad(sample, m, Direction.UP, O, 0xFFFFFFFF, true, true); });
		flags = new SimpleModelWrapper(sample.build(), false, base).materialFlags();
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		QuadCollection.Builder quads = new QuadCollection.Builder();
		for (Direction face : Direction.values()) {
			if (level.getBlockState(pos.relative(face)) == state)
				continue;
			float shade = switch (face) {
				case DOWN -> 0.5F;
				case UP -> 1F;
				case NORTH, SOUTH -> 0.8F;
				default -> 0.65F;
			};
			quad(quads, base, face, 0, grey(shade), unlit, false);
			overlays.ifPresent(o -> quad(quads, o[Math.floorMod(pos.getX() + pos.getZ(), o.length)], face, 0,
					0xFFFFFFFF, true, false));
			edges.ifPresent(icons -> {
				for (Direction edge : Direction.values()) {
					if (edge.getAxis() == face.getAxis())
						continue;
					BlockPos side = pos.relative(edge);
					BlockPos probe = side.relative(face);
					boolean neighbourMatches = BlockSpecialShield26.isSpecialShield(level, side);
					boolean neighbourFaceHidden = BlockSpecialShield26.isSpecialShield(level, probe)
							&& level.getBlockState(probe) == level.getBlockState(probe.above());
					if (!neighbourMatches || neighbourFaceHidden)
						quad(quads, icons[edgeIcon(face, edge)], face, O, 0xFFFFFFFF, true, true);
				}
			});
		}
		parts.add(new SimpleModelWrapper(quads.build(), false, base));
	}

	/** DragonAPI {@code EdgeDetectionRenderer}'s icon per face and edge direction. */
	private static int edgeIcon(Direction face, Direction edge) {
		Direction.Axis third = face.getAxis() == Direction.Axis.Y ? Direction.Axis.X : Direction.Axis.Y;
		if (edge.getAxis() == third)
			return edge.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 2 : 0;
		return edge.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 3 : 1;
	}

	private static int grey(float f) {
		int g = (int)(f * 255);
		return 0xFF000000 | g << 16 | g << 8 | g;
	}

	/**
	 * One face, pushed out by {@code lift}: in RenderBlocks' corner and UV layout for the icon and overlays, or in
	 * {@code EdgeDetectionRenderer}'s own layout for the glints.
	 */
	private static void quad(QuadCollection.Builder quads, Material.Baked material, Direction face, double lift,
			int color, boolean unlit, boolean edgeLayout) {
		double lo = -lift, hi = 1 + lift;
		double[][] v = !edgeLayout ? switch (face) {
			case DOWN -> new double[][] {{0, 0, 1, 0, 1}, {0, 0, 0, 0, 0}, {1, 0, 0, 1, 0}, {1, 0, 1, 1, 1}};
			case UP -> new double[][] {{1, 1, 1, 1, 1}, {1, 1, 0, 1, 0}, {0, 1, 0, 0, 0}, {0, 1, 1, 0, 1}};
			case NORTH -> new double[][] {{0, 1, 0, 1, 0}, {1, 1, 0, 0, 0}, {1, 0, 0, 0, 1}, {0, 0, 0, 1, 1}};
			case SOUTH -> new double[][] {{0, 1, 1, 0, 0}, {0, 0, 1, 0, 1}, {1, 0, 1, 1, 1}, {1, 1, 1, 1, 0}};
			case WEST -> new double[][] {{0, 1, 1, 1, 0}, {0, 1, 0, 0, 0}, {0, 0, 0, 0, 1}, {0, 0, 1, 1, 1}};
			default -> new double[][] {{1, 0, 1, 1, 1}, {1, 0, 0, 0, 1}, {1, 1, 0, 0, 0}, {1, 1, 1, 1, 0}};
		} : switch (face) {
			case DOWN -> new double[][] {{0, lo, 0, 0, 0}, {1, lo, 0, 1, 0}, {1, lo, 1, 1, 1}, {0, lo, 1, 0, 1}};
			case UP -> new double[][] {{0, hi, 1, 0, 1}, {1, hi, 1, 1, 1}, {1, hi, 0, 1, 0}, {0, hi, 0, 0, 0}};
			case EAST -> new double[][] {{hi, 0, 0, 0, 0}, {hi, 1, 0, 1, 0}, {hi, 1, 1, 1, 1}, {hi, 0, 1, 0, 1}};
			case WEST -> new double[][] {{lo, 0, 1, 0, 1}, {lo, 1, 1, 1, 1}, {lo, 1, 0, 1, 0}, {lo, 0, 0, 0, 0}};
			case NORTH -> new double[][] {{0, 0, lo, 0, 0}, {0, 1, lo, 1, 0}, {1, 1, lo, 1, 1}, {1, 0, lo, 0, 1}};
			default -> new double[][] {{1, 0, hi, 0, 1}, {1, 1, hi, 1, 1}, {0, 1, hi, 1, 0}, {0, 0, hi, 0, 0}};
		};
		QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
		vertex.setSprite(material);
		vertex.setTintIndex(-1);
		vertex.setShadeOverride(Direction.UP);
		vertex.setAmbientOcclusion(false);
		vertex.setLightEmission(unlit ? 15 : 0);
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

	public record Unbaked(Identifier texture, List<Identifier> overlays, List<Identifier> edges, boolean unlit)
			implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
				Identifier.CODEC.listOf().optionalFieldOf("overlays", List.of()).forGetter(Unbaked::overlays),
				Identifier.CODEC.listOf().optionalFieldOf("edges", List.of()).forGetter(Unbaked::edges),
				Codec.BOOL.optionalFieldOf("unlit", false).forGetter(Unbaked::unlit)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked base = baker.materials().get(new Material(texture), () -> "chromaticraft:special_shield");
			return new SpecialShieldModel(base, bakeAll(baker, overlays), bakeAll(baker, edges), unlit);
		}

		private static Optional<Material.Baked[]> bakeAll(ModelBaker baker, List<Identifier> ids) {
			if (ids.isEmpty())
				return Optional.empty();
			Material.Baked[] out = new Material.Baked[ids.size()];
			for (int i = 0; i < out.length; i++)
				out[i] = baker.materials().get(new Material(ids.get(i)), () -> "chromaticraft:special_shield");
			return Optional.of(out);
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
