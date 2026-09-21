package reika.chromaticraft.auxiliary.ability;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.ElementBufferCapacityBoost;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.registry.CrystalElement;

/** V33a Deathproof loss cache, retained between the death event and the replacement player clone. */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class AbilityDeathHandler {

	private static final String CHANCE_TAG = "bufferDeathChance";
	private static final double INITIAL_CHANCE = 10;
	private static final double CHANCE_INCREMENT = 5;
	private static final double MAX_CHANCE = 80;
	private static final int DEATH_COOLDOWN = 20 * 60 * 5;
	private static final Map<UUID, LossCache> CACHES = new HashMap<>();

	private AbilityDeathHandler() {}

	@SubscribeEvent
	public static void playerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
			return;
		if (Chromabilities.DEATHPROOF.enabledOn(player))
			CACHES.put(player.getUUID(), new LossCache(player));
		else
			player.getPersistentData().remove(CHANCE_TAG);
	}

	@SubscribeEvent
	public static void playerClone(PlayerEvent.Clone event) {
		if (!event.isWasDeath() || event.getEntity().level().isClientSide())
			return;
		LossCache cache = CACHES.remove(event.getOriginal().getUUID());
		if (cache != null)
			cache.apply(event.getEntity());
	}

	private static final class LossCache {
		private final List<Ability> abilities = new ArrayList<>();
		private final EnumSet<ElementBufferCapacityBoost> boosts = EnumSet.noneOf(ElementBufferCapacityBoost.class);
		private final ElementTagCompound energy = new ElementTagCompound();
		private final CompoundTag chanceData;
		private int capacity = 24;

		private LossCache(Player player) {
			CompoundTag root = player.getPersistentData();
			CompoundTag previous = root.getCompoundOrEmpty(CHANCE_TAG).copy();
			double chance = previous.contains("chance") ? previous.getDoubleOr("chance", INITIAL_CHANCE)
					: INITIAL_CHANCE;
			long time = player.level().getGameTime();
			if (previous.contains("time") && time > previous.getLongOr("time", 0) + DEATH_COOLDOWN)
				chance = Math.min(MAX_CHANCE, chance + CHANCE_INCREMENT);
			previous.putDouble("chance", chance);
			previous.putLong("time", time);
			root.put(CHANCE_TAG, previous.copy());
			chanceData = previous;

			for (Ability ability : ChromaAbilityData.getAvailableFrom(player)) {
				if (ability == Chromabilities.DEATHPROOF || player.getRandom().nextDouble() * 100 < chance)
					abilities.add(ability);
			}
			for (ElementBufferCapacityBoost boost : ElementBufferCapacityBoost.list) {
				if (boost.playerHas(player))
					boosts.add(boost);
			}
			for (CrystalElement element : CrystalElement.elements) {
				int amount = PlayerElementBuffer.instance.getPlayerContent(player, element);
				if (amount <= 0)
					continue;
				double removedRatio = 1D / (2 + player.getRandom().nextInt(3));
				int randomRange = (int)(amount * (1 - removedRatio));
				int saved = (int)(amount * removedRatio)
						+ (randomRange > 0 ? player.getRandom().nextInt(randomRange) : 0);
				saved = (int)(saved * chance / 100D);
				if (saved > 0)
					energy.addTag(element, saved);
			}
			while (capacity < energy.getMaximumValue())
				capacity *= 2;
		}

		private void apply(Player player) {
			player.getPersistentData().put(CHANCE_TAG, chanceData.copy());
			PlayerElementBuffer.instance.setElementCap(player, capacity, false);
			PlayerElementBuffer.instance.addToPlayer(player, energy, false);
			for (Ability ability : abilities)
				ChromaAbilityData.give(player, ability);
			for (ElementBufferCapacityBoost boost : boosts)
				boost.give(player);
			if (abilities.contains(Chromabilities.DEATHPROOF))
				Chromabilities.DEATHPROOF.setToPlayer(player, true);
		}
	}
}
