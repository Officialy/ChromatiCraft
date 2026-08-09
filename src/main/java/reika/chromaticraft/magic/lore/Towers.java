package reika.chromaticraft.magic.lore;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.dragonapi.DragonOptions;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.Hex;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.MapShape;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.Point;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code Towers}: the thirteen lore towers, laid out on a hex grid rather than scattered.
 *
 * <p>Each tower owns a fixed cube coordinate on a nine-ring hex flower. At world load the grid is
 * built at a radius scaled from {@code DragonOptions.WORLDSIZE}, rotated by a seeded random angle and
 * translated by a seeded offset, then every tower's location is snapped to a 16-block boundary. The
 * result is a ring of towers whose relative geometry is identical in every world but whose absolute
 * position and orientation are not.
 *
 * <p>The seed mixes the world seed with the world's creation time, so two worlds made from the same
 * seed still differ — that is deliberate upstream and is preserved.
 */
public enum Towers {

	ALPHA("α", 24, 0, 0, 0),
	BETA("β", 25, 0, 4, -4),
	GAMMA("γ", 26, -2, 4, -2),
	DELTA("δ", 27, -4, 4, 0),
	PSI("ψ", 47, -4, 2, 2),
	PHI("φ", 45, -4, 0, 4),
	OMEGA("ω", 48, -2, -2, 4),
	LAMBDA("λ", 34, 0, -4, 4),
	THETA("θ", 31, 2, -4, 2),
	CHI("χ", 46, 4, -4, 0),
	MU("μ", 35, 4, -2, -2),
	TAU("τ", 43, 4, 0, -4),
	SIGMA("σ", 42, 2, 2, -4);

	public final String character;
	public final int textureIndex;
	private final Hex hex;
	private ChunkPos position;

	public static final Towers[] towerList = values();

	private static final Map<ChunkPos, Towers> towerChunkCache = new HashMap<>();
	private static final EnumMap<Towers, BlockPos> towerCache = new EnumMap<>(Towers.class);
	private static long lastWorldSeed;

	private static final double TOWER_OFFSET_RADIUS = 2500;
	/** Keep the lore monuments clear of even the outer pieces of a predicted vanilla village. */
	private static final int VILLAGE_EXCLUSION_CHUNKS = 8;
	private static final int VILLAGE_RELOCATION_SEARCH = 16;

	Towers(String s, int idx, int x, int y, int z) {
		character = s;
		textureIndex = idx;
		hex = new Hex(1, x, y, z);
	}

	/** This is BLOCK coords, despite the ChunkPos type -- as upstream. */
	public ChunkPos getRootPosition() {
		return position;
	}

	/** Client-side correction from the server's authoritative layout. */
	public void setLocationFromServer(int x, int z) {
		towerChunkCache.remove(position);
		position = new ChunkPos(x, z);
		towerChunkCache.put(position, this);
	}

	/** Radius is in chunks, and is the size of a hex in "chunks as pixels". */
	// ServerLevel, not Level: only the server knows the seed in 26.2, which is precisely why V33a
	// has setLocationFromServer for the client half.
	public static synchronized void loadPositions(ServerLevel world, double radius) {
		towerChunkCache.clear();
		towerCache.clear();

		double r = radius * Mth.clamp(DragonOptions.WORLDSIZE.getValue() / 6000D, 0.5, 5);
		HexGrid grid = new HexGrid(9, r, true, MapShape.HEXAGON).flower();

		Random rand = new Random(world.getSeed()
				^ ReikaWorldHelper.getCurrentWorldID(world).worldCreationTime);
		rand.nextBoolean();
		rand.nextBoolean();
		double a = rand.nextDouble() * 360;
		double dx = -TOWER_OFFSET_RADIUS + rand.nextDouble() * TOWER_OFFSET_RADIUS * 2
				+ DragonOptions.WORLDCENTERX.getValue();
		double dz = -TOWER_OFFSET_RADIUS + rand.nextDouble() * TOWER_OFFSET_RADIUS * 2
				+ DragonOptions.WORLDCENTERZ.getValue();

		Set<StructurePlacement> villagePlacements = getVillagePlacements(world);
		for (Towers t : towerList) {
			grid.addHex(t.hex.q, t.hex.r, t.hex.s);
			Hex ref = grid.getHex(t.hex.q, t.hex.r, t.hex.s);
			Point p = grid.getHexLocation(ref).rotate(a, 0, 0).translate(dx, dz);
			int x = ReikaMathLibrary.roundToNearestX(16, (int)Math.round(p.x));
			int y = ReikaMathLibrary.roundToNearestX(16, (int)Math.round(p.y));
			ChunkPos clear = findVillageClearChunk(world, x >> 4, y >> 4, villagePlacements);
			x = clear.x() << 4;
			y = clear.z() << 4;
			t.position = new ChunkPos(x, y);
			towerChunkCache.put(t.position, t);
		}

		lastWorldSeed = world.getSeed();
	}

