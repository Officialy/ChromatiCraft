package reika.chromaticraft.render.item;

import java.util.Locale;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.tesr.RenderDimensionCore;

/** V33a inventory presentation of one coloured Dimension Core. */
public final class DimensionCoreItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"dimension_core");
	private final CrystalElement element;

	private DimensionCoreItemRenderer(CrystalElement element) {
		this.element = element;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(45));
		// V33a's inventory transform applies -30 degrees before the old renderer's inverted entity
		// model coordinate system. In the modern item pose that inversion is absent, so the equivalent
		// visible tilt is +30 degrees.
		poseStack.mulPose(Axis.XP.rotationDegrees(30));
		RenderDimensionCore.submitLayers(poseStack, collector, element.getColor(),
				(int)(System.currentTimeMillis() / 250 % 80), false);
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(-0.5F, -0.5F, -0.5F));
		output.accept(new Vector3f(1.5F, 1.5F, 1.5F));
	}

	public record Unbaked(CrystalElement element) implements NoDataSpecialModelRenderer.Unbaked {
		private static final com.mojang.serialization.Codec<CrystalElement> ELEMENT_CODEC =
				com.mojang.serialization.Codec.STRING.xmap(
						name -> CrystalElement.valueOf(name.toUpperCase(Locale.ROOT)),
						value -> value.name().toLowerCase(Locale.ROOT));
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
				instance.group(ELEMENT_CODEC.fieldOf("element").forGetter(Unbaked::element))
						.apply(instance, Unbaked::new));

		@Override
		public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public DimensionCoreItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new DimensionCoreItemRenderer(element);
		}
	}
}
