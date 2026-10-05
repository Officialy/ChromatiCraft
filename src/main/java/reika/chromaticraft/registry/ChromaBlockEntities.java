package reika.chromaticraft.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.block.BlockEncrustedCrystal.TileCrystalEncrusted;
import reika.chromaticraft.block.relay.BlockLumenRelay.TileEntityLumenRelay;
import reika.chromaticraft.tileentity.*;
import reika.chromaticraft.tileentity.acquisition.TileEntityCollector;
import reika.chromaticraft.tileentity.aoe.TileEntityWarpNode;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.networking.*;
import reika.chromaticraft.tileentity.plants.*;
import reika.chromaticraft.tileentity.recipe.*;

/**
 * ChromatiCraft {@link BlockEntityType} registry (mirrors ReactorBlockEntities). One entry per
 * {@link ChromaTiles} tile; grows as TileEntities port. Registered on the mod bus by the main class.
 */
public final class ChromaBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ChromatiCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<reika.chromaticraft.modinterface.voidritual.TileEntityVoidMonsterTrap>> VOID_MONSTER_TRAP=register("void_monster_trap",ChromaTiles.VOIDTRAP);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<reika.chromaticraft.tileentity.aoe.TileEntityDeathFogEmitter>> DEATH_FOG_EMITTER=register("death_fog_emitter",ChromaTiles.DEATHFOG);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<reika.chromaticraft.block.decoration.BlockColoredAltar.TileEntityColoredAltar>> COLORED_ALTAR=BLOCK_ENTITIES.register("colored_altar",()->new BlockEntityType<>(reika.chromaticraft.block.decoration.BlockColoredAltar.TileEntityColoredAltar::new,ChromaBlocks.COLORED_ALTARS.values().stream().map(DeferredHolder::get).toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDisplayPoint>> DISPLAY =
			register("display_point", ChromaTiles.DISPLAY);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalPylon>> PYLON =
			register("pylon", ChromaTiles.PYLON);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalRepeater>> REPEATER =
			register("crystal_repeater", ChromaTiles.REPEATER);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityWeakRepeater>> WEAK_REPEATER =
			register("weak_repeater", ChromaTiles.WEAKREPEATER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityRelaySource>> RELAY_SOURCE =
			register("relay_source", ChromaTiles.RELAYSOURCE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay>> FUNCTION_RELAY =
			register("function_relay", ChromaTiles.FUNCTIONRELAY);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityLumenRelay>> LUMEN_RELAY =
			BLOCK_ENTITIES.register("lumen_relay", () -> new BlockEntityType<>(
					TileEntityLumenRelay::new,
					java.util.stream.Stream.concat(ChromaBlocks.LUMEN_RELAYS.values().stream(),
							java.util.stream.Stream.of(ChromaBlocks.MULTICHROMIC_RELAY))
							.map(net.neoforged.neoforge.registries.DeferredHolder::get)
							.toArray(net.minecraft.world.level.block.Block[]::new)));

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
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityRitualTable>> RITUAL_TABLE =
			register("ritual_table", ChromaTiles.RITUAL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCrystalCharger>> CRYSTAL_CHARGER =
			register("crystal_charger", ChromaTiles.CHARGER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.block.BlockChromaTrail.TileChromaTrail>> CHROMA_TRAIL =
			BLOCK_ENTITIES.register("chroma_trail", () -> new BlockEntityType<>(
					reika.chromaticraft.block.BlockChromaTrail.TileChromaTrail::new, ChromaBlocks.CHROMA_TRAIL.get()));
	/** V33a CCCrystalPlant: one tile type for all sixteen Crystal Bloom blocks. */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant>> CRYSTAL_PLANT =
			BLOCK_ENTITIES.register("crystal_plant", () -> new BlockEntityType<>(
					reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant::new,
					ChromaBlocks.CRYSTAL_PLANTS.values().stream()
							.map(net.neoforged.neoforge.registries.DeferredHolder::get)
							.toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.plants.TileEntityChromaFlower>> CHROMA_FLOWER =
			register("chroma_flower", ChromaTiles.CHROMAFLOWER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.storage.TileEntityToolStorage>> TOOL_STORAGE =
			register("tool_storage", ChromaTiles.TOOLSTORAGE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter>> AUTO_ENCHANTER =
			register("auto_enchanter", ChromaTiles.ENCHANTER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.TileEntityLumenWire>> LUMEN_WIRE =
			register("lumen_wire", ChromaTiles.LUMENWIRE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.aoe.defence.TileEntityLumenTurret>> LUMEN_TURRET =
			register("lumen_turret", ChromaTiles.TURRET);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.modinterface.ae.TileEntityMEDistributor>> ME_DISTRIBUTOR =
			register("me_distributor", ChromaTiles.MEDISTRIBUTOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.transport.TileEntityFluidRelay>> FLUID_RELAY =
			register("fluid_relay", ChromaTiles.FLUIDRELAY);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.transport.TileEntityFluidDistributor>> FLUID_DISTRIBUTOR =
			register("fluid_distributor", ChromaTiles.FLUIDDISTRIBUTOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.transport.TileEntityRift>> RIFT =
			register("world_rift", ChromaTiles.RIFT);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump>> TELEPUMP =
			register("teleportation_pump", ChromaTiles.TELEPUMP);
	/**
	 * V33a's single {@code chroma.accelerator} tile id serves every colour: the type spans each implemented colour's
	 * block, and the factory builds that colour's own subclass.
	 */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade>> ADJACENCY =
			BLOCK_ENTITIES.register("adjacency_upgrade", () -> new BlockEntityType<>(
					(pos, state) -> ((reika.chromaticraft.block.BlockAdjacencyUpgrade)state.getBlock()).upgrade.createTileEntity(pos, state),
					ChromaBlocks.ADJACENCY_UPGRADES.values().stream()
							.map(net.neoforged.neoforge.registries.DeferredHolder::get)
							.toArray(net.minecraft.world.level.block.Block[]::new)));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner>> PARTICLE_SPAWNER =
			register("particle_spawner", ChromaTiles.PARTICLES);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.transport.TileEntityLaunchPad>> LAUNCH_PAD =
			register("launch_pad", ChromaTiles.LAUNCHPAD);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer>> CRYSTAL_BREWER =
			register("crystal_brewer", ChromaTiles.BREWER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCollector>> COLLECTOR =
			register("collector", ChromaTiles.COLLECTOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityFarmer>> FARMER =
			register("farmer", ChromaTiles.FARMER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityHeatLily>> HEAT_LILY =
			register("heat_lily", ChromaTiles.HEATLILY);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityBiomeReverter>> BIOME_REVERTER =
			register("biome_reverter", ChromaTiles.REVERTER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCobbleGen>> COBBLE_GENERATOR =
			register("cobble_generator", ChromaTiles.COBBLEGEN);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityAccelerationPlant>> PLANT_ACCELERATOR =
			register("plant_accelerator", ChromaTiles.PLANTACCEL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityCropSpeedPlant>> CROP_SPEED_PLANT =
			register("crop_speed_plant", ChromaTiles.CROPSPEED);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityHarvesterPlant>> HARVEST_PLANT =
			register("harvest_plant", ChromaTiles.HARVESTPLANT);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityItemInfuser>> ITEM_INFUSER =
			register("item_aura_infuser", ChromaTiles.INFUSER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityPlayerInfuser>> PLAYER_INFUSER =
			register("player_aura_infuser", ChromaTiles.PLAYERINFUSER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityPersonalCharger>> PERSONAL_CHARGER =
			register("personal_charger", ChromaTiles.PERSONAL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityFocusCrystal>> FOCUS_CRYSTAL =
			register("focus_crystal", ChromaTiles.FOCUSCRYSTAL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDataNode>> DATA_NODE =
			register("data_node", ChromaTiles.DATANODE);



	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityChromaCrystal>> POWER_CRYSTAL =
			register("power_crystal", ChromaTiles.CRYSTAL);
	public static final DeferredHolder<BlockEntityType<?>,
			BlockEntityType<reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger>> PYLON_TURBOCHARGER =
			register("pylon_turbocharger", ChromaTiles.PYLONTURBO);
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
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityMusicMemory>> MUSIC_MEMORY =
			BLOCK_ENTITIES.register("music_memory", () -> new BlockEntityType<>(
					TileEntityMusicMemory::new, ChromaBlocks.MUSIC_MEMORY.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityGOLTile>> GOL_TILE =
			BLOCK_ENTITIES.register("gol_tile", () -> new BlockEntityType<>(
					TileEntityGOLTile::new, ChromaBlocks.GOL_TILE.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityGOLController>> GOL_CONTROLLER =
			BLOCK_ENTITIES.register("gol_controller", () -> new BlockEntityType<>(
					TileEntityGOLController::new, ChromaBlocks.GOL_CONTROLLER.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityLaserEffector>> LASER_EFFECTOR =
			BLOCK_ENTITIES.register("laser_effector", () -> new BlockEntityType<>(
					TileEntityLaserEffector::new, ChromaBlocks.LASER_EFFECTOR.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityLaserController>> LASER_CONTROLLER =
			BLOCK_ENTITIES.register("laser_controller", () -> new BlockEntityType<>(
					TileEntityLaserController::new, ChromaBlocks.LASER_CONTROLLER.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityStructurePassword>> STRUCTURE_PASSWORD =
			BLOCK_ENTITIES.register("structure_password", () -> new BlockEntityType<>(
					TileEntityStructurePassword::new, ChromaBlocks.STRUCTURE_PASSWORD.get()));
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
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, AUTO_ENCHANTER.get(),
				(enchanter, context) -> enchanter.fluidHandler());
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, COLLECTOR.get(),
				(collector, context) -> collector.fluidHandler());
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FLUID_DISTRIBUTOR.get(),
				(distributor, side) -> distributor.fluidHandler(side));
		// V33a World Rift: IFluidHandler, RF IEnergyHandler and ISidedInventory views of the far side.
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, RIFT.get(), (rift, side) -> rift.fluidHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, RIFT.get(), (rift, side) -> rift.energyHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, RIFT.get(), (rift, side) -> rift.itemHandler(side));
		// V33a Teleportation Pump: IFluidHandler, drain-only.
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, TELEPUMP.get(), (pump, side) -> pump.fluidHandler());
	}

	private ChromaBlockEntities() {}
}
