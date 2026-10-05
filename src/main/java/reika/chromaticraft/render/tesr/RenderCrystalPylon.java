package reika.chromaticraft.render.tesr;

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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.IrisCompatibility;
import reika.chromaticraft.magic.CrystalTarget;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.render.ShaderCompatibleRenderTypes;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** V33a pylon core, enhanced halo, and transmitter beams on the 26.3 submit pipeline. */
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
        state.time = System.currentTimeMillis();
        state.beams.clear();
        double startWidth = pylon.getOutgoingBeamRadius();
        Map<Vec3, BeamGroup> groups = new LinkedHashMap<>();
        for (CrystalTarget target : pylon.getTargets()) {
            Vec3 end = new Vec3(
                    target.location.pos.getX() - pylon.getBlockPos().getX() + target.offsetX,
                    target.location.pos.getY() - pylon.getBlockPos().getY() + target.offsetY,
                    target.location.pos.getZ() - pylon.getBlockPos().getZ() + target.offsetZ);
            groups.computeIfAbsent(end, ignored -> new BeamGroup(end, target.endWidth,
                    target.widthLimit)).colors.add(target.color);
        }
        double radius = startWidth;
        double cycle = (state.time / 600D % 360) / 30D;
        for (BeamGroup group : groups.values()) {
            radius = Math.min(radius, group.widthLimit);
            state.beams.add(new Beam(group.end.x, group.end.y, group.end.z, radius,
                    Math.min(group.endWidth, group.widthLimit),
                    blendedBeamColor(group.colors, cycle, 0.125)));
        }
        state.hasTargets = !state.beams.isEmpty();
        state.color = 0xff000000 | pylon.getRenderColor();
        state.randomOffset = pylon.randomOffset;
        // V33a: Void Gaze's first level drew the flares with the depth test off, through any wall.
        state.xray = reika.chromaticraft.client.VoidGazeClient.isActive(
                reika.chromaticraft.magic.potions.PotionVoidGaze.VoidGazeLevels.PYLONXRAY);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (IrisCompatibility.shadowPass()) return;
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
                    (float)scale, state.color, 0, layer == 0 && !sun, state.xray);
        }

        if (state.conducting && state.enhanced) {
            double angle = (state.time / 50D) % 360;
            int mix = (int)(127 + 92 * Math.sin(Math.toRadians(angle / 2D)));
            int haloColor = 0xff000000 | ReikaColorAPI.mixColors(
                    state.color & 0xffffff, 0, mix / 255F);
            float scale = (float)(3 + Math.sin(Math.toRadians(angle)));
            this.submitBillboard(poseStack, collector, camera, TURBO, scale,
                    haloColor, (float)-angle, false, state.xray);
        }
        poseStack.popPose();
    }

    private void submitBeams(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.beams.isEmpty()) return;
        float scroll = (float)((state.time / 600D % 360) / 30D);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        for (Beam beam : state.beams) {
            submitAfterTerrain(poseStack, collector, IrisCompatibility.shadersEnabled()
                            ? ShaderCompatibleRenderTypes.beam(BEAM) : ChromaRenderPipelines.energyBeam(BEAM),
                    (pose, vertices) -> beamTube(pose, vertices, beam, scroll));
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
            Vector3f normal = ring(side, up, (a0+a1)*0.5, 1);
            beamVertex(pose, vertices, s0, scroll, scroll+1, color, normal);
            beamVertex(pose, vertices, s1, scroll, scroll+1, color, normal);
            beamVertex(pose, vertices, e1, scroll+1, scroll, color, normal);
            beamVertex(pose, vertices, e0, scroll+1, scroll, color, normal);
        }
    }

    private static void beamVertex(PoseStack.Pose pose, VertexConsumer vertices, Vector3f point,
            float u, float v, int color, Vector3f normal) {
        vertices.addVertex(pose, point.x, point.y, point.z).setUv(u, v).setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private static Vector3f ring(Vector3f side, Vector3f up, double angle, double radius) {
        return new Vector3f(side).mul((float)(Math.sin(angle) * radius))
                .add(new Vector3f(up).mul((float)(Math.cos(angle) * radius)));
    }

    private static int blendedBeamColor(List<CrystalElement> colors, double tick, double modulus) {
        if (colors.isEmpty()) return 0;
        int first = (int)(tick / modulus);
        first = (first + colors.size()) % colors.size();
        int c1 = colors.get(first).color();
        int c2 = colors.get((first + 1) % colors.size()).color();
        float fraction = (float)(tick % modulus / modulus);
        return ReikaColorAPI.mixColors(c1, c2, 1 - fraction);
    }

    private void submitBillboard(PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera, Identifier spriteId, float scale, int color, float roll,
            boolean core, boolean xray) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(spriteId);
        boolean shaders = IrisCompatibility.shadersEnabled();
        poseStack.pushPose();
        poseStack.rotate(camera.orientation);
        if (roll != 0) poseStack.rotate(Axis.ZP.rotationDegrees(roll));
        if (core) {
            CustomFeatureRenderer.Submit base = new CustomFeatureRenderer.Submit(
                    poseStack.last().copy(),
                    shaders ? ShaderCompatibleRenderTypes.pylon(TextureAtlas.LOCATION_BLOCKS, xray)
                            : xray ? ChromaRenderPipelines.pylonCoreThroughWall(TextureAtlas.LOCATION_BLOCKS)
                            : ChromaRenderPipelines.pylonCore(TextureAtlas.LOCATION_BLOCKS),
                    (pose, vertices) -> quad(pose, vertices, shaders ? alphaSprite(spriteId, true) : sprite, scale, color));
            collector.order(-1)
                    .submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, base);
        }
        submitAfterTerrain(poseStack, collector,
                shaders ? ShaderCompatibleRenderTypes.pylon(TextureAtlas.LOCATION_BLOCKS, xray)
                        : xray ? ChromaRenderPipelines.additiveSpriteThroughWall(TextureAtlas.LOCATION_BLOCKS)
                        : ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
                (pose, vertices) -> quad(pose, vertices, shaders ? alphaSprite(spriteId, false) : sprite, scale, color));
        poseStack.popPose();
    }

    private static TextureAtlasSprite alphaSprite(Identifier original, boolean core) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(original.withSuffix(core ? "_core_alpha" : "_alpha"));
    }

    /**
     * V33a drew the pylon after translucent world geometry. The ordinary 26.3 custom-geometry
     * phase runs before water, which lets water and clouds wash the additive colour away.
     */
    private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
            net.minecraft.client.renderer.rendertype.RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer renderer) {
        CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
                poseStack.last().copy(), renderType, renderer);
        collector.order(0)
        // TRANSLUCENT_CUSTOM_GEOMETRY is drained before the translucent chunk layer;
        // AFTER_TERRAIN is the actual post-water hook. Water is therefore already down and can
        // correctly occlude the glow, while the pipeline's depth still orders later compositing.
                .submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vertices,
            TextureAtlasSprite sprite, float scale, int color) {
        flareVertex(pose, vertices, -scale, -scale, sprite.getU0(), sprite.getV1(), color);
        flareVertex(pose, vertices, scale, -scale, sprite.getU1(), sprite.getV1(), color);
        flareVertex(pose, vertices, scale, scale, sprite.getU1(), sprite.getV0(), color);
        flareVertex(pose, vertices, -scale, scale, sprite.getU0(), sprite.getV0(), color);
    }

    private static void flareVertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float u, float v, int color) {
        vertices.addVertex(pose, x, y, 0).setUv(u, v).setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
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
        private boolean xray;
        private final List<Beam> beams = new ArrayList<>();
    }

    private record Beam(double x, double y, double z, double startRadius,
            double endRadius, int color) {}

    private static final class BeamGroup {
        private final Vec3 end;
        private final double endWidth;
        private final double widthLimit;
        private final List<CrystalElement> colors = new ArrayList<>();

        private BeamGroup(Vec3 end, double endWidth, double widthLimit) {
            this.end = end;
            this.endWidth = endWidth;
            this.widthLimit = widthLimit;
        }
    }
}
