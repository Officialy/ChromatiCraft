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
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelFluidRelay;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.render.ChromaBeam;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;
import reika.dragonapi.instantiable.effects.TruncatedCube;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * V33a {@code RenderFluidRelay}. The model stands on the tank face, rotated from its default downward mounting. Over it
 * a small truncated cube spins and breathes in ADDITIVEDARK grey, hue-cycling pink-to-red while auto-filtering, drawn
 * only within 24 blocks with an outline thinning with distance. Links to other anchors are white beams, each drawn by
 * one of its two ends, that flash briefly every so often and fade fully in while the player holds the Manipulator or
 * an anchor.
 */
public final class RenderFluidRelay implements BlockEntityRenderer<TileEntityFluidRelay, RenderFluidRelay.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "fluid_relay"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/fluid_relay.png");

	private static final TruncatedCube cube = new TruncatedCube(0.01, 0.125);

	/** V33a statics: the shared link-beam fade, advanced once per distinct world tick and partial tick. */
	private static long connectionAlphaTick;
	private static float connectionAlphaPTick;
	private static int connectionAlphaOverride = 0;

	private final ModelFluidRelay model;

	public RenderFluidRelay(BlockEntityRendererProvider.Context context) {
		model = new ModelFluidRelay(context.bakeLayer(MODEL_LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityFluidRelay te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.facing = te.getFacing();
		state.autoFilter = te.autoFilter;
		state.ticks = te.getTicksExisted()+partialTick;
		state.hash = System.identityHashCode(te);
		BlockPos pos = te.getBlockPos();
		state.pdist = (float)cameraPosition.distanceTo(Vec3.atCenterOf(pos));
		state.beams.clear();
		if (te.getLevel() == null || te.getConnections().isEmpty())
			return;
		long time = te.getLevel().getGameTime();
		if (connectionAlphaTick != time || connectionAlphaPTick != partialTick) {
			if (holdingRevealer()) {
				if (connectionAlphaOverride < 255) {
					connectionAlphaOverride = Math.min(255, connectionAlphaOverride+3);
				}
			}
			else if (connectionAlphaOverride > 0)
				connectionAlphaOverride -= 2;
			connectionAlphaTick = time;
			connectionAlphaPTick = partialTick;
		}
		double d = 0.1875;
		for (BlockPos c : te.getConnections()) {
			// V33a: only the anchor with the smaller hash draws the pair's beam.
			if (!(te.getLevel().getBlockEntity(c) instanceof TileEntityFluidRelay o)
					|| System.identityHashCode(o) <= state.hash)
				continue;
			int a = Math.max(connectionAlphaOverride,
					((int)(240*Math.sin(te.getTicksExisted()/32D+state.hash+System.identityHashCode(o)))-230)*24);
			if (a > 0) {
				Direction f1 = te.getFacing();
				Direction f2 = o.getFacing();
				Vec3 start = new Vec3(0.5+d*f1.getStepX(), 0.5+d*f1.getStepY(), 0.5+d*f1.getStepZ());
				Vec3 end = new Vec3(c.getX()-pos.getX()+0.5+d*f2.getStepX(), c.getY()-pos.getY()+0.5+d*f2.getStepY(),
						c.getZ()-pos.getZ()+0.5+d*f2.getStepZ());
				state.beams.add(new Beam(start, end, Math.min(255, a)));
			}
		}
	}

	/** V33a {@code HoldingChecks.MANIPULATOR.isClientHolding()} or holding an anchor. */
	private static boolean holdingRevealer() {
		Player ep = Minecraft.getInstance().player;
		if (ep == null)
			return false;
		ItemStack held = ep.getMainHandItem();
		return held.is(ChromaItems.MANIPULATOR.get()) || held.is(ChromaBlocks.FLUID_RELAY.get().asItem());
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		switch(state.facing) {
			case NORTH -> {
				poseStack.translate(0, 1, 0);
				poseStack.rotate(Axis.XP.rotationDegrees(90));
			}
			case SOUTH -> {
				poseStack.translate(0, 0, 1);
				poseStack.rotate(Axis.XP.rotationDegrees(-90));
			}
			case WEST -> {
				poseStack.translate(0, 1, 0);
				poseStack.rotate(Axis.ZP.rotationDegrees(-90));
			}
			case EAST -> {
				poseStack.translate(1, 0, 0);
				poseStack.rotate(Axis.ZP.rotationDegrees(90));
			}
			case UP -> {
				poseStack.translate(0, 1, 1);
				poseStack.rotate(Axis.XP.rotationDegrees(180));
			}
			case DOWN -> {}
		}
		submitModel(model, poseStack, collector, state.lightCoords);

		int c1 = 0x90909090;
		int c2 = 0xffffffff;
		if (state.autoFilter) {
			int t = (int)((state.ticks*2.4)%360);
			c1 = 0x90000000 | ReikaColorAPI.getModifiedHue(0xffa0a0, t) & 0xffffff;
		}
		if (state.pdist <= 24) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0.325, 0.5);
			long ms = System.currentTimeMillis();
			double ang = (ms/10D+state.hash)%360D;
			poseStack.rotate(Axis.YP.rotationDegrees((float)ang));
			double s = 0.625+0.25*Math.sin(ms/800D-state.hash)+0.1*Math.sin(ms/300D-state.hash);
			poseStack.scale((float)s, (float)s, (float)s);
			PoseStack.Pose pose = poseStack.last().copy();
			int fc1 = c1;
			int fc2 = c2;
			float pdist = state.pdist;
			// Faces in ADDITIVEDARK, the outline as lines; V33a drew both in the one additive pass.
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveColorTriangles(),
					(unused, v5) -> cube.renderFaces(pose, v5, 0, 0, 0, fc1));
			collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(),
					(unused, lines) -> cube.renderEdges(pose, lines, 0, 0, 0, fc2, pdist));
			poseStack.popPose();
		}
		poseStack.popPose();

		for (Beam b : state.beams)
			ChromaBeam.submit(poseStack, collector, b.start, b.end, b.alpha, 0.1875, 0xffffff, 0xffffff);
	}

	/** V33a {@code renderModel}: the model's origin at the block's top centre, flipped. */
	public static void submitModel(ModelFluidRelay model, PoseStack poseStack, SubmitNodeCollector collector, int light) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (unused, vertices) ->
				model.render(modelPose, vertices, light, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	/** V33a {@code getRenderBoundingBox}: the anchor's link range. */
	@Override
	public AABB getRenderBoundingBox(TileEntityFluidRelay te) {
		return new AABB(te.getBlockPos()).inflate(TileEntityFluidRelay.RELAY_RANGE);
	}

	private record Beam(Vec3 start, Vec3 end, int alpha) {}

	public static final class State extends BlockEntityRenderState {
		private Direction facing = Direction.DOWN;
		private boolean autoFilter;
		private float ticks;
		private int hash;
		private float pdist;
		private final List<Beam> beams = new ArrayList<>();
	}

}
