package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.render.tesr.RenderVoidMonsterTrap;

import java.util.function.Consumer;

public final class VoidMonsterTrapItemRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("chromaticraft","void_monster_trap");
    public void submit(PoseStack stack,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
        var state=new RenderVoidMonsterTrap.State(); state.preview=true; state.millis=System.currentTimeMillis(); state.time=state.millis/50D;
        RenderVoidMonsterTrap.drawInner(state,stack,collector,null);
    }
    public void getExtents(Consumer<Vector3fc> output) { output.accept(new Vector3f(0,0,0)); output.accept(new Vector3f(1,1,1)); }
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC=MapCodec.unit(new Unbaked());
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
        public VoidMonsterTrapItemRenderer bake(SpecialModelRenderer.BakingContext context) { return new VoidMonsterTrapItemRenderer(); }
    }
}
