package reika.chromaticraft.world.dimension.structure;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;

/**
 * Immutable, deterministic view of the authored V33a Chromatic Beams rooms.
 *
 * <p>The original {@code StructureExport} files are not compressed NBT: the entire byte stream is
 * reversed as the old lightweight encryption step. Decoding the stream here preserves every
 * authored effector and tile setting while giving the 26.2 structure/datagen layer a normal model
 * to convert into structure NBT. Room shells, doors and reward geometry remain the responsibility of
 * the structure piece, just as they were in V33a's {@code LaserLevel}.
 */
public final class LaserPuzzleLayout {

	public static final int ROOM_GAP = 23;
	private static final String RESOURCE_ROOT = "/assets/chromaticraft/structures/laser/";
	private static final String LEGACY_BLOCK = "ChromatiCraft:chromaticraft_block_lasereffect";

	private static final List<String> BASE_ROOMS = List.of(
			"mirrortut", "mirrors2", "refractortut", "splittertut");
	private static final List<String> MEDIUM_ROOMS = List.of(
			"filtertut", "polartut", "polar2", "oneway");
	private static final List<String> PRISM_TUTORIALS = List.of("prismtut1", "prismtut2");
	private static final List<String> HARD_ROOMS = List.of("prism3", "complex");

	private final int difficulty;
	private final List<PlacedRoom> rooms;

	private LaserPuzzleLayout(int difficulty, List<PlacedRoom> rooms) {
		this.difficulty = difficulty;
		this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
	}

	public static LaserPuzzleLayout create(int difficulty, long seed) {
		if (difficulty < 1 || difficulty > 3)
			throw new IllegalArgumentException("Laser puzzle difficulty must be 1..3, got " + difficulty);
		Random random = new Random(seed);
		List<PlacedRoom> placed = new ArrayList<>();
		int x = 13;
		for (String name : roomNames(difficulty)) {
			RoomBlueprint blueprint = load(name);
			List<Effector> effectors = new ArrayList<>(blueprint.effectors().size());
			for (Effector authored : blueprint.effectors())
				effectors.add(authored.randomizedDirection(random));
			RoomBlueprint resolved = new RoomBlueprint(blueprint.name(), blueprint.sizeX(),
					blueprint.sizeZ(), effectors);
			placed.add(new PlacedRoom(x, resolved));
			x += blueprint.sizeX() + ROOM_GAP;
		}
		return new LaserPuzzleLayout(difficulty, placed);
	}

	public static List<String> roomNames(int difficulty) {
		if (difficulty < 1 || difficulty > 3)
			throw new IllegalArgumentException("Laser puzzle difficulty must be 1..3, got " + difficulty);
		List<String> names = new ArrayList<>(12);
		names.addAll(BASE_ROOMS);
		if (difficulty > 1)
			names.addAll(MEDIUM_ROOMS);
		names.addAll(PRISM_TUTORIALS);
		if (difficulty > 2)
			names.addAll(HARD_ROOMS);
		return List.copyOf(names);
	}

	public int difficulty() {
		return difficulty;
	}

	public List<PlacedRoom> rooms() {
		return rooms;
	}

	public int totalEffectorCount() {
		return rooms.stream().mapToInt(room -> room.blueprint().effectors().size()).sum();
	}

	/** X coordinate at which V33a places the terminal loot room after the complex room. */
	public int lootStartX() {
		PlacedRoom last = rooms.getLast();
		return last.x() + last.blueprint().sizeX() + 8;
	}

