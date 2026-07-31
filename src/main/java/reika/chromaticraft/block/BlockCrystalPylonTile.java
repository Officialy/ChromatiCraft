package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.chromaticraft.auxiliary.interfaces.ProgressionTrigger;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/**
 * V33a {@code BlockCrystalPylon}'s progression role: looking at an assembled, conducting pylon grants
 * {@link ProgressStage#PYLON}.
 *
 * <p>This is load-bearing for the early game rather than cosmetic. PYLON is the prerequisite of
 * ALLCOLORS, ALLCOLORS gates the crystal-rune recipe, and placing a rune is the only way to reach
 * RUNEUSE — which is what unlocks tier-2 (auxiliary-stand) casting. Without this trigger the whole
 * branch is unreachable.
 *
 * <p>Kept as its own block class rather than folded into {@link BlockChromaticTile}, which every
 * other ChromatiCraft tile block shares.
 */
public class BlockCrystalPylonTile extends BlockChromaticTile implements ProgressionTrigger {

	public BlockCrystalPylonTile(Properties props, ChromaTiles tile) {
		super(props, tile);
	}

	@Override
	public ProgressStage[] getTriggers(Player ep, Level world, BlockPos pos) {
		// V33a requires the pylon to be assembled and conducting, not merely present.
		if (world.getBlockEntity(pos) instanceof TileEntityCrystalPylon pylon && pylon.canConduct())
			return new ProgressStage[]{ProgressStage.PYLON};
		return null;
	}
}
