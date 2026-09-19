package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;

/** Runtime-semantic tags for ChromatiCraft blocks whose behaviour depends on vanilla families. */
public final class ChromaBlockTagsProvider extends BlockTagsProvider {
    public ChromaBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, ChromatiCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var leaves = tag(BlockTags.LEAVES);
        ChromaBlocks.DYE_LEAVES.forEach(block -> leaves.add(block.getKey()));
        leaves.add(ChromaBlocks.RAINBOW_LEAVES.getKey(), ChromaBlocks.GLOWING_LEAVES.getKey());
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
    }
}
