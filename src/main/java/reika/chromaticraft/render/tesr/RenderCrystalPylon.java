package reika.chromaticraft.render.tesr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.CrystalTarget;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** V33a pylon core, enhanced halo, and inherited transmitter beam pass on the 26.2 submit pipeline. */
public final class RenderCrystalPylon implements BlockEntityRenderer<TileEntityCrystalPylon, RenderCrystalPylon.State> {

    private static final Identifier ROUND_FLARE = sprite("roundflare");
    private static final Identifier SUN_FLARE = sprite("sunflare");
    private static final Identifier TURBO = sprite("turbo");
    private static final Identifier BEAM = Identifier.fromNamespaceAndPath(
            ChromatiCraft.MODID, "textures/effect/beam.png");

    public RenderCrystalPylon(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileEntityCrystalPylon pylon, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pylon, state, partialTick, cameraPosition, breakProgress);
        state.enhanced = pylon.isEnhanced();
        state.unstable = pylon.isUnstable();
        state.conducting = pylon.canConduct();
        state.beams.clear();
        double startWidth = pylon.getOutgoingBeamRadius();
        for (CrystalTarget target : pylon.getTargets()) {
            state.beams.add(new Beam(
                    target.location.pos.getX() - pylon.getBlockPos().getX() + target.offsetX - 0.5,
                    target.location.pos.getY() - pylon.getBlockPos().getY() + target.offsetY - 0.5,
                    target.location.pos.getZ() - pylon.getBlockPos().getZ() + target.offsetZ - 0.5,
                    Math.min(startWidth, target.widthLimit),
                    Math.min(target.endWidth, target.widthLimit),
                    target.color.getColor()));
        }
        state.hasTargets = !state.beams.isEmpty();
        state.color = 0xff000000 | pylon.getRenderColor();
        state.randomOffset = pylon.randomOffset;
        state.time = System.currentTimeMillis();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        this.submitBeams(state, poseStack, collector);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        int count = state.enhanced ? 2 : 1;
        if (state.unstable) count++;
        for (int drawIndex = 0; drawIndex < count; drawIndex++) {
            int layer = drawIndex;
            boolean sun = false;
            double baseScale = 0;
            if (state.unstable) {
                if (layer == 0) {
                    sun = true;
                    baseScale = 0.25;
                } else {
                    layer--;
                    baseScale = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
                }
            }
            double phase = (layer * 60 + state.randomOffset
                    + state.time / 2000D * (1 + 3 * layer)) % 360;
            double scale = baseScale + layer * 0.5 + 2.5 + 0.5 * Math.sin(phase);
            if (state.hasTargets) scale++;
            if (!state.conducting) scale = 0.75;
            this.submitBillboard(poseStack, collector, camera, sun ? SUN_FLARE : ROUND_FLARE,
                    (float)scale, state.color, 0);
        }

