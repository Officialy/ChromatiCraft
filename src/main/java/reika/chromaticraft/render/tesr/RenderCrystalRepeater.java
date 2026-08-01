package reika.chromaticraft.render.tesr;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.CrystalTarget;
import reika.chromaticraft.magic.castingtuning.CastingTuningRegistry;
import reika.chromaticraft.magic.network.PylonFinder;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Full V33a crystal-repeater presentation on Minecraft 26.2's submit pipeline. */
public class RenderCrystalRepeater implements BlockEntityRenderer<TileEntityCrystalRepeater, RenderCrystalRepeater.State> {

    private static final Identifier SPARKLE = sprite("sparkle");
    private static final Identifier RAIN_FLARE = sprite("rainflare");
    private static final Identifier SUN_FLARE = sprite("sunflare");
    private static final Identifier CELL_FLARE = sprite("cellflare");
    private static final Identifier BEAM = effect("beam.png");
    private static final Identifier RANGE = effect("repeater_range.png");
    private static final Identifier TURBO_SECTIONS = effect("turbo/sections.png");
    private static final Identifier TURBO_RADIATE = effect("turbo/radiate.png");
    private static final Identifier TUNING_ICONS = effect("cast_tuning_icons.png");

    private static long fadeTick = Long.MIN_VALUE;
    private static float manipulatorFade;

