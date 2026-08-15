package reika.chromaticraft.world.dimension;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/**
 * V33a {@code DimensionStructureGenerator.DimensionStructureType}: the eighteen puzzle structures
 * that Proxima scatters, one per crystal element, around its central monument.
 *
 * <p>Upstream this is a nested enum of the 631-line generator base. It is lifted to its own type here
 * because the placement layer — {@link StructureCalculator}, and through it {@code RegionMapper} and
 * {@code BiomeDistributor} — depends only on the structure <em>identities and positions</em>, never on
 * any puzzle mechanic. Keeping the enum separate is what lets Proxima's terrain and biome layout be
 * ported while the puzzles themselves stay deliberately deferred.
 *
 * <p><b>{@link #isComplete()} is the real V33a gate, not a placeholder.</b> Upstream determines it by
 * constructing each generator once at class-init, running {@code startCalculate} at the origin, and
 * asking whether it produced a core; anything that failed is filtered out of
 * {@code getUsableStructures()} and never assigned to an element. That is exactly why
 * {@code allowUnfinishedStructures} exists — Reika shipped with unfinished generators. This port is in
 * the same position with all eighteen unported, so the same filter correctly yields nothing today. A
 * generator becomes usable the moment it is ported and handed to {@link #registerGenerator}; no other
 * code has to change.
 */
public enum DimensionStructureType {

	TDMAZE("Three-Dimensional Maze"),
	ALTAR(""),
	SHIFTMAZE("Shifting Maze"),
	LOCKS("Locks and Keys"),
	MUSIC("Crystal Music"),
	NONEUCLID("Complex Spaces"),
	GOL("Cellular Automata"),
	ANTFARM("Fading Light"),
	LASER("Chromatic Beams"),
	PINBALL("Expanding Motion"),
	GRAVITY("Luma Bursts"),
	BRIDGE("Dynamic Bridges"),
	WATER("Channeled Flow"),
	TESSELLATION("Spatial Satisfaction"),
	LIGHTPANEL("Glowing Logic"),
	PISTONTAPE("Filter Cycle"),
	RAYBLEND("Crystal Interference"),
	TRACES("Parallel Connections"),
	;

	/** V33a NBT key on the player's death-persistent research tag. */
	private static final String NBT_TAG = "structuresCompleted";

	public static final DimensionStructureType[] types = values();

	private final String description;

	/**
	 * The ported generator factory for this type, or null while the puzzle is deferred.
	 *
	 * <p>Deliberately a registry rather than a constructor argument: V33a resolves its generator class
	 * reflectively at class-init and immediately test-runs it, which cannot happen here for a class
	 * that does not exist yet. Registration is the modern equivalent of "this generator class compiles
	 * and produces a core".
	 */
	private Supplier<? extends ProximaStructureGenerator> generatorFactory;

	private final Map<UUID, ProximaStructureGenerator> generators = new java.util.HashMap<>();
	private static final Map<UUID, DimensionStructureType> generatorTypes = new java.util.HashMap<>();

	DimensionStructureType(String description) {
		this.description = description;
	}

	/**
	 * The contract a ported puzzle generator has to satisfy for its type to become usable. It is
	 * intentionally narrow: the placement layer only ever needs where a structure's entry sits, which
	 * V33a defaults to the placement coordinate and lets {@code calculate()} refine.
	 */
	public interface ProximaStructureGenerator {

		/** V33a startCalculate: seeds the generator at a placement and lays the structure out. */
		void startCalculate(reika.chromaticraft.registry.CrystalElement color, int x, int z,
				java.util.Random rand);

		/** V33a getEntryPosX/Z, which default to the placement coordinate until calculate() moves them. */
		int getEntryPosX();

		int getEntryPosZ();

		/** V33a isComplete: whether the layout actually produced a core. */
		boolean isComplete();

		void clear();

		UUID id();
	}

	/**
	 * Declares that this structure's generator is ported. V33a's equivalent is the class-init test run:
	 * the factory is exercised once at the origin and the type is only marked usable if that run
	 * produced a core, so a ported-but-broken generator is filtered out exactly as upstream filters an
	 * unfinished one.
	 */
	public synchronized void registerGenerator(Supplier<? extends ProximaStructureGenerator> factory) {
		generatorFactory = factory;
		ProximaStructureGenerator test = factory.get();
		test.startCalculate(reika.chromaticraft.registry.CrystalElement.WHITE, 0, 0, new java.util.Random());
		if (!test.isComplete())
			generatorFactory = null;
		test.clear();
	}

