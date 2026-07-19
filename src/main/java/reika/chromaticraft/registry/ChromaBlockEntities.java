package reika.chromaticraft.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.TileEntityDisplayPoint;

/**
 * ChromatiCraft {@link BlockEntityType} registry (mirrors ReactorBlockEntities). One entry per
 * {@link ChromaTiles} tile; grows as TileEntities port. Registered on the mod bus by the main class.
 */
public final class ChromaBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDisplayPoint>> DISPLAY =
			register("display_point", ChromaTiles.DISPLAY);

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
			String name, ChromaTiles tile) {
		// The tile's reflective (BlockPos, BlockState) factory returns the concrete BlockEntity.
		BlockEntityType.BlockEntitySupplier<T> factory = (pos, state) -> (T) tile.createBlockEntity(pos, state);
		return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, tile.getBlock()));
	}

	private ChromaBlockEntities() {}
}
