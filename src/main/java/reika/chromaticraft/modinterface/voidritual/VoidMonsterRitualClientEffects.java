package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import reika.chromaticraft.modinterface.voidritual.VoidMonsterDestructionRitual.Effects;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.particle.ChromaParticle.FadeGlow;
import reika.dragonapi.instantiable.particlecontroller.CollectingPositionController;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import reika.voidmonster.entity.EntityVoidMonster;

import java.util.*;

/** V33a's four shader envelopes and every ritual/eating particle effect. */
public final class VoidMonsterRitualClientEffects {
    private static final Set<UUID> FOCI=new HashSet<>();
    private static final EnumMap<Effects,Visual> VISUALS=new EnumMap<>(Effects.class);
    private static String dimension="";
    static { for (var effect:new Effects[]{Effects.COLLAPSING_SPHERE,Effects.WAVE,Effects.CURL,Effects.STRETCH}) VISUALS.put(effect,new Visual(effect)); }
    private VoidMonsterRitualClientEffects() {}
    public static boolean isFocus(Entity entity) { return dimension.equals(entity.level().dimension().identifier().toString()) && FOCI.contains(entity.getUUID()); }
    public static Collection<Visual> visuals() { return Collections.unmodifiableCollection(VISUALS.values()); }
    public static void tick(ClientTickEvent.Post event) { if (!Minecraft.getInstance().isPaused()) VISUALS.values().forEach(Visual::tick); }
    public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) reset(); }
    public static void reset() { FOCI.clear(); dimension=""; VISUALS.values().forEach(Visual::clear); VoidRitualShaders.reset(); }
    public static void handle(CompoundTag data) {
        ClientLevel world=Minecraft.getInstance().level; if (world==null) return;
        String incoming=data.getStringOr("dimension",""); if (!incoming.equals(world.dimension().identifier().toString())) return;
        if (!dimension.equals(incoming)) { reset(); dimension=incoming; }
        switch (data.getStringOr("action","")) {
            case "sync" -> {
                FOCI.clear();
                for (Tag tag:data.getListOrEmpty("entities")) try {
                    FOCI.add(UUID.fromString(tag.asString().orElse("")));
                } catch (IllegalArgumentException ignored) { }
                for (Visual visual:VISUALS.values())
                    if (visual.focus != null && !FOCI.contains(visual.focus)) visual.clear();
            }
            case "effect" -> {
                int ordinal=data.getIntOr("effect",-1); if (ordinal<0 || ordinal>=Effects.values().length) return;
                Entity entity=world.getEntity(data.getIntOr("entity",-1));
                if (!(entity instanceof EntityVoidMonster monster) || !monster.getUUID().toString().equals(data.getStringOr("uuid",""))) return;
                Effects effect=Effects.values()[ordinal]; Visual visual=VISUALS.get(effect); if (visual!=null) visual.activate(monster);
                if (effect==Effects.COLLAPSING_SPHERE) sphere(world,monster);
                if (effect==Effects.RAYS) rays(world,monster);
                ChromaSounds.FLAREATTACK.playSound(monster,1,effect==Effects.RAYS?.5F:1);
            }
            case "eat" -> eat(world,new Vec3(data.getIntOr("x",0)+.5,data.getIntOr("y",0)+.5,data.getIntOr("z",0)+.5));
        }
    }
    private static void sphere(ClientLevel world,Entity monster) {
        var random=world.getRandom(); Vec3 center=monster.position().add(0,1,0);
        for (int i=0;i<128;i++) {
            double[] xyz=ReikaPhysicsHelper.polarToCartesian(4+random.nextDouble()*.25,random.nextDouble()*360,random.nextDouble()*360);
            int duration=5+random.nextInt(4); Vec3 start=center.add(xyz[0],xyz[1],xyz[2]);
            var controller=new CollectingPositionController(start.x,start.y,start.z,center.x,center.y,center.z,duration);
            var fx=new FadeGlow(world,start.x,start.y,start.z,0,0,0,0xffffff,duration+1,1+random.nextFloat()*.5F,false,controller,0).setAlphaFading();
            Minecraft.getInstance().particleEngine.add(fx);
        }
    }
    private static void rays(ClientLevel world,Entity monster) {
        var random=world.getRandom(); Vec3 center=monster.position().add(0,1,0);
        for (int ray=0;ray<9;ray++) {
            double[] direction=ReikaPhysicsHelper.polarToCartesian(1,random.nextDouble()*360,random.nextDouble()*360);
            for (int i=0;i<128;i++) {
                double radius=random.nextDouble()*96; Vec3 point=center.add(direction[0]*radius+(random.nextDouble()*2-1)*.125,direction[1]*radius+(random.nextDouble()*2-1)*.125,direction[2]*radius+(random.nextDouble()*2-1)*.125);
                var fx=new FadeGlow(world,point.x,point.y,point.z,0,0,0,0xffffff,40,1.5F+random.nextFloat()*1.5F,false);
                double phase=random.nextDouble()*360;
                var expression=new reika.dragonapi.instantiable.formula.PeriodicExpression()
                        .addWave(1,1,phase).addWave(.5,2,phase+90).addWave(.125,4,phase).normalize();
                fx.setAlphaFading().setHeightColor(y -> ReikaColorAPI.mixColors(0xffffff,0,
                        Math.abs((float)expression.evaluate(System.currentTimeMillis()/200D))));
                Minecraft.getInstance().particleEngine.add(fx);
            }
        }
    }
    private static void eat(ClientLevel world,Vec3 center) {
        var random=world.getRandom();
        for (int i=0;i<20;i++) {
            Vec3 point=center.add((random.nextDouble()*2-1)*.25,random.nextDouble()-.5,(random.nextDouble()*2-1)*.25);
            double[] v=ReikaPhysicsHelper.polarToCartesian(.0625+random.nextDouble()*.1875,random.nextDouble()*360,random.nextDouble()*360);
            int life=20+random.nextInt(60); float scale=1.5F+random.nextFloat()*2.5F;
            var first=new FadeGlow(world,point.x,point.y,point.z,v[0],v[1],v[2],0x8f00ff,life,scale,false,null,0,"transfade",false).setAlphaFading();
            var second=new FadeGlow(world,point.x,point.y,point.z,v[0],v[1],v[2],0xa0a0a0,life,scale*(.4F+random.nextFloat()*.4F),false,null,0,"transfade",false).setAlphaFading().lockTo(first);
            Minecraft.getInstance().particleEngine.add(first); Minecraft.getInstance().particleEngine.add(second);
        }
    }
    public static final class Visual {
        public final Effects cause;
        public float intensity,size,thickness,waveRadius,waveThickness,waveAmplitude,waveSpeed,curlXZ,curlY,stretchFactor,stretchRadius,stretchApplication;
        public UUID focus; private boolean released; private float amplitude; private int time;
        private Visual(Effects cause) { this.cause=cause; clear(); }
        private void activate(Entity entity) {
            clear(); focus=entity.getUUID(); intensity=1;
            switch (cause) {
                case COLLAPSING_SPHERE -> thickness=.0125F;
                case WAVE -> { waveThickness=4; waveAmplitude=waveSpeed=1; }
                case CURL -> curlXZ=.0001F;
                case STRETCH -> { stretchFactor=1; stretchRadius=1000; amplitude=4; }
                default -> throw new IllegalStateException("Effect has no shader: "+cause);
            }
        }
        private void clear() { intensity=size=thickness=waveRadius=waveThickness=waveAmplitude=waveSpeed=curlXZ=curlY=stretchRadius=stretchApplication=0; stretchFactor=1; focus=null; released=false; amplitude=0; time=0; }
        private void tick() {
            if (intensity<=0) return;
            switch (cause) {
                case COLLAPSING_SPHERE -> { size=size*1.005F+.005F; thickness*=1.05F; if (size>=.5) intensity=0; else if (size>=.35) intensity*=.75F; }
                case WAVE -> { waveRadius=(waveRadius+1.25F*waveSpeed)*(1+.01F*waveSpeed); waveThickness=Math.clamp(waveRadius/3,4,35); waveAmplitude=Math.clamp(waveRadius/24,1,4); waveSpeed=Math.max(.25F,waveSpeed*.99F); if (waveRadius>200) intensity*=.95F; if (intensity<.05) intensity=0; }
                case CURL -> { curlXZ=curlXZ*3/4+(float)Math.sqrt(curlXZ)/4; if (curlXZ<.75) curlY+=.02F; else curlY*=.95F; curlXZ=Math.min(curlXZ,.95F); if (curlXZ>=.9) intensity*=.9F; if (intensity<.01) intensity=0; }
                case STRETCH -> {
                    if (stretchFactor>=amplitude+1) released=true;
                    if (released) { float value=(float)(amplitude*Math.cos(time*.125)); stretchFactor=value<0?1/(1+Math.abs(value)):1+value; amplitude*=.965F; time++; }
                    else stretchFactor*=1.0625F;
                    stretchApplication=1; if (amplitude<1) intensity*=.9975F; if (intensity<=.01) intensity=0;
                }
                default -> throw new IllegalStateException("Effect has no shader: "+cause);
            }
            if (intensity<=0) clear();
        }
    }
}
