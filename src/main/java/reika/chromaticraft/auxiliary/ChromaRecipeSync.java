package reika.chromaticraft.auxiliary;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaRecipeTypes;

/**
 * 26.2 no longer sends recipes to clients. The casting and pool-alloying recipes are synced so recipe viewers
 * (JEI) can show them; what a player may actually see is filtered client-side by their progression
 * ({@code ChromaClientRecipes}).
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaRecipeSync {

	private ChromaRecipeSync() {}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		event.sendRecipes(ChromaRecipeTypes.CASTING.get(), ChromaRecipeTypes.POOL_ALLOYING.get());
	}

}
