package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.models.ModelColoredAltar;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.tesr.RenderColoredAltar;

import java.util.function.Consumer;

public final class ColoredAltarItemRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("chromaticraft","colored_altar");
    private final ModelColoredAltar model; private final CrystalElement color;
    private ColoredAltarItemRenderer(ModelColoredAltar model,CrystalElement color) { this.model=model; this.color=color; }
    public void submit(PoseStack stack,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
        stack.pushPose(); stack.translate(.5,1.5,.5); stack.scale(1,-1,-1); var pose=new PoseStack(); pose.last().set(stack.last());
        int[] colors=RenderColoredAltar.crystalColors(color,System.currentTimeMillis()/50D);
        collector.submitCustomGeometry(stack,RenderTypes.entitySolid(RenderColoredAltar.TEXTURE),(p,out)->model.renderMetal(pose,out,light,overlay));
        collector.submitCustomGeometry(stack,RenderTypes.entityTranslucent(RenderColoredAltar.TEXTURE),(p,out)->model.renderCrystals(pose,out,colors,overlay)); stack.popPose();
    }
    public void getExtents(Consumer<Vector3fc> output) { output.accept(new Vector3f(0,0,0)); output.accept(new Vector3f(1,1,1)); }
    public record Unbaked(int color) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC=Codec.intRange(0,15).fieldOf("color").xmap(Unbaked::new,Unbaked::color);
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
        public ColoredAltarItemRenderer bake(SpecialModelRenderer.BakingContext context) { return new ColoredAltarItemRenderer(new ModelColoredAltar(context.entityModelSet().bakeLayer(RenderColoredAltar.MODEL_LAYER)),CrystalElement.elements[color]); }
    }
}
