package reika.chromaticraft.world.dimension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaSounds;

/**
 * V33a {@code CheatingPreventionSystem}: Proxima's puzzles assume you walk them, so the dimension
 * confiscates or disables items that would let you skip a wall.
 *
 * <p>The mechanism is fully ported — the block/item ban registry, all six {@link BanReaction}s, the
 * pre-join sweep that drops banned items back into the departure world, the post-join sweep that
 * deletes them, the held-item tick, the right-click gate, the explosion/knockback punishment, and the
 * {@link ProgressStage#STRUCTCHEAT} grant for anything past a warning.
 *
 * <p>What is legitimately empty is the ban <em>list</em>. Every entry V33a shipped names a 1.7.10
 * mod — EnderIO's travel anchor/telepad/staff, GraviSuite's Vajra, Thaumic Tinkerer's warp gate,
 * Draconic Evolution's teleporters, Botania's Flügel Eye, and two NotEnoughWands wands — and none of
 * them exist for 26.2. {@link #banItem} and {@link #banBlock} are public so an integration layer can
 * register the modern equivalents the moment any of those ports appear; the exact upstream
 * item-to-reaction mapping is recorded in {@link #LEGACY_BANS} rather than deleted.
 */
public final class CheatingPreventionSystem {

	public static final CheatingPreventionSystem instance = new CheatingPreventionSystem();

	/**
	 * The exact V33a ban table, as {@code modid:name -> reaction}. Kept as data so a future 26.2 port
	 * of any of these mods can be wired up without re-deriving the original severities.
	 */
	public static final Map<String, BanReaction> LEGACY_BANS = Map.ofEntries(
			Map.entry("enderio:blockTravelAnchor", BanReaction.DELETEONUSE),
			Map.entry("enderio:blockTelePad", BanReaction.DELETEONUSE),
			Map.entry("enderio:itemTravelStaff", BanReaction.DELETEONUSE),
			Map.entry("GraviSuite:vajra", BanReaction.PREVENTUSE),
			Map.entry("ThaumicTinkerer:warpGate", BanReaction.DELETEONUSE),
			Map.entry("DraconicEvolution:teleporterMKI", BanReaction.DROPONUSE),
			Map.entry("DraconicEvolution:teleporterMKII", BanReaction.DELETEONUSE),
			Map.entry("Botania:flugelEye", BanReaction.PREVENTUSE),
			Map.entry("NotEnoughWands:MovingWand", BanReaction.PREVENTUSE),
			Map.entry("NotEnoughWands:DisplacementWand", BanReaction.PREVENTUSE));

	private final Set<Block> bannedBlocks = new HashSet<>();
	private final Map<Item, BanReaction> bannedItems = new HashMap<>();

	private CheatingPreventionSystem() {
		// V33a resolved its bans through GameRegistry lookups that all fail on 26.2; applyLegacyBans
		// re-applies any of them whose mod does appear later.
		this.applyLegacyBans();
	}

