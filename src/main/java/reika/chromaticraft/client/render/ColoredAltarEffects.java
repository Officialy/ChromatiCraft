package reika.chromaticraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import reika.chromaticraft.block.decoration.BlockColoredAltar.TileEntityColoredAltar;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle.FadeGlow;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

public final class ColoredAltarEffects {
    private ColoredAltarEffects() {}
    /** V33a model fragments sample 1/32 of the authored altar texture. */
    public static boolean fragments(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level world, BlockPos pos,
            net.minecraft.client.particle.ParticleEngine engine, boolean destroyed) {
        if (!(world instanceof ClientLevel level)) return false;
        var random = level.getRandom();
        int count = destroyed ? 48 : 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            engine.add(new AltarFragment(level, state, pos,
                    pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), random.nextDouble() * .2 - .1,
                    .2, random.nextDouble() * .2 - .1));
        }
        return true;
    }
    private static final class AltarFragment extends net.minecraft.client.particle.TerrainParticle {
        private final float textureU, textureV;
        private AltarFragment(ClientLevel level, net.minecraft.world.level.block.state.BlockState state,
                BlockPos pos, double x, double y, double z, double dx, double dy, double dz) {
            super(level, x, y, z, dx, dy, dz, state, pos);
            setParticleSpeed(dx, dy, dz);
            textureU = Math.min(random.nextFloat(), 1 - 1 / 32F);
            textureV = Math.min(random.nextFloat(), 1 - 1 / 32F);
        }
        @Override protected float getU0() { return sprite.getU(textureU); }
        @Override protected float getU1() { return sprite.getU(textureU + 1 / 32F); }
        @Override protected float getV0() { return sprite.getV(textureV); }
        @Override protected float getV1() { return sprite.getV(textureV + 1 / 32F); }
    }
    public record Visual(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Visual> TYPE=new Type<>(Identifier.fromNamespaceAndPath("chromaticraft","altar_visual"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,Visual::data,Visual::new);
        public Type<Visual> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) { event.registrar("1").playToClient(Visual.TYPE,Visual.CODEC,(packet,context)->handle(packet.data)); }
    public static void send(ServerLevel level,BlockPos pos,Vec3 target,CrystalElement element,String action) {
        CompoundTag data=new CompoundTag(); data.putString("dimension",level.dimension().identifier().toString()); data.putLong("pos",pos.asLong());
        data.putDouble("x",target.x); data.putDouble("y",target.y); data.putDouble("z",target.z); data.putInt("color",element.ordinal()); data.putString("action",action);
        for (var player:level.players()) if (player.connection!=null && player.connection.hasChannel(Visual.TYPE)
                && player.position().distanceToSqr(Vec3.atLowerCornerOf(pos))<64*64) PacketDistributor.sendToPlayer(player,new Visual(data));
    }
    private static FadeGlow glow(ClientLevel world,Vec3 pos,Vec3 velocity,int color,int life,float scale,boolean rapid,String icon) {
        var fx=new FadeGlow(world,pos.x,pos.y,pos.z,velocity.x,velocity.y,velocity.z,color,life,scale,rapid,null,0,icon,true);
        Minecraft.getInstance().particleEngine.add(fx); return fx;
    }
    public static void particles(TileEntityColoredAltar altar) {
        if (!(altar.getLevel() instanceof ClientLevel level)) return;
        var mc=Minecraft.getInstance(); if (mc.player==null) return; var random=level.getRandom(); var element=altar.getColor();
        Vec3 center=Vec3.atCenterOf(altar.getBlockPos()); int setting=mc.options.particles().get().ordinal();
        double n=(setting==0?2:setting==1?1:.5)/Math.max(1,mc.player.distanceToSqr(center.add(0,-.25,0))/256);
        int count=n<=1?(random.nextDouble()<n?1:0):(int)n;
        for (int i=0;i<count;i++) {
            double radius=.55+(random.nextDouble()*2-1)*.05,angle=random.nextDouble()*Math.PI*2;
            var fx=glow(level,center.add(radius*Math.sin(angle),0,radius*Math.cos(angle)),Vec3.ZERO,element.color(),20+random.nextInt(20),1+random.nextFloat()*2,false,"fade");
            double slide=angle+(random.nextDouble()*2-1)*Math.toRadians(10);
            fx.setGravity((float)(.0625+(random.nextDouble()*2-1)*.03125)).setLegacyMotion(false).setColliding(Math.toDegrees(slide));
        }
        if (random.nextInt(2)==0) {
            int red=(int)(element.getRed()*(.25+random.nextDouble()*.5)),green=(int)(element.getGreen()*(.25+random.nextDouble()*.5)),blue=(int)(element.getBlue()*(.25+random.nextDouble()*.5));
            if (element==CrystalElement.WHITE || element==CrystalElement.BLACK || element==CrystalElement.GRAY || element==CrystalElement.LIGHTGRAY) red=green=blue=(red+green+blue)/3;
            glow(level,center.add((random.nextDouble()*2-1)*.25,-.125,(random.nextDouble()*2-1)*.25),Vec3.ZERO,(red<<16)|(green<<8)|blue,10+random.nextInt(60),.5F+random.nextFloat()*2,false,"fade");
        }
        if (random.nextInt(8)==0) reika.chromaticraft.render.particle.ChromaParticle.spawnAltarLaser(level,
                center.add((random.nextDouble()*2-1)*.375,.05,(random.nextDouble()*2-1)*.375),element,
                .5F+random.nextFloat()*2,-(float)(.0625+(random.nextDouble()*2-1)*.03125));
    }
    private static void handle(CompoundTag data) {
        var level=Minecraft.getInstance().level; if (level==null || !data.getStringOr("dimension","").equals(level.dimension().identifier().toString())) return;
        int ordinal=data.getIntOr("color",-1); if (ordinal<0 || ordinal>=16) return;
        var element=CrystalElement.elements[ordinal]; BlockPos pos=BlockPos.of(data.getLongOr("pos",0)); Vec3 center=Vec3.atCenterOf(pos);
        var random=level.getRandom(); String action=data.getStringOr("action","");
        if (action.equals("break")) {
            var state = ChromaBlocks.COLORED_ALTARS.get(element).get().defaultBlockState();
            for (int i = 0, count = 12 + random.nextInt(12); i < count; i++)
                Minecraft.getInstance().particleEngine.add(new AltarFragment(level, state, pos,
                        pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), (random.nextDouble() * 2 - 1) * .25,
                        .125 + random.nextDouble() * .875, (random.nextDouble() * 2 - 1) * .25));
            for (double a=0;a<360;a+=20) for (double b=-70;b<70;b+=20) {
                double[] v=reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper.polarToCartesian(.125+(random.nextDouble()*2-1)*.0625,b,a);
                int life=30+random.nextInt(20);
                var fx=glow(level,center,new Vec3(v[0],v[1],v[2]),0xffffff,life,2+random.nextFloat(),true,"fade_ray").setLegacyMotion(false).setColliding();
                fx.setHeightColor(y->ReikaColorAPI.mixColors(0xffffff,element.color(),1-fx.getAge()/(float)life));
            }
        } else if (action.equals("bolt") || action.equals("shock")) {
            Vec3 end=new Vec3(data.getDoubleOr("x",0),data.getDoubleOr("y",0),data.getDoubleOr("z",0));
            var bolt=new reika.dragonapi.instantiable.effects.LightningBolt(new reika.dragonapi.instantiable.data.immutable.DecimalPosition(center),new reika.dragonapi.instantiable.data.immutable.DecimalPosition(end),4+random.nextInt(4));
            bolt.scaleVariance(.5); bolt.maximize(); int life=20;
            for (int i=0;i<bolt.nsteps;i++) for (double fraction=0;fraction<=1;fraction+=.03125) {
                var a=bolt.getPosition(i); var b=bolt.getPosition(i+1); Vec3 point=new Vec3(a.xCoord() +(b.xCoord() - a.xCoord())*fraction, a.yCoord() +(b.yCoord() - a.yCoord())*fraction, a.zCoord() +(b.zCoord() - a.zCoord())*fraction);
                glow(level,point,Vec3.ZERO,element.color(),life,1.75F,true,"fade").freezeLife(action.equals("shock")?(int)(2*(i+fraction)):0);
            }
        }
    }
}
