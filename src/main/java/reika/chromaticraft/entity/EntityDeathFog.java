/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.dragonapi.base.InertEntity;
import reika.dragonapi.interfaces.entity.DestroyOnUnload;
import reika.dragonapi.libraries.ReikaEntityHelper;

/** Complete V33a drifting death fog, including its two hostile-only damage modes. */
public class EntityDeathFog extends InertEntity implements DestroyOnUnload {
    public static final int MIN_LIFE=40,MAX_LIFE=300;
    private static final EntityDataAccessor<Integer> LIFE=SynchedEntityData.defineId(EntityDeathFog.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENHANCED=SynchedEntityData.defineId(EntityDeathFog.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFESPAN=SynchedEntityData.defineId(EntityDeathFog.class,EntityDataSerializers.INT);
    private int lifespan;
    public EntityDeathFog(EntityType<? extends EntityDeathFog> type,Level level) { super(type,level); }
    public EntityDeathFog(Level level,double x,double y,double z,double vx,double vy,double vz,boolean enhanced) {
        this(ChromaEntityTypes.DEATH_FOG.get(),level); snapTo(x,y,z,0,0); setDeltaMovement(vx,vy,vz);
        lifespan=MIN_LIFE+random.nextInt(1+MAX_LIFE-MIN_LIFE); entityData.set(LIFE,lifespan); entityData.set(LIFESPAN,lifespan); entityData.set(ENHANCED,enhanced);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(LIFE,0); builder.define(LIFESPAN,0); builder.define(ENHANCED,false); }
    @Override public void tick() {
        xOld=getX(); yOld=getY(); zOld=getZ(); tickCount++;
        var velocity=getDeltaMovement(); move(MoverType.SELF,velocity); setDeltaMovement(velocity);
        if (getY()<level().getMinY()-40) { destroy(); return; }
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            entityData.set(LIFE,lifespan-tickCount);
            while (getY()<level().getMaxY()+1 && level().getBlockCollisions(this,getBoundingBox().inflate(.3)).iterator().hasNext()) {
                setPos(getX(),getY()+.1,getZ()); setDeltaMovement(getDeltaMovement().x,Math.max(getDeltaMovement().y,0),getDeltaMovement().z); syncVelocity=true;
            }
            for (LivingEntity living:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(isEnhanced()?4:2.5), e->e.isAlive()&&ReikaEntityHelper.isHostile(e))) {
                living.hurtServer(server,damageSources().magic(),isEnhanced()?.5F:1);
                if (isEnhanced()) living.damageCooldownTime=11;
            }
            if (getRemainingLife()<=0) destroy();
        }
    }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public void destroy() { discard(); }
    public boolean isEnhanced() { return entityData.get(ENHANCED); }
    public float getLifeRatio() { int total=entityData.get(LIFESPAN); return total==0?0:getRemainingLife()/(float)total; }
    public int getRemainingLife() { return entityData.get(LIFE); }
    @Override protected void readAdditionalSaveData(ValueInput input) { lifespan=input.getIntOr("life",0); int remaining=input.getIntOr("time",0); tickCount=lifespan-remaining; entityData.set(LIFESPAN,lifespan); entityData.set(LIFE,remaining); entityData.set(ENHANCED,input.getBooleanOr("enhance",false)); }
    @Override protected void addAdditionalSaveData(ValueOutput output) { output.putInt("life",lifespan); output.putInt("time",getRemainingLife()); output.putBoolean("enhance",isEnhanced()); }
}
