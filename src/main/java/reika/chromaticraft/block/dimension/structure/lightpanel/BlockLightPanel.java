package reika.chromaticraft.block.dimension.structure.lightpanel;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import reika.chromaticraft.world.dimension.structure.lightpanel.LightType;

/** V33a Light Panel with its packed type/active metadata split into explicit state. */
public final class BlockLightPanel extends Block {

	public static final EnumProperty<LightType> TYPE = EnumProperty.create("type", LightType.class);
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private final MapCodec<BlockLightPanel> codec = MapCodec.unit(this);

	public BlockLightPanel(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, LightType.TARGET).setValue(ACTIVE, false));
	}

	@Override public MapCodec<? extends BlockLightPanel> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE, ACTIVE);
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(ACTIVE) ? 15 : 0;
	}

	public static void activate(Level level, BlockPos pos, boolean active) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof BlockLightPanel && state.getValue(ACTIVE) != active)
			level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
	}
}
