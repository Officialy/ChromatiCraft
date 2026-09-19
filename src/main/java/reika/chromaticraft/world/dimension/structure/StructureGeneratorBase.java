package reika.chromaticraft.world.dimension.structure;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.DimensionStructureType.ProximaStructureGenerator;

/**
 * V33a {@code DimensionStructureGenerator}: the base every Proxima puzzle structure is built on.
 *
 * <p>Upstream this is a 631-line abstract class that is also its own worldgen writer — it owns a
 * {@code ChunkSplicedGenerationCache}, subclasses call {@code world.setBlock} into it as they lay
 * themselves out, and {@code generateChunk} later flushes whichever cells fall in a given chunk. That
 * half deliberately does not come across: 26.2 expresses "lay a shape out once, then write it chunk by
 * chunk with a clipped box" as a {@code Structure} and its pieces, which is the same idea with the
 * engine doing the bookkeeping. So a generator here <em>plans</em> — it fills {@link #plan} with the
 * cells it wants — and a {@code StructurePiece} paints them. {@code MonumentPiece} is that shape.
 *
 * <p>Everything else is upstream's, because everything else is what the rest of the mod talks to: the
 * placement coordinates and entry point the region mapper reads, the core the Dimension Core tile
 * attaches itself to, the solved and forced-open state that decides whether that core may be mined,
 * and the per-player password.
 *
 * <h2>Why {@code isComplete} is "did you place a core"</h2>
 *
 * <p>It reads like a placeholder and is not. Upstream test-runs every generator once at class-init and
 * marks the type usable only if that run produced a core, which is how Reika filtered out her own
 * unfinished generators; {@link DimensionStructureType#registerGenerator} already reproduces that
 * exactly. A generator that lays out no core is an unfinished generator, and saying so is the contract.
 */
public abstract class StructureGeneratorBase implements ProximaStructureGenerator {

	/**
	 * The cells this generator wants written, in insertion order. Upstream's equivalent is the splice
	 * cache it writes into; here it is plain data, so the piece that paints it can clip to a chunk and
	 * the generator itself never touches a level.
	 */
	protected final Map<BlockPos, BlockState> plan = new LinkedHashMap<>();

	/** V33a's breakable set: cells a player may mine even inside a structure that is still sealed. */
	private final Set<BlockPos> breakable = new HashSet<>();

	private final UUID id = UUID.randomUUID();

	private DimensionStructureType structureType;
	private int structureTypeIndex;
	private CrystalElement generationColor;

	protected int posX;
	protected int posY;
	protected int posZ;
	protected int entryX;
	protected int entryZ;

	private ChunkPos genCore;
	private ChunkPos centre;
	protected BlockPos coreLocation;
	private boolean forcedOpen;

	protected StructureGeneratorBase() {}

	@Override
	public final UUID id() {
		return id;
	}

	public final void setType(DimensionStructureType type, int index) {
		structureType = type;
		structureTypeIndex = index;
	}

	public final DimensionStructureType getType() {
		return structureType;
	}

	public final int getGenerationIndex() {
		return structureTypeIndex;
	}

	public final CrystalElement getCoreColor() {
		return generationColor;
	}

	public final int getPosX() {
		return posX;
	}

	public final int getPosY() {
		return posY;
	}

	public final int getPosZ() {
		return posZ;
	}

	@Override
	public final int getEntryPosX() {
		return entryX;
	}

	@Override
	public final int getEntryPosZ() {
		return entryZ;
	}

	/** V33a offsetEntry: a generator moves its own entry once it knows where its door ended up. */
	public void offsetEntry(int dx, int dz) {
		entryX += dx;
		entryZ += dz;
	}

