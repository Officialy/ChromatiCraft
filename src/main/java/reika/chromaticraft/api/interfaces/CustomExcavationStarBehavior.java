package reika.chromaticraft.api.interfaces;

import java.util.Collection;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.dragonapi.instantiable.data.immutable.BlockKey;

/** Implement this on a Block object to supply custom behavior for the excavation star. Intended for things like mining pipes which are readily
clearable en masse, or blocks where the propagation should spread according to nonstandard rules. */
public interface CustomExcavationStarBehavior {

	/** How far the mining effect can propagate from the source. Note that it cannot reduce the range, only increase it, so return -1 for "use default". */
	public int getRange(Level world, int x, int y, int z, Player ep);

	/** Which block/meta pairs are valid to spread to (including indirectly) from the given source. Return null or empty list for default behavior. */
	public Collection<BlockKey> getSpreadBlocks(Level world, int x, int y, int z);

}