	/**
	 * Village starts are determined entirely by their structure placements and the world seed. Query
	 * those placements here, before terrain decoration, rather than loading or reading neighboring
	 * chunks from a worldgen worker. This intentionally errs on the safe side when a predicted start
	 * later rejects its biome: a lore tower being slightly nudged is preferable to cutting through a
	 * village whose outer jigsaw pieces extend well beyond its start chunk.
	 */
	private static Set<StructurePlacement> getVillagePlacements(ServerLevel world) {
		if (!world.structureManager().shouldGenerateStructures())
			return Set.of();
		ChunkGeneratorStructureState state = world.getChunkSource().getGeneratorState();
		Set<StructurePlacement> placements = new HashSet<>();
		world.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements()
				.filter(holder -> holder.is(StructureTags.VILLAGE))
				.forEach(holder -> placements.addAll(state.getPlacementsForStructure(holder)));
		return Set.copyOf(placements);
	}

	private static ChunkPos findVillageClearChunk(ServerLevel world, int baseX, int baseZ,
			Set<StructurePlacement> placements) {
		if (placements.isEmpty())
			return new ChunkPos(baseX, baseZ);
		ChunkGeneratorStructureState state = world.getChunkSource().getGeneratorState();
		for (int radius = 0; radius <= VILLAGE_RELOCATION_SEARCH; radius++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dx = -radius; dx <= radius; dx++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
						continue;
					int x = baseX + dx;
					int z = baseZ + dz;
					if (isVillageClear(state, x, z, placements))
						return new ChunkPos(x, z);
				}
			}
		}
		// Vanilla village spacing makes this practically unreachable. Retain the authoritative tower
		// instead of deleting a required puzzle node if a datapack supplies pathological placements.
		return new ChunkPos(baseX, baseZ);
	}

	private static boolean isVillageClear(ChunkGeneratorStructureState state, int towerX, int towerZ,
			Set<StructurePlacement> placements) {
		for (StructurePlacement placement : placements) {
			for (int dz = -VILLAGE_EXCLUSION_CHUNKS; dz <= VILLAGE_EXCLUSION_CHUNKS; dz++) {
				for (int dx = -VILLAGE_EXCLUSION_CHUNKS; dx <= VILLAGE_EXCLUSION_CHUNKS; dx++) {
					if (placement.isStructureChunk(state, towerX + dx, towerZ + dz))
						return false;
				}
			}
		}
		return true;
	}

	/** In block coords. */
	public static Towers getTowerForChunk(int cx, int cz) {
		return towerChunkCache.get(new ChunkPos(cx, cz));
	}

	/**
	 * V33a {@code LoreManager.getTower}, which is only {@code initTowers} followed by this lookup.
	 * Exposed here so consumers do not have to drag in LoreManager, whose other half belongs to the
	 * lore/research vertical.
	 */
	public static Towers getTower(ServerLevel world, int cx, int cz) {
		if (!initialized(world))
			loadPositions(world, 64 * 16 * 2);
		return getTowerForChunk(cx, cz);
	}

	public void generatedAt(int x, int y, int z) {
		towerCache.put(this, new BlockPos(x, y, z));
	}

	public BlockPos getGeneratedLocation() {
		return towerCache.get(this);
	}

	public Towers getNeighbor1() {
		if (this == ALPHA)
			return null;
		if (this == SIGMA)
			return BETA;
		return towerList[this.ordinal() + 1];
	}

	public Towers getNeighbor2() {
		if (this == ALPHA)
			return null;
		if (this == BETA)
			return SIGMA;
		return towerList[this.ordinal() - 1];
	}

	public static boolean initialized(ServerLevel world) {
		return !towerChunkCache.isEmpty() && lastWorldSeed == world.getSeed();
	}
}
