package reika.chromaticraft;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipeInput;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.BlockHoverBlock.HoverType;
import reika.chromaticraft.data.ChromaTestStructureProvider;
import reika.chromaticraft.items.tools.wands.ItemBuilderWand;
import reika.chromaticraft.items.tools.wands.ItemFlightWand;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.auxiliary.trackers.KeyWatcher.Key;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;
import java.util.function.Consumer;

/** Runtime regressions for the second wand slice, using the generated arena large enough for its selections. */
final class ChromaWandPortTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        register(event, env, "wand_construction_controls", 30, ChromaWandPortTests::constructionControls);
        register(event, env, "wand_construction_resources", 30, ChromaWandPortTests::constructionResources);
        register(event, env, "wand_hover_modes_and_expiry", 180, ChromaWandPortTests::hover);
        register(event, env, "wand_excavation_variants_and_loot", 60, ChromaWandPortTests::excavation);
        register(event, env, "wand_breaker_queue_revalidation", 30, ChromaWandPortTests::queuedState);
        register(event, env, "wand_star_enchanting_components", 30, ChromaWandPortTests::enchanting);
        register(event, env, "wand_sonar_flags_cost_and_recipe", 30, ChromaWandPortTests::sonarItem);
        register(event, env, "wand_sonar_scan_and_categories", 30, ChromaWandPortTests::sonarScan);
        register(event, env, "wand_multitool_enchanting_and_wear", 30, ChromaWandPortTests::multitool);
        register(event, env, "wand_ore_harvester_loot_and_permissions", 30, ChromaWandPortTests::oreHarvester);
        register(event, env, "wand_bottleneck_ranking_and_recipe", 30, ChromaWandPortTests::bottleneck);
        register(event, env, "wand_spawner_charge_and_recipe", 30, ChromaWandPortTests::spawnerCharge);
        register(event, env, "wand_spawner_native_activation", 40, ChromaWandPortTests::spawnerActivation);
        register(event, env, "wand_magic_branch_variants_and_animation", 30, ChromaWandPortTests::magicBranches);
        register(event, env, "item_port_bulk_mover_transfers", 30, ChromaWandPortTests::bulkTransfers);
        register(event, env, "item_port_bulk_mover_menu_and_recipe", 30, ChromaWandPortTests::bulkMenu);

    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
            String name, int ticks, Consumer<GameTestHelper> test) {
        ChromaGameTests.register(event, env, name, ticks, ChromaTestStructureProvider.WAND_ARENA, test);
    }

    private static ServerPlayer survivalPlayer(GameTestHelper h) {
        // The vanilla helper overrides gameMode() to CREATIVE forever, even after setGameMode.
        // Use a configured connection with a real player whose survival/creative state can change.
        ServerPlayer player = ChromaGameTests.joinedMockPlayer(h, net.minecraft.world.level.GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getAbilities().mayBuild = true;
        player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(8, 8, 8))));
        CompoundTag energy = new CompoundTag();
        energy.putInt("cap", 1000);
        for (CrystalElement element : CrystalElement.elements) energy.putInt(element.name(), 1000);
        player.getPersistentData().put("CrystalBuffer", energy);
        // Fresh test levels begin at tick zero; expire source pendant stamps so their legacy
        // zero-default grace period cannot discount the deliberately unboosted cost fixture.
        CompoundTag pendant = new CompoundTag();
        for (CrystalElement element : CrystalElement.elements) pendant.putLong(element.name(), h.getLevel().getGameTime()-1000);
        player.getPersistentData().put("pendant_status", pendant);
        return player;
    }

    private static UseOnContext click(ServerPlayer player, BlockPos pos, Direction face) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }

    private static void constructionControls(GameTestHelper h) {
        var player = survivalPlayer(h);
        var level = h.getLevel();
        BlockPos center = h.absolutePos(new BlockPos(7, 2, 7));
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++)
            level.setBlockAndUpdate(center.offset(x,0,z), Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,center,Direction.UP,player).size()==25,
                "a connected five-by-five face must select all 25 available destinations");
        try {
            KeyWatcher.instance.setKey(player,Key.TAB,true);
            h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,center,Direction.UP,player).size()==16,
                    "TAB must select only the 16 perimeter blocks");
            KeyWatcher.instance.setKey(player,Key.TAB,false);
            KeyWatcher.instance.setKey(player,Key.TILDE,true);
            h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,center,Direction.UP,player).size()==9,
                    "TILDE must select only the nine interior blocks");
            KeyWatcher.instance.setKey(player,Key.TILDE,false);
            KeyWatcher.instance.setKey(player,Key.LCTRL,true);
            h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,center,Direction.UP,player).equals(List.of(center.above())),
                    "CTRL must reduce the horizontal face to its clicked column");
            KeyWatcher.instance.setKey(player,Key.LCTRL,false);
            BlockPos wall=h.absolutePos(new BlockPos(12,4,7));
            for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++)
                level.setBlockAndUpdate(wall.offset(0,y,z),Blocks.OAK_PLANKS.defaultBlockState());
            player.setShiftKeyDown(true);
            h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,wall,Direction.EAST,player).size()==5,
                    "Shift must restrict a vertical face to the clicked horizontal row");
            player.setShiftKeyDown(false);
            KeyWatcher.instance.setKey(player,Key.END,true);
            h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,wall,Direction.EAST,player).size()==10,
                    "END must retain only the two outside vertical columns of this face");
        } finally {
            player.setShiftKeyDown(false);
            for (Key key : new Key[]{Key.TAB,Key.TILDE,Key.LCTRL,Key.END}) KeyWatcher.instance.setKey(player,key,false);
        }
        // A matching but disconnected block, and a different state, must not extend the selected face.
        level.setBlockAndUpdate(center.east(4),Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(center.east(3),Blocks.BIRCH_PLANKS.defaultBlockState());
        h.assertTrue(ItemBuilderWand.getCoordinatesFor(level,center,Direction.UP,player).size()==25,
                "selection must stay on the connected matching material");
        h.succeed();
    }

    private static void constructionResources(GameTestHelper h) {
        var player = survivalPlayer(h);
        var level = h.getLevel();
        BlockPos center = h.absolutePos(new BlockPos(7,2,7));
        var state = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) level.setBlockAndUpdate(center.offset(x,0,z),state);
        ItemStack wand = new ItemStack(ChromaItems.CONSTRUCTION_WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        ItemStack logs = new ItemStack(Items.OAK_LOG,2);
        player.getInventory().setItem(1,logs);
        ChromaItems.CONSTRUCTION_WAND.get().onItemUseFirst(wand,click(player,center,Direction.UP));
        int placed=0;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) if (level.getBlockState(center.offset(x,1,z))==state) placed++;
        h.assertTrue(placed==2 && logs.isEmpty(),"construction must copy the exact axis and stop when its two materials run out");
        h.assertTrue(PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.BROWN)==980
                && PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.WHITE)==980
                && PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.BLACK)==990,
                "only successful placements may charge energy");
        player.getAbilities().mayBuild=false;
        player.getInventory().setItem(1,new ItemStack(Items.OAK_LOG,9));
        h.assertTrue(!ChromaItems.CONSTRUCTION_WAND.get().onItemUseFirst(wand,click(player,center,Direction.UP)).consumesAction(),
                "a player forbidden to build must not place through the construction star");
        h.assertTrue(player.getInventory().getItem(1).getCount()==9,"a rejected placement must retain its materials");
        h.succeed();
    }

    private static void hover(GameTestHelper h) {
        var player=survivalPlayer(h);
        var level=h.getLevel();
        ItemStack wand=new ItemStack(ChromaItems.HOVER_WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        ReikaItemHelper.updateStackTag(wand,tag -> tag.putInt("mode",Integer.MAX_VALUE));
        h.assertTrue(ItemFlightWand.getMode(wand)==HoverType.ELEVATE,"invalid saved modes must safely fall back to Elevator");
        List<BlockPos> fields=new ArrayList<>();
        int black=1000,lime=1000;
        for (HoverType mode:HoverType.values()) {
            BlockPos base=h.absolutePos(new BlockPos(2+4*mode.ordinal(),2,2));
            level.setBlockAndUpdate(base,Blocks.OAK_PLANKS.defaultBlockState());
            ItemFlightWand.setMode(wand,mode);
            ChromaItems.HOVER_WAND.get().useOn(click(player,base,Direction.UP));
            var cost=ChromaItems.HOVER_WAND.get().getEnergy(ItemFlightWand.costFactor(mode));
            black-=8*cost.getValue(CrystalElement.BLACK);lime-=8*cost.getValue(CrystalElement.LIME);
            for (int i=1;i<=8;i++) {
                BlockPos pos=base.above(i); fields.add(pos);
                var state=level.getBlockState(pos);
                h.assertTrue(state.is(ChromaBlocks.HOVER.get()) && state.getValue(BlockHoverBlock.TYPE)==mode
                        && state.getValue(BlockHoverBlock.DECAY)==BlockHoverBlock.Decay.ARMED,
                        "each mode must place eight armed fields with the selected motion type");
            }
        }
        h.assertTrue(PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.BLACK)==black
                && PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.LIME)==lime,
                "hover costs must retain the mode multipliers and per-field drain");
        BlockPos permanent=h.absolutePos(new BlockPos(7,3,12));
        level.setBlockAndUpdate(permanent,ChromaBlocks.HOVER.get().defaultBlockState());
        h.runAfterDelay(81,() -> {
            for (BlockPos pos:fields) h.assertTrue(level.getBlockState(pos).getValue(BlockHoverBlock.DECAY)==BlockHoverBlock.Decay.DECAYING,
                    "temporary fields must enter decay after 80 ticks");
        });
        h.runAfterDelay(161,() -> {
            for (BlockPos pos:fields) h.assertTrue(level.getBlockState(pos).isAir(),"every temporary field must expire by tick 159");
            h.assertTrue(level.getBlockState(permanent).is(ChromaBlocks.HOVER.get()),"structure-authored permanent fields must survive");
            h.succeed();
        });
    }

    private static void excavation(GameTestHelper h) {
        var player=survivalPlayer(h);
        var level=h.getLevel();
        ItemStack wand=new ItemStack(ChromaItems.EXCAVATION_WAND.get());
        var enchants=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        wand.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH),1);
        wand.enchant(ChromaEnchants.AUTOCOLLECT.getHolder(level.registryAccess()),1);
        player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        BlockPos first=h.absolutePos(new BlockPos(5,3,5));
        BlockPos second=first.east();
        level.setBlockAndUpdate(first,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
        level.setBlockAndUpdate(second,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        level.setBlockAndUpdate(first.north(),Blocks.BIRCH_LOG.defaultBlockState());
        h.assertTrue(!ChromaItems.EXCAVATION_WAND.get().canSpreadOn(level,first.below(),Blocks.STONE.defaultBlockState()),
                "overworld stone must remain excluded from excavation");
        BlockPos machine=h.absolutePos(new BlockPos(12,3,4));
        level.setBlockAndUpdate(machine,ChromaBlocks.CASTING_TABLE.get().defaultBlockState());
        h.assertTrue(!ChromaItems.EXCAVATION_WAND.get().canSpreadOn(level,machine,level.getBlockState(machine)),
                "crystal-network machinery must remain protected from excavation");
        h.assertTrue(!player.gameMode.destroyBlock(first) && level.getBlockState(first).is(Blocks.OAK_LOG),
                "the native completed-break hook must queue the wave and leave the first block for recursive mining");
        h.runAfterDelay(5,() -> {
            h.assertTrue(level.getBlockState(first).isAir() && level.getBlockState(second).isAir(),
                    "excavation must spread across all axis states of the same log species");
            h.assertTrue(level.getBlockState(first.north()).is(Blocks.BIRCH_LOG),"excavation must leave other species untouched");
            int count=0;for (ItemStack stack:player.getInventory()) if (stack.is(Items.OAK_LOG)) count+=stack.getCount();
            h.assertTrue(count==2,"auto collect must insert every mined log into the real player inventory");
            h.assertTrue(PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.BROWN)==998
                    && PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.YELLOW)==996,
                    "excavation must charge once per mined block");
            BlockPos ore=h.absolutePos(new BlockPos(10,3,10));
            level.setBlockAndUpdate(ore,Blocks.DIAMOND_ORE.defaultBlockState());
            ChromaItems.EXCAVATION_WAND.get().startExcavation(wand,ore,player);
            h.runAfterDelay(3,() -> {
                boolean silk=false;for(ItemStack stack:player.getInventory()) if(stack.is(Items.DIAMOND_ORE)) silk=true;
                h.assertTrue(silk,"native loot must receive the wand's Silk Touch component");h.succeed();
            });
        });
    }

    private static void queuedState(GameTestHelper h) {
        var level=h.getLevel();
        BlockPos first=h.absolutePos(new BlockPos(5,3,5)),next=first.east();
        level.setBlockAndUpdate(first,Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(next,Blocks.OAK_PLANKS.defaultBlockState());
        var breaker=ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(level,first,4);
        breaker.drops=false;
        int[] finished={0};
        breaker.call=new ProgressiveRecursiveBreaker.BreakerCallback() {
            @Override public boolean canBreak(ProgressiveRecursiveBreaker.ProgressiveBreaker b,net.minecraft.world.level.Level world,
                    BlockPos pos,net.minecraft.world.level.block.Block block) { return true; }
            @Override public void onPreBreak(ProgressiveRecursiveBreaker.ProgressiveBreaker b,net.minecraft.world.level.Level world,
                    BlockPos pos,net.minecraft.world.level.block.Block block) { }
            @Override public void onPostBreak(ProgressiveRecursiveBreaker.ProgressiveBreaker b,net.minecraft.world.level.Level world,
                    BlockPos pos,net.minecraft.world.level.block.Block block) { }
            @Override public void onFinish(ProgressiveRecursiveBreaker.ProgressiveBreaker b) { finished[0]++; }
        };
        ProgressiveRecursiveBreaker.instance.tick(TickRegistry.TickType.WORLD,level);
        h.assertTrue(level.getBlockState(first).isAir(),"the first breadth-first wave must mine its source");
        level.setBlockAndUpdate(next,Blocks.DIAMOND_BLOCK.defaultBlockState());
        ProgressiveRecursiveBreaker.instance.tick(TickRegistry.TickType.WORLD,level);
        h.assertTrue(level.getBlockState(next).is(Blocks.DIAMOND_BLOCK),"queued blocks must be checked again before mining a changed state");
        breaker.terminate();breaker.terminate();
        h.assertTrue(finished[0]==1,"completion callbacks must fire exactly once, including repeated termination");
        // No private inventory may intercept ordinary recursive-mining drops.
        h.assertTrue(breaker.dropInventory==null,"recursive breaking must drop into the world unless an inventory is explicitly supplied");
        level.setBlockAndUpdate(first,Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(next,Blocks.BIRCH_PLANKS.defaultBlockState());
        var blacklist=ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(level,first,4);
        blacklist.drops=false;
        blacklist.setBlacklist(new BlockKey(Blocks.BIRCH_PLANKS));
        ProgressiveRecursiveBreaker.instance.tick(TickRegistry.TickType.WORLD,level);
        h.assertTrue(level.getBlockState(first).isAir() && level.getBlockState(next).is(Blocks.BIRCH_PLANKS),
                "blacklists must invert matching instead of mining the forbidden material");
        blacklist.terminate();
        h.succeed();
    }

    private static CastingTableRecipe recipe(GameTestHelper h,String path) {
        for (var holder:h.getLevel().getServer().getRecipeManager().getRecipes())
            if (holder.id().identifier().getNamespace().equals(ChromatiCraft.MODID)
                    && holder.id().identifier().getPath().equals(path) && holder.value() instanceof CastingTableRecipe recipe) return recipe;
        throw new AssertionError("Missing casting recipe "+path);
    }

    private static void enchanting(GameTestHelper h) {
        var player=survivalPlayer(h);
        CastingTableRecipe recipe=recipe(h,"star_enchanting/excavation/fortune");
        h.assertTrue(recipe.duration()==1200 && recipe.stands().size()==8 && recipe.runes().size()==2,
                "Star Enchanting must retain its original duration, eight stands and two runes");
        h.assertTrue(!recipe.hasRequiredFragment(player),"Star Enchanting must require its research fragment");
        PlayerResearch.giveFragment(player,LexiconCatalog.byId("ENCHANTING"),false);
        h.assertTrue(recipe.hasRequiredFragment(player),"the authored enchanting fragment must unlock the recipe");
        ItemStack center=new ItemStack(ChromaItems.EXCAVATION_WAND.get());
        center.set(DataComponents.CUSTOM_NAME,Component.literal("My excavation star"));
        ReikaItemHelper.updateStackTag(center,tag -> tag.putString("custom_data","retained"));
        var silk=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        center.enchant(silk,1);
        List<ItemStack> grid=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));grid.set(4,center);
        var stands=Map.of(new BlockPos(0,0,-2),new ItemStack(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.PURPLE).get()),
                new BlockPos(0,0,2),new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.FOCUS_DUST).get()),
                new BlockPos(-2,0,0),new ItemStack(Items.LAPIS_LAZULI),new BlockPos(2,0,0),new ItemStack(Items.LAPIS_LAZULI),
                new BlockPos(-2,0,-2),new ItemStack(ChromaItems.ELEMENTAL_STONES.get(CrystalElement.PURPLE).get()),
                new BlockPos(2,0,2),new ItemStack(ChromaItems.ELEMENTAL_STONES.get(CrystalElement.BLACK).get()),
                new BlockPos(-2,0,2),new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.BINDING_CRYSTAL).get()),
                new BlockPos(2,0,-2),new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.BINDING_CRYSTAL).get()));
        var runes=Map.of(new BlockPos(4,-1,-4),CrystalElement.PURPLE,new BlockPos(-4,-1,4),CrystalElement.BLACK);
        var input=new CastingRecipeInput(grid,stands,runes,Map.of());
        h.assertTrue(recipe.matchesIgnoringAura(input),"the exact V33a Fortune stand and rune layout must match a lower-level star");
        ItemStack output=recipe.output(input);
        var fortune=recipe.completion().enhancement().orElseThrow().enchantment();
        h.assertTrue(output.getEnchantmentLevel(fortune)==5 && output.getEnchantmentLevel(silk)==1
                && output.get(DataComponents.CUSTOM_NAME).equals(center.get(DataComponents.CUSTOM_NAME))
                && output.get(DataComponents.CUSTOM_DATA).equals(center.get(DataComponents.CUSTOM_DATA)),
                "Fortune V must be added while retaining the name, Silk Touch and arbitrary custom data");
        h.assertTrue(center.getEnchantmentLevel(fortune)==0,"assembly must not mutate the central input");
        grid.set(4,output);
        h.assertTrue(!recipe.matchesIgnoringAura(new CastingRecipeInput(grid,stands,runes,Map.of())),"an already upgraded star must be rejected");
        var ops=RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess());
        var json=CastingTableRecipe.CODEC.codec().encodeStart(ops,recipe).getOrThrow();
        CastingTableRecipe decoded=CastingTableRecipe.CODEC.codec().parse(ops,json).getOrThrow();
        h.assertTrue(ItemStack.isSameItemSameComponents(decoded.output(input),output),
                "data codec round trips must preserve enhancement behavior");
        var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess(),
                net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        try {
            CastingTableRecipe.STREAM_CODEC.encode(wire,recipe);
            var synced=CastingTableRecipe.STREAM_CODEC.decode(wire);
            h.assertTrue(synced.completion().enhancement().equals(recipe.completion().enhancement())
                    && synced.completion().requiredFragment().equals("ENCHANTING"),"client recipe synchronization must retain the upgrade and fragment gate");
        } finally { wire.release(); }
        h.assertTrue(recipe(h,"excavation_star").stands().size()==12 && recipe(h,"construction_star").stands().size()==12
                && recipe(h,"hover_star").stands().size()==24,"the three base wand recipes must retain their authored stand counts");
        h.succeed();
    }
    private static void sonarItem(GameTestHelper h) {
        var player = survivalPlayer(h);
        ItemStack star = new ItemStack(ChromaItems.MOB_SONAR.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("ownerNote", "preserve me");
        star.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        var sonar = ChromaItems.MOB_SONAR.get();
        h.assertTrue(reika.chromaticraft.items.tools.wands.ItemMobSonar.getTypeFlags(star)==127,
                "a fresh sonar star must enable all seven source categories");
        reika.chromaticraft.items.tools.wands.ItemMobSonar.prepareData(star);
        tag = star.get(DataComponents.CUSTOM_DATA).copyTag();
        h.assertTrue(tag.getStringOr("ownerNote", "").equals("preserve me") && tag.getIntOr("typeFlags",0)==127,
                "preparing sonar flags must retain unrelated custom data");
        tag.putInt("typeFlags", 1 << reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.MOB.ordinal());
        star.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        reika.chromaticraft.items.tools.wands.ItemMobSonar.prepareData(star);
        h.assertTrue(reika.chromaticraft.items.tools.wands.ItemMobSonar.getTypeFlags(star)==16,
                "existing category selection must survive subsequent uses");
        player.setItemInHand(InteractionHand.MAIN_HAND, star);
        sonar.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.PINK)==996
                        && PlayerElementBuffer.instance.getPlayerContent(player,CrystalElement.BLUE)==996,
                "the source sonar cost is four pink and four blue lumens per use");
        var recipe = recipe(h,"creature_sounding_star");
        h.assertTrue(recipe.tier()==CastingTableRecipe.Tier.TEMPLE && recipe.runes().size()==2
                        && recipe.duration()==20 && recipe.experience()==40,
                "the source sonar recipe must retain its Temple tier, two runes, 20 ticks and 40 XP");
        h.succeed();
    }

    private static void sonarScan(GameTestHelper h) {
        var pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(10.9,6,10.9));
        var wolf = h.spawn(net.minecraft.world.entity.EntityTypes.WOLF, new Vec3(7,6,8));
        var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new Vec3(8,6,7));
        var squid = h.spawn(net.minecraft.world.entity.EntityTypes.SQUID, new Vec3(8,6,9));
        var slime = h.spawn(net.minecraft.world.entity.EntityTypes.SLIME, new Vec3(9,6,8));
        var villager = h.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER, new Vec3(8,6,8));
        var types = reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.values();
        h.assertTrue(reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.getFromEntity(pig)==types[3]
                        && reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.getFromEntity(wolf)==types[2]
                        && reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.getFromEntity(zombie)==types[4]
                        && reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.getFromEntity(slime)==types[6]
                        && reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.getFromEntity(squid)==types[5]
                        && reika.chromaticraft.auxiliary.MobSonar.EntitySonarType.getFromEntity(villager)==types[6],
                "the source category order must distinguish tameables, animals, EntityMob monsters, water mobs and other mobs");
        var ping = new reika.chromaticraft.auxiliary.MobSonar.Ping(
                Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(8,6,8))),64);
        Set<Integer> found = new HashSet<>();
        for (int i=0;i<24;i++) for (var mob : ping.tick(h.getLevel(),null))
            h.assertTrue(found.add(mob.getId()),"a ping must report each entity only once");
        h.assertTrue(found.contains(pig.getId()),"the expanding cube must include corner mobs outside a spherical scan");
        h.assertTrue(!ping.finished() && ping.radius()==3,"the radius must advance by precisely 1/8 block per tick");
        for (int i=24;i<512;i++) ping.tick(h.getLevel(),null);
        h.assertTrue(ping.finished() && ping.radius()==64,"the source 64-block wave must finish at tick 512");
        h.assertTrue(types[0].highlight==2 && types[2].highlight==1 && types[0].soundPitch<1 && types[4].soundPitch==(float)reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.C5.getRatio(reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.C4),
                "highlight layers and musical detection pitches must retain source values");
        h.succeed();
    }

    private static void multitool(GameTestHelper h) {
        var player = survivalPlayer(h);
        var tool = ChromaItems.MULTITOOL.get();
        ItemStack stack = new ItemStack(tool);
        var lookup = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var key : List.of(Enchantments.UNBREAKING,Enchantments.EFFICIENCY,Enchantments.FORTUNE,Enchantments.SILK_TOUCH))
            h.assertTrue(tool.supportsEnchantment(stack,lookup.getOrThrow(key)),"the Multitool must support all four original digging enchantments");
        h.assertTrue(!tool.supportsEnchantment(stack,lookup.getOrThrow(Enchantments.SHARPNESS)),
                "the Multitool must reject weapon enchantments");
        h.assertTrue(stack.getMaxDamage()==900 && tool.getDestroySpeed(stack,Blocks.OAK_LOG.defaultBlockState())==4
                        && tool.getDestroySpeed(stack,Blocks.DIRT.defaultBlockState())==4,
                "the Multitool must retain 900 uses and constant speed four on every material");
        BlockPos pos = h.absolutePos(new BlockPos(8,3,8));
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        tool.mineBlock(stack,h.getLevel(),Blocks.STONE.defaultBlockState(),pos,player);
        h.assertTrue(stack.getDamageValue()==1,"one nonzero-hardness block must consume one durability");
        tool.mineBlock(stack,h.getLevel(),Blocks.TORCH.defaultBlockState(),pos,player);
        h.assertTrue(stack.getDamageValue()==1,"zero-hardness blocks must not consume durability");
        var recipe = recipe(h,"multitool");
        h.assertTrue(recipe.runes().size()==2 && recipe.penaltyThreshold()==2,
                "the original two ring runes and repeat-XP threshold must survive datagen");
        h.succeed();
    }

    private static void oreHarvester(GameTestHelper h) {
        var player = survivalPlayer(h);
        ItemStack stack = new ItemStack(ChromaItems.ORE_HARVESTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var level=h.getLevel();
        BlockPos pos=h.absolutePos(new BlockPos(8,3,8));
        level.setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());
        var canceled = new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level,pos,level.getBlockState(pos),player);
        canceled.setCanceled(true);
        reika.chromaticraft.items.tools.ItemOreSilker.harvest(canceled);
        h.assertTrue(level.getBlockState(pos).is(Blocks.DIAMOND_ORE) && stack.getDamageValue()==0,
                "permission cancellation must retain the ore and tool durability");
        h.assertTrue(!player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(),
                "native completed mining must be replaced by the Harvester's silk loot");
        h.assertTrue(stack.getDamageValue()==1,"silking one ore must consume precisely one durability");
        BlockPos glow=pos.east(2);
        level.setBlockAndUpdate(glow,Blocks.GLOWSTONE.defaultBlockState());
        player.gameMode.destroyBlock(glow);
        h.assertTrue(level.getBlockState(glow).isAir() && stack.getDamageValue()==2,
                "glowstone must use the same source silk-harvesting path");
        BlockPos stone=pos.west(2);
        level.setBlockAndUpdate(stone,Blocks.STONE.defaultBlockState());
        var normal = new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level,stone,level.getBlockState(stone),player);
        reika.chromaticraft.items.tools.ItemOreSilker.harvest(normal);
        h.assertTrue(!normal.isCanceled() && level.getBlockState(stone).is(Blocks.STONE),
                "ordinary blocks must retain their normal harvest path");
        var lookup=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var tool=ChromaItems.ORE_HARVESTER.get();
        h.assertTrue(!tool.supportsEnchantment(stack,lookup.getOrThrow(Enchantments.FORTUNE))
                        && !tool.supportsEnchantment(stack,lookup.getOrThrow(Enchantments.SILK_TOUCH))
                        && tool.supportsEnchantment(stack,lookup.getOrThrow(Enchantments.UNBREAKING)),
                "automatic silk harvesting must not make Fortune or Silk Touch applicable enchantments");
        h.runAfterDelay(1,() -> {
            var items=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(4));
            h.assertTrue(items.stream().anyMatch(item->item.getItem().is(Items.DIAMOND_ORE))
                            && items.stream().anyMatch(item->item.getItem().is(Items.GLOWSTONE))
                            && items.stream().noneMatch(item->item.getItem().is(Items.DIAMOND)),
                    "the native loot context must produce ore and glowstone blocks, not their normal resources");
            h.assertTrue(recipe(h,"ore_harvester").runes().size()==4 && recipe(h,"ore_harvester").penaltyThreshold()==4,
                    "the Harvester must retain the source's four runes and repeat-XP threshold");
            h.succeed();
        });
    }


    private static void bottleneck(GameTestHelper h) {
        var level=h.getLevel();
        var a=new reika.dragonapi.instantiable.data.immutable.WorldLocation(level,h.absolutePos(new BlockPos(3,3,3)));
        var b=new reika.dragonapi.instantiable.data.immutable.WorldLocation(level,h.absolutePos(new BlockPos(4,3,3)));
        var c=new reika.dragonapi.instantiable.data.immutable.WorldLocation(level,h.absolutePos(new BlockPos(5,3,3)));
        var d=new reika.dragonapi.instantiable.data.immutable.WorldLocation(level,h.absolutePos(new BlockPos(6,3,3)));
        var e=new reika.dragonapi.instantiable.data.immutable.WorldLocation(level,h.absolutePos(new BlockPos(7,3,3)));
        var warnings=reika.chromaticraft.items.tools.ItemBottleneckFinder.rank(List.of(
                new reika.chromaticraft.items.tools.ItemBottleneckFinder.Measurement(a,10,1),
                new reika.chromaticraft.items.tools.ItemBottleneckFinder.Measurement(a,10,1),
                new reika.chromaticraft.items.tools.ItemBottleneckFinder.Measurement(b,10,4),
                new reika.chromaticraft.items.tools.ItemBottleneckFinder.Measurement(c,20,3),
                new reika.chromaticraft.items.tools.ItemBottleneckFinder.Measurement(d,30,2),
                new reika.chromaticraft.items.tools.ItemBottleneckFinder.Measurement(e,40,0)),CrystalElement.BLUE);
        var through=warnings.stream().filter(reika.chromaticraft.items.tools.ItemBottleneckFinder.Warning::throughput).toList();
        var loss=warnings.stream().filter(w->!w.throughput()).toList();
        h.assertTrue(through.size()==4 && through.get(0).location().equals(a) && through.get(1).location().equals(b)
                        && through.get(0).level().ordinal()==0 && through.get(1).level().ordinal()==0
                        && through.get(2).location().equals(c) && through.get(2).level().ordinal()==1
                        && through.get(3).location().equals(d) && through.get(3).level().ordinal()==2,
                "throughput ties must share a rank, repeated nodes must be deduplicated, and only three value groups warned");
        h.assertTrue(loss.size()==3 && loss.get(0).location().equals(b) && loss.get(1).location().equals(c)
                        && loss.get(2).location().equals(d),"loss warnings must rank high-to-low and omit nonpositive loss");
        h.assertTrue(warnings.stream().allMatch(w->w.color()==CrystalElement.BLUE),"warning colours must be retained per connected path");
        var payload=new reika.chromaticraft.network.ChromaNetwork.BottleneckWarning(level.dimension().identifier(),a.pos,2,false,CrystalElement.BLUE.ordinal());
        var buffer=Unpooled.buffer();
        try {
            reika.chromaticraft.network.ChromaNetwork.BottleneckWarning.CODEC.encode(buffer,payload);
            h.assertTrue(reika.chromaticraft.network.ChromaNetwork.BottleneckWarning.CODEC.decode(buffer).equals(payload),
                    "the native warning payload must retain dimension, position, category, rank and colour");
        } finally {buffer.release();}
        var recipe=recipe(h,"bottleneck_finder");
        h.assertTrue(recipe.tier()==CastingTableRecipe.Tier.MULTIBLOCK && recipe.stands().size()==24
                        && recipe.runes().isEmpty() && recipe.duration()==100 && recipe.experience()==200
                        && recipe.completion().requiredFragment().equals("BOTTLENECK") && recipe.penaltyThreshold()==1,
                "Aura Visualization Crystals must retain their 24-stand multiblock recipe and source progression gate");
        var player=survivalPlayer(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChromaItems.BOTTLENECK_FINDER.get()));
        var pos=h.absolutePos(new BlockPos(8,3,8));level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        h.assertTrue(ChromaItems.BOTTLENECK_FINDER.get().useOn(click(player,pos,Direction.UP))==net.minecraft.world.InteractionResult.PASS,
                "clicks on non-network blocks must pass through");
        h.succeed();
    }


    private static void spawnerCharge(GameTestHelper h) {
        var player=survivalPlayer(h);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var level=h.getLevel();var tool=ChromaItems.SPAWNER_BYPASS.get();
        player.getPersistentData().putLong("spawnertoggle",level.getGameTime()-1000);
        player.getPersistentData().putLong("spawnerpass",level.getGameTime()-1000);
        var stack=reika.chromaticraft.magic.ToolChargingSystem.instance.getChargedItem(tool,100);
        tool.inventoryTick(stack,level,player,net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        h.assertTrue(reika.chromaticraft.items.tools.powered.ItemSpawnerBypass.isActive(player)
                        && reika.chromaticraft.magic.ToolChargingSystem.instance.getCharge(stack)==100,
                "the carried crystal must activate without spending charge when no spawner is nearby");
        player.getPersistentData().putLong("spawnerpass",level.getGameTime());
        tool.inventoryTick(stack,level,player,net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        h.assertTrue(reika.chromaticraft.magic.ToolChargingSystem.instance.getCharge(stack)==99
                        && tool.getMaxCharge()==72000 && tool.getChargeStates()==1 && tool.getColor(stack)==CrystalElement.LIGHTGRAY,
                "a working crystal must spend one light gray lumen, with its source one-hour capacity and single sprite");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        tool.inventoryTick(stack,level,player,net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        h.assertTrue(reika.chromaticraft.magic.ToolChargingSystem.instance.getCharge(stack)==99,"creative carriers must retain charge");
        player.getPersistentData().putLong("spawnertoggle",level.getGameTime()-1000);
        tool.inventoryTick(new ItemStack(tool),level,player,net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        h.assertTrue(!reika.chromaticraft.items.tools.powered.ItemSpawnerBypass.isActive(player),"an empty crystal must not refresh activation");
        var recipe=recipe(h,"spawner_bypass");
        h.assertTrue(recipe.tier()==CastingTableRecipe.Tier.TEMPLE && recipe.grid().size()==9 && recipe.runes().size()==2
                        && recipe.duration()==20 && recipe.experience()==40 && recipe.penaltyThreshold()==1
                        && recipe.completion().requiredFragment().equals("SPAWNERBYPASS"),
                "the source nine-ingredient Temple recipe, runes and research gate must be retained");
        h.succeed();
    }

    private static short spawnerDelay(net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
        return spawner.saveWithoutMetadata(spawner.getLevel().registryAccess()).getShortOr("Delay",(short)-1);
    }

    private static void spawnerActivation(GameTestHelper h) {
        var player=survivalPlayer(h);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var level=h.getLevel();var tool=ChromaItems.SPAWNER_BYPASS.get();
        var pos=h.absolutePos(new BlockPos(8,5,8));var wall=pos.south(2);
        level.setBlockAndUpdate(pos,Blocks.SPAWNER.defaultBlockState());
        var spawner=(net.minecraft.world.level.block.entity.SpawnerBlockEntity)level.getBlockEntity(pos);
        player.setPos(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+4.5);
        player.getPersistentData().putLong("spawnertoggle",level.getGameTime()-1000);
        player.getPersistentData().putLong("spawnerpass",level.getGameTime()-1000);
        short initial=spawnerDelay(spawner);
        spawner.getSpawner().serverTick(level,pos);
        h.assertTrue(spawnerDelay(spawner)==initial-1,"ordinary nearby players must retain native spawner countdown");
        var stack=reika.chromaticraft.magic.ToolChargingSystem.instance.getChargedItem(tool,100);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        tool.inventoryTick(stack,level,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        spawner.getSpawner().serverTick(level,pos);
        h.assertTrue(spawnerDelay(spawner)==initial-1 && reika.chromaticraft.items.tools.powered.ItemSpawnerBypass.isWorking(player),
                "the actual BaseSpawner hook must stop countdown through opaque walls while marking nearby work");
        tool.inventoryTick(stack,level,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        h.assertTrue(reika.chromaticraft.magic.ToolChargingSystem.instance.getCharge(stack)==99,"blocking a nearby spawner must consume the working charge");
        level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());
        spawner.getSpawner().serverTick(level,pos);
        h.assertTrue(spawnerDelay(spawner)==initial-2,"opening line of sight must immediately restore the native countdown");
        level.setBlockAndUpdate(wall,Blocks.COBWEB.defaultBlockState());
        spawner.getSpawner().serverTick(level,pos);
        h.assertTrue(spawnerDelay(spawner)==initial-3,"cobweb must remain explicitly transparent to the original spawner tracer");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setPos(pos.getX()+40.5,pos.getY()+0.5,pos.getZ()+0.5);
        player.getPersistentData().putLong("spawnerpass",level.getGameTime()-1000);
        spawner.getSpawner().serverTick(level,pos);
        h.assertTrue(spawnerDelay(spawner)==initial-3 && !reika.chromaticraft.items.tools.powered.ItemSpawnerBypass.isWorking(player),
                "players outside the required range must not activate or mark nearby work");
        h.runAfterDelay(6,()-> {
            h.assertTrue(!reika.chromaticraft.items.tools.powered.ItemSpawnerBypass.isActive(player),"removed crystals must expire after the source five-tick grace period");
            h.succeed();
        });
    }


    private static void magicBranches(GameTestHelper h) {
        var seen=new HashSet<net.minecraft.world.item.Item>();
        for (var type : reika.chromaticraft.items.ItemMagicBranch.BranchTypes.list) {
            var item=ChromaItems.BRANCHES.get(type).get();
            h.assertTrue(seen.add(item) && item.branchType()==type && type.getStack().getItem()==item
                            && type.getStack().getMaxStackSize()==64,"each source branch metadata value must map to its own native stackable item");
            int phase=(type.ordinal()*4)%16;
            h.assertTrue(item.frameAt(0)==phase && item.frameAt(127)==phase && item.frameAt(128)==(phase+1)%16
                            && item.frameAt(2048)==phase,"all sixteen original frames must retain 128ms timing, phase and wraparound");
        }
        h.assertTrue(seen.size()==6,"the complete six-variant source branch family must be registered");
        h.succeed();
    }

    private static void bulkTransfers(GameTestHelper h) {
        var player=survivalPlayer(h);
        var item=ChromaItems.BULK_MOVER.get();var tool=new ItemStack(item);
        var template=new ItemStack(Items.DIAMOND);
        template.set(DataComponents.CUSTOM_NAME,Component.literal("Stored component"));
        reika.chromaticraft.items.tools.ItemBulkMover.setStoredItem(tool,template);
        reika.chromaticraft.items.tools.ItemBulkMover.setNumberToCarry(tool,100);
        ReikaItemHelper.updateStackTag(tool,tag->tag.putString("unrelated","keep"));
        var source=h.absolutePos(new BlockPos(3,3,3));
        var destination=source.east(4);var level=h.getLevel();
        level.setBlockAndUpdate(source,Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(destination,Blocks.CHEST.defaultBlockState());
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(source);
        chest.setItem(0,template.copyWithCount(64));chest.setItem(1,template.copyWithCount(64));
        chest.setItem(2,new ItemStack(Items.DIAMOND,64));
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        item.useOn(click(player,source,Direction.UP));
        h.assertTrue(reika.chromaticraft.items.tools.ItemBulkMover.getStoredItems(tool)==100
                && chest.getItem(0).getCount()+chest.getItem(1).getCount()==28 && chest.getItem(2).getCount()==64,
                "native chest extraction must move more than a stack and match the complete template components");
        var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,tool).getOrThrow()).getOrThrow();
        h.assertTrue(reika.chromaticraft.items.tools.ItemBulkMover.getStoredItems(restored)==100
                        && ItemStack.isSameItemSameComponents(reika.chromaticraft.items.tools.ItemBulkMover.getStoredItem(restored),template)
                        && ReikaItemHelper.getStackTag(restored).getStringOr("unrelated","").equals("keep"),
                "bulk count, native template components and unrelated custom data must survive native stack serialization");
        player.setItemInHand(InteractionHand.MAIN_HAND,restored);
        var target=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(destination);
        for(int i=0;i<27;i++) target.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        item.useOn(click(player,destination,Direction.UP));
        h.assertTrue(reika.chromaticraft.items.tools.ItemBulkMover.getStoredItems(restored)==100,
                "a full destination must leave the entire stored count intact");
        target.setItem(0,template.copyWithCount(54));
        item.useOn(click(player,destination,Direction.UP));
        h.assertTrue(target.getItem(0).getCount()==64 && reika.chromaticraft.items.tools.ItemBulkMover.getStoredItems(restored)==90,
                "partial insertion must subtract exactly the accepted ten items");
        target.setItem(1,ItemStack.EMPTY);target.setItem(2,ItemStack.EMPTY);
        item.useOn(click(player,destination,Direction.UP));
        h.assertTrue(reika.chromaticraft.items.tools.ItemBulkMover.getStoredItems(restored)==0
                        && target.getItem(1).getCount()+target.getItem(2).getCount()==90,
                "subsequent deposits must empty the tool while preserving every remaining item");
        h.succeed();
    }

    private static void bulkMenu(GameTestHelper h) {
        var player=survivalPlayer(h);var item=ChromaItems.BULK_MOVER.get();var tool=new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var menu=new reika.chromaticraft.container.MenuBulkMover(19,player.getInventory());
        var cursor=new ItemStack(Items.EMERALD,37);
        cursor.set(DataComponents.CUSTOM_NAME,Component.literal("Ghost sample"));
        menu.setCarried(cursor);
        menu.clicked(0,0,net.minecraft.world.inventory.ContainerInput.PICKUP,player);
        h.assertTrue(menu.getCarried().getCount()==37 && menu.getSlot(0).getItem().getCount()==1
                        && ItemStack.isSameItemSameComponents(reika.chromaticraft.items.tools.ItemBulkMover.getStoredItem(tool),cursor),
                "the ghost selector must copy one full-component template without consuming the cursor");
        h.assertTrue(menu.setNumberToCarry(player,99999) && menu.numberToCarry()==99999
                        && !menu.setNumberToCarry(player,-1) && !menu.setNumberToCarry(player,100000),
                "the server must accept the full five-digit quantity and reject values outside the source field");
        var packet=new reika.chromaticraft.network.ChromaNetwork.BulkMoverNumber(19,99999);
        var buffer=Unpooled.buffer();
        try {
            reika.chromaticraft.network.ChromaNetwork.BulkMoverNumber.CODEC.encode(buffer,packet);
            h.assertTrue(packet.equals(reika.chromaticraft.network.ChromaNetwork.BulkMoverNumber.CODEC.decode(buffer)),
                    "configuration packets must round-trip their menu identity and five-digit quantity");
        } finally {buffer.release();}
        player.setItemInHand(InteractionHand.MAIN_HAND,tool.copy());
        h.assertTrue(!menu.stillValid(player) && !menu.setNumberToCarry(player,1),
                "replacing the held tool must invalidate the old menu and its quantity updates");
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        ReikaItemHelper.updateStackTag(tool,tag->tag.putInt("count",1));
        h.assertTrue(!menu.stillValid(player),"filled movers must reject template reconfiguration");
        CastingTableRecipe recipe=recipe(h,"bulk_mover");
        h.assertTrue(recipe.tier()==CastingTableRecipe.Tier.TEMPLE && recipe.grid().size()==7 && recipe.runes().size()==2
                        && recipe.duration()==20 && recipe.experience()==40 && recipe.penaltyThreshold()==1
                        && recipe.runes().get(0).offset().equals(new BlockPos(-1,-1,-4))
                        && recipe.runes().get(1).offset().equals(new BlockPos(1,-1,4))
                        && recipe.completion().requiredFragment().equals("BULKMOVER"),
                "the mover must keep its seven original ingredients, exact lime runes, timing and research gate");
        h.succeed();
    }

}