        if (state.conducting && state.enhanced) {
            double angle = (state.time / 50D) % 360;
            int mix = (int)(127 + 92 * Math.sin(Math.toRadians(angle / 2D)));
            int haloColor = 0xff000000 | ReikaColorAPI.mixColors(
                    state.color & 0xffffff, 0, mix / 255F);
            float scale = (float)(3 + Math.sin(Math.toRadians(angle)));
            this.submitBillboard(poseStack, collector, camera, TURBO, scale,
                    haloColor, (float)-angle);
        }
        poseStack.popPose();
    }

    private void submitBeams(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.beams.isEmpty()) return;
        float scroll = (float)((state.time / 600D % 360) / 30D);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        for (Beam beam : state.beams) {
            PoseStack renderPose = new PoseStack();
            renderPose.last().set(poseStack.last());
            submitAfterTerrain(poseStack, collector, ChromaRenderPipelines.additiveSprite(BEAM),
                    (pose, vertices) -> beamTube(renderPose.last(), vertices, beam, scroll));
        }
        poseStack.popPose();
    }

    /** Six-sided tapered tube from V33a ChromaFX.drawEnergyTransferBeam. */
    private static void beamTube(PoseStack.Pose pose, VertexConsumer vertices, Beam beam, float scroll) {
        Vector3f axis = new Vector3f((float)beam.x, (float)beam.y, (float)beam.z);
        if (axis.lengthSquared() < 1.0E-6F) return;
        axis.normalize();
        Vector3f side = Math.abs(axis.y) < 0.99F
                ? axis.cross(new Vector3f(0, 1, 0), new Vector3f()).normalize()
                : axis.cross(new Vector3f(1, 0, 0), new Vector3f()).normalize();
        Vector3f up = axis.cross(side, new Vector3f()).normalize();
        int color = 0xff000000 | beam.color;
        for (int sideIndex = 0; sideIndex < 6; sideIndex++) {
            double a0 = sideIndex * Math.PI * 2 / 6D;
            double a1 = (sideIndex + 1) * Math.PI * 2 / 6D;
            Vector3f s0 = ring(side, up, a0, beam.startRadius * 0.75);
            Vector3f s1 = ring(side, up, a1, beam.startRadius * 0.75);
            Vector3f e0 = ring(side, up, a0, beam.endRadius * 0.75)
                    .add((float)beam.x, (float)beam.y, (float)beam.z);
            Vector3f e1 = ring(side, up, a1, beam.endRadius * 0.75)
                    .add((float)beam.x, (float)beam.y, (float)beam.z);
            vertices.addVertex(pose, s0.x, s0.y, s0.z).setUv(scroll, scroll + 1).setColor(color);
            vertices.addVertex(pose, s1.x, s1.y, s1.z).setUv(scroll, scroll + 1).setColor(color);
            vertices.addVertex(pose, e1.x, e1.y, e1.z).setUv(scroll + 1, scroll).setColor(color);
            vertices.addVertex(pose, e0.x, e0.y, e0.z).setUv(scroll + 1, scroll).setColor(color);
        }
    }

    private static Vector3f ring(Vector3f side, Vector3f up, double angle, double radius) {
        return new Vector3f(side).mul((float)(Math.sin(angle) * radius))
                .add(new Vector3f(up).mul((float)(Math.cos(angle) * radius)));
    }

    private void submitBillboard(PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera, Identifier spriteId, float scale, int color, float roll) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(spriteId);
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        if (roll != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        PoseStack renderPose = new PoseStack();
        renderPose.last().set(poseStack.last());
        submitAfterTerrain(poseStack, collector,
                ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
                (pose, vertices) -> quad(renderPose.last(), vertices, sprite, scale, color));
        poseStack.popPose();
    }

    /**
     * V33a drew the pylon after translucent world geometry. The ordinary 26.2 custom-geometry
     * phase runs before water, which lets water and clouds wash the additive colour away.
     */
    private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
            net.minecraft.client.renderer.rendertype.RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer renderer) {
        CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
                poseStack.last().copy(), renderType, renderer);
        ((OrderedSubmitNodeCollectorExtension)collector.order(0))
        // TRANSLUCENT_CUSTOM_GEOMETRY, not AFTER_TERRAIN. AFTER_TERRAIN runs before the translucent
        // chunk layer, so the glow was drawn into main *before* water: writing depth there made water
        // fail its own depth test and punched square holes in the surface, while not writing it left
        // the cloud target -- which the post-chain composites over main -- with nothing to sort
        // against, so clouds covered pylons in front of them. Submitting after translucent terrain
        // resolves both: water is already down so it cannot be rejected, depth testing still hides
        // the glow behind water and terrain, and the depth this pipeline now writes lets the cloud
        // compositor order it correctly.
                .submitSpecial(RenderPhaseKeys.TRANSLUCENT_CUSTOM_GEOMETRY, submit);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vertices,
            TextureAtlasSprite sprite, float scale, int color) {
        vertices.addVertex(pose, -scale, -scale, 0).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
        vertices.addVertex(pose, scale, -scale, 0).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
        vertices.addVertex(pose, scale, scale, 0).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
        vertices.addVertex(pose, -scale, scale, 0).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(
                ChromatiCraft.MODID, "block/icons/" + name);
    }

    public static final class State extends BlockEntityRenderState {
        private boolean enhanced;
        private boolean unstable;
        private boolean conducting;
        private boolean hasTargets;
        private int color = 0xffffffff;
        private int randomOffset;
        private long time;
        private final List<Beam> beams = new ArrayList<>();
    }

    private record Beam(double x, double y, double z, double startRadius,
            double endRadius, int color) {}
}
