package reika.chromaticraft.render.model;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import reika.chromaticraft.block.worldgen26.BlockDecoFlower;

import java.util.List;

/** Aura Ivy's existing sprite and grass tint on double-sided wall planes, including hanging runs. */
public final class AuraIvyModel implements DynamicBlockStateModel {
    private final BlockStateModelPart[] faces;
    private final Material.Baked material;
    private final int flags;

    private AuraIvyModel(BlockStateModelPart[] faces, Material.Baked material) {
        this.faces = faces;
        this.material = material;
        int flags = 0;
        for (var face : faces) flags |= face.materialFlags();
        this.flags = flags;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            List<BlockStateModelPart> parts) {
        int mask = BlockDecoFlower.ivyFaces(level, pos);
        for (int i = 0; i < faces.length; i++)
            if ((mask & (1 << i)) != 0) parts.add(faces[i]);
    }

    @Override public Material.Baked particleMaterial() { return material; }
    @Override public int materialFlags() { return flags; }

    public record Unbaked(Identifier texture) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = Identifier.CODEC.fieldOf("texture")
                .xmap(Unbaked::new, Unbaked::texture);

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            Material.Baked material = baker.materials().get(new Material(texture), () -> "chromaticraft:aura_ivy");
            BlockStateModelPart[] faces = new BlockStateModelPart[4];
            float inset = 1 / 32F;
            float[][] ends = {{0, inset, 1, inset}, {1 - inset, 0, 1 - inset, 1},
                    {1, 1 - inset, 0, 1 - inset}, {inset, 1, inset, 0}};
            for (int i = 0; i < ends.length; i++) {
                float[] end = ends[i];
                var quads = new QuadCollection.Builder();
                for (boolean reverse : new boolean[] {false, true}) {
                    var quad = new MutableQuad();
                    quad.setSprite(material);
                    float ax = end[reverse ? 2 : 0], az = end[reverse ? 3 : 1];
                    float bx = end[reverse ? 0 : 2], bz = end[reverse ? 1 : 3];
                    quad.setPosition(0, ax, 1, az);
                    quad.setPosition(1, ax, 0, az);
                    quad.setPosition(2, bx, 0, bz);
                    quad.setPosition(3, bx, 1, bz);
                    float u = reverse ? 1 : 0;
                    quad.setUvFromSprite(0, u, 0);
                    quad.setUvFromSprite(1, u, 1);
                    quad.setUvFromSprite(2, 1 - u, 1);
                    quad.setUvFromSprite(3, 1 - u, 0);
                    quad.setColor(-1);
                    quad.setTintIndex(0);
                    quad.recomputeNormals(true);
                    quads.addUnculledFace(quad.toBakedQuad());
                }
                faces[i] = new SimpleModelWrapper(quads.build(), false, material);
            }
            return new AuraIvyModel(faces, material);
        }

        @Override public void resolveDependencies(Resolver resolver) {}
        @Override public MapCodec<Unbaked> codec() { return CODEC; }
    }
}
