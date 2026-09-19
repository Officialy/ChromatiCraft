package reika.chromaticraft.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.networking.TileEntityWeakRepeater;

/** Block-specific V33a behavior for the obtainable wooden repeater frame. */
public final class BlockWeakRepeater extends BlockChromaticTile {
	public static final BooleanProperty RUPTURED = BooleanProperty.create("ruptured");

	public BlockWeakRepeater(Properties properties) {
		super(properties, ChromaTiles.WEAKREPEATER);
		this.registerDefaultState(this.stateDefinition.any().setValue(RUPTURED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RUPTURED);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof TileEntityWeakRepeater repeater
				&& (state.getValue(RUPTURED) || repeater.isRuptured()))
			return repeater.createBrokenDrops();
		return super.getDrops(state, params);
	}

	@Override
	public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos,
			Direction direction) {
		return direction == Direction.UP;
	}
}
