package reika.chromaticraft.auxiliary;

import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Preserves V33a's deliberately short 300-tick lifetime for Orchid-produced item entities. */
public final class CobbleGeneratorItemExpiry {
	private static final String EXPIRY_TAG = "chromaticraft_cobble_generator_expiry";

	private CobbleGeneratorItemExpiry() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(CobbleGeneratorItemExpiry::onItemTick);
	}

	public static void mark(ItemEntity item) {
		item.getPersistentData().putLong(EXPIRY_TAG, item.level().getGameTime() + 300);
	}

	private static void onItemTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof ItemEntity item) || item.level().isClientSide()) return;
		long expiry = item.getPersistentData().getLongOr(EXPIRY_TAG, Long.MAX_VALUE);
		if (item.level().getGameTime() >= expiry) item.discard();
	}
}
