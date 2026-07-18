package reika.chromaticraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTabs;

/**
 * ChromatiCraft main mod class. Port-in-progress: this is the minimal 26.2 @Mod entry point that
 * wires the DeferredRegister families onto the mod event bus, growing as content is ported (mirrors
 * ReactorCraft.java). The 1.7.10 original (config, packets, proxies, tab objects, fluid, etc.) is
 * preserved in origin/master and re-expressed subsystem-by-subsystem as those areas port.
 */
@Mod(ChromatiCraft.MODID)
public class ChromatiCraft {

	public static final String MODID = "chromaticraft";
	public static final String packetChannel = "ChromaData";
	public static final Logger LOGGER = LogManager.getLogger("ChromatiCraft");

	public static ChromatiCraft instance;

	public ChromatiCraft(IEventBus modEventBus, ModContainer modContainer) {
		instance = this;

		ChromaBlocks.BLOCKS.register(modEventBus);
		ChromaBlocks.ITEMS.register(modEventBus);
		ChromaItems.ITEMS.register(modEventBus);
		ChromaTabs.CREATIVE_MODE_TABS.register(modEventBus);
	}
}
