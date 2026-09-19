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
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
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

    private static void renderPoints(PoseStack.Pose pose, VertexConsumer vertices,
                                     List<DecimalPosition> points, int color, int alpha) {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        Vector3f normal = new Vector3f();
        if (points.size() < 2)
            return;
        // RenderTypes.lines() consumes independent vertex pairs, unlike V33a's GL_LINE_STRIP.
        // Pair every adjacent sample, then close the final sample back to the first. The previous
        // port emitted (sample, first) for every sample, which turned the locus into a radial web.
        for (int i = 1; i < points.size(); i++)
            addSegment(pose, vertices, points.get(i - 1), points.get(i), red, green, blue, alpha,
                    normal);
        addSegment(pose, vertices, points.getLast(), points.getFirst(), red, green, blue, alpha,
                normal);
    }

    private static void addSegment(PoseStack.Pose pose, VertexConsumer vertices,
                                   DecimalPosition first, DecimalPosition second,
                                   int red, int green, int blue, int alpha, Vector3f normal) {
        vertices.addVertex(pose, (float) (0.5 + first.xCoord), (float) (0.5 + first.yCoord),
                (float) (0.5 + first.zCoord)).setNormal(pose, normal)
                .setColor(red, green, blue, alpha).setLineWidth(2F);
        vertices.addVertex(pose, (float) (0.5 + second.xCoord), (float) (0.5 + second.yCoord),
                (float) (0.5 + second.zCoord)).setNormal(pose, normal)
                .setColor(red, green, blue, alpha).setLineWidth(2F);
    }

    public void submit(SubmitNodeCollector collector, PoseStack stack, int color,
                       boolean inWorld) {
        List<DecimalPosition> points = spline.get(32, true);
        int alpha = color >>> 24;
        this.submitLine(collector, stack, points, color, alpha);
        if (inWorld) {
            this.submitLine(collector, stack, points, color, alpha / 4);
            this.submitLine(collector, stack, points, color, alpha / 4);
        }
    }

    private void submitLine(SubmitNodeCollector collector, PoseStack stack,
                            List<DecimalPosition> points, int color, int alpha) {
        PoseStack renderPose = new PoseStack();
        renderPose.last().set(stack.last());
        SubmitNodeCollector.CustomGeometryRenderer geometry = (ignored, vertices) ->
                renderPoints(renderPose.last(), vertices, points, color, alpha);
        CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
                stack.last().copy(), ChromaRenderPipelines.auraLocusLines(), geometry);
        ((OrderedSubmitNodeCollectorExtension) collector.order(0))
                .submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
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
