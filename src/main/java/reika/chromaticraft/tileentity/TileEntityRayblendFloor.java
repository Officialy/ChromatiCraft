package reika.chromaticraft.tileentity;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.block.dimension.structure.rayblend.BlockRayblendFloor26;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.world.dimension.structure.RayBlendLayout;

/**
 * V33a {@code TileEntityRayblendFloor} with the puzzle logic of {@code RayBlendPuzzle}, which in V33a lived in the
 * generator's memory, keyed by random UUIDs a restart lost.
 *
 * <p>Each tile stores the structure's seed and origin and its own room and cell, so any tile rebuilds the room's plan
 * ({@link RayBlendLayout#cached}). The crystals themselves are the state: a cell's crystal is whatever crystal block
 * sits on its tile, which V33a mirrored into {@code GridSlot.currentCrystal} through block updates. (V33a's generator
 * wrote the starting crystals without an update, so until a player touched them they did not count; reading the
 * world counts them from the start, as the puzzle intends.) The room's solved flag, V33a's {@code isComplete}, is
 * kept on the tile of cell (0, 0) and, as in V33a, only recomputed when a crystal is added.
 */
public final class TileEntityRayblendFloor extends BlockEntity {

	/** Block event on a cell's tile: V33a's {@code RAYBLENDPING} colour-exclusion particles; the param is the colour. */
	public static final int PING_EVENT = 1;

	private int layoutVersion = RayBlendLayout.CURRENT_VERSION;
	private int difficulty = 3;
	private long seed;
	private int originX;
	private int originZ;
	private int roomIndex = -1;
	private int cellX;
	private int cellZ;
	private boolean complete;

