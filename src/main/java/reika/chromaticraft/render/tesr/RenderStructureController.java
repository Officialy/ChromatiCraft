package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.client.ScreenParticleRendering;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderStructControl}: the fragment structure's root is not a solid block but a glowing
 * flare hanging in the air, one block across, facing the viewer and turning on its own.
 *
 * <p>Upstream's flare is {@code ChromaIcons.SPINFLARE}, a terrain-atlas sprite whose 180-frame strip
 * the atlas animates one frame per tick. The port uses the same stitched sprite: everything under
 * {@code textures/block/} is stitched into the block atlas whether or not a model names it, and its
 * {@code .mcmeta} keeps the animation. An in-world, non-monument controller blends it INVERTEDADD.
 *
 * <p>CHROMA-PORT: upstream also draws the monument line ring over this (the
 * {@code monument_lines_big.png} pass and the {@code structcontrol} shader), and gates the whole
 * thing on {@code isVisible}/{@code isMonument}/{@code isInWorld}. None of those flags exist on the
 * ported block entity, so the flare draws unconditionally for now; restore the gating and the
 * monument pass with the monument ritual.
 *
 * <p>V33a's {@code renderDelegate} hands a Biome Fragment's controller to
 * {@code BiomeStructurePuzzle.render}, which warns a viewer who lacks the {@code BIOMESTRUCT}
 * prerequisites (the same check the server's shaft gate makes) that the shaft will turn them back: a
 * multiplied X with a faint alpha copy over it six and three-quarter blocks up, red hive and rift layers
 * around it, and a pulsing red glint dome twelve blocks across the shaft. Client progression is the
 * player's synced death-persistent stage tag, so the overlay goes away as soon as the last prerequisite
 * lands.
 */
public final class RenderStructureController
		implements BlockEntityRenderer<TileEntityStructureController, RenderStructureController.State> {

	/** V33a scales its +-1 quad by 0.5 about the block centre: the flare is exactly one block across. */
	private static final float RADIUS = 0.5F;

	/** {@code BiomeStructurePuzzle.render}'s dome: radius 12, centred half a block over the shaft at y 5.5. */
	private static final double DOME_RADIUS = 12;
	private static final double DOME_X = 0.5;
	private static final double DOME_Y = 5.5;
	private static final double DOME_Z = 0.5;

	public RenderStructureController(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityStructureController controller, State state,
			float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(controller, state, partialTick, cameraPosition, breakProgress);
		CrystalElement colour = controller.getColor();
		state.colour = colour == null ? 0xffffff : colour.color();
		Player viewer = Minecraft.getInstance().player;
		state.biomeGate = controller.getStructureType() == TileEntityStructureController.StructureType.BIOME_FRAGMENT
				&& viewer != null && !ProgressStage.BIOMESTRUCT.playerHasPrerequisites(viewer);
		state.millis = System.currentTimeMillis();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		// V33a turned by -playerViewY then playerViewX: a full billboard, the camera's own rotation.
		poseStack.rotate(camera.orientation);
		TextureAtlasSprite flare = sprite(ChromaIcons.SPINFLARE.getIcon());
		int colour = 0xff000000 | state.colour;
		// Draw with the pose handed to the callback. PoseStack reuses its Pose objects, so one captured
		// here is overwritten by later renders before this deferred submit runs, which put the flare
		// somewhere else entirely.
		submitComposedScene(poseStack, collector, ChromaRenderPipelines.invertedAddSprite(TextureAtlas.LOCATION_BLOCKS),
				(pose, buffer) -> {
					float u = flare.getU0();
					float v = flare.getV0();
					float du = flare.getU1();
					float dv = flare.getV1();
					buffer.addVertex(pose, -RADIUS, -RADIUS, 0).setUv(u, v).setColor(colour);
					buffer.addVertex(pose, RADIUS, -RADIUS, 0).setUv(du, v).setColor(colour);
					buffer.addVertex(pose, RADIUS, RADIUS, 0).setUv(du, dv).setColor(colour);
					buffer.addVertex(pose, -RADIUS, RADIUS, 0).setUv(u, dv).setColor(colour);
				});
		poseStack.popPose();
		// V33a ran the puzzle's render at the block origin, after the flare and outside its billboard turn.
		if (state.biomeGate)
			submitBiomeGate(state, poseStack, collector);
	}

	/**
	 * V33a {@code BiomeStructurePuzzle.render}, pass for pass: MULTIPLY X, DEFAULT X at alpha 64, ADDITIVEDARK hive
	 * pair and rift, then the glint dome, which inherits ADDITIVEDARK because upstream never reset the blend. Each
	 * pass gets its own submit order so they composite in that sequence.
	 */
	private static void submitBiomeGate(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		TextureAtlasSprite x = sprite(ChromaIcons.X.getIcon());
		TextureAtlasSprite hive = sprite(ChromaIcons.HIVE.getIcon());
		TextureAtlasSprite rift = sprite(ChromaIcons.RIFT.getIcon());

		submitComposedScene(poseStack, collector, 1, ChromaRenderPipelines.multiplySprite(TextureAtlas.LOCATION_BLOCKS),
				(pose, buffer) -> layer(buffer, pose, x, 6.75F, 0xffffffff));
		submitComposedScene(poseStack, collector, 2, ChromaRenderPipelines.translucentSprite(TextureAtlas.LOCATION_BLOCKS),
				(pose, buffer) -> layer(buffer, pose, x, 6.75F, 0x40ffffff));
		submitComposedScene(poseStack, collector, 3, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(pose, buffer) -> {
					layer(buffer, pose, hive, 6, 0xff300000);
					layer(buffer, pose, hive, 7, 0xff300000);
					layer(buffer, pose, rift, 6.5F, 0xffff0000);
				});

		long ms = state.millis;
		int colour = 0xff000000 | ReikaColorAPI.mixColors(0xff0000, 0x700000, (float)(0.5+0.5*Math.sin(ms/100D)));
		submitComposedScene(poseStack, collector, 4,
				ChromaRenderPipelines.additiveSprite(ItemFeatureRenderer.ENCHANTED_GLINT_ITEM),
				(pose, buffer) -> dome(buffer, pose, colour, ms));
	}

	/** One of upstream's 3x3 horizontal sheets, from (-1, y, -1) to (2, y, 2), the whole sprite stretched over it. */
	private static void layer(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite ico, float y, int colour) {
		float u = ico.getU0();
		float v = ico.getV0();
		float du = ico.getU1();
		float dv = ico.getV1();
		buffer.addVertex(pose, -1, y, -1).setUv(u, v).setColor(colour);
		buffer.addVertex(pose, 2, y, -1).setUv(du, v).setColor(colour);
		buffer.addVertex(pose, 2, y, 2).setUv(du, dv).setColor(colour);
		buffer.addVertex(pose, -1, y, 2).setUv(u, dv).setColor(colour);
	}

	/**
	 * Upstream's squashed sphere: rings every 0.375 blocks from k = -12 to 12 whose radius is
	 * {@code sqrt((r(1-0.75(k/r)^2))^2 - k^2)}, in ten-degree segments, with the glint scrolling around it once per
	 * 18 seconds and up it at 1/220 per millisecond. The glint texture's own sampler repeats, so the UVs run free.
	 */
	private static void dome(VertexConsumer buffer, PoseStack.Pose pose, int colour, long ms) {
		double r = DOME_RADIUS;
		double dk = 0.5*r/16;
		double di = 10;
		for (double k = -r; k <= r-dk; k += dk) {
			double dr = r*(1-0.75*Math.pow(k/r, 2));
			double dr2 = r*(1-0.75*Math.pow((k+dk)/r, 2));
			double r2 = Math.abs(k) >= r ? 0 : Math.sqrt(Math.max(0, dr*dr-k*k));
			double r3 = Math.abs(k) >= r ? 0 : Math.sqrt(Math.max(0, dr2*dr2-(k+dk)*(k+dk)));
			if (Double.isNaN(r2) || Double.isNaN(r3))
				continue;
			for (int i = 0; i < 360; i += di) {
				double a = Math.toRadians(i);
				double a2 = Math.toRadians(i+di);
				double ti = i+(ms/50D%360);
				double tk = k+(ms/220D%360);
				float u = (float)(ti/360D*3);
				float du = (float)((ti+di)/360D*3);
				float v = (float)(tk*r/1024D);
				float dv = (float)((tk+dk)*r/1024D);
				double s1 = Math.sin(a);
				double s2 = Math.sin(a2);
				double c1 = Math.cos(a);
				double c2 = Math.cos(a2);
				vertex(buffer, pose, DOME_X+r2*c1, DOME_Y+k, DOME_Z+r2*s1, u, v, colour);
				vertex(buffer, pose, DOME_X+r2*c2, DOME_Y+k, DOME_Z+r2*s2, du, v, colour);
				vertex(buffer, pose, DOME_X+r3*c2, DOME_Y+k+dk, DOME_Z+r3*s2, du, dv, colour);
				vertex(buffer, pose, DOME_X+r3*c1, DOME_Y+k+dk, DOME_Z+r3*s1, u, dv, colour);
			}
		}
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z,
			float u, float v, int colour) {
		buffer.addVertex(pose, (float)x, (float)y, (float)z).setUv(u, v).setColor(colour);
	}

	private static TextureAtlasSprite sprite(Identifier id) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}

	private static void submitComposedScene(PoseStack poseStack, SubmitNodeCollector collector,
			RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		submitComposedScene(poseStack, collector, 0, renderType, renderer);
	}

	private static void submitComposedScene(PoseStack poseStack, SubmitNodeCollector collector, int order,
			RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
				poseStack.last().copy(), renderType, renderer);
		collector.order(order)
				.submitSpecial(ScreenParticleRendering.PHASE, submit);
	}

	/** The Biome Fragment's dome reaches twelve blocks out from the shaft; keep it drawn while any of it is in view. */
	@Override
	public AABB getRenderBoundingBox(TileEntityStructureController controller) {
		if (controller.getStructureType() != TileEntityStructureController.StructureType.BIOME_FRAGMENT)
			return BlockEntityRenderer.super.getRenderBoundingBox(controller);
		BlockPos pos = controller.getBlockPos();
		return new AABB(pos.getX()+DOME_X-DOME_RADIUS, pos.getY()+DOME_Y-DOME_RADIUS, pos.getZ()+DOME_Z-DOME_RADIUS,
				pos.getX()+DOME_X+DOME_RADIUS, pos.getY()+DOME_Y+DOME_RADIUS, pos.getZ()+DOME_Z+DOME_RADIUS);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	public static final class State extends BlockEntityRenderState {
		int colour = 0xffffff;
		/** A Biome Fragment, viewed by a client player who lacks the {@code BIOMESTRUCT} prerequisites. */
		boolean biomeGate;
		long millis;
	}
}
