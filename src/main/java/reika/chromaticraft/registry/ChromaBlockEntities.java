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
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.block.BlockEncrustedCrystal.TileCrystalEncrusted;
import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.tileentity.networking.TileEntityCompoundRepeater;
import reika.chromaticraft.tileentity.networking.TileEntityPylonLink;
import reika.chromaticraft.tileentity.networking.TileEntityCreativeSource;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;

/**
 * ChromatiCraft {@link BlockEntityType} registry (mirrors ReactorBlockEntities). One entry per
 * {@link ChromaTiles} tile; grows as TileEntities port. Registered on the mod bus by the main class.
 */
public final class ChromaBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDisplayPoint>> DISPLAY =
			register("display_point", ChromaTiles.DISPLAY);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalPylon>> PYLON =
			register("pylon", ChromaTiles.PYLON);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalRepeater>> REPEATER =
			register("crystal_repeater", ChromaTiles.REPEATER);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntitySkypeater>> SKYPEATER =
			register("skypeater", ChromaTiles.SKYPEATER);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCreativeSource>> CREATIVEPYLON =
			register("creative_pylon", ChromaTiles.CREATIVEPYLON);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCompoundRepeater>> COMPOUND =
			register("compound_repeater", ChromaTiles.COMPOUND);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityPylonLink>> PYLON_LINK =
			register("pylon_link", ChromaTiles.PYLONLINK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityItemStand>> ITEM_STAND =
			register("casting_item_stand", ChromaTiles.STAND);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCastingTable>> CASTING_TABLE =
			register("casting_table", ChromaTiles.TABLE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityFocusCrystal>> FOCUS_CRYSTAL =
			register("focus_crystal", ChromaTiles.FOCUSCRYSTAL);



	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityChromaCrystal>> POWER_CRYSTAL =
			register("power_crystal", ChromaTiles.CRYSTAL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrystalEncrusted>> ENCRUSTED =
			BLOCK_ENTITIES.register("encrusted_crystal",
					() -> new BlockEntityType<>(TileCrystalEncrusted::new, ChromaBlocks.ENCRUSTED_CRYSTALS.stream()
						.map(holder -> (net.minecraft.world.level.block.Block)holder.get())
						.toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityChroma>> CHROMA_POOL =
			BLOCK_ENTITIES.register("liquid_chroma",
					() -> new BlockEntityType<>(TileEntityChroma::new, ChromaBlocks.CHROMA.get()));

	@SuppressWarnings("unchecked")

	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
			String name, ChromaTiles tile) {
		// The tile's reflective (BlockPos, BlockState) factory returns the concrete BlockEntity.
		BlockEntityType.BlockEntitySupplier<T> factory = (pos, state) -> (T) tile.createBlockEntity(pos, state);
		return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, tile.getBlock()));
	}

	private ChromaBlockEntities() {}
}
