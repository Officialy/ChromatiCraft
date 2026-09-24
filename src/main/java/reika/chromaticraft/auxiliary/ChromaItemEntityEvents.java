package reika.chromaticraft.auxiliary;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.minecraft.util.TriState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.ItemFertilitySeed;
import reika.dragonapi.instantiable.event.ItemLeafCollisionEvent;

/** V33a's two Fertility Seed hooks outside the item itself. */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaFertilitySeedEvents {

	private ChromaFertilitySeedEvents() {}

	/**
	 * V33a {@code ChromaAux} collision filter: a dropped Fertility Seed falls through dye leaves, decay
	 * leaves, glowing leaves and every other leaf block, so roots under a canopy seed the ground.
	 */
	@SubscribeEvent
	public static void fallThroughLeaves(ItemLeafCollisionEvent event) {
		if (event.item.getItem().getItem() instanceof ItemFertilitySeed && ItemFertilitySeed.passesThrough(event.state))
			event.setCanceled(true);
	}

	/** V33a {@code preventFertilitySeedReuse}: once a seed has woken it can no longer be picked up. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void preventFertilitySeedReuse(ItemEntityPickupEvent.Pre event) {
		if (event.getItemEntity().getItem().getItem() instanceof ItemFertilitySeed
				&& event.getItemEntity().getAge() >= ItemFertilitySeed.INITIAL_DELAY)
			event.setCanPickup(TriState.FALSE);
	}
}