	public TileEntityRayblendFloor(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.RAYBLEND_FLOOR.get(), pos, state);
	}

	public void configure(int difficulty, long seed, int originX, int originZ, int room, int cellX, int cellZ) {
		configure(RayBlendLayout.CURRENT_VERSION, difficulty, seed, originX, originZ, room, cellX, cellZ);
	}

	public void configure(int version, int difficulty, long seed, int originX, int originZ, int room, int cellX, int cellZ) {
		layoutVersion = version;
		this.difficulty = difficulty;
		this.seed = seed;
		this.originX = originX;
		this.originZ = originZ;
		roomIndex = room;
		this.cellX = cellX;
		this.cellZ = cellZ;
		setChanged();
	}

	public int cellX() { return cellX; }
	public int cellZ() { return cellZ; }
	public int roomIndex() { return roomIndex; }

	public @Nullable RayBlendLayout layout() {
		return roomIndex < 0 ? null : RayBlendLayout.cached(layoutVersion, difficulty, seed, originX, originZ);
	}

	public RayBlendLayout.@Nullable Room room() {
		RayBlendLayout layout = layout();
		return layout == null || roomIndex >= layout.rooms().size() ? null : layout.rooms().get(roomIndex);
	}

	/** V33a {@code TileEntityRayblendFloor.allowsCrystalAt}: no generator or a blocked cell refuses everything. */
	public boolean allowsCrystal(CrystalElement e) {
		RayBlendLayout.Room room = room();
		if (room == null || level == null || getBlockState().getValue(BlockRayblendFloor26.BLOCKED))
			return false;
		return regionCount(level, room, room.region()[room.cell(cellX, cellZ)], e) == 0
				&& !rowOrColHas(level, room, cellX, cellZ, e, false);
	}

	/** V33a {@code blockUpdate} into {@code RayBlendGenerator.setCrystal}. */
	public void blockUpdate() {
		RayBlendLayout.Room room = room();
		if (room == null || level == null)
			return;
		CrystalElement e = BlockRayblendFloor26.crystalOf(level.getBlockState(worldPosition.above()));
		if (e != null) {
			ping(level, room, cellX, cellZ, e);
			setComplete(level, room, isValid(level, room));
		}
		// V33a removeCrystal deliberately leaves isComplete alone ("//isComplete = false;").
		updateDoors(level, room, isComplete(level, room));
	}

	// --------------------------------------------------------------------------------------- room logic

	public static @Nullable CrystalElement crystalAt(Level level, RayBlendLayout.Room room, int cx, int cz) {
		return BlockRayblendFloor26.crystalOf(level.getBlockState(room.floor(cx, cz).above()));
	}

	/** V33a {@code Subgrid.countColorPresence}. */
	public static int regionCount(Level level, RayBlendLayout.Room room, int region, CrystalElement e) {
		int n = 0;
		for (int i = 0; i < room.edge(); i++)
			for (int k = 0; k < room.edge(); k++)
				if (room.region()[room.cell(i, k)] == region && crystalAt(level, room, i, k) == e)
					n++;
		return n;
	}

	/** V33a {@code rowOrColHas}; {@code skipSelf} is GridSlot.isValid clearing its own crystal for the check. */
	public static boolean rowOrColHas(Level level, RayBlendLayout.Room room, int cx, int cz, CrystalElement e,
			boolean skipSelf) {
		for (int i = 0; i < room.edge(); i++) {
			if (!(skipSelf && i == cz) && crystalAt(level, room, cx, i) == e)
				return true;
			if (!(skipSelf && i == cx) && crystalAt(level, room, i, cz) == e)
				return true;
		}
		return false;
	}

	/** V33a {@code isValid}: every open cell holds a crystal its region, row and column hold nowhere else. */
	public static boolean isValid(Level level, RayBlendLayout.Room room) {
		for (int i = 0; i < room.edge(); i++) {
			for (int k = 0; k < room.edge(); k++) {
				int c = room.cell(i, k);
				if (room.blocked()[c])
					continue;
				CrystalElement e = crystalAt(level, room, i, k);
				if (e == null || regionCount(level, room, room.region()[c], e) > 1 || rowOrColHas(level, room, i, k, e, true))
					return false;
			}
		}
		return true;
	}

	private static @Nullable TileEntityRayblendFloor anchor(Level level, RayBlendLayout.Room room) {
		return level.getBlockEntity(room.floor(0, 0)) instanceof TileEntityRayblendFloor te ? te : null;
	}

	public static boolean isComplete(Level level, RayBlendLayout.Room room) {
		TileEntityRayblendFloor te = anchor(level, room);
		return te != null && te.complete;
	}

	public static void setComplete(Level level, RayBlendLayout.Room room, boolean complete) {
		TileEntityRayblendFloor te = anchor(level, room);
		if (te != null && te.complete != complete) {
			te.complete = complete;
			te.setChanged();
		}
	}

	/** V33a {@code updateDoors}: every door of the room follows the solved flag. */
	public static void updateDoors(Level level, RayBlendLayout.Room room, boolean open) {
		for (BlockPos door : room.doors()) {
			if (level.getBlockEntity(door) instanceof TileEntityChromaDoor d) {
				if (open)
					d.open(0);
				else
					d.close();
			}
		}
	}

	/** V33a {@code forceOpen}. */
	public static void forceOpen(Level level, RayBlendLayout.Room room) {
		setComplete(level, room, true);
		updateDoors(level, room, true);
	}

	/**
	 * V33a {@code ping}: a ding at the new crystal, pitched for its colour, and an exclusion flash over every other
	 * cell of its region, row and column.
	 */
	private static void ping(Level level, RayBlendLayout.Room room, int cx, int cz, CrystalElement e) {
		BlockPos at = room.floor(cx, cz).above();
		ChromaSounds.DING.playSoundAtBlock(level, at, 1, (float)CrystalMusicManager.instance.getDingPitchScale(e));
		Set<BlockPos> pinged = new LinkedHashSet<>();
		int region = room.region()[room.cell(cx, cz)];
		for (int i = 0; i < room.edge(); i++)
			for (int k = 0; k < room.edge(); k++)
				if ((i != cx || k != cz) && room.region()[room.cell(i, k)] == region)
					pinged.add(room.floor(i, k));
		for (int p = 0; p < room.edge(); p++) {
			if (p != cz)
				pinged.add(room.floor(cx, p));
			if (p != cx)
				pinged.add(room.floor(p, cz));
		}
		for (BlockPos floor : pinged) {
			BlockState state = level.getBlockState(floor);
			if (state.getBlock() instanceof BlockRayblendFloor26)
				level.blockEvent(floor, state.getBlock(), PING_EVENT, e.ordinal());
		}
	}

	/** V33a {@code doPingParticle}: nine big centre blurs over the cell's crystal spot. */
	@Override
	public boolean triggerEvent(int id, int value) {
		if (id == PING_EVENT) {
			if (level != null && level.isClientSide()) {
				int color = CrystalElement.elements[Math.floorMod(value, 16)].getColor();
				BlockPos at = worldPosition.above();
				for (double i = 0.25; i <= 0.75; i += 0.25)
					for (double k = 0.25; k <= 0.75; k += 0.25)
						ChromaParticle.spawnIconFade(level, at.getX() + i, at.getY() + 0.5, at.getZ() + k, 0, 0, 0,
								color, 60, 5, ChromaIcons.CENTER.getIconName(), true, true);
			}
			return true;
		}
		return super.triggerEvent(id, value);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("layoutVersion", layoutVersion);
		output.putInt("difficulty", difficulty);
		output.putLong("seed", seed);
		output.putInt("originX", originX);
		output.putInt("originZ", originZ);
		output.putInt("room", roomIndex);
		output.putInt("tileX", cellX);
		output.putInt("tileZ", cellZ);
		output.putBoolean("complete", complete);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		layoutVersion = input.getIntOr("layoutVersion", RayBlendLayout.LEGACY_VERSION);
		difficulty = input.getIntOr("difficulty", 3);
		seed = input.getLongOr("seed", 0);
		originX = input.getIntOr("originX", 0);
		originZ = input.getIntOr("originZ", 0);
		roomIndex = input.getIntOr("room", -1);
		cellX = input.getIntOr("tileX", 0);
		cellZ = input.getIntOr("tileZ", 0);
		complete = input.getBooleanOr("complete", false);
	}

	/** The client mirrors the plan key so its own click check agrees with the server's. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
		return saveCustomOnly(provider);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
