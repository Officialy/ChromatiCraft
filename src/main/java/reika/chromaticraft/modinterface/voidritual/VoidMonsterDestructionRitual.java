/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import reika.dragonapi.instantiable.Interpolation;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.world.MonsterGenerator;

import java.util.*;

/** Complete V33a health-curve ritual; identities are scoped to the world and entity UUID. */
public final class VoidMonsterDestructionRitual {
    public static final ResourceKey<DamageType> DAMAGE=VoidRitualDamageTypes.KEY;
    private static final Set<VoidMonsterDestructionRitual> ACTIVE=new HashSet<>();
    private static final List<DelayedDamage> DELAYED=new ArrayList<>();
    private final WorldLocation center;
    private final Player startingPlayer;
    private final UUID monsterUUID;
    private final EntityVoidMonster monsterReference;
    private final ServerLevel world;
    private final Interpolation healthCurve=new Interpolation(false);
    private int tick,effectCooldown;
    private long lastWorldTick=-1;
    public VoidMonsterDestructionRitual(TileEntityVoidMonsterTrap trap,EntityVoidMonster monster) {
        center=new WorldLocation(trap); startingPlayer=trap.getPlacer(); monsterUUID=monster.getUUID(); monsterReference=monster; world=(ServerLevel)monster.level();
        int duration=1200+world.getRandom().nextInt(2401); healthCurve.addPoint(0,1);
        for (double d=.1;d<1;d+=.1) healthCurve.addPoint((d+world.getRandom().nextDouble()*.09-.045)*duration,1-d+world.getRandom().nextDouble()*.4-.2);
        healthCurve.addPoint(duration,0); ACTIVE.add(this); sync(world);
    }
    public EntityVoidMonster getEntity() { Entity entity=world.getEntity(monsterUUID); return entity instanceof EntityVoidMonster monster?monster:null; }
    public boolean tick() {
        if (world.getGameTime()<=lastWorldTick) return false;
        lastWorldTick=world.getGameTime(); tick++; if (effectCooldown>0) effectCooldown--;
        EntityVoidMonster monster=getEntity(); if (monster==null) { onEnd(); return true; }
        monster.moveTowards(center.pos.getX()+.5,center.pos.getY()+.5,center.pos.getZ()+.5,2);
        if (effectCooldown==0) for (Effects effect:Effects.values()) if (world.getRandom().nextInt(effect.chance)==0) {
            effect.doEffectServer(this,monster); effectCooldown=effect.cooldown; break;
        }
        return monster.getHealth()<=0;
    }
    public void onPrematureTermination() {
        onEnd(); EntityVoidMonster monster=getEntity();
        if (monster!=null && monster.isAlive()) { monster.heal(6000); monster.setTarget(startingPlayer); monster.increaseDifficulty(2); }
    }
    public void onCompletion() {
        MonsterGenerator.instance.addCooldown(monsterReference,1200*(20+world.getRandom().nextInt(26)));
        onEnd();
    }
    private void onEnd() { ACTIVE.remove(this); sync(world); }
    public float getCurrentTargetHealthFraction() { return (float)healthCurve.getValue(tick); }
    public static boolean isFocusOfActiveRitual(Entity entity) {
        if (entity.level().isClientSide()) return VoidMonsterRitualClientEffects.isFocus(entity);
        return ACTIVE.stream().anyMatch(r->r.world==entity.level() && r.monsterUUID.equals(entity.getUUID()));
    }
    public static Set<UUID> activeEntities(Level world) {
        Set<UUID> result=new HashSet<>(); ACTIVE.stream().filter(r->r.world==world).forEach(r->result.add(r.monsterUUID)); return result;
    }
    public static void sync(ServerLevel world) { VoidRitualNetwork.sync(world,activeEntities(world)); }
    public static void tickScheduled(ServerLevel world) {
        Iterator<DelayedDamage> iterator=DELAYED.iterator();
        while (iterator.hasNext()) { DelayedDamage damage=iterator.next(); if (damage.monster.level()==world && world.getGameTime()>=damage.when) { iterator.remove(); if (!damage.monster.isRemoved()) runAttack(damage.monster,damage.source,damage.amount); } }
    }
    public static void unload(Level world) { ACTIVE.removeIf(r->r.world==world); DELAYED.removeIf(d->d.monster.level()==world); }
    public enum Effects {
        COLLAPSING_SPHERE(70,20,0,40),RAYS(40,40,0,20),EXPLOSION(200,0,0,20),WAVE(400,20,0,120),CURL(400,40,30,100),STRETCH(400,30,20,160);
        public final int chance,damage,delay,cooldown;
        Effects(int chance,int damage,int delay,int cooldown) { this.chance=chance; this.damage=damage; this.delay=delay; this.cooldown=cooldown; }
        public void doEffectServer(VoidMonsterDestructionRitual ritual,EntityVoidMonster monster) {
            if (monster.getHealth()/monster.getMaxHealth()>=ritual.getCurrentTargetHealthFraction()) {
                DamageSource source=new VoidMonsterRitualDamage(ritual.world,ritual.startingPlayer);
                if (delay>0) DELAYED.add(new DelayedDamage(monster,source,damage,ritual.world.getGameTime()+delay)); else runAttack(monster,source,damage);
            }
            if (this==EXPLOSION) ritual.world.explode(monster,monster.getX(),monster.getY(),monster.getZ(),9,true,Level.ExplosionInteraction.NONE);
            VoidRitualNetwork.effect(monster,this);
        }
    }
    private static void runAttack(EntityVoidMonster monster,DamageSource source,int amount) {
        if (!monster.isAlive()) return;
        monster.setHealth(amount>=monster.getHealth()?.1F:monster.getHealth()-amount+1);
        monster.hurtServer((ServerLevel)monster.level(),source,1);
    }
    private record DelayedDamage(EntityVoidMonster monster,DamageSource source,int amount,long when) {}
    public static final class VoidMonsterRitualDamage extends DamageSource {
        public VoidMonsterRitualDamage(ServerLevel level,Player player) { super(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE),player); }
    }
    public static void bootstrap(net.minecraft.data.worldgen.BootstrapContext<DamageType> context) { VoidRitualDamageTypes.bootstrap(context); }
}
