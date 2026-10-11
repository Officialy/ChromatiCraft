package reika.chromaticraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.items.tools.ItemAuraPouch;
import reika.chromaticraft.items.tools.ItemEfficiencyCrystal;
import reika.chromaticraft.items.tools.ItemRecipeCacheCapsule;
import reika.chromaticraft.items.tools.ItemWarpCapsule;
import reika.chromaticraft.items.tools.wands.ItemCaptureWand;
import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.Arrays;
import java.util.List;

/** Item behavior, inventory transfers and entity serialization regressions. */
final class ChromaItemPortTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event, env, "item_port_efficiency", 80, ChromaItemPortTests::efficiency);
        ChromaGameTests.register(event, env, "item_port_capture", 80, ChromaItemPortTests::capture);
        ChromaGameTests.register(event, env, "item_port_recall", 80, ChromaItemPortTests::recall);
        ChromaGameTests.register(event, env, "item_port_recipe_tablet", 80, ChromaItemPortTests::tablet);
        ChromaGameTests.register(event, env, "item_port_cave_exit_deep", 80,
                reika.chromaticraft.data.ChromaTestStructureProvider.WAND_ARENA, ChromaItemPortTests::caveExitDeep);
    }

    private static void caveExitDeep(GameTestHelper h) {
        // A sealed chamber exercises deep connected-space traversal through the actual tool.
        for (int x = 0; x <= 15; x++) {
            for (int y = 1; y <= 14; y++) {
                for (int z = 0; z <= 15; z++) {
                    boolean shell = x == 0 || x == 15 || z == 0 || z == 15 || y == 1 || y == 14;
                    h.setBlock(new BlockPos(x, y, z), shell ? net.minecraft.world.level.block.Blocks.STONE
                            : net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos start = new BlockPos(8, 12, 8), bottom = new BlockPos(8, 2, 8);
        player.setPos(Vec3.atCenterOf(h.absolutePos(start))); player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ChromaItems.CAVE_PATHER.get()));
        h.assertTrue(ChromaItems.CAVE_PATHER.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND)
                == net.minecraft.world.InteractionResult.SUCCESS, "the real sneaking route finder completes its connected-space search");
        h.assertTrue(h.getBlockState(bottom).is(ChromaBlocks.CHROMA_TRAIL.get())
                && h.getBlockState(start).is(ChromaBlocks.CHROMA_TRAIL.get())
                && h.getLevel().getBlockEntity(h.absolutePos(bottom)) instanceof reika.chromaticraft.block.BlockChromaTrail.TileChromaTrail
                && h.getBlockState(bottom.below()).is(net.minecraft.world.level.block.Blocks.STONE),
                "the deepest reachable floor receives a real trail waypoint without replacing the supporting floor");
        h.succeed();
    }

    private static void efficiency(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var charging = ToolChargingSystem.instance;
        var powered = charging.getChargedItem(ChromaItems.PURIFY.get(), 100);
        h.assertTrue(!ItemEfficiencyCrystal.isActive(player), "a new player must not have attenuation protection");
        h.assertTrue(charging.removeCharge(powered, 9, player) == 9, "ordinary tools must pay full charge");
        ChromaItems.EFFICIENCY_GEM.get().inventoryTick(new ItemStack(ChromaItems.EFFICIENCY_GEM.get()), h.getLevel(), player, null);
        h.assertTrue(charging.removeCharge(powered, 9, player) == 3, "carried gem must reduce charge drain to one third");
        player.getPersistentData().putLong("last_efficiency", h.getLevel().getGameTime()-20);
        h.assertTrue(charging.removeCharge(powered, 9, player) == 9, "attenuation protection must expire after 20 ticks");
        var pouch = new ItemStack(ChromaItems.AURA_POUCH.get());
        var contents = new ItemStack[ItemAuraPouch.SIZE];
        Arrays.fill(contents, ItemStack.EMPTY);
        contents[0] = new ItemStack(ChromaItems.EFFICIENCY_GEM.get());
        ChromaItems.AURA_POUCH.get().setItems(pouch, contents);
        ChromaItems.AURA_POUCH.get().setSlotActive(pouch, 0, false);
        ChromaItems.AURA_POUCH.get().inventoryTick(pouch, h.getLevel(), player, null);
        h.assertTrue(!ItemEfficiencyCrystal.isActive(player), "disabled pouch slots must not activate their gem");
        ChromaItems.AURA_POUCH.get().setSlotActive(pouch, 0, true);
        ChromaItems.AURA_POUCH.get().inventoryTick(pouch, h.getLevel(), player, null);
        h.assertTrue(ItemEfficiencyCrystal.isActive(player), "enabled pouch slots must tick their gem");
        h.succeed();
    }

    private static void capture(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        var wand = new ItemStack(ChromaItems.CAPTURE_WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var cow = EntityTypes.COW.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(cow != null, "the cow type must instantiate");
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        cow.setPos(Vec3.atCenterOf(pos));
        cow.setHealth(7);
        cow.setCustomName(Component.literal("Stored cow"));
        var uuid = cow.getUUID();
        h.getLevel().addFreshEntity(cow);
        ChromaItems.CAPTURE_WAND.get().interactLivingEntity(wand, player, cow, InteractionHand.MAIN_HAND);
        h.assertTrue(cow.isRemoved() && ChromaItems.CAPTURE_WAND.get().hasEntity(wand), "capture must transfer the live mob into the wand");
        var saved = ItemCaptureWand.getMob(wand, h.getLevel());
        h.assertTrue(saved != null && saved.getUUID().equals(uuid) && saved.getHealth() == 7
                && Component.literal("Stored cow").equals(saved.getCustomName()), "the mob's UUID, health and name must survive serialization");
        h.runAfterDelay(2, () -> {
            ChromaItems.CAPTURE_WAND.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
            h.assertTrue(!ChromaItems.CAPTURE_WAND.get().hasEntity(wand), "successful spawn must clear the stored mob");
            var released = h.getLevel().getEntity(uuid);
            h.assertTrue(released != null && !released.isRemoved() && Component.literal("Stored cow").equals(released.getCustomName()),
                    "release must restore exactly the captured mob to the level");
            h.succeed();
        });
    }

    private static void recall(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var item = new ItemStack(ChromaItems.RECALL_TABLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var target = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))).add(0, 15, 0);
        player.setPos(target);
        ChromaItems.RECALL_TABLET.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(ItemWarpCapsule.isBound(item) && ItemWarpCapsule.position(item).distanceTo(target) < 0.01,
                "first use must bind the player's position");
        ReikaItemHelper.updateStackTag(item, tag -> tag.putString("dim", "minecraft:the_nether"));
        player.setPos(target.add(50, 0, 0));
        var before = player.position();
        ChromaItems.RECALL_TABLET.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.position().equals(before) && !item.isEmpty(), "a dimension mismatch must retain the tablet and player position");
        ReikaItemHelper.updateStackTag(item, tag -> tag.putString("dim", h.getLevel().dimension().identifier().toString()));
        player.setPos(target.add(2050, 0, 0));
        before = player.position();
        ChromaItems.RECALL_TABLET.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.position().equals(before) && !item.isEmpty(), "outside 2000 blocks recall must fail without consumption");
        player.setPos(target.add(30, 0, 0));
        ChromaItems.RECALL_TABLET.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().isEmpty(), "an open-sky recall must consume the tablet");
        h.assertTrue(Math.abs(player.getX()-target.x) <= 12 && Math.abs(player.getZ()-target.z) <= 12
                && player.getY() >= target.y+1 && player.getY() <= target.y+9, "recall must use the original randomized arrival range");
        h.succeed();
    }

    private static void tablet(GameTestHelper h) {
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        var at = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlockAndUpdate(at, ChromaBlocks.CASTING_TABLE.get().defaultBlockState());
        var table = (TileEntityCastingTable)h.getLevel().getBlockEntity(at);
        table.setPlacer(player);
        for (var pos : List.of(at.west(2), at.east(2)))
            h.getLevel().setBlockAndUpdate(pos, ChromaBlocks.ITEM_STAND.get().defaultBlockState());
        var occupied = (TileEntityItemStand)h.getLevel().getBlockEntity(at.west(2));
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Cached blade"));
        sword.setDamageValue(41);
        occupied.setItem(0, sword.copy());
        var tablets = new ItemStack(ChromaItems.RECIPE_TABLET.get(), 4);
        h.assertTrue(tablets.getMaxStackSize() == 4, "empty Recipe Tablets must retain the original stack size");
        player.setItemInHand(InteractionHand.MAIN_HAND, tablets);
        var hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        ChromaItems.RECIPE_TABLET.get().onItemUseFirst(tablets, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        ItemStack filled = ItemStack.EMPTY;
        for (var stack : player.getInventory()) if (ItemRecipeCacheCapsule.hasRecipe(stack)) filled = stack;
        h.assertTrue(tablets.getCount() == 3 && filled.getCount() == 1 && occupied.getItem(0).isEmpty(),
                "a stack of empty tablets must store the inventory in exactly one separate tablet");
        player.setItemInHand(InteractionHand.MAIN_HAND, filled);
        var removedAt = at.east(2);
        h.getLevel().removeBlock(removedAt, false);
        ChromaItems.RECIPE_TABLET.get().onItemUseFirst(filled, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(ItemRecipeCacheCapsule.hasRecipe(filled) && occupied.getItem(0).isEmpty(),
                "a missing destination must leave all contents in the tablet");
        h.getLevel().setBlockAndUpdate(removedAt, ChromaBlocks.ITEM_STAND.get().defaultBlockState());
        ChromaItems.RECIPE_TABLET.get().onItemUseFirst(filled, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(filled.isEmpty() && ItemStack.isSameItemSameComponents(occupied.getItem(0), sword),
                "restoring must preserve damage and components, then consume the filled tablet");
        h.assertTrue(((TileEntityItemStand)h.getLevel().getBlockEntity(removedAt)).getItem(0).isEmpty(), "saved empty stands must restore as empty");
        h.succeed();
    }
}
