package reika.chromaticraft.auxiliary;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/**
 * V33a's player-specific, non-expiring Focus Crystal villager offer.
 *
 * <p>Minecraft 26.2 made villager offers data-driven, but a trade's loot context contains the
 * merchant rather than the player who is about to open the screen.  The offer definition therefore
 * lives in the {@code villager_trade} datapack registry while this small interaction bridge applies
 * the two pieces which cannot be expressed by that codec: the {@link ProgressStage#CRYSTALS} player
 * gate and V33a's independent 40% roll per eligible villager.  The roll is persisted on the entity,
 * so reloading or repeatedly opening the screen cannot reroll it.
 */
public final class FocusCrystalTradeHandler {

	private static final String ROLLED_TAG = "chromaticraft_focus_trade_rolled";
	private static final String ENABLED_TAG = "chromaticraft_focus_trade_enabled";
	private static final float CHANCE = 0.4F;

	private FocusCrystalTradeHandler() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(FocusCrystalTradeHandler::onVillagerInteract);
	}

	private static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof Villager villager)
				|| !(villager.level() instanceof ServerLevel level)
				|| event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND)
				return;
		Player player = event.getEntity();
		if (ReikaPlayerAPI.isFake(player) || !isEligibleProfession(villager))
			return;

		CompoundTag data = villager.getPersistentData();
		if (!data.getBooleanOr(ROLLED_TAG, false)) {
			data.putBoolean(ROLLED_TAG, true);
			data.putBoolean(ENABLED_TAG, villager.getRandom().nextFloat() < CHANCE);
		}

		MerchantOffers offers = villager.getOffers();
		offers.removeIf(FocusCrystalTradeHandler::isFocusCrystalOffer);
		if (data.getBooleanOr(ENABLED_TAG, false) && ProgressStage.CRYSTALS.isPlayerAtStage(player))
			createOffer(level, villager).ifPresent(offers::add);
	}

	private static boolean isEligibleProfession(Villager villager) {
		var profession = villager.getVillagerData().profession();
		// V33a profession ids 1, 2 and 3 were librarian, priest and blacksmith.  Modern
		// Minecraft split blacksmith into its three specialist professions.
		return profession.is(VillagerProfession.LIBRARIAN)
				|| profession.is(VillagerProfession.CLERIC)
				|| profession.is(VillagerProfession.ARMORER)
				|| profession.is(VillagerProfession.TOOLSMITH)
				|| profession.is(VillagerProfession.WEAPONSMITH);
	}

	private static Optional<MerchantOffer> createOffer(ServerLevel level, Villager villager) {
		Optional<VillagerTrade> definition = level.registryAccess()
				.lookupOrThrow(Registries.VILLAGER_TRADE)
				.getOptional(ChromaWorldGenProvider.FOCUS_CRYSTAL_TRADE);
		if (definition.isEmpty())
			return Optional.empty();
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, villager.position())
				.withParameter(LootContextParams.THIS_ENTITY, villager)
				.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,
						net.minecraft.util.Unit.INSTANCE)
				.create(LootContextParamSets.VILLAGER_TRADE)).create(Optional.empty());
		return Optional.ofNullable(definition.get().getOffer(context));
	}

	public static boolean isFocusCrystalOffer(MerchantOffer offer) {
		return offer.getResult().is(ChromaBlocks.FOCUS_CRYSTAL.get().asItem());
	}
}
