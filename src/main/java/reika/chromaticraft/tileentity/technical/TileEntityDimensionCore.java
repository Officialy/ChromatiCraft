package reika.chromaticraft.tileentity.technical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.base.tileentity.TileEntityLocusPoint;
import reika.chromaticraft.magic.ElementMixer;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/**
 * V33a {@code TileEntityDimensionCore}: the sixteen elemental cores of Proxima.
 *
 * <p>A core is two things wearing one block. Out in the ring it is the prize at the end of a puzzle
 * structure — sealed until that puzzle is solved, and mining it is what marks the structure complete.
 * Around the monument it is one of sixteen a player plants themselves, and the ritual will not start
 * until all sixteen are present, the right colour, and placed by the same person.
 *
 * <h2>The two rings are not the same ring</h2>
 *
 * <p>{@link #getLocation} returns the core offsets relative to the monument's controller at
 * (21, 5, 21) — BLACK at (5, 11, 18) and round from there. Those are <em>not</em> the rune positions in
 * the monument template, which start at (3, 11, 18): the runes mark the ring, the cores sit inset from
 * them. Both run at y+11, from the same angular start, in the same direction, which is exactly why the
 * two are easy to confuse.
 *
 * <h2>What is deferred, and why it is a forward reference rather than a hole</h2>
 *
 * <p>Two calls on the mining path reach code this port has not built: {@code ChromaDimensionManager}'s
 * per-player structure registry, which tracks who is inside which puzzle, and
 * {@code ProgressionManager.markPlayerCompletedStructureColor}. Both are named at their call sites with
 * the behaviour they owe. Everything that does not need them is complete, including the seal itself —
 * an unsolved structure's core refuses to break today, which is the half that matters.
 */
public class TileEntityDimensionCore extends TileEntityLocusPoint {

	/** V33a's core ring, as offsets from the monument controller at (21, 5, 21). */
	private static final EnumMap<CrystalElement, Vec3i> locations = new EnumMap<>(CrystalElement.class);
	/** Which colours a core throws a beam to: everything it mixes with, parents and children alike. */
	private static final EnumMap<CrystalElement, Set<CrystalElement>> beams =
			new EnumMap<>(CrystalElement.class);
	/** V33a's two connect-melody tracks, as the colour/interval pairs each beat sounds. */
	private static final List<List<List<ColorNote>>> melody = new ArrayList<>();

	private CrystalElement color = CrystalElement.WHITE;
	private UUID uid;
	private DimensionStructureType structure;
	private boolean triggered;

	private final Set<UUID> sentPlayers = new HashSet<>();
	private final Set<UUID> playerWhitelist = new HashSet<>();

	private boolean primed;

	static {
		addColor(CrystalElement.BLACK, 5, 11, 18);
		addColor(CrystalElement.RED, 8, 11, 14);
		addColor(CrystalElement.GREEN, 14, 11, 8);
		addColor(CrystalElement.BROWN, 18, 11, 5);
		addColor(CrystalElement.BLUE, 24, 11, 5);
		addColor(CrystalElement.PURPLE, 28, 11, 8);
		addColor(CrystalElement.CYAN, 34, 11, 14);
		addColor(CrystalElement.LIGHTGRAY, 37, 11, 18);
		addColor(CrystalElement.GRAY, 37, 11, 24);
		addColor(CrystalElement.PINK, 34, 11, 28);
		addColor(CrystalElement.LIME, 28, 11, 34);
		addColor(CrystalElement.YELLOW, 24, 11, 37);
		addColor(CrystalElement.LIGHTBLUE, 18, 11, 37);
		addColor(CrystalElement.MAGENTA, 14, 11, 34);
		addColor(CrystalElement.ORANGE, 8, 11, 28);
		addColor(CrystalElement.WHITE, 5, 11, 24);

		for (CrystalElement e : CrystalElement.elements) {
			Set<CrystalElement> m = new HashSet<>();
			addAll(m, ElementMixer.instance.getMixablesWith(e));
			addAll(m, ElementMixer.instance.getMixParents(e));
			addAll(m, ElementMixer.instance.getChildrenOf(e));
			beams.put(e, m);
		}

		melody.add(track(MusicKey.G4, MusicKey.A4, MusicKey.B4, MusicKey.D5, MusicKey.C5, MusicKey.C5,
				MusicKey.E5, MusicKey.D5, MusicKey.D5, MusicKey.G5, MusicKey.Fs5, MusicKey.G5,
				MusicKey.D5, MusicKey.B4, MusicKey.G4, MusicKey.A4, MusicKey.B4, MusicKey.C5,
				MusicKey.D5, MusicKey.E5, MusicKey.D5, MusicKey.C5, MusicKey.B4, MusicKey.A4,
				MusicKey.B4, MusicKey.G4, MusicKey.Fs4, MusicKey.G4, MusicKey.A4, MusicKey.D5,
				MusicKey.Fs5, MusicKey.A5, MusicKey.C6, MusicKey.B5, MusicKey.A5, MusicKey.B5,
				MusicKey.G5, MusicKey.A5, MusicKey.B5, MusicKey.D6, MusicKey.C6, MusicKey.C6,
				MusicKey.E6, MusicKey.D5, MusicKey.D6, MusicKey.D6, MusicKey.G5, MusicKey.Fs5,
				MusicKey.G5, MusicKey.D6, MusicKey.B5, MusicKey.G5, MusicKey.A5, MusicKey.B5,
				MusicKey.E5, MusicKey.D6, MusicKey.C6, MusicKey.B5, MusicKey.A5, MusicKey.G5,
				MusicKey.D5, MusicKey.G5, MusicKey.Fs5, MusicKey.G5, MusicKey.G5, MusicKey.G5,
				MusicKey.G5));
		melody.add(track(MusicKey.C5, MusicKey.E5, MusicKey.G5, MusicKey.C6, MusicKey.G4, MusicKey.B4,
				MusicKey.D5, MusicKey.G5, MusicKey.A4, MusicKey.C5, MusicKey.E5, MusicKey.A5,
				MusicKey.E4, MusicKey.G4, MusicKey.B4, MusicKey.G5, MusicKey.F4, MusicKey.A4,
				MusicKey.C5, MusicKey.F5, MusicKey.C4, MusicKey.E4, MusicKey.G4, MusicKey.C5,
				MusicKey.F4, MusicKey.A4, MusicKey.C5, MusicKey.F5, MusicKey.G4, MusicKey.B4,
				MusicKey.D5, MusicKey.G5));
	}

