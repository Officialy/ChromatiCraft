package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;

/**
 * V33a {@code CrystalGlowRenderer.renderInventoryBlock}: not the placed shape but a flat card -- the base
 * material's texture with the crystal's overbright sprite on it -- turned 45 degrees and tipped back 30, at 1.6x,
 * with lighting off.
 */
public final class CrystalGlowItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "crystal_glow");

	private final SpriteGetter sprites;
	private final SpriteId base;
	private final SpriteId lamp;

	private CrystalGlowItemRenderer(SpriteGetter sprites, SpriteId base, SpriteId lamp) {
		this.sprites = sprites;
		this.base = base;
		this.lamp = lamp;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(45));
		poseStack.mulPose(Axis.XP.rotationDegrees(-30));
		poseStack.scale(1.6F, 1.6F, 1.6F);
		poseStack.translate(-0.5, -0.5, 0);
		PoseStack.Pose pose = poseStack.last().copy();
		TextureAtlasSprite base = sprites.get(this.base);
		TextureAtlasSprite lamp = sprites.get(this.lamp);
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.unlitEntityCutout(TextureAtlas.LOCATION_BLOCKS),
				(unused, out) -> {
					card(pose, out, base, 0.2875F, 0.4375F, 0);
					// Drawn after the plate at the same depth upstream; a hair forward here so it wins cleanly.
					card(pose, out, lamp, 0.1875F, 0.325F, 0.001F);
				});
		poseStack.popPose();
	}

	private static void card(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite, float w, float h, float z) {
		vertex(pose, out, sprite, 0.5F - w, 0.5F - h, z, 0, 0);
		vertex(pose, out, sprite, 0.5F + w, 0.5F - h, z, 1, 0);
		vertex(pose, out, sprite, 0.5F + w, 0.5F + h, z, 1, 1);
		vertex(pose, out, sprite, 0.5F - w, 0.5F + h, z, 0, 1);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite, float x, float y,
			float z, float u, float v) {
		out.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(sprite.getU(u), sprite.getV(v))
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0));
		output.accept(new Vector3f(1));
	}

	public record Unbaked(Identifier base, Identifier lamp) implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
				Identifier.CODEC.fieldOf("lamp").forGetter(Unbaked::lamp)
		).apply(instance, Unbaked::new));

		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }

		@Override
		public CrystalGlowItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new CrystalGlowItemRenderer(context.sprites(), new SpriteId(TextureAtlas.LOCATION_BLOCKS, base),
					new SpriteId(TextureAtlas.LOCATION_BLOCKS, lamp));
		}
	}
}
