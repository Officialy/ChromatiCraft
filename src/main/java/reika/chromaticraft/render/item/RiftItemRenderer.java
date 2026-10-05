package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.tesr.RenderRift;
import reika.chromaticraft.tileentity.transport.TileEntityRift;

import java.util.function.Consumer;

/**
 * V33a drew the World Rift item with its TESR on a render-only tile, which took the out-of-world branch: the black box
 * and a 0.025-block halo in a hue cycling every fifteen seconds (V33a registered no item icon).
 */
public final class RiftItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "world_rift");

	private RiftItemRenderer() {

	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		RenderRift.submitBlackBox(poseStack, collector);
		RenderRift.submitAura(poseStack, collector, TileEntityRift.itemColor(), 0.025F);
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
		public RiftItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new RiftItemRenderer();
		}
	}
}