    public RenderCrystalRepeater(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileEntityCrystalRepeater repeater, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(repeater, state, partialTick, cameraPosition, breakProgress);
        state.conducting = repeater.canConduct();
        state.rainAffected = repeater.isRainAffected();
        state.tableGrouped = repeater.isTableGrouped();
        state.cluster = repeater.hasClusterRender();
        state.turbo = repeater.isTurbocharged();
        state.color = 0xff000000 | this.getHaloRenderColor(repeater);
        state.ticks = repeater.getTicksExisted();
        state.time = System.currentTimeMillis();
        state.range = Math.min(repeater.getReceiveRange(), repeater.getSendRange());
        state.rangeAlpha = repeater.getRangeAlpha();
        state.connectionAlpha = repeater.updateAndGetConnectionRenderAlpha();
        state.manipulatorFade = updateManipulatorFade(repeater);
        state.tuningIcon = -1;
        if (state.manipulatorFade > 0 && repeater.getCaster() != null && repeater.getLevel() != null) {
            state.tuningIcon = CastingTuningRegistry.instance
                    .getTuningKey(repeater.getLevel(), repeater.getCaster()).iconIndex();
        }
        state.playerLine = findPlayerLine(repeater, state.connectionAlpha);

        state.beams.clear();
        double startWidth = repeater.getOutgoingBeamRadius();
        for (CrystalTarget target : repeater.getTargets()) {
            state.beams.add(new Beam(
                    target.location.pos.getX() - repeater.getBlockPos().getX() + target.offsetX - 0.5,
                    target.location.pos.getY() - repeater.getBlockPos().getY() + target.offsetY - 0.5,
                    target.location.pos.getZ() - repeater.getBlockPos().getZ() + target.offsetZ - 0.5,
                    Math.min(startWidth, target.widthLimit),
                    Math.min(target.endWidth, target.widthLimit), target.color.getColor()));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        submitBeams(state, poseStack, collector);
        if (!state.conducting) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        if (state.playerLine != null)
            submitDashedLine(poseStack, collector, state.playerLine, state.connectionAlpha);
        if (state.rangeAlpha > 0)
            submitRangeSphere(state, poseStack, collector);

        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        submitAtlasBillboard(poseStack, collector, SPARKLE, 0.75F, 0xffffffff, 0);

        float iconScale = (float)((2.75 + 0.125 * Math.sin(state.ticks / 40D)) * 0.75);
        if (state.rainAffected)
            submitAtlasBillboard(poseStack, collector, RAIN_FLARE, iconScale, state.color, 0);
        if (state.tableGrouped)
            submitAtlasBillboard(poseStack, collector, SUN_FLARE, iconScale, state.color, 0);
        else if (state.cluster)
            submitAtlasBillboard(poseStack, collector, CELL_FLARE, iconScale * 1.25F, state.color, 0);

        if (state.tuningIcon >= 0 && state.manipulatorFade > 0)
            submitTuningIcon(state, poseStack, collector);
        if (state.turbo)
            submitTurboHalo(state, poseStack, collector);
        poseStack.popPose();
        poseStack.popPose();
    }

    protected int getHaloRenderColor(TileEntityCrystalRepeater repeater) {
        CrystalElement element = repeater.getActiveColor();
        return element != null ? element.getColor() : 0xffffff;
    }

    private static float updateManipulatorFade(TileEntityCrystalRepeater repeater) {
        if (repeater.getLevel() == null) return 0;
        long tick = repeater.getLevel().getGameTime();
        if (tick != fadeTick) {
            fadeTick = tick;
            var player = Minecraft.getInstance().player;
            ItemStack held = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
            if (held.is(ChromaItems.MANIPULATOR.get()))
                manipulatorFade = Math.min(1, manipulatorFade + 0.125F);
            else
                manipulatorFade = Math.max(0, manipulatorFade - 0.03125F);
        }
        return manipulatorFade;
    }

    private static @Nullable Vec3 findPlayerLine(TileEntityCrystalRepeater repeater, int alpha) {
        if (alpha <= 0 || repeater.getLevel() == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return null;
        HitResult hit = minecraft.player.pick(4.5, 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return null;
        var target = blockHit.getBlockPos().relative(blockHit.getDirection());
        double dx = target.getX() - repeater.getBlockPos().getX();
        double dy = target.getY() - repeater.getBlockPos().getY();
        double dz = target.getZ() - repeater.getBlockPos().getZ();
        if (dx * dx + dy * dy + dz * dz > repeater.getSendRange() * repeater.getSendRange()) return null;
        return PylonFinder.lineOfSight(repeater.getLevel(), repeater.getX(), repeater.getY(), repeater.getZ(),
                target.getX(), target.getY(), target.getZ()).hasLineOfSight ? new Vec3(dx, dy, dz) : null;
    }

    private static void submitTuningIcon(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        int color = ReikaColorAPI.mixColors(state.color & 0xffffff, 0xffffff,
                0.875F + 0.125F * (float)Math.sin(state.ticks / 90D));
        color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(color, state.manipulatorFade);
        float scale = (float)(1.3125 + 0.046875 * Math.sin(state.ticks / 6D));
        if (state.turbo) scale *= 1.25F;
        int col = state.tuningIcon % 4;
        int row = state.tuningIcon / 4;
        submitTexturedQuad(poseStack, collector, TUNING_ICONS, scale, color, 0,
                col / 4F, row / 4F, (col + 1) / 4F, (row + 1) / 4F);
    }

    private static void submitTurboHalo(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        double phase = state.time / 50D % 360;
        float cumulative = 0;
        int layer = 0;
        for (int angle = 0; angle < 90; angle += 15) {
            cumulative += angle + (float)phase;
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(cumulative));
            float z = -angle / 15F * 0.0075F;
            float scale = (float)((1.5 + 0.5 * Math.sin(Math.toRadians(4 * phase + angle * 2))) * 0.75);
            submitTexturedQuad(poseStack, collector, TURBO_SECTIONS, scale, state.color, z, 0, 0, 1, 1);
            float u = (state.ticks + layer * 2) % 18 / 18F;
            submitTexturedQuad(poseStack, collector, TURBO_RADIATE, 2.25F, state.color, z - 0.00375F,
                    u, 0, u + 1 / 18F, 1);
            poseStack.popPose();
            layer++;
        }
    }

    private static void submitRangeSphere(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        int color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(
                state.color & 0xffffff, state.rangeAlpha / 512F);
        double radius = state.range;
        double stepY = 0.5 * radius / 16D;
        double pulse = 0.75 * Math.sin(state.ticks / 128D);
        PoseStack renderPose = copy(poseStack);
        submitAfterTerrain(poseStack, collector, ChromaRenderPipelines.additiveSprite(RANGE),
                (pose, vertices) -> {
                    for (double y = -radius; y <= radius; y += stepY) {
                        double nextY = y + stepY;
                        double ring = pulse + Math.sqrt(Math.max(0, radius * radius - y * y));
                        double nextRing = pulse + Math.sqrt(Math.max(0, radius * radius - nextY * nextY));
                        for (int degrees = 0; degrees < 360; degrees += 10) {
                            double a0 = Math.toRadians(degrees);
                            double a1 = Math.toRadians(degrees + 10);
                            double ti = degrees + state.time / 50D % 360;
                            double tk = y + state.time / 220D % 360;
                            float u0 = (float)(ti / 360D * 3);
                            float u1 = (float)((ti + 10) / 360D * 3);
                            float v0 = (float)(tk * radius / 1024D);
                            float v1 = (float)((tk + stepY) * radius / 1024D);
                            vertex(vertices, renderPose.last(), 0.5 + ring * Math.cos(a0), 0.5 + y,
                                    0.5 + ring * Math.sin(a0), u0, v0, color);
                            vertex(vertices, renderPose.last(), 0.5 + ring * Math.cos(a1), 0.5 + y,
                                    0.5 + ring * Math.sin(a1), u1, v0, color);
                            vertex(vertices, renderPose.last(), 0.5 + nextRing * Math.cos(a1), 0.5 + nextY,
                                    0.5 + nextRing * Math.sin(a1), u1, v1, color);
                            vertex(vertices, renderPose.last(), 0.5 + nextRing * Math.cos(a0), 0.5 + nextY,
                                    0.5 + nextRing * Math.sin(a0), u0, v1, color);
                        }
                    }
                });
    }

    private static void submitDashedLine(PoseStack poseStack, SubmitNodeCollector collector, Vec3 end, int alpha) {
        PoseStack renderPose = copy(poseStack);
        int color = (alpha << 24) | 0xffffff;
        submitAfterTerrain(poseStack, collector, RenderTypes.linesTranslucent(), (pose, vertices) -> {
            double length = end.length();
            int segments = Math.max(1, (int)Math.ceil(length * 4));
            Vector3f normal = new Vector3f((float)end.x, (float)end.y, (float)end.z).normalize();
            for (int i = 0; i < segments; i += 2) {
                double f0 = i / (double)segments;
                double f1 = Math.min(1, (i + 1) / (double)segments);
                lineVertex(vertices, renderPose.last(), end.scale(f0), normal, color);
                lineVertex(vertices, renderPose.last(), end.scale(f1), normal, color);
            }
        });
    }

    private static void submitBeams(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.beams.isEmpty()) return;
        float scroll = (float)((state.time / 600D % 360) / 30D);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        for (Beam beam : state.beams) {
            PoseStack renderPose = copy(poseStack);
            submitAfterTerrain(poseStack, collector, ChromaRenderPipelines.additiveSprite(BEAM),
                    (pose, vertices) -> beamTube(renderPose.last(), vertices, beam, scroll));
        }
        poseStack.popPose();
    }

    private static void beamTube(PoseStack.Pose pose, VertexConsumer vertices, Beam beam, float scroll) {
        Vector3f axis = new Vector3f((float)beam.x, (float)beam.y, (float)beam.z);
        if (axis.lengthSquared() < 1.0E-6F) return;
        axis.normalize();
        Vector3f side = Math.abs(axis.y) < 0.99F
                ? axis.cross(new Vector3f(0, 1, 0), new Vector3f()).normalize()
                : axis.cross(new Vector3f(1, 0, 0), new Vector3f()).normalize();
        Vector3f up = axis.cross(side, new Vector3f()).normalize();
        int color = 0xff000000 | beam.color;
        for (int i = 0; i < 6; i++) {
            double a0 = i * Math.PI * 2 / 6D;
            double a1 = (i + 1) * Math.PI * 2 / 6D;
            Vector3f s0 = ring(side, up, a0, beam.startRadius * 0.75);
            Vector3f s1 = ring(side, up, a1, beam.startRadius * 0.75);
            Vector3f e0 = ring(side, up, a0, beam.endRadius * 0.75).add((float)beam.x, (float)beam.y, (float)beam.z);
            Vector3f e1 = ring(side, up, a1, beam.endRadius * 0.75).add((float)beam.x, (float)beam.y, (float)beam.z);
            vertex(vertices, pose, s0.x, s0.y, s0.z, scroll, scroll + 1, color);
            vertex(vertices, pose, s1.x, s1.y, s1.z, scroll, scroll + 1, color);
            vertex(vertices, pose, e1.x, e1.y, e1.z, scroll + 1, scroll, color);
            vertex(vertices, pose, e0.x, e0.y, e0.z, scroll + 1, scroll, color);
        }
    }

    private static Vector3f ring(Vector3f side, Vector3f up, double angle, double radius) {
        return new Vector3f(side).mul((float)(Math.sin(angle) * radius))
                .add(new Vector3f(up).mul((float)(Math.cos(angle) * radius)));
    }

    private static void submitAtlasBillboard(PoseStack poseStack, SubmitNodeCollector collector,
            Identifier spriteId, float scale, int color, float z) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(spriteId);
        PoseStack renderPose = copy(poseStack);
        submitAfterTerrain(poseStack, collector,
                ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
                (pose, vertices) -> quad(vertices, renderPose.last(), scale, z, color,
                        sprite.getU0(), sprite.getV1(), sprite.getU1(), sprite.getV0()));
    }

