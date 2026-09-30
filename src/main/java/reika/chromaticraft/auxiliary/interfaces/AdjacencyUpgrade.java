package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.registry.CrystalElement;

/**
 * The active tier and colour supplied by an operating Adjacency Upgrade. {@link #isOperating} is V33a's two gates: no
 * redstone signal ({@code canRun}), and, with Powered Adjacency Upgrades on, at least 100 of its colour stored.
 */
public interface AdjacencyUpgrade {
	CrystalElement getColor();
	int getTier();
	boolean isOperating();

	/**
	 * V33a {@code TileEntityAdjacencyUpgrade.getAdjacentUpgrade}: the highest {@code 1+tier} among operating upgrades of
	 * {@code color} on the six faces of {@code pos}, or 0 when there is none.
	 */
	static int getAdjacentTier(Level world, BlockPos pos, CrystalElement color) {
		int max = 0;
		for (Direction dir : Direction.values()) {
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof AdjacencyUpgrade upgrade && upgrade.isOperating() && upgrade.getColor() == color)
				max = Math.max(max, 1+upgrade.getTier());
		}
		return max;
	}
}
