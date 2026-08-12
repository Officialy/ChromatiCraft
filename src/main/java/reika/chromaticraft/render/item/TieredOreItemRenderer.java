package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;

/** Inventory equivalent of V33a's two-pass TieredOreRenderer. */
public final class TieredOreItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "tiered_ore");
	private final SpriteGetter sprites;
	private final SpriteId underlay;
	private final SpriteId overlay;

	private TieredOreItemRenderer(SpriteGetter sprites, SpriteId underlay, SpriteId overlay) {
		this.sprites = sprites;
		this.underlay = underlay;
		this.overlay = overlay;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		TextureAtlasSprite baseSprite = sprites.get(underlay);
		TextureAtlasSprite glowSprite = sprites.get(overlay);
		collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				(unused, vertices) -> emitCube(poseStack.last(), vertices, baseSprite,
						0, 1, lightCoords, overlayCoords));
		collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				(unused, vertices) -> emitCube(poseStack.last(), vertices, glowSprite,
						-0.002F, 1.002F, LightCoordsUtil.FULL_BRIGHT, overlayCoords));
	}

	private static void emitCube(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite,
			float min, float max, int light, int overlay) {
		face(pose,out,sprite,Direction.DOWN,light,overlay, min,min,min, max,min,min, max,min,max, min,min,max);
		face(pose,out,sprite,Direction.UP,light,overlay, min,max,max, max,max,max, max,max,min, min,max,min);
		face(pose,out,sprite,Direction.NORTH,light,overlay, max,min,min, min,min,min, min,max,min, max,max,min);
		face(pose,out,sprite,Direction.SOUTH,light,overlay, min,min,max, max,min,max, max,max,max, min,max,max);
		face(pose,out,sprite,Direction.WEST,light,overlay, min,min,min, min,min,max, min,max,max, min,max,min);
		face(pose,out,sprite,Direction.EAST,light,overlay, max,min,max, max,min,min, max,max,min, max,max,max);
	}

	private static void face(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite,
			Direction normal, int light, int overlay, float... xyz) {
		for (int i = 0; i < 4; i++) {
			float u = i == 1 || i == 2 ? 1 : 0;
			float v = i >= 2 ? 1 : 0;
			out.addVertex(pose, xyz[i*3], xyz[i*3+1], xyz[i*3+2])
					.setColor(0xFFFFFFFF).setUv(sprite.getU(u), sprite.getV(v))
					.setOverlay(overlay).setLight(light)
					.setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
		}
	}

	@Override public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0));
		output.accept(new Vector3f(1));
	}

	public record Unbaked(Identifier underlay, Identifier overlay)
			implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.fieldOf("underlay").forGetter(Unbaked::underlay),
				Identifier.CODEC.fieldOf("overlay").forGetter(Unbaked::overlay)
		).apply(instance, Unbaked::new));
		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public TieredOreItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new TieredOreItemRenderer(context.sprites(),
					new SpriteId(TextureAtlas.LOCATION_BLOCKS, underlay),
					new SpriteId(TextureAtlas.LOCATION_BLOCKS, overlay));
		}
	}
}
