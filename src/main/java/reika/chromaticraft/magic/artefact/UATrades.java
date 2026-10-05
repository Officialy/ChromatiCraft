/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.artefact;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
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
import reika.dragonapi.libraries.ReikaPlayerAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * V33a {@code UATrades}: villagers who will buy an Unknown Artefact (as a Thargoid Sensor) or a Lumen Pod
 * (as Meta-Alloys) for emeralds, priced from those commodities' Elite: Dangerous market values scaled so
 * the dearest fetches {@link #BASE_TRADE_YIELD} emeralds. The offers never run out.
 *
 * <p>Each villager independently rolls, once, whether it carries each offer (20% for the artefact, 10% for
 * the Lumen Pod, and only farmers, librarians and clerics - 1.7.10 professions 0-2 - buy Lumen Pods). The
 * artefact offer is only shown to players who have found an artefact. As with the Focus Crystal offer,
 * 26.2's villager-trade registry holds the offers and this bridge applies the per-villager roll and the
 * per-player gate, which the trade codec cannot express.
 *
 * <p>V33a refreshed its prices from eddb.io at startup, falling back to the defaults below; eddb.io shut
 * down in 2021, so the defaults it shipped are the prices. External commodity hooks remain registrable.
 */
public class UATrades {

	public static final int BASE_TRADE_YIELD = 48;

	public static final UATrades instance = new UATrades();

	private static final String ROLL_PREFIX = "chromaticraft_ua_trade_";

	private final HashMap<String, Integer> prices = new HashMap<>();
	private int MAX_PRICE = -1;
	private final List<EDCommodityHook> externalHooks = new ArrayList<>();

	private UATrades() {
		this.loadDefaults();
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(UATrades::onVillagerInteract);
	}

	private void loadDefaults() {
		this.addPrice("Thargoid Sensor", 288000);
		this.addPrice("Meta-Alloys", 114000);
	}

	public void registerCommodityHook(EDCommodityHook ech) {
		externalHooks.add(ech);
		this.addPrice(ech.getCommodityID(), 0);
	}

	private void addPrice(String id, int val) {
		prices.put(id, val);
		MAX_PRICE = Math.max(MAX_PRICE, val);
	}

	/** V33a {@code calcValue}: emeralds paid for one unit of the commodity. */
	public static int calcValue(String id) {
		int price = instance.prices.get(id);
		return price * BASE_TRADE_YIELD / instance.MAX_PRICE;
	}

	/** One of V33a's {@code UATypeTrade}s. */
	public enum TradeType {
		ARTEFACT(ChromaWorldGenProvider.UA_TRADE, "Thargoid Sensor", 0.2F),
		META_ALLOY(ChromaWorldGenProvider.META_ALLOY_TRADE, "Meta-Alloys", 0.1F);

		public final ResourceKey<VillagerTrade> offer;
		public final String priceID;
		public final float chance;

		TradeType(ResourceKey<VillagerTrade> offer, String priceID, float chance) {
			this.offer = offer;
			this.priceID = priceID;
			this.chance = chance;
		}

		/** V33a {@code isValid(EntityPlayer)}. */
		public boolean isValidFor(Player ep) {
			return this != ARTEFACT || ProgressStage.ARTEFACT.isPlayerAtStage(ep);
		}

		/** V33a {@code isValidForVillager}. */
		public boolean isValidForVillager(Villager ev) {
			if (this == ARTEFACT)
				return true;
			var p = ev.getVillagerData().profession();
			// 1.7.10 professions 0-2 were farmer, librarian and priest; the later careers split off them.
			return p.is(VillagerProfession.FARMER) || p.is(VillagerProfession.FISHERMAN) || p.is(VillagerProfession.SHEPHERD)
					|| p.is(VillagerProfession.FLETCHER) || p.is(VillagerProfession.LIBRARIAN)
					|| p.is(VillagerProfession.CARTOGRAPHER) || p.is(VillagerProfession.CLERIC);
		}
	}

	private static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)
				|| event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND)
			return;
		Player player = event.getEntity();
		if (ReikaPlayerAPI.isFake(player))
			return;
		CompoundTag data = villager.getPersistentData();
		MerchantOffers offers = villager.getOffers();
		for (TradeType t : TradeType.values()) {
			String rolled = ROLL_PREFIX + t.name().toLowerCase() + "_rolled";
			String enabled = ROLL_PREFIX + t.name().toLowerCase();
			if (!data.getBooleanOr(rolled, false)) {
				data.putBoolean(rolled, true);
				data.putBoolean(enabled, villager.getRandom().nextFloat() < t.chance && t.isValidForVillager(villager));
			}
			offers.removeIf(o -> isOffer(o, t));
			if (data.getBooleanOr(enabled, false) && t.isValidFor(player))
				createOffer(level, villager, t).ifPresent(offers::add);
		}
	}

	private static boolean isOffer(MerchantOffer o, TradeType t) {
		return switch (t) {
			case ARTEFACT -> o.getBaseCostA().is(reika.chromaticraft.registry.ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get());
			case META_ALLOY -> o.getBaseCostA().is(reika.chromaticraft.registry.ChromaBlocks.META_ALLOY_LAMP.get().asItem());
		};
	}

	private static Optional<MerchantOffer> createOffer(ServerLevel level, Villager villager, TradeType t) {
		Optional<VillagerTrade> definition = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
				.getOptional(t.offer);
		if (definition.isEmpty())
			return Optional.empty();
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, villager.position())
				.withParameter(LootContextParams.THIS_ENTITY, villager)
				.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, net.minecraft.util.Unit.INSTANCE)
				.create(LootContextParamSets.VILLAGER_TRADE)).create(Optional.empty());
		return Optional.ofNullable(definition.get().getOffer(context));
	}

	/** V33a {@code EDCommodityHook}: another mod's commodity, priced alongside these. */
	public interface EDCommodityHook {

		/** EDDB id */
		String getCommodityID();
		/** Out of 1 */
		double getChancePerVillager();
		MerchantOffer createTrade();
		void onPriceReceived(int average, int upper, int max);

	}

}
