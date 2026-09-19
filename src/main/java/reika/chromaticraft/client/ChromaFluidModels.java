package reika.chromaticraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.fluid.CustomFluidRenderer;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaFluids;

/** V33a Liquid Chroma sprites plus source-state color propagation for the 26.2 fluid model. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ChromaFluidModels {
	private static final Material STILL = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/activechroma"));
	private static final Material LUMA_STILL = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/aether/aether_still_0"));
	private static final Material LUMA_FLOW = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/aether/aether_flow2"));
	private static final Material FLOW = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/activechroma_flowing"));
	private static final Material ENDER_STILL = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/ender"));
	private static final Material ENDER_FLOW = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/flowingender"));

	private ChromaFluidModels() {}

	@SubscribeEvent
	public static void registerFluidModels(RegisterFluidModelsEvent event) {
		FluidModel.Unbaked model = new FluidModel.Unbaked(STILL, FLOW, null, new PoolTint());
		event.register(model, ChromaFluids.CHROMA.get(), ChromaFluids.FLOWING_CHROMA.get());
		// V33a registers aether_full/aether_flow2 on the Fluid itself. Source blocks then replace the
		// still sprite with one cell of the 4x4 star-field mosaic; flowing faces retain flow2.
		event.register(new FluidModel.Unbaked(LUMA_STILL, LUMA_FLOW, null, new UntintedFluid(),
				new LumaRenderer()),
				ChromaFluids.LUMA.get(), ChromaFluids.FLOWING_LUMA.get());
		// V33a BlockLiquidEnder.registerBlockIcons: chromaticraft:fluid/ender and .../flowingender,
		// drawn untinted (the sprites already carry the colour).
		event.register(new FluidModel.Unbaked(ENDER_STILL, ENDER_FLOW, null, new UntintedFluid()),
				ChromaFluids.ENDER.get(), ChromaFluids.FLOWING_ENDER.get());
	}

	private static final class PoolTint implements FluidTintSource {
		@Override
		public int color(FluidState state) {
			return 0xFFFFFFFF;
		}

		@Override
		public int colorInWorld(FluidState fluidState, BlockState blockState,
				BlockAndTintGetter level, BlockPos pos) {
			return findColor(level, pos, blockState.getValue(LiquidBlock.LEVEL), 0);
		}

		private static int findColor(BlockAndTintGetter level, BlockPos pos, int fluidLevel, int depth) {
			if (depth > 16 || !level.getBlockState(pos).is(ChromaBlocks.CHROMA.get())) return 0xFFFFFFFF;
			if (level.getBlockEntity(pos) instanceof TileEntityChroma pool) return pool.getColor();
			BlockPos above = pos.above();
			BlockState aboveState = level.getBlockState(above);
			if (aboveState.is(ChromaBlocks.CHROMA.get())) {
				int color = findColor(level, above, aboveState.getValue(LiquidBlock.LEVEL), depth+1);
				if (color != 0xFFFFFFFF) return color;
			}
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos next = pos.relative(direction);
				BlockState nextState = level.getBlockState(next);
				if (nextState.is(ChromaBlocks.CHROMA.get()) && nextState.getValue(LiquidBlock.LEVEL) < fluidLevel) {
					int color = findColor(level, next, nextState.getValue(LiquidBlock.LEVEL), depth+1);
					if (color != 0xFFFFFFFF) return color;
				}
			}
			return 0xFFFFFFFF;
		}
	}
	/** Luma and Liquid Ender both draw their sprites unmodified; neither had a V33a colour multiplier. */
	private static final class UntintedFluid implements FluidTintSource {
		@Override public int color(FluidState state) { return 0xFFFFFFFF; }
		@Override public int colorInWorld(FluidState fluidState, BlockState blockState,
				BlockAndTintGetter level, BlockPos pos) { return 0xFFFFFFFF; }
	}

	/** Reproduces V33a BlockEtherealLuma#getIcon without replacing vanilla fluid geometry. */
	private static final class LumaRenderer implements CustomFluidRenderer {
		private static final Identifier BASE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"block/fluid/aether/aether_still_0");

		@Override
		public boolean renderFluid(FluidRenderer renderer, FluidState state, BlockAndTintGetter level,
				BlockPos pos, FluidRenderer.Output output, BlockState blockState) {
			var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
			TextureAtlasSprite source = atlas.getSprite(BASE);
			int dx = Math.floorMod(pos.getX(), 4);
			int dz = Math.floorMod(pos.getZ(), 4);
			boolean proxima = Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.dimension().equals(
						reika.chromaticraft.registry.ChromaDimensions.PROXIMA);
			if (proxima) {
				int swap = dx;
				dx = dz;
				dz = swap;
			}
			int index = dz * 4 + dx;
			String name = "block/fluid/aether/aether_still_" + (proxima ? "dim_" : "") + index;
			TextureAtlasSprite target = atlas.getSprite(
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name));
			FluidRenderer.Output remapped = layer -> new VertexConsumerWrapper(output.getBuilder(layer)) {
				@Override
				public VertexConsumer setUv(float u, float v) {
					if (inside(source, u, v)) {
						float unitU = (u - source.getU0()) / (source.getU1() - source.getU0());
						float unitV = (v - source.getV0()) / (source.getV1() - source.getV0());
						return super.setUv(target.getU(unitU), target.getV(unitV));
					}
					return super.setUv(u, v);
				}
			};
			renderer.tesselate(level, pos, remapped, blockState, state);
			return true;
		}

		private static boolean inside(TextureAtlasSprite sprite, float u, float v) {
			float epsilon = 1.0e-6F;
			return u >= sprite.getU0() - epsilon && u <= sprite.getU1() + epsilon
					&& v >= sprite.getV0() - epsilon && v <= sprite.getV1() + epsilon;
		}
	}
}
