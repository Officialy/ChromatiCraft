package reika.chromaticraft.render.model;

import java.util.ArrayList;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import reika.chromaticraft.block.BlockEncrustedCrystal;
import reika.chromaticraft.block.BlockEncrustedCrystal.CrystalGrowth;
import reika.dragonapi.instantiable.GridDistortion;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code CrystalEncrustingRenderer}. Each of the six faces can carry an independently growing
 * crust, drawn as a scatter of small tapered boxes standing off that face.
 *
 * <p>The source is a position-seeded static block renderer, so this is a chunk-mesh
 * {@link DynamicBlockStateModel} rather than a block entity renderer. The seed is V33a's
 * {@code ISBRH.calcSeed(x, y, z) = chunkXZ2Int(x, z) ^ y} followed by the same two discarded
 * {@code nextBoolean()} calls, so a given block grows the same crust shape it did in 1.7.10.
 *
 * <p>Per face the source picks {@code n} in 4..8 grid cells, distorts that grid, then places
 * {@code min(n*n/2, 6 + amt*amt/10)} pieces (×1.5 when the crystal is special) of height
 * {@code (3+2*amt .. 8+4*amt)/96}. Each piece is tinted by mixing the element colour toward white
 * and black by two random factors and shifting the hue by ±5, drawn full-bright, and then overdrawn
 * with the glow frame — and, when special, the special sprite at low alpha.
 *
 * <p>Growth is block-entity state, so the parts cannot be pre-baked per variant the way the cave
 * crystal's sixteen arm masks are; the quads are built during {@code collectParts} from the live
 * growth data. That is the same work the source did per frame in {@code renderWorldBlock}, and it
 * only runs when a chunk section re-meshes.
 */
public final class EncrustedCrystalModel implements DynamicBlockStateModel {

	private static final int MIN_SEGMENTS = 4;
	private static final int MAX_SEGMENTS = 8;
	/** V33a alpha: a special crystal's body is translucent, an ordinary one solid. */
	private static final int SPECIAL_BODY_ALPHA = 160;
	private static final int SPECIAL_FRAME_ALPHA = 240;
	private static final int ORDINARY_FRAME_ALPHA = 192;

	private final Material.Baked crystal;
	private final Material.Baked glowFrame;
	private final Material.Baked special;

	private EncrustedCrystalModel(Material.Baked crystal, Material.Baked glowFrame, Material.Baked special) {
		this.crystal = crystal;
		this.glowFrame = glowFrame;
		this.special = special;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> parts) {
		if (!(level.getBlockEntity(pos) instanceof BlockEncrustedCrystal.TileCrystalEncrusted tile))
			return;
		var growths = tile.getGrowths();
		if (growths.isEmpty())
			return;

		// V33a ISBRH.calcSeed plus its two discarded rolls.
		Random rand = new Random(((long)pos.getX() & 0xFFFFFFFFL)
				| (((long)pos.getZ() & 0xFFFFFFFFL) << 32) ^ pos.getY());
		rand.nextBoolean();
		rand.nextBoolean();

		QuadCollection.Builder quads = new QuadCollection.Builder();
		boolean drewAnything = false;
		for (CrystalGrowth growth : growths)
			drewAnything |= this.buildFace(quads, growth, tile.isSpecial(), rand);
		if (!drewAnything)
			return;
		parts.add(new SimpleModelWrapper(quads.build(), false, crystal));
	}

	/** V33a renderCrystalFace: the per-face grid, piece count and height band. */
	private boolean buildFace(QuadCollection.Builder quads, CrystalGrowth growth, boolean isSpecial, Random rand) {
		int amt = growth.getGrowth();
		int h1 = 3 + amt * 2;
		int h2 = 8 + amt * 4;
		int n = MIN_SEGMENTS + rand.nextInt(MAX_SEGMENTS - MIN_SEGMENTS + 1);
		double w = 1D / n;
		GridDistortion grid = new GridDistortion(n);
		grid.maxDeviation *= 0.66;
		grid.snapToEdges = false;
		grid.randomize(rand);
		int pieces = Math.min(n * n / 2, 6 + amt * amt / 10);
		if (isSpecial)
			pieces *= 1.5;
		boolean[][] rendered = new boolean[n][n];
		boolean drew = false;
		for (int i = 0; i < pieces; i++) {
			int a = rand.nextInt(n);
			int b = rand.nextInt(n);
			if (rendered[a][b])
				continue;
			rendered[a][b] = true;
			int rh = h1 + rand.nextInt(h2 - h1 + 1);
			this.buildPiece(quads, growth, isSpecial, rand, a, b, w, rh / 96D, grid);
			drew = true;
		}
		return drew;
	}

	/** V33a renderCrystalPiece: the per-piece tint, body, glow frame and special overlay. */
	private void buildPiece(QuadCollection.Builder quads, CrystalGrowth growth, boolean isSpecial,
			Random rand, int a, int b, double w, double h, GridDistortion grid) {
		float f1 = 0.75F + rand.nextFloat() * 0.25F;
		float f2 = 0.75F + rand.nextFloat() * 0.25F;
		int color = ReikaColorAPI.mixColors(growth.color.getColor(), 0xffffff, f1);
		color = ReikaColorAPI.mixColors(color, 0x000000, f2);
		int hue = ReikaColorAPI.getHue(color) - 5 + rand.nextInt(11);
		color = ReikaColorAPI.getModifiedHue(color, hue);

		GridDistortion.OffsetGroup offset = grid.getOffset(a, b);
		// V33a distorts both ends of the peg: the growth face and the face opposite it.
		CrystalPiece piece = CrystalPiece.forSide(growth.side, a, b, w, h);
		piece.applyOffset(growth.side, offset);
		piece.applyOffset(growth.side.getOpposite(), offset);
		piece.clamp();
		int bodyAlpha = isSpecial ? SPECIAL_BODY_ALPHA : 255;
		CrystalQuads.addPiece(quads, piece, crystal, color, bodyAlpha);
		CrystalQuads.addPiece(quads, piece, glowFrame,
				ReikaColorAPI.mixColors(color, 0xffffff, 0.9F),
				isSpecial ? SPECIAL_FRAME_ALPHA : ORDINARY_FRAME_ALPHA);
		if (isSpecial)
			CrystalQuads.addPiece(quads, piece, special, 0xffffff, 48);
	}

	@Override public Material.Baked particleMaterial() { return crystal; }
	@Override public int materialFlags() { return BakedQuad.FLAG_TRANSLUCENT; }

	public record Unbaked(Identifier texture, Identifier glowFrame, Identifier specialTexture)
			implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
				Identifier.CODEC.fieldOf("glow_frame").forGetter(Unbaked::glowFrame),
				Identifier.CODEC.fieldOf("special_texture").forGetter(Unbaked::specialTexture)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked body = baker.materials().get(new Material(texture, true),
					() -> "chromaticraft:encrusted_crystal/" + texture);
			Material.Baked frame = baker.materials().get(new Material(glowFrame, true),
					() -> "chromaticraft:encrusted_crystal/" + glowFrame);
			Material.Baked spec = baker.materials().get(new Material(specialTexture, true),
					() -> "chromaticraft:encrusted_crystal/" + specialTexture);
			return new EncrustedCrystalModel(body, frame, spec);
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
