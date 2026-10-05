package reika.chromaticraft.auxiliary;

import net.minecraft.util.TriState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.ItemFertilitySeed;
import reika.dragonapi.instantiable.event.ItemLeafCollisionEvent;

/** V33a item-entity hooks that live outside the items themselves (Fertility Seeds, Enhanced Ender Eyes). */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaItemEntityEvents {

	private ChromaItemEntityEvents() {}

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

	/** V33a {@code lockT2EnderEyes}: an owned Enhanced Ender Eye can only be picked up by its owner. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void lockT2EnderEyes(ItemEntityPickupEvent.Pre event) {
		if (event.getItemEntity().getItem().getItem() instanceof reika.chromaticraft.items.ItemT2EnderEye eye
				&& !eye.isCollectableBy(event.getItemEntity(), event.getPlayer()))
			event.setCanPickup(TriState.FALSE);
	}
}
