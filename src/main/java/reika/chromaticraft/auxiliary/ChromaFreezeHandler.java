package reika.chromaticraft.auxiliary;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.tileentity.plants.TileEntityHeatLily;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.instantiable.event.IceFreezeEvent;

/** V33a freeze exclusions for Heat Lilies, Luminous Cliffs, and Proxima. */
public final class ChromaFreezeHandler {
	private ChromaFreezeHandler() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ChromaFreezeHandler::onFreeze);
	}

	private static void onFreeze(IceFreezeEvent event) {
		BlockPos pos = new BlockPos(event.xCoord, event.yCoord, event.zCoord);
		boolean luminousCliffs = event.world.getBiome(pos).is(ChromaBiomes.LUMINOUS_CLIFFS)
				|| event.world.getBiome(pos).is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
		if (event.world.dimension().equals(ChromaDimensions.PROXIMA) || luminousCliffs
				|| TileEntityHeatLily.stopFreeze(event.world, pos))
			event.setCanceled(true);
	}
}
