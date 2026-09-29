package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.level.LegacyMotionTags;

/** Runtime-semantic tags for ChromatiCraft blocks whose behaviour depends on vanilla families. */
public final class ChromaBlockTagsProvider extends BlockTagsProvider {
    public ChromaBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, ChromatiCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 26.3 made movement blocking, suffocation, fluid blocking and fluid washing tag-driven and
        // NeoForge tags no modded blocks; give every block its 26.2 behaviour (see LegacyMotionTags).
        var motionTag = tag(BlockTags.BLOCKS_MOTION_NO_LEAVES);
        var leafTag = tag(BlockTags.LEAVES);
        var washedTag = tag(BlockTags.WASHED_AWAY_BY_FLUIDS);
        // Every leaf block (dye, rainbow and glowing leaves) is a LeavesBlock, so the classifier already puts it in
        // #minecraft:leaves; listing them again only duplicated the entries.
        LegacyMotionTags.classifyEntries(ChromaBlocks.BLOCKS.getEntries(), motionTag::add, leafTag::add, washedTag::add);

        tag(BlockTags.SMALL_FLOWERS).add(ChromaBlocks.GLOW_DAISY.getKey());

		// Tool speed is tag-driven in 26.2. Hardness alone left every ported stone/crystal machine
		// mining at bare-hand speed even with a netherite pickaxe, so restore the material families
		// represented by the active registry slice instead of fixing only the first reported block.
		var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
		pickaxe.add(ChromaBlocks.CLIFF_STONE.getKey(),
				ChromaBlocks.ENERGIZED_ROCK.getKey(), ChromaBlocks.ELEMENTAL_STONES.getKey(),
				ChromaBlocks.FUSED_CRYSTALS.getKey(), ChromaBlocks.RADIANT_STONE.getKey(),
				ChromaBlocks.ENDER_STONE.getKey(), ChromaBlocks.FLUID_STONE.getKey(),
				ChromaBlocks.FIRAXITE.getKey(), ChromaBlocks.VIBRANT_CRYSTALS.getKey(),
				ChromaBlocks.SPACERIFT_STONE.getKey(), ChromaBlocks.GLOWING_ROCK.getKey(),
				ChromaBlocks.ECHOSTONE.getKey(), ChromaBlocks.LUMENITE_ORE.getKey(),
				ChromaBlocks.FIRESTONE.getKey(), ChromaBlocks.THERMITIC_ROCK.getKey(),
				ChromaBlocks.AVOLITE_ORE.getKey(),
				ChromaBlocks.WARP_NODE.getKey(),
				ChromaBlocks.UNKNOWN_ARTEFACT.getKey(), ChromaBlocks.CAVE_INDICATOR.getKey(),
				ChromaBlocks.STORAGE.getKey(), ChromaBlocks.DISPLAY_POINT.getKey(),
				ChromaBlocks.PYLON.getKey(), ChromaBlocks.REPEATER.getKey(), ChromaBlocks.WEAK_REPEATER.getKey(),
				ChromaBlocks.RELAY_SOURCE.getKey(),
				ChromaBlocks.FUNCTION_RELAY.getKey(),
				ChromaBlocks.SKYPEATER.getKey(), ChromaBlocks.CREATIVEPYLON.getKey(),
				ChromaBlocks.COMPOUND.getKey(), ChromaBlocks.ITEM_STAND.getKey(),
				ChromaBlocks.CASTING_TABLE.getKey(), ChromaBlocks.RITUAL_TABLE.getKey(), ChromaBlocks.DATA_NODE.getKey(),
				ChromaBlocks.FOCUS_CRYSTAL.getKey(), ChromaBlocks.PYLON_LINK.getKey(),
				ChromaBlocks.POWER_CRYSTAL.getKey());
		pickaxe.add(ChromaBlocks.CRYSTAL_CHARGER.getKey());
		pickaxe.add(ChromaBlocks.CRYSTAL_BREWER.getKey());
		pickaxe.add(ChromaBlocks.LAUNCH_PAD.getKey());
		pickaxe.add(ChromaBlocks.PARTICLE_SPAWNER.getKey());
		pickaxe.add(ChromaBlocks.LUMEN_TURRET.getKey());
		pickaxe.add(ChromaBlocks.LUMEN_WIRE.getKey());
		pickaxe.add(ChromaBlocks.FLUID_RELAY.getKey());
		pickaxe.add(ChromaBlocks.AUTO_ENCHANTER.getKey());
		pickaxe.add(ChromaBlocks.TOOL_STORAGE.getKey());
		pickaxe.add(ChromaBlocks.PYLON_TURBOCHARGER.getKey());
		pickaxe.add(ChromaBlocks.COLLECTOR.getKey());
		pickaxe.add(ChromaBlocks.ITEM_INFUSER.getKey());
		pickaxe.add(ChromaBlocks.PLAYER_INFUSER.getKey());
		pickaxe.add(ChromaBlocks.PERSONAL_CHARGER.getKey());
		ChromaBlocks.LUMEN_RELAYS.values().forEach(block -> pickaxe.add(block.getKey()));
		pickaxe.add(ChromaBlocks.MULTICHROMIC_RELAY.getKey());
		pickaxe.add(ChromaBlocks.TRAP_FLOOR.getKey(), ChromaBlocks.SHIFT_LOCK.getKey(),
				ChromaBlocks.LOCK_KEY.getKey(), ChromaBlocks.COLOR_LOCK.getKey(),
				ChromaBlocks.LIGHT_PANEL.getKey(), ChromaBlocks.PANEL_SWITCH.getKey(),
				ChromaBlocks.MUSIC_TRIGGER.getKey());
		pickaxe.add(ChromaBlocks.BIOME_REPLAY.getKey());
		pickaxe.add(ChromaBlocks.MUSIC_MEMORY.getKey());
		pickaxe.add(ChromaBlocks.GOL_TILE.getKey(), ChromaBlocks.GOL_CONTROLLER.getKey());
		ChromaBlocks.SHIELDING.values().forEach(block -> pickaxe.add(block.getKey()));
		// V33a requiresPickaxe is only true for some of the decoration; the rest breaks by hand, so
		// only those go in the pickaxe tag.
		for (reika.chromaticraft.registry.ProximaDecoTypes type
				: reika.chromaticraft.registry.ProximaDecoTypes.list)
			if (type.requiresPickaxe())
				pickaxe.add(ChromaBlocks.deco(type).getKey());
		tag(BlockTags.LOGS).add(ChromaBlocks.GLOW_LOG.getKey());
		tag(BlockTags.MINEABLE_WITH_AXE).add(ChromaBlocks.GLOW_LOG.getKey());
		// V33a isBeaconBase: Floatstone alone, which 26.2 expresses as a tag rather than a method.
		tag(BlockTags.BEACON_BASE_BLOCKS).add(
				ChromaBlocks.deco(reika.chromaticraft.registry.ProximaDecoTypes.FLOATSTONE).getKey());
		ChromaBlocks.CAVE_CRYSTALS.forEach(block -> pickaxe.add(block.getKey()));
		ChromaBlocks.CRYSTAL_LAMPS.forEach(block -> pickaxe.add(block.getKey()));
		ChromaBlocks.SUPER_CRYSTALS.forEach(block -> pickaxe.add(block.getKey()));
		ChromaBlocks.ENCRUSTED_CRYSTALS.forEach(block -> pickaxe.add(block.getKey()));
		ChromaBlocks.CRYSTALLINE_STONE.forEach(block -> pickaxe.add(block.getKey()));
		ChromaBlocks.RUNES.forEach(block -> pickaxe.add(block.getKey()));

		tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ChromaBlocks.MUD.getKey(),
				ChromaBlocks.CLIFF_DIRT.getKey(), ChromaBlocks.CLIFF_GRASS.getKey(),
				ChromaBlocks.CLIFF_FARMLAND.getKey());
		var hoe = tag(BlockTags.MINEABLE_WITH_HOE);
		ChromaBlocks.DYE_LEAVES.forEach(block -> hoe.add(block.getKey()));
		hoe.add(ChromaBlocks.RAINBOW_LEAVES.getKey(), ChromaBlocks.GLOWING_LEAVES.getKey());
		tag(BlockTags.MINEABLE_WITH_AXE).add(ChromaBlocks.LOOT_CHEST.getKey());
		// Sparkling blocks mine at their proxy's speed (BlockSparkle.getDestroyProgress delegates), so
		// the tool tags simply follow the proxy's. They never need a correct tool to drop.
		// Tags are unbound during datagen, so the proxy's own tags cannot be queried here.
		ChromaBlocks.SPARKLE.forEach((type, block) -> {
			switch (type) {
				case COBBLE, STONE, BRICKS, OBSIDIAN -> pickaxe.add(block.getKey());
				case DIRT, SAND, GRAVEL, CLAY -> tag(BlockTags.MINEABLE_WITH_SHOVEL).add(block.getKey());
			}
		});
		// V33a BlockDyeFlower is a BlockFlower; it joins the flowers the way vanilla's own do.
		ChromaBlocks.DYE_FLOWERS.forEach(block -> tag(BlockTags.SMALL_FLOWERS).add(block.getKey()));
    }
}
