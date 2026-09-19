package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import reika.chromaticraft.magic.progression.ProgressStage;

public final class TieredPlantPodModel implements DynamicBlockStateModel {

	private static final Direction[] FACES = Direction.values();
	private static final int EXPANSION_VARIANTS = 16;

	private final BlockStateModelPart[][] variants;
	private final Material.Baked particle;
	private final ProgressStage stage;

	private TieredPlantPodModel(BlockStateModelPart[][] variants, Material.Baked particle,
			ProgressStage stage) {
		this.variants = variants;
		this.particle = particle;
		this.stage = stage;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> parts) {
		var player = Minecraft.getInstance().player;
		if (player != null && !player.isCreative() && !stage.isPlayerAtStage(player))
			return;
		int mask = 0;
		for (Direction direction : FACES) {
			if (level.getBlockState(pos.relative(direction)).is(BlockTags.LOGS))
				mask |= 1 << direction.ordinal();
		}
		parts.add(variants[mask][random.nextInt(EXPANSION_VARIANTS)]);
	}

	@Override public Material.Baked particleMaterial() { return particle; }

	@Override
	public int materialFlags() {
		int flags = 0;
		for (BlockStateModelPart[] byExpansion : variants)
			for (BlockStateModelPart part : byExpansion)
				flags |= part.materialFlags();
		return flags;
	}

	public record Unbaked(Identifier back, Identifier front, String stage)
			implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.fieldOf("back").forGetter(Unbaked::back),
				Identifier.CODEC.fieldOf("front").forGetter(Unbaked::front),
				com.mojang.serialization.Codec.STRING.fieldOf("stage").forGetter(Unbaked::stage)
		).apply(instance, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked backing = baker.materials().get(new Material(back),
					() -> "chromaticraft:tiered_plant_pod/back/" + back);
			Material.Baked overlay = baker.materials().get(new Material(front),
					() -> "chromaticraft:tiered_plant_pod/front/" + front);
			BlockStateModelPart[][] variants = new BlockStateModelPart[1 << FACES.length][EXPANSION_VARIANTS];
			for (int mask = 0; mask < variants.length; mask++) {
				Box body = Box.forMask(mask);
				for (int expansion = 0; expansion < EXPANSION_VARIANTS; expansion++) {
					// Midpoint sampling avoids overweighting either endpoint of V33a's continuous range.
					float amount = (expansion + 0.5F) / EXPANSION_VARIANTS / 16F;
					QuadCollection.Builder quads = new QuadCollection.Builder();
					for (Direction face : FACES) {
						var quad = bakeFace(baker, face, body, backing, false);
						// RenderBlocks only asks the neighbour to cull a face which reaches the block
						// boundary; inset pod faces remain visible even beside an unrelated solid block.
						if (body.touches(face)) quads.addCulledFace(face, quad);
						else quads.addUnculledFace(quad);
					}
					Box glow = body.inflate(amount);
					for (Direction face : FACES)
						quads.addUnculledFace(bakeFace(baker, face, glow, overlay, true));
					variants[mask][expansion] = new SimpleModelWrapper(quads.build(), false, overlay);
				}
			}
			return new TieredPlantPodModel(variants, overlay, ProgressStage.valueOf(stage));
		}

		private static net.minecraft.client.resources.model.geometry.BakedQuad bakeFace(ModelBaker baker,
				Direction face, Box box, Material.Baked material, boolean emissive) {
			QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
			vertex.setSprite(material);
			vertex.setTintIndex(-1);
			vertex.setShade(!emissive);
			vertex.setAmbientOcclusion(!emissive);
			vertex.setLightEmission(emissive ? 15 : 0);
			vertex.setDirection(face);
			for (Point point : vertices(face, box)) {
				// 26.2 sprite coordinates are normalized, unlike V33a's 0..16 IIcon offsets.
				// The glow can extend beyond a block edge beside a log; keep those UVs in the
				// sprite as well, without clipping the expanded geometry.
				float u = switch (face.getAxis()) {
					case X -> point.z;
					case Y, Z -> point.x;
				};
				float v = face.getAxis() == Direction.Axis.Y
						? point.z : 1 - point.y;
				vertex.addVertex(point.x, point.y, point.z).setColor(0xFFFFFFFF)
						.setUv(material.sprite().getU(Math.clamp(u, 0F, 1F)),
								material.sprite().getV(Math.clamp(v, 0F, 1F)))
						.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
			}
			return vertex.bakeQuad(baker.interner());
		}

		private static Point[] vertices(Direction face, Box b) {
			return switch (face) {
				case DOWN -> new Point[] {p(b.minX, b.minY, b.maxZ), p(b.minX, b.minY, b.minZ),
						p(b.maxX, b.minY, b.minZ), p(b.maxX, b.minY, b.maxZ)};
				case UP -> new Point[] {p(b.minX, b.maxY, b.minZ), p(b.minX, b.maxY, b.maxZ),
						p(b.maxX, b.maxY, b.maxZ), p(b.maxX, b.maxY, b.minZ)};
				case NORTH -> new Point[] {p(b.maxX, b.maxY, b.minZ), p(b.maxX, b.minY, b.minZ),
						p(b.minX, b.minY, b.minZ), p(b.minX, b.maxY, b.minZ)};
				case SOUTH -> new Point[] {p(b.minX, b.maxY, b.maxZ), p(b.minX, b.minY, b.maxZ),
						p(b.maxX, b.minY, b.maxZ), p(b.maxX, b.maxY, b.maxZ)};
				case WEST -> new Point[] {p(b.minX, b.maxY, b.minZ), p(b.minX, b.minY, b.minZ),
						p(b.minX, b.minY, b.maxZ), p(b.minX, b.maxY, b.maxZ)};
				case EAST -> new Point[] {p(b.maxX, b.maxY, b.maxZ), p(b.maxX, b.minY, b.maxZ),
						p(b.maxX, b.minY, b.minZ), p(b.maxX, b.maxY, b.minZ)};
			};
		}

		private static Point p(float x, float y, float z) { return new Point(x, y, z); }

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}

	private record Point(float x, float y, float z) {}

	private record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		private static Box forMask(int mask) {
			return new Box(has(mask, Direction.WEST) ? 0 : 0.25F,
					has(mask, Direction.DOWN) ? 0 : 0.25F,
					has(mask, Direction.NORTH) ? 0 : 0.25F,
					has(mask, Direction.EAST) ? 1 : 0.75F,
					has(mask, Direction.UP) ? 1 : 0.75F,
					has(mask, Direction.SOUTH) ? 1 : 0.75F);
		}

		private Box inflate(float amount) {
			return new Box(minX - amount, minY - amount, minZ - amount,
					maxX + amount, maxY + amount, maxZ + amount);
		}

		private boolean touches(Direction direction) {
			return switch (direction) {
				case WEST -> minX == 0;
				case EAST -> maxX == 1;
				case DOWN -> minY == 0;
				case UP -> maxY == 1;
				case NORTH -> minZ == 0;
				case SOUTH -> maxZ == 1;
			};
		}

		private static boolean has(int mask, Direction direction) {
			return (mask & 1 << direction.ordinal()) != 0;
		}
	}
}
