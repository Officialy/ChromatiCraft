package reika.chromaticraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.tileentity.transport.TileEntityLaunchPad;

/** Real generated launch-pad template, server validation, enhancement and sync data. */
final class ChromaLaunchPadTests {
    static void register(RegisterGameTestsEvent event,Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event,env,"launch_pad_validation",80,ChromaLaunchPadTests::validation);
        ChromaGameTests.register(event,env,"launch_pad_enhancement_and_sync",80,ChromaLaunchPadTests::sync);
    }
    private static TileEntityLaunchPad place(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));
        ChromaStructures.LAUNCHPAD.getArray(h.getLevel(),pos.getX(),pos.getY(),pos.getZ()).place();
        return (TileEntityLaunchPad)h.getLevel().getBlockEntity(pos);
    }
    private static void validation(GameTestHelper h) {
        var pad=place(h);pad.validateStructure();
        h.assertTrue(pad.hasStructure() && !pad.isEnhanced(),"authored frame must validate without an ender ring");
        var corner=pad.getBlockPos().offset(-1,0,-1);var saved=h.getLevel().getBlockState(corner);
        h.getLevel().setBlock(corner,Blocks.AIR.defaultBlockState(),3);pad.validateStructure();
        h.assertTrue(!pad.hasStructure(),"broken frame must invalidate the source structure");
        h.getLevel().setBlock(corner,saved,3);pad.validateStructure();
        h.assertTrue(pad.hasStructure(),"restored exact frame must validate again");h.succeed();
    }
    private static void sync(GameTestHelper h) {
        var pad=place(h);var pos=pad.getBlockPos();
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) if(Math.abs(x)==2 || Math.abs(z)==2)
            h.getLevel().setBlock(pos.offset(x,0,z),ChromaFluids.ENDER.get().defaultFluidState().createLegacyBlock(),3);
        pad.validateStructure();h.assertTrue(pad.hasStructure() && pad.isEnhanced(),"complete source-ender ring must enhance a real frame");
        CompoundTag saved=pad.saveWithoutMetadata(h.getLevel().registryAccess());saved.putInt("charge",10);
        var copy=new TileEntityLaunchPad(pos,ChromaBlocks.LAUNCH_PAD.get().defaultBlockState());
        pad.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        copy.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),pad.getUpdateTag(h.getLevel().registryAccess())));
        h.assertTrue(copy.hasStructure() && copy.isEnhanced() && copy.getChargeFraction()==.5F,"structure/enhancement/charge must survive native save and update-tag loading");
        h.getLevel().setBlock(pos.offset(2,0,0),Blocks.AIR.defaultBlockState(),3);pad.validateStructure();
        h.assertTrue(pad.hasStructure() && !pad.isEnhanced(),"partial ender ring must remove enhancement without invalidating the frame");h.succeed();
    }
}
