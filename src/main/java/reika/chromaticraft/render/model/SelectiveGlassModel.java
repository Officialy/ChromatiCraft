package reika.chromaticraft.render.model;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.rendering.connected.ConnectedQuads;

import java.util.List;
import java.util.Random;

/**
 * V33a {@code SelectiveGlassRenderer} (translucent pass only; the solid pass drew nothing):
 * <ul>
 * <li>every face draws the {@code glass/glass_k} border sprite for each unconnected numpad edge (the centre, 5,
 * excepted), full-bright, shaded per face (up 1, north/south 0.65, east/west 0.5, down 0.4), with an alpha of
 * {@code 1 - light/32} -- the darker its surroundings, the more solid the pane looks. The light is the brighter
 * of block light and sky light scaled by the sun, read when the chunk is built;</li>
 * <li>over that, each face may carry an overlay picked from a seed of the four-second window, the position and
 * the face: four times in five nothing, three times in twenty the pane's centre sprite, once in twenty a faint
 * engraved rune (red, pink, black, white, lime or light grey, weighted 20/10/5/5/2/2), lit by the world.</li>
 * </ul>
 * Faces between two panes are culled by the block. Everything is prebaked; per chunk only the neighbour and
 * light reads and the overlay roll happen.
 */
public final class SelectiveGlassModel implements DynamicBlockStateModel {

	private static final int LEVELS = 16;
	private static final float[] FACE_SHADE = new float[6];
	private static final CrystalElement[] RUNES = {CrystalElement.RED, CrystalElement.BLACK, CrystalElement.PINK,
			CrystalElement.WHITE, CrystalElement.LIME, CrystalElement.LIGHTGRAY};
	private static final int[] RUNE_WEIGHTS = {20, 5, 10, 5, 2, 2};
	private static final int RUNE_TOTAL = 44;

	static {
		FACE_SHADE[Direction.DOWN.ordinal()] = 0.4F;
		FACE_SHADE[Direction.UP.ordinal()] = 1F;
		FACE_SHADE[Direction.NORTH.ordinal()] = 0.65F;
		FACE_SHADE[Direction.SOUTH.ordinal()] = 0.65F;
		FACE_SHADE[Direction.WEST.ordinal()] = 0.5F;
		FACE_SHADE[Direction.EAST.ordinal()] = 0.5F;
	}

	/** [face][edge 1-9][light level] */
	private final BlockStateModelPart[][][] edges;
	private final BlockStateModelPart[] centres;
	/** [face][rune] */
	private final BlockStateModelPart[][] runes;
	private final Material.Baked particle;

	private SelectiveGlassModel(BlockStateModelPart[][][] edges, BlockStateModelPart[] centres,
			BlockStateModelPart[][] runes, Material.Baked particle) {
		this.edges = edges;
		this.centres = centres;
		this.runes = runes;
		this.particle = particle;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		int light = lightLevel(level, pos);
		long window = System.currentTimeMillis() / 4000 * 4000;
		for (Direction face : Direction.values()) {
			boolean[] un = ConnectedQuads.unconnectedEdges(level, pos, state, face);
			for (int k = 1; k <= 9; k++) {
				if (k != 5 && un[k])
					parts.add(edges[face.ordinal()][k][light]);
			}
			Random rand = new Random(window + pos.hashCode() + (face.ordinal() << 6));
			int r = rand.nextInt(100);
			if (r >= 80 && r < 95)
				parts.add(centres[face.ordinal()]);
			else if (r >= 95)
				parts.add(runes[face.ordinal()][weightedRune(rand)]);
		}
	}

	private static int weightedRune(Random rand) {
		int roll = rand.nextInt(RUNE_TOTAL);
		for (int i = 0; i < RUNE_WEIGHTS.length; i++) {
			roll -= RUNE_WEIGHTS[i];
			if (roll < 0)
				return i;
		}
		return 0;
	}

	/** V33a {@code max(blockLight, skyLight * getSunBrightnessFactor)}, rounded to a whole level. */
	private static int lightLevel(BlockAndTintGetter level, BlockPos pos) {
		float sky = level.getBrightness(LightLayer.SKY, pos);
		Level world = Minecraft.getInstance().level;
		if (world != null)
			sky *= sunBrightnessFactor(world);
		float l = Math.max(level.getBrightness(LightLayer.BLOCK, pos), sky);
		return Mth.clamp(Math.round(l), 0, LEVELS - 1);
	}

	/** 1.7.10 {@code World.getSunBrightnessFactor}. */
	private static float sunBrightnessFactor(Level world) {
		double frac = Mth.frac(world.getOverworldClockTime() / 24000.0 - 0.25);
		double d1 = 0.5 - Math.cos(frac * Math.PI) / 2.0;
		float angle = (float)((frac * 2.0 + d1) / 3.0);
		float f = 1 - (Mth.cos(angle * (float)Math.PI * 2) * 2 + 0.5F);
		f = 1 - Mth.clamp(f, 0, 1);
		f *= 1 - world.getRainLevel(0) * 5 / 16F;
		f *= 1 - world.getThunderLevel(0) * 5 / 16F;
		return f;
	}

	@Override public Material.Baked particleMaterial() { return particle; }
	@Override public int materialFlags() { return BakedQuad.FLAG_TRANSLUCENT; }