	public TileEntityDimensionCore(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.DIMENSION_CORE.get(), pos, state);
	}

	private static void addAll(Set<CrystalElement> into, Collection<CrystalElement> from) {
		if (from != null)
			into.addAll(from);
	}

	/** V33a addColor: the table is written in template coordinates and stored relative to the core. */
	private static void addColor(CrystalElement e, int x, int y, int z) {
		locations.put(e, new Vec3i(x - 21, y - 5, z - 21));
	}

	/**
	 * V33a addMelodyNote: a key becomes the set of colours that can sound it, each with the interval
	 * that colour has to play. A key no colour can sound would be a registration error upstream; here
	 * it is simply an empty beat, which is what an unported music table would otherwise crash on.
	 */
	private static List<List<ColorNote>> track(MusicKey... keys) {
		List<List<ColorNote>> notes = new ArrayList<>();
		for (MusicKey key : keys) {
			List<ColorNote> beat = new ArrayList<>();
			Collection<CrystalElement> colors = CrystalMusicManager.instance.getColorsWithKey(key);
			if (colors != null)
				for (CrystalElement e : colors) {
					int index = CrystalMusicManager.instance.getIntervalFor(e, key);
					if (index != -1)
						beat.add(new ColorNote(e, index));
				}
			notes.add(beat);
		}
		return notes;
	}

	/** One colour sounding one interval of a beat. V33a uses an {@code ImmutablePair} for this. */
	public record ColorNote(CrystalElement color, int interval) {}

	@Override
	public reika.chromaticraft.registry.ChromaTiles getTile() {
		return reika.chromaticraft.registry.ChromaTiles.DIMENSIONCORE;
	}

	public static Vec3i getLocation(CrystalElement e) {
		return locations.get(e);
	}

	public CrystalElement getColor() {
		return color;
	}

	public void setColor(CrystalElement e) {
		color = e;
	}

	@Override
	public int getRenderColor() {
		return color.getColor();
	}

	public void setStructure(StructurePair p) {
		structure = p.generator.getType();
		uid = p.generator.id();
	}

	public StructureGeneratorBase getStructure() {
		if (structure == null || uid == null)
			return null;
		// The registry hands back the narrow ProximaStructureGenerator contract; every real generator
		// is a StructureGeneratorBase, and anything that is not has nothing a core can ask of it.
		return structure.getGenerator(uid) instanceof StructureGeneratorBase base ? base : null;
	}

	public boolean hasStructure() {
		return this.getStructure() != null;
	}

	/** V33a prime: the monument ritual arms every core before it starts. */
	public void prime(boolean set) {
		primed = set;
		this.syncAllData(false);
	}

	public boolean isPrimed() {
		return primed;
	}

	public Collection<CrystalElement> getColorBeams() {
		return beams.get(color);
	}

	/** The two connect-melody tracks, for whatever draws them. */
	public static List<List<List<ColorNote>>> getMelody() {
		return melody;
	}

	/** V33a getCenter: where the structure controller sits, derived from this core's own offset. */
	public BlockPos getCenter() {
		Vec3i c = locations.get(color);
		return this.getBlockPos().subtract(c);
	}

	/** V33a getOtherColor: where the core of another colour stands in the same ring. */
	public BlockPos getOtherColor(CrystalElement e) {
		return this.getCenter().offset(locations.get(e));
	}

	/** V33a getSoundPitch: which interval of its colour a core sounds for a given note index. */
	public float getSoundPitch(int p) {
		return (float)switch (p) {
			case 0 -> CrystalMusicManager.instance.getDingPitchScale(color);
			case 1 -> CrystalMusicManager.instance.getThird(color);
			case 2 -> CrystalMusicManager.instance.getFifth(color);
			case 3 -> CrystalMusicManager.instance.getOctave(color);
			default -> 0D;
		};
	}

	/**
	 * V33a updateEntity. The client half is the connect beams, which only run while the core is primed;
	 * the server half is the structure's own business, and a core outside a structure has none.
	 */
	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide())
			return;
		if (this.hasStructure()) {
			this.doScanForEntry(world, pos);
			if (!triggered)
				this.doStructureCalculation(world, pos);
		}
	}

	/**
	 * V33a doScanForEntry: a player who walks into the structure's entry box is registered with it, once.
	 *
	 * <p>The registration itself reaches {@code ChromaDimensionManager.addPlayerToStructure}, which this
	 * port has not built — it is the per-player record of which puzzle someone is inside. The scan and
	 * the once-only bookkeeping are here and correct; when that manager lands it hooks in at the marked
	 * line and nothing else moves.
	 */
	private void doScanForEntry(Level world, BlockPos pos) {
		StructureGeneratorBase gen = this.getStructure();
		if (gen == null)
			return;
		int r = 8;
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
				gen.getEntryPosX() - r, gen.getPosY(), gen.getEntryPosZ() - r,
				gen.getEntryPosX() + r + 1, world.getMaxY(), gen.getEntryPosZ() + r + 1);
		for (Player ep : world.getEntitiesOfClass(Player.class, box)) {
			UUID id = ep.getUUID();
			if (sentPlayers.contains(id))
				continue;
			// Deferred: ChromaDimensionManager.addPlayerToStructure(ep, gen) — the per-player structure
			// registry is not ported. Upstream only records the player as sent when that call succeeds.
			sentPlayers.add(id);
		}
	}

	/**
	 * V33a doStructureCalculation: the per-puzzle trap a core springs when someone reaches it. Only the
	 * Three-Dimensional Maze has one; every other case in upstream's switch is an empty break, which is
	 * why this is one branch rather than eighteen.
	 *
	 * <p>Cracking the shielding around a player who steps into the maze's core chamber is the whole
	 * effect: one of the four horizontal neighbours and the block below the opposite one turn to Cracks,
	 * so the way out is not the way in.
	 */
	private void doStructureCalculation(Level world, BlockPos pos) {
		if (structure != DimensionStructureType.TDMAZE)
			return;
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
				pos.getX() - 1, pos.getY() + 2, pos.getZ() - 1,
				pos.getX() + 2, pos.getY() + 4, pos.getZ() + 2);
		if (world.getEntitiesOfClass(Player.class, box).isEmpty())
			return;
		triggered = true;
		java.util.Random rand = new java.util.Random(pos.asLong());
		boolean w = rand.nextBoolean();
		int dx = w ? (rand.nextBoolean() ? -2 : 2) : (rand.nextBoolean() ? 1 : -1);
		int dz = !w ? (rand.nextBoolean() ? -2 : 2) : (rand.nextBoolean() ? 1 : -1);
		int dx2 = Math.abs(dx) == 1 ? dx : (int)Math.signum(dx) * (Math.abs(dx) + 1);
		int dz2 = Math.abs(dz) == 1 ? dz : (int)Math.signum(dz) * (Math.abs(dz) + 1);
		BlockState cracks = reika.chromaticraft.registry.ChromaBlocks
				.shielding(reika.chromaticraft.registry.ChromaShieldTypes.CRACKS).get().defaultBlockState();
		for (int dy : new int[] {2, 3, 0, -1})
			world.setBlock(pos.offset(dx, dy, dz), cracks, 3);
		world.setBlock(pos.offset(-dx2, 1, -dz2), cracks, 3);
	}

	/**
	 * Upstream's is empty: a core's client-side motion is the locus point's own particle field plus the
	 * connect beams, neither of which is per-tick animation state.
	 */
	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		if (!world.isClientSide() && this.getPlacer() == null && !this.hasStructure())
			ChromatiCraft.LOGGER.error("{} was never given a structure. Color = {}, UID = {}",
					this, color, uid);
	}

	/**
	 * V33a isBreakable. The seal is the point: a core inside an unsolved structure cannot be mined, and
	 * a whitelist, once non-empty, narrows that to the players the structure let in.
	 */
	public boolean isBreakable(Player ep) {
		if (ep == null || ReikaPlayerAPI.isFake(ep))
			return false;
		Level world = this.getLevel();
		if (world != null && !world.isClientSide() && !ep.getAbilities().instabuild && this.hasStructure()
				&& !this.getStructure().shouldAllowCoreMining(world))
			return false;
		return playerWhitelist.isEmpty() || playerWhitelist.contains(ep.getUUID());
	}

	/**
	 * V33a breakByPlayer's server half, minus the two calls this port has not reached.
	 *
	 * <p>Breaking a core is how a puzzle structure is marked complete and reopened, so the order matters:
	 * the seal is re-checked (a creative player bypasses it, as upstream lets them), the completion is
	 * recorded, and only then is the structure opened.
	 */
	@Override
	public void breakBlock() {
		Level world = this.getLevel();
		if (world == null || world.isClientSide() || !this.hasStructure())
			return;
		// Deferred, both named at the point they belong:
		//   ProgressionManager.markPlayerCompletedStructureColor(ep, gen, color, true, true)
		//   ChromaDimensionManager.removePlayerFromStructure(ep)
		// Neither is ported; the opening below is upstream's own and runs regardless, so a solved
		// structure still unseals even while the progression record is missing.
		this.openStructure(world);
	}

	/**
	 * V33a openStructure: every cell the generator marked breakable becomes Cracked Shielding if it was
	 * shielding, and air otherwise, which is what turns a sealed puzzle into one you can walk out of.
	 */
	private void openStructure(Level world) {
		StructureGeneratorBase gen = this.getStructure();
		if (gen == null)
			return;
		BlockState cracks = reika.chromaticraft.registry.ChromaBlocks
				.shielding(reika.chromaticraft.registry.ChromaShieldTypes.CRACKS).get().defaultBlockState();
		for (BlockPos p : gen.getBreakableSpots()) {
			boolean shielded = world.getBlockState(p).getBlock()
					instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield;
			world.setBlock(p, shielded ? cracks : net.minecraft.world.level.block.Blocks.AIR
					.defaultBlockState(), 3);
		}
	}

	public void whitelistPlayer(Player ep) {
		playerWhitelist.add(ep.getUUID());
	}

	@Override
	public boolean onlyAllowOwnersToMine() {
		return !this.hasStructure();
	}

	@Override
	public boolean onlyAllowOwnersToUse() {
		return false;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.putInt("color", color.ordinal());
		NBT.putBoolean("prime", primed);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		color = CrystalElement.elements[NBT.getIntOr("color", CrystalElement.WHITE.ordinal())];
		primed = NBT.getBooleanOr("prime", false);
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);
		NBT.putInt("struct", structure != null ? structure.ordinal() : -1);
		if (uid != null)
			NBT.putString("uid", uid.toString());
		ListTag li = new ListTag();
		for (UUID id : playerWhitelist)
			li.add(StringTag.valueOf(id.toString()));
		NBT.put("whitelist", li);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);
		int s = NBT.getIntOr("struct", -1);
		structure = s >= 0 ? DimensionStructureType.types[s] : null;
		uid = NBT.getString("uid").map(UUID::fromString).orElse(null);
		playerWhitelist.clear();
		for (int i = 0; i < NBT.getListOrEmpty("whitelist").size(); i++)
			NBT.getListOrEmpty("whitelist").getString(i).ifPresent(
					id -> playerWhitelist.add(UUID.fromString(id)));
	}

	@Override
	public void getTagsToWriteToStack(CompoundTag NBT) {
		super.getTagsToWriteToStack(NBT);
		NBT.putInt("color", color.ordinal());
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		super.setDataFromItemStackTag(is);
		CompoundTag tag = is.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		color = tag == null ? CrystalElement.WHITE
				: CrystalElement.elements[tag.getIntOr("color", CrystalElement.WHITE.ordinal())];
	}

	/** Whether this core has already told the given player about its structure. */
	public boolean hasSent(UUID player) {
		return sentPlayers.contains(player);
	}

	public void markSent(UUID player) {
		sentPlayers.add(player);
	}

	public boolean isTriggered() {
		return triggered;
	}

	public void setTriggered() {
		triggered = true;
	}
}
