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

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.block.dimension.structure.laser.BlockLaserEffector26;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.render.model.LaserEffectorGeometry;
import reika.chromaticraft.tileentity.TileEntityLaserEffector;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.dragonapi.instantiable.effects.TruncatedCube;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderLaserTarget}, bound to the target tiles. Above each half-block target it tumbles a
 * truncated cube in the target colour at 192 alpha with lighter edges, bobbing and breathing on
 * wall-clock sines that run twice as fast once the target is triggered; a triggered target also faces
 * the camera with the rotating flare. Full-block targets draw nothing here. Everything is drawn
 * fullbright with V33a's ADDITIVEDARK blend. The pass-through target's ISBRH centre was commented out
 * upstream, so this renderer is the only part of it above the pedestal.
 */
public final class RenderLaserTarget
		implements BlockEntityRenderer<TileEntityLaserEffector, RenderLaserTarget.State> {

	private static final TruncatedCube cube = new TruncatedCube(0.045, 0.125);

	public RenderLaserTarget(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityLaserEffector te, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		BlockState block = te.getBlockState();
		EffectType type = EffectType.byLegacyMetadata(block.getValue(BlockLaserEffector26.TYPE));
		state.visible = type.isTarget() && !block.getValue(BlockLaserEffector26.FULL_BLOCK);
		state.triggered = block.getValue(BlockLaserEffector26.LIT);
		state.color = LaserEffectorGeometry.renderColor(block.getValue(BlockLaserEffector26.COLOR));
		state.hash = te.hashCode();
		state.pdist = (float)Math.sqrt(cameraPosition.distanceToSqr(Vec3.atCenterOf(te.getBlockPos())));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.visible)
			return;
		double t = System.currentTimeMillis();
		if (state.triggered)
			t *= 2;
		double s = (state.triggered ? 0.625 : 0.5) + 0.0625 * Math.sin(t / 400D) + 0.125 * Math.sin(t / 2000D);
		double dy = (state.triggered ? 0.1875 : 0.125) + 0.0625 * Math.cos(t / 1200D) + 0.125 * Math.cos(t / 4000D);
		int c = (state.color & 0xffffff) | (192 << 24);
		int c2 = ReikaColorAPI.mixColors(c, 0xffffff | (192 << 24), 0.75F);
		if ((state.color & 0xffffff) == 0xffffff)
			c = (c & 0xff000000) | 0xbfbfbf;

		poseStack.pushPose();
		poseStack.translate(0.5, 0.5 + dy, 0.5);
		poseStack.rotate(Axis.XP.rotationDegrees((float)((t / 20D) % 360)));
		poseStack.rotate(Axis.YP.rotationDegrees((float)((t / 20D + state.hash) % 360)));
		poseStack.rotate(Axis.ZP.rotationDegrees((float)((t / 20D - state.hash) % 360)));
		float s2 = (float)Math.sqrt(s * 1.75);
		poseStack.scale(s2, s2, s2);
		int fill = c;
		float pdist = state.pdist;
		PoseStack.Pose cubePose = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(),
				(unused, out) -> cube.renderFaces(cubePose, out, 0, 0, 0, fill));
		collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(),
				(unused, out) -> cube.renderEdges(cubePose, out, 0, 0, 0, c2, pdist));
		poseStack.popPose();

		if (state.triggered) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0.5 + dy, 0.5);
			poseStack.scale((float)s, (float)s, (float)s);
			poseStack.rotate(camera.orientation);
			int flare = 0xff000000 | state.color;
			TextureAtlasSprite ico = Minecraft.getInstance().getAtlasManager()
					.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.SPINFLARE.getIcon());
			PoseStack.Pose pose = poseStack.last().copy();
			collector.submitCustomGeometry(poseStack,
					ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS), (unused, out) -> {
						out.addVertex(pose, -1, -1, 0).setUv(ico.getU0(), ico.getV0()).setColor(flare);
						out.addVertex(pose, 1, -1, 0).setUv(ico.getU1(), ico.getV0()).setColor(flare);
						out.addVertex(pose, 1, 1, 0).setUv(ico.getU1(), ico.getV1()).setColor(flare);
						out.addVertex(pose, -1, 1, 0).setUv(ico.getU0(), ico.getV1()).setColor(flare);
					});
			poseStack.popPose();
		}
	}

	/** The cube bobs up to ~0.6 above the block centre and spans up to ~0.6 either side. */
	@Override
	public AABB getRenderBoundingBox(TileEntityLaserEffector te) {
		return new AABB(te.getBlockPos()).inflate(1);
	}

	public static final class State extends BlockEntityRenderState {
		private boolean visible;
		private boolean triggered;
		private int color;
		private int hash;
		private float pdist;
	}
}
