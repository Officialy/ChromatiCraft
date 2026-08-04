package reika.chromaticraft.render.model;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;

import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Emits an axis-aligned box as six textured quads, the way V33a's
 * {@code ReikaRenderHelper.renderBlockPieceNonCuboid} / {@code CubePoints.renderIconOnSides} drew the
 * small crystal pieces: every face is drawn (the pieces are freestanding, so nothing may be culled),
 * the sprite is sampled over the piece's own footprint rather than stretched, and the source's
 * {@code setBrightness(240)} becomes full light emission with shading and ambient occlusion off.
 */
final class CrystalQuads {

	private CrystalQuads() {}

	static void addPiece(QuadCollection.Builder quads, CrystalPiece piece,
			Material.Baked material, int rgb, int alpha) {
		int colour = (alpha << 24) | (rgb & 0xFFFFFF);
		for (Direction face : Direction.values())
			addFace(quads, piece, face, material, colour);
	}

	private static void addFace(QuadCollection.Builder quads, CrystalPiece piece,
			Direction face, Material.Baked material, int colour) {
		int[] corners = CrystalPiece.face(face);
		// The sprite is sampled over the piece's own footprint in block space, as V33a's
		// renderBlockPieceNonCuboid does, so neighbouring pieces show different slices of the icon
		// rather than each repeating the whole texture.
		int uAxis = face.getAxis() == Direction.Axis.X ? 2 : 0;
		int vAxis = face.getAxis() == Direction.Axis.Y ? 2 : 1;

		QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
		vertex.setSprite(material, Transparency.TRANSLUCENT);
		vertex.setTintIndex(-1);
		vertex.setShade(false);
		vertex.setAmbientOcclusion(false);
		vertex.setLightEmission(15);
		vertex.setDirection(face);
		for (int corner : corners) {
			// getU/getV take a NORMALISED 0..1 coordinate within the sprite, not the 0..16 block-space
			// figure the 1.7.10 API wanted. Passing 0..16 ran sixteen sprites past the right edge of
			// this one and sampled the rest of the atlas, which drew the block as a sheet of icons.
			float u = (float)Math.clamp(piece.get(corner, uAxis), 0D, 1D);
			float v = (float)Math.clamp(piece.get(corner, vAxis), 0D, 1D);
			vertex.addVertex((float)piece.get(corner, 0), (float)piece.get(corner, 1), (float)piece.get(corner, 2))
					.setColor(colour)
					.setUv(material.sprite().getU(u), material.sprite().getV(v))
					.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
		}
		quads.addUnculledFace(vertex.bakeQuad());
	}
}
