package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.rendering.connected.ConnectedQuads;

/**
 * 26.2 chunk-mesh port of V33a's cliff-dirt neighbour-conditional face selection, from pristine
 * {@code BlockCliffStone#getIcon(IBlockAccess, x, y, z, side)} (the {@code Variants.DIRT} branch):
 *
 * <pre>
 * if (v == Variants.DIRT) {
 *     if (s == 0) { // bottom face
 *         if (below == air) return STONE.baseTexture;
 *     }
 *     else if (s > 1) { // the four side faces
 *         if (below != this block) return DIRT.blend[0]; // "dirt_blend_down" for every side
 *     }
 * }
 * // side==1 (top) always falls through to the plain dirt_base texture
 * </pre>
 *
 * The shipped V33a class registers {@code dirt_blend_up/down/left/right} but only ever looks up
 * {@code blend[0]} (a leftover from a per-index registration loop that always resolved to
 * {@code "_blend_down"}), so a single blend sprite is used uniformly on whichever side faces are
 * exposed at the bottom of a dirt run — never four direction-specific overlays, and never stacked
 * on top of the base texture. The earlier port used a flat, unconditional {@code dirt_base} cube for
 * every face, which is why the material read as washed out: it lost the darker blend edge V33a draws
 * wherever cliff dirt meets something other than more cliff dirt below it, and the "floating dirt
 * reveals stone underneath" bottom-face swap.
 */
public final class CliffDirtModel implements DynamicBlockStateModel {

    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private final BlockStateModelPart top;
    private final BlockStateModelPart sideNormal;
    private final BlockStateModelPart sideBlend;
    private final BlockStateModelPart bottomNormal;
    private final BlockStateModelPart bottomStone;
    private final Material.Baked particle;
    private final int flags;

    private CliffDirtModel(BlockStateModelPart top, BlockStateModelPart sideNormal, BlockStateModelPart sideBlend,
            BlockStateModelPart bottomNormal, BlockStateModelPart bottomStone, Material.Baked particle) {
        this.top = top;
        this.sideNormal = sideNormal;
        this.sideBlend = sideBlend;
        this.bottomNormal = bottomNormal;
        this.bottomStone = bottomStone;
        this.particle = particle;
        this.flags = top.materialFlags() | sideNormal.materialFlags() | sideBlend.materialFlags()
                | bottomNormal.materialFlags() | bottomStone.materialFlags();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, List<BlockStateModelPart> parts) {
        BlockState below = level.getBlockState(pos.below());
        boolean belowIsCliffDirt = below.is(ChromaBlocks.CLIFF_DIRT.get());
        boolean belowIsAir = below.isAir();

        parts.add(top);
        parts.add(belowIsCliffDirt ? sideNormal : sideBlend);
        parts.add(belowIsAir ? bottomStone : bottomNormal);
    }

    @Override public Material.Baked particleMaterial() { return particle; }
    @Override public int materialFlags() { return flags; }

    public record Unbaked(Identifier dirtBase, Identifier dirtBlend, Identifier stoneBase) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("dirt_base").forGetter(Unbaked::dirtBase),
                Identifier.CODEC.fieldOf("dirt_blend").forGetter(Unbaked::dirtBlend),
                Identifier.CODEC.fieldOf("stone_base").forGetter(Unbaked::stoneBase)
        ).apply(i, Unbaked::new));

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            Material.Baked dirtMat = baker.materials().get(new Material(dirtBase),
                    () -> "chromaticraft:cliff_dirt/" + dirtBase);
            Material.Baked blendMat = baker.materials().get(new Material(dirtBlend),
                    () -> "chromaticraft:cliff_dirt/" + dirtBlend);
            Material.Baked stoneMat = baker.materials().get(new Material(stoneBase),
                    () -> "chromaticraft:cliff_dirt/" + stoneBase);

            BlockStateModelPart top = ConnectedQuads.facePart(baker, Direction.UP, dirtMat, 0);
            BlockStateModelPart sideNormal = sidesPart(baker, dirtMat);
            BlockStateModelPart sideBlend = sidesPart(baker, blendMat);
            BlockStateModelPart bottomNormal = ConnectedQuads.facePart(baker, Direction.DOWN, dirtMat, 0);
            BlockStateModelPart bottomStone = ConnectedQuads.facePart(baker, Direction.DOWN, stoneMat, 0);
            return new CliffDirtModel(top, sideNormal, sideBlend, bottomNormal, bottomStone, dirtMat);
        }

        private static BlockStateModelPart sidesPart(ModelBaker baker, Material.Baked mat) {
            QuadCollection.Builder b = new QuadCollection.Builder();
            for (Direction d : SIDES)
                b.addCulledFace(d, ConnectedQuads.bakeFaceQuad(baker, d, mat, 0));
            return new SimpleModelWrapper(b.build(), true, mat);
        }

        @Override public void resolveDependencies(Resolver resolver) {}
        @Override public MapCodec<Unbaked> codec() { return CODEC; }
    }
}
