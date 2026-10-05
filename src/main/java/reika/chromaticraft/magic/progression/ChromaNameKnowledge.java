package reika.chromaticraft.magic.progression;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import reika.chromaticraft.auxiliary.interfaces.TieredItem;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.block.worldgen26.BlockTieredOre;
import reika.chromaticraft.block.worldgen26.BlockTieredPlant;
import reika.chromaticraft.item.ItemChromaBerry;
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.item.ItemElementalStone;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/** The V33a item-name gates, independent of client classes and evaluated against live player data. */
public final class ChromaNameKnowledge {
    private ChromaNameKnowledge() {}

    public static boolean hidden(ItemStack stack, Player player) {
        if (player == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        if (item instanceof TieredItem tier && tier.isTiered(stack)) {
            ProgressStage stage = tier.getDiscoveryTier(stack);
            if (stage == null || !stage.isPlayerAtStage(player)) return true;
        }
        if (item instanceof ItemCrystalShard shard && shard.boosted()
                && !ProgressStage.RUNEUSE.isPlayerAtStage(player)) return true;
        if (item instanceof ItemElementalStone && !ProgressStage.RUNEUSE.isPlayerAtStage(player)) return true;
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockTieredPlant plant
                && !plant.getPlant().stage().isPlayerAtStage(player)) return true;
        String research = requiredFragment(stack);
        if (research != null && !PlayerResearch.hasFragment(player, LexiconCatalog.byId(research))) return true;
        CrystalElement color = color(stack);
        return color != null && !ProgressionManager.instance.hasPlayerDiscoveredColor(player, color);
    }

    /** Ore names use the host's localized name, not scrambled ore glyphs. */
    public static Block disguise(ItemStack stack, Player player) {
        return player != null && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof BlockTieredOre ore
                && !ore.getProgressStage().isPlayerAtStage(player) ? ore.getDisguise() : null;
    }

    private static String requiredFragment(ItemStack stack) {
        for (var item : ChromaItems.CLUSTERS.values()) if (stack.is(item.get())) return "GROUPS";
        for (var entry : ChromaItems.CRAFTING.entrySet()) {
            if (!stack.is(entry.getValue().get())) continue;
            return switch (entry.getKey()) {
                case VOID_CORE, ENERGY_CORE, TRANSFORMATION_CORE, CRYSTAL_FOCUS, CRYSTAL_LENS -> "CORES";
                case CHROMA_INGOT, FIERY_INGOT, ENDER_INGOT, WATER_INGOT, CONDUCTIVE_INGOT,
                        AURA_INGOT, SPACE_INGOT, COMPLEX_INGOT, EXPERIENCE_GEM -> "ALLOYS";
                default -> null;
            };
        }
        return null;
    }

    /** Only families whose original ItemCrystalBasic/colored block-item supplied this gate. */
    public static CrystalElement color(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof ItemCrystalShard shard) return shard.element();
        if (item instanceof ItemChromaBerry berry) return berry.element();
        if (item instanceof ItemElementalStone stone) return stone.element();
        if (item instanceof reika.chromaticraft.items.ItemCrystalSeeds seeds) return seeds.getElement();
        if (item instanceof reika.chromaticraft.items.tools.ItemPendant pendant) return pendant.getColor();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof CrystalBlock crystal) return crystal.getCrystalElement(block.defaultBlockState());
            if (block instanceof CrystalTypeBlock crystal) return crystal.getCrystalElement();
            if (block instanceof reika.chromaticraft.block.BlockCrystalPlant plant) return plant.getElement();
            for (CrystalElement color : CrystalElement.elements) {
                int index = color.ordinal();
                if (block == ChromaBlocks.RUNES.get(index).get()
                        || block == ChromaBlocks.DYE_LEAVES.get(index).get()
                        || block == ChromaBlocks.DYE_SAPLINGS.get(index).get()
                        || block == ChromaBlocks.DYE_FLOWERS.get(index).get()
                        || block == ChromaBlocks.DYE_VINES.get(index).get()
                        || block == ChromaBlocks.FERTILE_DYE_VINES.get(index).get()) return color;
            }
        }
        return null;
    }

    public static boolean canReadFragment(Player player, LexiconCatalog.Entry page) {
        return PlayerResearch.hasFragment(player, page) || PlayerResearch.canStepTo(player, page);
    }

    /** V33a canRunRecipe(null, player): the inherited casting tiers plus the authored recipe gates. */
    public static boolean recipeStagesKnown(CastingTableRecipe recipe, Player player) {
        if (!ProgressStage.CRYSTALS.isPlayerAtStage(player) || !recipe.minimumResearch().playerHas(player)) return false;
        if (recipe.tier().ordinal() >= CastingTableRecipe.Tier.TEMPLE.ordinal()
                && !ProgressStage.RUNEUSE.isPlayerAtStage(player)) return false;
        if (recipe.tier().ordinal() >= CastingTableRecipe.Tier.MULTIBLOCK.ordinal()
                && !ProgressStage.MULTIBLOCK.isPlayerAtStage(player)) return false;
        if (recipe.tier() == CastingTableRecipe.Tier.PYLON
                && (!ProgressStage.PYLON.isPlayerAtStage(player) || !ProgressStage.REPEATER.isPlayerAtStage(player))) return false;
        return recipe.requiredProgress().stream().allMatch(stage -> stage.isPlayerAtStage(player));
    }
}
