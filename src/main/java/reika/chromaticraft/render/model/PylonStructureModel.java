package reika.chromaticraft.render.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockPylonStructure.StoneTypes;
import reika.dragonapi.instantiable.rendering.connected.ConnectedQuads;

/**
 * Crystalline-stone types whose artwork V33a picks from neighbouring blocks rather than from any
 * placement state, reproducing {@code BlockPylonStructure.getIconIndex}.
 *
 * <p>V33a's {@code BlockPylonStructure} has no axis or orientation state at all — the port had
 * invented an {@code AXIS} property, which made appearance depend on how a block was placed instead
 * of what it connects to. Three types use the neighbour scan, each with its own rule, and every rule
 * is evaluated <em>per face</em>:
 *
 * <ul>
 *   <li><b>beams</b> — top and bottom only: a same-type neighbour on X gives index 2, on Z index 1,
 *       otherwise 0. Side faces always take index 0.</li>
 *   <li><b>resonance rings</b> — top and bottom prefer the X/Z pair, then any vertical neighbour
 *       forces index 1, and finally an isolated ring (no same-type neighbour) that still touches a
 *       horizontal neighbour takes index 1 on side faces, or on top/bottom only when that neighbour
 *       lies on X.</li>
 *   <li><b>corners</b> — a four-way rotation chosen per face from which pair of neighbours is
 *       present, matching V33a's per-side table. Corners match on the block alone, not the type.</li>
 * </ul>
 */
public final class PylonStructureModel implements DynamicBlockStateModel {

	private static final Direction[] FACES = Direction.values();

	/** [face][index] single-face parts; the index range is the type's V33a variant count. */
	private final BlockStateModelPart[][] parts;
	private final StoneTypes type;
	private final Material.Baked particle;
	private final int flags;

	private PylonStructureModel(BlockStateModelPart[][] parts, StoneTypes type, Material.Baked particle) {
		this.parts = parts;
		this.type = type;
		this.particle = particle;
		int materialFlags = 0;
		for (BlockStateModelPart[] perFace : parts)
			for (BlockStateModelPart part : perFace)
				if (part != null) materialFlags |= part.materialFlags();
		this.flags = materialFlags;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> out) {
		for (Direction face : FACES) {
			int index = this.iconIndex(level, pos, state, face);
			BlockStateModelPart[] perFace = parts[face.ordinal()];
			out.add(perFace[Math.min(Math.max(index, 0), perFace.length - 1)]);
		}
	}

	/** V33a {@code getIconIndex}; -1 means "no override", which the caller clamps to 0. */
	private int iconIndex(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
		boolean vertical = face.getAxis() == Direction.Axis.Y;
		if (type == StoneTypes.RESORING) {
			if (vertical) {
				if (sameType(level, pos.east(), state) || sameType(level, pos.west(), state)) return 0;
				if (sameType(level, pos.south(), state) || sameType(level, pos.north(), state)) return 1;
			}
			if (sameType(level, pos.above(), state) || sameType(level, pos.below(), state)) return 1;
			// V33a's flag/flag2/flag3 walk: isolated from same-type rings, but touching some
			// crystalline stone horizontally (flag3 narrows that to the X pair).
			boolean isolated = true;
			boolean horizontal = false;
			boolean onXAxis = false;
			for (Direction dir : FACES) {
				if (!sameBlock(level, pos.relative(dir), state)) continue;
				if (sameType(level, pos.relative(dir), state)) isolated = false;
				if (dir.getAxis() != Direction.Axis.Y) {
					horizontal = true;
					if (dir.getAxis() == Direction.Axis.X) onXAxis = true;
				}
			}
			if (isolated && horizontal && (!vertical || onXAxis)) return 1;
			return -1;
		}
		if (type.isBeam() && vertical) {
			// V33a compares the wrapped metadata, so a glow beam joins an ordinary beam.
			if (sameBeam(level, pos.east(), state) || sameBeam(level, pos.west(), state)) return 2;
			if (sameBeam(level, pos.south(), state) || sameBeam(level, pos.north(), state)) return 1;
			return -1;
		}
		if (type == StoneTypes.CORNER) {
			boolean east = sameBlock(level, pos.east(), state);
			boolean west = sameBlock(level, pos.west(), state);
			boolean south = sameBlock(level, pos.south(), state);
			boolean north = sameBlock(level, pos.north(), state);
			boolean up = sameBlock(level, pos.above(), state);
			boolean down = sameBlock(level, pos.below(), state);
			return switch (face) {
				case DOWN, UP -> east && south ? 0 : west && south ? 1 : east && north ? 3 : west && north ? 2 : -1;
				case NORTH -> down && east ? 1 : down && west ? 0 : up && east ? 2 : up && west ? 3 : -1;
				case SOUTH -> down && east ? 0 : down && west ? 1 : up && east ? 3 : up && west ? 2 : -1;
				case WEST -> down && south ? 0 : down && north ? 1 : up && south ? 3 : up && north ? 2 : -1;
				case EAST -> down && south ? 1 : down && north ? 0 : up && south ? 2 : up && north ? 3 : -1;
			};
		}
		return -1;
	}

