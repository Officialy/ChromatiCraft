package reika.chromaticraft.render.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.platform.Transparency;
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

import reika.chromaticraft.block.dimension.structure.laser.BlockLaserEffector26;
import reika.chromaticraft.render.model.LaserEffectorGeometry.Quad;
import reika.chromaticraft.render.model.LaserEffectorGeometry.Sprite;
import reika.chromaticraft.render.model.LaserEffectorGeometry.Vertex;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;

/**
 * Chunk-mesh port of V33a's {@code LaserEffectorRenderer} ISBRH. The geometry is
 * {@link LaserEffectorGeometry}; this model only binds its named sprites and bakes one part per
 * TYPE x DIRECTION x COLOR x FULL_BLOCK x ROTATEABLE combination, on first use. LIT changes the
 * block's light level, not its mesh, so it is not part of the key.
 */
public final class LaserEffectorModel implements DynamicBlockStateModel {

	private final Map<Sprite, Material.Baked> sprites;
	private final Map<Integer, BlockStateModelPart> parts = new ConcurrentHashMap<>();
	private final int flags;

	private LaserEffectorModel(Map<Sprite, Material.Baked> sprites) {
		this.sprites = sprites;
		int f = 0;
		// Every sprite and transparency mode appears in the emitter's full block plus each type's
		// rotateable half block, so the union of those parts' flags covers every state.
		for (EffectType type : EffectType.values())
			f |= part(type.ordinal(), 0, 7, false, true).materialFlags();
		f |= part(EffectType.EMITTER.ordinal(), 0, 7, true, false).materialFlags();
		f |= part(EffectType.TARGET.ordinal(), 0, 7, true, false).materialFlags();
		flags = f;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> out) {
		if (!state.hasProperty(BlockLaserEffector26.TYPE))
			return;
		out.add(part(state.getValue(BlockLaserEffector26.TYPE),
				state.getValue(BlockLaserEffector26.DIRECTION), state.getValue(BlockLaserEffector26.COLOR),
				state.getValue(BlockLaserEffector26.FULL_BLOCK), state.getValue(BlockLaserEffector26.ROTATEABLE)));
	}

	private BlockStateModelPart part(int type, int direction, int color, boolean full, boolean rotateable) {
		int key = type | direction << 4 | color << 7 | (full ? 1 << 10 : 0) | (rotateable ? 1 << 11 : 0);
		return parts.computeIfAbsent(key, k -> bake(LaserEffectorGeometry.build(
				EffectType.byLegacyMetadata(type), CubeDirections.list[direction], color, full, rotateable)));
	}

	private BlockStateModelPart bake(List<Quad> quads) {
		QuadCollection.Builder builder = new QuadCollection.Builder();
		for (Quad quad : quads) {
			Material.Baked material = sprites.get(quad.sprite());
			List<Vertex> vertices = quad.vertices();
			float[] normal = newellNormal(vertices);
			Direction face = Direction.getApproximateNearest(normal[0], normal[1], normal[2]);
			QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
			if (quad.overlay())
				vertex.setSprite(material, Transparency.TRANSLUCENT);
			else
				vertex.setSprite(material);
			vertex.setTintIndex(-1);
			// V33a set every face's brightness explicitly in the vertex colour, and drew the raw
			// tessellator quads without ambient occlusion; UP is the unshaded factor.
			vertex.setShadeOverride(Direction.UP);
			vertex.setAmbientOcclusion(false);
			vertex.setDirection(face);
			for (Vertex v : vertices)
				vertex.addVertex((float)v.x(), (float)v.y(), (float)v.z())
						.setColor(v.argb())
						.setUv(material.sprite().getU(v.u()), material.sprite().getV(v.v()))
						.setNormal(normal[0], normal[1], normal[2]);
			if (quad.boundary())
				builder.addCulledFace(face, vertex.bakeQuad());
			else
				builder.addUnculledFace(vertex.bakeQuad());
		}
		return new SimpleModelWrapper(builder.build(), false, sprites.get(Sprite.BASE));
	}

	/** Newell's method: robust for the source's folded and degenerate (triangle) quads. */
	private static float[] newellNormal(List<Vertex> vertices) {
		double nx = 0, ny = 0, nz = 0;
		for (int i = 0; i < vertices.size(); i++) {
			Vertex a = vertices.get(i), b = vertices.get((i + 1) % vertices.size());
			nx += (a.y() - b.y()) * (a.z() + b.z());
			ny += (a.z() - b.z()) * (a.x() + b.x());
			nz += (a.x() - b.x()) * (a.y() + b.y());
		}
		double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len < 1.0E-9)
			return new float[] {0, 1, 0};
		return new float[] {(float)(nx / len), (float)(ny / len), (float)(nz / len)};
	}

	@Override public Material.Baked particleMaterial() { return sprites.get(Sprite.BASE); }
	@Override public int materialFlags() { return flags; }

	public record Unbaked(Identifier slabTop, Identifier slabSide, Identifier obsidian, Identifier crystal,
			Identifier planks, Identifier chiseled, Identifier base, Identifier emitterBack,
			Identifier emitterFront, Identifier targetBack, Identifier targetFront)
			implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("slab_top").forGetter(Unbaked::slabTop),
				Identifier.CODEC.fieldOf("slab_side").forGetter(Unbaked::slabSide),
				Identifier.CODEC.fieldOf("obsidian").forGetter(Unbaked::obsidian),
				Identifier.CODEC.fieldOf("crystal").forGetter(Unbaked::crystal),
				Identifier.CODEC.fieldOf("planks").forGetter(Unbaked::planks),
				Identifier.CODEC.fieldOf("chiseled").forGetter(Unbaked::chiseled),
				Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
				Identifier.CODEC.fieldOf("emitter_back").forGetter(Unbaked::emitterBack),
				Identifier.CODEC.fieldOf("emitter_front").forGetter(Unbaked::emitterFront),
				Identifier.CODEC.fieldOf("target_back").forGetter(Unbaked::targetBack),
				Identifier.CODEC.fieldOf("target_front").forGetter(Unbaked::targetFront)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Map<Sprite, Identifier> ids = new EnumMap<>(Sprite.class);
			ids.put(Sprite.SLAB_TOP, slabTop);
			ids.put(Sprite.SLAB_SIDE, slabSide);
			ids.put(Sprite.OBSIDIAN, obsidian);
			ids.put(Sprite.CRYSTAL, crystal);
			ids.put(Sprite.PLANKS, planks);
			ids.put(Sprite.CHISELED, chiseled);
			ids.put(Sprite.BASE, base);
			ids.put(Sprite.EMITTER_BACK, emitterBack);
			ids.put(Sprite.EMITTER_FRONT, emitterFront);
			ids.put(Sprite.TARGET_BACK, targetBack);
			ids.put(Sprite.TARGET_FRONT, targetFront);
			Map<Sprite, Material.Baked> baked = new EnumMap<>(Sprite.class);
			for (Map.Entry<Sprite, Identifier> entry : ids.entrySet())
				baked.put(entry.getKey(), baker.materials().get(new Material(entry.getValue()),
						() -> "chromaticraft:laser_effector/" + entry.getKey()));
			return new LaserEffectorModel(baked);
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
