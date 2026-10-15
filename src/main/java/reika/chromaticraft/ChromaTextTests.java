package reika.chromaticraft;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.auxiliary.render.ChromaText;
import reika.chromaticraft.magic.progression.*;
import reika.chromaticraft.registry.*;

/** Real-player unlock/relock contracts and common-side component checks for discovery text. */
final class ChromaTextTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event, env, "text_glyph_components", 100, ChromaTextTests::glyphs);
        ChromaGameTests.register(event, env, "text_tier_unlock", 100, ChromaTextTests::tiers);
        ChromaGameTests.register(event, env, "text_color_and_boosted_unlock", 100, ChromaTextTests::colors);
        ChromaGameTests.register(event, env, "text_research_unlock", 100, ChromaTextTests::research);
        ChromaGameTests.register(event, env, "text_ore_disguise", 100, ChromaTextTests::ore);
        ChromaGameTests.register(event, env, "text_fragment_readability", 100, ChromaTextTests::fragments);
    }

    private static void glyphs(GameTestHelper h) {
        var text = Component.literal("Unknown ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Crystal\nPower").withStyle(ChatFormatting.BOLD));
        var hidden = ChromaText.scramble(text);
        h.assertTrue(!hidden.getString().contains("Unknown") && !hidden.getString().contains("Crystal"),
                "concealed components must not leak their readable source when flattened");
        h.assertTrue(hidden.getString().charAt(7) == ' ' && hidden.getString().charAt(15) == '\n',
                "spaces and line breaks must retain word-wrap boundaries");
        h.assertTrue(ChromaText.scramble(hidden) == hidden, "tooltip passes must not rescramble a name");
        h.assertTrue(!ChromaText.scramble(hidden.copy().append(" Secret")).getString().contains("Secret"),
                "an already scrambled sibling must not exempt a readable sibling from concealment");
        h.assertTrue(ChromaText.conceal(text, false) == text, "readable text must keep its original component");
        hidden.visit((style, part) -> {
            if (part.isEmpty()) return java.util.Optional.empty();
            h.assertTrue(style.getFont().equals(new FontDescription.Resource(ChromaText.FONT)), "every hidden run needs the glyph font");
            if (part.contains("\n")) h.assertTrue(style.isBold(), "bold on a sibling run must survive");
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        var relic = new ItemStack(ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get());
        java.util.List<Component> lines = new java.util.ArrayList<>();
        relic.getItem().appendHoverText(relic, net.minecraft.world.item.Item.TooltipContext.of(h.getLevel()),
                net.minecraft.world.item.component.TooltipDisplay.DEFAULT, lines::add,
                net.minecraft.world.item.TooltipFlag.NORMAL);
        h.assertTrue(lines.size() == 1 && lines.getFirst().getString().codePoints().allMatch(c ->
                        Character.isWhitespace(c) || c >= ChromaText.GLYPH_BASE && c < ChromaText.GLYPH_BASE + 256),
                "the Unknown Artefact's scrolling text must use the original glyphs");
        h.succeed();
    }

    private static void tiers(GameTestHelper h) {
        var p = ChromaGameTests.joinedCreativeMockPlayer(h);
        var stack = ChromaItems.tieredStack(ChromaTieredItems.CHROMA_DUST);
        h.assertTrue(ChromaNameKnowledge.hidden(stack, p), "fresh player must not read tiered resources");
        ProgressionManager.instance.setPlayerStage(p, ProgressStage.CRYSTALS, true, false, false);
        h.assertTrue(!ChromaNameKnowledge.hidden(stack, p), "discovery must reveal the same existing stack");
        ProgressionManager.instance.setPlayerStage(p, ProgressStage.CRYSTALS, false, false, false);
        h.assertTrue(ChromaNameKnowledge.hidden(stack, p), "revoking progression must hide the existing stack again");
        h.assertTrue(!ChromaNameKnowledge.hidden(stack, null), "startup with no player must leave registry names readable");
        h.assertTrue(!ChromaNameKnowledge.hidden(new ItemStack(Items.DIAMOND), p), "vanilla names must stay readable");
        h.succeed();
    }

    private static void colors(GameTestHelper h) {
        var p = ChromaGameTests.joinedCreativeMockPlayer(h);
        var color = CrystalElement.RED;
        var plain = ChromaItems.shardStack(color);
        var boosted = ChromaItems.boostedShardStack(color, 1);
        h.assertTrue(ChromaNameKnowledge.hidden(plain, p), "undiscovered crystal color must be unreadable");
        ProgressionManager.instance.setPlayerDiscoveredColor(p, color, true, false);
        h.assertTrue(!ChromaNameKnowledge.hidden(plain, p), "color discovery must reveal ordinary shards");
        h.assertTrue(ChromaNameKnowledge.hidden(boosted, p), "color alone must not reveal boosted shards");
        ProgressionManager.instance.setPlayerStage(p, ProgressStage.RUNEUSE, true, false, false);
        h.assertTrue(!ChromaNameKnowledge.hidden(boosted, p), "boosted shards require both color and RUNEUSE");
        ProgressionManager.instance.setPlayerDiscoveredColor(p, color, false, false);
        h.assertTrue(ChromaNameKnowledge.hidden(boosted, p), "stage alone must not reveal boosted shards");
        h.succeed();
    }

    private static void research(GameTestHelper h) {
        var p = ChromaGameTests.joinedCreativeMockPlayer(h);
        var core = ChromaItems.craftingStack(ChromaCraftingItems.ENERGY_CORE);
        var raw = ChromaItems.craftingStack(ChromaCraftingItems.RAW_CRYSTAL);
        h.assertTrue(ChromaNameKnowledge.hidden(core, p), "cores need the CORES fragment");
        h.assertTrue(!ChromaNameKnowledge.hidden(raw, p), "ordinary crafting ingredients must not acquire invented gates");
        var page = LexiconCatalog.byId("CORES");
        PlayerResearch.giveFragment(p, page, false);
        h.assertTrue(!ChromaNameKnowledge.hidden(core, p), "learning CORES must reveal the core");
        PlayerResearch.removeFragment(p, page);
        h.assertTrue(ChromaNameKnowledge.hidden(core, p), "forgetting CORES must conceal its name");
        h.succeed();
    }

    private static void ore(GameTestHelper h) {
        var p = ChromaGameTests.joinedCreativeMockPlayer(h);
        var stack = new ItemStack(ChromaBlocks.ENERGIZED_ROCK.get());
        h.assertTrue(ChromaNameKnowledge.disguise(stack, p) == net.minecraft.world.level.block.Blocks.STONE,
                "undiscovered chroma ore must take the host's localized name");
        ProgressionManager.instance.setPlayerStage(p, ProgressStage.CRYSTALS, true, false, false);
        h.assertTrue(ChromaNameKnowledge.disguise(stack, p) == null, "discovery must expose the ore's real name");
        h.succeed();
    }

    private static void fragments(GameTestHelper h) {
        var p = ChromaGameTests.joinedCreativeMockPlayer(h);
        var candidates = PlayerResearch.nextResearch(p);
        h.assertTrue(!candidates.isEmpty(), "a fresh player must have eligible research");
        var level = reika.chromaticraft.magic.progression.ResearchProgress.getLevel(p);
        for (var page : LexiconCatalog.obtainablePages()) {
            h.assertTrue(PlayerResearch.canStepTo(p, page) == candidates.contains(page),
                    "read-only fragment eligibility must match next research for " + page.id());
        }
        var cores = LexiconCatalog.byId("CORES");
        h.assertTrue(!ChromaNameKnowledge.canReadFragment(p, cores), "future fragment titles must be concealed");
        h.assertTrue(reika.chromaticraft.magic.progression.ResearchProgress.getLevel(p) == level,
                "checking tooltip eligibility must not advance research");
        PlayerResearch.giveFragment(p, cores, false);
        h.assertTrue(ChromaNameKnowledge.canReadFragment(p, cores), "owned fragment titles must be readable");
        h.succeed();
    }
}
