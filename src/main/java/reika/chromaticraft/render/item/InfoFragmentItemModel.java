package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.gui.LexiconIconResolver;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.ResearchFragmentData;

/**
 * V33a decoded-fragment inventory rendering.
 *
 * <p>The ordinary fragment sprite remains the base model. A decoded fragment adds its research
 * page's real tab item at half scale; while Shift is held in a GUI that tab item replaces the paper
 * at full scale. Keeping this as an item model (rather than a global screen hook) also makes JEI and
 * every vanilla container use the behavior consistently.
 */
public final class InfoFragmentItemModel implements ItemModel {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "info_fragment");
	private static final NestedItemRenderer NESTED = new NestedItemRenderer();

	private final ItemModel base;
	private final Matrix4fc transformation;

	private InfoFragmentItemModel(ItemModel base, Matrix4fc transformation) {
		this.base = base;
		this.transformation = new Matrix4f(transformation);
	}

	@Override
	public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
			ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		LexiconCatalog.Entry page = ResearchFragmentData.read(item).page();
		boolean gui = displayContext == ItemDisplayContext.GUI;
		boolean shift = gui && shiftDown();
		if (page == null || !gui || !shift || page.id().equals("FRAGMENT"))
			base.update(output, item, resolver, displayContext, level, owner, seed);
		if (page == null || !gui)
			return;

		ItemStack icon = LexiconIconResolver.icon(page);
		if (icon.isEmpty())
			return;
		ItemStackRenderState nested = new ItemStackRenderState();
		resolver.updateForTopItem(nested, icon, ItemDisplayContext.GUI, level, owner, seed + 1);
		ItemStackRenderState.LayerRenderState layer = output.newLayer();
		Matrix4f transform = new Matrix4f(transformation);
		if (!shift) {
			// V33a scales the callback by (s, -s, s), then draws at (16, -16). Both
			// coordinates therefore become positive half-item offsets in model space. The
			// old negative Y translation pushed the icon below the slot, leaving only a
			// clipped corner visible in inventories.
			transform.translate(0.25F, 0.25F, 0.25F).scale(0.5F);
		}
		layer.setLocalTransform(transform);
		layer.setExtents(() -> new Vector3fc[] {new Vector3f(-0.5F), new Vector3f(0.5F)});
		layer.setupSpecialModel(NESTED, nested);
		output.appendModelIdentityElement(page.id());
		// Shift swaps the complete layer set (paper + inset versus full-sized page icon),
		// so it is part of the render identity rather than an untracked keyboard side effect.
		output.appendModelIdentityElement(shift);
	}

	private static boolean shiftDown() {
		var window = Minecraft.getInstance().getWindow();
		return InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT)
				|| InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
	}

	private static final class NestedItemRenderer implements SpecialModelRenderer<ItemStackRenderState> {
		@Override
		public void submit(@Nullable ItemStackRenderState state, PoseStack poseStack,
				SubmitNodeCollector collector, int lightCoords, int overlayCoords,
				boolean hasFoil, int outlineColor) {
			if (state != null)
				state.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
		}

		@Override public void getExtents(Consumer<Vector3fc> output) {
			output.accept(new Vector3f(-0.5F));
			output.accept(new Vector3f(0.5F));
		}
		@Override public @Nullable ItemStackRenderState extractArgument(ItemStack stack) { return null; }
	}

	public record Unbaked(ItemModel.Unbaked base) implements ItemModel.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				ItemModels.CODEC.fieldOf("base").forGetter(Unbaked::base)
		).apply(instance, Unbaked::new));

		@Override public MapCodec<? extends ItemModel.Unbaked> type() { return MAP_CODEC; }
		@Override public void resolveDependencies(ResolvableModel.Resolver resolver) { base.resolveDependencies(resolver); }
		@Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
			return new InfoFragmentItemModel(base.bake(context, transformation), transformation);
		}
	}
}