	/** V33a createGenerator(idx): a fresh cached generator, or null while this type is unported. */
	public synchronized ProximaStructureGenerator createGenerator() {
		if (generatorFactory == null)
			return null;
		ProximaStructureGenerator generator = generatorFactory.get();
		generators.put(generator.id(), generator);
		generatorTypes.put(generator.id(), this);
		return generator;
	}

	/** V33a isComplete(): whether this structure's generator produced a core when test-run. */
	public synchronized boolean isComplete() {
		return generatorFactory != null;
	}

	/** V33a getUsableStructures(): the types that may be assigned to an element. */
	public static List<DimensionStructureType> usableStructures() {
		List<DimensionStructureType> usable = new ArrayList<>(types.length);
		for (DimensionStructureType type : types)
			if (type.isComplete())
				usable.add(type);
		return usable;
	}

	public String getDisplayText() {
		return description;
	}

	/** V33a getIconIndex: the structure icons start eight into the sheet. */
	public int getIconIndex() {
		return 8 + this.ordinal();
	}

	/** V33a markPlayerCompleted: one boolean per structure on the death-persistent research tag. */
	public void markPlayerCompleted(Player ep) {
		CompoundTag data = rootTag(ep);
		data.putBoolean("struct_" + this.ordinal(), true);
		ReikaPlayerAPI.getDeathPersistentNBT(ep).put(NBT_TAG, data);
	}

	public boolean hasPlayerCompleted(Player ep) {
		return NBTCompat.getBoolean(rootTag(ep), "struct_" + this.ordinal(), false);
	}

	private static CompoundTag rootTag(Player ep) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(ep);
		CompoundTag data = NBTCompat.getCompound(root, NBT_TAG);
		root.put(NBT_TAG, data);
		return data;
	}

	public synchronized ProximaStructureGenerator getGenerator(UUID id) {
		return generators.get(id);
	}

	public static ProximaStructureGenerator getGeneratorByID(UUID id) {
		DimensionStructureType type = generatorTypes.get(id);
		return type == null ? null : type.getGenerator(id);
	}

	/** V33a resetCachedGenerators, called whenever the dimension's layout is recomputed. */
	public static synchronized void resetCachedGenerators() {
		generatorTypes.clear();
		for (DimensionStructureType type : types)
			type.generators.clear();
	}

	/**
	 * V33a {@code StructureTypeData}: the per-element identity of a structure, and the seed of the
	 * password a player has to read off it. {@code generationIndex} counts how many times the usable
	 * set has been exhausted and reused, so two elements sharing a structure type still get different
	 * passwords.
	 */
	public record StructureTypeData(reika.chromaticraft.registry.CrystalElement color,
			DimensionStructureType type, int generationIndex) {

		/**
		 * V33a getPassword: the player's UUID hash XORed with per-structure, per-type, per-difficulty
		 * and per-version constants, then SHA-1'd. Ported verbatim so a structure's password does not
		 * change under a player.
		 *
		 * <p>V33a's version term is {@code Loader.MC_VERSION.hashCode()}; the 26.2 equivalent is the
		 * running game version's name, which is passed in rather than read statically so datagen and
		 * tests can pin it.
		 */
		public int getPassword(Player ep, String gameVersion) {
			int hash = (ep != null ? ep.getUUID() : DragonAPI.Reika_UUID).toString().hashCode();
			hash = hash ^ 3178531 * generationIndex;
			hash = hash ^ 1780943 * type.ordinal();
			hash = hash ^ 4702617 * ChromaOptions.getStructureDifficulty();
			hash = hash ^ 3689507 * gameVersion.hashCode();
			return ReikaFileReader.HashType.SHA1.hash(hash).hashCode();
		}
	}

	/** Convenience for the client-side colour-to-type map V33a builds in getStructureColorTypes. */
	public static EnumMap<reika.chromaticraft.registry.CrystalElement, StructureTypeData> emptyTypeMap() {
		return new EnumMap<>(reika.chromaticraft.registry.CrystalElement.class);
	}
}
