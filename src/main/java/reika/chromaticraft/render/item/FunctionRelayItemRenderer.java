package reika.chromaticraft.render.item;

import java.util.function.Consumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.render.tesr.RenderFunctionRelay;

public final class FunctionRelayItemRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("chromaticraft", "function_relay");

    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
            boolean foil, int outlineColor) {
        RenderFunctionRelay.submitItem(pose, collector);
    }

    @Override public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.7F));
        output.accept(new Vector3f(1.7F));
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
        @Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
        @Override public FunctionRelayItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new FunctionRelayItemRenderer();
        }
    }
}
