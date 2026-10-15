package reika.chromaticraft;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.magic.progression.ChromaAdvancements;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.ChromaItems;

final class ChromaAdvancementTests {
    private ChromaAdvancementTests() {}

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event, env, "advancement_catalog", 80, ChromaAdvancementTests::catalog);
        ChromaGameTests.register(event, env, "advancement_login_lexicon", 80, ChromaAdvancementTests::login);
        ChromaGameTests.register(event, env, "advancement_progression_mirror", 80, ChromaAdvancementTests::progress);
        ChromaGameTests.register(event, env, "advancement_saved_progression_backfill", 80, ChromaAdvancementTests::backfill);
        ChromaGameTests.register(event, env, "advancement_research_reset", 80, ChromaAdvancementTests::reset);
        ChromaGameTests.register(event, env, "advancement_electricraft_items", 80, ChromaAdvancementTests::electricraft);
        ChromaGameTests.register(event, env, "advancement_geostrata_collection", 80, ChromaAdvancementTests::geostrata);
    }

    private static boolean done(ServerPlayer player, ProgressStage stage) {
        var id = reika.chromaticraft.data.ChromaAdvancementProvider.id(stage);
        var advancement = player.level().getServer().getAdvancements().get(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void catalog(GameTestHelper h) {
        for (var stage : ProgressStage.list) {
            if (stage == ProgressStage.NEVER) continue;
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(reika.chromaticraft.data.ChromaAdvancementProvider.id(stage)) != null,
                    "authored progression advancement " + stage);
        }
        h.succeed();
    }

    private static void login(GameTestHelper h) {
        var player = ChromaGameTests.joinedCreativeMockPlayer(h);
        int books = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(ChromaItems.LEXICON.get())) books += player.getInventory().getItem(i).getCount();
        h.assertTrue(books == 1, "the real login event delivers the Lexicon once");
        h.assertTrue(!done(player, ProgressStage.CTM), "a starter book cannot award completed research");
        h.succeed();
    }

    private static void progress(GameTestHelper h) {
        var player = ChromaGameTests.joinedCreativeMockPlayer(h);
        ProgressionManager.instance.setPlayerStage(player, ProgressStage.CASTING, true, false, false);
        h.assertTrue(done(player, ProgressStage.CASTING), "grant the selected research stage");
        h.assertTrue(done(player, ProgressStage.CRYSTALS), "grant the automatically recorded prerequisite too");
        ProgressionManager.instance.setPlayerStage(player, ProgressStage.CRYSTALS, false, false, false);
        h.assertTrue(!done(player, ProgressStage.CASTING) && !done(player, ProgressStage.CRYSTALS), "stage removal revokes all removed descendants");
        h.succeed();
    }

    private static void backfill(GameTestHelper h) {
        var player = ChromaGameTests.joinedCreativeMockPlayer(h);
        ProgressionManager.instance.setPlayerStage(player, ProgressStage.CRYSTALS, true, false, false);
        reika.dragonapi.libraries.AdvancementHelper.revoke(player, reika.chromaticraft.data.ChromaAdvancementProvider.id(ProgressStage.CRYSTALS));
        h.assertTrue(!done(player, ProgressStage.CRYSTALS), "simulate a save made before advancement data existed");
        ChromaAdvancements.sync(player);
        h.assertTrue(done(player, ProgressStage.CRYSTALS), "saved progression backfills advancement state");
        h.succeed();
    }

    private static void reset(GameTestHelper h) {
        var player = ChromaGameTests.joinedCreativeMockPlayer(h);
        ProgressionManager.instance.setPlayerStage(player, ProgressStage.CRYSTALS, true, false, false);
        ProgressionManager.instance.resetPlayerProgression(player, false);
        h.assertTrue(!done(player, ProgressStage.CRYSTALS), "the direct stage-tag reset must revoke its advancement");
        h.succeed();
    }

    private static boolean done(ServerPlayer player, String id) {
        var advancement = player.level().getServer().getAdvancements().get(Identifier.parse(id));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void obtain(ServerPlayer player, net.minecraft.world.level.ItemLike item) {
        var stack = new net.minecraft.world.item.ItemStack(item);
        player.getInventory().setItem(0, stack);
        net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
    }

    private static void electricraft(GameTestHelper h) {
        var player = ChromaGameTests.joinedCreativeMockPlayer(h);
        var book = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse("electricraft:electribook")).orElseThrow();
        obtain(player, book);
        h.assertTrue(done(player, "electricraft:root"), "handbook inventory criterion");
        h.assertTrue(!done(player, "electricraft:read_handbook"), "book possession is not reading");
        book.use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(done(player, "electricraft:read_handbook"), "read the ElectriCraft book on the server");
        obtain(player, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(Identifier.parse("electricraft:copper_wire")).orElseThrow());
        h.assertTrue(done(player, "electricraft:copper_wire") && !done(player, "electricraft:star_battery"), "inventory milestones match their actual items");
        h.succeed();
    }

    private static void geostrata(GameTestHelper h) {
        var player = ChromaGameTests.joinedCreativeMockPlayer(h);
        var granite = reika.geostrata.registry.RockTypes.GRANITE;
        obtain(player, reika.geostrata.registry.RockShapes.COBBLE.getBlock(granite));
        h.assertTrue(done(player, "geostrata:root") && done(player, "geostrata:rocks/granite"), "cobblestone satisfies the rock criterion");
        h.assertTrue(!done(player, "geostrata:all_rocks"), "one rock cannot complete the survey");
        for (var rock : reika.geostrata.registry.RockTypes.rockList)
            obtain(player, reika.geostrata.registry.RockShapes.SMOOTH.getBlock(rock));
        h.assertTrue(done(player, "geostrata:all_rocks"), "all distinct rock criteria accumulate over time");
        obtain(player, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse("geostrata:granite_connected_connected")));
        h.assertTrue(done(player, "geostrata:masonry/connected"), "connected masonry uses the real registered block");
        h.succeed();
    }
}
