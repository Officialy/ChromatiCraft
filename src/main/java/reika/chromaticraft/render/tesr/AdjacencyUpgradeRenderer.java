/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code AdjacencyUpgradeRenderer}: three four-pixel line rings (vertical, side-on vertical, horizontal) turning at
 * a speed that grows with tier, each pulsing between the colour's two render colours on a cycle that shortens with
 * tier, around a camera-facing glow. A redstone-stopped upgrade turns six times slower in grey. The item draws the glow
 * as a fixed cross beneath half-again larger rings.
 */
public final class AdjacencyUpgradeRenderer implements BlockEntityRenderer<TileEntityAdjacencyUpgrade, AdjacencyUpgradeRenderer.State> {

	private static final float LINE_WIDTH = 4F;

	public AdjacencyUpgradeRenderer(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityAdjacencyUpgrade te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.upgrade = AdjacencyUpgrades.upgrades[te.getColor().ordinal()];
		state.tier = te.getTier();
		state.disabled = te.getLevel() != null && !te.canRun(te.getLevel(), te.getBlockPos());
		state.seed = te.getBlockPos().getX()+te.getBlockPos().getY()*4+te.getBlockPos().getZ()*2;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		submitUpgrade(poseStack, collector, state.upgrade, state.tier, state.disabled, state.seed, camera.orientation);
	}

	/**
	 * The whole V33a render from the block's corner. {@code cameraOrientation} null is V33a's out-of-world (item)
	 * branch.
	 */
	public static void submitUpgrade(PoseStack poseStack, SubmitNodeCollector collector, AdjacencyUpgrades upgrade,
			int tier, boolean disabled, int seed, @Nullable Quaternionfc cameraOrientation) {
		boolean inWorld = cameraOrientation != null;
		poseStack.pushPose();
		// V33a glScaled(1, -1, -1) after moving to the block.
		poseStack.scale(1, -1, -1);

		if (!inWorld) {
			double a = 0.5;
			double b = -0.4;
			double c = -0.5;
			poseStack.pushPose();
			poseStack.translate(a, b, c);
			submitInnerItem(poseStack, collector);
			poseStack.rotate(Axis.YP.rotationDegrees(90));
			submitInnerItem(poseStack, collector);
			poseStack.popPose();
		}

		submitMiddle(poseStack, collector, upgrade, tier, disabled, seed, inWorld);

		if (inWorld) {
			poseStack.popPose();
			submitInnerWorld(poseStack, collector, cameraOrientation);
			return;
		}
		poseStack.popPose();
	}

	private static void submitMiddle(PoseStack poseStack, SubmitNodeCollector collector, AdjacencyUpgrades upgrade,
			int tier, boolean dis, int seed, boolean inWorld) {
		poseStack.pushPose();
		poseStack.translate(0.5, -0.5, -0.5);

		double s = 1.5;
		if (!inWorld) {
			poseStack.scale((float)s, (float)s, (float)s);
		}

		double time = System.currentTimeMillis()+seed*48D;
		if (dis) {
			time = time/6D;
		}
		double rsc = 0.2+tier/4D;
		double rx = (time*rsc/4)%360;
		double ry = (time*rsc/3)%360;
		double rz = (time*rsc/5)%360;

		double d = 7200-tier*975;
		double d2 = d/2;
		double d3 = d/3;

		double t1 = time%d;
		if (dis) {
			t1 = t1/12;
		}

		double b1 = t1 >= d2 ? (d-t1)/d2 : t1/d2;
		double t2 = (time+d3)%d;
		double b2 = t2 >= d2 ? (d-t2)/d2 : t2/d2;
		double t3 = (time+2*d3)%d;
		double b3 = t3 >= d2 ? (d-t3)/d2 : t3/d2;

		int ca = upgrade.color1;
		int cb = upgrade.color2;

		int c1 = ReikaColorAPI.mixColors(ca, cb, (float)b1);
		int c2 = ReikaColorAPI.mixColors(ca, cb, (float)b2);
		int c3 = ReikaColorAPI.mixColors(ca, cb, (float)b3);

		if (dis) {
			c1 = ReikaColorAPI.mixColors(0x000000, 0xffffff, (float)b1);
			c2 = ReikaColorAPI.mixColors(0x000000, 0xffffff, (float)b2);
			c3 = ReikaColorAPI.mixColors(0x000000, 0xffffff, (float)b3);
		}

		c1 = c1 | 0xff000000;
		c2 = c2 | 0xff000000;
		c3 = c3 | 0xff000000;

		poseStack.rotate(Axis.XP.rotationDegrees((float)rx));
		submitRing(poseStack, collector, 0.25, c1, 1, 15);
		poseStack.rotate(Axis.YP.rotationDegrees((float)ry));
		submitRing(poseStack, collector, 0.28125, c2, 0, 15);
		poseStack.rotate(Axis.ZP.rotationDegrees((float)rz));
		submitRing(poseStack, collector, 0.3125, c3, 2, 15);
		poseStack.popPose();
	}

