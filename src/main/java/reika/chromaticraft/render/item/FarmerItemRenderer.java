package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelFarmer;
import reika.chromaticraft.render.tesr.RenderFarmer;

/** The V33a Farmer Techne model in inventory, hand, frame, and dropped-item contexts. */
public final class FarmerItemRenderer implements NoDataSpecialModelRenderer {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "farmer");
    private final ModelFarmer model;

    private FarmerItemRenderer(ModelFarmer model) {
        this.model = model;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int lightCoords,
            int overlayCoords, boolean hasFoil, int outlineColor) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(0.85F, 0.85F, 0.85F);
        pose.translate(-0.5, -0.5, -0.5);
        RenderFarmer.submitMachine(pose, collector, model, lightCoords);
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0));
        output.accept(new Vector3f(1));
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public FarmerItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new FarmerItemRenderer(new ModelFarmer(
                    context.entityModelSet().bakeLayer(RenderFarmer.MODEL_LAYER)));
        }
    }
}
