/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.interfaces.customrangeupgrade.RangeUpgradeable;
import reika.chromaticraft.auxiliary.interfaces.CustomHitbox;
import reika.chromaticraft.auxiliary.interfaces.FocusAcceleratable;
import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.VariableTexture;
import reika.chromaticraft.base.tileentity.ChargedCrystalPowered;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.base.tileentity.TileEntityRelayPowered;
import reika.chromaticraft.base.tileentity.TileEntityWirelessPowered;
import reika.chromaticraft.block.BlockChromaPlantTile;
import reika.chromaticraft.block.BlockCrystalTile;
import reika.chromaticraft.block.BlockDecoPlant;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalRepeater;
import reika.chromaticraft.magic.interfaces.LumenTile;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.modinterface.TileEntityFloatingLandmark;
import reika.chromaticraft.modinterface.TileEntityLifeEmitter;
import reika.chromaticraft.modinterface.TileEntityManaBooster;
import reika.chromaticraft.modinterface.TileEntityPageExtractor;
import reika.chromaticraft.modinterface.TileEntitySmelteryDistributor;
import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;
import reika.chromaticraft.modinterface.ae.TileEntityPatternCache;
import reika.chromaticraft.modinterface.bees.TileEntityBeeStorage;
import reika.chromaticraft.modinterface.bees.TileEntityLumenAlveary;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityAspectFormer;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityAspectJar;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityEssentiaRelay;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityFluxMaker;
import reika.chromaticraft.modinterface.voidritual.TileEntityVoidMonsterTrap;
import reika.chromaticraft.tileentity.TileEntityBiomePainter;
import reika.chromaticraft.tileentity.TileEntityCrystalConsole;
import reika.chromaticraft.tileentity.TileEntityDataNode;
import reika.chromaticraft.tileentity.TileEntityDisplayPoint;
import reika.chromaticraft.tileentity.TileEntityFarmer;
import reika.chromaticraft.tileentity.TileEntityLumenWire;
import reika.chromaticraft.tileentity.TileEntityPersonalCharger;
import reika.chromaticraft.tileentity.TileEntityProgressionLinker;
import reika.chromaticraft.tileentity.aoe.TileEntityAIShutdown;
import reika.chromaticraft.tileentity.aoe.TileEntityAreaBreaker;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.chromaticraft.tileentity.aoe.TileEntityCaveLighter;
import reika.chromaticraft.tileentity.aoe.TileEntityCrystalLaser;
import reika.chromaticraft.tileentity.aoe.TileEntityDeathFogEmitter;
import reika.chromaticraft.tileentity.aoe.TileEntityHoverPad;
import reika.chromaticraft.tileentity.aoe.TileEntityItemCollector;
import reika.chromaticraft.tileentity.aoe.TileEntityItemInserter;
import reika.chromaticraft.tileentity.aoe.TileEntityLampController;
import reika.chromaticraft.tileentity.aoe.TileEntityMultiBuilder;
import reika.chromaticraft.tileentity.aoe.TileEntityVillageRepair;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityAvoLaser;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityChromaLamp;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityCloakingTower;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityCrystalBeacon;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityCrystalFence;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityExplosionShield;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityGuardianStone;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityLumenTurret;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityMeteorTower;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityRangeBoost;
import reika.chromaticraft.tileentity.acquisition.TileEntityCollector;
import reika.chromaticraft.tileentity.acquisition.TileEntityItemFabricator;
import reika.chromaticraft.tileentity.acquisition.TileEntityMiner;
import reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.tileentityfocuscrystal.CrystalTier;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger;
import reika.chromaticraft.tileentity.decoration.TileEntityAreaHologram;
import reika.chromaticraft.tileentity.decoration.TileEntityCrystalMusic;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner;
import reika.chromaticraft.tileentity.networking.TileEntityAtmosphericRelay;
import reika.chromaticraft.tileentity.networking.TileEntityCompoundRepeater;
import reika.chromaticraft.tileentity.networking.TileEntityCreativeSource;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalBroadcaster;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.chromaticraft.tileentity.networking.TileEntityNetworkOptimizer;
import reika.chromaticraft.tileentity.networking.TileEntityPylonLink;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater;
import reika.chromaticraft.tileentity.networking.TileEntityWeakRepeater;
import reika.chromaticraft.tileentity.networking.TileEntityWirelessSource;
import reika.chromaticraft.tileentity.plants.TileEntityAccelerationPlant;
import reika.chromaticraft.tileentity.plants.TileEntityBiomeReverter;
import reika.chromaticraft.tileentity.plants.TileEntityChromaFlower;
import reika.chromaticraft.tileentity.plants.TileEntityCobbleGen;
import reika.chromaticraft.tileentity.plants.TileEntityCropSpeedPlant;
import reika.chromaticraft.tileentity.plants.TileEntityHarvesterPlant;
import reika.chromaticraft.tileentity.plants.TileEntityHeatLily;
import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter;
import reika.chromaticraft.tileentity.processing.TileEntityCrystalFurnace;
import reika.chromaticraft.tileentity.processing.TileEntityEnchantDecomposer;
import reika.chromaticraft.tileentity.processing.TileEntityGlowFire;
import reika.chromaticraft.tileentity.processing.TileEntityInventoryTicker;
import reika.chromaticraft.tileentity.processing.TileEntitySpawnerReprogrammer;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingAuto;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingInjector;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityChromaCrafter;
import reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer;
import reika.chromaticraft.tileentity.recipe.TileEntityItemInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.chromaticraft.tileentity.recipe.TileEntityPlayerInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;
import reika.chromaticraft.tileentity.storage.TileEntityCrystalTank;
import reika.chromaticraft.tileentity.storage.TileEntityMultiStorage;
import reika.chromaticraft.tileentity.storage.TileEntityPowerTree;
import reika.chromaticraft.tileentity.storage.TileEntityToolStorage;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.tileentity.technical.TileEntityStructControl;
import reika.chromaticraft.tileentity.transport.TileEntityConduitBridge;
import reika.chromaticraft.tileentity.transport.TileEntityFluidDistributor;
import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;
import reika.chromaticraft.tileentity.transport.TileEntityItemRift;
import reika.chromaticraft.tileentity.transport.TileEntityLaunchPad;
import reika.chromaticraft.tileentity.transport.TileEntityNetworkItemTransporter;
import reika.chromaticraft.tileentity.transport.TileEntityRFDistributor;
import reika.chromaticraft.tileentity.transport.TileEntityRift;
import reika.chromaticraft.tileentity.transport.TileEntityRouterHub;
import reika.chromaticraft.tileentity.transport.TileEntityTeleportGate;
import reika.chromaticraft.tileentity.transport.TileEntityTransportWindow;
import reika.dragonapi.DragonAPICore;
import reika.dragonapi.ModList;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.data.maps.BlockMap;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.interfaces.registry.TileEnum;
import reika.dragonapi.interfaces.tileentity.RedstoneTile;
import reika.dragonapi.interfaces.tileentity.SidePlacedTile;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum ChromaTiles implements TileEnum {

	CHROMAFLOWER("chroma.flower", 		ChromaBlocks.TILEPLANT, 	TileEntityChromaFlower.class, 		0, "ChromaFlowerRenderer"),
	ENCHANTER("chroma.enchanter", 		ChromaBlocks.TILEENTITY, 	TileEntityAutoEnchanter.class, 		0),
	VILLAGEREPAIR("chroma.village",		ChromaBlocks.TILEENTITY, 	TileEntityVillageRepair.class,		1),
	REPROGRAMMER("chroma.reprogrammer", ChromaBlocks.TILEMODELLED, 	TileEntitySpawnerReprogrammer.class, 0, "RenderSpawnerProgrammer"),
	COLLECTOR("chroma.collector", 		ChromaBlocks.TILEENTITY, 	TileEntityCollector.class, 			4),
	TABLE("chroma.table", 				ChromaBlocks.TILEENTITY, 	TileEntityCastingTable.class, 		5),
	RIFT("chroma.rift", 				ChromaBlocks.RIFT, 			TileEntityRift.class, 				0, "RenderRift"),
	BREWER("chroma.brewer", 			ChromaBlocks.TILEENTITY, 	TileEntityCrystalBrewer.class, 		6),
	GUARDIAN("chroma.guardian", 		ChromaBlocks.TILECRYSTAL, 	TileEntityGuardianStone.class, 		0, "GuardianStoneRenderer"),
	ADJACENCY("chroma.accelerator", 	ChromaBlocks.ADJACENCY, 	TileEntityAdjacencyUpgrade.class,	-1, "AdjacencyUpgradeRenderer"),
	PYLON("chroma.pylon", 				ChromaBlocks.PYLON, 		TileEntityCrystalPylon.class, 		0, "RenderCrystalPylon"),
	REPEATER("chroma.repeater", 		ChromaBlocks.PYLON, 		TileEntityCrystalRepeater.class, 	1, "RenderCrystalRepeater"),
	LASER("chroma.laser", 				ChromaBlocks.TILEMODELLED, 	TileEntityCrystalLaser.class, 		1, "RenderCrystalLaser"),
	STAND("chroma.stand", 				ChromaBlocks.TILEMODELLED, 	TileEntityItemStand.class, 			2, "RenderItemStand"),
	CHARGER("chroma.charger", 			ChromaBlocks.TILEMODELLED, 	TileEntityCrystalCharger.class, 	3, "RenderCrystalCharger"),
	FURNACE("chroma.furnace", 			ChromaBlocks.TILEENTITY, 	TileEntityCrystalFurnace.class, 	7),
	RITUAL("chroma.ritual", 			ChromaBlocks.TILEENTITY, 	TileEntityRitualTable.class, 		8),
	TANK("chroma.tank", 				ChromaBlocks.TILEENTITY, 	TileEntityCrystalTank.class, 		9, "TankRender"),
	FENCE("chroma.fence", 				ChromaBlocks.TILEMODELLED, 	TileEntityCrystalFence.class, 		4, "RenderCrystalFence"),
	BEACON("chroma.beacon", 			ChromaBlocks.TILEMODELLED, 	TileEntityCrystalBeacon.class, 		5, "RenderCrystalBeacon"),
	ITEMRIFT("chroma.itemrift", 		ChromaBlocks.TILEMODELLED, 	TileEntityItemRift.class, 			6, "RenderItemRift"),
	CRYSTAL("chroma.chromacrystal", 	ChromaBlocks.RAINBOWCRYSTAL, TileEntityChromaCrystal.class, 	0),
	INFUSER("chroma.infuser", 			ChromaBlocks.TILEMODELLED, 	TileEntityItemInfuser.class, 		7, "RenderInfuser3"),
	FABRICATOR("chroma.fabricator",		ChromaBlocks.TILEMODELLED, 	TileEntityItemFabricator.class, 	8, "RenderItemFabricator"),
	MINER("chroma.miner",				ChromaBlocks.TILEMODELLED, 	TileEntityMiner.class, 				9, "RenderMiner"),
	HEATLILY("chroma.heatlily",			ChromaBlocks.DECOPLANT, 	TileEntityHeatLily.class, 			0),
	ITEMCOLLECTOR("chroma.itemcoll",	ChromaBlocks.TILEENTITY, 	TileEntityItemCollector.class, 		10),
	TICKER("chroma.ticker",				ChromaBlocks.TILEENTITY, 	TileEntityInventoryTicker.class, 	11),
	AISHUTDOWN("chroma.aishutdown",		ChromaBlocks.TILEMODELLED, 	TileEntityAIShutdown.class, 		10),
	TELEPUMP("chroma.telepump",			ChromaBlocks.TILEENTITY, 	TileEntityTeleportationPump.class, 	12),
	COMPOUND("chroma.compound",			ChromaBlocks.PYLON,			TileEntityCompoundRepeater.class, 	2, "RenderMultiRepeater"),
	RELAYSOURCE("chroma.fibersource", 	ChromaBlocks.TILEMODELLED, 	TileEntityRelaySource.class, 		12, "RenderRelaySource"),
	ASPECTJAR("chroma.aspectjar",		ChromaBlocks.TILEMODELLED2, TileEntityAspectJar.class,			2, "AspectJarRenderer", ModList.THAUMCRAFT),
	ASPECT("chroma.aspect", 			ChromaBlocks.TILEMODELLED, 	TileEntityAspectFormer.class,		14, "RenderAspectFormer", ModList.THAUMCRAFT),
	BIOMEPAINTER("chroma.biomechg",		ChromaBlocks.TILEMODELLED2, TileEntityBiomePainter.class,		3, "RenderBiomePainter"),
	LAMP("chroma.lamp",					ChromaBlocks.TILEMODELLED, 	TileEntityChromaLamp.class, 		15, "RenderRainbowLamp"),
	POWERTREE("chroma.powertree",		ChromaBlocks.PYLON, 		TileEntityPowerTree.class, 			3, "PowerTreeRender"),
	LAMPCONTROL("chroma.lampcontrol",	ChromaBlocks.TILEMODELLED2, TileEntityLampController.class, 	0, "RenderLampControl"),
	CREATIVEPYLON("chroma.creativepylon",ChromaBlocks.PYLON, 		TileEntityCreativeSource.class, 	4, "RenderCreativePylon"),
	STRUCTCONTROL("chroma.structcontrol",ChromaBlocks.PYLON,		TileEntityStructControl.class,		5, "RenderStructControl"),
	LIFEEMITTER("chroma.lifeemitter",	ChromaBlocks.TILEMODELLED2, TileEntityLifeEmitter.class,		1/*, "RenderLifeEmitter"*/),
	FARMER("chroma.farmer",				ChromaBlocks.TILEMODELLED2,	TileEntityFarmer.class,				4, "RenderFarmer"),
	AURAPOINT("chroma.aurapoint",		ChromaBlocks.TILECRYSTALNONCUBE, TileEntityAuraPoint.class,		1, "RenderAuraPoint"),
	DIMENSIONCORE("chroma.dimcore",		ChromaBlocks.PYLON,			TileEntityDimensionCore.class,		6, "RenderDimensionCore"),
	AUTOMATOR("chroma.automator",		ChromaBlocks.TILECRYSTALNONCUBE, TileEntityCastingAuto.class,	2, "RenderCastingAuto"),
	DISPLAY("chroma.display",			ChromaBlocks.TILECRYSTAL,	TileEntityDisplayPoint.class,		1),
	MEDISTRIBUTOR("chroma.medistrib",	ChromaBlocks.TILEMODELLED2,	TileEntityMEDistributor.class,		5, "RenderMEDistributor", ModList.APPENG),
	WINDOW("chroma.window",				ChromaBlocks.TILEMODELLED2, TileEntityTransportWindow.class,	6, "RenderTransportWindow"),
	RFDISTRIBUTOR("chroma.rfdistrib",	ChromaBlocks.TILEMODELLED2, TileEntityRFDistributor.class,		7, "RenderRFDistributor"),
	PERSONAL("chroma.personal",			ChromaBlocks.PYLON,			TileEntityPersonalCharger.class,	7, "RenderPersonalCharger"),
	MUSIC("chroma.music",				ChromaBlocks.TILECRYSTAL,	TileEntityCrystalMusic.class,		2, "RenderCrystalMusic"),
	PATTERNS("chroma.patterns",			ChromaBlocks.TILEENTITY,	TileEntityPatternCache.class,		13, ModList.APPENG),
	PYLONTURBO("chroma.turbo", 			ChromaBlocks.TILEMODELLED2,	TileEntityPylonTurboCharger.class,	8, "RenderPylonTurboCharger"),
	TURRET("chroma.turret",				ChromaBlocks.TILEMODELLED2,	TileEntityLumenTurret.class,		9, "RenderLumenTurret"),
	CONSOLE("chroma.console",			ChromaBlocks.CONSOLE,		TileEntityCrystalConsole.class,		0, "RenderCrystalConsole"),
	BROADCAST("chroma.broadcast",		ChromaBlocks.PYLON,			TileEntityCrystalBroadcaster.class,	8, "RenderCrystalBroadcast"),
	CLOAKING("chroma.tower",			ChromaBlocks.TILEMODELLED2, TileEntityCloakingTower.class,		10, "RenderCloakingTower"),
	//POSLINK("chroma.poslink",			ChromaBlocks.TILEMODELLED2,	TileEntityPositionRelay.class,		10),
	HOLOGRAM("chroma.hologram",			ChromaBlocks.TILEMODELLED2, TileEntityAreaHologram.class,		11, "RenderAreaHologram"),
	LIGHTER("chroma.lighter",			ChromaBlocks.TILEMODELLED2,	TileEntityCaveLighter.class,		12, "RenderCaveLighter"),
	STORAGE("chroma.itemstorage",		ChromaBlocks.TILEENTITY,	TileEntityMultiStorage.class,		14),
	GLOWFIRE("chroma.glowfire",			ChromaBlocks.TILEMODELLED2,	TileEntityGlowFire.class,			13, "RenderGlowFire"),
	ESSENTIARELAY("chroma.essentia",	ChromaBlocks.TILEMODELLED2,	TileEntityEssentiaRelay.class,		14, "RenderEssentiaRelay"),
	INSERTER("chroma.inserter",			ChromaBlocks.TILEENTITY,	TileEntityItemInserter.class,		15),
	REVERTER("chroma.reverter",			ChromaBlocks.DECOPLANT,		TileEntityBiomeReverter.class,		1),
	COBBLEGEN("chroma.cobblegen",		ChromaBlocks.DECOPLANT,		TileEntityCobbleGen.class,			2),
	PLANTACCEL("chroma.plantaccel",		ChromaBlocks.DECOPLANT,		TileEntityAccelerationPlant.class,	3),
	CROPSPEED("chroma.cropspeed",		ChromaBlocks.DECOPLANT,		TileEntityCropSpeedPlant.class,		4),
	CHROMACRAFTER("chroma.chcrafter",	ChromaBlocks.TILEMODELLED2,	TileEntityChromaCrafter.class,		15, "RenderChromaCrafter"),
	WEAKREPEATER("chroma.weakrepeater",	ChromaBlocks.PYLON,			TileEntityWeakRepeater.class,		9, "RenderWeakRepeater"),
	ENCHANTDECOMP("chroma.enchantdecomp",ChromaBlocks.TILEMODELLED3,TileEntityEnchantDecomposer.class,	0, "RenderEnchantDecomposer"),
	LUMENWIRE("chroma.lumenwire",		ChromaBlocks.TILEMODELLED3,	TileEntityLumenWire.class,			1, "RenderLumenWire"),
	PARTICLES("chroma.particles",		ChromaBlocks.TILEMODELLED3,	TileEntityParticleSpawner.class,	2, "RenderParticleSpawner"),
	WIRELESS("chroma.wireless",			ChromaBlocks.TILEMODELLED3,	TileEntityWirelessSource.class,		3, "RenderWirelessSource"),
	HOVERPAD("chroma.hoverpad",			ChromaBlocks.TILEENTITY2,	TileEntityHoverPad.class,			6/*, "RenderHoverPad"*/),
	METEOR("chroma.meteor",				ChromaBlocks.TILEMODELLED3,	TileEntityMeteorTower.class,		5, "RenderMeteorTower"),
	FLUIDDISTRIBUTOR("chroma.fluiddistrib",ChromaBlocks.TILEMODELLED3,TileEntityFluidDistributor.class,	6, "RenderFluidDistributor"),
	FLUXMAKER("chroma.taintmaker",		ChromaBlocks.TILEMODELLED3,	TileEntityFluxMaker.class,			7, "RenderFluxMaker", ModList.THAUMCRAFT),
	AREABREAKER("chroma.areabreaker",	ChromaBlocks.TILEMODELLED3, TileEntityAreaBreaker.class,		8, "RenderAreaBreaker"),
	TELEPORT("chroma.gate",				ChromaBlocks.TILEMODELLED3,	TileEntityTeleportGate.class,		9, "RenderTeleportGate"),
	FLUIDRELAY("chroma.fluidrelay",		ChromaBlocks.TILEMODELLED3,	TileEntityFluidRelay.class,			10, "RenderFluidRelay"),
	IONOSPHERIC("chroma.atmospheric",	ChromaBlocks.PYLON,			TileEntityAtmosphericRelay.class,	10),
	BOOKDECOMP("chroma.bookdecomp",		ChromaBlocks.TILEMODELLED3,	TileEntityPageExtractor.class,		11, "RenderPageExtractor", ModList.MYSTCRAFT),
	HARVESTPLANT("chroma.harvestplant",	ChromaBlocks.DECOPLANT,		TileEntityHarvesterPlant.class,		5),
	BRIDGE("chroma.bridge",				ChromaBlocks.TILEENTITY2,	TileEntityConduitBridge.class,		0),
	AVOLASER("chroma.avolaser",			ChromaBlocks.TILEMODELLED3,	TileEntityAvoLaser.class,			12, "RenderAvoLaser"),
	ALVEARY("chroma.alveary",			ChromaBlocks.TILEENTITY2,	TileEntityLumenAlveary.class,		1, "RenderAlveary", ModList.FORESTRY),
	ROUTERHUB("chroma.router",			ChromaBlocks.TILEMODELLED3,	TileEntityRouterHub.class,			13, "RenderRouterHub"),
	FOCUSCRYSTAL("chroma.focuscrystal",	ChromaBlocks.TILEMODELLED3, TileEntityFocusCrystal.class,		14, "RenderFocusCrystal"),
	DATANODE("chroma.datanode",			ChromaBlocks.TILEMODELLED3,	TileEntityDataNode.class,			15, "RenderDataNode"),
	PYLONLINK("chroma.pylonlink",		ChromaBlocks.TILEENTITY2,	TileEntityPylonLink.class,			2,	"RenderPylonLink"),
	SKYPEATER("chroma.skypeater",		ChromaBlocks.PYLON,			TileEntitySkypeater.class,			11, "RenderSkypeater"),
	FUNCTIONRELAY("chroma.funcrelay",	ChromaBlocks.TILEMODELLED4,	TileEntityFunctionRelay.class,		0, "RenderFunctionRelay"),
	MULTIBUILDER("chroma.multibuilder",	ChromaBlocks.TILEMODELLED4,	TileEntityMultiBuilder.class,		1, "RenderMultiBuilder"),
	EXPLOSIONSHIELD("chroma.explosionshield",ChromaBlocks.TILEENTITY2,TileEntityExplosionShield.class,	3),
	PROGRESSLINK("chroma.progresslink",	ChromaBlocks.TILEMODELLED4,	TileEntityProgressionLinker.class,	2, "RenderProgressionLinker"),
	MANABOOSTER("chroma.manabooster",	ChromaBlocks.TILEMODELLED4,	TileEntityManaBooster.class,		3, "RenderManaBooster", ModList.BOTANIA),
	DEATHFOG("chroma.deathfog",			ChromaBlocks.TILEENTITY2,	TileEntityDeathFogEmitter.class,	4, ModList.VOIDMONSTER),
	OPTIMIZER("chroma.optimizer",		ChromaBlocks.TILEMODELLED4,	TileEntityNetworkOptimizer.class,	4, "RenderNetworkOptimizer"),
	LANDMARK("chroma.landmark",			ChromaBlocks.TILEMODELLED4, TileEntityFloatingLandmark.class,	5, "RenderFloatingLandmark", ModList.BUILDCRAFT),
	INJECTOR("chroma.injector",			ChromaBlocks.TILEENTITY2,	TileEntityCastingInjector.class,	5),
	VOIDTRAP("chroma.voidtrap",			ChromaBlocks.TILEMODELLED4,	TileEntityVoidMonsterTrap.class,	6, "RenderVoidMonsterTrap", ModList.VOIDMONSTER),
	PLAYERINFUSER("chroma.playerinfuser", ChromaBlocks.TILEMODELLED4, TileEntityPlayerInfuser.class, 	7, "RenderInfuser3"),
	SMELTERYDISTRIBUTOR("chroma.smeltery", ChromaBlocks.TILEMODELLED4, TileEntitySmelteryDistributor.class, 8, "RenderSmelteryDistributor", ModList.TINKERER),
	LAUNCHPAD("chroma.launchpad",		ChromaBlocks.TILEENTITY2,	TileEntityLaunchPad.class,			7, "RenderLaunchPad"),
	TOOLSTORAGE("chroma.toolstorage",	ChromaBlocks.TILEENTITY2,	TileEntityToolStorage.class,		8, "RenderMassStorage"),
	BEESTORAGE("chroma.beestorage",		ChromaBlocks.TILEMODELLED4,	TileEntityBeeStorage.class,			9, "RenderBeeStorage", ModList.FORESTRY),
	NETWORKITEM("chroma.networkitem",	ChromaBlocks.TILEMODELLED4,	TileEntityNetworkItemTransporter.class, 10, "RenderItemNetwork");

	private final Class tile;
	private final String name;
	private final String renderer;
	private final int metadata;
	private final ChromaBlocks block;
	private TileEntity renderInstance;
	private final ModList dependency;

	public static final ChromaTiles[] TEList = values();

	private static final BlockMap<ChromaTiles> chromaMappings = new BlockMap();
	private static final ItemHashMap<ChromaTiles> craftMap = new ItemHashMap();

	private ChromaTiles(String n, ChromaBlocks b, Class <? extends TileEntityChromaticBase> c, int meta) {
		this(n, b, c, meta, null, null);
	}

	private ChromaTiles(String n, ChromaBlocks b, Class <? extends TileEntityChromaticBase> c, int meta, String r) {
		this(n, b, c, meta, r, null);
	}

	private ChromaTiles(String n, ChromaBlocks b, Class <? extends TileEntityChromaticBase> c, int meta, ModList mod) {
		this(n, b, c, meta, null, mod);
	}

	private ChromaTiles(String n, ChromaBlocks b, Class <? extends TileEntityChromaticBase> c, int meta, String r, ModList mod) {
		tile = c;
		name = n;
		block = b;
		metadata = meta;
		renderer = r;
		dependency = mod;
	}

	public Block getBlock() {
		return block.getBlockInstance();
	}

	public int getBlockMetadata() {
		return metadata%16;
	}

	public String getName() {
		return StatCollector.translateToLocal(name);
	}

	public String getUnlocalizedName() {
		return name;
	}

	public boolean renderInPass1() {
		switch(this) {
			case ADJACENCY:
			case RIFT:
			case PYLON:
			case REPEATER:
			case COMPOUND:
			case LASER:
			case BEACON:
			case FENCE:
			case STAND:
			case RELAYSOURCE:
			case ASPECT:
			case LAMP:
			case MINER:
			case POWERTREE:
			case LAMPCONTROL:
			case STRUCTCONTROL:
			case CHROMAFLOWER:
			case DIMENSIONCORE:
			case AURAPOINT:
			case AUTOMATOR:
			case MEDISTRIBUTOR:
			case RFDISTRIBUTOR:
			case FLUIDDISTRIBUTOR:
			case PERSONAL:
			case PYLONTURBO:
			case TURRET:
			case CONSOLE:
			case BROADCAST:
			case CLOAKING:
				//case TANK:
				//case ITEMRIFT:
			case HOLOGRAM:
			case ESSENTIARELAY:
			case WEAKREPEATER:
			case ENCHANTDECOMP:
			case FABRICATOR:
			case PARTICLES:
			case METEOR:
			case LUMENWIRE:
			case AREABREAKER:
			case TELEPORT:
			case FLUIDRELAY:
			case BOOKDECOMP:
			case AVOLASER:
			case ALVEARY:
			case ROUTERHUB:
			case FOCUSCRYSTAL:
			case DATANODE:
			case SKYPEATER:
			case FLUXMAKER:
			case FUNCTIONRELAY:
			case WIRELESS:
			case CHROMACRAFTER:
			case MULTIBUILDER:
			case GLOWFIRE:
			case MANABOOSTER:
			case OPTIMIZER:
			case ASPECTJAR:
			case LANDMARK:
			case VOIDTRAP:
			case PLAYERINFUSER:
			case SMELTERYDISTRIBUTOR:
			case LAUNCHPAD:
			case TOOLSTORAGE:
			case BEESTORAGE:
			case NETWORKITEM:
				return true;
			default:
				return false;
		}
	}

	public boolean hasRender() {
		return renderer != null;
	}

	public String getRenderer() {
		if (!this.hasRender())
			throw new RuntimeException("Tile "+name+" has no render to call!");
		return "Reika.ChromatiCraft.Render.TESR."+renderer;
	}

	public TileEntity createTEInstanceForRender(int offset) {
		if (this == ADJACENCY) {
			return AdjacencyUpgrades.upgrades[offset].createTEInstanceForRender();
		}
		if (renderInstance == null) {
			try {
				renderInstance = (TileEntity)tile.newInstance();
			}
			catch (Throwable e) {
				throw new RegistrationException(ChromatiCraft.instance, "Could not create TE instance to render "+this, e);
			}
		}
		((TileEntityChromaticBase)renderInstance).animateItem();
		if (this == FOCUSCRYSTAL && renderInstance != null) {
			((TileEntityFocusCrystal)renderInstance).setDataFromItemStackTag(CrystalTier.tierList[offset].getCraftedItem());
		}
		return renderInstance;
	}

	public Class<? extends TileEntity> getTEClass() {
		return tile;
	}

	public static ChromaTiles getTile(IBlockAccess iba, int x, int y, int z) {
		Block id = iba.getBlock(x, y, z);
		int meta = iba.getBlockMetadata(x, y, z);
		return getTileFromIDandMetadata(id, meta);
	}

	public static ChromaTiles getTileFromIDandMetadata(Block id, int meta) {
		return chromaMappings.get(id, meta);
	}

	public static TileEntity createTEFromIDAndMetadata(Block id, int meta) {
		ChromaTiles index = getTileFromIDandMetadata(id, meta);
		if (index == null) {
			ChromatiCraft.logger.logError("ID "+id+" and metadata "+meta+" are not a valid tile identification pair!");
			Thread.dumpStack();
			return null;
		}
		if (index.hasPrerequisite() && !index.getPrerequisite().isLoaded() && !ReikaObfuscationHelper.isDeObfEnvironment()) {
			ChromatiCraft.logger.logError("The tile "+index+" is dependent on "+index.getPrerequisite()+" and should not attempt to be loaded without it!");
			Thread.dumpStack();
			return null;
		}
		if (index == ADJACENCY) {
			return AdjacencyUpgrades.upgrades[meta].createTileEntity();
		}
		Class TEClass = index.tile;
		try {
			return (TileEntity)TEClass.newInstance();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
			throw new RegistrationException(ChromatiCraft.instance, "ID "+id+" and Metadata "+meta+" failed to instantiate its TileEntity of "+TEClass);
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RegistrationException(ChromatiCraft.instance, "ID "+id+" and Metadata "+meta+" failed; illegally accessed a member its TileEntity of "+TEClass);
		}
		catch (LinkageError e) {
			ChromatiCraft.logger.logError("ID "+id+" and Metadata "+meta+" failed to instantiate its TileEntity of "+TEClass+"; class could not be loaded properly");
			e.printStackTrace();
			return null;
		}
	}

	public static ArrayList<ChromaTiles> getTilesForBlock(Block b) {
		ArrayList<ChromaTiles> li = new ArrayList();
		for (int i = 0; i < 16; i++) {
			ChromaTiles c = getTileFromIDandMetadata(b, i);
			if (c != null)
				li.add(c);
		}
		return li;
	}

	public static void loadMappings() {
		for (int i = 0; i < TEList.length; i++) {
			ChromaTiles r = TEList[i];
			Block id = r.getBlock();
			int meta = r.getBlockMetadata();
			if (chromaMappings.containsKey(id, meta))
				throw new RegistrationException(ChromatiCraft.instance, "ID/Meta conflict @ "+id+"/"+meta+": "+r+" & "+chromaMappings.get(id, meta));
			chromaMappings.put(id, meta, r);
			if (r == ChromaTiles.ADJACENCY) {
				craftMap.put(ChromaItems.ADJACENCY.getAnyMetaStack(), r);
			}
			else {
				ItemStack is = r.getCraftedProduct();
				if (is != null)
					craftMap.put(is, r);
			}
		}
	}

	public static void loadPostMappings() {
		for (int i = 0; i < TEList.length; i++) {
			ChromaTiles r = TEList[i];
			if (RangeUpgradeable.class.isAssignableFrom(r.tile)) {
				TileEntityRangeBoost.addBasicHandling(r.tile, r.getCraftedProduct());
			}
		}
	}

	public boolean hasSneakActions() {
		if (this == STAND)
			return true;
		if (this == BOOKDECOMP)
			return true;
		if (this == MULTIBUILDER)
			return true;
		if (this == TABLE)
			return true;
		return false;
	}

	public boolean allowsAcceleration() {
		if (this.isRepeater())
			return false;
		if (FocusAcceleratable.class.isAssignableFrom(tile))
			return false;
		switch(this) {
			case RITUAL:
			case POWERTREE:
			case PYLON:
			case AUTOMATOR:
			case AURAPOINT:
			case RELAYSOURCE:
			case PERSONAL:
			case PYLONTURBO:
			case CLOAKING:
			case ADJACENCY:
			case ALVEARY:
				return false;
			default:
				return true;
		}
	}

	public ItemStack getCraftedProduct() {
		if (this == RIFT)
			return ChromaItems.RIFT.getStackOf();
		if (this == ADJACENCY)
			return ChromaItems.ADJACENCY.getStackOf();
		if (this == CREATIVEPYLON || this == STRUCTCONTROL)
			return null;
		return ChromaItems.PLACER.getStackOfMetadata(this.ordinal());
	}

	public ItemStack getCraftedProduct(TileEntity te) {
		ItemStack is = this.getCraftedProduct();
		if (te instanceof NBTTile) {
			if (is.stackTagCompound == null)
				is.stackTagCompound = new NBTTagCompound();
			((NBTTile)te).getTagsToWriteToStack(is.stackTagCompound);
		}
		return is;
	}

	public ItemStack getCraftedNBTProduct(Object... args) {
		ItemStack is = this.getCraftedProduct();
		is.stackTagCompound = new NBTTagCompound();
		for (int i = 0; i < args.length; i += 2) {
			String s = (String)args[i];
			Object o = args[i+1];
			if (o instanceof String) {
				is.stackTagCompound.setString(s, (String)o);
			}
			else if (o instanceof Integer) {
				is.stackTagCompound.setInteger(s, (Integer)o);
			}
			else if (o instanceof Double) {
				is.stackTagCompound.setDouble(s, (Double)o);
			}
			else if (o instanceof Boolean) {
				is.stackTagCompound.setBoolean(s, (Boolean)o);
			}
		}
		return is;
	}

	public boolean isAvailableInCreativeInventory() {
		if (this == RIFT || this == ADJACENCY)
			return false;
		if (tile == TileEntityChromaticBase.class)
			return false;
		if (DragonAPICore.isReikasComputer())
			return true;
		if (dependency != null && !dependency.isLoaded())
			return false;
		return true;
	}

	public boolean canBeVertical() {
		switch(this) {
			case LASER:
				return true;
			default:
				return false;
		}
	}

	public boolean hasNBTVariants() {
		return NBTTile.class.isAssignableFrom(tile);
	}

	public boolean isSidePlaced() {
		return SidePlacedTile.class.isAssignableFrom(tile);
	}

	public double getMinX(TileEntityChromaticBase te) {
		switch(this) {
			case CHARGER:
				return 0.125;
			case ASPECTJAR:
				return 0.1875;
			case WINDOW:
				return ((TileEntityTransportWindow)te).getFacing().offsetX == 0 ? 0 : 0.4375;
			case TURRET:
				return 0.25;
			case ITEMRIFT:
				return ((TileEntityItemRift)te).getFacing() == ForgeDirection.EAST ? 0.6875 : 0;
			default:
				return 0;
		}
	}

	public double getMinY(TileEntityChromaticBase te) {
		switch(this) {
			case ITEMRIFT:
				return ((TileEntityItemRift)te).getFacing() == ForgeDirection.UP ? 0.6875 : 0;
			default:
				return 0;
		}
	}

	public double getMinZ(TileEntityChromaticBase te) {
		switch(this) {
			case CHARGER:
				return 0.125;
			case ASPECTJAR:
				return 0.1875;
			case WINDOW:
				return ((TileEntityTransportWindow)te).getFacing().offsetZ == 0 ? 0 : 0.4375;
			case TURRET:
				return 0.25;
			case ITEMRIFT:
				return ((TileEntityItemRift)te).getFacing() == ForgeDirection.SOUTH ? 0.6875 : 0;
			default:
				return 0;
		}
	}

	public double getMaxX(TileEntityChromaticBase te) {
		switch(this) {
			case CHARGER:
				return 0.875;
			case ASPECTJAR:
				return 0.8125;
			case WINDOW:
				return ((TileEntityTransportWindow)te).getFacing().offsetX == 0 ? 1 : 0.5625;
			case TURRET:
				return 0.75;
			case ITEMRIFT:
				return ((TileEntityItemRift)te).getFacing() == ForgeDirection.WEST ? 0.3125 : 1;
			default:
				return 1;
		}
	}

	public double getMaxY(TileEntityChromaticBase te) {
		switch(this) {
			case RELAYSOURCE:
				return 0.625;
			case STAND:
				return 0.75;
			case INFUSER:
			case PLAYERINFUSER:
				return 0.5;
			case ASPECTJAR:
				return 0.875;
			case PYLONTURBO:
				return 0.5;
			case TURRET:
				return 0.625;
			case ENCHANTDECOMP:
				return 0.5625;
			case BOOKDECOMP:
				return 0.25;
			case ROUTERHUB:
				return 0.875;
			case FOCUSCRYSTAL:
				return 0.375;
			case REPROGRAMMER:
				return 0.25;
			case MULTIBUILDER:
				return 0.75;
			case PROGRESSLINK:
				return 0.375;
			case NETWORKITEM:
				return 0.875;
			case ITEMRIFT:
				return ((TileEntityItemRift)te).getFacing() == ForgeDirection.DOWN ? 0.3125 : 1;
			default:
				return 1;
		}
	}

	public double getMaxZ(TileEntityChromaticBase te) {
		switch(this) {
			case CHARGER:
				return 0.875;
			case ASPECTJAR:
				return 0.8125;
			case WINDOW:
				return ((TileEntityTransportWindow)te).getFacing().offsetZ == 0 ? 1 : 0.5625;
			case TURRET:
				return 0.75;
			case ITEMRIFT:
				return ((TileEntityItemRift)te).getFacing() == ForgeDirection.NORTH ? 0.3125 : 1;
			default:
				return 1;
		}
	}

	@SideOnly(Side.CLIENT)
	public int getRenderOffset() {
		switch(this) {
			case LASER:
			case REPEATER:
			case COMPOUND:
			case BROADCAST:
			case WEAKREPEATER:
			case METEOR:
				return 0;
			case CHARGER:
				return 29;
			case CLOAKING:
				return 27;
			case TELEPORT:
				return 8;
			case FLUIDRELAY:
				return 55;
			default:
				return 21;
		}
	}

	@SideOnly(Side.CLIENT)
	public boolean hasBlockRender() {
		return !this.hasRender() || this == TANK || this == TABLE || this == CONSOLE || this == ALVEARY || this == PYLONLINK || this == LAUNCHPAD || this == TOOLSTORAGE;
	}

	public boolean isPlant() {
		return this.getBlock() instanceof BlockChromaPlantTile || this.getBlock() instanceof BlockDecoPlant;
	}

	public boolean isDummiedOut() {
		if (DragonAPICore.isReikasComputer() && ReikaObfuscationHelper.isDeObfEnvironment())
			return false;
		if (this.hasPrerequisite() && !this.getPrerequisite().isLoaded())
			return true;
		if (tile == TileEntityChromaticBase.class)
			return true;
		return false;
	}

	public boolean hasPrerequisite() {
		return dependency != null;
	}

	public ModList getPrerequisite() {
		return dependency;
	}

	public boolean isIncomplete() {
		return block.hasModel() && !this.hasRender();
	}

	public boolean isConfigDisabled() {
		return false;
	}

	public boolean allowFakePlacer() {
		if (this.isCrystalNetworkTile())
			return false;
		if (this == TABLE || this == RITUAL || this == STAND || this == AUTOMATOR)
			return false;
		if (this == AURAPOINT || this == DIMENSIONCORE)
			return false;
		return true;
	}

	public boolean isCrystalNetworkTile() {
		return CrystalNetworkTile.class.isAssignableFrom(tile);
	}

	public boolean isRepeater() {
		return CrystalRepeater.class.isAssignableFrom(tile);
	}

	public boolean needsRenderOffset() {
		return this == TURRET || this == PYLONTURBO;
	}

	public boolean isTextureFace() {
		switch(this) {
			case PERSONAL:
			case AUTOMATOR:
			case CLOAKING:
			case LIGHTER:
			case ESSENTIARELAY:
			case METEOR:
			case WIRELESS:
			case TELEPORT:
			case BOOKDECOMP:
			case GLOWFIRE:
			case FLUXMAKER:
			case FUNCTIONRELAY:
			case DIMENSIONCORE:
			case AURAPOINT:
			case MANABOOSTER:
			case OPTIMIZER:
			case VOIDTRAP:
			case LANDMARK:
			case SMELTERYDISTRIBUTOR:
				return true;
			default:
				return false;
		}
	}

	public boolean needsSilkTouch() {
		if (this == DIMENSIONCORE)
			return false;
		return block.getBlockInstance() instanceof BlockCrystalTile;
	}

	public boolean isLumenTile() {
		return LumenTile.class.isAssignableFrom(tile);
	}

	public boolean isIntangible() {
		switch(this) {
			case GLOWFIRE:
			case ESSENTIARELAY:
			case LUMENWIRE:
			case PARTICLES:
			case WIRELESS:
			case TELEPORT:
			case SKYPEATER:
			case FLUXMAKER:
			case FUNCTIONRELAY:
			case MANABOOSTER:
			case OPTIMIZER:
			case VOIDTRAP:
			case SMELTERYDISTRIBUTOR:
			case LANDMARK:
				return true;
			default:
				return false;
		}
	}

	public boolean providesCustomHitbox() {
		return CustomHitbox.class.isAssignableFrom(tile);
	}

	public boolean suppliesRedstone() {
		return RedstoneTile.class.isAssignableFrom(tile);
	}

	public boolean isPylonPowered() {
		return CrystalReceiver.class.isAssignableFrom(tile);
	}

	public boolean isRelayPowered() {
		return TileEntityRelayPowered.class.isAssignableFrom(tile);
	}

	public boolean isChargedCrystalPowered() {
		return ChargedCrystalPowered.class.isAssignableFrom(tile);
	}

	public boolean isWirelessPowered() {
		if (this == ADJACENCY)
			return ChromaOptions.POWEREDACCEL.getState();
		return TileEntityWirelessPowered.class.isAssignableFrom(tile);
	}

	public boolean isRotateableRepeater() {
		return this.isRepeater() && TileEntityCrystalRepeater.class.isAssignableFrom(tile) && this != BROADCAST;
	}

	public boolean isTurbochargeableRepeater() {
		return this.isRepeater() && this != WEAKREPEATER && this != IONOSPHERIC && this != SKYPEATER;
	}

	public boolean canPlayerPlace(EntityPlayer ep) {
		if (this == AURAPOINT)
			return ProgressStage.CTM.isPlayerAtStage(ep);
		return true;
	}

	public boolean hasTextureVariants() {
		return VariableTexture.class.isAssignableFrom(tile);
	}

	public ChromaResearch getFragment() {
		return ChromaResearch.getPageFor(this);
	}

	public boolean match(ItemStack is) {
		return ReikaItemHelper.matchStacks(is, this.getCraftedProduct());
	}

	public static ChromaTiles getTileByCraftedItem(ItemStack is) {
		return craftMap.get(is);
	}

}
