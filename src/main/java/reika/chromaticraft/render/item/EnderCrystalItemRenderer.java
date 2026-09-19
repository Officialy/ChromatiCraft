package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.ItemEnderCrystal;

/** V33a's bedrock-base End Crystal item renderer, including its empty flattened blue form. */
public final class EnderCrystalItemRenderer implements SpecialModelRenderer<Boolean> {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"ender_crystal_mover");
	private static final Identifier CRYSTAL_TEXTURE =
			Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");

	private final EndCrystalModel crystalModel;
	private final SpriteGetter sprites;
	private final SpriteId bedrock;

	private EnderCrystalItemRenderer(EndCrystalModel crystalModel, SpriteGetter sprites) {
		this.crystalModel = crystalModel;
		this.sprites = sprites;
		this.bedrock = new SpriteId(TextureAtlas.LOCATION_BLOCKS,
				Identifier.withDefaultNamespace("block/bedrock"));
	}

	@Override
	public void submit(@Nullable Boolean filled, PoseStack poseStack, SubmitNodeCollector collector,
			int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
		boolean hasCrystal = Boolean.TRUE.equals(filled);
		TextureAtlasSprite sprite = sprites.get(bedrock);
		collector.submitCustomGeometry(poseStack,
				RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS), (unused, vertices) -> {
			float min = hasCrystal ? 0.25F : 0.125F;
			float max = hasCrystal ? 0.75F : 0.875F;
			float bottom = 0.08F;
			float top = hasCrystal ? 0.33F : 0.205F;
			emitBox(poseStack.last(), vertices, sprite, min, bottom, min, max, top, max,
					hasCrystal ? 0xFFFFFFFF : 0xFFBEBEFF, lightCoords, overlayCoords);
		});

		if (!hasCrystal)
			return;
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.59F, 0.5F);
		poseStack.scale(0.42F, 0.42F, 0.42F);
		poseStack.scale(2, 2, 2);
		poseStack.translate(0, -0.5F, 0);
		EndCrystalRenderState state = new EndCrystalRenderState();
		state.ageInTicks = (System.currentTimeMillis() % 1_200_000L) / 50F;
		state.showsBottom = false;
		state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
		state.outlineColor = outlineColor;
		collector.submitModel(crystalModel, state, poseStack, CRYSTAL_TEXTURE,
				LightCoordsUtil.FULL_BRIGHT, overlayCoords, outlineColor, null);
		poseStack.popPose();
	}

	private static void emitBox(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite,
			float x0, float y0, float z0, float x1, float y1, float z1, int color, int light,
			int overlay) {
		face(pose,out,sprite,Direction.DOWN,color,light,overlay, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1);
		face(pose,out,sprite,Direction.UP,color,light,overlay, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0);
		face(pose,out,sprite,Direction.NORTH,color,light,overlay, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
		face(pose,out,sprite,Direction.SOUTH,color,light,overlay, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
		face(pose,out,sprite,Direction.WEST,color,light,overlay, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
		face(pose,out,sprite,Direction.EAST,color,light,overlay, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
	}

	private static void face(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite,
			Direction normal, int color, int light, int overlay, float... xyz) {
		for (int i = 0; i < 4; i++) {
			float u = i == 1 || i == 2 ? 1 : 0;
			float v = i >= 2 ? 1 : 0;
			out.addVertex(pose, xyz[i*3], xyz[i*3+1], xyz[i*3+2]).setColor(color)
					.setUv(sprite.getU(u), sprite.getV(v)).setOverlay(overlay).setLight(light)
					.setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
		}
	}

	@Override public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0));
		output.accept(new Vector3f(1));
	}

	@Override public Boolean extractArgument(ItemStack stack) {
		return ItemEnderCrystal.stored(stack) > 0;
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<Boolean> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends SpecialModelRenderer.Unbaked<Boolean>> type() { return MAP_CODEC; }
		@Override public EnderCrystalItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new EnderCrystalItemRenderer(
					new EndCrystalModel(context.entityModelSet().bakeLayer(ModelLayers.END_CRYSTAL)),
					context.sprites());
		}
	}
}
