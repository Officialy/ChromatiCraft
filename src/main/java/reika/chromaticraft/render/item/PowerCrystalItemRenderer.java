package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
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
import reika.chromaticraft.render.model.CaveCrystalGeometry;

/** V33a inventory renderer for the all-armed chroma Power Crystal and its pylon-stone base. */
public final class PowerCrystalItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"power_crystal");
	private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"block/crystal/chroma");
	private static final Identifier BASE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"block/pylon/block_0");

	private final SpriteGetter sprites;

	private PowerCrystalItemRenderer(SpriteGetter sprites) {
		this.sprites = sprites;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		PoseStack.Pose pose = poseStack.last();
		TextureAtlasSprite base = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, BASE));
		collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				(ignored, out) -> CaveCrystalGeometry.emitBase((points, normal, shade) -> {
					int color = 0xff000000 | shade << 16 | shade << 8 | shade;
					for (CaveCrystalGeometry.Point point : points) {
						Vector3f at = point.position();
						out.addVertex(pose, at.x, at.y, at.z).setColor(color)
								.setUv(base.getU(point.u()), base.getV(point.v()))
								.setOverlay(overlayCoords).setLight(lightCoords)
								.setNormal(pose, normal.x, normal.y, normal.z);
					}
				}, false));

		TextureAtlasSprite crystal = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, CRYSTAL));
		collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS),
				(ignored, out) -> CaveCrystalGeometry.emit((points, normal) -> {
					for (CaveCrystalGeometry.Point point : points) {
						Vector3f at = point.position();
						out.addVertex(pose, at.x, at.y, at.z).setColor(0xdcffffff)
								.setUv(crystal.getU(point.u()), crystal.getV(point.v()))
								.setOverlay(overlayCoords).setLight(LightCoordsUtil.FULL_BRIGHT)
								.setNormal(pose, normal.x, normal.y, normal.z);
					}
				}, 0xf, false, false, false));

		// In V33a the inventory crystal is rendered in its connected/active state, with an emissive
		// animated sprite rather than the inert world mesh. Retain the translucent body above, then add
		// a gentle full-bright ADDITIVE2 halo so the animation reads at inventory scale as it did there.
		float pulse = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 180D);
		int glow = ((int)(72 + 56 * pulse) << 24) | 0x00ffffff;
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.scale(1.035F, 1.035F, 1.035F);
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		PoseStack.Pose glowPose = poseStack.last();
		collector.submitCustomGeometry(poseStack,
				ChromaRenderPipelines.additiveAlphaSprite(TextureAtlas.LOCATION_BLOCKS),
				(ignored, out) -> CaveCrystalGeometry.emit((points, normal) -> {
					for (CaveCrystalGeometry.Point point : points) {
						Vector3f at = point.position();
						out.addVertex(glowPose, at.x, at.y, at.z).setColor(glow)
								.setUv(crystal.getU(point.u()), crystal.getV(point.v()))
								.setOverlay(overlayCoords).setLight(LightCoordsUtil.FULL_BRIGHT)
								.setNormal(glowPose, normal.x, normal.y, normal.z);
					}
				}, 0xf, false, false, false));
		poseStack.popPose();
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
		public PowerCrystalItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new PowerCrystalItemRenderer(context.sprites());
		}
	}
}
