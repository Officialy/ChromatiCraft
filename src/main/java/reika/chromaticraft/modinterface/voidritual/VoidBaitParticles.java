package reika.chromaticraft.modinterface.voidritual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.entity.EntityMonsterBait;
import reika.chromaticraft.render.particle.ChromaParticle.FadeGlow;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
/** Native particle equivalent of V33a bait blurs and EntityLockMotionController. */
public final class VoidBaitParticles {
    private VoidBaitParticles() {}
    public static void spawn(EntityMonsterBait bait,Monster target) {
        if (!(bait.level() instanceof ClientLevel world)) return;
        var random=bait.getRandom();
        for (int i=0,n=1+random.nextInt(4);i<n;i++) {
            double radius=.5+(random.nextDouble()*2-1)*.0625,angle=random.nextDouble()*Math.PI*2;
            int color=ReikaColorAPI.mixColors(0xa0a0a0,0xffffff,Math.min(random.nextFloat(),bait.getBrightness()));
            if (target!=null) color=ReikaColorAPI.mixColorBiDirectional(ReikaEntityHelper.mobToColor(target),0,0xffffff,.25F+random.nextFloat()*.5F);
            int life=Math.min(bait.getLife(),10+random.nextInt(71)); float size=.5F+random.nextFloat()*1.5F;
            var particle=new FadeGlow(world,bait.getX()+radius*Math.cos(angle),bait.getY(),bait.getZ()+radius*Math.sin(angle),0,0,0,color,life,size,true,null,0,"fade_gentle",true);
            particle.setGravity((float)((random.nextDouble()*2-1)*.035)).setAlphaFading();
            if (target!=null) {
                particle.setTickMotion(fx -> {
                    Vec3 delta=target.position().add(0,target.getBbHeight()/2,0).subtract(fx.position());
                    double distance=fx.position().distanceTo(target.position());
                    fx.setMotion(distance>1.0e-7?delta.scale((fx.getAge()*.03125/8)/distance):Vec3.ZERO);
                });
            }
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }
}
