package reika.chromaticraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker;
import reika.dragonapi.base.BlockTieredResource;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.libraries.level.ReikaBlockHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/** Native harvesting and helper integration for the shared tiered-resource contract. */
final class ChromaTieredResourceTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event, env, "tiered_contract_native_ore", 80, ChromaTieredResourceTests::nativeOre);
        ChromaGameTests.register(event, env, "tiered_contract_native_plant", 80, ChromaTieredResourceTests::nativePlant);
        ChromaGameTests.register(event, env, "tiered_contract_placement", 80, ChromaTieredResourceTests::placement);
        ChromaGameTests.register(event, env, "tiered_contract_loot", 80, ChromaTieredResourceTests::loot);
        ChromaGameTests.register(event, env, "tiered_contract_drop_event", 80, ChromaTieredResourceTests::dropEvent);
        ChromaGameTests.register(event, env, "tiered_contract_recursive", 80, ChromaTieredResourceTests::recursive);
    }

    private static ServerPlayer survival(GameTestHelper h) {
        return connectedPlayer(h, GameType.SURVIVAL);
    }

    private static ServerPlayer connectedPlayer(GameTestHelper h, GameType type) {
        return ChromaGameTests.joinedMockPlayer(h, type);
    }

    private static void grant(Player player, ProgressStage stage) {
        ProgressionManager.instance.setPlayerStage(player, stage, true, false, false);
    }

    private static ItemStack enchanted(GameTestHelper h, Item item,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment, int level) {
        ItemStack tool = new ItemStack(item);
        tool.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);
        return tool;
    }

    private static BlockPos place(GameTestHelper h, Block block, int x) {
        BlockPos pos = h.absolutePos(new BlockPos(x, 3, 3));
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        return pos;
    }

    private static int items(GameTestHelper h, BlockPos pos, Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), e -> e.getItem().is(item))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static List<ExperienceOrb> xp(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(1));
    }

    private static void nativeOre(GameTestHelper h) {
        ServerPlayer player = survival(h);
        var ore = ChromaBlocks.VIBRANT_CRYSTALS.get();
        Item resource = ChromaItems.TIERED.get(ChromaTieredItems.RESONANCE_DUST).get();
        BlockPos hidden = place(h, ore, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(h, Items.DIAMOND_PICKAXE, Enchantments.SILK_TOUCH, 1));
        h.assertTrue(player.gameMode.destroyBlock(hidden) && h.getLevel().getBlockState(hidden).isAir(), "native survival removes hidden ore");
        h.assertTrue(items(h, hidden, Items.END_STONE) == 1 && items(h, hidden, resource) == 0 && xp(h, hidden).isEmpty(),
                "insufficient-tier Silk Touch yields only the host block, without tier resources or XP");
        grant(player, ProgressStage.ABILITY);
        BlockPos visible = place(h, ore, 8);
        ItemStack tool = enchanted(h, Items.DIAMOND_PICKAXE, Enchantments.FORTUNE, 3);
        tool.setDamageValue(tool.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        h.assertTrue(player.gameMode.destroyBlock(visible), "native survival harvest succeeds on the tool's last use");
        h.assertTrue(player.getMainHandItem().isEmpty() && items(h, visible, resource) == 13
                && items(h, visible, ore.asItem()) == 0 && !xp(h, visible).isEmpty(),
                "the pre-damage Fortune III snapshot yields exactly 13 resources once and sufficient-tier XP");
        BlockPos silk = place(h, ore, 13);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(h, Items.DIAMOND_PICKAXE, Enchantments.SILK_TOUCH, 1));
        h.assertTrue(player.gameMode.destroyBlock(silk) && items(h, silk, resource) == 1 && items(h, silk, ore.asItem()) == 0,
                "sufficient-tier Silk Touch still yields the source resource instead of the ore block");
        h.succeed();
    }

    private static void nativePlant(GameTestHelper h) {
        ServerPlayer player = survival(h);
        var plant = ChromaBlocks.tieredPlant(ChromaTieredPlants.ESSENCE_LILY).get();
        Item dust = ChromaItems.TIERED.get(ChromaTieredItems.ELEMENT_DUST).get();
        BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3));
        h.getLevel().setBlock(pos.below(), Blocks.WATER.defaultBlockState(), 3);
        h.getLevel().setBlock(pos, plant.defaultBlockState(), 3);
        h.assertTrue(player.gameMode.destroyBlock(pos) && items(h, pos, dust) == 0 && xp(h, pos).isEmpty(),
                "even a forced insufficient-tier plant break has no drops or XP");
        grant(player, ProgressStage.CHARGE);
        h.getLevel().setBlock(pos, plant.defaultBlockState(), 3);
        ItemStack sword = enchanted(h, Items.DIAMOND_SWORD, Enchantments.LOOTING, 3);
        sword.setDamageValue(sword.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        h.assertTrue(player.gameMode.destroyBlock(pos) && items(h, pos, dust) == 20 && !xp(h, pos).isEmpty(),
                "native plant harvest retains max(Fortune,Looting), the exact lily formula and original XP");
        BlockPos creative = pos.east(5);
        h.getLevel().setBlock(creative.below(), Blocks.WATER.defaultBlockState(), 3);
        h.getLevel().setBlock(creative, plant.defaultBlockState(), 3);
        ServerPlayer builder = connectedPlayer(h, GameType.CREATIVE);
        h.assertTrue(builder.gameMode.destroyBlock(creative) && items(h, creative, dust) == 0 && xp(h, creative).isEmpty(),
                "creative removal emits neither resources nor XP");
        h.succeed();
    }

    private static void placement(GameTestHelper h) {
        ServerPlayer player = survival(h);
        var plant = ChromaBlocks.tieredPlant(ChromaTieredPlants.AURA_BLOOM).get();
        BlockPos floor = h.absolutePos(new BlockPos(3, 2, 3));
        h.getLevel().setBlock(floor, Blocks.DIRT.defaultBlockState(), 3);
        ItemStack stack = new ItemStack(plant, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var use = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
        stack.useOn(use);
        h.assertTrue(h.getLevel().getBlockState(floor.above()).isAir() && stack.getCount() == 1,
                "real BlockItem placement consumes an insufficient-tier plant and removes it");
        grant(player, ProgressStage.CRYSTALS);
        stack.useOn(use);
        h.assertTrue(h.getLevel().getBlockState(floor.above()).is(plant), "sufficient-tier native placement remains");
        var ore = ChromaBlocks.FIRESTONE.get();
        BlockPos pos = place(h, ore, 8);
        ore.setPlacedBy(h.getLevel(), pos, ore.defaultBlockState(), player, new ItemStack(ore));
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "survival ore placement remains gated by its own stage");
        ServerPlayer builder = connectedPlayer(h, GameType.CREATIVE);
        place(h, ore, 8);
        ore.setPlacedBy(h.getLevel(), pos, ore.defaultBlockState(), builder, new ItemStack(ore));
        h.assertTrue(h.getLevel().getBlockState(pos).is(ore), "creative inspection/building remains allowed");
        h.succeed();
    }

    private static void loot(GameTestHelper h) {
        ServerPlayer player = survival(h);
        grant(player, ProgressStage.CTM);
        grant(player, ProgressStage.POWERCRYSTAL);
        int resources = 0;
        BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3));
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BlockTieredResource) || !BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(ChromatiCraft.MODID)) continue;
            resources++;
            h.getLevel().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            h.assertTrue(Block.getDrops(block.defaultBlockState(), h.getLevel(), pos, null).isEmpty(),
                    "unowned native drops stay empty: " + block);
            // Grant each concrete gate; this deliberately also covers all seven plant identities.
            if (block instanceof reika.chromaticraft.block.worldgen26.BlockTieredOre ore) grant(player, ore.getProgressStage());
            if (block instanceof reika.chromaticraft.block.worldgen26.BlockTieredPlant plant) grant(player, plant.getPlant().stage());
            h.assertTrue(!Block.getDrops(block.defaultBlockState(), h.getLevel(), pos, null, player, ItemStack.EMPTY).isEmpty(),
                    "loaded generated loot table resolves the real tier resources: " + block);
        }
        h.assertTrue(resources == 22, "all 15 accepted ores and seven plants inherit the shared contract");
        h.succeed();
    }

    private static void dropEvent(GameTestHelper h) {
        ServerPlayer player = survival(h);
        grant(player, ProgressStage.ABILITY);
        var ore = ChromaBlocks.VIBRANT_CRYSTALS.get();
        BlockPos pos = place(h, ore, 3);
        int[] calls = {0};
        Consumer<BlockDropsEvent> cancel = event -> {
            if (event.getLevel() == h.getLevel() && event.getPos().equals(pos)) {
                calls[0]++;
                h.assertTrue(event.getState().is(ore) && event.getBreaker() == player
                        && event.getDroppedExperience() >= 2 && event.getDroppedExperience() <= 6,
                        "native event retains original state, owner and XP range after removal");
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            h.assertTrue(player.gameMode.destroyBlock(pos), "event cancellation suppresses drops without undoing removal");
            h.assertTrue(calls[0] == 1 && h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).isEmpty()
                    && xp(h, pos).isEmpty(), "one cancelled event suppresses both items and XP");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        place(h, ore, 3);
        Consumer<BlockDropsEvent> modify = event -> {
            if (event.getLevel() == h.getLevel() && event.getPos().equals(pos)) {
                event.getDrops().clear();
                event.getDrops().add(new ItemEntity(h.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.DIAMOND, 2)));
                event.setDroppedExperience(3);
            }
        };
        NeoForge.EVENT_BUS.addListener(modify);
        try {
            h.assertTrue(player.gameMode.destroyBlock(pos) && items(h, pos, Items.DIAMOND) == 2 && !xp(h, pos).isEmpty(),
                    "modified event contents and experience are the values actually spawned");
        } finally { NeoForge.EVENT_BUS.unregister(modify); }
        h.succeed();
    }

    private static void recursive(GameTestHelper h) {
        ServerPlayer player = survival(h);
        grant(player, ProgressStage.ABILITY);
        var ore = ChromaBlocks.VIBRANT_CRYSTALS.get();
        Item dust = ChromaItems.TIERED.get(ChromaTieredItems.RESONANCE_DUST).get();
        BlockPos pos = place(h, ore, 3), unowned = place(h, ore, 8);
        ServerPlayer insufficient = survival(h);
        ItemStack before = insufficient.getMainHandItem().copy();
        var host = ReikaWorldHelper.getDropsAt(h.getLevel(), pos, 4, insufficient);
        var silk = ReikaBlockHelper.getSilkTouch(h.getLevel(), pos, ore, insufficient, false);
        h.assertTrue(!ore.isPlayerSufficientTier(insufficient) && host.size() == 1 && host.getFirst().is(Items.END_STONE),
                "insufficient-tier world helper returns host loot: " + host);
        h.assertTrue(ItemStack.matches(before, insufficient.getMainHandItem()) && silk != null && silk.is(Items.END_STONE),
                "Silk Touch helper returns host loot and resource queries preserve the held stack: before=" + before
                        + ", after=" + insufficient.getMainHandItem() + ", silk=" + silk);
        h.assertTrue(ReikaWorldHelper.getDropsAt(h.getLevel(), pos, 4, player).stream().mapToInt(ItemStack::getCount).sum() == 17,
                "shared world helper recognizes the real ore and its explicit Fortune level");
        h.assertTrue(ReikaBlockHelper.getSilkTouch(h.getLevel(), pos, ore, player, false).is(dust),
                "shared Silk Touch helper returns tier resources, never the disguised ore block");
        List<ItemStack> drops = new ArrayList<>();
        var breaker = ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(h.getLevel(), pos, 1);
        breaker.player = player; breaker.fortune = 4; breaker.silkTouch = true;
        breaker.bounds = BlockBox.block(pos.getX(), pos.getY(), pos.getZ());
        breaker.dropConsumer = (where, stack) -> drops.add(stack);
        var noOwner = ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(h.getLevel(), unowned, 1);
        noOwner.bounds = BlockBox.block(unowned.getX(), unowned.getY(), unowned.getZ());
        noOwner.dropConsumer = (where, stack) -> { throw new AssertionError("unowned recursive mining must not yield tier resources"); };
        h.runAfterDelay(4, () -> {
            h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockState(unowned).isAir()
                    && drops.stream().allMatch(stack -> stack.is(dust))
                    && drops.stream().mapToInt(ItemStack::getCount).sum() == 17,
                    "actual recursive miner preserves player-gated source drops instead of silk-touching the block");
            h.succeed();
        });
    }
}
