package reika.chromaticraft.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
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
			ChromatiCraft.MODID, "block/fluid/aether/aether_still"));
	private static final Material LUMA_FLOW = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/aether/aether_flow"));
	private static final Material FLOW = new Material(Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/fluid/activechroma_flowing"));

	private ChromaFluidModels() {}

	@SubscribeEvent
	public static void registerFluidModels(RegisterFluidModelsEvent event) {
		FluidModel.Unbaked model = new FluidModel.Unbaked(STILL, FLOW, null, new PoolTint());
		event.register(model, ChromaFluids.CHROMA.get(), ChromaFluids.FLOWING_CHROMA.get());
		event.register(new FluidModel.Unbaked(LUMA_STILL, LUMA_FLOW, null, new LumaTint()),
				ChromaFluids.LUMA.get(), ChromaFluids.FLOWING_LUMA.get());
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
	private static final class LumaTint implements FluidTintSource {
		@Override public int color(FluidState state) { return 0xFFFFFFFF; }
		@Override public int colorInWorld(FluidState fluidState, BlockState blockState,
				BlockAndTintGetter level, BlockPos pos) { return 0xFFFFFFFF; }
	}
}
