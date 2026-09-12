package reika.chromaticraft.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.TileEntityDisplayPoint;
import reika.chromaticraft.tileentity.TileEntityDataNode;
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
import reika.chromaticraft.tileentity.recipe.TileEntityItemInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityPlayerInfuser;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.chromaticraft.tileentity.aoe.TileEntityWarpNode;
import reika.chromaticraft.tileentity.TileEntityDummyAux;
import reika.chromaticraft.tileentity.TileEntityCrystalPortal;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;
import reika.chromaticraft.tileentity.TileEntityHeatLamp;
import reika.chromaticraft.tileentity.TileEntityLightSwitch;
import reika.chromaticraft.tileentity.TileEntityLockKey;
import reika.chromaticraft.tileentity.TileEntityColorLock;

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

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay>> FUNCTION_RELAY =
			register("function_relay", ChromaTiles.FUNCTIONRELAY);
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
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalCharger>> CRYSTAL_CHARGER =
			register("crystal_charger", ChromaTiles.CHARGER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityItemInfuser>> ITEM_INFUSER =
			register("item_aura_infuser", ChromaTiles.INFUSER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityPlayerInfuser>> PLAYER_INFUSER =
			register("player_aura_infuser", ChromaTiles.PLAYERINFUSER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityFocusCrystal>> FOCUS_CRYSTAL =
			register("focus_crystal", ChromaTiles.FOCUSCRYSTAL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDataNode>> DATA_NODE =
			register("data_node", ChromaTiles.DATANODE);



	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityChromaCrystal>> POWER_CRYSTAL =
			register("power_crystal", ChromaTiles.CRYSTAL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrystalEncrusted>> ENCRUSTED =
			BLOCK_ENTITIES.register("encrusted_crystal",
					() -> new BlockEntityType<>(TileCrystalEncrusted::new, ChromaBlocks.ENCRUSTED_CRYSTALS.stream()
						.map(holder -> (net.minecraft.world.level.block.Block)holder.get())
						.toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>,
			BlockEntityType<reika.chromaticraft.tileentity.technical.TileEntityDimensionCore>> DIMENSION_CORE =
			BLOCK_ENTITIES.register("dimension_core",
					() -> new BlockEntityType<>(
							reika.chromaticraft.tileentity.technical.TileEntityDimensionCore::new,
							// One registry identity per element, so the type has to accept all sixteen.
							ChromaBlocks.DIMENSION_CORES.values().stream()
									.map(net.neoforged.neoforge.registries.DeferredHolder::get)
									.toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>,
			BlockEntityType<reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint>> AURA_POINT =
			BLOCK_ENTITIES.register("aura_point",
					() -> new BlockEntityType<>(
							reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint::new,
							ChromaBlocks.AURA_POINT.get()));
	public static final DeferredHolder<BlockEntityType<?>,
			BlockEntityType<reika.chromaticraft.tileentity.dimension.TileEntityFireJet>> FIRE_JET =
			BLOCK_ENTITIES.register("fire_jet",
					() -> new BlockEntityType<>(
							reika.chromaticraft.tileentity.dimension.TileEntityFireJet::new,
							ChromaBlocks.FIRE_JET.get()));
	public static final DeferredHolder<BlockEntityType<?>,
			BlockEntityType<reika.chromaticraft.tileentity.dimension.TileEntityGlowingCracks>> GLOWING_CRACKS =
			BLOCK_ENTITIES.register("glowing_cracks",
					() -> new BlockEntityType<>(
							reika.chromaticraft.tileentity.dimension.TileEntityGlowingCracks::new,
							ChromaBlocks.GLOWING_CRACKS.get()));

	public static final DeferredHolder<BlockEntityType<?>,
			BlockEntityType<reika.chromaticraft.tileentity.dimension.TileEntityVoidRift>> VOID_RIFT =
			BLOCK_ENTITIES.register("void_rift",
					() -> new BlockEntityType<>(
							reika.chromaticraft.tileentity.dimension.TileEntityVoidRift::new,
							ChromaBlocks.VOID_RIFTS.values().stream()
									.map(net.neoforged.neoforge.registries.DeferredHolder::get)
									.toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityChroma>> CHROMA_POOL =
			BLOCK_ENTITIES.register("liquid_chroma",
					() -> new BlockEntityType<>(TileEntityChroma::new, ChromaBlocks.CHROMA.get()));

	@SuppressWarnings("unchecked")

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityLootChest>> LOOT_CHEST =
			BLOCK_ENTITIES.register("loot_chest", () -> new BlockEntityType<>(
					TileEntityLootChest::new, ChromaBlocks.LOOT_CHEST.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityStructureController>> STRUCTURE_CONTROLLER =
			BLOCK_ENTITIES.register("structure_controller", () -> new BlockEntityType<>(
					TileEntityStructureController::new, ChromaBlocks.STRUCTURE_CONTROLLER.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityChromaDoor>> CHROMA_DOOR =
			BLOCK_ENTITIES.register("chroma_door", () -> new BlockEntityType<>(
					TileEntityChromaDoor::new, ChromaBlocks.CHROMA_DOOR.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityHeatLamp>> HEAT_LAMP =
			BLOCK_ENTITIES.register("heat_lamp", () -> new BlockEntityType<>(
					TileEntityHeatLamp::new, ChromaBlocks.HEAT_LAMP.get(), ChromaBlocks.COLD_LAMP.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDummyAux>> DUMMY_AUX =
			BLOCK_ENTITIES.register("dummy_aux", () -> new BlockEntityType<>(
					TileEntityDummyAux::new, ChromaBlocks.DUMMY_AUX.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityLightSwitch>> LIGHT_SWITCH =
			BLOCK_ENTITIES.register("panel_switch", () -> new BlockEntityType<>(
					TileEntityLightSwitch::new, ChromaBlocks.PANEL_SWITCH.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityColorLock>> COLOR_LOCK =
			BLOCK_ENTITIES.register("color_lock", () -> new BlockEntityType<>(
					TileEntityColorLock::new, ChromaBlocks.COLOR_LOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityLockKey>> LOCK_KEY =
			BLOCK_ENTITIES.register("lock_key", () -> new BlockEntityType<>(
					TileEntityLockKey::new, ChromaBlocks.LOCK_KEY.get()));

	/**
	 * Both Portal Rift identities share one entity type: V33a had one block and read the destination
	 * from metadata, so its single tile class serves both.
	 */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalPortal>> PORTAL =
			BLOCK_ENTITIES.register("portal_rift", () -> new BlockEntityType<>(
					TileEntityCrystalPortal::new, ChromaBlocks.PORTAL.get(), ChromaBlocks.RETURN_PORTAL.get()));

	/** Worldgen block rather than a ChromaTiles machine, so it registers against its block directly. */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityWarpNode>> WARP_NODE =
			BLOCK_ENTITIES.register("warp_node", () -> new BlockEntityType<>(
					TileEntityWarpNode::new, ChromaBlocks.WARP_NODE.get()));

	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
			String name, ChromaTiles tile) {
		// The tile's reflective (BlockPos, BlockState) factory returns the concrete BlockEntity.
		BlockEntityType.BlockEntitySupplier<T> factory = (pos, state) -> (T) tile.createBlockEntity(pos, state);
		return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, tile.getBlock()));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, ITEM_INFUSER.get(),
				(infuser, context) -> infuser.fluidHandler());
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, PLAYER_INFUSER.get(),
				(infuser, context) -> infuser.fluidHandler());
	}

	private ChromaBlockEntities() {}
}
