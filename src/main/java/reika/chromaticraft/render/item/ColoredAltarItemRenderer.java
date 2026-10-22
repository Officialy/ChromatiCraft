package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import reika.dragonapi.client.EntityLighting;
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
    private static final int ITEM_TICK_OFFSET=new java.util.Random().nextInt(360);
    private final ModelColoredAltar model; private final CrystalElement color;
    private ColoredAltarItemRenderer(ModelColoredAltar model,CrystalElement color) { this.model=model; this.color=color; }
    public void submit(PoseStack stack,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
        stack.pushPose();itemTransform(stack);RenderColoredAltar.modelTransform(stack);var pose=new PoseStack();pose.last().set(stack.last());
        int[] colors=RenderColoredAltar.crystalColors(color,ITEM_TICK_OFFSET);
        collector.order(0).submitCustomGeometry(stack,EntityLighting.LIGHTMAP_ONLY.alphaModel(RenderColoredAltar.TEXTURE,true,true),
            (p,out)->model.renderMetal(pose,out,light,overlay));
        collector.order(1).submitCustomGeometry(stack,EntityLighting.LIGHTMAP_ONLY.alphaModel(RenderColoredAltar.TEXTURE,false,true),
            (p,out)->model.renderCrystals(pose,out,colors,light,overlay,ModelColoredAltar.ALL_CRYSTALS));stack.popPose();
    }
    /** Rebase the legacy centred item helper into native block-item coordinates, then retain its authored transforms. */
    public static void itemTransform(PoseStack stack) {
        stack.translate(.5,.5,.5);stack.translate(0,-.125,0);stack.translate(-.5,-.0625,-.5);stack.scale(1.25F,1.25F,1.25F);stack.translate(0,-.1,0);
    }
    public void getExtents(Consumer<Vector3fc> output) {var stack=new PoseStack();itemTransform(stack);RenderColoredAltar.modelTransform(stack);model.getExtents(stack,output);}
    public record Unbaked(int color) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC=Codec.intRange(0,15).fieldOf("color").xmap(Unbaked::new,Unbaked::color);
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
        public ColoredAltarItemRenderer bake(SpecialModelRenderer.BakingContext context) { return new ColoredAltarItemRenderer(new ModelColoredAltar(context.entityModelSet().bakeLayer(RenderColoredAltar.MODEL_LAYER)),CrystalElement.elements[color]); }
    }
}
