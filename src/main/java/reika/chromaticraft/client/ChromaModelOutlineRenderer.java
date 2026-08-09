package reika.chromaticraft.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.render.tesr.RenderItemStand;

/** Exact hover/mining outlines extracted from ChromatiCraft's non-voxel rendered models. */
public final class ChromaModelOutlineRenderer implements CustomBlockOutlineRenderer {

    private static final int NORMAL_COLOR = 0x66000000;
    private static final int HIGH_CONTRAST_COLOR = -11010079;
    private static final ModelPart ITEM_STAND_MODEL = RenderItemStand.createStandLayer().bakeRoot();

    private final BlockPos pos;
    private final List<Line> lines;

    private ChromaModelOutlineRenderer(BlockPos pos, List<Line> lines) {
        this.pos = pos;
        this.lines = lines;
    }

    public static void extract(ExtractBlockOutlineRenderStateEvent event) {
        List<Line> lines;
        if (event.getBlockState().getBlock() instanceof CrystalBlock) {
            // Cave, potion/super and lamp crystals all use the same Java-authored spike mesh.
            // Extracting the baked model keeps the outline exact for each family's base/arm rules.
            lines = modelLines(event);
        } else if (event.getBlockState().is(ChromaBlocks.ITEM_STAND.get())) {
            lines = itemStandLines();
        } else {
            return;
        }
        if (!lines.isEmpty())
            event.addCustomRenderer(new ChromaModelOutlineRenderer(event.getBlockPos(), lines));
    }

    /** Reusable Java/baked-model-to-outline bridge for non-voxel ChromatiCraft models. */
    private static List<Line> modelLines(ExtractBlockOutlineRenderStateEvent event) {
        BlockStateModel model = Minecraft.getInstance().getModelManager()
                .getBlockStateModelSet().get(event.getBlockState());
        List<BlockStateModelPart> parts = new ArrayList<>(1);
        model.collectParts(event.getLevel(), event.getBlockPos(), event.getBlockState(),
                RandomSource.create(event.getBlockState().getSeed(event.getBlockPos())), parts);

        LineCollector collector = new LineCollector();
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) {
                for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++)
                    collector.add(quad.position(vertex), quad.position((vertex + 1) % BakedQuad.VERTEX_COUNT));
            }
        }
        return collector.finish();
    }

    private static List<Line> itemStandLines() {
        PoseStack modelPose = new PoseStack();
        modelPose.translate(0.5F, 1.5F, 0.5F);
        modelPose.scale(1F, -1F, -1F);
        LineCollector collector = new LineCollector();
        ITEM_STAND_MODEL.visit(modelPose, (pose, path, cubeIndex, cube) -> {
            for (ModelPart.Polygon polygon : cube.polygons) {
                ModelPart.Vertex[] vertices = polygon.vertices();
                for (int vertex = 0; vertex < vertices.length; vertex++) {
                    collector.add(transform(pose, vertices[vertex]),
                            transform(pose, vertices[(vertex + 1) % vertices.length]));
                }
            }
        });
        return collector.finish();
    }

    private static Vector3f transform(PoseStack.Pose pose, ModelPart.Vertex vertex) {
        return pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
    }

    @Override
    public boolean render(BlockOutlineRenderState renderState, SubmitNodeCollector collector,
            PoseStack poseStack, LevelRenderState levelRenderState) {
        float normalWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        if (renderState.highContrast())
            submit(collector, poseStack, levelRenderState, RenderTypes.secondaryBlockOutline(),
                    0xFF000000, 7F);
        // Match vanilla exactly: its translucent black and the window-selected normal line width.
        int mainColor = renderState.highContrast() ? HIGH_CONTRAST_COLOR : NORMAL_COLOR;
        submit(collector, poseStack, levelRenderState, RenderTypes.lines(), mainColor, normalWidth);
        return true;
    }

    private void submit(SubmitNodeCollector collector, PoseStack poseStack,
            LevelRenderState levelRenderState, RenderType renderType, int color, float width) {
        var camera = levelRenderState.cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        collector.submitCustomGeometry(poseStack, renderType, (pose, vertices) -> {
            Vector3f normal = new Vector3f();
            for (Line line : lines) {
                normal.set(line.to).sub(line.from).normalize();
                vertices.addVertex(pose, line.from.x, line.from.y, line.from.z)
                        .setColor(color).setNormal(pose, normal).setLineWidth(width);
                vertices.addVertex(pose, line.to.x, line.to.y, line.to.z)
                        .setColor(color).setNormal(pose, normal).setLineWidth(width);
            }
        });
        poseStack.popPose();
    }

    private static final class LineCollector {
        private final List<Line> lines = new ArrayList<>();
        private final Set<EdgeKey> edges = new HashSet<>();

        void add(Vector3fc from, Vector3fc to) {
            if (from.distanceSquared(to) <= 1.0E-10F) return;
            EdgeKey key = EdgeKey.of(from, to);
            if (edges.add(key)) lines.add(new Line(new Vector3f(from), new Vector3f(to)));
        }

        List<Line> finish() {
            return List.copyOf(lines);
        }
    }

    private record Line(Vector3f from, Vector3f to) {}

    private record PointKey(int x, int y, int z) implements Comparable<PointKey> {
        static PointKey of(Vector3fc point) {
            return new PointKey(Float.floatToIntBits(point.x()), Float.floatToIntBits(point.y()),
                    Float.floatToIntBits(point.z()));
        }

        @Override
        public int compareTo(PointKey other) {
            int compare = Integer.compareUnsigned(x, other.x);
            if (compare == 0) compare = Integer.compareUnsigned(y, other.y);
            if (compare == 0) compare = Integer.compareUnsigned(z, other.z);
            return compare;
        }
    }

    private record EdgeKey(PointKey first, PointKey second) {
        static EdgeKey of(Vector3fc from, Vector3fc to) {
            PointKey a = PointKey.of(from);
            PointKey b = PointKey.of(to);
            return a.compareTo(b) <= 0 ? new EdgeKey(a, b) : new EdgeKey(b, a);
        }
    }
}
