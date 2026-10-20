package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;

/** Modern submit port of V33a RenderLocusPoint's three-layer Dimension Core glow knot. */
public final class RenderDimensionCore implements
		BlockEntityRenderer<TileEntityDimensionCore, RenderDimensionCore.State> {

	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/effect/aurapoint2-grid.png");

	public RenderDimensionCore(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

    @Override
    public void extractRenderState(TileEntityDimensionCore core,State state,float partialTick,Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(core,state,partialTick,cameraPosition,breaking);
        state.color=core.getRenderColor();state.frame=(int)(System.currentTimeMillis()/250%80);
        state.ritual=reika.chromaticraft.client.render.MonumentRitualEffects.isRunning();
        state.inWorld=core.hasLevel();
        state.roll=sourceRoll(core.getBlockPos().getX()+.5-cameraPosition.x,core.getBlockPos().getZ()+.5-cameraPosition.z);
        if(state.inWorld)reika.chromaticraft.client.render.LocusPointScreenEffects.addDimensionCore(core);
    }
    public static float sourceRoll(double dx,double dz) {return (float)(180+Math.toDegrees(Math.atan2(dx,dz)));}
    @Override
    public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        submitCore(pose,collector,state.color,state.frame,state.inWorld,state.ritual,camera.yRot,camera.xRot,state.roll);
    }
    /** Full inherited source transform; detached callers can normalize its offset in their item adapter. */
    public static void submitCore(PoseStack pose,SubmitNodeCollector collector,int color,int frame,boolean world,
            boolean ritual,float yaw,float pitch,float roll) {
        pose.pushPose();pose.translate(.5,.5,.5);
        if(world) {pose.rotate(com.mojang.math.Axis.YP.rotationDegrees(-yaw));pose.rotate(com.mojang.math.Axis.XP.rotationDegrees(pitch));}
        else {pose.translate(.55,.5,.5);pose.rotate(com.mojang.math.Axis.YP.rotationDegrees(45));pose.rotate(com.mojang.math.Axis.XP.rotationDegrees(-30));}
        reika.chromaticraft.render.LocusRendering.coreLayers(pose,collector,color,frame,world?roll:0,world,!ritual,
                reika.chromaticraft.render.LocusRendering.CORE_LIGHT);
        pose.popPose();
    }
    /** Layers for a caller that has already established its item billboard pose. */
    public static void submitLayers(PoseStack pose,SubmitNodeCollector collector,int color,int frame,boolean world) {
        reika.chromaticraft.render.LocusRendering.coreLayers(pose,collector,color,frame,0,world,true,
                reika.chromaticraft.render.LocusRendering.CORE_LIGHT);
    }

	@Override
	public AABB getRenderBoundingBox(TileEntityDimensionCore core) {
		return new AABB(core.getBlockPos()).inflate(1.5);
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
		private int color;
		private int frame;
		private boolean ritual;
        private boolean inWorld;
        private float roll;
	}
}
