package reika.chromaticraft.client;

import java.util.List;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.dye26.BlockDyeLeaf;
import reika.chromaticraft.world.RainbowForestTreeFeature;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;

/**
 * V33a {@code ChromaClientEventController}'s Rainbow Forest log glow: in the forest, the trunk of every
 * dye tree glows in its leaves' colour.
 *
 * <p>Upstream gave every wood block in a Rainbow Forest a second render pass. The pass climbs the trunk
 * through the same kind of log; if the first block past the top is a natural dye leaf, it draws one of
 * five animated glow overlays on each exposed side of the log, full-bright, in that dye's colour, a hair
 * proud of the face. Which overlay a log gets comes from a fine simplex field
 * ({@code RainbowForestGenerator.getWoodGlowIndex}), so the glow varies block to block up the trunk. Any
 * other trunk — a vanilla tree, a rainbow tree, a player-placed log column — does not glow.
 *
 * <p>26.2 has no second pass hook for other blocks, so the equivalent is a wrapper around every pillar
 * block's model that adds those faces. Which pillars are logs is a tag, and tags are not loaded while
 * models bake, so the wrapper is applied to all of them and tests the tag when it meshes. The climb stays
 * inside the section above the log's own, which is as far as a meshing region can read; no dye tree
 * trunk is anywhere near that tall. Whether a log is "in the forest" is the same blended biome probe the
 * water colours use, taken at the halfway point so the edge falls where upstream's hard biome edge did.
 *
 * <p>CHROMA-PORT: upstream skipped the pass while the player's line-of-sight ability view was on
 * ({@code AbilityHelper.isLOSViewEnabled}); that ability is not ported yet.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class RainbowForestLogGlow {

	private static final int OVERLAYS = 5;
	/** Upstream's outward offset. */
	private static final float OFFSET = 0.0025F;

	private RainbowForestLogGlow() {}

	@SubscribeEvent
	public static void wrapLogModels(ModelEvent.ModifyBakingResult event) {
		Material.Baked[] overlays = new Material.Baked[OVERLAYS];
		for (int i = 0; i < OVERLAYS; i++) {
			Identifier id = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dye/tree_overlay/" + i);
			overlays[i] = new Material.Baked(event.getTextureGetter().apply(id), false);
		}
		int flags = 0;
		for (Material.Baked overlay : overlays)
			flags |= Wrapped.face(Direction.NORTH, overlay, -1).materialInfo().flags();
		int overlayFlags = flags;
		event.getBakingResult().blockStateModels().replaceAll((state, model) ->
				state.getBlock() instanceof RotatedPillarBlock ? new Wrapped(model, overlays, overlayFlags) : model);
	}

	private static final class Wrapped extends DelegateBlockStateModel {

		private final Material.Baked[] overlays;
		private final int overlayFlags;

		private Wrapped(BlockStateModel delegate, Material.Baked[] overlays, int overlayFlags) {
			super(delegate);
			this.overlays = overlays;
			this.overlayFlags = overlayFlags;
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
				List<BlockStateModelPart> parts) {
			super.collectParts(level, pos, state, random, parts);
			if (!state.is(BlockTags.LOGS))
				return;
			BlockState top = topOfTrunk(level, pos, state);
			if (top == null || !(top.getBlock() instanceof BlockDyeLeaf leaf) || top.getValue(BlockStateProperties.PERSISTENT))
				return;
			if (OverworldBiomePresence.strength(level, pos, OverworldBiomePresence.RAINBOW_FOREST) < 0.5F)
				return;
			int color = 0xFF000000 | ReikaDyeHelper.dyes[leaf.getElement().ordinal()].getColor();
			Material.Baked overlay = overlays[RainbowForestTreeFeature.getWoodGlowIndex(pos.getX(), pos.getY(), pos.getZ(), OVERLAYS)];
			QuadCollection.Builder quads = new QuadCollection.Builder();
			for (Direction face : Direction.Plane.HORIZONTAL)
				quads.addCulledFace(face, face(face, overlay, color));
			parts.add(new SimpleModelWrapper(quads.build(), false, overlay));
		}

		/**
		 * Up through the same log to the first other block, or null if the trunk runs past what the
		 * meshing region holds (the section above this one).
		 */
		private static BlockState topOfTrunk(BlockAndTintGetter level, BlockPos pos, BlockState log) {
			int limit = Math.min(((pos.getY() >> 4) + 2) * 16 - 1, level.getMaxY());
			BlockPos.MutableBlockPos at = pos.mutable();
			while (at.getY() < limit) {
				at.move(Direction.UP);
				BlockState state = level.getBlockState(at);
				if (!state.is(log.getBlock()))
					return state;
			}
			return null;
		}

		static net.minecraft.client.resources.model.geometry.BakedQuad face(Direction face, Material.Baked overlay, int color) {
			MutableQuad quad = new MutableQuad();
			quad.setCubeFace(face, -OFFSET, 0, -OFFSET, 1 + OFFSET, 1, 1 + OFFSET);
			quad.setSprite(overlay);
			for (int i = 0; i < 4; i++) {
				float along = face.getAxis() == Direction.Axis.X ? quad.z(i) : quad.x(i);
				boolean far = face == Direction.NORTH || face == Direction.EAST ? along < 0.5F : along > 0.5F;
				quad.setUvFromSprite(i, far ? 1 : 0, quad.y(i) > 0.5F ? 0 : 1);
			}
			quad.setColor(color);
			quad.recomputeNormals(false);
			quad.setShade(false);
			quad.setAmbientOcclusion(false);
			quad.setLightEmission(15);
			quad.setTintIndex(-1);
			return quad.toBakedQuad();
		}

		@Override
		public int materialFlags() {
			return super.materialFlags() | overlayFlags;
		}

		@Override
		public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			return this.materialFlags() | super.materialFlags(level, pos, state);
		}
	}
}
