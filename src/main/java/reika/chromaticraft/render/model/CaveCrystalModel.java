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

/**
 * Exact 26.2 chunk-mesh port of V33a {@code CrystalRenderer}.
 *
 * <p>Covers every {@code CrystalRenderedBlock}, not just the cave crystal: the spikes are identical
 * across them and the only difference is {@code renderBase()}, which adds the stone plinth for
 * crystal lamps and potion crystals. V33a also derives {@code below} as
 * {@code !renderBase() && blockBelow instanceof CrystalBlock}, so a based crystal never joins
 * downward — reproduced here by forcing {@code below} false whenever a base is drawn.
 */
public final class CaveCrystalModel implements DynamicBlockStateModel {

    private static final int ALPHA = 220;
    private static final int TEXTURE_SIZE = 16;

    private final BlockStateModelPart[][][][] variants;
    private final Material.Baked particle;
    private final int flags;

    private final boolean hasBase;

    private CaveCrystalModel(BlockStateModelPart[][][][] variants, Material.Baked particle, boolean hasBase) {
        this.variants = variants;
        this.particle = particle;
        this.hasBase = hasBase;
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
        // V33a: below = !renderBase() && blockBelow instanceof CrystalBlock -- a based crystal
        // sits on its own plinth and never joins downward.
        boolean below = !hasBase && belowState.getBlock() instanceof CrystalBlock;
        parts.add(variants[flip ? 1 : 0][above ? 1 : 0][below ? 1 : 0][arms]);
    }

    @Override public Material.Baked particleMaterial() { return particle; }
    @Override public int materialFlags() { return flags; }

    /**
     * @param texture     the crystal outline sprite
     * @param baseTexture the plinth sprite for {@code renderBase()} crystals, absent for cave
     *                    crystals. V33a calls {@code getBaseBlock(..., UP)} for the top, bottom
     *                    <em>and</em> side faces, so one sprite covers the whole plinth.
     */
    public record Unbaked(Identifier texture, java.util.Optional<Identifier> baseTexture)
            implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
                        Identifier.CODEC.optionalFieldOf("base_texture").forGetter(Unbaked::baseTexture)
                ).apply(instance, Unbaked::new));

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            // V33a rendered this in pass 1 with alpha 220. The outline texture itself is opaque,
            // so relying on sprite transparency silently puts it on the solid chunk layer.
            Material source = new Material(texture, true);
            Material.Baked material = baker.materials().get(source,
                    () -> "chromaticraft:cave_crystal/" + texture);
            Material.Baked base = baseTexture
                    .map(id -> baker.materials().get(new Material(id, false),
                            () -> "chromaticraft:crystal_base/" + id))
                    .orElse(null);
            BlockStateModelPart[][][][] variants = new BlockStateModelPart[2][2][2][16];
            for (int flip = 0; flip < 2; flip++)
                for (int above = 0; above < 2; above++)
                    for (int below = 0; below < 2; below++)
                        for (int mask = 0; mask < 16; mask++)
                            variants[flip][above][below][mask] = bakeVariant(
                                    baker, material, base, mask, flip != 0, above != 0, below != 0);
            return new CaveCrystalModel(variants, material, base != null);
        }

        private static BlockStateModelPart bakeVariant(ModelBaker baker, Material.Baked material,
                Material.Baked base, int mask, boolean flip, boolean above, boolean below) {
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
            if (base != null) {
                // V33a's plinth shading is flat per-face, baked in rather than lit by the pipeline.
                CaveCrystalGeometry.emitBase((points, normal, shade) -> {
                    QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
                    vertex.setSprite(base, Transparency.NONE);
                    vertex.setTintIndex(-1);
                    vertex.setShade(false);
                    vertex.setAmbientOcclusion(false);
                    int colour = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
                    for (CaveCrystalGeometry.Point point : points) {
                        vertex.addVertex(point.position().x, point.position().y, point.position().z)
                                .setColor(colour)
                                .setUv(base.sprite().getU(point.u()), base.sprite().getV(point.v()))
                                .setNormal(normal.x, normal.y, normal.z);
                    }
                    quads.addUnculledFace(vertex.bakeQuad(baker.interner()));
                }, flip);
            }
            return new SimpleModelWrapper(quads.build(), false, material);
        }

        @Override public void resolveDependencies(Resolver resolver) {}
        @Override public MapCodec<Unbaked> codec() { return CODEC; }
    }

}
