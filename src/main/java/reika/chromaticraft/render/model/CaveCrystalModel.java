package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import org.joml.Vector3f;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.registry.ChromaBlocks;

/** Exact 26.2 chunk-mesh port of V33a {@code CrystalRenderer}'s cave-crystal geometry. */
public final class CaveCrystalModel implements DynamicBlockStateModel {

    private static final int ALPHA = 220;
    private static final int TEXTURE_SIZE = 16;

    private final BlockStateModelPart[][][][] variants;
    private final Material.Baked particle;
    private final int flags;

    private CaveCrystalModel(BlockStateModelPart[][][][] variants, Material.Baked particle) {
        this.variants = variants;
        this.particle = particle;
        int materialFlags = 0;
        for (BlockStateModelPart[][][] flipped : variants)
            for (BlockStateModelPart[][] above : flipped)
                for (BlockStateModelPart[] below : above)
                    for (BlockStateModelPart part : below)
                        materialFlags |= part.materialFlags();
        flags = materialFlags | BakedQuad.FLAG_TRANSLUCENT;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, List<BlockStateModelPart> parts) {
        int arms = BlockCaveCrystal.armMask(pos);
        BlockState belowState = level.getBlockState(pos.below());
        BlockState aboveState = level.getBlockState(pos.above());
        boolean flip = BlockCaveCrystal.isCeilingMounted(level, pos);
        boolean above = ChromaBlocks.isCaveCrystal(aboveState);
        boolean below = belowState.getBlock() instanceof CrystalBlock;
        parts.add(variants[flip ? 1 : 0][above ? 1 : 0][below ? 1 : 0][arms]);
    }

    @Override public Material.Baked particleMaterial() { return particle; }
    @Override public int materialFlags() { return flags; }

