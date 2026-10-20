package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.GlowKnot;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.Map;
import java.util.WeakHashMap;

/** 26.3 submit port of V33a's Aura Locus renderer. */
@net.neoforged.fml.common.EventBusSubscriber(modid="chromaticraft",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class RenderAuraPoint implements
		BlockEntityRenderer<TileEntityAuraPoint, RenderAuraPoint.State> {

	private static final Identifier STAR_FLARE = sprite("starflare");
		private static final Map<TileEntityAuraPoint, KnotState> knots = new WeakHashMap<>();

    /** Keep already-created client knots moving while frustum-culled, at the original six steps/tick. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void tickKnots(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var minecraft=Minecraft.getInstance();
        if(minecraft.isPaused())return;
        var iterator=knots.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var tile=entry.getKey();
            if(tile.isRemoved() || tile.getLevel()!=minecraft.level) {iterator.remove();continue;}
            if(tile.hasLevel())entry.getValue().advance(tile.getTicksExisted());
        }
    }

	public RenderAuraPoint(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

    @Override
    public void extractRenderState(TileEntityAuraPoint point,State state,float partialTick,Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(point,state,partialTick,cameraPosition,breaking);
        state.coreColor=point.getRenderColor();
        state.color=0xff000000|ReikaColorAPI.getModifiedSat(state.coreColor,.875F);
        state.pvp=point.doPvP();state.age=point.getTicksExisted()+partialTick;
        state.frame=(int)(System.currentTimeMillis()/250%80);state.inWorld=point.hasLevel();
        state.ritual=reika.chromaticraft.client.render.MonumentRitualEffects.isRunning();
        double dx=point.getBlockPos().getX()+.5-cameraPosition.x,dy=point.getBlockPos().getY()+.5-cameraPosition.y,dz=point.getBlockPos().getZ()+.5-cameraPosition.z;
        state.roll=RenderDimensionCore.sourceRoll(dx,dz);state.fadeYaw=state.roll;
        state.fadePitch=(float)Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        if(state.inWorld)reika.chromaticraft.client.render.LocusPointScreenEffects.addAuraPoint(point);
        KnotState knot=knots.computeIfAbsent(point,ignored->new KnotState());
        if(state.inWorld)knot.advance(point.getTicksExisted());else knot.knot.update();
        state.knot=knot.knot.snapshot();
    }
    @Override
    public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        RenderDimensionCore.submitCore(pose,collector,state.coreColor,state.frame,state.inWorld,state.ritual,camera.yRot,camera.xRot,state.roll);
        if(state.pvp && state.inWorld) {
            float scale=(float)(3.5+Math.sin(state.age/32D));
            var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(STAR_FLARE);
            pose.pushPose();pose.translate(.5,.5,.5);
            pose.rotate(com.mojang.math.Axis.YP.rotationDegrees(-camera.yRot));pose.rotate(com.mojang.math.Axis.XP.rotationDegrees(camera.xRot));
            reika.chromaticraft.render.LocusRendering.quad(pose,collector,reika.chromaticraft.render.LocusRendering.screen(!state.ritual),true,
                    -scale,-scale,scale,scale,0,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),state.color,reika.chromaticraft.render.LocusRendering.CORE_LIGHT);
            pose.popPose();
        }
        if(state.knot!=null)GlowKnot.submit(state.knot,collector,pose,state.color,state.inWorld,state.inWorld && !state.ritual,
                state.inWorld?1:1.5F,state.fadeYaw,state.fadePitch,state.lightCoords);
        // CHROMA-PORT: GuiMachineDescription.runningRender's 2.5px/+0.1875 knot context is supplied
        // through GlowKnot.submit when that excluded legacy guide renderer is ported.
    }

	private static Identifier sprite(String name) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/" + name);
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityAuraPoint point) {
		return new AABB(point.getBlockPos()).inflate(5);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	public static final class State extends BlockEntityRenderState {
		private int color = 0xffffffff;
		private int coreColor;
        private boolean inWorld,ritual;
        private float roll,fadeYaw,fadePitch;
        private int frame;
		private float age;
		private boolean pvp;
		private GlowKnot.@Nullable Frame knot;
	}

	private static final class KnotState {
		private final GlowKnot knot = new GlowKnot(0.875);
		private int lastTick = Integer.MIN_VALUE;

		private void advance(int tick) {
			if (lastTick == Integer.MIN_VALUE) {
				lastTick = tick;
				return;
			}
			int elapsed = Math.clamp(tick - lastTick, 0, 20);
			for (int i = 0; i < elapsed * 6; i++)
				knot.update();
			lastTick = tick;
		}
	}
}
