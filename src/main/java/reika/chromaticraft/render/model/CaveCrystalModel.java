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
            // Geometry lives in CaveCrystalGeometry so the inventory item renderer draws exactly the
            // same spikes; here each emitted quad is baked into the chunk mesh.
            CaveCrystalGeometry.emit((points, normal) -> {
                QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
                vertex.setSprite(material, Transparency.TRANSLUCENT);
                vertex.setTintIndex(0);
                vertex.setShade(false);
                vertex.setLightEmission(15);
                vertex.setAmbientOcclusion(false);
                for (CaveCrystalGeometry.Point point : points) {
                    vertex.addVertex(point.position().x, point.position().y, point.position().z)
                            .setColor(0xDCFFFFFF)
                            .setUv(material.sprite().getU(point.u()), material.sprite().getV(point.v()))
                            .setNormal(normal.x, normal.y, normal.z);
                }
                quads.addUnculledFace(vertex.bakeQuad(baker.interner()));
            }, mask, flip, above, below);
            return new SimpleModelWrapper(quads.build(), false, material);
        }

        @Override public void resolveDependencies(Resolver resolver) {}
        @Override public MapCodec<Unbaked> codec() { return CODEC; }
    }

}