	/**
	 * V33a startCalculate. The entry defaults to the placement coordinate and {@link #calculate} is
	 * free to move it; that default is what the region mapper reads for a structure whose generator
	 * never refines it.
	 */
	@Override
	public final void startCalculate(CrystalElement color, int chunkX, int chunkZ, Random rand) {
		generationColor = color;
		genCore = new ChunkPos(chunkX >> 4, chunkZ >> 4);
		posX = chunkX;
		posZ = chunkZ;
		entryX = chunkX;
		entryZ = chunkZ;
		long start = System.currentTimeMillis();
		ChromatiCraft.LOGGER.info("Calculating a {} {}", color, this.getType());
		this.calculate(chunkX, chunkZ, rand);
		ChromatiCraft.LOGGER.info("Done in {} ms", System.currentTimeMillis() - start);
	}

	/** V33a calculate. The coordinates handed in are block coordinates, not chunk indices. */
	protected abstract void calculate(int chunkX, int chunkZ, Random rand);

	@Override
	public final void clear() {
		plan.clear();
		breakable.clear();
		centre = null;
		coreLocation = null;
		forcedOpen = false;
		this.clearCaches();
	}

	/** V33a clearCaches: a subclass drops whatever layout state it kept between runs. */
	protected abstract void clearCaches();

	public final Map<BlockPos, BlockState> getPlan() {
		return Collections.unmodifiableMap(plan);
	}

	public final Set<BlockPos> getBreakableSpots() {
		return Collections.unmodifiableSet(breakable);
	}

	public final void addBreakable(int x, int y, int z) {
		breakable.add(new BlockPos(x, y, z));
	}

	public final ChunkPos getLocation() {
		return genCore;
	}

	public final ChunkPos getCentralLocation() {
		if (centre == null)
			centre = new ChunkPos(genCore.x() + (this.getCenterXOffset() >> 4),
					genCore.z() + (this.getCenterZOffset() >> 4));
		return centre;
	}

	public final ChunkPos getEntryLocation() {
		return new ChunkPos(entryX >> 4, entryZ >> 4);
	}

	protected abstract int getCenterXOffset();

	protected abstract int getCenterZOffset();

	/** V33a placeCore: records where the Dimension Core goes; the piece is what writes it. */
	public final void placeCore(int x, int y, int z) {
		coreLocation = new BlockPos(x, y, z);
	}

	public final BlockPos getCoreLocation() {
		return coreLocation;
	}

	public boolean hasCore() {
		return coreLocation != null;
	}

	@Override
	public boolean isComplete() {
		return this.hasCore();
	}

	/** V33a shouldAllowCoreMining: the core is sealed until the puzzle is solved, or forced open. */
	public final boolean shouldAllowCoreMining(Level world) {
		return forcedOpen || this.hasBeenSolved(world);
	}

	protected abstract boolean hasBeenSolved(Level world);

	public final void forceOpen(Level world) {
		forcedOpen = true;
		this.openStructure(world);
	}

	public final boolean forcedOpen() {
		return forcedOpen;
	}

	protected abstract void openStructure(Level world);

	/** Per-player puzzle hook. Empty in V33a's base; Bridge is the only original override. */
	public void tickPlayer(Player player) {
	}

	/** V33a getPassword, through the type identity that already carries the hash. */
	public final int getPassword(Player player, String gameVersion) {
		return new DimensionStructureType.StructureTypeData(generationColor, this.getType(),
				structureTypeIndex).getPassword(player, gameVersion);
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName() + " @ " + this.getCentralLocation()
				+ " (E = " + this.getEntryLocation() + ")";
	}

	/**
	 * V33a {@code StructurePair}: which generator a Dimension Core belongs to, and in which colour.
	 * Equality is by generator identity and colour, exactly as upstream, so two cores of the same
	 * colour in different structures are different pairs.
	 */
	public static final class StructurePair {

		public final StructureGeneratorBase generator;
		public final CrystalElement color;

		/** V33a generatedDimension, so a core knows which level it was laid out for. */
		public ResourceKey<Level> generatedDimension;

		public StructurePair(StructureGeneratorBase generator, CrystalElement color) {
			this.generator = generator;
			this.color = color;
		}

		@Override
		public int hashCode() {
			return (color.ordinal() << 8) | generator.getType().ordinal();
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof StructurePair other && other.color == color
					&& other.generator == generator;
		}

		@Override
		public String toString() {
			return color.name() + " " + generator;
		}
	}
}
