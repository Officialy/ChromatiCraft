package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.tesr.ChromaFlowerRenderer;

import java.util.function.Consumer;

/**
 * V33a drew the Chromatic Flower item with its TESR on a render-only tile whose hue kept advancing. The same flower is
 * drawn here with the hue turning one degree per tick of the clock, the layers a fixed 20 degrees apart (the middle of
 * the placed flower's random 10-30).
 */
public final class ChromaFlowerItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "chroma_flower");

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		int hue = (int)(System.currentTimeMillis()/50%360);
		int[] hues = {hue, (hue+20)%360, (hue+340)%360};
		ChromaFlowerRenderer.submitFlower(poseStack, collector, hues, lightCoords,
				RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS), true);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0, 0, 0));
		output.accept(new Vector3f(1, 1, 1));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public ChromaFlowerItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new ChromaFlowerItemRenderer();
		}
	}
}
