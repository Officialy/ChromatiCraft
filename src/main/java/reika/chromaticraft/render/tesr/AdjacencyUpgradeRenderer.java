/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code AdjacencyUpgradeRenderer}: three four-pixel line rings (vertical, side-on vertical, horizontal) turning at
 * a speed that grows with tier, each pulsing between the colour's two render colours on a cycle that shortens with
 * tier, around a camera-facing glow. A redstone-stopped upgrade turns six times slower in grey. The item draws the glow
 * as a fixed cross beneath half-again larger rings.
 */
public final class AdjacencyUpgradeRenderer implements BlockEntityRenderer<TileEntityAdjacencyUpgrade, AdjacencyUpgradeRenderer.State> {

	private static final float LINE_WIDTH = 4F;

	public AdjacencyUpgradeRenderer(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityAdjacencyUpgrade te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.upgrade = AdjacencyUpgrades.upgrades[te.getColor().ordinal()];
		state.tier = te.getTier();
		state.disabled = te.getLevel() != null && !te.canRun(te.getLevel(), te.getBlockPos());
		state.seed = te.getBlockPos().getX()+te.getBlockPos().getY()*4+te.getBlockPos().getZ()*2;
        state.inWorld=te.hasLevel();
        state.animation=animationAt(state.upgrade,state.tier,state.disabled,state.seed,System.currentTimeMillis());
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		submitAt(poseStack,collector,state.animation,state.inWorld,camera.yRot,camera.xRot,state.lightCoords);
	}

	/**
	 * The whole V33a render from the block's corner. {@code cameraOrientation} null is V33a's out-of-world (item)
	 * branch.
	 */
	public static void submitUpgrade(PoseStack poseStack, SubmitNodeCollector collector, AdjacencyUpgrades upgrade,
			int tier, boolean disabled, int seed, @Nullable Quaternionfc cameraOrientation) {
        var angles=cameraOrientation==null?new Vector3f():cameraOrientation.getEulerAnglesYXZ(new Vector3f());
        submitAt(poseStack,collector,animationAt(upgrade,tier,disabled,seed,System.currentTimeMillis()),cameraOrientation!=null,
                180-(float)Math.toDegrees(angles.y),-(float)Math.toDegrees(angles.x),LightCoordsUtil.FULL_BRIGHT);
    }

    /** Source item helper with native caller light, captured before queuing any geometry. */
    public static void submitItemUpgrade(PoseStack pose,SubmitNodeCollector collector,AdjacencyUpgrades upgrade,int tier,int light) {
        submitAt(pose,collector,animationAt(upgrade,tier,false,0,System.currentTimeMillis()),false,0,0,light);
    }

    /** Render an immutable source animation snapshot; previews can provide their own captured clock/camera. */
    public static void submitAt(PoseStack poseStack,SubmitNodeCollector collector,Animation animation,
            boolean inWorld,float yaw,float pitch,int light) {
        poseStack.pushPose();poseStack.scale(1,-1,-1);
        if(!inWorld) {
            poseStack.pushPose();poseStack.translate(.5,-.4,-.5);
            submitInnerItem(poseStack,collector,light);
            poseStack.rotate(Axis.YP.rotationDegrees(90));submitInnerItem(poseStack,collector,light);
            poseStack.popPose();
        }
        submitMiddle(poseStack,collector,animation,inWorld,light);
        if(inWorld)submitInnerWorld(poseStack,collector,yaw,pitch,light);
        poseStack.popPose();
    }

    public record Animation(double rx,double ry,double rz,int color1,int color2,int color3) {}
    public static Animation animationAt(AdjacencyUpgrades upgrade,int tier,boolean dis,int seed,long millis) {
		double time = millis+seed*48D;
		if (dis) {
			time = time/6D;
		}
		double rsc = 0.2+tier/4D;
		double rx = (time*rsc/4)%360;
		double ry = (time*rsc/3)%360;
		double rz = (time*rsc/5)%360;

		double d = 7200-tier*975;
		double d2 = d/2;
		double d3 = d/3;

		double t1 = time%d;
		if (dis) {
			t1 = t1/12;
		}

		double b1 = t1 >= d2 ? (d-t1)/d2 : t1/d2;
		double t2 = (time+d3)%d;
		double b2 = t2 >= d2 ? (d-t2)/d2 : t2/d2;
		double t3 = (time+2*d3)%d;
		double b3 = t3 >= d2 ? (d-t3)/d2 : t3/d2;

		int ca = upgrade.color1;
		int cb = upgrade.color2;

		int c1 = ReikaColorAPI.mixColors(ca, cb, (float)b1);
		int c2 = ReikaColorAPI.mixColors(ca, cb, (float)b2);
		int c3 = ReikaColorAPI.mixColors(ca, cb, (float)b3);

		if (dis) {
			c1 = ReikaColorAPI.mixColors(0x000000, 0xffffff, (float)b1);
			c2 = ReikaColorAPI.mixColors(0x000000, 0xffffff, (float)b2);
			c3 = ReikaColorAPI.mixColors(0x000000, 0xffffff, (float)b3);
		}

		c1 = c1 | 0xff000000;
		c2 = c2 | 0xff000000;
		c3 = c3 | 0xff000000;

        return new Animation(rx,ry,rz,c1,c2,c3);
    }
    private static void submitMiddle(PoseStack poseStack,SubmitNodeCollector collector,Animation animation,boolean inWorld,int light) {
        poseStack.pushPose();poseStack.translate(.5,-.5,-.5);
        if(!inWorld)poseStack.scale(1.5F,1.5F,1.5F);

		poseStack.rotate(Axis.XP.rotationDegrees((float)animation.rx));
		submitRing(poseStack, collector, 0.25, animation.color1, 1, 15, light, inWorld);
		poseStack.rotate(Axis.YP.rotationDegrees((float)animation.ry));
		submitRing(poseStack, collector, 0.28125, animation.color2, 0, 15, light, inWorld);
		poseStack.rotate(Axis.ZP.rotationDegrees((float)animation.rz));
		submitRing(poseStack, collector, 0.3125, animation.color3, 2, 15, light, inWorld);
		poseStack.popPose();
	}

