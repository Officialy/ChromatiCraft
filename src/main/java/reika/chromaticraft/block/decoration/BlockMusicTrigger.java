package reika.chromaticraft.block.decoration;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/** Four-quadrant V33a crystal music trigger, usable by redstone or direct side clicks. */
public final class BlockMusicTrigger extends Block {

	public interface Handler {
		void onMusicTrigger(BlockPos triggerPos, CrystalElement element, MusicKey key,
				@Nullable Player player);
	}

	private final MapCodec<BlockMusicTrigger> codec = MapCodec.unit(this);

	public BlockMusicTrigger(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override public MapCodec<? extends BlockMusicTrigger> codec() { return codec; }

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			@Nullable Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide() && level.hasNeighborSignal(pos))
			ping(level, pos, Math.clamp(level.getBestNeighborSignal(pos) / 4, 0, 3), null);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		int index = quadrant(pos, hit);
		if (index >= 0 && !level.isClientSide()) ping(level, pos, index, player);
		return InteractionResult.SUCCESS;
	}

	private static void ping(Level level, BlockPos pos, int index, @Nullable Player player) {
		BlockState crystalState = level.getBlockState(pos.above());
		CrystalElement element;
		if (crystalState.getBlock() instanceof reika.chromaticraft.base.CrystalBlock crystal)
			element = crystal.getCrystalElement(crystalState);
		else if (crystalState.getBlock() instanceof CrystalTypeBlock crystal)
			element = crystal.getCrystalElement(crystalState);
		else return;
		MusicKey key = CrystalMusicManager.instance.getKeys(element).get(index);
		ChromaSounds.DING.playSoundAtBlock(level, pos, 1,
				CrystalMusicManager.instance.getScaledDing(element, index));
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2 * i / 8D;
			level.addParticle(new DustParticleOptions(element.getColor(), 1F), pos.getX() + 0.5,
					pos.getY() + 1.5, pos.getZ() + 0.5, Math.cos(angle) * 0.08, 0.04,
					Math.sin(angle) * 0.08);
		}
		Handler handler = findHandler(level, pos);
		if (handler != null) handler.onMusicTrigger(pos, element, key, player);
	}

	/** V33a searches its authored controller offsets; a bounded BE scan is rotation-safe in NBT. */
	private static @Nullable Handler findHandler(Level level, BlockPos pos) {
		for (BlockPos check : BlockPos.betweenClosed(pos.offset(-7, -3, -7), pos.offset(7, 1, 7)))
			if (level.getBlockEntity(check) instanceof Handler handler) return handler;
		return null;
	}

	private static int quadrant(BlockPos pos, BlockHitResult hit) {
		Direction side = hit.getDirection();
		if (side.getAxis().isVertical()) return -1;
		Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		double horizontal = side.getAxis() == Direction.Axis.X ? local.z : local.x;
		double vertical = local.y;
		if (side == Direction.NORTH || side == Direction.EAST) horizontal = 1 - horizontal;
		boolean lowHorizontal = horizontal >= 0.125 && horizontal <= 0.4375;
		boolean highHorizontal = horizontal >= 0.5625 && horizontal <= 0.875;
		boolean lowVertical = vertical >= 0.125 && vertical <= 0.4375;
		boolean highVertical = vertical >= 0.5625 && vertical <= 0.875;
		if (lowHorizontal && highVertical) return 0;
		if (highHorizontal && highVertical) return 1;
		if (highHorizontal && lowVertical) return 2;
		if (lowHorizontal && lowVertical) return 3;
		return -1;
	}
}
