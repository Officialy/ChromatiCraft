package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.ChromaGameTests;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.registry.VoidEntities;

/** Exercises the real optional API, damage-event pipeline and scheduled ritual attacks. */
public final class VoidCompatibilityGameTests {
    private VoidCompatibilityGameTests() {}
    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event,env,"void_compat_nether_transfer",140,VoidCompatibilityGameTests::netherTransfer);
        ChromaGameTests.register(event,env,"void_compat_essence_progress",80,VoidCompatibilityGameTests::essence);
        ChromaGameTests.register(event,env,"void_compat_pylon_bonus",80,VoidCompatibilityGameTests::pylonBonus);
        ChromaGameTests.register(event,env,"void_compat_trap_inventory",80,VoidCompatibilityGameTests::inventory);
        ChromaGameTests.register(event,env,"void_compat_ritual_protection",80,VoidCompatibilityGameTests::protection);
        ChromaGameTests.register(event,env,"void_compat_ritual_delayed",100,VoidCompatibilityGameTests::delayed);
    }
    private static EntityVoidMonster monster(GameTestHelper h) {
        var entity=VoidEntities.VOID_MONSTER.get().create(h.getLevel(),EntitySpawnReason.EVENT);
        h.assertTrue(entity!=null,"VoidMonster factory must be present");
        entity.forcePersist=true; entity.snapTo(h.absoluteVec(new Vec3(4,3,4))); h.getLevel().addFreshEntity(entity);
        return entity;
    }
    private static TileEntityVoidMonsterTrap trap(GameTestHelper h) {
        var at=h.absolutePos(new BlockPos(4,2,4)); h.getLevel().setBlockAndUpdate(at,ChromaBlocks.VOID_MONSTER_TRAP.get().defaultBlockState());
        var tile=(TileEntityVoidMonsterTrap)h.getLevel().getBlockEntity(at);
        h.assertTrue(tile!=null,"trap block must instantiate its actual receiver");
        tile.setPlacer(reika.chromaticraft.ChromaGameTests.joinedCreativeMockPlayer(h)); return tile;
    }
    private static void pylonBonus(GameTestHelper h) {
        var entity=monster(h);
        // V33a retains immunity to ordinary damage without a player attacker. Exercise
        // the actual death/drop pipeline with a pylon source, preserving that admission rule.
        entity.setHealth(0);
        entity.die(reika.chromaticraft.auxiliary.PylonDamage.source(h.getLevel(),CrystalElement.BLACK));
        h.assertTrue(entity.isDeadOrDying(), "the native pylon-attributed death must enter the drop pipeline");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,entity.getBoundingBox().inflate(3));
        int fragments=drops.stream().filter(item->item.getItem().is(ChromaItems.INFO_FRAGMENT.get())).mapToInt(item->item.getItem().getCount()).sum();
        int fireCharges=drops.stream().filter(item->item.getItem().is(net.minecraft.world.item.Items.FIRE_CHARGE)).mapToInt(item->item.getItem().getCount()).sum();
        h.assertTrue(fragments>=4 && fragments<=7,"a pylon kill must grant the authored four to seven blank research fragments");
        h.assertTrue(fireCharges>=6,"a pylon kill must perform the normal drop roll and two complete bonus rolls");
        drops.forEach(ItemEntity::discard); entity.discard(); h.succeed();
    }
    private static void essence(GameTestHelper h) {
        var entity=monster(h); var player=reika.chromaticraft.ChromaGameTests.joinedCreativeMockPlayer(h);
        entity.doGhostDamage(player,0,1);
        var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,entity.getBoundingBox().inflate(3), e->e.getItem().is(ChromaItems.VOID_MONSTER_ESSENCE.get()));
        h.assertTrue(!items.isEmpty(),"a 50-damage player ghost hit must produce corrupted essence through LivingIncomingDamageEvent");
        reika.chromaticraft.magic.progression.ProgressionManager.instance.setPlayerStage(player,ProgressStage.BEDROCK,true,false,false);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new reika.voidmonster.api.PlayerLookAtVoidMonsterEvent(player,entity));
        h.assertTrue(ProgressStage.VOIDMONSTER.isPlayerAtStage(player),"the public look event must grant VoidMonster progression");
        items.forEach(ItemEntity::discard); entity.discard(); h.succeed();
    }
    private static void inventory(GameTestHelper h) {
        var tile=trap(h); Vec3 at=Vec3.atCenterOf(tile.getBlockPos());
        var item=new ItemEntity(h.getLevel(),at.x,at.y,at.z,new ItemStack(ChromaItems.VOID_MONSTER_ESSENCE.get(),3));
        item.tickCount=80; h.getLevel().addFreshEntity(item); tile.updateEntity(h.getLevel(),tile.getBlockPos());
        h.assertTrue(tile.getItem(0).getCount()==1 && item.getItem().getCount()==2,"trap vacuum must transfer one essence per tick without duplicating the stack");
        h.assertTrue(!tile.canPlaceItem(0,new ItemStack(ChromaItems.VOID_MONSTER_ESSENCE.get())),"trap must retain its authored vacuum-only inventory input");
        tile.setEnergy(CrystalElement.PINK,tile.getMaxStorage(CrystalElement.PINK));
        h.assertTrue(tile.getEnergy(CrystalElement.PINK)==12000 && tile.getReceiveRange()==24 && tile.maxThroughput()==100,"trap must retain receiver capacities and throughput");
        item.discard(); h.getLevel().removeBlock(tile.getBlockPos(),false); h.succeed();
    }
    private static void protection(GameTestHelper h) {
        var tile=trap(h); var entity=monster(h); var ritual=new VoidMonsterDestructionRitual(tile,entity);
        float health=entity.getHealth();
        h.assertTrue(VoidMonsterDestructionRitual.isFocusOfActiveRitual(entity),"ritual focus must be visible to the damage hook immediately");
        entity.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(tile.getPlacer()),10);
        h.assertTrue(entity.getHealth()==health,"external player damage must be canceled while a ritual is active");
        VoidMonsterDestructionRitual.Effects.RAYS.doEffectServer(ritual,entity);
        h.assertTrue(entity.getHealth()==health-40,"the ritual's own source must pass protection and apply its authored health subtraction");
        float difficulty=entity.getDifficulty(), previousMaximum=entity.getMaxHealth(); ritual.onPrematureTermination();
        h.assertTrue(!VoidMonsterDestructionRitual.isFocusOfActiveRitual(entity) && entity.getHealth()==previousMaximum && entity.getDifficulty()>difficulty,
                "breaking the ritual must restore the monster, escalate difficulty and release focus protection");
        entity.discard(); h.getLevel().removeBlock(tile.getBlockPos(),false); h.succeed();
    }
    private static void delayed(GameTestHelper h) {
        var tile=trap(h); var entity=monster(h); var ritual=new VoidMonsterDestructionRitual(tile,entity);
        float health=entity.getHealth(); VoidMonsterDestructionRitual.Effects.CURL.doEffectServer(ritual,entity);
        h.assertTrue(entity.getHealth()==health,"curl damage must wait for its 30-tick deadline");
        h.runAfterDelay(31,()-> {
            h.assertTrue(entity.getHealth()<health-35,"scheduled curl damage must fire through the server tick handler");
            ritual.onCompletion(); h.assertTrue(!VoidMonsterDestructionRitual.isFocusOfActiveRitual(entity),"completion must clear focus and sync the remaining ritual set");
            entity.discard(); h.getLevel().removeBlock(tile.getBlockPos(),false); h.succeed();
        });
    }
    private static void netherTransfer(GameTestHelper h) {
        var destination=trap(h);
        var nether=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        h.assertTrue(nether!=null,"the real Nether level must exist");
        var anchor=new BlockPos(h.absolutePos(new BlockPos(4,0,4)).getX(),150,h.absolutePos(new BlockPos(4,0,4)).getZ());
        var array=new VoidMonsterNetherStructure().getArray(nether,anchor.getX(),anchor.getY(),anchor.getZ());
        array.place(net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE | net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE);
        array.matchInWorld((level,x,y,z,expected)->h.fail("Nether layout mismatch at "+new BlockPos(x,y,z)+": expected "+expected.asBlockKey().blockID+", found "+level.getBlockState(new BlockPos(x,y,z))));
        var source=(TileEntityVoidMonsterTrap)nether.getBlockEntity(anchor);
        source.updateEntity(); source.validateStructure();
        h.assertTrue(source.hasStructure(),"the authored Nether layout must match after placement");
        h.assertTrue(source.tryConnect(h.getLevel(),destination.getBlockPos()) && source.hasValidConnection(),"a real cross-dimension link must resolve both traps");
        var entity=VoidEntities.VOID_MONSTER.get().create(nether,EntitySpawnReason.EVENT);
        entity.setNether(); entity.forcePersist=true; entity.snapTo(Vec3.atCenterOf(anchor).add(0,-1,0)); nether.addFreshEntity(entity);
        var uuid=entity.getUUID(); int triggered=0;
        for (var point:VoidMonsterNetherStructure.getTNTLocations()) {
            var tntAt=anchor.offset(point.xCoord() -2, point.yCoord() -6, point.zCoord() -2);
            var tnt=new net.minecraft.world.entity.item.PrimedTnt(nether,tntAt.getX()+.5,tntAt.getY(),tntAt.getZ()+.5,null);
            h.assertTrue(TileEntityVoidMonsterTrap.handleTNTTrigger(nether,tnt),"each authored TNT point must be consumed by the trigger handler");
            if (++triggered<4) h.assertTrue(nether.getEntity(uuid)!=null,"teleport must wait for all four TNT triggers");
            tnt.discard();
        }
        h.assertTrue(entity.isRemoved() && nether.getEntity(uuid)==null,"the fourth TNT must remove the source entity after native dimension transfer");
        h.assertTrue(!nether.getBlockState(anchor).is(ChromaBlocks.VOID_MONSTER_TRAP.get()),"the consumed Nether focus must remove itself");
        h.succeedWhen(()-> {
            var moved=h.getLevel().getEntity(uuid);
            h.assertTrue(moved instanceof EntityVoidMonster,"the native destination lookup must track the transferred monster UUID");
            h.assertTrue(h.getLevel().noCollision(moved,moved.getBoundingBox()),"the arrival must fit the monster's actual native bounding box");
            moved.discard(); h.getLevel().removeBlock(destination.getBlockPos(),false);
        });
    }

}