	/**
	 * V33a's GL_LINE_LOOPs as the independent segment pairs {@code RenderTypes.lines()} takes: plane 0 is
	 * {@code renderVCircle(phi 0)} (Y-Z), 1 is {@code renderVCircle(phi 90)} (X-Y, running the other way round) and 2
	 * is {@code renderCircle} (X-Z).
	 */
	private static void submitRing(PoseStack poseStack, SubmitNodeCollector collector, double r, int argb, int plane, int step, int light, boolean world) {
        SubmitNodeCollector.CustomGeometryRenderer draw=(pose,lines)->{
			Vector3f prev = null;
			Vector3f first = null;
			for (int i = 0; i < 360; i += step) {
				Vector3f p = ringPoint(r, plane, i);
				if (prev != null)
					segment(pose, lines, prev, p, argb, light);
				else
					first = p;
				prev = p;
			}
			segment(pose, lines, prev, first, argb, light);
        };
        if(world)ScreenParticleRendering.submitGeometry(collector,poseStack,ChromaRenderPipelines.ADJACENCY_LINES_TYPE,draw);
        else collector.submitCustomGeometry(poseStack,ChromaRenderPipelines.ADJACENCY_ITEM_LINES_TYPE,draw);
	}

	private static Vector3f ringPoint(double r, int plane, int deg) {
		double a = Math.toRadians(deg);
		return switch(plane) {
			case 0 -> new Vector3f(0, (float)(r*Math.cos(a)), (float)(r*Math.sin(a)));
			case 1 -> new Vector3f((float)(-r*Math.sin(a)), (float)(r*Math.cos(a)), 0);
			default -> new Vector3f((float)(r*Math.cos(a)), 0, (float)(r*Math.sin(a)));
		};
	}

	private static void segment(PoseStack.Pose pose, VertexConsumer lines, Vector3f a, Vector3f b, int argb, int light) {
		Vector3f n = new Vector3f(b).sub(a).normalize();
		lines.addVertex(pose, a.x, a.y, a.z).setColor(argb).setNormal(pose, n.x, n.y, n.z).setLineWidth(LINE_WIDTH).setLight(light);
		lines.addVertex(pose, b.x, b.y, b.z).setColor(argb).setNormal(pose, n.x, n.y, n.z).setLineWidth(LINE_WIDTH).setLight(light);
	}

	private static TextureAtlasSprite glow() {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.GLOWSECTION.getIcon());
	}

	/** V33a {@code drawInner}, in world: the glow section, a sixteenth trimmed off each edge, facing the camera. */
    private static void submitInnerWorld(PoseStack pose,SubmitNodeCollector collector,float yaw,float pitch,int light) {
        pose.pushPose();pose.translate(.5,-.5,-.5);pose.scale(.1875F,.1875F,.1875F);
        pose.rotate(Axis.YP.rotationDegrees(yaw));pose.rotate(Axis.XP.rotationDegrees(pitch));
        submitGlowQuad(pose,collector,light,true);pose.popPose();
    }
    private static void submitInnerItem(PoseStack pose,SubmitNodeCollector collector,int light) {
        pose.pushPose();pose.translate(0,-.125,0);pose.rotate(Axis.YP.rotationDegrees(-45));
        pose.rotate(Axis.XP.rotationDegrees(-30));pose.scale(.1875F,.1875F,.1875F);
        submitGlowQuad(pose,collector,light,false);pose.popPose();
    }
    private static void submitGlowQuad(PoseStack pose,SubmitNodeCollector collector,int light,boolean world) {
        var sprite=glow();float du=sprite.getU1()-sprite.getU0(),dv=sprite.getV1()-sprite.getV0();
        float u0=sprite.getU0()+du/16,u1=sprite.getU1()-du/16,v0=sprite.getV0()+dv/16,v1=sprite.getV1()-dv/16;
        SubmitNodeCollector.CustomGeometryRenderer draw=(captured,out)->{
            glowVertex(captured,out,-1,-1,u0,v0,light);glowVertex(captured,out,1,-1,u1,v0,light);
            glowVertex(captured,out,1,1,u1,v1,light);glowVertex(captured,out,-1,1,u0,v1,light);
        };
        var type=EntityLighting.LIGHTMAP_ONLY.screen(TextureAtlas.LOCATION_BLOCKS,false,!world);
        if(world)ScreenParticleRendering.submitGeometry(collector,pose,type,draw);else collector.submitCustomGeometry(pose,type,draw);
    }
    private static void glowVertex(PoseStack.Pose pose,VertexConsumer out,float x,float y,float u,float v,int light) {
        out.addVertex(pose,x,y,0).setUv(u,v).setColor(0xffffffff).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose,0,0,1);
    }

	public static final class State extends BlockEntityRenderState {
		private AdjacencyUpgrades upgrade = AdjacencyUpgrades.LIGHTBLUE;
		private int tier;
		private boolean disabled;
		private int seed;
        private boolean inWorld;
        private Animation animation;
	}

}
