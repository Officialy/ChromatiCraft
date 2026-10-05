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

import java.util.EnumSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelWaterLock;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityRotatingLock;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderWaterLock}. The solid pass draws the lock's four wall segments and, for every arm that is
 * currently closed, the channel plug, all turned by the in-progress rotation. Checkpoint and endpoint locks
 * also raise an animated, yaw-facing light cone (ADDITIVEDARK): yellow for the exit, green over a checkpoint
 * that holds fluid, red over one that does not.
 */
public final class RenderWaterLock implements BlockEntityRenderer<TileEntityRotatingLock, RenderWaterLock.State> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "water_lock"), "main");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/entity/waterlock.png");
	private static final Identifier LIGHTCONE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/effect/lightcone-foggy.png");

	private final ModelWaterLock model;

	public RenderWaterLock(BlockEntityRendererProvider.Context context) {
		model = new ModelWaterLock(context.bakeLayer(LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityRotatingLock te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.closed.clear();
		state.closed.addAll(Direction.Plane.HORIZONTAL.stream().toList());
		te.openEnds().forEach(state.closed::remove);
		state.rotation = te.rotationProgress();
		state.checkpoint = te.isCheckpoint();
		state.endpoint = te.isEndpoint();
		state.fluid = te.getLevel() != null
				&& te.getLevel().getBlockState(te.getBlockPos().above()).is(ChromaBlocks.EVER_FLUID.get());
		state.tick = (te.getLevel() != null ? te.getLevel().getGameTime() : 0) + te.hashCode();
	}

	/** DragonAPI {@code ReikaDirectionHelper.getHeading}. */
	private static int heading(Direction dir) {
		return switch (dir) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0, 0.95, 0);

		// ChromaRenderBase.preRenderModel.
		poseStack.pushPose();
		poseStack.scale(1, -1, -1);
		poseStack.translate(0.5, -1.5, -0.5);
		int light = state.lightCoords;
		for (Direction dir : state.closed) {
			poseStack.pushPose();
			poseStack.rotate(Axis.YP.rotationDegrees(heading(dir) + state.rotation));
			PoseStack pose = copy(poseStack);
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
					(unused, out) -> model.renderChannel(pose, out, light));
			poseStack.popPose();
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			poseStack.pushPose();
			poseStack.rotate(Axis.YP.rotationDegrees(heading(dir) + state.rotation));
			PoseStack pose = copy(poseStack);
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
					(unused, out) -> model.renderSegment(pose, out, light));
			poseStack.popPose();
		}
		poseStack.popPose();

		if (state.checkpoint || state.endpoint) {
			poseStack.translate(0.5, 0, 0.5);
			poseStack.scale(2, 2, 2);
			long tick = state.tick;
			int idx = (int)Math.floorMod(tick, 48L);
			float u = (idx % 12) / 12F;
			float v = (idx / 12) / 4F;
			float du = u + 1 / 12F;
			float dv = v + 1 / 4F;
			float h = 2.5F;
			double f = 0.5 + 0.5 * Math.sin(tick / 8D) + 0.125 * Math.sin((tick + 300) / 3D) + 0.25 * Math.cos((tick + 20) / 7D);
			int c1;
			int c2;
			if (state.endpoint) {
				c1 = 0xffff00;
				c2 = 0xaaaa44;
			}
			else {
				c1 = state.fluid ? 0x00ff00 : 0xff0000;
				c2 = state.fluid ? 0x44aa44 : 0xaa4444;
			}
			int c = 0xff000000 | ReikaColorAPI.mixColors(c1, c2, (float)Mth.clamp(f, 0, 1));
			poseStack.rotate(Axis.YP.rotationDegrees(-camera.yRot));
			PoseStack.Pose pose = poseStack.last().copy();
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.legacyAdditiveSprite(LIGHTCONE),
					(unused, out) -> {
						out.addVertex(pose, -0.5F, h, 0).setUv(u, v).setColor(c);
						out.addVertex(pose, 0.5F, h, 0).setUv(du, v).setColor(c);
						out.addVertex(pose, 0.5F, 0, 0).setUv(du, dv).setColor(c);
						out.addVertex(pose, -0.5F, 0, 0).setUv(u, dv).setColor(c);
					});
		}
		poseStack.popPose();
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	/** V33a {@code getRenderBoundingBox}: the block expanded by the lock size sideways and 3 up/down. */
	@Override
	public AABB getRenderBoundingBox(TileEntityRotatingLock te) {
		return new AABB(te.getBlockPos()).inflate(3, 3, 3).expandTowards(0, 3, 0);
	}

	public static final class State extends BlockEntityRenderState {
		private final Set<Direction> closed = EnumSet.noneOf(Direction.class);
		private int rotation;
		private boolean checkpoint;
		private boolean endpoint;
		private boolean fluid;
		private long tick;
	}
}
