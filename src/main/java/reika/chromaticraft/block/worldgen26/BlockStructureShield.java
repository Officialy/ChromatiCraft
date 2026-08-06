package reika.chromaticraft.block.worldgen26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;

import reika.chromaticraft.registry.ChromaShieldTypes;

/**
 * V33a {@code BlockStructureShield}: the material ChromatiCraft's worldgen structures are built from.
 *
 * <p>Each material is its own registered block (see {@link ChromaShieldTypes}); the
 * {@link #REINFORCED} flag is V33a's metadata bit 3. A reinforced block is unbreakable unless its
 * material is one of the cracked ones, cannot be moved by pistons, and resists blasts — that is what
 * stops players tunnelling into a structure rather than solving it. Breaking one yields the plain
 * form of the same material, which is V33a's {@code damageDropped = meta % 8}.
 */
public class BlockStructureShield extends Block {

	/** V33a metadata bit 3: this block belongs to a structure rather than a player's inventory. */
	public static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");

	private final ChromaShieldTypes type;
	private final MapCodec<BlockStructureShield> codec = MapCodec.unit(this);

	public BlockStructureShield(BlockBehaviour.Properties properties, ChromaShieldTypes type) {
		super(properties);
		this.type = type;
		registerDefaultState(stateDefinition.any().setValue(REINFORCED, false));
	}

	@Override
	public MapCodec<? extends BlockStructureShield> codec() {
		return codec;
	}

	public ChromaShieldTypes getShieldType() {
		return type;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(REINFORCED);
	}

	/**
	 * V33a getBlockHardness: a reinforced, non-cracked shield returns -1, which is vanilla's
	 * "unbreakable" sentinel and is why these read as bedrock inside a structure.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (state.getValue(REINFORCED) && !type.isMineable())
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	/** V33a getMobilityFlag: structure blocks are not piston-movable. */
	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return state.getValue(REINFORCED) ? PushReaction.BLOCK : PushReaction.NORMAL;
	}

	// V33a getLightOpacity returns 0 only for Glass. 26.2 has no getLightBlock override; occlusion
	// comes from the block properties, where the transparent materials are given noOcclusion().

	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbour, net.minecraft.core.Direction side) {
		return neighbour.is(this) || super.skipRendering(state, neighbour, side);
	}

	/**
	 * V33a isUnbreakable, consumed by DragonAPI's SemiUnbreakable contract: explosions and any other
	 * removal path must respect the same rule the hardness does.
	 */
	public boolean isUnbreakable(BlockState state) {
		return state.getValue(REINFORCED) && !type.isMineable();
	}

	// V33a setResistance(6000) is carried by the block properties rather than an override.

	/** The plain form of this material, which is what a reinforced block drops. */
	public BlockState plainState() {
		return this.defaultBlockState().setValue(REINFORCED, false);
	}
}
