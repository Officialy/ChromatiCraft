package reika.chromaticraft.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a PARTICLES, a {@code BlockModelledChromaTile} entry: intangible (no collision; a quarter-block hitbox at the
 * centre), drawn only by its renderer. Right-clicked with a book it copies its program into the book, or with a
 * programmed book loads the program from it; with a fluid container it spawns that fluid; otherwise, and after the book,
 * it opens its GUI. Sneaking does nothing, so blocks can be placed against it.
 */
public final class BlockParticleSpawner extends BlockChromaticTile {

	private static final VoxelShape HITBOX = Block.box(6, 6, 6, 10, 10, 10);

	public BlockParticleSpawner(Properties properties) {
		super(properties, ChromaTiles.PARTICLES);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return HITBOX;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack is, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.isShiftKeyDown())
			return InteractionResult.PASS;
		if (!(level.getBlockEntity(pos) instanceof TileEntityParticleSpawner tp))
			return InteractionResult.PASS;
		if (is.is(Items.BOOK)) {
			if (!level.isClientSide()) {
				CompoundTag tag = ReikaItemHelper.getStackTag(is);
				if (tag != null && tag.contains("particleprogram")) {
					tp.readCopyableData(tag.getCompoundOrEmpty("particleprogram"));
					level.playSound(null, pos, SoundType.STONE.getPlaceSound(), SoundSource.BLOCKS, 1, 1);
				}
				else {
					// V33a replaced the book's whole tag with the program.
					CompoundTag data = new CompoundTag();
					CompoundTag program = new CompoundTag();
					tp.writeCopyableData(program);
					data.put("particleprogram", program);
					ReikaItemHelper.setStackTag(is, data);
				}
			}
		}
		else {
			Optional<FluidStack> fs = FluidUtil.getFluidContained(is);
			if (fs.isPresent() && !fs.get().isEmpty()) {
				if (!level.isClientSide())
					tp.markAsFluid(fs.get().getFluid());
				return InteractionResult.SUCCESS;
			}
		}
		return this.open(level, pos, player, tp);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown())
			return InteractionResult.PASS;
		if (!(level.getBlockEntity(pos) instanceof TileEntityParticleSpawner tp))
			return InteractionResult.PASS;
		return this.open(level, pos, player, tp);
	}

	private InteractionResult open(Level level, BlockPos pos, Player player, TileEntityParticleSpawner tp) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && tp.isPlayerAccessible(player))
			serverPlayer.openMenu(tp, pos);
		return InteractionResult.SUCCESS;
	}
}