	/** Re-resolves {@link #LEGACY_BANS} against the live registries; a missing entry is skipped. */
	public void applyLegacyBans() {
		LEGACY_BANS.forEach((name, reaction) -> {
			int split = name.indexOf(':');
			Identifier id = Identifier.fromNamespaceAndPath(
					name.substring(0, split).toLowerCase(java.util.Locale.ROOT), name.substring(split + 1));
			BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block -> this.banBlock(block, reaction));
			BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> this.banItem(item, reaction));
		});
	}

	public void banBlock(Block b, BanReaction r) {
		if (b == null)
			return;
		bannedBlocks.add(b);
		Item item = b.asItem();
		if (item != Items.AIR)
			bannedItems.put(item, r == null ? BanReaction.DELETEONUSE : r);
	}

	public void banItem(Item i, BanReaction r) {
		if (i != null && i != Items.AIR)
			bannedItems.put(i, r);
	}

	public boolean isBannedDimensionBlock(Block b) {
		return bannedBlocks.contains(b);
	}

	/** V33a handleRightClicks: in Proxima, a banned item's use is intercepted before it happens. */
	public boolean handleRightClick(Player ep, ItemStack held) {
		if (!isInProxima(ep))
			return false;
		BanReaction r = this.getReaction(held);
		if (r == null || !r.reactsToUse())
			return false;
		r.perform(ep, held, -1);
		return true;
	}

	/** V33a preJoin: banned items are dropped in the world being left, before the transfer. */
	public void preJoin(Player ep) {
		this.checkInventory(ep, BanReaction.PREVENTBRING);
	}

	/** V33a postJoin: anything banned that still arrived is deleted outright. */
	public void postJoin(Player ep) {
		this.checkInventory(ep, BanReaction.DELETEONENTRY);
	}

	private void checkInventory(Player ep, BanReaction br) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack is = ep.getItemBySlot(slot);
			if (this.getReaction(is) == br)
				br.performOnEquipment(ep, is, slot);
		}
		for (int i = 0; i < ep.getInventory().getContainerSize(); i++) {
			ItemStack is = ep.getInventory().getItem(i);
			if (this.getReaction(is) == br)
				br.perform(ep, is, i);
		}
	}

	/** V33a tick: DELETEONHOLD items are confiscated for merely being in hand in Proxima. */
	public void tick(Player ep) {
		if (!isInProxima(ep))
			return;
		ItemStack held = ep.getMainHandItem();
		BanReaction r = this.getReaction(held);
		if (r != null && r.reactsToTick())
			r.perform(ep, held, -1);
	}

	private static boolean isInProxima(Player ep) {
		return ep.level().dimension() == ChromaDimensions.PROXIMA;
	}

	private BanReaction getReaction(ItemStack is) {
		return is == null || is.isEmpty() ? null : bannedItems.get(is.getItem());
	}

	/** V33a punishCheatingPlayer: two explosions, 5-10 generic damage, a shove, and +10 fall. */
	public void punishCheatingPlayer(Player ep) {
		ChromaSounds.SHOCKWAVE.playSound(ep, 1, 1);
		ChromaSounds.SHOCKWAVE.playSound(ep, 1, 0.5F);
		ep.hurt(ep.damageSources().generic(), 5 + ep.getRandom().nextInt(6));
		Vec3 look = ep.getLookAngle();
		// V33a knocks the player back from a point one and a half blocks below their own eyeline in
		// the direction they are facing, i.e. away from whatever they were trying to reach.
		Vec3 from = ep.position().add(look.x, look.y - 1.5, look.z);
		Vec3 push = ep.position().subtract(from).normalize().scale(2.5);
		ep.setDeltaMovement(ep.getDeltaMovement().add(push));
		ep.hurtMarked = true;
		ep.fallDistance += 10;
	}

	public enum BanReaction {
		PREVENTUSE,
		PREVENTBRING,
		DROPONUSE,
		DELETEONUSE,
		DELETEONHOLD,
		DELETEONENTRY,
		;

		void perform(Player ep, ItemStack is, int slot) {
			switch (this) {
				case PREVENTUSE -> ChromaSounds.ERROR.playSound(ep);
				case PREVENTBRING, DELETEONENTRY -> {
					if (this == PREVENTBRING)
						dropPermanently(ep, is);
					if (slot >= 0)
						ep.getInventory().setItem(slot, ItemStack.EMPTY);
					else
						ep.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
				case DROPONUSE, DELETEONUSE, DELETEONHOLD -> {
					instance.punishCheatingPlayer(ep);
					if (this == DROPONUSE)
						dropPermanently(ep, is);
					ep.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
			}
			if (this.givesProgress())
				ProgressStage.STRUCTCHEAT.stepPlayerTo(ep);
		}

		void performOnEquipment(Player ep, ItemStack is, EquipmentSlot slot) {
			if (this == PREVENTBRING)
				dropPermanently(ep, is);
			ep.setItemSlot(slot, ItemStack.EMPTY);
			if (this.givesProgress())
				ProgressStage.STRUCTCHEAT.stepPlayerTo(ep);
		}

		/** V33a set the dropped entity's lifespan to Integer.MAX_VALUE so it waits for you. */
		private static void dropPermanently(Player ep, ItemStack is) {
			ItemEntity dropped = ep.drop(is.copy(), false);
			if (dropped != null)
				dropped.setUnlimitedLifetime();
		}

		public boolean reactsToUse() {
			return this == PREVENTUSE || this == DROPONUSE || this == DELETEONUSE;
		}

		public boolean reactsToTick() {
			return this == DELETEONHOLD;
		}

		private boolean givesProgress() {
			return this.ordinal() >= DROPONUSE.ordinal();
		}
	}
}