    private static void submitTexturedQuad(PoseStack poseStack, SubmitNodeCollector collector,
            Identifier texture, float scale, int color, float z, float u0, float v0, float u1, float v1) {
        PoseStack renderPose = copy(poseStack);
        submitAfterTerrain(poseStack, collector, ChromaRenderPipelines.additiveSprite(texture),
                (pose, vertices) -> quad(vertices, renderPose.last(), scale, z, color, u0, v1, u1, v0));
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose, float scale, float z,
            int color, float u0, float v0, float u1, float v1) {
        vertex(vertices, pose, -scale, -scale, z, u0, v0, color);
        vertex(vertices, pose, scale, -scale, z, u1, v0, color);
        vertex(vertices, pose, scale, scale, z, u1, v1, color);
        vertex(vertices, pose, -scale, scale, z, u0, v1, color);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, double x, double y, double z,
            float u, float v, int color) {
        vertices.addVertex(pose, (float)x, (float)y, (float)z).setUv(u, v).setColor(color);
    }

    private static void lineVertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point,
            Vector3f normal, int color) {
        vertices.addVertex(pose, (float)point.x, (float)point.y, (float)point.z).setColor(color)
                .setNormal(pose, normal.x, normal.y, normal.z).setLineWidth(6);
    }

    private static PoseStack copy(PoseStack source) {
        PoseStack copy = new PoseStack();
        copy.last().set(source.last());
        return copy;
    }

    private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
            RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
                poseStack.last().copy(), renderType, renderer);
        ((OrderedSubmitNodeCollectorExtension)collector.order(0))
                .submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
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
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/" + name);
    }

    private static Identifier effect(String name) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/effect/" + name);
    }

    public static final class State extends BlockEntityRenderState {
        private boolean conducting;
        private boolean rainAffected;
        private boolean tableGrouped;
        private boolean cluster;
        private boolean turbo;
        private int color;
        private int ticks;
        private int range;
        private int rangeAlpha;
        private int connectionAlpha;
        private int tuningIcon;
        private float manipulatorFade;
        private long time;
        private @Nullable Vec3 playerLine;
        private final List<Beam> beams = new ArrayList<>();
    }

    private record Beam(double x, double y, double z, double startRadius, double endRadius, int color) {}
}