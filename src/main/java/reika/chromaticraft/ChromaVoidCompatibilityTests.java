package reika.chromaticraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.auxiliary.ability.AbilityEffects;
import reika.chromaticraft.entity.EntityDeathFog;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.TileEntityDeathFogEmitter;

/** Shared content must function with either optional-mod runtime configuration. */
public final class ChromaVoidCompatibilityTests {
    private ChromaVoidCompatibilityTests() {}
    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event, env, "void_compat_bait", 80, ChromaVoidCompatibilityTests::bait);
        ChromaGameTests.register(event, env, "void_compat_fog", 80, ChromaVoidCompatibilityTests::fog);
        ChromaGameTests.register(event, env, "void_compat_emitter", 80, ChromaVoidCompatibilityTests::emitter);
        ChromaGameTests.register(event, env, "void_compat_layouts", 80, ChromaVoidCompatibilityTests::layouts);
    }
    private static void bait(GameTestHelper h) {
        var player=h.makeMockServerPlayerInLevel();
        player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4))));
        player.setOnGround(true);
        h.assertTrue(AbilityEffects.mobBait(player), "grounded MOBBAIT must create a real entity");
        h.assertTrue(!AbilityEffects.mobBait(player), "nearby bait must block a second cast in the overworld");
        var bait=h.getLevel().getEntitiesOfClass(reika.chromaticraft.entity.EntityMonsterBait.class, player.getBoundingBox().inflate(6)).getFirst();
        h.assertTrue(bait.getLife()>=100 && bait.getLife()<=600 && bait.isActive(), "bait must retain its authored lifetime and owner");
        var zombie=EntityTypes.ZOMBIE.create(h.getLevel(),EntitySpawnReason.COMMAND);
        zombie.setNoAi(true); zombie.setPos(bait.position().add(2,0,0)); h.getLevel().addFreshEntity(zombie);
        int before=bait.getLife(); bait.tick();
        h.assertTrue(zombie.getTarget()==bait && bait.getLife()==before-1, "nearest hostile mob must target the bait while its lifetime decreases");
        bait.attack(40); h.assertTrue(bait.getLife()==before-11, "VoidMonster attacks must drain a quarter of the damage from bait life");
        bait.discard(); zombie.discard(); h.succeed();
    }
    private static void fog(GameTestHelper h) {
        Vec3 center=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,3,4)));
        var zombie=EntityTypes.ZOMBIE.create(h.getLevel(),EntitySpawnReason.COMMAND);
        var cow=EntityTypes.COW.create(h.getLevel(),EntitySpawnReason.COMMAND);
        zombie.setNoAi(true); zombie.setPos(center.add(1,0,0)); cow.setPos(center.add(0,0,1));
        h.getLevel().addFreshEntity(zombie); h.getLevel().addFreshEntity(cow);
        float cowHealth=cow.getHealth(), health=zombie.getHealth();
        var normal=new EntityDeathFog(h.getLevel(),center.x,center.y,center.z,.1,0,0,false);
        h.getLevel().addFreshEntity(normal); normal.tick();
        h.assertTrue(zombie.getHealth()==health-1 && cow.getHealth()==cowHealth, "ordinary fog must damage only hostile mobs");
        h.assertTrue(Math.abs(normal.getDeltaMovement().x-.1)<.00001, "fog collision movement must preserve drift velocity");
        zombie.damageCooldownTime=0; health=zombie.getHealth();
        var enhanced=new EntityDeathFog(h.getLevel(),center.x,center.y,center.z,0,0,0,true);
        h.getLevel().addFreshEntity(enhanced); enhanced.tick();
        h.assertTrue(zombie.getHealth()==health-.5F && zombie.damageCooldownTime==11, "enhanced fog must deal half damage with the authored repeat-hit window");
        normal.destroy(); enhanced.destroy(); zombie.discard(); cow.discard(); h.succeed();
    }
    private static void emitter(GameTestHelper h) {
        BlockPos at=h.absolutePos(new BlockPos(8,2,8));
        h.getLevel().setBlockAndUpdate(at,ChromaBlocks.DEATH_FOG_EMITTER.get().defaultBlockState());
        var tile=(TileEntityDeathFogEmitter)h.getLevel().getBlockEntity(at);
        h.assertTrue(tile!=null && tile.getMaxStackSize()==32, "emitter must be a real one-slot block entity");
        tile.setItem(0,new ItemStack(ChromaItems.VOID_MONSTER_ESSENCE.get(),32));
        tile.updateEntity(h.getLevel(),at);
        var fog=h.getLevel().getEntitiesOfClass(EntityDeathFog.class,new net.minecraft.world.phys.AABB(at).inflate(1));
        h.assertTrue(!fog.isEmpty() && fog.stream().allMatch(EntityDeathFog::isEnhanced), "corrupted essence must enhance the emitted fog");
        fog.forEach(EntityDeathFog::destroy); h.getLevel().removeBlock(at,false); h.succeed();
    }
    private static void layouts(GameTestHelper h) {
        var ritual=new reika.chromaticraft.modinterface.voidritual.VoidMonsterRitualStructure().getArray(h.getLevel(),0,0,0);
        var nether=new reika.chromaticraft.modinterface.voidritual.VoidMonsterNetherStructure().getArray(h.getLevel(),0,0,0);
        h.assertTrue(ritual.getMaxX()-ritual.getMinX()==28 && ritual.getMaxZ()-ritual.getMinZ()==28, "ritual must retain its full 29 by 29 footprint");
        long tnt=nether.keySet().stream().filter(pos->nether.getBlockAt(pos.getX(),pos.getY(),pos.getZ())==net.minecraft.world.level.block.Blocks.TNT).count();
        h.assertTrue(tnt==4, "Nether trap must retain exactly four TNT trigger points");
        var altar=ChromaBlocks.COLORED_ALTARS.get(CrystalElement.WHITE).get();
        BlockPos at=h.absolutePos(new BlockPos(8,2,8)); h.getLevel().setBlockAndUpdate(at,altar.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockEntity(at) instanceof reika.chromaticraft.block.decoration.BlockColoredAltar.TileEntityColoredAltar,
                "all colored altars must instantiate their full native block entity");
        h.assertTrue(altar.defaultBlockState().getShape(h.getLevel(),at).bounds().maxY==.5, "altar must retain its half-height collision shape");
        h.getLevel().removeBlock(at,false); h.succeed();
    }
}
