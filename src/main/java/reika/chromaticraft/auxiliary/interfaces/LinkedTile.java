package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/** The two ends of a persistent, player-owned world link. */
public interface LinkedTile {
	void onLink(boolean other);
	void syncAllData(boolean fullNBT);
	void setTarget(WorldLocation loc);
	void assignLinkID(LinkedTile other);
	void markForDrop();
	boolean isPrimary();
	void setPrimary(boolean primary);
	void setPlacer(Player player);
	boolean linkTo(WorldLocation loc);
	boolean linkTo(Level world, BlockPos pos);
	void reset();
}
