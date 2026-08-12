package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.model.CaveCrystalGeometry;

/**
 * Inventory/held rendering for cave crystals, drawing the same V33a spike geometry the in-world
 * block uses instead of the flat outline cube the port shipped.
 *
 * <p>V33a's inventory render had no neighbours to consult, so the arm mask, ceiling flip and the
 * above/below joins are all fixed here rather than sampled from the world — a free-standing crystal
 * with all four side spikes, which is what {@code CrystalRenderer.renderInventoryBlock} drew.
 */
public final class CaveCrystalItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "cave_crystal");

	/** V33a's inventory crystal: all four side spikes, floor-mounted, no neighbour joins. */
	private static final int ITEM_ARM_MASK = 0b1111;

	private final SpriteGetter sprites;
	private final SpriteId sprite;
	private final int tint;
	/** Non-null for renderBase() crystals (lamp, potion crystal); null for cave crystals. */
	private final SpriteId baseSprite;

	private CaveCrystalItemRenderer(SpriteGetter sprites, SpriteId sprite, int tint, SpriteId baseSprite) {
		this.sprites = sprites;
		this.sprite = sprite;
		this.tint = tint;
		this.baseSprite = baseSprite;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		PoseStack.Pose pose = poseStack.last();
		if (baseSprite != null) {
			TextureAtlasSprite baseTexture = sprites.get(baseSprite);
			// renderBase() is the opaque support underneath a lamp/potion crystal. Submit it first:
			// 26.2 preserves submission order inside the item-translucent target, and drawing this
			// after the crystal allowed the plinth's depth-writing faces to replace the coloured
			// mesh with a white silhouette in GUI/hand renders.
			collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
					(unused, vertices) -> CaveCrystalGeometry.emitBase((points, normal, shade) -> {
						int colour = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
						for (CaveCrystalGeometry.Point point : points) {
							Vector3f position = point.position();
							vertices.addVertex(pose, position.x, position.y, position.z)
									.setColor(colour)
									.setUv(baseTexture.getU(point.u()), baseTexture.getV(point.v()))
									.setOverlay(overlayCoords)
									.setLight(lightCoords)
									.setNormal(pose, normal.x, normal.y, normal.z);
						}
					}, false));
		}
		TextureAtlasSprite crystalTexture = sprites.get(sprite);
		// The geometry is authored in block space (0..1), which is already the item model's cube.
		// Special item models render into the item target; the entity translucent type can be
		// flattened by the item compositor even though its vertex alpha is valid. This is the 26.2
		// item-target translucent path and preserves the same DC alpha used by the world model.
		collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				(unused, vertices) -> CaveCrystalGeometry.emit((points, normal) -> {
					for (CaveCrystalGeometry.Point point : points) {
						Vector3f position = point.position();
						vertices.addVertex(pose, position.x, position.y, position.z)
								.setColor(tint)
								.setUv(crystalTexture.getU(point.u()), crystalTexture.getV(point.v()))
								.setOverlay(overlayCoords)
								.setLight(lightCoords)
								.setNormal(pose, normal.x, normal.y, normal.z);
					}
				}, ITEM_ARM_MASK, false, false, false));
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0F, 0F, 0F));
		output.accept(new Vector3f(1F, 1F, 1F));
	}

	/**
	 * @param texture the crystal outline sprite, matching the block model's
	 * @param element  the crystal's colour; V33a tinted the shared greyscale outline per element
	 */
	public record Unbaked(Identifier texture, CrystalElement element, java.util.Optional<Identifier> baseTexture)
			implements NoDataSpecialModelRenderer.Unbaked {

		/** CrystalElement has no codec of its own; serialise by enum name as the recipes do. */
		private static final com.mojang.serialization.Codec<CrystalElement> ELEMENT_CODEC =
				com.mojang.serialization.Codec.STRING.xmap(
						name -> CrystalElement.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
						element -> element.name().toLowerCase(java.util.Locale.ROOT));

		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
				ELEMENT_CODEC.fieldOf("element").forGetter(Unbaked::element),
				Identifier.CODEC.optionalFieldOf("base_texture").forGetter(Unbaked::baseTexture)
		).apply(instance, Unbaked::new));

		@Override
		public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public CaveCrystalItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			SpriteId baked = new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture);
			SpriteId base = baseTexture.map(id -> new SpriteId(TextureAtlas.LOCATION_BLOCKS, id)).orElse(null);
			// Alpha 220 is V33a's pass-1 crystal alpha, the same value the block model bakes in.
			return new CaveCrystalItemRenderer(context.sprites(), baked,
					ARGB.color(220, element.getColor()), base);
		}
	}
}
