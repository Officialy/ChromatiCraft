package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelFocusCrystal;
import reika.chromaticraft.models.ModelFocusCrystalMessy;
import reika.chromaticraft.render.tesr.RenderFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal.CrystalTier;

import java.util.function.Consumer;

/**
 * V33a's focus crystal item: ChromaItemRenderer ran RenderFocusCrystal on a render-only tile carrying the stack's
 * tier, at (0, -0.1, 0). Out of the world only the solid pass drew -- base plate and shards, no bobber or flare --
 * and the shimmer clock was the world time.
 */
public final class FocusCrystalItemRenderer implements SpecialModelRenderer<CrystalTier> {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "focus_crystal");

	private final ModelFocusCrystal ordered;
	private final ModelFocusCrystalMessy unordered;
	/** Stands in for V33a's per-render tile instance, whose identity hash seeded the shimmer. */
	private final Object identity = new Object();

	private FocusCrystalItemRenderer(ModelFocusCrystal ordered, ModelFocusCrystalMessy unordered) {
		this.ordered = ordered;
		this.unordered = unordered;
	}

	@Override
	public void submit(@Nullable CrystalTier tier, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		CrystalTier t = tier != null ? tier : CrystalTier.FLAWED;
		Minecraft mc = Minecraft.getInstance();
		float time = mc.level != null ? mc.level.getGameTime() : 0;
		time += System.identityHashCode(identity) % 512000 + t.ordinal() * 15;
		poseStack.pushPose();
		poseStack.translate(0, -0.1, 0);
		RenderFocusCrystal.submitSolid(poseStack, collector, t.usesOrganizedModel() ? ordered : unordered, t,
				identity, time, lightCoords);
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0, -0.1F, 0));
		output.accept(new Vector3f(1, 0.9F, 1));
	}

	@Override
	public CrystalTier extractArgument(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		int tier = data == null ? 0 : data.copyTag().getIntOr("tier", 0);
		return CrystalTier.VALUES[Mth.clamp(tier, 0, CrystalTier.VALUES.length - 1)];
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<CrystalTier> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends SpecialModelRenderer.Unbaked<CrystalTier>> type() { return MAP_CODEC; }
		@Override public FocusCrystalItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new FocusCrystalItemRenderer(
					new ModelFocusCrystal(context.entityModelSet().bakeLayer(RenderFocusCrystal.ORDERED_LAYER)),
					new ModelFocusCrystalMessy(context.entityModelSet().bakeLayer(RenderFocusCrystal.MESSY_LAYER)));
		}
	}
}
