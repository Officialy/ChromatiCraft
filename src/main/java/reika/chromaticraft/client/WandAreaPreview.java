package reika.chromaticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import org.joml.Vector3f;
import reika.chromaticraft.auxiliary.render.BlockChangeCache;
import reika.chromaticraft.base.ItemBlockChangingWand;
import reika.chromaticraft.client.render.IrisCompatibility;
import reika.chromaticraft.items.tools.wands.ItemBuilderWand;

import java.util.List;

/** V33a's white area outline and Excavation selection fill, extracted with the native world outline. */
public record WandAreaPreview(List<BlockPos> positions, boolean fill) implements CustomBlockOutlineRenderer {
    private static final BlockChangeCache CACHE = new BlockChangeCache();

    public static void extract(ExtractBlockOutlineRenderStateEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { CACHE.clearCache(); return; }
        var stack = mc.player.getMainHandItem();
        var pos = event.getBlockPos();
        if (stack.getItem() instanceof ItemBlockChangingWand wand) {
            if (!wand.canSpreadOn(mc.level, pos, event.getBlockState()) || event.getBlockState().isAir()) return;
            event.addCustomRenderer(new WandAreaPreview(CACHE.get(mc.level, pos, mc.player, stack), true));
        } else if (stack.getItem() instanceof ItemBuilderWand && mc.hitResult instanceof BlockHitResult hit) {
            event.addCustomRenderer(new WandAreaPreview(List.copyOf(ItemBuilderWand.getCoordinatesFor(
                    mc.level, pos, hit.getDirection(), mc.player)), false));
        } else CACHE.clearCache();
    }

    @Override
    public boolean render(BlockOutlineRenderState outline, SubmitNodeCollector collector,
            PoseStack poseStack, LevelRenderState state) {
        if (IrisCompatibility.shadowPass()) return false;
        var camera = state.cameraRenderState.pos;
        float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vertices) -> {
            for (var pos : positions) {
                float x = (float)(pos.getX()-camera.x), y = (float)(pos.getY()-camera.y), z = (float)(pos.getZ()-camera.z);
                float o = fill ? 0.0125F : 0;
                for (int axis = 0; axis < 3; axis++)
                    for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
                        Vector3f p = new Vector3f(x-o,y-o,z-o), q;
                        p.setComponent((axis+1)%3, p.get((axis+1)%3)+a*(1+2*o));
                        p.setComponent((axis+2)%3, p.get((axis+2)%3)+b*(1+2*o));
                        q = new Vector3f(p); q.setComponent(axis, q.get(axis)+1+2*o);
                        Vector3f normal = new Vector3f(q).sub(p).normalize();
                        vertices.addVertex(pose,p.x,p.y,p.z).setColor(-1).setNormal(pose,normal.x,normal.y,normal.z).setLineWidth(width);
                        vertices.addVertex(pose,q.x,q.y,q.z).setColor(-1).setNormal(pose,normal.x,normal.y,normal.z).setLineWidth(width);
                    }
            }
        });
        if (fill) collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, vertices) -> {
            for (var pos : positions) {
                float x=(float)(pos.getX()-camera.x)-0.0125F, y=(float)(pos.getY()-camera.y)-0.0125F,
                        z=(float)(pos.getZ()-camera.z)-0.0125F, s=1.025F;
                float[][] corners={{x,y,z},{x+s,y,z},{x+s,y+s,z},{x,y+s,z},
                        {x,y,z+s},{x+s,y,z+s},{x+s,y+s,z+s},{x,y+s,z+s}};
                for (int[] face : new int[][]{{0,3,2,1},{4,5,6,7},{0,1,5,4},{3,7,6,2},{0,4,7,3},{1,2,6,5}})
                    for (int corner : face) {
                        float[] p=corners[corner];
                        vertices.addVertex(pose,p[0],p[1],p[2]).setColor(255,255,255,64);
                    }
            }
        });
        // Keep the target's own vanilla/model outline in addition to the source area overlay.
        return false;
    }
}

