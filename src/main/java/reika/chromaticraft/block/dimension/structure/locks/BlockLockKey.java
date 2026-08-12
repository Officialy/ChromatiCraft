package reika.chromaticraft.block.dimension.structure.locks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLockKey;

/**
 * V33a's portable structure lock key. The old metadata was a generated room channel, so it is an
 * explicit runtime blockstate value rather than sixteen new content registrations.
 */
public final class BlockLockKey extends Block implements EntityBlock {

	public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 7);
	private final MapCodec<BlockLockKey> codec = MapCodec.unit(this);

	public BlockLockKey(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CHANNEL, 0));
	}

	@Override public MapCodec<? extends BlockLockKey> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CHANNEL);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLockKey(pos, state);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof TileEntityLockKey key && !key.canPlayerAccess(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof TileEntityLockKey key))
			return;
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		int channel = Math.clamp(tag.getIntOr("channel", state.getValue(CHANNEL)), 0, 7);
		if (channel != state.getValue(CHANNEL))
			level.setBlock(pos, state.setValue(CHANNEL, channel), Block.UPDATE_ALL);
		key.readPortableData(tag);
		key.notifyDelegate(true, placer instanceof Player player ? player : null);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityLockKey key) {
			key.notifyDelegate(false, player);
			if (!player.isCreative())
				popResource(level, pos, key.createPortableStack(state.getValue(CHANNEL)));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos,
			Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
		if (level.getBlockEntity(pos) instanceof TileEntityLockKey key)
			key.notifyDelegate(false, null);
		super.onExplosionHit(state, level, pos, explosion, onHit);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
			boolean includeData) {
		return level.getBlockEntity(pos) instanceof TileEntityLockKey key
				? key.createPortableStack(state.getValue(CHANNEL)) : new ItemStack(this);
	}

	/** Returns the rune touching this key, matching V33a's six-face scan. */
	public static @Nullable CrystalElement adjacentRune(LevelReader level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockState adjacent = level.getBlockState(pos.relative(direction));
			if (adjacent.getBlock() instanceof BlockCrystalRune rune)
				return rune.getColor();
		}
		return null;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int count = 2 + random.nextInt(2);
		for (int i = 0; i < count; i++)
			level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(),
					pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
	}
}
