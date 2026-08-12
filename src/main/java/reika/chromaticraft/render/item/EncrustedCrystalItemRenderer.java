package reika.chromaticraft.render.item;

import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
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
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** V33a {@code CrystalEncrustingRenderer.renderInventoryBlock}, on the translucent item target. */
public final class EncrustedCrystalItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "encrusted_crystal");

	private final SpriteGetter sprites;
	private final SpriteId texture;
	private final CrystalElement element;

	private EncrustedCrystalItemRenderer(SpriteGetter sprites, SpriteId texture, CrystalElement element) {
		this.sprites = sprites;
		this.texture = texture;
		this.element = element;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		TextureAtlasSprite sprite = sprites.get(texture);
		int seed = Minecraft.getInstance().player != null
				? Minecraft.getInstance().player.getUUID().hashCode() ^ element.ordinal()
				: 0x6C756D65 ^ element.ordinal();
		Random random = new Random(seed);
		random.nextBoolean(); // exact discarded V33a inventory roll
		int colour = 0xFF000000 | ReikaColorAPI.mixColors(element.getColor(), 0xFFFFFF, 0.85F);
		collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				(unused, vertices) -> {
					boolean[][] used = new boolean[6][6];
					for (int i = 0; i < 12; i++) {
						int x = random.nextInt(6);
						int z = random.nextInt(6);
						if (used[x][z])
							continue;
						used[x][z] = true;
						float x0 = x / 6F;
						float x1 = (x + 1) / 6F;
						float z0 = z / 6F;
						float z1 = (z + 1) / 6F;
						float y1 = 0.2F + random.nextFloat() * 0.6F;
						emitBox(poseStack.last(), vertices, sprite, colour, lightCoords, overlayCoords,
								x0, 0, z0, x1, y1, z1);
					}
				});
	}

	private static void emitBox(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertices,
			TextureAtlasSprite sprite, int colour, int light, int overlay,
			float x0, float y0, float z0, float x1, float y1, float z1) {
		emitFace(pose, vertices, sprite, colour, light, overlay, Direction.DOWN,
				x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1);
		emitFace(pose, vertices, sprite, colour, light, overlay, Direction.UP,
				x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0);
		emitFace(pose, vertices, sprite, colour, light, overlay, Direction.NORTH,
				x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
		emitFace(pose, vertices, sprite, colour, light, overlay, Direction.SOUTH,
				x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
		emitFace(pose, vertices, sprite, colour, light, overlay, Direction.WEST,
				x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
		emitFace(pose, vertices, sprite, colour, light, overlay, Direction.EAST,
				x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
	}

	private static void emitFace(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertices,
			TextureAtlasSprite sprite, int colour, int light, int overlay, Direction normal, float... xyz) {
		for (int i = 0; i < 4; i++) {
			float u = (i == 1 || i == 2) ? 1 : 0;
			float v = i >= 2 ? 1 : 0;
			vertices.addVertex(pose, xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2])
					.setColor(colour).setUv(sprite.getU(u), sprite.getV(v))
					.setOverlay(overlay).setLight(light)
					.setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
		}
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0, 0, 0));
		output.accept(new Vector3f(1, 1, 1));
	}

	public record Unbaked(Identifier texture, CrystalElement element)
			implements NoDataSpecialModelRenderer.Unbaked {
		private static final com.mojang.serialization.Codec<CrystalElement> ELEMENT_CODEC =
				com.mojang.serialization.Codec.STRING.xmap(
						name -> CrystalElement.valueOf(name.toUpperCase(Locale.ROOT)),
						e -> e.name().toLowerCase(Locale.ROOT));
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
				ELEMENT_CODEC.fieldOf("element").forGetter(Unbaked::element)
		).apply(instance, Unbaked::new));

		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public EncrustedCrystalItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new EncrustedCrystalItemRenderer(context.sprites(),
					new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture), element);
		}
	}
}
