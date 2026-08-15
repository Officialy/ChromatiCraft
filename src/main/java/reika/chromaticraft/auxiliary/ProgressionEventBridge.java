package reika.chromaticraft.auxiliary;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/**
 * The progression-only event hooks formerly hosted in V33a's very broad
 * {@code ChromaticEventManager}.
 *
 * <p>Keeping this as a narrow modern listener restores the parts of the ordinary gameplay path
 * that must exist before a player can legitimately satisfy the Proxima portal prerequisites,
 * without dragging the old manager's unrelated protection, integration, and rendering branches
 * into the build. Each listener is a direct transcription of the corresponding V33a method.
 */
public final class ProgressionEventBridge {

	private static final double BOSS_CREDIT_RANGE_SQ = 384D * 384D;

	private ProgressionEventBridge() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ProgressionEventBridge::onBlockDrops);
		NeoForge.EVENT_BUS.addListener(ProgressionEventBridge::onDimensionChange);
		NeoForge.EVENT_BUS.addListener(ProgressionEventBridge::onLivingDeath);
		NeoForge.EVENT_BUS.addListener(ProgressionEventBridge::onEffectAdded);
		NeoForge.EVENT_BUS.addListener(ProgressionEventBridge::onBlockBreak);
		NeoForge.EVENT_BUS.addListener(ProgressionEventBridge::onVillagerTrade);
	}

	/** V33a {@code addBlockBreakProgress}: ore mining and crop harvesting. */
	private static void onBlockDrops(BlockDropsEvent event) {
		if (!(event.getBreaker() instanceof Player player) || ReikaPlayerAPI.isFake(player))
			return;
		if (event.getState().is(Tags.Blocks.ORES))
			ProgressStage.MINE.stepPlayerTo(player);
		if (event.getState().is(BlockTags.CROPS))
			ProgressStage.HARVEST.stepPlayerTo(player);
	}

	/** V33a {@code onDimensionChange}: entering vanilla's Nether or End is the trigger. */
	private static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getTo() == Level.NETHER)
			ProgressStage.NETHER.stepPlayerTo(event.getEntity());
		else if (event.getTo() == Level.END)
			ProgressStage.END.stepPlayerTo(event.getEntity());
		// The old Twilight Forest branch remains conditional on that integration being ported.
	}

	/** V33a {@code triggerBossProgress} plus its energy-buffer death milestone. */
	private static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player dead
				&& PlayerElementBuffer.instance.getPlayerTotalEnergy(dead) >= 90_000)
			ProgressStage.DIE.stepPlayerTo(dead);

		Entity source = event.getSource().getEntity();
		if (!(source instanceof Player player) || source.level().dimension() != event.getEntity().level().dimension()
				|| source.distanceToSqr(event.getEntity()) > BOSS_CREDIT_RANGE_SQ)
			return;
		if (event.getEntity() instanceof EnderDragon)
			ProgressStage.KILLDRAGON.stepPlayerTo(player);
		else if (event.getEntity() instanceof WitherBoss)
			ProgressStage.KILLWITHER.stepPlayerTo(player);
		if (event.getEntity() instanceof Enemy)
			ProgressStage.KILLMOB.stepPlayerTo(player);
	}

	/** V33a {@code ApplyPotionEvent}; any newly applied effect records the potion milestone. */
	private static void onEffectAdded(MobEffectEvent.Added event) {
		if (event.getEntity() instanceof Player player)
			ProgressStage.POTION.stepPlayerTo(player);
	}

	/** V33a {@code harvestSpawner}: breaking a discovered vanilla spawner records the milestone. */
	private static void onBlockBreak(BreakBlockEvent event) {
		if (!event.getLevel().isClientSide() && event.getState().is(Blocks.SPAWNER)
				&& !ReikaPlayerAPI.isFake(event.getPlayer()))
			ProgressStage.BREAKSPAWNER.stepPlayerTo(event.getPlayer());
	}

	/**
	 * V33a {@code buyFocusCrystals}: the stage is earned by completing the villager purchase, not by
	 * merely crafting, placing, or looking at a focus crystal.  The data-driven offer itself belongs
	 * to the village/trade milestone; this listener already preserves its authoritative grant site.
	 */
	private static void onVillagerTrade(TradeWithVillagerEvent event) {
		if (FocusCrystalTradeHandler.isFocusCrystalOffer(event.getMerchantOffer())) {
			// V33a FocusCrystalTrade.incrementToolUses is a no-op.  Resetting the modern
			// offer immediately preserves that behavior even across the theoretical int cap.
			event.getMerchantOffer().resetUses();
			ProgressStage.FOCUSCRYSTAL.stepPlayerTo(event.getEntity());
		}
	}
}
