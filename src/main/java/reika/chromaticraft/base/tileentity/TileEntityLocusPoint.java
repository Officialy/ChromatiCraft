package reika.chromaticraft.base.tileentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;

import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.dragonapi.instantiable.data.blockstruct.ThreadSafeTileCache;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.data.maps.NestedMap;
import reika.dragonapi.interfaces.blockentity.LocationCached;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code TileEntityLocusPoint}: the base for tiles a player plants and later wants to find again —
 * Aura Points, Dimension Cores, and the rest of the "somewhere in the world I put one of these" family.
 *
 * <p>Its whole reason to exist is the cache. A locus point is looked up by <em>who placed it</em>, not
 * by where it is, and often from a thread that is not the one holding the world: the ability system
 * asks "does this player have an aura point anywhere", bee product checks ask the same, and neither
 * has a position to start from. So every instance registers itself into a per-class, per-owner
 * {@link ThreadSafeTileCache} on its first tick and removes itself when broken, and the static queries
 * below are the only sanctioned way to ask.
 *
 * <p>The cache holds {@link WorldLocation}s rather than tiles on purpose: a location outlives the tile,
 * survives chunk unload, and can be resolved across dimensions, which a held reference could not. The
 * cache drops entries whose tile has gone as it walks them.
 *
	 * <p>The owner also round-trips through the dropped item. This is not optional for an Aura Locus:
	 * placing a picked-up locus as the current player would otherwise silently steal it because the
	 * ordinary block placement callback runs before the item's saved owner data is restored.
 */
public abstract class TileEntityLocusPoint extends TileEntityChromaticBase
		implements LocationCached, OwnedTile {

	/**
	 * Keyed by concrete class and then by owner. Upstream's own comment concedes this is not strictly
	 * thread safe and that it has never been seen to break; the set inside each entry is, which is what
	 * the traversals actually contend on.
	 */
	private static final NestedMap<Class<? extends TileEntityLocusPoint>, UUID, ThreadSafeTileCache>
			cache = new NestedMap<>();

	protected TileEntityLocusPoint(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setRemoved() {
		ThreadSafeTileCache c = cache.get(this.getClass(), this.getPlacerID());
		if (c != null)
			c.remove(new WorldLocation(this));
		super.setRemoved();
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		cacheTile(this);
	}

	private static void cacheTile(TileEntityLocusPoint te) {
		Class<? extends TileEntityLocusPoint> cl = te.getClass();
		ThreadSafeTileCache c = cache.get(cl, te.getPlacerID());
		if (c == null) {
			c = new ThreadSafeTileCache().setTileClass(TileEntityLocusPoint.class);
			cache.put(cl, te.getPlacerID(), c);
		}
		c.add(new WorldLocation(te));
	}

	/** The colour this point glows and draws its particles in. */
	public abstract int getRenderColor();

	public static boolean hasLoci(Class<? extends TileEntityLocusPoint> cl, UUID uid) {
		ThreadSafeTileCache c = cache.get(cl, uid);
		return c != null && !c.isEmpty();
	}

	public static WorldLocation getMatchFromCache(Class<? extends TileEntityLocusPoint> cl, UUID uid,
			Function<WorldLocation, Boolean> check) {
		ThreadSafeTileCache c = cache.get(cl, uid);
		if (c == null || c.isEmpty())
			return null;
		return c.iterateAsSearch(check);
	}

	public static void forEach(Class<? extends TileEntityLocusPoint> cl, UUID uid,
			Consumer<WorldLocation> action) {
		ThreadSafeTileCache c = cache.get(cl, uid);
		if (c == null || c.isEmpty())
			return;
		c.simpleIterate(action);
	}

	@SuppressWarnings("unchecked")
	public static <T extends TileEntityLocusPoint> Collection<T> getTiles(Class<T> cl, UUID uid) {
		ArrayList<T> li = new ArrayList<>();
		ThreadSafeTileCache c = cache.get(cl, uid);
		if (c == null || c.isEmpty())
			return li;
		c.simpleIterate(w -> {
			// A location can outlive its tile; the cache drops such entries as it walks, but a
			// traversal that only reads must not hand a null out to its caller.
			Object te = w.getBlockEntity();
			if (cl.isInstance(te))
				li.add((T)te);
		});
		return li;
	}

	/** Whether any point of this class, from any owner, lies within {@code r} of the position. */
	public static boolean isPointWithin(Class<? extends TileEntityLocusPoint> cl, Level world,
			BlockPos pos, int r) {
		Map<UUID, ThreadSafeTileCache> map = cache.getMap(cl);
		if (map == null)
			return false;
		for (ThreadSafeTileCache c : map.values())
			if (c.iterateAsSearch(loc -> loc.getDimension().equals(world.dimension())
					&& Math.sqrt(loc.pos.distSqr(pos)) <= r) != null)
				return true;
		return false;
	}

	public static void clearCache() {
		cache.clear();
	}

	@Override
	public boolean isOwnedByPlayer(Player ep) {
		UUID placer = this.getPlacerID();
		return placer != null && placer.equals(ep.getUUID());
	}

	@Override
	public void getTagsToWriteToStack(CompoundTag NBT) {
		if (placer != null && !placer.isEmpty())
			NBT.putString("place", placer);
		if (placerUUID != null)
			NBT.putString("placeUUID", placerUUID.toString());
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		if (tag == null)
			return;
		placer = tag.getStringOr("place", placer != null ? placer : "");
		String id = tag.getStringOr("placeUUID", "");
		if (!id.isEmpty()) {
			try {
				placerUUID = UUID.fromString(id);
			}
			catch (IllegalArgumentException ignored) {
				// A malformed item must not erase the valid placer just assigned by placement.
			}
		}
	}

	@Override
	public void addTooltipInfo(List li, boolean shift) {}
}
