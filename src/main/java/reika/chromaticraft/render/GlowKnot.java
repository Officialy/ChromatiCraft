/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Vector3f;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;
import reika.dragonapi.instantiable.math.Spline.SplineAnchor;
import reika.dragonapi.instantiable.math.Spline.SplineType;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

import java.util.List;
import java.util.Random;

/**
 * The living forty-eight-anchor centripetal spline at the heart of V33a's Aura Locus.
 */
public final class GlowKnot {

    private static final Random RAND = new Random();
    public final int density = 48;
    public final double size;
    private final Spline spline = new Spline(SplineType.CENTRIPETAL);

    public GlowKnot(double size) {
        this.size = size;
        for (int i = 0; i < density; i++)
            spline.addPoint(new KnotPoint(size, RAND.nextDouble() * 360,
                    RAND.nextDouble() * 360, size));
    }

    /** A frame owns its samples, independently of the living spline and reusable render state. */
    public record Frame(List<DecimalPosition> points) {
        public Frame {points=List.copyOf(points);}
    }
    public Frame snapshot() {return new Frame(spline.get(32,true));}

    /** Full source spline and fade: default-alpha widths 1/5/10, followed by the screen billboard. */
    public static void submit(Frame frame,SubmitNodeCollector collector,PoseStack pose,int color,
            boolean world,boolean depthTest,float baseWidth,float fadeYaw,float fadePitch,int light) {
        submitLine(frame,collector,pose,color,baseWidth,world,depthTest,light);
        if(world) {
            int faint=(color&0xffffff)|((color>>>24)/4)<<24;
            submitLine(frame,collector,pose,faint,5,world,depthTest,light);
            submitLine(frame,collector,pose,faint,10,world,depthTest,light);
        }
        var sprite=net.minecraft.client.Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS)
                .getSprite(reika.chromaticraft.registry.ChromaIcons.FADE.getIcon());
        int alpha=160;
        int faded=alpha<<24|reika.dragonapi.libraries.rendering.ReikaColorAPI.getColorWithBrightnessMultiplier(color&0xffffff,alpha/255F);
        pose.pushPose();pose.translate(.5,.5,.5);
        if(world) {pose.rotate(com.mojang.math.Axis.YP.rotationDegrees(fadeYaw));pose.rotate(com.mojang.math.Axis.XP.rotationDegrees(fadePitch));}
        LocusRendering.quad(pose,collector,LocusRendering.screen(depthTest),world,-1.25F,-1.25F,1.25F,1.25F,.05F,
                sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),faded,light);
        pose.popPose();
    }
    /** Compatibility entry point for independent knot users; snapshots before any submission. */
    public void submit(SubmitNodeCollector collector,PoseStack pose,int color,boolean world) {
        submit(snapshot(),collector,pose,color,world,world,world?1:1.5F,0,0,LocusRendering.CORE_LIGHT);
    }
    private static void submitLine(Frame frame,SubmitNodeCollector collector,PoseStack pose,int color,
            float width,boolean world,boolean depthTest,int light) {
        LocusRendering.submit(pose,collector,LocusRendering.lines(depthTest),(captured,vertices)->{
            var points=frame.points();
            for(int i=0;i<points.size();i++) {
                var first=points.get(i);var second=points.get((i+1)%points.size());
                float dx=(float)(second.xCoord()-first.xCoord()),dy=(float)(second.yCoord()-first.yCoord()),dz=(float)(second.zCoord()-first.zCoord());
                var normal=new Vector3f(dx,dy,dz);
                // Closed interpolation repeats the initial endpoint; omit that zero-length closure.
                if(normal.lengthSquared()==0)continue;
                normal.normalize();
                lineVertex(captured,vertices,first,color,width,normal,light);
                lineVertex(captured,vertices,second,color,width,normal,light);
            }
        },world);
    }
    private static void lineVertex(PoseStack.Pose pose,VertexConsumer out,DecimalPosition point,int color,float width,Vector3f normal,int light) {
        out.addVertex(pose,(float)(.5+point.xCoord()),(float)(.5+point.yCoord()),(float)(.5+point.zCoord()))
                .setColor(color).setNormal(pose,normal).setLineWidth(width).setLight(light);
    }

    public void update() {
        spline.update();
    }

    private static final class KnotPoint implements SplineAnchor {

        private final double maxSize;
        private double radius;
        private double theta;
        private double phi;
        private double targetRadius;
        private double targetTheta;
        private double targetPhi;

        private KnotPoint(double radius, double theta, double phi, double size) {
            this.radius = radius;
            this.theta = theta;
            this.phi = phi;
            maxSize = size;
            this.pickNewTarget();
        }

        @Override
        public DecimalPosition asPosition() {
            double[] point = ReikaPhysicsHelper.polarToCartesian(radius, theta, phi);
            return new DecimalPosition(point[0], point[1], point[2]);
        }

        @Override
        public void update() {
            double dr = targetRadius - radius;
            double dt = targetTheta - theta;
            double dp = targetPhi - phi;
            if (Math.abs(dr) < 0.05 && Math.abs(dt) < 1 && Math.abs(dp) < 1) {
                this.pickNewTarget();
                return;
            }
            if (Math.abs(dr) >= 0.05)
                radius += 0.025 * Math.signum(dr);
            if (Math.abs(dt) >= 1)
                theta += 0.25 * Math.signum(dt);
            if (Math.abs(dp) >= 1)
                phi += 0.25 * Math.signum(dp);
        }

        private void pickNewTarget() {
            targetRadius = ReikaRandomHelper.getRandomPlusMinus(maxSize, maxSize / 16D, RAND);
            targetTheta = RAND.nextDouble() * 360;
            targetPhi = RAND.nextDouble() * 360;
        }
    }
}
