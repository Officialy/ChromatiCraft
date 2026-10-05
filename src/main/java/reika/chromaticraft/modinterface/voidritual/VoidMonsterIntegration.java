package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaItems;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.api.PlayerLookAtVoidMonsterEvent;
import reika.voidmonster.api.VoidMonsterEatLightEvent;

public final class VoidMonsterIntegration {
    private VoidMonsterIntegration() {}
    public static void register(IEventBus modBus) {
        modBus.addListener(VoidRitualNetwork::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,VoidMonsterIntegration::damage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,VoidMonsterIntegration::look);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,VoidMonsterIntegration::death);
        NeoForge.EVENT_BUS.addListener(VoidMonsterIntegration::pylonBonus);
        NeoForge.EVENT_BUS.addListener(VoidMonsterIntegration::eatLight);
        NeoForge.EVENT_BUS.addListener(VoidMonsterIntegration::explosion);
        NeoForge.EVENT_BUS.addListener(VoidMonsterIntegration::tick);
        NeoForge.EVENT_BUS.addListener(VoidMonsterIntegration::unload);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)-> { if (event.getEntity().level() instanceof ServerLevel server) VoidMonsterDestructionRitual.sync(server); });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event)-> { if (event.getEntity().level() instanceof ServerLevel server) VoidMonsterDestructionRitual.sync(server); });
    }
    private static void damage(LivingIncomingDamageEvent event) {
        if (VoidMonsterDestructionRitual.isFocusOfActiveRitual(event.getEntity()) && !(event.getSource() instanceof VoidMonsterDestructionRitual.VoidMonsterRitualDamage)) { event.setCanceled(true); return; }
        if (event.getEntity() instanceof EntityVoidMonster monster && event.getSource().getEntity() instanceof Player && monster.level() instanceof ServerLevel server) {
            float number=event.getAmount()/40F;
            while (number>1) { monster.spawnAtLocation(server,new net.minecraft.world.item.ItemStack(ChromaItems.VOID_MONSTER_ESSENCE.get())); number--; }
            if (number>0 && monster.getRandom().nextFloat()<number) monster.spawnAtLocation(server,new net.minecraft.world.item.ItemStack(ChromaItems.VOID_MONSTER_ESSENCE.get()));
        }
        if (event.getEntity() instanceof Player player && event.getAmount()>=1 && event.getSource().getEntity() instanceof EntityVoidMonster) ProgressStage.VOIDMONSTER.stepPlayerTo(player);
    }
    private static void look(PlayerLookAtVoidMonsterEvent event) { if (!event.getEntity().level().isClientSide()) ProgressStage.VOIDMONSTER.stepPlayerTo(event.getEntity()); }
    private static void death(LivingDeathEvent event) { if (event.getEntity() instanceof Player player && event.getSource().getEntity() instanceof EntityVoidMonster) ProgressStage.VOIDMONSTERDIE.stepPlayerTo(player); }
    private static void pylonBonus(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof EntityVoidMonster monster)
                || !(monster.level() instanceof ServerLevel server)
                || !reika.chromaticraft.auxiliary.PylonDamage.isPylonDamage(event.getSource())) return;
        for (int i = 0, count = 4 + monster.getRandom().nextInt(4); i < count; i++)
            monster.spawnAtLocation(server, new net.minecraft.world.item.ItemStack(ChromaItems.INFO_FRAGMENT.get()));
        monster.dropAdditionalLoot(server, event.getSource(), 2);
        reika.chromaticraft.registry.ChromaSounds.POWERDOWN.playSound(monster, 2, 1);
        reika.chromaticraft.registry.ChromaSounds.POWERDOWN.playSound(monster, 2, .5F);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                monster.getX(), monster.getY(), monster.getZ(), 1, 0, 0, 0, 0);
    }
    private static void eatLight(VoidMonsterEatLightEvent event) {
        if (event.block==Blocks.REDSTONE_TORCH) {
            BlockPos pos=new BlockPos(event.xCoord,event.yCoord,event.zCoord);
            var supporting=event.world.getBlockState(pos.below()).getBlock();
            if (supporting instanceof reika.chromaticraft.block.BlockCrystallineStone || supporting instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield) event.setCanceled(true);
        }
    }
    private static void explosion(ExplosionEvent.Start event) {
        if (!event.getLevel().isClientSide() && event.getExplosion().getDirectSourceEntity() instanceof net.minecraft.world.entity.item.PrimedTnt tnt && TileEntityVoidMonsterTrap.handleTNTTrigger(event.getLevel(),tnt)) event.setCanceled(true);
    }
    private static void tick(LevelTickEvent.Post event) { if (event.getLevel() instanceof ServerLevel server) VoidMonsterDestructionRitual.tickScheduled(server); }
    private static void unload(LevelEvent.Unload event) { if (event.getLevel() instanceof net.minecraft.world.level.Level level) { TileEntityVoidMonsterTrap.unload(level); VoidMonsterDestructionRitual.unload(level); } }
}