	public record Unbaked() implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

		/** V33a's edge-quad corners and UVs per face (u0/u1/v0/v1 as 0/1), as the world pass wrote them. */
		private static final float[][][] EDGE_QUADS = new float[6][][];
		/** The overlay pass used a different UV order on the four side faces. */
		private static final float[][][] OVERLAY_QUADS = new float[6][][];

		static {
			EDGE_QUADS[Direction.UP.ordinal()] = new float[][] {{1,1,0,0,0},{0,1,0,1,0},{0,1,1,1,1},{1,1,1,0,1}};
			EDGE_QUADS[Direction.DOWN.ordinal()] = new float[][] {{0,0,0,1,0},{1,0,0,0,0},{1,0,1,0,1},{0,0,1,1,1}};
			EDGE_QUADS[Direction.EAST.ordinal()] = new float[][] {{1,0,0,1,0},{1,1,0,0,0},{1,1,1,0,1},{1,0,1,1,1}};
			EDGE_QUADS[Direction.WEST.ordinal()] = new float[][] {{0,1,0,0,0},{0,0,0,1,0},{0,0,1,1,1},{0,1,1,0,1}};
			EDGE_QUADS[Direction.SOUTH.ordinal()] = new float[][] {{0,1,1,0,0},{0,0,1,1,0},{1,0,1,1,1},{1,1,1,0,1}};
			EDGE_QUADS[Direction.NORTH.ordinal()] = new float[][] {{0,0,0,1,0},{0,1,0,0,0},{1,1,0,0,1},{1,0,0,1,1}};
			OVERLAY_QUADS[Direction.UP.ordinal()] = EDGE_QUADS[Direction.UP.ordinal()];
			OVERLAY_QUADS[Direction.DOWN.ordinal()] = EDGE_QUADS[Direction.DOWN.ordinal()];
			OVERLAY_QUADS[Direction.EAST.ordinal()] = new float[][] {{1,0,0,1,1},{1,1,0,1,0},{1,1,1,0,0},{1,0,1,0,1}};
			OVERLAY_QUADS[Direction.WEST.ordinal()] = new float[][] {{0,0,1,1,1},{0,1,1,1,0},{0,1,0,0,0},{0,0,0,0,1}};
			OVERLAY_QUADS[Direction.SOUTH.ordinal()] = new float[][] {{1,0,1,1,1},{1,1,1,1,0},{0,1,1,0,0},{0,0,1,0,1}};
			OVERLAY_QUADS[Direction.NORTH.ordinal()] = new float[][] {{1,1,0,0,0},{1,0,0,0,1},{0,0,0,1,1},{0,1,0,1,0}};
		}

		private static Identifier glass(int k) {
			return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/glass/glass_" + k);
		}

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked[] glass = new Material.Baked[10];
			for (int k = 0; k <= 9; k++)
				glass[k] = sprite(baker, glass(k));
			BlockStateModelPart[][][] edges = new BlockStateModelPart[6][10][LEVELS];
			BlockStateModelPart[] centres = new BlockStateModelPart[6];
			BlockStateModelPart[][] runes = new BlockStateModelPart[6][RUNES.length];
			for (Direction face : Direction.values()) {
				int f = face.ordinal();
				for (int k = 1; k <= 9; k++) {
					if (k == 5)
						continue;
					for (int l = 0; l < LEVELS; l++) {
						int alpha = Math.round((1 - l / 32F) * 255);
						int shade = Math.round(FACE_SHADE[f] * 255);
						int colour = (alpha << 24) | (shade << 16) | (shade << 8) | shade;
						edges[f][k][l] = part(baker, face, EDGE_QUADS[f], glass[k], colour, 15);
					}
				}
				centres[f] = part(baker, face, OVERLAY_QUADS[f], glass[5], 0xFFFFFFFF, 0);
				for (int r = 0; r < RUNES.length; r++) {
					Material.Baked rune = sprite(baker, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
							"block/runes/engraved/tile" + RUNES[r].ordinal() + "_0"));
					runes[f][r] = part(baker, face, OVERLAY_QUADS[f], rune, (96 << 24) | 0xFFFFFF, 0);
				}
			}
			return new SelectiveGlassModel(edges, centres, runes, glass[0]);
		}

		private static Material.Baked sprite(ModelBaker baker, Identifier texture) {
			return baker.materials().get(new Material(texture, true), () -> "chromaticraft:selective_glass/" + texture);
		}

		private static BlockStateModelPart part(ModelBaker baker, Direction face, float[][] corners,
				Material.Baked sprite, int colour, int emission) {
			QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
			vertex.setSprite(sprite, Transparency.TRANSLUCENT);
			vertex.setTintIndex(-1);
			vertex.setShadeOverride(Direction.UP);
			vertex.setAmbientOcclusion(false);
			vertex.setLightEmission(emission);
			vertex.setDirection(face);
			for (float[] c : corners) {
				vertex.addVertex(c[0], c[1], c[2]).setColor(colour)
						.setUv(sprite.sprite().getU(c[3]), sprite.sprite().getV(c[4]))
						.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
			}
			QuadCollection.Builder quads = new QuadCollection.Builder();
			quads.addCulledFace(face, vertex.bakeQuad(baker.interner()));
			return new SimpleModelWrapper(quads.build(), false, sprite);
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
