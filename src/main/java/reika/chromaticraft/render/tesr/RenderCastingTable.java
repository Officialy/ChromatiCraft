package reika.chromaticraft.render.tesr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;

/** V33a's full-bright, transient elemental engravings across an upgraded casting structure. */
public final class RenderCastingTable implements BlockEntityRenderer<TileEntityCastingTable, RenderCastingTable.State> {

    private static final int SPAWN_INTERVAL = 50;
    private static final int MAX_AGE = 2000;
    private static final int ORIGINAL_ALPHA = 40;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double FACE_OFFSET = 0.001;

    /** Renderer-owned like V33a's WorldLocation map; weak keys prevent removed tables leaking forever. */
    private final Map<TileEntityCastingTable, Animation> animations = new WeakHashMap<>();

    public RenderCastingTable(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileEntityCastingTable table, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPosition, breakProgress);
        state.runes.clear();
        if (table.getLevel() == null) return;

        // NOT table.getBlocks(): that builds the tier's FilledBlockArray from the NBT datapack
        // template, which needs a ServerLevel and hard-crashed the render thread. V33a only used the
        // array to enumerate candidate coordinates and then tested the world block anyway.
        List<BlockPos> candidates = table.getEngravableBlocks();
        if (candidates.isEmpty()) {
            animations.remove(table);
            return;
        }

        Animation animation = animations.computeIfAbsent(table, unused -> new Animation());
        animation.advance(table, candidates);
        BlockPos origin = table.getBlockPos();
        for (Map.Entry<BlockPos, Rune> entry : animation.runes.entrySet()) {
            BlockPos relative = entry.getKey().subtract(origin);
            state.runes.add(new RenderedRune(relative.getX(), relative.getY(), relative.getZ(), entry.getValue().element));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (RenderedRune rune : state.runes) {
            Identifier texture = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
                    "textures/block/runes/engraved/tile" + rune.element.ordinal() + "_0.png");
            PoseStack runePose = copy(poseStack);
            runePose.translate(rune.x, rune.y, rune.z);
            collector.submitCustomGeometry(runePose, RenderTypes.entityTranslucentEmissive(texture),
                    (pose, vertices) -> emitRuneCube(pose, vertices));
        }
    }

    private static void emitRuneCube(PoseStack.Pose pose, VertexConsumer vertices) {
        int color = (ORIGINAL_ALPHA << 24) | 0xFFFFFF;
        // V33a intentionally submitted the same six faces four times, strengthening the faint overlay.
        for (int pass = 0; pass < 4; pass++) {
            face(vertices, pose, 0, 0, 1 + FACE_OFFSET, 1, 0, 1 + FACE_OFFSET, 1, 1, 1 + FACE_OFFSET, 0, 1, 1 + FACE_OFFSET, 0, 0, 1, color);
            face(vertices, pose, 0, 1, -FACE_OFFSET, 1, 1, -FACE_OFFSET, 1, 0, -FACE_OFFSET, 0, 0, -FACE_OFFSET, 0, 0, -1, color);
            face(vertices, pose, -FACE_OFFSET, 0, 0, -FACE_OFFSET, 0, 1, -FACE_OFFSET, 1, 1, -FACE_OFFSET, 1, 0, -1, 0, 0, color);
            face(vertices, pose, 1 + FACE_OFFSET, 1, 0, 1 + FACE_OFFSET, 1, 1, 1 + FACE_OFFSET, 0, 1, 1 + FACE_OFFSET, 0, 0, 1, 0, 0, color);
            face(vertices, pose, 0, 1 + FACE_OFFSET, 1, 1, 1 + FACE_OFFSET, 1, 1, 1 + FACE_OFFSET, 0, 0, 1 + FACE_OFFSET, 0, 0, 1, 0, color);
            face(vertices, pose, 0, -FACE_OFFSET, 0, 1, -FACE_OFFSET, 0, 1, -FACE_OFFSET, 1, 0, -FACE_OFFSET, 1, 0, -1, 0, color);
        }
    }

    private static void face(VertexConsumer vertices, PoseStack.Pose pose,
            double x0, double y0, double z0, double x1, double y1, double z1,
            double x2, double y2, double z2, double x3, double y3, double z3,
            float nx, float ny, float nz, int color) {
        vertex(vertices, pose, x0, y0, z0, 0, 1, nx, ny, nz, color);
        vertex(vertices, pose, x1, y1, z1, 1, 1, nx, ny, nz, color);
        vertex(vertices, pose, x2, y2, z2, 1, 0, nx, ny, nz, color);
        vertex(vertices, pose, x3, y3, z3, 0, 0, nx, ny, nz, color);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, double x, double y, double z,
            float u, float v, float nx, float ny, float nz, int color) {
        vertices.addVertex(pose, (float)x, (float)y, (float)z).setColor(color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
    }

    private static PoseStack copy(PoseStack source) {
        PoseStack copy = new PoseStack();
        copy.last().set(source.last());
        return copy;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    private static final class Animation {
        private final Map<BlockPos, Rune> runes = new LinkedHashMap<>();
        private long lastSpawnTick = Long.MIN_VALUE;

        private void advance(TileEntityCastingTable table, List<BlockPos> engravable) {
            Iterator<Rune> iterator = runes.values().iterator();
            while (iterator.hasNext()) {
                Rune rune = iterator.next();
                if (++rune.age > MAX_AGE) iterator.remove();
            }

            long tick = table.getLevel().getGameTime();
            if (tick == lastSpawnTick || tick % SPAWN_INTERVAL != 0) return;
            lastSpawnTick = tick;

            List<BlockPos> candidates = new ArrayList<>();
            for (BlockPos pos : engravable) {
                BlockState blockState = table.getLevel().getBlockState(pos);
                if (blockState.getBlock() instanceof BlockCrystallineStone stone
                        && stone.getStoneType().ordinal() <= BlockCrystallineStone.StoneTypes.COLUMN.ordinal())
                    candidates.add(pos.immutable());
            }
            if (!candidates.isEmpty()) {
                BlockPos selected = candidates.get(table.getLevel().getRandom().nextInt(candidates.size()));
                runes.put(selected, new Rune(CrystalElement.randomElement()));
            }
        }
    }

    private static final class Rune {
        private final CrystalElement element;
        private int age;
        private Rune(CrystalElement element) { this.element = element; }
    }

    private record RenderedRune(int x, int y, int z, CrystalElement element) {}

    public static final class State extends BlockEntityRenderState {
        private final List<RenderedRune> runes = new ArrayList<>();
    }
}