	private static boolean sameBlock(BlockAndTintGetter level, BlockPos pos, BlockState self) {
		return level.getBlockState(pos).getBlock() == self.getBlock();
	}

	private boolean sameType(BlockAndTintGetter level, BlockPos pos, BlockState self) {
		BlockState other = level.getBlockState(pos);
		return other.getBlock() == self.getBlock()
				&& other.getValue(BlockPylonStructure.TYPE).intValue() == self.getValue(BlockPylonStructure.TYPE).intValue();
	}

	/** V33a {@code getWrappedMeta}: on top/bottom faces every beam type reads as BEAM. */
	private boolean sameBeam(BlockAndTintGetter level, BlockPos pos, BlockState self) {
		BlockState other = level.getBlockState(pos);
		if (other.getBlock() != self.getBlock()) return false;
		return StoneTypes.list[other.getValue(BlockPylonStructure.TYPE)].isBeam();
	}

	@Override public Material.Baked particleMaterial() { return particle; }
	@Override public int materialFlags() { return flags; }

	/**
	 * @param stoneType which V33a neighbour rule to apply
	 * @param top       textures for the up/down faces, indexed by icon index
	 * @param side      textures for the four horizontal faces, indexed by icon index
	 * @param topGlow   optional full-bright overlay for up/down, same indexing
	 * @param sideGlow  optional full-bright overlay for the sides, same indexing
	 */
	public record Unbaked(int stoneType, List<Identifier> top, List<Identifier> side,
			List<Identifier> topGlow, List<Identifier> sideGlow) implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(0, 15).fieldOf("stone_type").forGetter(Unbaked::stoneType),
				Identifier.CODEC.listOf().fieldOf("top").forGetter(Unbaked::top),
				Identifier.CODEC.listOf().fieldOf("side").forGetter(Unbaked::side),
				Identifier.CODEC.listOf().optionalFieldOf("top_glow", List.of()).forGetter(Unbaked::topGlow),
				Identifier.CODEC.listOf().optionalFieldOf("side_glow", List.of()).forGetter(Unbaked::sideGlow)
		).apply(instance, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Map<Direction, BlockStateModelPart[]> byFace = new EnumMap<>(Direction.class);
			Material.Baked particle = null;
			for (Direction face : FACES) {
				boolean vertical = face.getAxis() == Direction.Axis.Y;
				List<Identifier> base = vertical ? top : side;
				List<Identifier> glow = vertical ? topGlow : sideGlow;
				BlockStateModelPart[] variants = new BlockStateModelPart[base.size()];
				for (int i = 0; i < base.size(); i++) {
					Material.Baked baseMat = baker.materials().get(new Material(base.get(i)),
							() -> "chromaticraft:pylon_structure/" + base.get(0));
					if (particle == null) particle = baseMat;
					QuadCollection.Builder quads = new QuadCollection.Builder();
					quads.addCulledFace(face, ConnectedQuads.bakeFaceQuad(baker, face, baseMat, 0));
					if (i < glow.size()) {
						// V33a's second render pass: the same face, fractionally proud of the block
						// so it does not z-fight, drawn full-bright. Inflation is why the emissive
						// faces need explicit UVs -- see the layeredCube note in ChromaModelProvider.
						Material.Baked glowMat = baker.materials().get(new Material(glow.get(i)),
								() -> "chromaticraft:pylon_structure_glow/" + glow.get(0));
						quads.addUnculledFace(ConnectedQuads.bakeFaceQuad(baker, face, glowMat, 0.002F));
					}
					variants[i] = new SimpleModelWrapper(quads.build(), true, baseMat);
				}
				byFace.put(face, variants);
			}
			BlockStateModelPart[][] parts = new BlockStateModelPart[FACES.length][];
			for (Direction face : FACES) parts[face.ordinal()] = byFace.get(face);
			return new PylonStructureModel(parts, StoneTypes.list[stoneType], particle);
		}

		/** Textures are resolved through the material baker, so there are no model dependencies. */
		@Override public void resolveDependencies(Resolver resolver) {}

		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
