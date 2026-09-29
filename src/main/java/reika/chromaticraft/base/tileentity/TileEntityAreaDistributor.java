package reika.chromaticraft.base.tileentity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.api.interfaces.customrangeupgrade.RangeUpgradeable;
import reika.dragonapi.instantiable.data.blockstruct.BreadthFirstSearch;
import reika.dragonapi.instantiable.data.blockstruct.OpenPathFinder;
import reika.dragonapi.instantiable.data.blockstruct.OpenPathFinder.PassRules;
import reika.dragonapi.instantiable.data.immutable.BlockBox;

/** V33a area-distributor scan and open-path selection, using 26.3 positions without loading chunks. */
public abstract class TileEntityAreaDistributor extends TileEntityChromaticBase implements RangeUpgradeable {

	public static final int SCAN_RADIUS_XZ = 16;
	private static final int SCAN_INTERVAL = 40;
	private final Set<BlockPos> storages = new HashSet<>();
	private final Set<BlockPos> inputs = new HashSet<>();
	private final Map<BlockPos, LinkedList<BlockPos>> pathCache = new HashMap<>();
	private int range = SCAN_RADIUS_XZ;
	private int requestedRange = SCAN_RADIUS_XZ;

	protected TileEntityAreaDistributor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (range < requestedRange)
			range++;
		else if (range > requestedRange)
			range = requestedRange;
		if (this.getTicksExisted() == 1 || this.getTicksExisted() % SCAN_INTERVAL == 0)
			this.scanAndCache(world, pos);
	}

	protected final void addInput(BlockPos pos) {
		inputs.add(pos.immutable());
		storages.remove(pos);
	}

	protected final Iterator<BlockPos> getTargets() {
		return storages.iterator();
	}

	public final int getCachedTargetCount() {
		return storages.size();
	}

	private void scanAndCache(Level world, BlockPos origin) {
		int r = this.getRange();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -r / 2; dy <= 0; dy++) {
					cursor.setWithOffset(origin, dx, dy, dz);
					if (!world.isLoaded(cursor) || inputs.contains(cursor))
						continue;
					BlockEntity te = world.getBlockEntity(cursor);
					if (this.isValidTarget(te) && this.canAccess(world, origin, cursor))
						storages.add(cursor.immutable());
				}
			}
		}
	}

	private boolean canAccess(Level world, BlockPos origin, BlockPos target) {
		LinkedList<BlockPos> cached = pathCache.get(target);
		if (cached != null && this.pathStillOpen(world, cached))
			return true;
		pathCache.remove(target);
		BlockBox bounds = new BlockBox(origin.getX(), origin.getY(), origin.getZ(),
				target.getX(), target.getY(), target.getZ());
		LinkedList<BlockPos> path = BreadthFirstSearch.getOpenPathBetween(world, origin, target,
				SCAN_RADIUS_XZ, bounds, EnumSet.of(PassRules.SMALLNONSOLID, PassRules.SOFT));
		if (path.isEmpty())
			return false;
		pathCache.put(target.immutable(), path);
		return true;
	}

	private boolean pathStillOpen(Level world, LinkedList<BlockPos> path) {
		for (int i = 1; i < path.size() - 1; i++) {
			BlockPos pos = path.get(i);
			if (!world.isLoaded(pos) || !OpenPathFinder.isEmptyBlock(world, pos,
					EnumSet.of(PassRules.SMALLNONSOLID, PassRules.SOFT)))
				return false;
		}
		return true;
	}

	protected abstract boolean isValidTarget(BlockEntity tile);

	@Override
	public final void upgradeRange(double factor) {
		requestedRange = Math.max(SCAN_RADIUS_XZ, (int)(SCAN_RADIUS_XZ * factor));
	}

	public final int getRange() {
		return range;
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("range", range);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		range = Math.max(SCAN_RADIUS_XZ, tag.getIntOr("range", SCAN_RADIUS_XZ));
		requestedRange = range;
	}
}
