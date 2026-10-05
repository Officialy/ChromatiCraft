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
import net.minecraft.client.model.geom.ModelLayerLocation;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelFocusCrystal;
import reika.chromaticraft.models.ModelFocusCrystalBase;
import reika.chromaticraft.models.ModelFocusCrystalMessy;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal.CrystalTier;
import reika.dragonapi.instantiable.effects.TruncatedCube;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderFocusCrystal}. The solid pass draws the tier's stone base plate (none for Flawed) and the
 * ring of shards (the disordered model below Refined), each shard shimmering towards white on its own cycle.
 * The translucent pass adds, in the world only, a bobbing and tumbling truncated cube in the tier colour, a
 * camera-facing star flare, and the beam to the accelerated machine.
 */
public final class RenderFocusCrystal
		implements BlockEntityRenderer<TileEntityFocusCrystal, RenderFocusCrystal.State> {

	public static final ModelLayerLocation ORDERED_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "focus_crystal"), "ordered");
	public static final ModelLayerLocation MESSY_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "focus_crystal"), "messy");

	private static final Identifier STARFLARE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/starflare");
	private static final Identifier LASER = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/laser_2");

	private static final TruncatedCube cube = new TruncatedCube(0.005, 0.15);

	private final ModelFocusCrystal ordered;
	private final ModelFocusCrystalMessy unordered;

	public RenderFocusCrystal(BlockEntityRendererProvider.Context context) {
		ordered = new ModelFocusCrystal(context.bakeLayer(ORDERED_LAYER));
		unordered = new ModelFocusCrystalMessy(context.bakeLayer(MESSY_LAYER));
	}

	/** {@code Textures/TileEntity/focuscrystal<suffix>.png}. */
	public static Identifier texture(CrystalTier tier) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"textures/entity/focus_crystal" + tier.textureSuffix() + ".png");
	}

	/**
	 * V33a {@code FocusCrystalRecipes.getBaseRenderIcon}: the top icon of the slab the tier's recipe is built on
	 * -- stone slab, chiseled quartz, engraved crystalline stone -- or none for Flawed.
	 */
	public static @Nullable Identifier baseIcon(CrystalTier tier) {
		return switch (tier) {
			case DEFAULT -> Identifier.withDefaultNamespace("block/smooth_stone");
			case REFINED -> Identifier.withDefaultNamespace("block/chiseled_quartz_block_top");
			case EXQUISITE, TURBOCHARGED -> Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/pylon/block_7");
			default -> null;
		};
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityFocusCrystal te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.tier = te.getTier();
		state.identity = te;
		int hash = System.identityHashCode(te);
		state.t = te.getTicksExisted() + partialTick + hash % 512000 + state.tier.ordinal() * 15;
		state.n = 12 + 2 * Math.sin(hash % (Math.PI * 2));
		state.pdist = (float)Math.sqrt(cameraPosition.distanceToSqr(Vec3.atCenterOf(te.getBlockPos())));
		TileEntityFocusCrystal.FocusConnection c = te.getConnection();
		state.beamEnd = c == null ? null : new Vec3(-c.relativeLocation().getX() + 0.5,
				-c.relativeLocation().getY(), -c.relativeLocation().getZ() + 0.5);
		state.beamWidth = 0.375F + (float)(0.25 * Math.sin(hash + System.currentTimeMillis() / 800D));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		ModelFocusCrystalBase model = state.tier.usesOrganizedModel() ? ordered : unordered;
		submitSolid(poseStack, collector, model, state.tier, state.identity, state.t, state.lightCoords);

		double dy = 0.125 * Math.sin((state.t / state.n) % (Math.PI * 2));
		submitBobber(state, poseStack, collector, dy);
		submitFlare(state, poseStack, collector, camera, dy);
		if (state.beamEnd != null)
			submitBeam(state, poseStack, collector);
	}

	/** The base plate and the shard model: all V33a drew outside the world (items, the structure preview). */
	public static void submitSolid(PoseStack poseStack, SubmitNodeCollector collector, ModelFocusCrystalBase model,
			CrystalTier tier, Object identity, float t, int light) {
		Identifier icon = baseIcon(tier);
		if (icon != null) {
			TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(icon);
			PoseStack.Pose pose = poseStack.last().copy();
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.unlitEntityCutout(TextureAtlas.LOCATION_BLOCKS),
					(unused, out) -> base(pose, out, sprite, light));
		}
		poseStack.pushPose();
		poseStack.translate(0.5, 1.625, 0.5);
		poseStack.rotate(Axis.XP.rotationDegrees(180));
		PoseStack modelPose = copy(poseStack);
		int color = tier.renderColor(t);
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.unlitEntityCutout(texture(tier)),
				(unused, out) -> model.render(modelPose, out, light, identity, t, color));
		poseStack.popPose();
	}

	/** V33a {@code renderBase}: a 2px slab with the icon on top and a 2px-wide strip of it on each side. */
	private static void base(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite ico, int light) {
		float h = 0.125F;
		float u = ico.getU0();
		float v = ico.getV0();
		float du = ico.getU1();
		float dv = ico.getV1();
		float u2 = ico.getU(1 / 16F);
		float du2 = ico.getU(3 / 16F);
		int top = 0xffffffff;
		int bottom = 0xff7f7f7f;
		int sideX = 0xff9f9f9f;
		int sideZ = 0xffbfbfbf;

		quad(pose, out, light, top, 0, h, 1, u, dv, 1, h, 1, du, dv, 1, h, 0, du, v, 0, h, 0, u, v);
		quad(pose, out, light, bottom, 0, 0, 0, u, v, 1, 0, 0, du, v, 1, 0, 1, du, dv, 0, 0, 1, u, dv);

		quad(pose, out, light, sideX, 0, 0, 1, u2, dv, 0, h, 1, du2, dv, 0, h, 0, du2, v, 0, 0, 0, u2, v);
		quad(pose, out, light, sideX, 1, 0, 0, u2, v, 1, h, 0, du2, v, 1, h, 1, du2, dv, 1, 0, 1, u2, dv);

		quad(pose, out, light, sideZ, 0, 0, 0, u2, v, 0, h, 0, du2, v, 1, h, 0, du2, dv, 1, 0, 0, u2, dv);
		quad(pose, out, light, sideZ, 1, 0, 1, u2, dv, 1, h, 1, du2, dv, 0, h, 1, du2, v, 0, 0, 1, u2, v);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out, int light, int color, float... d) {
		for (int i = 0; i < 4; i++) {
			int k = i * 5;
			out.addVertex(pose, d[k], d[k + 1], d[k + 2]).setColor(color).setUv(d[k + 3], d[k + 4])
					.setLight(light).setNormal(pose, 0, 1, 0);
		}
	}

	/** V33a {@code renderBobber}: the tumbling cube, sized by tier, in the tier colour at 192 alpha. */
	private static void submitBobber(State state, PoseStack poseStack, SubmitNodeCollector collector, double dy) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5 + dy, 0.5);
		float ang = (float)((100 * state.t / (state.n * state.n)) % 360D);
		poseStack.rotate(Axis.XP.rotationDegrees(ang));
		poseStack.rotate(Axis.YP.rotationDegrees(ang));
		poseStack.rotate(Axis.ZP.rotationDegrees(ang));
		float s = (1 + state.tier.effectiveOrdinal()) / 4F;
		poseStack.scale(s, s, s);
		int c0 = state.tier.renderColor(state.t);
		int c = (c0 & 0xffffff) | (192 << 24);
		int c2 = ReikaColorAPI.mixColors(c, 0xffffff | (192 << 24), 0.75F);
		if ((c0 & 0xffffff) == 0xffffff)
			c = (c & 0xff000000) | 0xbfbfbf;
		int fill = c;
		PoseStack.Pose pose = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(),
				(unused, out) -> cube.renderFaces(pose, out, 0, 0, 0, fill));
		float pdist = state.pdist;
		collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(),
				(unused, out) -> cube.renderEdges(pose, out, 0, 0, 0, c2, pdist));
		poseStack.popPose();
	}

	/** V33a {@code renderFlare}: the STARFLARE icon facing the camera, dimmed to 75% of the tier colour. */
	private static void submitFlare(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, double dy) {
		poseStack.pushPose();
		double s = 0.75 * (0.25 + (1 + state.tier.ordinal()) / 4D);
		poseStack.translate(0.5, 0.5 + dy, 0.5);
		poseStack.scale((float)s, (float)s, (float)s);
		poseStack.rotate(camera.orientation);
		int c = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(state.tier.renderColor(state.t), 0.75F);
		TextureAtlasSprite ico = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(STARFLARE);
		PoseStack.Pose pose = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(unused, out) -> {
					out.addVertex(pose, -1, -1, 0).setUv(ico.getU0(), ico.getV0()).setColor(c);
					out.addVertex(pose, 1, -1, 0).setUv(ico.getU1(), ico.getV0()).setColor(c);
					out.addVertex(pose, 1, 1, 0).setUv(ico.getU1(), ico.getV1()).setColor(c);
					out.addVertex(pose, -1, 1, 0).setUv(ico.getU0(), ico.getV1()).setColor(c);
				});
		poseStack.popPose();
	}

	/**
	 * V33a {@code renderLines} -> {@code ChromaFX.renderBeam(0.5, 0.25, 0.5, target, t, 255, width)}: a white line
	 * core and three crossed LASER planes, in whole-block sections plus the remainder.
	 */
	private static void submitBeam(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		Vec3 start = new Vec3(0.5, 0.25, 0.5);
		Vec3 end = state.beamEnd;
		PoseStack.Pose pose = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (unused, out) -> {
			Vec3 d = end.subtract(start).normalize();
			out.addVertex(pose, (float)start.x, (float)start.y, (float)start.z).setColor(0xffffffff)
					.setNormal(pose, (float)d.x, (float)d.y, (float)d.z).setLineWidth(2F);
			out.addVertex(pose, (float)end.x, (float)end.y, (float)end.z).setColor(0xffffffff)
					.setNormal(pose, (float)d.x, (float)d.y, (float)d.z).setLineWidth(2F);
		});
		TextureAtlasSprite laser = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(LASER);
		float h = state.beamWidth;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(unused, out) -> {
					double dd = start.distanceTo(end);
					int n = (int)dd;
					for (int i = 0; i < n; i++)
						beamSection(pose, out, laser, start.lerp(end, i / dd), start.lerp(end, (i + 1) / dd), h);
					if (dd - n > 0)
						beamSection(pose, out, laser, start.lerp(end, n / dd), end, h);
				});
	}

	private static void beamSection(PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite ico, Vec3 a, Vec3 b, float h) {
		int c = 0xffffffff;
		float[][] axes = {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}};
		for (float[] ax : axes) {
			float ox = ax[0] * h;
			float oy = ax[1] * h;
			float oz = ax[2] * h;
			out.addVertex(pose, (float)a.x - ox, (float)a.y - oy, (float)a.z - oz).setUv(ico.getU0(), ico.getV0()).setColor(c);
			out.addVertex(pose, (float)a.x + ox, (float)a.y + oy, (float)a.z + oz).setUv(ico.getU0(), ico.getV1()).setColor(c);
			out.addVertex(pose, (float)b.x + ox, (float)b.y + oy, (float)b.z + oz).setUv(ico.getU1(), ico.getV1()).setColor(c);
			out.addVertex(pose, (float)b.x - ox, (float)b.y - oy, (float)b.z - oz).setUv(ico.getU1(), ico.getV0()).setColor(c);
		}
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityFocusCrystal te) {
		AABB box = new AABB(te.getBlockPos()).inflate(1);
		TileEntityFocusCrystal.FocusConnection c = te.getConnection();
		return c == null ? box : box.minmax(new AABB(c.target()));
	}

	public static final class State extends BlockEntityRenderState {
		private CrystalTier tier = CrystalTier.FLAWED;
		private Object identity;
		private float t;
		private double n;
		private float pdist;
		private @Nullable Vec3 beamEnd;
		private float beamWidth;
	}

}
