package reika.chromaticraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.block.BlockChromaCrafter;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.tileentity.recipe.TileEntityChromaCrafter;

/** Real registered Chroma Crafter, production pool recipes and native transactional capabilities. */
final class ChromaCrafterTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event,env,"crafter_fluid_transactions",80,ChromaCrafterTests::fluid);
        ChromaGameTests.register(event,env,"crafter_tally_and_recipe",80,ChromaCrafterTests::tally);
        ChromaGameTests.register(event,env,"crafter_craft_surplus",80,ChromaCrafterTests::surplus);
        ChromaGameTests.register(event,env,"crafter_doubling_output_capacity",80,ChromaCrafterTests::doubling);
        ChromaGameTests.register(event,env,"crafter_save_restore",80,ChromaCrafterTests::save);
        ChromaGameTests.register(event,env,"crafter_structure_energy_pause",80,ChromaCrafterTests::pause);
        ChromaGameTests.register(event,env,"crafter_recipe_focus_data",80,ChromaCrafterTests::data);
        ChromaGameTests.register(event,env,"crafter_break_conservation",80,ChromaCrafterTests::broken);
    }
    private static TileEntityChromaCrafter place(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos,ChromaBlocks.CHROMA_CRAFTER.get().defaultBlockState(),3);
        h.getLevel().setBlock(pos.above(),ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get().defaultBlockState(),3);
        h.getLevel().setBlock(pos.above(2),ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get().defaultBlockState(),3);
        return (TileEntityChromaCrafter)h.getLevel().getBlockEntity(pos);
    }
    private static ItemStack dust(int count) { return new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get(),count); }
    private static ItemStack berries(int count) { return ChromaItems.craftingStack(ChromaCraftingItems.ETHER_BERRIES).copyWithCount(count); }
    private static void input(TileEntityChromaCrafter tile, ItemStack stack) {
        tile.setItem(0,stack); tile.updateEntity(tile.getLevel(),tile.getBlockPos());
    }
    private static void setup(TileEntityChromaCrafter tile) { input(tile,new ItemStack(Items.IRON_INGOT,2)); input(tile,dust(32)); }
    private static void energy(TileEntityChromaCrafter tile) {
        var cost = tile.getRequiredEnergy();
        for (var color : cost.elementSet()) tile.setEnergyClient(color,cost.getValue(color));
    }
    private static void fill(TileEntityChromaCrafter tile) {
        for (int i = 0; i < 120; i++) try (var transaction = Transaction.openRoot()) {
            tile.fluidHandler(Direction.NORTH).insert(0,FluidResource.of(ChromaFluids.CHROMA.get()),25,transaction);
            transaction.commit();
        }
    }
    private static void finish(TileEntityChromaCrafter tile) {
        for (int i = 0; i < TileEntityChromaCrafter.CRAFTING_DURATION; i++) tile.updateEntity(tile.getLevel(),tile.getBlockPos());
    }
    private static void fluid(GameTestHelper h) {
        var tile = place(h); var chroma = FluidResource.of(ChromaFluids.CHROMA.get());
        var nativeHandler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,tile.getBlockPos(),Direction.NORTH);
        h.assertTrue(nativeHandler == tile.fluidHandler(Direction.NORTH),"registered native fluid view");
        try (var tx = Transaction.openRoot()) {
            h.assertTrue(nativeHandler.insert(0,chroma,1000,tx) == 25,"original per-call 25 mB limit");
            h.assertTrue(tile.fluidHandler(Direction.UP).insert(0,chroma,25,tx) == 0,"vertical input denied");
            h.assertTrue(nativeHandler.extract(0,chroma,25,tx) == 0,"input-only fluid");
            h.assertTrue(nativeHandler.insert(0,FluidResource.of(net.minecraft.world.level.material.Fluids.WATER),25,tx) == 0,"wrong fluid denied");
        }
        h.assertTrue(tile.getChromaLevel() == 0,"aborted fluid transaction rolls back");
        try (var tx = Transaction.openRoot()) {
            nativeHandler.insert(0,chroma,25,tx);
            try (var child = Transaction.open(tx)) { tile.fluidHandler(Direction.EAST).insert(0,chroma,25,child); }
            h.assertTrue(tile.getChromaLevel() == 25,"nested cross-face rollback"); tx.commit();
        }
        fill(tile); h.assertTrue(tile.getChromaLevel() == 3000,"bounded full tank");
        h.getLevel().setBlock(tile.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        var componentTile = place(h);
        var marked = chroma.with(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("preserved chroma"));
        try (var tx = Transaction.openRoot()) { componentTile.fluidHandler(Direction.NORTH).insert(0,marked,25,tx); }
        h.assertTrue(componentTile.getChromaLevel() == 0 && componentTile.fluidHandler(null).getResource(0).isEmpty(),"abort restores amount and resource identity");
        try (var tx = Transaction.openRoot()) {
            componentTile.fluidHandler(Direction.NORTH).insert(0,marked,25,tx);
            h.assertTrue(componentTile.fluidHandler(Direction.EAST).insert(0,chroma,25,tx) == 0,"distinct fluid components never mix");
            try (var child = Transaction.open(tx)) { componentTile.fluidHandler(Direction.EAST).insert(0,marked,25,child); child.commit(); }
        }
        h.assertTrue(componentTile.getChromaLevel() == 0,"committed child still rolls back with its root across faces");
        try (var tx = Transaction.openRoot()) { componentTile.fluidHandler(Direction.NORTH).insert(0,marked,25,tx); tx.commit(); }
        var loaded = new TileEntityChromaCrafter(componentTile.getBlockPos(),componentTile.getBlockState());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),componentTile.saveWithoutMetadata(h.getLevel().registryAccess())));
        h.assertTrue(loaded.getChromaLevel() == 25 && loaded.fluidHandler(null).getResource(0).equals(marked),"native fluid components survive save/load");
        h.succeed();
    }
    private static void tally(GameTestHelper h) {
        var tile = place(h); setup(tile);
        h.assertTrue(tile.getCurrentItems().get(new ItemStack(Items.IRON_INGOT)) == 2
                && tile.getCurrentItems().get(dust(1)) == 32,"typed integer tally");
        h.assertTrue(tile.getActiveRecipeId().getPath().endsWith("chroma_ingot") && tile.getRecipeTicks() == 300,"actual source pool recipe and duration");
        var snapshot = tile.getStoredRecipeItems(); snapshot.getFirst().setCount(1);
        h.assertTrue(tile.getCurrentItems().get(new ItemStack(Items.IRON_INGOT)) == 2,"detached stored-item view");
        h.assertTrue(tile.canTakeItemThroughFace(1,new ItemStack(Items.IRON_INGOT),Direction.UP)
                && !tile.canTakeItemThroughFace(0,new ItemStack(Items.IRON_INGOT),Direction.DOWN),"output-only extraction");
        h.succeed();
    }
    private static void surplus(GameTestHelper h) {
        var tile = place(h); setup(tile); energy(tile); fill(tile); finish(tile);
        h.assertTrue(tile.getItem(1).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.CHROMA_INGOT).get()) && tile.getItem(1).getCount() == 1,"one native alloy output");
        h.assertTrue(tile.getCurrentItems().get(new ItemStack(Items.IRON_INGOT)) == 1
                && tile.getCurrentItems().get(dust(1)) == 16,"only one catalyst and sixteen dust consumed");
        h.assertTrue(tile.getChromaLevel() == 0 && tile.getEnergy().isEmpty() && tile.getActiveRecipeId() == null,"one complete fluid/energy cost");
        h.succeed();
    }
    private static void doubling(GameTestHelper h) {
        var tile = place(h); setup(tile);
        int saturation = reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma.ETHER_SATURATION;
        input(tile,berries(saturation*2)); energy(tile); fill(tile);
        tile.setItem(1,ChromaItems.craftingStack(ChromaCraftingItems.CHROMA_INGOT).copyWithCount(63));
        finish(tile);
        h.assertTrue(tile.getItem(1).getCount() == 63 && tile.getChromaLevel() == 3000
                && tile.getBerryCount() == saturation*2 && tile.getCurrentItems().get(dust(1)) == 32,"blocked doubled output conserves all resources");
        tile.removeItem(1,63); tile.updateEntity(tile.getLevel(),tile.getBlockPos());
        h.assertTrue(tile.getItem(1).getCount() == 2 && tile.getBerryCount() == saturation,"source berry doubling and exact saturation consumption");
        h.succeed();
    }
    private static void save(GameTestHelper h) {
        var tile = place(h); setup(tile); input(tile,berries(7)); energy(tile); fill(tile);
        for (int i = 0; i < 17; i++) tile.updateEntity(tile.getLevel(),tile.getBlockPos());
        var saved = tile.saveWithoutMetadata(h.getLevel().registryAccess());
        var loaded = new TileEntityChromaCrafter(tile.getBlockPos(),tile.getBlockState());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        loaded.setLevel(h.getLevel());
        h.assertTrue(loaded.getRecipeTicks() == 283 && loaded.getBerryCount() == 7 && loaded.getChromaLevel() == 3000
                && loaded.getRenderItems().size() == 3 && loaded.getCurrentItems().get(dust(1)) == 32,"complete persistence and client display snapshot");
        loaded.updateEntity(h.getLevel(),loaded.getBlockPos());
        h.assertTrue(loaded.getActiveRecipe() != null && loaded.getRecipeTicks() == 282,"saved recipe resolves from current native recipe registry"); h.succeed();
    }
    private static void pause(GameTestHelper h) {
        var tile = place(h); setup(tile); fill(tile); finish(tile);
        h.assertTrue(tile.getRecipeTicks() == 300,"no energy pauses timer"); energy(tile);
        h.getLevel().setBlock(tile.getBlockPos().above(2),Blocks.AIR.defaultBlockState(),3); finish(tile);
        h.assertTrue(tile.getRecipeTicks() == 300 && !tile.hasStructure(),"broken chamber pauses timer");
        h.assertTrue(!tile.getBlockState().getValue(BlockChromaCrafter.FORMED)
                && h.getLevel().getBlockState(tile.getBlockPos().above()).getValue(BlockStructureShield.CRAFTER_SECTION) == 0,"disconnected glass resets");
        h.getLevel().setBlock(tile.getBlockPos().above(2),ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get().defaultBlockState(),3);
        finish(tile); h.assertTrue(!tile.getItem(1).isEmpty(),"restored chamber resumes"); h.succeed();
    }
    private static void data(GameTestHelper h) {
        var tile = place(h); tile.updateEntity(tile.getLevel(),tile.getBlockPos());
        h.assertTrue(tile.getBlockState().requiresCorrectToolForDrops() && tile.getBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),"original rock material uses native pickaxe harvesting");
        h.assertTrue(tile.getBlockState().getValue(BlockChromaCrafter.FORMED)
                && h.getLevel().getBlockState(tile.getBlockPos().above()).getValue(BlockStructureShield.CRAFTER_SECTION) == 1
                && h.getLevel().getBlockState(tile.getBlockPos().above(2)).getValue(BlockStructureShield.CRAFTER_SECTION) == 2,"data-owned connected chamber states");
        var recipe = h.getLevel().getServer().getRecipeManager().recipeMap().byType(ChromaRecipeTypes.CASTING.get()).stream()
                .filter(r -> r.id().identifier().getPath().endsWith("chroma_crafter")).findFirst().orElseThrow().value();
        h.assertTrue(recipe.stands().size() == 24 && recipe.runes().size() == 5 && recipe.aura().size() == 3
                && recipe.duration() == 400 && recipe.tier() == CastingTableRecipe.Tier.PYLON,"original machine recipe layout and policy");
        h.assertTrue(tile.getRelativeFocusCrystalLocations().size() == 4 && tile.getAccelerationFactor() == 0.5F
                && tile.getCraftingDuration() == 300,"four original focus offsets and unboosted timing");
        h.succeed();
    }
    private static void broken(GameTestHelper h) {
        var tile = place(h); setup(tile); input(tile,berries(9)); tile.setItem(1,new ItemStack(Items.DIAMOND,3));
        BlockPos pos = tile.getBlockPos(); h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        int iron=0, dust=0, berry=0, diamond=0;
        for (var item : drops) {
            ItemStack stack=item.getItem();
            if (stack.is(Items.IRON_INGOT)) iron+=stack.getCount();
            if (stack.is(ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get())) dust+=stack.getCount();
            if (stack.is(berries(1).getItem())) berry+=stack.getCount();
            if (stack.is(Items.DIAMOND)) diamond+=stack.getCount();
        }
        h.assertTrue(iron == 2 && dust == 32 && berry == 9 && diamond == 3,"any-removal drops internal inputs/berries and container output exactly once"); h.succeed();
    }
}