    public record Unbaked(Identifier texture) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = Identifier.CODEC.fieldOf("texture")
                .xmap(Unbaked::new, Unbaked::texture);

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            // V33a rendered this in pass 1 with alpha 220. The outline texture itself is opaque,
            // so relying on sprite transparency silently puts it on the solid chunk layer.
            Material source = new Material(texture, true);
            Material.Baked material = baker.materials().get(source,
                    () -> "chromaticraft:cave_crystal/" + texture);
            BlockStateModelPart[][][][] variants = new BlockStateModelPart[2][2][2][16];
            for (int flip = 0; flip < 2; flip++)
                for (int above = 0; above < 2; above++)
                    for (int below = 0; below < 2; below++)
                        for (int mask = 0; mask < 16; mask++)
                            variants[flip][above][below][mask] = bakeVariant(
                                    baker, material, mask, flip != 0, above != 0, below != 0);
            return new CaveCrystalModel(variants, material);
        }

        private static BlockStateModelPart bakeVariant(ModelBaker baker, Material.Baked material,
                int mask, boolean flip, boolean above, boolean below) {
            QuadCollection.Builder quads = new QuadCollection.Builder();
            centralSpike(baker, quads, material, above, flip);
            if ((mask & 8) != 0) xSpike(baker, quads, material, .1875F, below, flip);
            if ((mask & 4) != 0) xSpike(baker, quads, material, -.1875F, below, flip);
            if ((mask & 2) != 0) zSpike(baker, quads, material, .1875F, below, flip);
            if ((mask & 1) != 0) zSpike(baker, quads, material, -.1875F, below, flip);
            return new SimpleModelWrapper(quads.build(), false, material);
        }

        private static void centralSpike(ModelBaker baker, QuadCollection.Builder quads,
                Material.Baked material, boolean above, boolean flip) {
            float core = .15F;
            float height = above ? 1F : .8F;
            if (!above) {
                pointQuad(baker, quads, material, flip, false,
                        p(.5F-core,height,.5F-core,0,0), p(.5F-core,height,.5F+core,1,0),
                        p(.5F,1,.5F,1,1), p(.5F,1,.5F,1,1));
                pointQuad(baker, quads, material, flip, false,
                        p(.5F-core,height,.5F+core,0,0), p(.5F+core,height,.5F+core,1,0),
                        p(.5F,1,.5F,1,1), p(.5F,1,.5F,1,1));
                pointQuad(baker, quads, material, flip, false,
                        p(.5F,1,.5F,0,1), p(.5F,1,.5F,1,1),
                        p(.5F+core,height,.5F+core,1,0), p(.5F+core,height,.5F-core,0,0));
                pointQuad(baker, quads, material, flip, false,
                        p(.5F,1,.5F,0,1), p(.5F,1,.5F,1,1),
                        p(.5F+core,height,.5F-core,1,0), p(.5F-core,height,.5F-core,0,0));
            }

            BodyUv uv = bodyUv();
            pointQuad(baker, quads, material, flip, false,
                    p(.5F-core,height,.5F-core,uv.u0,uv.v1), p(.5F+core,height,.5F-core,uv.u1,uv.v1),
                    p(.5F+core,0,.5F-core,uv.u1,uv.v0), p(.5F-core,0,.5F-core,uv.u0,uv.v0));
            pointQuad(baker, quads, material, flip, false,
                    p(.5F-core,0,.5F+core,uv.u0,uv.v0), p(.5F+core,0,.5F+core,uv.u1,uv.v0),
                    p(.5F+core,height,.5F+core,uv.u1,uv.v1), p(.5F-core,height,.5F+core,uv.u0,uv.v1));
            pointQuad(baker, quads, material, flip, false,
                    p(.5F+core,height,.5F-core,uv.u0,uv.v1), p(.5F+core,height,.5F+core,uv.u1,uv.v1),
                    p(.5F+core,0,.5F+core,uv.u1,uv.v0), p(.5F+core,0,.5F-core,uv.u0,uv.v0));
            pointQuad(baker, quads, material, flip, false,
                    p(.5F-core,0,.5F-core,uv.u0,uv.v0), p(.5F-core,0,.5F+core,uv.u1,uv.v0),
                    p(.5F-core,height,.5F+core,uv.u1,uv.v1), p(.5F-core,height,.5F-core,uv.u0,uv.v1));

        }

        private static void xSpike(ModelBaker baker, QuadCollection.Builder quads,
                Material.Baked material, float out, boolean below, boolean flip) {
            float core = .12F;
            float height = .55F;
            float rise = height/6F;
            float y = below ? .15F : -.05F;
            float tipHeight = .1F;
            int direction = out > 0 ? 1 : -1;
            boolean reverse = out > 0;
            float near = .5F + core*direction + out;
            float far = .5F + core*direction*3 + out;
            float tip = .5F + core*direction + out*2;

            pointQuad(baker, quads, material, flip, reverse,
                    p(near,y+height+rise,.5F+core,0,0), p(near,y+height+rise,.5F-core,1,0),
                    p(tip,y+height+rise+tipHeight,.5F,1,1), p(tip,y+height+rise+tipHeight,.5F,1,1));
            pointQuad(baker, quads, material, flip, reverse,
                    p(tip,y+height+rise+tipHeight,.5F,0,1), p(tip,y+height+rise+tipHeight,.5F,1,1),
                    p(far,y+height,.5F-core,1,0), p(far,y+height,.5F+core,0,0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(tip,y+height+rise+tipHeight,.5F,0,1), p(tip,y+height+rise+tipHeight,.5F,1,1),
                    p(far,y+height,.5F+core,1,0), p(near,y+height+rise,.5F+core,0,0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(near,y+height+rise,.5F-core,0,0), p(far,y+height,.5F-core,1,0),
                    p(tip,y+height+rise+tipHeight,.5F,1,1), p(tip,y+height+rise+tipHeight,.5F,1,1));

            BodyUv uv = bodyUv();
            float farScale = 3F;
            float baseScale = below ? 1F : farScale;
            float inset = below ? Math.abs(core*direction) : 0;
            float nearRise = below ? rise*4 : rise;
            float topRise = rise;
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F+core*direction,y+nearRise,.5F-core,uv.u0,uv.v0),
                    p(.5F+core*direction*baseScale,y-inset,.5F-core,uv.u1,uv.v0),
                    p(.5F+core*direction*farScale+out,y+height,.5F-core,uv.u1,uv.v1),
                    p(.5F+core*direction+out,y+height+topRise,.5F-core,uv.u0,uv.v1));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F+core*direction+out,y+height+topRise,.5F+core,uv.u0,uv.v1),
                    p(.5F+core*direction*farScale+out,y+height,.5F+core,uv.u1,uv.v1),
                    p(.5F+core*direction*baseScale,y-inset,.5F+core,uv.u1,uv.v0),
                    p(.5F+core*direction,y+nearRise,.5F+core,uv.u0,uv.v0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F+core*direction+out,y+height+topRise,.5F-core,uv.u0,uv.v1),
                    p(.5F+core*direction+out,y+height+topRise,.5F+core,uv.u1,uv.v1),
                    p(.5F+core*direction,y+nearRise,.5F+core,uv.u1,uv.v0),
                    p(.5F+core*direction,y+nearRise,.5F-core,uv.u0,uv.v0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F+core*direction*baseScale,y-inset,.5F-core,uv.u0,uv.v0),
                    p(.5F+core*direction*baseScale,y-inset,.5F+core,uv.u1,uv.v0),
                    p(.5F+core*direction*farScale+out,y+height,.5F+core,uv.u1,uv.v1),
                    p(.5F+core*direction*farScale+out,y+height,.5F-core,uv.u0,uv.v1));
            if (!below) {
                pointQuad(baker, quads, material, flip, reverse,
                        p(.5F-core*direction*farScale+out*2.56F,y+rise,.5F-core,uv.u0,uv.v1),
                        p(.5F-core*direction*farScale+out*2.56F,y+rise,.5F+core,uv.u1,uv.v1),
                        p(.5F+core*direction*farScale,y,.5F+core,uv.u1,uv.v0),
                        p(.5F+core*direction*farScale,y,.5F-core,uv.u0,uv.v0));
            }
        }

        private static void zSpike(ModelBaker baker, QuadCollection.Builder quads,
                Material.Baked material, float out, boolean below, boolean flip) {
            float core = .12F;
            float height = .55F;
            float rise = height/6F;
            float y = below ? .1F : -.1F;
            float tipHeight = .1F;
            int direction = out > 0 ? 1 : -1;
            boolean reverse = out < 0;
            float near = .5F + core*direction + out;
            float far = .5F + core*direction*3 + out;
            float tip = .5F + core*direction + out*2;

            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F+core,y+height+rise,near,0,0), p(.5F-core,y+height+rise,near,1,0),
                    p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,0,1));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,1,1),
                    p(.5F-core,y+height,far,1,0), p(.5F+core,y+height,far,0,0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,1,1),
                    p(.5F+core,y+height,far,1,0), p(.5F+core,y+height+rise,near,0,0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F-core,y+height+rise,near,0,0), p(.5F-core,y+height,far,1,0),
                    p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,0,1));

            BodyUv uv = bodyUv();
            float farScale = 3F;
            float baseScale = below ? 1F : farScale;
            float inset = below ? Math.abs(core*direction) : 0;
            float nearRise = below ? rise*4 : rise;
            float topRise = rise;
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F-core,y+nearRise,.5F+core*direction,uv.u0,uv.v0),
                    p(.5F-core,y-inset,.5F+core*direction*baseScale,uv.u1,uv.v0),
                    p(.5F-core,y+height,.5F+core*direction*farScale+out,uv.u1,uv.v1),
                    p(.5F-core,y+height+topRise,.5F+core*direction+out,uv.u0,uv.v1));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F+core,y+height+topRise,.5F+core*direction+out,uv.u0,uv.v1),
                    p(.5F+core,y+height,.5F+core*direction*farScale+out,uv.u1,uv.v1),
                    p(.5F+core,y-inset,.5F+core*direction*baseScale,uv.u1,uv.v0),
                    p(.5F+core,y+nearRise,.5F+core*direction,uv.u0,uv.v0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F-core,y+height+topRise,.5F+core*direction+out,uv.u0,uv.v1),
                    p(.5F+core,y+height+topRise,.5F+core*direction+out,uv.u1,uv.v1),
                    p(.5F+core,y+nearRise,.5F+core*direction,uv.u1,uv.v0),
                    p(.5F-core,y+nearRise,.5F+core*direction,uv.u0,uv.v0));
            pointQuad(baker, quads, material, flip, reverse,
                    p(.5F-core,y-inset,.5F+core*direction*baseScale,uv.u0,uv.v0),
                    p(.5F+core,y-inset,.5F+core*direction*baseScale,uv.u1,uv.v0),
                    p(.5F+core,y+height,.5F+core*direction*farScale+out,uv.u1,uv.v1),
                    p(.5F-core,y+height,.5F+core*direction*farScale+out,uv.u0,uv.v1));
            if (!below) {
                pointQuad(baker, quads, material, flip, reverse,
                        p(.5F-core,y+rise+.0025F,.5F-core*direction*farScale+out*2.56F,uv.u0,uv.v1),
                        p(.5F+core,y+rise+.0025F,.5F-core*direction*farScale+out*2.56F,uv.u1,uv.v1),
                        p(.5F+core,y,.5F+core*direction*farScale,uv.u1,uv.v0),
                        p(.5F-core,y,.5F+core*direction*farScale,uv.u0,uv.v0));
            }
        }

        private static BodyUv bodyUv() {
            float v1 = 1F - 1F/TEXTURE_SIZE;
            float v0 = v1/(TEXTURE_SIZE*1.2F);
            float u0 = 1F/(TEXTURE_SIZE*2F);
            float u1 = 1F - (1F-u0)/(TEXTURE_SIZE*2F);
            return new BodyUv(u0, v0, u1, v1);
        }

        private static Point p(float x, float y, float z, float u, float v) {
            return new Point(new Vector3f(x, y, z), u, v);
        }

        private static void pointQuad(ModelBaker baker, QuadCollection.Builder out,
                Material.Baked material, boolean flip, boolean reverse, Point... source) {
            Point[] points = new Point[4];
            for (int i = 0; i < 4; i++) {
                Point original = source[reverse ? 3-i : i];
                Vector3f position = new Vector3f(original.position);
                if (flip) position.y = 1F-position.y;
                points[i] = new Point(position, original.u, original.v);
            }
            if (flip) {
                Point swap = points[0]; points[0] = points[3]; points[3] = swap;
                swap = points[1]; points[1] = points[2]; points[2] = swap;
            }

            Vector3f normal = normal(points);
            QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
            vertex.setSprite(material, Transparency.TRANSLUCENT);
            vertex.setTintIndex(0);
            vertex.setShade(false);
            vertex.setLightEmission(15);
            vertex.setAmbientOcclusion(false);
            for (Point point : points) {
                vertex.addVertex(point.position.x, point.position.y, point.position.z)
                        .setColor(0xDCFFFFFF)
                        .setUv(material.sprite().getU(point.u), material.sprite().getV(point.v))
                        .setNormal(normal.x, normal.y, normal.z);
            }
            out.addUnculledFace(vertex.bakeQuad(baker.interner()));

            // Legacy immediate-mode quads were visible from either side. The modern terrain
            // pipeline culls backfaces, so bake the opposite winding as well; otherwise the
            // narrow branch-tip faces disappear and look like a rotated/gapped cap.
            QuadBakingVertexConsumer back = new QuadBakingVertexConsumer();
            back.setSprite(material, Transparency.TRANSLUCENT);
            back.setTintIndex(0);
            back.setShade(false);
            back.setLightEmission(15);
            back.setAmbientOcclusion(false);
            for (int i = points.length - 1; i >= 0; i--) {
                Point point = points[i];
                back.addVertex(point.position.x, point.position.y, point.position.z)
                        .setColor(0xDCFFFFFF)
                        .setUv(material.sprite().getU(point.u), material.sprite().getV(point.v))
                        .setNormal(-normal.x, -normal.y, -normal.z);
            }
            out.addUnculledFace(back.bakeQuad(baker.interner()));
        }

        private static Vector3f normal(Point[] points) {
            int[][] triangles = {{0,1,2}, {0,2,3}, {0,1,3}, {1,2,3}};
            for (int[] triangle : triangles) {
                Vector3f a = points[triangle[0]].position;
                Vector3f cross = new Vector3f(points[triangle[1]].position).sub(a)
                        .cross(new Vector3f(points[triangle[2]].position).sub(a));
                if (cross.lengthSquared() > 1.0E-8F) return cross.normalize();
            }
            return new Vector3f(0, 1, 0);
        }

        @Override public void resolveDependencies(Resolver resolver) {}
        @Override public MapCodec<Unbaked> codec() { return CODEC; }
    }

    private record Point(Vector3f position, float u, float v) {}
    private record BodyUv(float u0, float v0, float u1, float v1) {}
}
