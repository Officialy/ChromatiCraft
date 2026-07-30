package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;

/**
 * Submit-pipeline renderer for the V33a casting-stand display item. The stand body remains a normal
 * baked block model; this renderer restores the animated, count-sensitive orbiting item copies and
 * the manipulator-only stack-count billboard.
 */
public final class RenderItemStand implements BlockEntityRenderer<TileEntityItemStand, RenderItemStand.State> {

    private static final Identifier MANIPULATOR = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "manipulator");
	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "item_stand"), "main");
	public static final Identifier STAND_TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/item_stand.png");

    private final ItemModelResolver itemModelResolver;
    private final Font font;
	private final ModelPart standModel;

    public RenderItemStand(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
        font = context.font();
		standModel = context.bakeLayer(MODEL_LAYER);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileEntityItemStand stand, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(stand, state, partialTick, cameraPosition, breakProgress);
        for (int i = 0; i < state.items.length; i++) state.items[i] = null;
        ItemStack stack = stand.getItem(0);
        state.copyCount = copiesFor(stack.getCount());
        state.animationTick = stand.getTicksExisted() + partialTick;
        state.stackCount = stack.getCount();
        state.showCount = !stack.isEmpty() && isHoldingManipulator(Minecraft.getInstance().player);
        if (stack.isEmpty()) return;
        int seed = Long.hashCode(stand.getBlockPos().asLong());
        for (int i = 0; i < state.copyCount; i++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(itemState, stack, ItemDisplayContext.GROUND,
                    stand.getLevel(), null, seed + i);
            state.items[i] = itemState;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		// V33a ModelItemStand used the standard entity-model origin (floor at model Y=24).
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(STAND_TEXTURE),
				(pose, vertices) -> standModel.render(modelPose, vertices, state.lightCoords,
						OverlayTexture.NO_OVERLAY));
		poseStack.popPose();

        if (state.copyCount <= 0) return;
        double scale = state.copyCount > 1 ? 1D / Math.pow(state.copyCount, 0.25) : 1;
        for (int i = 0; i < state.copyCount; i++) {
            ItemStackRenderState item = state.items[i];
            if (item == null) continue;
            double angle = (state.animationTick * 3D) % 360D + i * 360D / state.copyCount;
            double bob = 0.0625D * Math.sin(Math.toRadians(angle * 2D));
            poseStack.pushPose();
            poseStack.translate(0.5, 0.625 + bob, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees((float)angle));
            if (state.copyCount > 1) {
                poseStack.scale((float)scale, (float)scale, (float)scale);
                poseStack.translate(0.3125, 0, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.animationTick * 4F + i * 90F / state.copyCount));
            }
            item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        if (state.showCount) {
            var text = Component.literal(Integer.toString(state.stackCount)).getVisualOrderText();
            poseStack.pushPose();
            poseStack.translate(0.5, 1.25, 0.5);
            poseStack.mulPose(camera.orientation);
            poseStack.scale(0.03125F, -0.03125F, 0.03125F);
            collector.submitText(poseStack, -font.width(text) / 2F, 0, text, true,
                    Font.DisplayMode.POLYGON_OFFSET, 15728880, 0xffffffff, 0, 0);
            poseStack.popPose();
        }
    }

    private static int copiesFor(int count) {
        if (count >= 32) return 6;
        if (count >= 18) return 5;
        if (count >= 8) return 4;
        if (count >= 4) return 3;
        if (count >= 2) return 2;
        return count > 0 ? 1 : 0;
    }

    private static boolean isHoldingManipulator(@Nullable Player player) {
        if (player == null) return false;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && MANIPULATOR.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
        }
        return false;
    }

	/** Exact 128x128 Techne cuboids, pivots, UV origins, and rotations from V33a ModelItemStand. */
	public static LayerDefinition createStandLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		part(root, "top_n", 93, 20, -2, 0, -2, 6, 1, 7, -1, 17, 3, 30, 0, 0);
		part(root, "top_s", 0, 20, -4, 0, -5, 6, 1, 7, 1, 17, -3, -30, 0, 0);
		part(root, "top_e", 64, 20, -2, 0, 0, 7, 1, 6, 3, 17, -3, 0, 0, -30);
		part(root, "top_w", 32, 20, -5, 0, 0, 7, 1, 6, -3, 17, -3, 0, 0, 30);
		part(root, "top_center", 0, 64, 0, 0, 0, 6, 1, 6, -3, 18, -3, 0, 0, 0);
		part(root, "base", 32, 0, 0, 0, 0, 16, 1, 16, -8, 23, -8, 0, 0, 0);
		part(root, "wall_nw", 0, 32, -7.8F, 0, -3.5F, 1, 9, 7, 0, 14, 0, 0, -45, 0);
		part(root, "wall_n", 0, 50, 0, 0, 0, 6, 9, 1, -3, 14, -8, 0, 0, 0);
		part(root, "wall_s", 0, 50, 0, 0, 0, 6, 9, 1, -3, 14, 7, 0, 0, 0);
		part(root, "wall_e", 20, 32, 0, 0, 1, 1, 9, 6, 7, 14, -4, 0, 0, 0);
		part(root, "wall_w", 20, 32, 0, 0, 0, 1, 9, 6, -8, 14, -3, 0, 0, 0);
		part(root, "wall_ne", 0, 32, -7.8F, 0, -3.5F, 1, 9, 7, 0, 14, 0, 0, 45, 0);
		part(root, "wall_se", 0, 32, -7.8F, 0, -3.5F, 1, 9, 7, 0, 14, 0, 0, 135, 0);
		part(root, "wall_sw", 0, 32, -7.8F, 0, -3.5F, 1, 9, 7, 0, 14, 0, 0, -135, 0);
		part(root, "fin_nw", 0, 0, -4, -8.2F, -2.4F, 8, 5, 1, 0, 19, 0, 45, -135, 0);
		part(root, "fin_sw", 0, 7, -4, -8.2F, -2.4F, 8, 5, 1, 0, 19, 0, 45, -45, 0);
		part(root, "fin_se", 0, 73, -4, -8.2F, -2.4F, 8, 5, 1, 0, 19, 0, 45, 45, 0);
		part(root, "fin_ne", 0, 81, -4, -8.2F, -2.4F, 8, 5, 1, 0, 19, 0, 45, 135, 0);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void part(PartDefinition root, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xDegrees, float yDegrees, float zDegrees) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(x, y, z, dx, dy, dz), PartPose.offsetAndRotation(px, py, pz,
					(float)Math.toRadians(xDegrees), (float)Math.toRadians(yDegrees), (float)Math.toRadians(zDegrees)));
	}

    public static final class State extends BlockEntityRenderState {
        private final ItemStackRenderState[] items = new ItemStackRenderState[6];
        private int copyCount;
        private int stackCount;
        private float animationTick;
        private boolean showCount;
    }
}
