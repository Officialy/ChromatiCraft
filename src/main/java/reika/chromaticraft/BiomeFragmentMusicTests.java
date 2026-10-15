package reika.chromaticraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.world.OverworldStructureFeature;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/**
 * The Biome Fragment's melody puzzle played the way a player plays it: the placed structure, its own
 * demonstration and replays, and real clicks on its eight authored Music Triggers.
 */
final class BiomeFragmentMusicTests {

	/** V33a crystal positions, in the controller's colour order; each stands on its trigger. */
	private static final int[][] CRYSTALS = {{-4,3,1},{-5,3,1},{-1,3,-4},{-1,3,-5},
			{4,3,-1},{5,3,-1},{1,3,4},{1,3,5}};
	/** The trigger face each room exposes to the player: west room, north, east, south. */
	private static final Direction[] FACES = {Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST,
			Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST};

	private record Note(int crystal, int quadrant, MusicKey key) {
	}

	static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
		// The demonstration runs three times at up to ~400 ticks each for the longest prefab.
		ChromaGameTests.register(event, env, "biome_fragment_music_full_flow", 1800,
				BiomeFragmentMusicTests::fullFlow);
	}

	private static void fullFlow(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(12, 6, 12));
		helper.assertTrue(new OverworldStructureFeature(OverworldStructureFeature.Type.BIOME_FRAGMENT, false)
				.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
						RandomSource.create(0x5EA1EDL), origin),
				"the command Biome Fragment feature must place the structure");
		TileEntityStructureController controller =
				(TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller != null, "the placed Biome Fragment must have its controller");
		List<MusicKey> melody = controller.getBiomeMelody();
		int firstNote = indexOfFirstNote(melody);

		// Every authored trigger must be playable: crystal above, reinforced shielding below.
		List<Note> playable = new ArrayList<>();
		for (int i = 0; i < CRYSTALS.length; i++) {
			BlockPos crystal = origin.offset(CRYSTALS[i][0], CRYSTALS[i][1], CRYSTALS[i][2]);
			CrystalElement color = controller.getBiomeCrystalColor(i);
			helper.assertTrue(helper.getLevel().getBlockState(crystal).is(ChromaBlocks.crystalLamp(color).get()),
					"crystal " + i + " must be the controller's " + color + " lamp");
			helper.assertTrue(helper.getLevel().getBlockState(crystal.below()).is(ChromaBlocks.MUSIC_TRIGGER.get()),
					"crystal " + i + " must stand on a Music Trigger");
			BlockState shield = helper.getLevel().getBlockState(crystal.below(2));
			helper.assertTrue(shield.getBlock() instanceof BlockStructureShield
							&& shield.getValue(BlockStructureShield.REINFORCED),
					"trigger " + i + " must stand on reinforced shielding, V33a's routing gate");
			for (int q = 0; q < 4; q++)
				playable.add(new Note(i, q, CrystalMusicManager.instance.getKeys(color).get(q)));
		}

		// Two-octave LO/HI recordings keep every note of the melody inside the client's 0.5-2.0 clamp.
		for (MusicKey key : melody) {
			if (key == null) continue;
			ChromaSounds.PitchedSound sent = ChromaSounds.resolvePitch(ChromaSounds.DING,
					(float)CrystalMusicManager.instance.getPitchFactor(key));
			helper.assertTrue(sent.pitch() >= 0.5F && sent.pitch() <= 2F,
					key + " must be sent at an audible pitch, got " + sent.pitch());
		}
		ChromaSounds.PitchedSound whiteOctave = ChromaSounds.resolvePitch(ChromaSounds.DING,
				CrystalMusicManager.instance.getScaledDing(CrystalElement.WHITE, 3));
		helper.assertTrue(whiteOctave.sound() == ChromaSounds.DING.getUpshiftedPitch()
						&& whiteOctave.sound() != ChromaSounds.DING
						&& Math.abs(whiteOctave.pitch() - 1F) < 1e-3F,
				"C7 (white's octave) must play ding2_hi at pitch 1, not DING clamped to 2");

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(Vec3.atCenterOf(origin.offset(0, 4, 10)));
		helper.assertTrue(helper.getLevel().players().contains(player),
				"the mock player must be a real level player for the 20-block checks");
		helper.assertTrue(controller.isBiomeMelodyPlaying(),
				"a freshly generated fragment must be waiting to demonstrate its melody");

		helper.startSequence()
				// 1. The automatic demonstration plays through, then its forty-tick cooldown runs out.
				.thenWaitUntil(() -> helper.assertTrue(!controller.isBiomeMelodyPlaying()
						&& controller.getBiomeMusicCooldown() == 0, "the demonstration must finish"))
				// 2. The upper pedestal replays it.
				.thenExecute(() -> {
					BlockPos pedestal = origin.offset(0, 10, 0);
					helper.assertTrue(helper.getLevel().getBlockState(pedestal).is(ChromaBlocks.BIOME_REPLAY.get()),
							"the replay pedestal must sit ten above the controller");
					click(helper, player, new BlockHitResult(Vec3.atCenterOf(pedestal).add(0, 0.5, 0),
							Direction.UP, pedestal, false));
					helper.assertTrue(controller.isBiomeMelodyPlaying(), "the pedestal must replay the melody");
				})
				.thenWaitUntil(() -> helper.assertTrue(!controller.isBiomeMelodyPlaying()
						&& controller.getBiomeMusicCooldown() == 0, "the pedestal replay must finish"))
				// 3. So does holding an Examination Crystal near the structure.
				.thenExecute(() -> player.setItemInHand(InteractionHand.MAIN_HAND,
						new ItemStack(ChromaItems.PROBE.get())))
				.thenWaitUntil(() -> helper.assertTrue(controller.isBiomeMelodyPlaying(),
						"holding an Examination Crystal must replay the melody"))
				.thenExecute(() -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY))
				// 4. A player without the prerequisites cannot sink down the shaft.
				.thenExecute(() -> {
					player.snapTo(Vec3.atCenterOf(origin.offset(0, 6, 0)));
					player.setDeltaMovement(0, -0.6, 0);
				})
				.thenExecuteAfter(2, () -> {
					helper.assertTrue(player.getDeltaMovement().y >= 0,
							"the shaft must cancel the downward motion of a player without the prerequisites");
					player.snapTo(Vec3.atCenterOf(origin.offset(0, 4, 10)));
				})
				// 5. Play part of the melody, then a wrong note: the guess restarts from the first note.
				.thenExecute(() -> {
					List<MusicKey> notes = melody.stream().filter(Objects::nonNull).toList();
					int partial = Math.min(2, notes.size() - 1);
					for (int n = 0; n < partial; n++) {
						playNote(helper, player, origin, playable, notes.get(n));
						helper.assertTrue(controller.getBiomeGuessIndex() > melody.indexOf(notes.get(n)),
								"clicking the trigger for " + notes.get(n) + " must advance the guess");
					}
					MusicKey expected = notes.get(partial);
					Note wrong = playable.stream().filter(note -> note.key() != expected).findFirst().orElseThrow();
					clickTrigger(helper, player, origin, wrong);
					helper.assertTrue(controller.getBiomeGuessIndex() == firstNote,
							"a wrong note must restart the guess at the first note, got "
									+ controller.getBiomeGuessIndex());
					helper.assertTrue(!controller.isBiomeComplete(), "a wrong note must not complete the puzzle");
				})
				// 6. The whole melody, note by note, on the real triggers.
				.thenExecute(() -> {
					for (MusicKey note : melody)
						if (note != null) {
							helper.assertTrue(!controller.isBiomeComplete(),
									"the puzzle must not complete before its last note");
							playNote(helper, player, origin, playable, note);
						}
					helper.assertTrue(controller.isBiomeComplete(), "the full melody must complete the puzzle");
					assertOpened(helper, origin);
				})
				// 7. Nothing closes again afterwards.
				.thenExecuteAfter(20, () -> assertOpened(helper, origin))
				.thenSucceed();
	}

	private static void assertOpened(GameTestHelper helper, BlockPos origin) {
		for (BlockPos barrier : barrierLocations(origin)) {
			BlockState state = helper.getLevel().getBlockState(barrier);
			helper.assertTrue(state.is(ChromaBlocks.CHROMA_DOOR.get()) && state.getValue(BlockChromaDoor.OPEN)
							&& state.getCollisionShape(helper.getLevel(), barrier).isEmpty(),
					"completion must open the barrier at " + barrier.subtract(origin));
		}
		for (BlockPos cache : List.of(origin.offset(-4, 2, -5), origin.offset(5, 2, -4),
				origin.offset(4, 2, 5), origin.offset(-5, 2, 4)))
			helper.assertTrue(helper.getLevel().getBlockEntity(cache) instanceof TileEntityLootChest chest
							&& !chest.isStructureLocked(),
					"completion must unlock the lower cache at " + cache.subtract(origin));
	}

	/** V33a {@code getBarrierLocations}: the central floor and the four lower cache walls. */
	private static List<BlockPos> barrierLocations(BlockPos origin) {
		List<BlockPos> result = new ArrayList<>();
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) result.add(origin.offset(x, 1, z));
		for (int y = 2; y <= 4; y++)
			for (int d = 4; d <= 5; d++) {
				result.add(origin.offset(d, y, 3));
				result.add(origin.offset(-3, y, d));
				result.add(origin.offset(-d, y, -3));
				result.add(origin.offset(3, y, -d));
			}
		return result;
	}

	private static int indexOfFirstNote(List<MusicKey> melody) {
		for (int i = 0; i < melody.size(); i++)
			if (melody.get(i) != null) return i;
		throw new IllegalStateException("melody has no notes");
	}

	private static void playNote(GameTestHelper helper, ServerPlayer player, BlockPos origin,
			List<Note> playable, MusicKey key) {
		Note note = playable.stream().filter(candidate -> candidate.key() == key).findFirst().orElse(null);
		helper.assertTrue(note != null, "no generated crystal can play " + key);
		clickTrigger(helper, player, origin, note);
	}

	/**
	 * Clicks the quadrant the way the player sees the face: 0 top-left, 1 top-right, 2 bottom-right,
	 * 3 bottom-left, left and right as seen standing in front of it.
	 */
	private static void clickTrigger(GameTestHelper helper, ServerPlayer player, BlockPos origin, Note note) {
		int[] at = CRYSTALS[note.crystal()];
		BlockPos trigger = origin.offset(at[0], at[1] - 1, at[2]);
		Direction face = FACES[note.crystal()];
		helper.assertTrue(helper.getLevel().getBlockState(trigger.relative(face)).isAir(),
				"trigger " + note.crystal() + " must expose its " + face + " face to the room");
		double fromLeft = note.quadrant() == 0 || note.quadrant() == 3 ? 0.25 : 0.75;
		double height = note.quadrant() <= 1 ? 0.75 : 0.25;
		Direction left = face.getOpposite().getCounterClockWise();
		double along = left.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - fromLeft : fromLeft;
		double out = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
		Vec3 local = face.getAxis() == Direction.Axis.X
				? new Vec3(out, height, along) : new Vec3(along, height, out);
		click(helper, player, new BlockHitResult(Vec3.atLowerCornerOf(trigger).add(local), face, trigger, false));
	}

	/** The server's own right-click path, as a client's use-item-on packet reaches it. */
	private static void click(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
		helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(),
						player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit)
						.consumesAction(),
				"the click on " + hit.getBlockPos() + " must be consumed");
	}
}
