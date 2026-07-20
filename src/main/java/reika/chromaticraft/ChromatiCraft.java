package reika.chromaticraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.magic.potions.PotionBetterSaturation;
import reika.chromaticraft.magic.potions.PotionCustomRegen;
import reika.chromaticraft.registry.ChromaBlockEntities;
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

	public static final DeferredRegister<MobEffect> MOB_EFFECTS =
			DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MODID);

	public static final DeferredHolder<MobEffect, MobEffect> betterRegen = MOB_EFFECTS.register("regeneration",
			() -> new PotionCustomRegen(MobEffectCategory.BENEFICIAL, 0xCD5CAB));
	public static final DeferredHolder<MobEffect, MobEffect> betterSat = MOB_EFFECTS.register("saturation",
			() -> new PotionBetterSaturation(MobEffectCategory.BENEFICIAL, 0xA55926));

	public ChromatiCraft(IEventBus modEventBus, ModContainer modContainer) {
		instance = this;

		ChromaBlocks.BLOCKS.register(modEventBus);
		ChromaBlocks.ITEMS.register(modEventBus);
		ChromaItems.ITEMS.register(modEventBus);
		ChromaBlockEntities.BLOCK_ENTITIES.register(modEventBus);
		ChromaTabs.CREATIVE_MODE_TABS.register(modEventBus);
		MOB_EFFECTS.register(modEventBus);

		// In-world game tests (progression core). Runnable via `gradlew :ChromatiCraft:runGameTest`.
		modEventBus.addListener(ChromaGameTests::onRegisterGameTests);
		ChromaGameTests.TEST_INSTANCE_TYPES.register(modEventBus);

		// Force-load the progression singleton so it wires ProgressionAPI.instance.progressManager
		// (consumed by CrystalElement.playerHas and others) before any gameplay query.
		reika.chromaticraft.magic.progression.ProgressionManager.init();
	}
}
