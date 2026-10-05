/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.tesr.dimension;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityPistonController;
import reika.chromaticraft.world.dimension.structure.PistonTapePlan;

/**
 * V33a {@code RenderPistonDisplay}, for the Tape Data Display controller: a black 2.5-block panel on its front
 * with one row per door, each row the door's expected colours (pulsing slightly, solid on the step being
 * played) followed by its number, white on the active step and grey otherwise. Drawn unlit with alpha blending.
 */
public final class RenderPistonDisplay implements BlockEntityRenderer<TileEntityPistonController, RenderPistonDisplay.State> {

	private final Font font;

	public RenderPistonDisplay(BlockEntityRendererProvider.Context context) {
		font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityPistonController te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.visible = te.type() == 3;
		state.facing = te.facing();
		state.doors.clear();
		if (state.visible)
			for (PistonTapePlan.DoorKey key : te.displayList()) {
				int[] colors = new int[key.colorCount()];
				for (int i = 0; i < colors.length; i++)
					colors[i] = key.color(i).getRenderColor();
				state.doors.add(new Row(key.value, colors, key.index));
			}
		state.active = te.isDisplayActive();
		state.activeDoor = te.activeDoor();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.visible || state.doors.isEmpty())
			return;
		double tw = 1.25;
		double th = 1.25;
		double o = 0.03125 / 2;
		double y2 = th - o;
		double y1 = -th + o;
		double dy = (y2 - y1) / state.doors.size();
		double y = y2;
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, -o);
		poseStack.rotate(Axis.ZP.rotationDegrees(180));
		switch (state.facing) {
			case SOUTH -> {
				poseStack.rotate(Axis.YP.rotationDegrees(180));
				poseStack.translate(0, 0, -1 - o * 2);
			}
			case WEST -> {
				poseStack.rotate(Axis.YP.rotationDegrees(270));
				poseStack.translate(tw / 2, 0, -0.5 - o);
			}
			case EAST -> {
				poseStack.rotate(Axis.YP.rotationDegrees(90));
				poseStack.translate(-tw / 2, 0, -0.5 - o);
			}
			default -> {}
		}
		PoseStack.Pose backing = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.translucentColorTriangles(),
				(unused, out) -> quad(backing, out, -tw, th, tw, -th, 0xff000000));
		double pad = o * 0.5;
		double gap = 0.03125 / 2;
		for (Row d : state.doors) {
			boolean active = state.active && state.activeDoor == d.index();
			int count = d.colors().length;
			poseStack.pushPose();
			poseStack.translate(0, -y, 0);
			double sw = 0.0625 + 0.03125 * (count - 1);
			double h = th * 2 / state.doors.size() - gap;
			double w = (tw * 2 / count) * (1 - sw) - pad / (count * 2) - gap;
			PoseStack.Pose rowPose = poseStack.last().copy();
			List<double[]> rects = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				double ox = pad + i * w - tw + gap * count / 2D;
				double t = d.index() * 2.8734 + i * 3.47820 + System.currentTimeMillis() / 300D;
				int a = active ? 255 : 230 + (int)(25 * Math.sin(t));
				rects.add(new double[] {ox, h, w + ox, 0, ((long)a << 24 | (d.colors()[i] & 0xffffff))});
			}
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.translucentColorTriangles(),
					(unused, out) -> {
						for (double[] r : rects)
							quad(rowPose, out, r[0], r[1], r[2], r[3], (int)(long)r[4]);
					});
			double fs = 0.03125 * Math.min(tw, th) / 1.5 / 2;
			poseStack.scale((float)fs, (float)fs, (float)fs);
			StringBuilder sg = new StringBuilder(String.valueOf(d.value()));
			while (sg.length() < count)
				sg.insert(0, '0');
			double ds = Math.max(tw, th);
			double ox = -(count - 3) / fs * 0.275 - 0.25 / fs;
			if (count == 4)
				ox += 0.175 / fs;
			var text = Component.literal(sg.toString()).getVisualOrderText();
			poseStack.translate(-font.width(text) / 2D + (1 - sw) * w / fs + sw * 1.35 / fs * ds - ox,
					font.lineHeight / 2D * ds * 1.35, 0);
			collector.submitText(poseStack, 0, 0, text, false, Font.DisplayMode.POLYGON_OFFSET, 15728880,
					active ? 0xffffffff : 0xffb0b0b0, 0, 0);
			y -= dy;
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	/** One flat quad as two triangles, at z = 0 in the pose. */
	private static void quad(PoseStack.Pose pose, VertexConsumer out, double x1, double y1, double x2, double y2, int argb) {
		float ax = (float)x1, ay = (float)y1, bx = (float)x2, by = (float)y2;
		out.addVertex(pose, ax, ay, 0).setColor(argb);
		out.addVertex(pose, bx, ay, 0).setColor(argb);
		out.addVertex(pose, bx, by, 0).setColor(argb);
		out.addVertex(pose, ax, ay, 0).setColor(argb);
		out.addVertex(pose, bx, by, 0).setColor(argb);
		out.addVertex(pose, ax, by, 0).setColor(argb);
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityPistonController te) {
		return new AABB(te.getBlockPos()).inflate(3);
	}

	private record Row(int value, int[] colors, int index) {}

	public static final class State extends BlockEntityRenderState {
		private boolean visible;
		private Direction facing = Direction.NORTH;
		private final List<Row> doors = new ArrayList<>();
		private boolean active;
		private int activeDoor;
	}
}
