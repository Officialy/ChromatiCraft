package reika.chromaticraft.render.tesr.dimension;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.dimension.TileEntityGlowingCracks;

/**
 * V33a {@code RenderGlowingCracks}: the light in the ground, which is the whole of what a Glowing
 * Cracks block looks like — {@code getImageFileName} returns null and the block itself is a sliver a
 * thousandth thick.
 *
 * <p>Two nine-by-nine quads laid flat a hair above the ground. The lower one carries a colour mixed
 * between two random hues, its four corners each on their own sine so the sheet drifts rather than
 * pulsing as one; the upper is white at three-quarters to full brightness on four more. Both are
 * additive, so what the player sees is the ground glowing through, not a decal on it.
 *
 * <p>Every constant that varies per block comes from one {@code Random} seeded on the tile's identity,
 * exactly as upstream does: the period, the eight sine rates and the eight phase offsets, the two hues
 * and the quarter-turn the whole sheet is rotated by. That is what stops a field of cracks beating in
 * unison, and reseeding it per frame is what keeps a given crack the same crack.
 */
public final class RenderGlowingCracks
		implements BlockEntityRenderer<TileEntityGlowingCracks, RenderGlowingCracks.State> {

	/** Reika's own 1024x1024 sheet, drawn across the whole nine-by-nine rather than tiled. */
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/dimgen/glowcracks.png");

	private final Random rand = new Random();

	public RenderGlowingCracks(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityGlowingCracks cracks, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(cracks, state, partialTick, cameraPosition,
				breakProgress);
		// Upstream seeds on the tile's identity hash. A position hash is the stable equivalent and
		// survives the tile being recreated, which identity hashing does not.
		rand.setSeed(cracks.getBlockPos().asLong());

		double period = 600 + 400 * rand.nextDouble();
		double t1 = (System.currentTimeMillis() / period) % 360D + rand.nextDouble() * 3;
		for (int i = 0; i < 4; i++) {
			double rate = 0.5 + rand.nextDouble();
			double phase = -2 + rand.nextDouble() * 4;
			state.colorMix[i] = (float)(0.5 + 0.5 * Math.sin(t1 * rate + phase));
		}
		double t2 = (System.currentTimeMillis() / 512D) % 360D;
		for (int i = 0; i < 4; i++) {
			double rate = 0.5 + rand.nextDouble();
			double phase = -2 + rand.nextDouble() * 4;
			state.whiteMix[i] = (float)(0.75 + 0.25 * Math.sin(t2 * rate + phase));
		}
		state.color1 = shiftHue(0xFF0000, rand.nextInt(360));
		state.color2 = shiftHue(0xFF0000, rand.nextInt(360));
		state.quarterTurns = rand.nextInt(4);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		final int r = TileEntityGlowingCracks.RADIUS;
		poseStack.pushPose();
		poseStack.translate(0.5F, 0F, 0.5F);
		poseStack.mulPose(Axis.YP.rotationDegrees(state.quarterTurns * 90F));
		poseStack.translate(-0.5F, 0F, -0.5F);

		final int[] tint = new int[4];
		for (int i = 0; i < 4; i++)
			tint[i] = 0xFF000000 | mixColors(state.color1, state.color2, state.colorMix[i]);
		final int[] white = new int[4];
		for (int i = 0; i < 4; i++)
			white[i] = 0xFF000000 | scaleBrightness(0xFFFFFF, state.whiteMix[i]);

		// Additive so the sheet reads as light through the ground; upstream's ADDITIVEDARK with the
		// depth mask off is what `translucent` plus a full-bright light gives here.
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE),
				(pose, vertices) -> {
					quad(vertices, pose, r, tint);
					quad(vertices, pose, r, white);
				});
		poseStack.popPose();
	}

	/** One flat sheet from {@code -r} to {@code r+1}, a hair above the ground, corner-coloured. */
	private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vertices,
			PoseStack.Pose pose, int r, int[] colors) {
		final float y = 0.005F;
		vertex(vertices, pose, -r, y, r + 1, 0, 1, colors[0]);
		vertex(vertices, pose, r + 1, y, r + 1, 1, 1, colors[1]);
		vertex(vertices, pose, r + 1, y, -r, 1, 0, colors[2]);
		vertex(vertices, pose, -r, y, -r, 0, 0, colors[3]);
	}

	private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack.Pose pose,
			float x, float y, float z, float u, float v, int color) {
		vertices.addVertex(pose, x, y, z).setColor(color).setUv(u, v)
				.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
				.setLight(0xF000F0).setNormal(pose, 0, 1, 0);
	}

	/** {@code ReikaColorAPI.getModifiedHue}. */
	private static int shiftHue(int color, int degrees) {
		float[] hsb = java.awt.Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
		return java.awt.Color.HSBtoRGB(degrees / 360F, hsb[1], hsb[2]) & 0xFFFFFF;
	}

	/** {@code ReikaColorAPI.mixColors}: {@code f} of the first, the rest of the second. */
	private static int mixColors(int c1, int c2, float f) {
		int r = (int)((c1 >> 16 & 255) * f + (c2 >> 16 & 255) * (1 - f));
		int g = (int)((c1 >> 8 & 255) * f + (c2 >> 8 & 255) * (1 - f));
		int b = (int)((c1 & 255) * f + (c2 & 255) * (1 - f));
		return r << 16 | g << 8 | b;
	}

	/** {@code ReikaColorAPI.getColorWithBrightnessMultiplier}. */
	private static int scaleBrightness(int color, float factor) {
		int r = Math.min(255, (int)((color >> 16 & 255) * factor));
		int g = Math.min(255, (int)((color >> 8 & 255) * factor));
		int b = Math.min(255, (int)((color & 255) * factor));
		return r << 16 | g << 8 | b;
	}

	/** The per-frame values the sheet is drawn with. */
	public static final class State extends BlockEntityRenderState {
		public final float[] colorMix = new float[4];
		public final float[] whiteMix = new float[4];
		public int color1;
		public int color2;
		public int quarterTurns;
	}
}