	/**
	 * V33a's GL_LINE_LOOPs as the independent segment pairs {@code RenderTypes.lines()} takes: plane 0 is
	 * {@code renderVCircle(phi 0)} (Y-Z), 1 is {@code renderVCircle(phi 90)} (X-Y, running the other way round) and 2
	 * is {@code renderCircle} (X-Z).
	 */
	private static void submitRing(PoseStack poseStack, SubmitNodeCollector collector, double r, int argb, int plane, int step) {
		collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, lines) -> {
			Vector3f prev = null;
			Vector3f first = null;
			for (int i = 0; i < 360; i += step) {
				Vector3f p = ringPoint(r, plane, i);
				if (prev != null)
					segment(pose, lines, prev, p, argb);
				else
					first = p;
				prev = p;
			}
			segment(pose, lines, prev, first, argb);
		});
	}

	private static Vector3f ringPoint(double r, int plane, int deg) {
		double a = Math.toRadians(deg);
		return switch(plane) {
			case 0 -> new Vector3f(0, (float)(r*Math.cos(a)), (float)(r*Math.sin(a)));
			case 1 -> new Vector3f((float)(-r*Math.sin(a)), (float)(r*Math.cos(a)), 0);
			default -> new Vector3f((float)(r*Math.cos(a)), 0, (float)(r*Math.sin(a)));
		};
	}

	private static void segment(PoseStack.Pose pose, VertexConsumer lines, Vector3f a, Vector3f b, int argb) {
		Vector3f n = new Vector3f(b).sub(a).normalize();
		lines.addVertex(pose, a.x, a.y, a.z).setColor(argb).setNormal(pose, n.x, n.y, n.z).setLineWidth(LINE_WIDTH);
		lines.addVertex(pose, b.x, b.y, b.z).setColor(argb).setNormal(pose, n.x, n.y, n.z).setLineWidth(LINE_WIDTH);
	}

	private static TextureAtlasSprite glow() {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.GLOWSECTION.getIcon());
	}

	/** V33a {@code drawInner}, in world: the glow section, a sixteenth trimmed off each edge, facing the camera. */
	private static void submitInnerWorld(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc orientation) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.rotate(orientation);
		float s = 0.1875F;
		poseStack.scale(s, s, s);
		submitGlowQuad(poseStack, collector, false);
		poseStack.popPose();
	}

	/** V33a {@code drawInner}, as an item: tilted 45 degrees round and 30 back, an eighth of a block lower. */
	private static void submitInnerItem(PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.translate(0, -0.125, 0);
		poseStack.rotate(Axis.YP.rotationDegrees(-45));
		poseStack.rotate(Axis.XP.rotationDegrees(-30));
		float s = 0.1875F;
		poseStack.scale(s, s, s);
		submitGlowQuad(poseStack, collector, true);
		poseStack.popPose();
	}

	private static void submitGlowQuad(PoseStack poseStack, SubmitNodeCollector collector, boolean flippedFrame) {
		TextureAtlasSprite ico = glow();
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(pose, v5) -> {
					float u = ico.getU0();
					float v = ico.getV0();
					float du = ico.getU1();
					float dv = ico.getV1();
					float uu = du-u;
					float vv = dv-v;
					float r = 16;
					u += uu/r;
					du -= uu/r;
					v += vv/r;
					dv -= vv/r;
					// V33a's quad runs (-1,-1) to (1,1) in its y-flipped frame; the camera frame is not flipped.
					float y = flippedFrame ? 1 : -1;
					int c = 0xffffffff;
					v5.addVertex(pose, -1, -y, 0).setUv(u, v).setColor(c);
					v5.addVertex(pose, 1, -y, 0).setUv(du, v).setColor(c);
					v5.addVertex(pose, 1, y, 0).setUv(du, dv).setColor(c);
					v5.addVertex(pose, -1, y, 0).setUv(u, dv).setColor(c);
				});
	}

	public static final class State extends BlockEntityRenderState {
		private AdjacencyUpgrades upgrade = AdjacencyUpgrades.LIGHTBLUE;
		private int tier;
		private boolean disabled;
		private int seed;
	}

}
