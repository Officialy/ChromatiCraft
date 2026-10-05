package reika.chromaticraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.UUID;

/** V33a inert bait. A Mob on 26.3 so hostile mobs can retain it as their real LivingEntity target. */
public class EntityMonsterBait extends Mob {
    private static final int MIN_LIFE=100, MAX_LIFE=600;
    private static final EntityDataAccessor<Integer> LIFE=SynchedEntityData.defineId(EntityMonsterBait.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> PLACER=SynchedEntityData.defineId(EntityMonsterBait.class, EntityDataSerializers.STRING);
    private UUID placer;
    private int lifespan;
    private Monster currentEntity;
    public EntityMonsterBait(EntityType<? extends EntityMonsterBait> type, Level level) { super(type, level); setNoAi(true); }
    public EntityMonsterBait(Level level, Player player) {
        this(ChromaEntityTypes.MONSTER_BAIT.get(), level);
        initialize(player);
    }
    public void initialize(Player player) {
        setLifeSpan(MIN_LIFE+random.nextInt(1+MAX_LIFE-MIN_LIFE));
        snapTo(player.getX(), player.getY()+player.getEyeHeight()/4, player.getZ(), 0, 0);
        placer=player.getUUID(); entityData.set(PLACER,placer.toString());
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(LIFE,0); builder.define(PLACER,""); }
    @Override public void tick() {
        xOld=getX(); yOld=getY(); zOld=getZ(); tickCount++;
        currentEntity=findClosestValidMob();
        if (level().isClientSide()) {
            if (getLife()>5) reika.chromaticraft.modinterface.voidritual.VoidBaitParticles.spawn(this,currentEntity);
        } else {
            if (isActive() && currentEntity!=null) currentEntity.setTarget(this);
            entityData.set(LIFE,getLife()-1);
            if (getLife()<=0 || getY()<level().getMinY()-40) discard();
        }
    }
    private Monster findClosestValidMob() {
        Monster best=null; double distance=Double.POSITIVE_INFINITY;
        for (Monster mob: level().getEntitiesOfClass(Monster.class,getBoundingBox().inflate(16))) {
            if (!mob.isAlive() || mob.getTarget()!=this && mob.getTarget() instanceof EntityMonsterBait) continue;
            double d=distanceToSqr(mob);
            if (d<distance) { distance=d; best=mob; }
        }
        return best;
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input); setLifeSpan(input.getIntOr("life",0));
        placer=input.read("placer",net.minecraft.core.UUIDUtil.STRING_CODEC).orElse(null); entityData.set(PLACER,placer==null?"":placer.toString());
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output); output.putInt("life",lifespan);
        if (placer!=null) output.store("placer",net.minecraft.core.UUIDUtil.STRING_CODEC,placer);
    }
    private void setLifeSpan(int value) { lifespan=value; entityData.set(LIFE,value); }
    public boolean isActive() { return !entityData.get(PLACER).isEmpty() && getLife()>0; }
    public double maxRangeSquared() { return 100; }
    public void attack(double damage) { entityData.set(LIFE,(int)(getLife()-damage/4)); if (getLife()<=0) discard(); }
    public int getLife() { return entityData.get(LIFE); }
    public float getBrightness() { return getLife()<=0?0:getLife()>=MIN_LIFE?1:(float)Math.sqrt(getLife()/(float)MIN_LIFE); }
    public int getRenderColor(double layer) {
        if (currentEntity!=null) return ReikaColorAPI.mixColorBiDirectional(ReikaEntityHelper.mobToColor(currentEntity),0,0xffffff,(float)(.5+.25*Math.sin(tickCount/12D+layer*60)));
        int color=ReikaColorAPI.getColorWithBrightnessMultiplier(0xffffff,getBrightness());
        return ReikaColorAPI.getColorWithBrightnessMultiplier(color,.65F+(float)(.35*Math.sin(tickCount/10D+layer*40)));
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source,float amount) { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public void checkDespawn() { }
}
