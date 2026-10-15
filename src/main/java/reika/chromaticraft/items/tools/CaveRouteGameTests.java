package reika.chromaticraft.items.tools;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.ChromaGameTests;
import reika.chromaticraft.block.BlockChromaTrail.TileChromaTrail;
import reika.chromaticraft.data.ChromaTestStructureProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;

public final class CaveRouteGameTests {
    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event, env, "cave_route_deferred_exit", 100, ChromaTestStructureProvider.WAND_ARENA, CaveRouteGameTests::exit);
        ChromaGameTests.register(event, env, "cave_route_deepest", 100, ChromaTestStructureProvider.WAND_ARENA, CaveRouteGameTests::deep);
        ChromaGameTests.register(event, env, "cave_route_trail_cycles", 30, ChromaTestStructureProvider.WAND_ARENA, CaveRouteGameTests::cycles);
        ChromaGameTests.register(event, env, "cave_route_replacement_cancellation", 30, ChromaTestStructureProvider.WAND_ARENA, CaveRouteGameTests::cancellation);
    }
    private static ServerPlayer player(GameTestHelper h, BlockPos pos) {
        var player = ChromaGameTests.joinedMockPlayer(h, net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChromaItems.CAVE_PATHER.get()));
        return player;
    }
    private static void chamber(GameTestHelper h) {
        for (BlockPos pos : BlockPos.betweenClosed(h.absolutePos(new BlockPos(1,1,1)),h.absolutePos(new BlockPos(14,14,14))))
            h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
    }
    private static void use(GameTestHelper h, ServerPlayer player) {
        ChromaItems.CAVE_PATHER.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
    }
    private static void exit(GameTestHelper h) {
        chamber(h);
        BlockPos start=h.absolutePos(new BlockPos(3,5,7)), end=h.absolutePos(new BlockPos(11,5,7));
        for(int x=0;x<=8;x++) h.getLevel().setBlock(start.east(x),Blocks.AIR.defaultBlockState(),3);
        for(int y=end.getY();y<=h.getLevel().getMaxY();y++) h.getLevel().setBlock(new BlockPos(end.getX(),y,end.getZ()),Blocks.AIR.defaultBlockState(),3);
        var player=player(h,start);use(h,player);
        h.assertTrue(h.getLevel().getBlockState(start).isAir(),"use must queue, without searching or placing in the click tick");
        h.succeedWhen(()->{
            h.assertTrue(h.getLevel().getBlockState(end).is(ChromaBlocks.CHROMA_TRAIL.get()),"exit waypoint eventually placed by native server ticks");
            var trail=(TileChromaTrail)h.getLevel().getBlockEntity(start);
            h.assertTrue(trail.getFullPath().equals(java.util.List.of(start,start.east(4),end)),"original spacing, order and links retained");
        });
    }
    private static void deep(GameTestHelper h) {
        chamber(h);
        BlockPos start=h.absolutePos(new BlockPos(4,8,7)), deepest=start.east(5).below(5);
        for(int y=0;y<=2;y++)h.getLevel().setBlock(start.below(y),Blocks.AIR.defaultBlockState(),3);
        for(int x=0;x<=5;x++)h.getLevel().setBlock(start.east(x),Blocks.AIR.defaultBlockState(),3);
        for(int y=0;y<=5;y++)h.getLevel().setBlock(start.east(5).below(y),Blocks.AIR.defaultBlockState(),3);
        var player=player(h,start);player.setShiftKeyDown(true);use(h,player);
        h.assertTrue(h.getLevel().getBlockState(start).isAir(),"sneak search also deferred");
        h.succeedWhen(()->h.assertTrue(h.getLevel().getBlockState(deepest).is(ChromaBlocks.CHROMA_TRAIL.get()),"deepest branch reached, rather than first local minimum"));
    }
    private static void cycles(GameTestHelper h) {
        BlockPos a=h.absolutePos(new BlockPos(4,5,7)),b=a.east(4),c=b.east(4);
        for(var pos:java.util.List.of(a,b,c))h.getLevel().setBlock(pos,ChromaBlocks.CHROMA_TRAIL.get().defaultBlockState(),3);
        var first=(TileChromaTrail)h.getLevel().getBlockEntity(a);
        var middle=(TileChromaTrail)h.getLevel().getBlockEntity(b);
        var last=(TileChromaTrail)h.getLevel().getBlockEntity(c);
        first.setData(c,b);middle.setData(a,c);last.setData(b,a);
        var path=middle.getFullPath();
        h.assertTrue(path.size()==3&&new java.util.HashSet<>(path).size()==3,"cyclic trail visits each point once");
        last.setData(b,new BlockPos(30_000_000,c.getY(),30_000_000));first.setData((BlockPos)null,b);
        h.assertTrue(middle.getFullPath().equals(java.util.List.of(a,b,c)),"unloaded link ends traversal without chunk loading");h.succeed();
    }
    private static long jobsFor(ServerPlayer player) {
        try {
            var queue=CaveRouteJobs.class.getDeclaredField("JOBS");queue.setAccessible(true);
            long count=0;
            for(Object job:(java.util.Collection<?>)queue.get(null)) {
                var owner=job.getClass().getDeclaredField("player");owner.setAccessible(true);
                if(owner.get(job).equals(player.getUUID()))count++;
            }
            return count;
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void cancellation(GameTestHelper h) {
        chamber(h);BlockPos start=h.absolutePos(new BlockPos(4,8,7));h.getLevel().setBlock(start,Blocks.AIR.defaultBlockState(),3);
        var player=player(h,start);player.setShiftKeyDown(true);use(h,player);use(h,player);
        h.assertTrue(jobsFor(player)==1,"repeated use replaces the old job rather than stacking searches");
        player.setHealth(0);
        h.runAfterDelay(3,()->{
            h.assertTrue(jobsFor(player)==0&&h.getLevel().getBlockState(start).isAir(),"dead player's pending job is released without world edits");h.succeed();
        });
    }
}
