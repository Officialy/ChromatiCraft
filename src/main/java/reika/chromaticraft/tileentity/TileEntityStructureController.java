package reika.chromaticraft.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.auxiliary.BiomeStructureMelodies;
import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.decoration.BlockMusicTrigger;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightPanel;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightSwitch;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/**
 * Modern persistence/lifecycle owner for V33a's six natural fragment structures.
 * Geometry is NBT-owned; this entity stores only state that changes after placement.
 */
public final class TileEntityStructureController extends RandomizableContainerBlockEntity
		implements TileEntityLockKey.Handler, TileEntityLightSwitch.Handler, BlockMusicTrigger.Handler {

	public enum StructureType {
		CAVERN(ProgressStage.CAVERN, 3, 1, 3, 0, 0, 0),
		BURROW(ProgressStage.BURROW, 0.5, 0.5, 0.5, 2, 1, 0),
		OCEAN(ProgressStage.OCEAN, 1, 1, 1, 0, 2, 0),
		DESERT(ProgressStage.DESERTSTRUCT, 5, 2, 5, 0, 1, 0),
		SNOW(ProgressStage.SNOWSTRUCT, 6, 1, 6, 0, 4, 2),
		BIOME_FRAGMENT(ProgressStage.BIOMESTRUCT, 7, 6, 7, 0, 4, 0);

		private final ProgressStage progress;
		private final double radiusX;
		private final double radiusY;
		private final double radiusZ;
		private final double offsetX;
		private final double offsetY;
		private final double offsetZ;

		StructureType(ProgressStage progress, double radiusX, double radiusY, double radiusZ,
				double offsetX, double offsetY, double offsetZ) {
			this.progress = progress;
			this.radiusX = radiusX;
			this.radiusY = radiusY;
			this.radiusZ = radiusZ;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
		}

		public String serializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final int SIZE = 27;
	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private @Nullable StructureType structure;
	private CrystalElement color = CrystalElement.WHITE;
	private boolean triggered;
	private boolean regenerated;
	private int structureVersion;
	private int trapTick;
	private boolean furnaceRoom;
	private boolean lootRoom;
	private boolean generationErrored;
	private @Nullable UUID lastTriggerPlayer;

	/**
	 * The monument half. A monument controller has no {@code structure} — it is not a fragment structure
	 * — so its state is separate and its tick runs before the fragment path's early return.
	 */
	private boolean isMonument;
	private boolean triggeredMonument;
	private transient reika.chromaticraft.magic.@Nullable MonumentCompletionRitual monument;
	private boolean biomePuzzleReady;
	private int biomeKeyChannel;
	private final int[] biomeDoorMasks = new int[4];
	private final CrystalElement[] biomeDoorColors = new CrystalElement[8];
	private final CrystalElement[] biomeRuneColors = new CrystalElement[8];
	private final CrystalElement[] biomeCrystalColors = new CrystalElement[8];
	private final ArrayList<MusicKey> biomeMelody = new ArrayList<>();
	private int biomePlaySpeed = 8;
	private int biomeGuessIndex;
	private long biomeMusicTick;
	private long biomeNextNoteTick = 10;
	private int biomeMelodyIndex = -1;
	private int biomeMusicCooldown;
	private boolean biomeComplete;

	public TileEntityStructureController(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.STRUCTURE_CONTROLLER.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state,
			TileEntityStructureController controller) {
		// The monument runs before the fragment-structure gate below: a monument controller has no
		// structure type, so that early return would otherwise never let its ritual tick.
		if (controller.isMonument && controller.triggeredMonument && controller.monument != null) {
			controller.monument.tick();
			if (!controller.monument.isRunning())
				controller.endMonumentRitual();
		}
		if (controller.structure == null)
			return;
		if (controller.trapTick > 0) {
			controller.trapTick--;
			if (controller.trapTick == 0 && controller.structure == StructureType.OCEAN)
				controller.resetOceanTrap();
			controller.setChanged();
		}
		if (controller.structure == StructureType.BIOME_FRAGMENT)
			controller.tickBiomeMusic();
		if (controller.triggered || level.getGameTime() % 4 != 0)
			return;
		for (Player player : level.players()) {
			if (player.getBoundingBox().intersects(controller.proximityBox())) {
				controller.triggerProximity(player);
				break;
			}
		}
	}

	public void initialize(StructureType type, CrystalElement element, int version,
			boolean hasFurnaceRoom, boolean hasLootRoom) {
		structure = type;
		color = element == null ? CrystalElement.WHITE : element;
		structureVersion = version;
		furnaceRoom = hasFurnaceRoom;
		lootRoom = hasLootRoom;
		if (type == StructureType.BIOME_FRAGMENT)
			initializeBiomePuzzle(net.minecraft.util.RandomSource.create(
					Objects.hash(level != null ? level.dimension() : "unbound", worldPosition, version)));
		triggered = false;
		regenerated = true;
		setChanged();
	}

	/** Generates and persists the full randomized V33a Biome Fragment puzzle state. */
	public void initializeBiomePuzzle(net.minecraft.util.RandomSource random) {
		biomeKeyChannel = random.nextInt(8);
		java.util.HashSet<Integer> used = new java.util.HashSet<>();
		for (int i = 0; i < biomeDoorMasks.length; i++) {
			int ownCorner = 1 << i;
			int mask;
			do mask = random.nextInt(16);
			while (used.contains(mask) || mask == ownCorner);
			biomeDoorMasks[i] = mask;
			used.add(mask);
		}
		java.util.ArrayList<CrystalElement> available = new java.util.ArrayList<>(List.of(CrystalElement.elements));
		for (int i = 0; i < biomeDoorColors.length; i++)
			biomeDoorColors[i] = available.remove(random.nextInt(available.size()));
		ArrayList<CrystalElement> shuffledRunes = new ArrayList<>(List.of(biomeDoorColors));
		Collections.shuffle(shuffledRunes, new Random(random.nextLong()));
		for (int i = 0; i < biomeRuneColors.length; i++) biomeRuneColors[i] = shuffledRunes.get(i);
		initializeBiomeMelody(random);
		biomePuzzleReady = true;
		bindBiomePuzzleBlocks();
		setChanged();
	}

	/** Attaches every generated interaction tile to this persistent controller. */
	public void bindBiomePuzzleBlocks() {
		if (level == null || !biomePuzzleReady) return;
		for (int door = 0; door < 4; door++)
			for (BlockPos pos : biomeColorDoorLocations(door))
				if (level.getBlockEntity(pos) instanceof TileEntityColorLock lock)
					lock.setColors(biomeDoorColors[door * 2], biomeDoorColors[door * 2 + 1]);
		for (int i = 0; i < 4; i++) {
			BlockPos pos = biomeSwitchPosition(i);
			if (level.getBlockEntity(pos) instanceof TileEntityLightSwitch panel)
				panel.setDelegate(worldPosition);
		}
		for (BlockPos pos : List.of(worldPosition.offset(0, 5, -2), worldPosition.offset(0, 5, 2))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ChromaBlocks.LOCK_KEY.get())) {
				if (state.getValue(reika.chromaticraft.block.dimension.structure.locks.BlockLockKey.CHANNEL)
						!= biomeKeyChannel)
					level.setBlock(pos, state.setValue(
							reika.chromaticraft.block.dimension.structure.locks.BlockLockKey.CHANNEL,
							biomeKeyChannel), 3);
				if (level.getBlockEntity(pos) instanceof TileEntityLockKey key)
					key.setDelegate(worldPosition);
			}
		}
		updateBiomeColorDoors();
		updateBiomeSwitchDoors(false);
	}

	public int getBiomeKeyChannel() { return biomeKeyChannel; }
	public int getBiomeDoorMask(int door) { return biomeDoorMasks[door]; }
	public CrystalElement getBiomeDoorColor(int door, int index) { return biomeDoorColors[door * 2 + index]; }
	public CrystalElement getBiomeRuneColor(int index) { return biomeRuneColors[index]; }
	public CrystalElement getBiomeCrystalColor(int index) { return biomeCrystalColors[index]; }
	public List<MusicKey> getBiomeMelody() { return Collections.unmodifiableList(biomeMelody); }
	public int getBiomePlaySpeed() { return biomePlaySpeed; }
	public int getBiomeGuessIndex() { return biomeGuessIndex; }
	public boolean isBiomeComplete() { return biomeComplete; }

	private void initializeBiomeMelody(net.minecraft.util.RandomSource random) {
		ArrayList<BiomeStructureMelodies.Melody> candidates =
				new ArrayList<>(BiomeStructureMelodies.prefabs());
		while (!candidates.isEmpty()) {
			BiomeStructureMelodies.Melody prefab = candidates.remove(random.nextInt(candidates.size()));
			List<CrystalElement> colors = calculateBiomeCrystalColors(prefab.notes(), random);
			if (colors == null) continue;
			biomeMelody.clear();
			biomeMelody.addAll(prefab.notes());
			biomePlaySpeed = prefab.playbackRate();
			for (int i = 0; i < biomeCrystalColors.length; i++)
				biomeCrystalColors[i] = colors.get(i);
			resetBiomeGuess();
			biomeMusicTick = 0;
			biomeNextNoteTick = 10;
			// V33a starts at zero, automatically demonstrating the melody to the first nearby player.
			biomeMelodyIndex = 0;
			biomeMusicCooldown = 0;
			biomeComplete = false;
			return;
		}
		throw new IllegalStateException("No V33a Biome Fragment melody can be played with eight crystals");
	}

	private static @Nullable List<CrystalElement> calculateBiomeCrystalColors(List<MusicKey> melody,
			net.minecraft.util.RandomSource random) {
		Set<MusicKey> needed = new HashSet<>(melody);
		needed.remove(null);
		ArrayList<CrystalElement> colors = new ArrayList<>(8);
		while (!needed.isEmpty()) {
			CrystalElement best = null;
			int bestAmount = 0;
			for (CrystalElement element : CrystalElement.elements) {
				if (colors.contains(element)) continue;
				int amount = 0;
				for (MusicKey key : CrystalMusicManager.instance.getKeys(element))
					if (needed.contains(key)) amount++;
				if (amount > bestAmount) {
					best = element;
					bestAmount = amount;
				}
			}
			if (best == null) return null;
			colors.add(best);
			needed.removeAll(CrystalMusicManager.instance.getKeys(best));
			if (colors.size() > 8) return null;
		}
		while (colors.size() < 8) {
			CrystalElement element = CrystalElement.elements[random.nextInt(CrystalElement.elements.length)];
			if (!colors.contains(element)) colors.add(element);
		}
		return colors;
	}

	private void resetBiomeGuess() {
		biomeGuessIndex = 0;
		advanceBiomeGuessPastRests();
	}

	private void advanceBiomeGuessPastRests() {
		while (biomeGuessIndex < biomeMelody.size() && biomeMelody.get(biomeGuessIndex) == null)
			biomeGuessIndex++;
	}

	@Override
	public void onMusicTrigger(BlockPos triggerPos, CrystalElement element, MusicKey key,
			@Nullable Player player) {
		if (structure != StructureType.BIOME_FRAGMENT || level == null || biomeComplete
				|| biomeMelody.isEmpty()) return;
		boolean creativeBypass = player != null && ProgressStage.CTM.isPlayerAtStage(player);
		MusicKey expected = biomeGuessIndex < biomeMelody.size() ? biomeMelody.get(biomeGuessIndex) : null;
		if (creativeBypass || key == expected) {
			biomeGuessIndex++;
			advanceBiomeGuessPastRests();
			if (biomeGuessIndex >= biomeMelody.size()) completeBiomePuzzle(player);
			else setChanged();
		}
		else {
			resetBiomeGuess();
			ChromaSounds.ERROR.playSoundAtBlock(level, worldPosition);
			setChanged();
		}
	}

	/** Replays the source melody unless a playback or its forty-tick cooldown is active. */
	public void triggerBiomeMelody() {
		if (structure != StructureType.BIOME_FRAGMENT || biomeMelodyIndex >= 0
				|| biomeMusicCooldown > 0 || biomeMelody.isEmpty()) return;
		biomeNextNoteTick = biomeMusicTick;
		biomeMelodyIndex = 0;
		setChanged();
	}

	private void tickBiomeMusic() {
		if (level == null || !biomePuzzleReady || biomeMelody.isEmpty()) return;
		boolean playerNearby = level.players().stream().anyMatch(player -> player.distanceToSqr(
				worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
				worldPosition.getZ() + 0.5) <= 400);
		if (!playerNearby) return;
		if (biomeMusicCooldown > 0) {
			biomeMusicCooldown--;
			setChanged();
		}
		if (biomeMelodyIndex < 0) return;
		biomeMusicTick++;
		if (biomeMusicTick >= biomeNextNoteTick) playNextBiomeNote();
	}

	private void playNextBiomeNote() {
		MusicKey key = biomeMelody.get(biomeMelodyIndex);
		if (key != null) {
			ChromaSounds.DING.playSoundAtBlock(level, worldPosition, 1,
					(float)CrystalMusicManager.instance.getPitchFactor(key));
			if (level instanceof ServerLevel server) {
				Set<CrystalElement> colors = CrystalMusicManager.instance.getColorsWithKey(key);
				double radius = 4.5 - 0.5 * (colors.size() - 1);
				for (CrystalElement element : colors)
					server.sendParticles(new DustParticleOptions(element.getColor(), 1F),
							worldPosition.getX() + 0.5, worldPosition.getY() + 3.5,
							worldPosition.getZ() + 0.5, 6, radius / 6, radius / 6,
							radius / 6, 0);
			}
		}
		biomeNextNoteTick += biomePlaySpeed;
		biomeMelodyIndex++;
		if (biomeMelodyIndex >= biomeMelody.size()) {
			biomeMelodyIndex = -1;
			biomeNextNoteTick = 0;
			biomeMusicCooldown = 40;
		}
		setChanged();
	}

	private void completeBiomePuzzle(@Nullable Player player) {
		biomeComplete = true;
		biomeGuessIndex = biomeMelody.size();
		ChromaSounds.CAST.playSoundAtBlock(level, worldPosition);
		for (BlockPos pos : biomeBarrierLocations()) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(pos, state.setValue(BlockChromaDoor.OPEN, true), 3);
		}
		for (BlockPos pos : biomeLowerChestLocations())
			if (level.getBlockEntity(pos) instanceof TileEntityLootChest chest)
				chest.setStructureLocked(false);
		setChanged();
	}

	private List<BlockPos> biomeLowerChestLocations() {
		return List.of(worldPosition.offset(-4, 2, -5), worldPosition.offset(5, 2, -4),
				worldPosition.offset(4, 2, 5), worldPosition.offset(-5, 2, 4));
	}

	private List<BlockPos> biomeBarrierLocations() {
		ArrayList<BlockPos> result = new ArrayList<>();
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) result.add(worldPosition.offset(x, 1, z));
		for (int y = 2; y <= 4; y++)
			for (int distance = 4; distance <= 5; distance++) {
				result.add(worldPosition.offset(distance, y, 3));
				result.add(worldPosition.offset(-3, y, distance));
				result.add(worldPosition.offset(-distance, y, -3));
				result.add(worldPosition.offset(3, y, -distance));
			}
		return result;
	}

	@Override
	public boolean canAccessLockKey(BlockPos keyPos, @Nullable Player player) {
		return structure != StructureType.BIOME_FRAGMENT || player != null
				&& ProgressStage.BIOMESTRUCT.playerHasPrerequisites(player);
	}

	@Override
	public void onLockKeyChanged(BlockPos keyPos, int channel, @Nullable CrystalElement rune,
			boolean added, @Nullable Player player) {
		if (structure == StructureType.BIOME_FRAGMENT)
			updateBiomeColorDoors(added ? null : keyPos);
	}

	@Override
	public void onLightSwitch(BlockPos switchPos, int panelLevel, int channel, boolean up,
			@Nullable Player player) {
		if (structure != StructureType.BIOME_FRAGMENT || level == null)
			return;
		if (player == null || !ProgressStage.BIOMESTRUCT.playerHasPrerequisites(player)) {
			BlockState state = level.getBlockState(switchPos);
			if (state.getBlock() instanceof BlockLightSwitch)
				level.setBlock(switchPos, state.setValue(BlockLightSwitch.UP, false), 3);
			ChromaSounds.ERROR.playSoundAtBlock(level, switchPos);
			return;
		}
		updateBiomeSwitchDoors(ProgressStage.CTM.isPlayerAtStage(player));
	}

	private void updateBiomeColorDoors() {
		updateBiomeColorDoors(null);
	}

	private void updateBiomeColorDoors(@Nullable BlockPos removing) {
		if (level == null || !biomePuzzleReady) return;
		java.util.EnumSet<CrystalElement> open = java.util.EnumSet.noneOf(CrystalElement.class);
		int[][] runes = {{5,5,6},{6,5,5},{-5,5,6},{-6,5,5},
				{5,5,-6},{6,5,-5},{-5,5,-6},{-6,5,-5}};
		for (int[] offset : runes) {
			BlockPos runePos = worldPosition.offset(offset[0], offset[1], offset[2]);
			BlockState rune = level.getBlockState(runePos);
			if (rune.getBlock() instanceof reika.chromaticraft.block.BlockCrystalRune crystal
					&& !runePos.above().equals(removing)
					&& level.getBlockState(runePos.above()).is(ChromaBlocks.LOCK_KEY.get()))
				open.add(crystal.getColor());
		}
		for (int door = 0; door < 4; door++)
			for (BlockPos pos : biomeColorDoorLocations(door))
				if (level.getBlockEntity(pos) instanceof TileEntityColorLock lock)
					lock.setOpenColors(open);
	}

	private void updateBiomeSwitchDoors(boolean forceOpen) {
		for (int door = 0; door < 4; door++) {
			boolean open = forceOpen || currentBiomeSwitchMask() == biomeDoorMasks[door];
			for (BlockPos pos : biomeSwitchDoorLocations(door))
				BlockShiftLock.setOpen(level, pos, open);
			BlockPos indicator = biomeSwitchPosition(door);
			BlockLightPanel.activate(level, indicator.above(), !open);
			BlockLightPanel.activate(level, indicator.below(), open);
		}
	}

	private int currentBiomeSwitchMask() {
		int mask = 0;
		for (int i = 0; i < 4; i++)
			if (BlockLightSwitch.isSwitchUp(level, biomeSwitchPosition(i))) mask |= 1 << i;
		return mask;
	}

	/** NW, SW, NE, SE: the exact ordering used by V33a's four switch booleans. */
	private BlockPos biomeSwitchPosition(int index) {
		return switch (index) {
			case 0 -> worldPosition.offset(-3, 8, -3);
			case 1 -> worldPosition.offset(3, 8, -3);
			case 2 -> worldPosition.offset(-3, 8, 3);
			case 3 -> worldPosition.offset(3, 8, 3);
			default -> throw new IllegalArgumentException("Switch index " + index);
		};
	}

	private List<BlockPos> biomeColorDoorLocations(int door) {
		java.util.ArrayList<BlockPos> result = new java.util.ArrayList<>(6);
		for (int y = 2; y <= 4; y++) for (int d = 1; d <= 2; d++) result.add(switch (door) {
			case 0 -> worldPosition.offset(d, y, -3);
			case 1 -> worldPosition.offset(3, y, d);
			case 2 -> worldPosition.offset(-d, y, 3);
			case 3 -> worldPosition.offset(-3, y, -d);
			default -> throw new IllegalArgumentException("Door index " + door);
		});
		return result;
	}

	private List<BlockPos> biomeSwitchDoorLocations(int door) {
		java.util.ArrayList<BlockPos> result = new java.util.ArrayList<>(12);
		for (int y = 6; y <= 8; y++) for (int d = 5; d <= 6; d++) switch (door) {
			case 0 -> { result.add(worldPosition.offset(-2, y, -d)); result.add(worldPosition.offset(-d, y, -2)); }
			case 1 -> { result.add(worldPosition.offset(-2, y, d)); result.add(worldPosition.offset(-d, y, 2)); }
			case 2 -> { result.add(worldPosition.offset(2, y, -d)); result.add(worldPosition.offset(d, y, -2)); }
			case 3 -> { result.add(worldPosition.offset(2, y, d)); result.add(worldPosition.offset(d, y, 2)); }
			default -> throw new IllegalArgumentException("Door index " + door);
		};
		return result;
	}

	/**
	 * Adds V33a's information fragment reward after the library loot has been rolled.
	 *
	 * <p>Upstream adds these through {@code ReikaInventoryHelper.addToIInv}, which merges into a
	 * matching stack or takes the first free slot and simply returns false when there is neither. A
	 * full chest therefore dropped the reward in V33a too, and this matches that rather than treating
	 * it as impossible — twenty-seven slots is not much once the vanilla stronghold library table has
	 * rolled and ChromaChests' injections have been added on top, and a hard failure here would abort
	 * chunk generation over loot.
	 */
	public void addReward(ItemStack reward) {
		if (reward.isEmpty())
			return;
		this.unpackLootTable(null);
		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack present = items.get(slot);
			if (present.isEmpty()) {
				items.set(slot, reward.copy());
				setChanged();
				return;
			}
			if (ItemStack.isSameItemSameComponents(present, reward)
					&& present.getCount() + reward.getCount() <= present.getMaxStackSize()) {
				present.grow(reward.getCount());
				setChanged();
				return;
			}
		}
		reika.chromaticraft.ChromatiCraft.LOGGER.warn(
				"Structure controller at {} had no room for its {} reward; the rolled loot filled all {} slots",
				worldPosition, reward, items.size());
	}

	private AABB proximityBox() {
		AABB block = new AABB(worldPosition);
		return block.move(structure.offsetX, structure.offsetY, structure.offsetZ)
				.inflate(structure.radiusX, structure.radiusY, structure.radiusZ);
	}

	private void triggerProximity(Player player) {
		if (structure == null || triggered || level == null)
			return;
		switch (structure) {
			case CAVERN -> closeCavernEntrance();
			case BURROW -> crack(worldPosition.offset(2, 1, 0), ChromaShieldTypes.CRACK);
			case OCEAN -> crackOceanCover();
			case DESERT -> openDesert();
			case SNOW -> openSnow();
			case BIOME_FRAGMENT -> { /* Puzzle data owns its entrance transitions. */ }
		}
		ProgressStage.ANYSTRUCT.giveToPlayer(player, true);
		structure.progress.giveToPlayer(player, true);
		lastTriggerPlayer = player.getUUID();
		triggered = true;
		setChanged();
	}

	private void closeCavernEntrance() {
		BlockState cloak = ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
		level.setBlock(worldPosition.offset(7, 0, 0), cloak, 3);
		level.setBlock(worldPosition.offset(7, -1, 0), cloak, 3);
		ChromaSounds.TRAP.playSoundAtBlock(level, worldPosition);
	}

	private void crack(BlockPos pos, ChromaShieldTypes type) {
		BlockState state = ChromaBlocks.shielding(type).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
		level.setBlock(pos, state, 3);
		level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_BREAK,
				net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
	}

	private void crackOceanCover() {
		// V33a OceanStructure.getCovers: the two 5x3 ceiling panels over the long arms.
		for (int x = -1; x <= 1; x++)
			for (int z = 13; z <= 17; z++)
				crack(worldPosition.offset(x, 2, z), ChromaShieldTypes.CRACKS);
		for (int x = 13; x <= 17; x++)
			for (int z = -1; z <= 1; z++)
				crack(worldPosition.offset(x, 2, z), ChromaShieldTypes.CRACKS);
		ChromaSounds.TRAP.playSoundAtBlock(level, worldPosition);
	}

	private void openDesert() {
		// V33a controller-relative corner caps and the four concealed lower columns.
		int[][] caps = {{-2, 6, -2}, {-2, 6, 2}, {2, 6, -2}, {2, 6, 2}};
		for (int[] p : caps)
			level.setBlock(worldPosition.offset(p[0], p[1], p[2]),
					ChromaBlocks.shielding(ChromaShieldTypes.STONE).get().defaultBlockState()
							.setValue(BlockStructureShield.REINFORCED, true), 3);
		int[][] lower = {{-2, 2, -2}, {-2, 2, 2}, {2, 2, -2}, {2, 2, 2}};
		for (int[] p : lower)
			level.setBlock(worldPosition.offset(p[0], p[1], p[2]),
					ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState()
							.setValue(BlockStructureShield.REINFORCED, true), 3);

		// The way in. V33a cracks a run of shielding along all four walls, which is the only route to
		// the lower chamber -- without these the desert structure has no entrance at all. Upstream
		// writes them against an anchor seven west, three down and seven north of the controller;
		// these are the same blocks expressed relative to the controller itself.
		int[][] cracks = {
				{5, 2, -1}, {5, 2, 0}, {5, 2, 1}, {5, 3, 0},
				{4, 2, -1}, {4, 2, 0}, {4, 2, 1}, {1, 2, 4},
				{1, 2, 5}, {1, 2, -5}, {1, 2, -4}, {0, 2, 4},
				{0, 2, 5}, {-5, 2, -1}, {-5, 2, 0}, {-5, 2, 1},
				{-5, 3, 0}, {-4, 2, -1}, {-4, 2, 0}, {-4, 2, 1},
				{-4, 3, 0}, {-1, 2, -5}, {-1, 2, -4}, {-1, 2, 4},
				{-1, 2, 5}, {0, 2, -5}, {0, 2, -4}, {0, 3, -5},
				{0, 3, -4}, {0, 3, 4}, {0, 3, 5}, {4, 3, 0}
		};
		for (int[] p : cracks)
			crack(worldPosition.offset(p[0], p[1], p[2]), ChromaShieldTypes.CRACK);

		ChromaSounds.TRAP.playSoundAtBlock(level, worldPosition);
	}

	private void openSnow() {
		// V33a SnowStructure.getCrackToCenter, normalized from the authored (8,3,6) anchor.
		for (int x = -1; x <= 1; x++)
			for (int z = 1; z <= 4; z++)
				crack(worldPosition.offset(x, 2, z), ChromaShieldTypes.CRACKS);

		// WorldLocation.hashCode seeded this choice in V33a. The discarded long is intentional:
		// structure construction consumed the first choice before the controller replayed it.
		Random route = new Random(Objects.hash(level.dimension(), worldPosition));
		route.nextLong();
		int[][][] roofGroups = {
				{{-4,7,-1},{-4,7,0},{-4,8,-1},{-4,8,0}},
				{{-4,7,4},{-4,7,5},{-4,8,4},{-4,8,5}},
				{{-3,7,-2},{-3,8,-2},{-2,7,-2},{-2,8,-2}},
				{{-3,7,6},{-3,8,6},{-2,7,6},{-2,8,6}},
				{{2,7,-2},{2,8,-2},{3,7,-2},{3,8,-2}},
				{{2,7,6},{2,8,6},{3,7,6},{3,8,6}},
				{{4,7,-1},{4,7,0},{4,8,-1},{4,8,0}},
				{{4,7,4},{4,7,5},{4,8,4},{4,8,5}}
		};
		for (int[] cell : roofGroups[route.nextInt(roofGroups.length)])
			crack(worldPosition.offset(cell[0], cell[1], cell[2]), ChromaShieldTypes.CRACK);

		Direction direction = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)
				.get(route.nextInt(4));
		BlockShiftLock.Passability passability = BlockShiftLock.Passability.hidden(direction);
		for (int a = -1; a <= 1; a++)
			for (int y = 3; y <= 5; y++) {
				BlockPos pos = switch (direction) {
					case WEST -> worldPosition.offset(-2, y, 2 + a);
					case EAST -> worldPosition.offset(2, y, 2 + a);
					case NORTH -> worldPosition.offset(a, y, 0);
					case SOUTH -> worldPosition.offset(a, y, 4);
					default -> throw new IllegalStateException();
				};
				BlockState state = level.getBlockState(pos);
				if (state.is(ChromaBlocks.SHIFT_LOCK.get()))
					level.setBlock(pos, state.setValue(BlockShiftLock.PASSABILITY, passability), 3);
			}
		ChromaSounds.TRAP.playSoundAtBlock(level, worldPosition);
	}

	public void onHit(Player player, BlockPos hitPos) {
		if (structure == StructureType.OCEAN && (hitPos.getY() == worldPosition.getY()
				|| hitPos.getY() == worldPosition.getY() - 1)
				&& Math.abs(hitPos.getX() - worldPosition.getX()) <= 3
				&& Math.abs(hitPos.getZ() - worldPosition.getZ()) <= 3)
			triggerOceanTrap(player);
	}


	private void triggerOceanTrap(Player player) {
		if (level == null)
			return;
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				level.setBlock(worldPosition.offset(x, -3, z), Blocks.AIR.defaultBlockState(), 3);
		ChromaSounds.TRAP.playSound(player, 1, 1);
		trapTick = 40;
		setChanged();
	}

	private void resetOceanTrap() {
		BlockState cloak = ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				level.setBlock(worldPosition.offset(x, -3, z), cloak, 3);
	}

	public void reopenStructure() {
		if (level == null || structure == null)
			return;
		if (structure == StructureType.CAVERN) {
			level.setBlock(worldPosition.offset(7, 0, 0), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(worldPosition.offset(7, -1, 0), Blocks.AIR.defaultBlockState(), 3);
		}
		else if (structure != StructureType.DESERT)
			return;
		triggered = false;
		lastTriggerPlayer = null;
		setChanged();
	}

	/**
	 * V33a {@code TileEntityStructControl.breakBlock}: destroying the controller unseals the whole
	 * structure. Upstream walks its block array rewriting every shield and loot chest to
	 * {@code meta % 8} -- dropping metadata bit 3, the reinforced flag -- so the shell becomes ordinary
	 * mineable material.
	 *
	 * <p>That is what makes the reward reachable: the loot chests themselves are open to anyone who has
	 * not claimed them, and it is the reinforced shield capping each one that actually holds them shut.
	 * Without this the structure stays sealed forever and the chests can never be opened, which is
	 * exactly what happened before it was ported.
	 *
	 * <p>The sweep is over the structure's own bounds rather than a stored array: the port builds its
	 * structures from NBT templates and keeps no block list, and clearing a flag on blocks that are
	 * already unreinforced costs nothing.
	 */
	public void onControllerBroken() {
		if (level == null || level.isClientSide() || structure == null)
			return;
		AABB box = proximityBox().inflate(2);
		BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof BlockStructureShield
					&& state.getValue(BlockStructureShield.REINFORCED))
				level.setBlock(pos, state.setValue(BlockStructureShield.REINFORCED, false), 3);
		}
		ChromaSounds.POWERDOWN.playSoundAtBlock(level, worldPosition);
	}

	public boolean canBreak(Player player) {
		if (structure == StructureType.OCEAN)
			return player != null && player.distanceToSqr(worldPosition.getX() + 0.5,
					worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 2.5 * 2.5;
		// Door-state checks remain attached to the optional Burrow room blocks when that template lands.
		return structure != StructureType.BURROW || (!furnaceRoom && !lootRoom);
	}

	/** V33a setMonument: marks this controller as the monument's, and syncs that to clients. */
	public void setMonument() {
		isMonument = true;
		this.setChanged();
	}

	public boolean isMonument() {
		return isMonument;
	}

	/**
	 * V33a triggerMonument. The checks run before anything is announced, so a monument that is not ready
	 * makes no sound and leaves no state behind.
	 */
	public boolean triggerMonument(Player ep) {
		Level world = this.getLevel();
		if (world == null || world.isClientSide())
			return false;
		triggeredMonument = true;
		monument = new reika.chromaticraft.magic.MonumentCompletionRitual(world, this.getBlockPos(), ep);
		if (monument.doChecks()) {
			monument.start();
			if (world instanceof net.minecraft.server.level.ServerLevel server)
				reika.chromaticraft.network.ChromaNetwork.sendMonumentRitualState(server,
						this.getBlockPos(), true,
						world.dimension() == reika.chromaticraft.registry.ChromaDimensions.PROXIMA);
			this.setChanged();
			return true;
		}
		// doChecks has already ended it; drop the object so a failed attempt can be retried cleanly.
		triggeredMonument = false;
		monument = null;
		return false;
	}

	public void endMonumentRitual() {
		if (!triggeredMonument)
			return;
		triggeredMonument = false;
		if (monument != null && monument.isRunning())
			monument.endRitual();
		monument = null;
		if (this.getLevel() instanceof net.minecraft.server.level.ServerLevel server)
			reika.chromaticraft.network.ChromaNetwork.sendMonumentRitualState(server,
					this.getBlockPos(), false, false);
		this.setChanged();
	}

	public boolean isTriggerPlayer(Player player) {
		return player != null && player.getUUID().equals(lastTriggerPlayer);
	}

	public @Nullable StructureType getStructureType() {
		return structure;
	}

	public CrystalElement getColor() {
		return color;
	}

	public boolean wasTriggered() {
		return triggered;
	}

	public boolean generationErrored() {
		return generationErrored;
	}

	public void markGenerationErrored() {
		generationErrored = true;
		setChanged();
	}

	@Override public int getContainerSize() { return SIZE; }
	@Override protected NonNullList<ItemStack> getItems() { return items; }
	@Override protected void setItems(NonNullList<ItemStack> list) { items = list; }
	@Override protected Component getDefaultName() { return Component.literal("Structure Controller"); }
	@Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory, this);
	}
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

	@Override
	public void onLoad() {
		super.onLoad();
		if (level == null || level.isClientSide() || structure != StructureType.BIOME_FRAGMENT) return;
		boolean incompleteState = !biomePuzzleReady || biomeMelody.isEmpty()
				|| java.util.Arrays.stream(biomeDoorColors).anyMatch(Objects::isNull)
				|| java.util.Arrays.stream(biomeRuneColors).anyMatch(Objects::isNull)
				|| java.util.Arrays.stream(biomeCrystalColors).anyMatch(Objects::isNull);
		if (incompleteState)
			initializeBiomePuzzle(net.minecraft.util.RandomSource.create(
					Objects.hash(level.dimension(), worldPosition, structureVersion)));
		else
			bindBiomePuzzleBlocks();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output))
			ContainerHelper.saveAllItems(output, items);
		if (structure != null)
			output.putString("structure", structure.serializedName());
		output.putString("color", color.name());
		output.putBoolean("triggered", triggered);
		output.putBoolean("regenerated", regenerated);
		output.putInt("structureVersion", structureVersion);
		output.putInt("trapTick", trapTick);
		output.putBoolean("furnaceRoom", furnaceRoom);
		output.putBoolean("lootRoom", lootRoom);
		output.putBoolean("generationErrored", generationErrored);
		if (lastTriggerPlayer != null)
			output.putString("lastTriggerPlayer", lastTriggerPlayer.toString());
		output.putBoolean("monument", isMonument);
		output.putBoolean("monument_t", triggeredMonument);
		output.putBoolean("biomePuzzleReady", biomePuzzleReady);
		output.putInt("biomeKeyChannel", biomeKeyChannel);
		ValueOutput.TypedOutputList<Integer> masks = output.list("biomeDoorMasks", com.mojang.serialization.Codec.INT);
		for (int mask : biomeDoorMasks) masks.add(mask);
		ValueOutput.TypedOutputList<String> colors = output.list("biomeDoorColors", com.mojang.serialization.Codec.STRING);
		for (CrystalElement element : biomeDoorColors) if (element != null) colors.add(element.name());
		ValueOutput.TypedOutputList<String> runes = output.list("biomeRuneColors", com.mojang.serialization.Codec.STRING);
		for (CrystalElement element : biomeRuneColors) if (element != null) runes.add(element.name());
		ValueOutput.TypedOutputList<String> crystals = output.list("biomeCrystalColors", com.mojang.serialization.Codec.STRING);
		for (CrystalElement element : biomeCrystalColors) if (element != null) crystals.add(element.name());
		ValueOutput.TypedOutputList<Integer> melody = output.list("biomeMelody", com.mojang.serialization.Codec.INT);
		for (MusicKey key : biomeMelody) melody.add(key == null ? -1 : key.ordinal());
		output.putInt("biomePlaySpeed", biomePlaySpeed);
		output.putInt("biomeGuessIndex", biomeGuessIndex);
		output.putLong("biomeMusicTick", biomeMusicTick);
		output.putLong("biomeNextNoteTick", biomeNextNoteTick);
		output.putInt("biomeMelodyIndex", biomeMelodyIndex);
		output.putInt("biomeMusicCooldown", biomeMusicCooldown);
		output.putBoolean("biomeComplete", biomeComplete);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input))
			ContainerHelper.loadAllItems(input, items);
		structure = input.getString("structure").map(name -> {
			try { return StructureType.valueOf(name.toUpperCase(Locale.ROOT)); }
			catch (IllegalArgumentException ignored) { return null; }
		}).orElse(null);
		color = input.getString("color").map(name -> {
			try { return CrystalElement.valueOf(name); }
			catch (IllegalArgumentException ignored) { return CrystalElement.WHITE; }
		}).orElse(CrystalElement.WHITE);
		triggered = input.getBooleanOr("triggered", false);
		regenerated = input.getBooleanOr("regenerated", false);
		structureVersion = input.getIntOr("structureVersion", 0);
		trapTick = input.getIntOr("trapTick", 0);
		furnaceRoom = input.getBooleanOr("furnaceRoom", false);
		lootRoom = input.getBooleanOr("lootRoom", false);
		generationErrored = input.getBooleanOr("generationErrored", false);
		lastTriggerPlayer = input.getString("lastTriggerPlayer").flatMap(name -> {
			try { return java.util.Optional.of(UUID.fromString(name)); }
			catch (IllegalArgumentException ignored) { return java.util.Optional.empty(); }
		}).orElse(null);
		isMonument = input.getBooleanOr("monument", false);
		// The ritual object itself is not persisted: a ritual interrupted by a save or a restart is over,
		// and upstream reconstructs one from scratch on the next trigger.
		triggeredMonument = input.getBooleanOr("monument_t", false);
		biomePuzzleReady = input.getBooleanOr("biomePuzzleReady", false);
		biomeKeyChannel = Math.clamp(input.getIntOr("biomeKeyChannel", 0), 0, 7);
		int i = 0;
		for (int mask : input.listOrEmpty("biomeDoorMasks", com.mojang.serialization.Codec.INT))
			if (i < biomeDoorMasks.length) biomeDoorMasks[i++] = mask & 15;
		i = 0;
		for (String name : input.listOrEmpty("biomeDoorColors", com.mojang.serialization.Codec.STRING)) {
			if (i >= biomeDoorColors.length) break;
			try { biomeDoorColors[i++] = CrystalElement.valueOf(name); }
			catch (IllegalArgumentException ignored) { }
		}
		loadElements(input, "biomeRuneColors", biomeRuneColors);
		loadElements(input, "biomeCrystalColors", biomeCrystalColors);
		biomeMelody.clear();
		for (int ordinal : input.listOrEmpty("biomeMelody", com.mojang.serialization.Codec.INT))
			biomeMelody.add(MusicKey.getByIndex(ordinal));
		biomePlaySpeed = Math.max(1, input.getIntOr("biomePlaySpeed", 8));
		biomeGuessIndex = Math.clamp(input.getIntOr("biomeGuessIndex", 0), 0, biomeMelody.size());
		biomeMusicTick = Math.max(0, input.getLongOr("biomeMusicTick", 0));
		biomeNextNoteTick = Math.max(0, input.getLongOr("biomeNextNoteTick", 10));
		biomeMelodyIndex = Math.clamp(input.getIntOr("biomeMelodyIndex", -1), -1,
				Math.max(-1, biomeMelody.size() - 1));
		biomeMusicCooldown = Math.max(0, input.getIntOr("biomeMusicCooldown", 0));
		biomeComplete = input.getBooleanOr("biomeComplete", false);
		advanceBiomeGuessPastRests();
	}

	private static void loadElements(ValueInput input, String key, CrystalElement[] target) {
		int index = 0;
		for (String name : input.listOrEmpty(key, com.mojang.serialization.Codec.STRING)) {
			if (index >= target.length) break;
			try { target[index++] = CrystalElement.valueOf(name); }
			catch (IllegalArgumentException ignored) { }
		}
	}
}
