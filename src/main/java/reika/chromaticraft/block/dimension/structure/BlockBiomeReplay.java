package reika.chromaticraft.block.dimension.structure;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.tileentity.TileEntityStructureController;

/** Modern NBT-safe form of the V33a structure-data callback that replays the Biome melody. */
public final class BlockBiomeReplay extends Block {

	private final MapCodec<BlockBiomeReplay> codec = MapCodec.unit(this);

	public BlockBiomeReplay(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override public MapCodec<? extends BlockBiomeReplay> codec() { return codec; }

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			TileEntityStructureController controller = findController(level, pos);
			if (controller != null) controller.triggerBiomeMelody();
		}
		return InteractionResult.SUCCESS;
	}

	private static TileEntityStructureController findController(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos.below(10)) instanceof TileEntityStructureController controller)
			return controller;
		// Retains the callback through NBT rotation or hand relocation without global structure state.
		for (BlockPos check : BlockPos.betweenClosed(pos.offset(-1, -12, -1), pos.offset(1, -8, 1)))
			if (level.getBlockEntity(check) instanceof TileEntityStructureController controller)
				return controller;
		return null;
	}
}