	public static RoomBlueprint load(String name) {
		String normalized = name.toLowerCase(Locale.ROOT);
		String path = RESOURCE_ROOT + normalized + ".struct";
		try (InputStream in = ChromatiCraft.class.getResourceAsStream(path)) {
			if (in == null)
				throw new IllegalStateException("Missing V33a laser room asset " + path);
			byte[] encrypted = in.readAllBytes();
			for (int i = 0, j = encrypted.length - 1; i < j; i++, j--) {
				byte swap = encrypted[i];
				encrypted[i] = encrypted[j];
				encrypted[j] = swap;
			}
			CompoundTag root = NbtIo.read(new DataInputStream(new ByteArrayInputStream(encrypted)));
			ListTag data = root.getListOrEmpty("data");
			if (data.isEmpty())
				throw new IllegalStateException("Laser room " + normalized + " contains no effectors");

			List<RawEffector> raw = new ArrayList<>(data.size());
			int minX = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;
			for (CompoundTag entry : data.compoundStream().toList()) {
				CompoundTag type = entry.getCompoundOrEmpty("type");
				String id = type.getStringOr("id", "");
				if (!LEGACY_BLOCK.equals(id))
					throw new IllegalStateException("Unexpected block '" + id + "' in laser room " + normalized);
				int meta = type.getIntOr("meta", -1);
				EffectType effect = EffectType.byLegacyMetadata(meta);
				CompoundTag location = entry.getCompoundOrEmpty("loc");
				int x = location.getIntOr("x", 0);
				int y = location.getIntOr("y", 0);
				int z = location.getIntOr("z", 0);
				CompoundTag tile = entry.getCompoundOrEmpty("tiledat").copy();
				raw.add(new RawEffector(new BlockPos(x, y, z), effect, tile));
				minX = Math.min(minX, x);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxZ = Math.max(maxZ, z);
			}

			List<Effector> effectors = new ArrayList<>(raw.size());
			for (RawEffector entry : raw) {
				CompoundTag tile = entry.tile();
				int direction = Math.floorMod(tile.getIntOr("dir", 0), CubeDirections.list.length);
				effectors.add(new Effector(entry.position().offset(-minX, 0, -minZ), entry.type(),
						CubeDirections.list[direction], tile.getBooleanOr("red", true),
						tile.getBooleanOr("green", true), tile.getBooleanOr("blue", true),
						tile.getBooleanOr("free", false), tile.getBooleanOr("fixed", false),
						tile.getIntOr("mindiff", 0), tile.getIntOr("timer", 2),
						tile.getBooleanOr("fullblock", false), tile.getBooleanOr("silent", false),
						tile.getDoubleOr("speed", 1)));
			}
			// V33a BlockBox#getSizeX/Z is max-min, not the inclusive cell count.
			return new RoomBlueprint(normalized, maxX - minX, maxZ - minZ, effectors);
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not decode V33a laser room " + normalized, e);
		}
	}

	public enum EffectType {
		EMITTER,
		TARGET,
		TARGET_THRU,
		MIRROR,
		DOUBLEMIRROR,
		SLITMIRROR,
		REFRACTOR,
		ONEWAY,
		SPLITTER,
		PRISM,
		COLORIZER,
		POLARIZER;

		private static final EffectType[] VALUES = values();

		public static EffectType byLegacyMetadata(int metadata) {
			if (metadata < 0 || metadata >= VALUES.length)
				throw new IllegalArgumentException("Unknown V33a laser effector metadata " + metadata);
			return VALUES[metadata];
		}

		public boolean isTarget() {
			return this == TARGET || this == TARGET_THRU;
		}

		public boolean isOmnidirectional() {
			return this == TARGET_THRU || this == COLORIZER;
		}
	}

	public record Effector(BlockPos position, EffectType type, CubeDirections direction,
			boolean red, boolean green, boolean blue, boolean rotateable, boolean fixed,
			int rotateableDifficulty, int prismTimer, boolean fullBlock, boolean silent,
			double speedFactor) {

		private Effector randomizedDirection(Random random) {
			boolean randomize = !fixed && !type.isOmnidirectional() && type != EffectType.EMITTER
					&& !type.isTarget();
			return randomize ? new Effector(position, type,
					CubeDirections.list[random.nextInt(CubeDirections.list.length)], red, green, blue,
					rotateable, fixed, rotateableDifficulty, prismTimer, fullBlock, silent, speedFactor)
					: this;
		}
	}

	public record RoomBlueprint(String name, int sizeX, int sizeZ, List<Effector> effectors) {
		public RoomBlueprint {
			effectors = Collections.unmodifiableList(new ArrayList<>(effectors));
		}

		public long emitterCount() {
			return effectors.stream().filter(e -> e.type() == EffectType.EMITTER).count();
		}

		public long targetCount() {
			return effectors.stream().filter(e -> e.type().isTarget()).count();
		}
	}

	public record PlacedRoom(int x, RoomBlueprint blueprint) {}

	private record RawEffector(BlockPos position, EffectType type, CompoundTag tile) {}
}
