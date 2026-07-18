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

import java.util.HashMap;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.oredict.OreDictionary;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.BlockChromaTile;
import reika.chromaticraft.base.BlockModelledChromaTile;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.base.ItemBlockTileRegistry;
import reika.chromaticraft.block.BlockActiveChroma;
import reika.chromaticraft.block.BlockAdjacencyUpgrade;
import reika.chromaticraft.block.BlockCastingInjectorFocus;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockChromaPlantTile;
import reika.chromaticraft.block.BlockChromaPortal;
import reika.chromaticraft.block.BlockChromaTrail;
import reika.chromaticraft.block.BlockCrystalConsole;
import reika.chromaticraft.block.BlockCrystalFence;
import reika.chromaticraft.block.BlockCrystalHive;
import reika.chromaticraft.block.BlockCrystalPlant;
import reika.chromaticraft.block.BlockCrystalPylon;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockCrystalTank;
import reika.chromaticraft.block.BlockCrystalTile;
import reika.chromaticraft.block.BlockCrystalTileNonCube;
import reika.chromaticraft.block.BlockDecoPlant;
import reika.chromaticraft.block.BlockDummyAux;
import reika.chromaticraft.block.BlockEncrustedCrystal;
import reika.chromaticraft.block.BlockEnderTNT;
import reika.chromaticraft.block.BlockFakeSky;
import reika.chromaticraft.block.BlockHeatLamp;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.BlockHoverPad;
import reika.chromaticraft.block.BlockLiquidEnder;
import reika.chromaticraft.block.BlockMultiStorage;
import reika.chromaticraft.block.BlockPath;
import reika.chromaticraft.block.blockpath.PathType;
import reika.chromaticraft.block.BlockPolyCrystal;
import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockRFNode;
import reika.chromaticraft.block.BlockRedstonePod;
import reika.chromaticraft.block.BlockRift;
import reika.chromaticraft.block.BlockRouterNode;
import reika.chromaticraft.block.BlockSelectiveGlass;
import reika.chromaticraft.block.BlockTrapFloor;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.crystal.BlockCrystalGlass;
import reika.chromaticraft.block.crystal.BlockCrystalGlow;
import reika.chromaticraft.block.crystal.blockcrystalglow.Bases;
import reika.chromaticraft.block.crystal.BlockCrystalLamp;
import reika.chromaticraft.block.crystal.BlockPowerTree;
import reika.chromaticraft.block.crystal.BlockRainbowCrystal;
import reika.chromaticraft.block.crystal.BlockSuperCrystal;
import reika.chromaticraft.block.decoration.BlockAvoLamp;
import reika.chromaticraft.block.decoration.BlockColoredAltar;
import reika.chromaticraft.block.decoration.BlockEtherealLight;
import reika.chromaticraft.block.decoration.BlockMetaAlloyLamp;
import reika.chromaticraft.block.decoration.BlockMusicTrigger;
import reika.chromaticraft.block.decoration.BlockRangedLamp;
import reika.chromaticraft.block.decoration.BlockRepeaterLight;
import reika.chromaticraft.block.dimension.BlockBedrockCrack;
import reika.chromaticraft.block.dimension.BlockDimensionChunkloader;
import reika.chromaticraft.block.dimension.BlockDimensionDeco;
import reika.chromaticraft.block.dimension.BlockDimensionDecoTile;
import reika.chromaticraft.block.dimension.BlockLightedLeaf;
import reika.chromaticraft.block.dimension.BlockLightedLog;
import reika.chromaticraft.block.dimension.BlockLightedSapling;
import reika.chromaticraft.block.dimension.BlockLiquidLumen;
import reika.chromaticraft.block.dimension.BlockVoidCave;
import reika.chromaticraft.block.dimension.BlockVoidRift;
import reika.chromaticraft.block.dimension.structure.BlockRayblendFloor;
import reika.chromaticraft.block.dimension.structure.BlockSpecialShield;
import reika.chromaticraft.block.dimension.structure.BlockStructureDataStorage;
import reika.chromaticraft.block.dimension.structure.antfarm.BlockAntKey;
import reika.chromaticraft.block.dimension.structure.bridge.BlockBridgeControl;
import reika.chromaticraft.block.dimension.structure.bridge.BlockDynamicBridge;
import reika.chromaticraft.block.dimension.structure.gol.BlockGOLController;
import reika.chromaticraft.block.dimension.structure.gol.BlockGOLTile;
import reika.chromaticraft.block.dimension.structure.gravity.BlockGravityTile;
import reika.chromaticraft.block.dimension.structure.laser.BlockLaserEffector;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightPanel;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightSwitch;
import reika.chromaticraft.block.dimension.structure.locks.BlockColoredLock;
import reika.chromaticraft.block.dimension.structure.locks.BlockLockFence;
import reika.chromaticraft.block.dimension.structure.locks.BlockLockFreeze;
import reika.chromaticraft.block.dimension.structure.locks.BlockLockKey;
import reika.chromaticraft.block.dimension.structure.music.BlockMusicMemory;
import reika.chromaticraft.block.dimension.structure.noneuclid.BlockTeleport;
import reika.chromaticraft.block.dimension.structure.pinball.BlockPinballTile;
import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonController;
import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonTapeBit;
import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonTarget;
import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftKey;
import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock;
import reika.chromaticraft.block.dimension.structure.shiftmaze.blockshiftlock.Passability;
import reika.chromaticraft.block.dimension.structure.water.BlockEverFluid;
import reika.chromaticraft.block.dimension.structure.water.BlockRotatingLock;
import reika.chromaticraft.block.dye.BlockDye;
import reika.chromaticraft.block.dye.BlockDyeFlower;
import reika.chromaticraft.block.dye.BlockDyeGrass;
import reika.chromaticraft.block.dye.BlockDyeLeaf;
import reika.chromaticraft.block.dye.BlockDyeSapling;
import reika.chromaticraft.block.dye.BlockDyeVine;
import reika.chromaticraft.block.dye.BlockRainbowLeaf;
import reika.chromaticraft.block.dye.BlockRainbowSapling;
import reika.chromaticraft.block.relay.BlockFloatingRelay;
import reika.chromaticraft.block.relay.BlockLumenRelay;
import reika.chromaticraft.block.relay.BlockRelayFilter;
import reika.chromaticraft.block.worldgen.BlockCaveIndicator;
import reika.chromaticraft.block.worldgen.BlockChromaMud;
import reika.chromaticraft.block.worldgen.BlockCliffStone;
import reika.chromaticraft.block.worldgen.blockcliffstone.Variants;
import reika.chromaticraft.block.worldgen.BlockDecoFlower;
import reika.chromaticraft.block.worldgen.BlockEtherealLuma;
import reika.chromaticraft.block.worldgen.BlockLootChest;
import reika.chromaticraft.block.worldgen.BlockNetherBypassGate;
import reika.chromaticraft.block.worldgen.BlockSparkle;
import reika.chromaticraft.block.worldgen.BlockStructureShield;
import reika.chromaticraft.block.worldgen.BlockTieredOre;
import reika.chromaticraft.block.worldgen.BlockTieredPlant;
import reika.chromaticraft.block.worldgen.BlockUnknownArtefact;
import reika.chromaticraft.block.worldgen.BlockWarpNode;
import reika.chromaticraft.items.itemblock.ItemBlockChromaFlower;
import reika.chromaticraft.items.itemblock.ItemBlockChromaTiered;
import reika.chromaticraft.items.itemblock.ItemBlockCrystal;
import reika.chromaticraft.items.itemblock.ItemBlockCrystalColors;
import reika.chromaticraft.items.itemblock.ItemBlockCrystalGlow;
import reika.chromaticraft.items.itemblock.ItemBlockCrystalHive;
import reika.chromaticraft.items.itemblock.ItemBlockCrystalPlant;
import reika.chromaticraft.items.itemblock.ItemBlockCrystalTank;
import reika.chromaticraft.items.itemblock.ItemBlockDecoFlower;
import reika.chromaticraft.items.itemblock.ItemBlockDyeTypes;
import reika.chromaticraft.items.itemblock.ItemBlockHover;
import reika.chromaticraft.items.itemblock.ItemBlockLockKey;
import reika.chromaticraft.items.itemblock.ItemBlockLumenRelay;
import reika.chromaticraft.items.itemblock.ItemBlockMultiType;
import reika.chromaticraft.items.itemblock.ItemBlockPath;
import reika.chromaticraft.items.itemblock.ItemBlockRainbowLeaf;
import reika.chromaticraft.items.itemblock.ItemBlockRainbowSapling;
import reika.chromaticraft.items.itemblock.ItemBlockRangedLamp;
import reika.chromaticraft.items.itemblock.ItemBlockRouterNode;
import reika.chromaticraft.items.itemblock.ItemBlockSidePlaced;
import reika.chromaticraft.items.itemblock.ItemBlockStructShield;
import reika.chromaticraft.items.itemblock.ItemRedstonePodPlacer;
import reika.dragonapi.base.BlockCustomLeaf;
import reika.dragonapi.interfaces.registry.BlockEnum;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
import reika.dragonapi.libraries.java.ReikaStringParser;
import reika.dragonapi.libraries.world.ReikaBlockHelper;

public enum ChromaBlocks implements BlockEnum {

	TILEPLANT(BlockChromaPlantTile.class, 		ItemBlockChromaFlower.class, 	"Chromatic Plant"),
	TILEENTITY(BlockChromaTile.class, 			ItemBlockTileRegistry.class,	"Chromatic Tile"),
	TILEMODELLED(BlockModelledChromaTile.class, ItemBlockTileRegistry.class,	"Modelled Chromatic Tile"),
	RUNE(BlockCrystalRune.class, 				ItemBlockCrystalColors.class, 	"block.crystalrune"),
	CHROMA(BlockActiveChroma.class, 			ChromatiCraft.chroma, 			"fluid.chroma"),
	RIFT(BlockRift.class, 						ItemBlockTileRegistry.class,	"Rift"),
	CRYSTAL(BlockCaveCrystal.class, 			ItemBlockCrystal.class, 		"crystal.cave"), //Cave Crystal
	RAINBOWCRYSTAL(BlockRainbowCrystal.class, 									"crystal.rainbow"),
	LAMP(BlockCrystalLamp.class, 				ItemBlockCrystal.class, 		"crystal.lamp"),
	SUPER(BlockSuperCrystal.class, 				ItemBlockCrystal.class, 		"crystal.super"),
	PLANT(BlockCrystalPlant.class, 				ItemBlockCrystalPlant.class, 	"crystal.plant"),
	HIVE(BlockCrystalHive.class, 				ItemBlockCrystalHive.class, 	"block.crystalhive"),
	TILECRYSTAL(BlockCrystalTile.class,			ItemBlockTileRegistry.class,	"Crystal Tile"),
	TILECRYSTALNONCUBE(BlockCrystalTileNonCube.class,ItemBlockTileRegistry.class,"Crystal Tile Non-Cube"),
	DECAY(BlockDyeLeaf.class, 					ItemBlockDyeTypes.class, 		"dye.leaf"),
	DYELEAF(BlockDyeLeaf.class, 				ItemBlockDyeTypes.class, 		"dye.leaf"),
	DYESAPLING(BlockDyeSapling.class, 			ItemBlockDyeTypes.class, 		"dye.sapling"),
	DYE(BlockDye.class, 						ItemBlockDyeTypes.class, 		"dye.block"),
	RAINBOWLEAF(BlockRainbowLeaf.class, 		ItemBlockRainbowLeaf.class, 	"rainbow.leaf"),
	RAINBOWSAPLING(BlockRainbowSapling.class, 	ItemBlockRainbowSapling.class, 	"rainbow.sapling"),
	DYEFLOWER(BlockDyeFlower.class, 			ItemBlockDyeTypes.class, 		"dye.flower"),
	ENDER(BlockLiquidEnder.class, 				ChromatiCraft.ender,			"Liquid Ender"),
	DYEGRASS(BlockDyeGrass.class,				ItemBlockDyeTypes.class,		"dye.grass"),
	PYLONSTRUCT(BlockPylonStructure.class,		ItemBlockMultiType.class,		"block.pylon"),
	PYLON(BlockCrystalPylon.class,				ItemBlockTileRegistry.class,	"crystal.pylon"),
	TANK(BlockCrystalTank.class,				ItemBlockCrystalTank.class,		"crystal.tank"),
	FENCE(BlockCrystalFence.class,												"chroma.fencerelay"),
	TIEREDPLANT(BlockTieredPlant.class,			ItemBlockChromaTiered.class,	"chroma.tieredplant"),
	TIEREDORE(BlockTieredOre.class,				ItemBlockChromaTiered.class,	"chroma.tieredore"),
	DECOPLANT(BlockDecoPlant.class, 			ItemBlockChromaFlower.class, 	"Chromatic Plant 2"),
	POWERTREE(BlockPowerTree.class,				ItemBlockDyeTypes.class,		"chroma.powerleaf"),
	TILEMODELLED2(BlockModelledChromaTile.class, 								"Modelled Chromatic Tile 2"),
	LAMPBLOCK(BlockRangedLamp.class,			ItemBlockRangedLamp.class,		"chroma.lampblock"),
	TNT(BlockEnderTNT.class,													"chroma.endertnt"),
	PATH(BlockPath.class,						ItemBlockPath.class,			"chroma.path"),
	STRUCTSHIELD(BlockStructureShield.class,	ItemBlockStructShield.class,	"chroma.shield"),
	LOOTCHEST(BlockLootChest.class,												"chroma.loot"),
	PORTAL(BlockChromaPortal.class,												"chroma.portal"),
	RELAY(BlockLumenRelay.class,				ItemBlockLumenRelay.class,		"chroma.relay"),
	GLOW(BlockCrystalGlow.class,				ItemBlockCrystalGlow.class,		"chroma.glow"),
	HEATLAMP(BlockHeatLamp.class,				ItemBlockSidePlaced.class,		"chroma.heatlamp"),
	VOIDRIFT(BlockVoidRift.class,				ItemBlockDyeTypes.class,		"chroma.voidrift"),
	DIMGEN(BlockDimensionDeco.class,			ItemBlockMultiType.class,		"chroma.dimdeco"),
	DIMGENTILE(BlockDimensionDecoTile.class,	ItemBlockMultiType.class,		"chroma.dimdeco2"),
	COLORLOCK(BlockColoredLock.class,											"chroma.colorlock"),
	DIMDATA(BlockStructureDataStorage.class,	ItemBlockMultiType.class,		"chroma.dimdata"),
	LOCKFENCE(BlockLockFence.class,												"chroma.lockfence"),
	LOCKFREEZE(BlockLockFreeze.class,											"chroma.lockfreeze"),
	LOCKKEY(BlockLockKey.class,					ItemBlockLockKey.class,			"chroma.lockkey"),
	GLOWLEAF(BlockLightedLeaf.class,											"chroma.glowleaf"),
	GLOWLOG(BlockLightedLog.class,												"chroma.glowlog"),
	GLOWSAPLING(BlockLightedSapling.class,										"chroma.glowsapling"),
	HOVER(BlockHoverBlock.class,				ItemBlockHover.class,			"chroma.hover"),
	GOL(BlockGOLTile.class,														"chroma.gol"),
	GOLCONTROL(BlockGOLController.class,										"chroma.golcontrol"),
	MUSICMEMORY(BlockMusicMemory.class,											"chroma.musicmem"),
	MUSICTRIGGER(BlockMusicTrigger.class,										"chroma.musictrigger"),
	SHIFTKEY(BlockShiftKey.class,				ItemBlockMultiType.class,		"chroma.shiftkey"),
	SHIFTLOCK(BlockShiftLock.class,				ItemBlockMultiType.class,		"chroma.shiftlock"),
	TELEPORT(BlockTeleport.class,												"chroma.teleportblock"),
	SPECIALSHIELD(BlockSpecialShield.class,		ItemBlockStructShield.class,	"chroma.specialshield"),
	COLORALTAR(BlockColoredAltar.class,			ItemBlockDyeTypes.class,		"chroma.coloraltar"),
	DOOR(BlockChromaDoor.class,													"chroma.door"),
	GLASS(BlockCrystalGlass.class,				ItemBlockDyeTypes.class,		"chroma.glass"),
	CONSOLE(BlockCrystalConsole.class,											"chroma.console"),
	LIGHT(BlockEtherealLight.class,												"chroma.light"),
	STORAGE(BlockMultiStorage.class,											"chroma.storageblock"),
	DECOFLOWER(BlockDecoFlower.class,			ItemBlockDecoFlower.class,		"chroma.decoflower"),
	SELECTIVEGLASS(BlockSelectiveGlass.class,									"chroma.selectiveglass"),
	TILEMODELLED3(BlockModelledChromaTile.class, 								"Modelled Chromatic Tile 3"),
	PAD(BlockHoverPad.class,					ItemBlockMultiType.class,		"chroma.hoverpadaux"),
	ADJACENCY(BlockAdjacencyUpgrade.class,										"Adjacency Upgrade"),
	ANTKEY(BlockAntKey.class,					ItemBlockMultiType.class,		"chroma.antkey"),
	LASEREFFECT(BlockLaserEffector.class,		ItemBlockMultiType.class,		"chroma.lasereffect"),
	PINBALL(BlockPinballTile.class,				ItemBlockMultiType.class,		"chroma.pinball"),
	GRAVITY(BlockGravityTile.class,				ItemBlockMultiType.class,		"chroma.gravity"),
	TRAIL(BlockChromaTrail.class,												"chroma.trail"),
	BRIDGE(BlockDynamicBridge.class,			ItemBlockMultiType.class,		"chroma.bridge"),
	BRIDGECONTROL(BlockBridgeControl.class,		ItemBlockMultiType.class,		"chroma.bridgecontrol"),
	SPARKLE(BlockSparkle.class,					ItemBlockMultiType.class,		"chroma.sparkle"),
	LUMA(BlockEtherealLuma.class,				ChromatiCraft.luma,				"fluid.luma"),
	TILEENTITY2(BlockChromaTile.class, 											"Chromatic Tile 2"),
	AVOLAMP(BlockAvoLamp.class,					ItemBlockSidePlaced.class,		"chroma.avolamp"),
	RELAYFILTER(BlockRelayFilter.class,											"chroma.relayfilter"),
	ROUTERNODE(BlockRouterNode.class,			ItemBlockRouterNode.class,		"chroma.routernode"),
	EVERFLUID(BlockEverFluid.class,												"chroma.everfluid"),
	LIGHTPANEL(BlockLightPanel.class,			ItemBlockMultiType.class,		"chroma.lightpanel"),
	PANELSWITCH(BlockLightSwitch.class,											"chroma.panelswitch"),
	ARTEFACT(BlockUnknownArtefact.class,										"chroma.artefactblock"),
	DUMMYAUX(BlockDummyAux.class,												"chroma.dummyaux"),
	CLIFFSTONE(BlockCliffStone.class,			ItemBlockMultiType.class,		"chroma.cliffstone"),
	//LOREREADER(BlockLoreReader.class,											"chroma.lorereader");
	CAVEINDICATOR(BlockCaveIndicator.class,										"chroma.caveindicator"),
	TILEMODELLED4(BlockModelledChromaTile.class, 								"Modelled Chromatic Tile 4"),
	FLOATINGRELAY(BlockFloatingRelay.class,										"Ethereal Relay"),
	REPEATERLAMP(BlockRepeaterLight.class,		ItemBlockMultiType.class,		"Repeater Lamp"),
	WATERLOCK(BlockRotatingLock.class,											"chroma.lock"),
	REDSTONEPOD(BlockRedstonePod.class,			ItemRedstonePodPlacer.class,	"chroma.redstonepod"),
	POLYCRYSTAL(BlockPolyCrystal.class,											"chroma.polycrystal"),
	MOLTENLUMEN(BlockLiquidLumen.class, 		ChromatiCraft.lumen,			"Molten Lumen"),
	METAALLOYLAMP(BlockMetaAlloyLamp.class,		ItemBlockSidePlaced.class,		"chroma.metaalloy"),
	WARPNODE(BlockWarpNode.class,												"chroma.warpnode"),
	FAKESKY(BlockFakeSky.class,													"chroma.fakesky"),
	CHUNKLOADER(BlockDimensionChunkloader.class,								"chroma.chunkloader"),
	//SPAWNERCONTROL(BlockSpawnerShutdown.class,	ItemBlockSidePlaced.class,		"chroma.spawnershutdown"),
	TRAPFLOOR(BlockTrapFloor.class,												"chroma.trapfloor"),
	BEDROCKCRACK(BlockBedrockCrack.class,										"chroma.bedrockcrack"),
	RFPOD(BlockRFNode.class,					ItemBlockSidePlaced.class,		"chroma.rfpod"),
	PISTONBIT(BlockPistonTapeBit.class,			ItemBlockMultiType.class,		"chroma.pistonbit"),
	PISTONTARGET(BlockPistonTarget.class,										"chroma.pistontarget"),
	PISTONCONTROL(BlockPistonController.class,									"chroma.pistoncontrol"),
	RAYBLEND(BlockRayblendFloor.class,											"chroma.rayblend"),
	ENCRUSTED(BlockEncrustedCrystal.class,		ItemBlockDyeTypes.class,		"chroma.encrusted"),
	INJECTORAUX(BlockCastingInjectorFocus.class,								"chroma.injectorfocus"),
	//LIFEWATER(BlockTempleLifewater.class,		ChromatiCraft.lifewater,		"chroma.lifewater"),
	DYEVINE(BlockDyeVine.class,					ItemBlockDyeTypes.class,		"chroma.dyevine"),
	FERTILEDYEVINE(BlockDyeVine.class,			ItemBlockDyeTypes.class,		"chroma.dyevinefertile"),
	NETHERGATE(BlockNetherBypassGate.class,										"chroma.nethergate"),
	VOIDCAVE(BlockVoidCave.class,				ItemBlockMultiType.class,		"chroma.voidcave"),
	MUD(BlockChromaMud.class,													"chroma.mud"),
	;

	private Class blockClass;
	private String blockName;
	private Class itemBlock;
	private Fluid fluid;

	public static final ChromaBlocks[] blockList = values();
	private static final HashMap<Block, ChromaBlocks> blockMap = new HashMap();

	private ChromaBlocks(Class <? extends Block> cl, Class<? extends ItemBlock> ib, Fluid f, String n) {
		blockClass = cl;
		blockName = n;
		itemBlock = ib;
		fluid = f;
	}

	private ChromaBlocks(Class <? extends Block> cl, Fluid f, String n) {
		this(cl, null, f, n);
	}

	private ChromaBlocks(Class <? extends Block> cl, Class<? extends ItemBlock> ib, String n) {
		this(cl, ib, null, n);
	}

	private ChromaBlocks(Class <? extends Block> cl, String n) {
		this(cl, null, null, n);
	}

	public Material getBlockMaterial() {
		if (this.isCrystal())
			return ChromatiCraft.crystalMat;
		switch(this) {
			case SELECTIVEGLASS:
				return Material.glass;
			case TILEPLANT:
			case TIEREDPLANT:
			case DECOPLANT:
			case DECOFLOWER:
			case PLANT:
			case METAALLOYLAMP:
			case DYEVINE:
			case FERTILEDYEVINE:
				return Material.plants;
			case CHROMA:
				//case ACTIVECHROMA:
			case ENDER:
			case LUMA:
			case EVERFLUID:
				//case LIFEWATER:
				return Material.water;
			case MOLTENLUMEN:
				return Material.lava;
			case TILECRYSTAL:
			case TILECRYSTALNONCUBE:
			case PYLON:
				//case FIBER:
			case POWERTREE:
			case RELAY:
			case ADJACENCY:
			case RELAYFILTER:
			case FLOATINGRELAY:
			case REPEATERLAMP:
			case POLYCRYSTAL:
				return ChromatiCraft.crystalMat;
			case AVOLAMP:
				return Material.iron;
			case TNT:
				return Material.tnt;
			case PORTAL:
				return Material.portal;
			case GLOWLOG:
				return Material.wood;
			case GLOWLEAF:
				return Material.leaves;
			case HOVER:
			case TRAIL:
				return ChromatiCraft.airMat;
			case LIGHT:
			case ROUTERNODE:
				return Material.circuits;
			case SPARKLE:
			case CLIFFSTONE:
			case MUD:
				return Material.ground;
			default:
				return Material.rock;
		}
	}

	public boolean isFluid() {
		return fluid != null;
	}

	public Fluid getFluid() {
		return fluid;
	}

	public boolean isDye() {
		switch(this) {
			case DYE:
			case DYELEAF:
			case DECAY:
			case DYESAPLING:
			case DYEFLOWER:
			case DYEGRASS:
			case DYEVINE:
			case FERTILEDYEVINE:
				return true;
			default:
				return false;
		}
	}

	public boolean isLeaf() {
		return BlockCustomLeaf.class.isAssignableFrom(blockClass);
	}

	public boolean isDyePlant() {
		return this == DYESAPLING || this == DYEGRASS || this == DYEFLOWER || this == DYEVINE || this == FERTILEDYEVINE;
	}

	public boolean isSapling() {
		return BlockSapling.class.isAssignableFrom(blockClass);
	}

	public boolean isTechnical() {
		return BlockChromaTile.class.isAssignableFrom(blockClass) || ReikaBlockHelper.isLiquid(this.getBlockInstance());
	}

	@Override
	public Class[] getConstructorParamTypes() {
		if (this.isFluid())
			return new Class[]{Fluid.class, Material.class};
		if (this == DECAY || this == DYELEAF || this == DYEVINE || this == FERTILEDYEVINE)
			return new Class[]{boolean.class};
		if (this.isLeaf() || this.isDyePlant() || this.isSapling())
			return new Class[0];
		if (this == GLOWLOG)
			return new Class[0];
		if (this == DECOFLOWER)
			return new Class[0];
		if (this == NETHERGATE)
			return new Class[0];
		return new Class[]{Material.class};
	}

	@Override
	public Object[] getConstructorParams() {
		if (this.isFluid())
			return new Object[]{this.getFluid(), this.getBlockMaterial()};
		if (this == DECAY || this == DYELEAF)
			return new Object[]{this == DECAY};
		if (this == DYEVINE || this == FERTILEDYEVINE)
			return new Object[]{this == FERTILEDYEVINE};
		if (this.isLeaf() || this.isDyePlant() || this.isSapling())
			return new Object[0];
		if (this == GLOWLOG)
			return new Object[0];
		if (this == DECOFLOWER)
			return new Object[0];
		if (this == NETHERGATE)
			return new Object[0];
		return new Object[]{this.getBlockMaterial()};
	}

	@Override
	public String getUnlocalizedName() {
		return ReikaStringParser.stripSpaces(blockName);
	}

	@Override
	public Class getObjectClass() {
		return blockClass;
	}

	@Override
	public String getBasicName() {
		return StatCollector.translateToLocal(blockName);
	}

	@Override
	public String getMultiValuedName(int meta) {
		if (!this.hasMultiValuedName())
			return this.getBasicName();
		if (meta == OreDictionary.WILDCARD_VALUE)
			return this.getBasicName()+" (Any)";
		if (this == GLOW) {
			return Bases.baseList[meta/16].displayName+"-Based "+CrystalElement.elements[meta%16].displayName+" "+this.getBasicName();
		}
		if (this.isCrystal() || this.isDye())
			return CrystalElement.elements[meta].displayName+" "+this.getBasicName();
		switch(this) {
			case RUNE:
			case PLANT: //"Crystal Bloom"
			case LAMPBLOCK:
			case POWERTREE:
			case VOIDRIFT:
				return CrystalElement.elements[meta%16].displayName+" "+this.getBasicName();
			case HIVE:
				return meta == 0 ? "Crystal Hive" : "Pure Hive";
			case PYLON:
				return this.getBasicName();
			case PYLONSTRUCT:
				return StatCollector.translateToLocal("chromablock.pylon."+meta);
			case TIEREDORE:
			case TIEREDPLANT:
				return StatCollector.translateToLocal(this.getBasicName()+"."+meta);
			case PATH:
				return PathType.list[meta].name+" "+this.getBasicName();
			case STRUCTSHIELD:
				return this.getBasicName()+" "+BlockStructureShield.BlockType.list[meta%8].name;
			case SPECIALSHIELD:
				return this.getBasicName();
			case RELAY:
				return (meta == 16 ? "Omni" : CrystalElement.elements[meta].displayName)+" "+this.getBasicName();
			case DIMGEN:
				return StatCollector.translateToLocal("chromablock.dimgen."+BlockDimensionDeco.DimDecoTypes.list[meta].name().toLowerCase(Locale.ENGLISH));
			case DIMGENTILE:
				return StatCollector.translateToLocal("chromablock.dimgen."+BlockDimensionDecoTile.DimDecoTileTypes.list[meta].name().toLowerCase(Locale.ENGLISH));
			case DIMDATA:
				return StatCollector.translateToLocal("chromablock.dimdata."+meta);
			case LOCKKEY:
			case HOVER:
				return this.getBasicName();
			case DECOFLOWER:
				return StatCollector.translateToLocal("chroma.flower."+BlockDecoFlower.Flowers.list[meta].name().toLowerCase(Locale.ENGLISH));
			case LASEREFFECT:
				return StatCollector.translateToLocal("chromablock.laser."+BlockLaserEffector.LaserEffectType.list[meta].name().toLowerCase(Locale.ENGLISH));
			case PINBALL:
				return StatCollector.translateToLocal("chromablock.pinball."+BlockPinballTile.PinballRerouteType.list[meta].name().toLowerCase(Locale.ENGLISH));
			case GRAVITY:
				return StatCollector.translateToLocal("chromablock.gravity."+BlockGravityTile.GravityTiles.list[meta].name().toLowerCase(Locale.ENGLISH));
			case ANTKEY:
				return this.getBasicName()+" Size "+(meta+1);
			case BRIDGE:
			case BRIDGECONTROL:
				return this.getBasicName();
			case SPARKLE:
				return "Sparkling "+new ItemStack(BlockSparkle.BlockTypes.list[meta].getBlockProxy()).getDisplayName();
			case ROUTERNODE:
				return StatCollector.translateToLocal("chromablock.routernode."+meta);
			case CLIFFSTONE:
				return "Cliff "+Variants.getVariant(meta).getBlockProxy().getLocalizedName();
			case REPEATERLAMP:
				return BlockRepeaterLight.MODELS[meta].getName()+" Lamp";
			case HEATLAMP:
				return meta >= 8 ? "Freeze Lamp" : "Heat Lamp";
			case SHIFTLOCK:
				return ReikaObfuscationHelper.isDeObfEnvironment() ? this.getBasicName()+" ["+Passability.list[meta]+"]" : this.getBasicName();
			case PAD:
				return meta == 0 ? this.getBasicName() : this.getBasicName()+" (Decoration)";
			case PISTONTARGET:
			case PISTONCONTROL:
				return StatCollector.translateToLocal(this.getBasicName()+"."+meta);
			case PISTONBIT:
				return this.getBasicName()+" ("+ReikaStringParser.capFirstChar(BlockPistonTapeBit.getColor(meta).getName())+")";
			default:
				return "";
		}
	}

	@Override
	public boolean hasMultiValuedName() {
		switch(this) {
			case CHROMA:
			case TANK:
			case TNT:
			case FENCE:
			case LOOTCHEST:
			case PORTAL:
			case COLORLOCK:
			case LOCKKEY:
			case LOCKFENCE:
			case LOCKFREEZE:
			case GLOWLOG:
			case GLOWLEAF:
			case GLOWSAPLING:
			case HOVER:
			case GOL:
			case GOLCONTROL:
			case MUSICMEMORY:
			case MUSICTRIGGER:
			case SHIFTKEY:
			case TELEPORT:
			case DOOR:
			case CONSOLE:
			case LIGHT:
			case SELECTIVEGLASS:
				//case PAD:
			case TRAIL:
			case BRIDGE:
			case BRIDGECONTROL:
			case LUMA:
			case AVOLAMP:
			case RELAYFILTER:
			case EVERFLUID:
			case WATERLOCK:
			case PANELSWITCH:
			case LIGHTPANEL:
			case DUMMYAUX:
				//case LOREREADER:
			case ARTEFACT:
			case CAVEINDICATOR:
			case FLOATINGRELAY:
			case REDSTONEPOD:
			case POLYCRYSTAL:
			case METAALLOYLAMP:
			case FAKESKY:
			case WARPNODE:
			case CHUNKLOADER:
				//case SPAWNERCONTROL:
			case TRAPFLOOR:
			case BEDROCKCRACK:
			case RFPOD:
				//case PISTONBIT:
			case RAYBLEND:
			case INJECTORAUX:
			case NETHERGATE:
			case VOIDCAVE:
			case MUD:
				return false;
			default:
				return true;
		}
	}

	public boolean isCrystal() {
		return CrystalTypeBlock.class.isAssignableFrom(blockClass);
	}

	@Override
	public int getNumberMetadatas() {
		if (this.isCrystal() || this.isDye())
			return CrystalElement.elements.length;
		switch(this) {
			case RUNE:
			case PLANT:
			case VOIDRIFT:
				return 16;
			case HIVE:
				return 2;
			case PYLON:
				return 2;
			case PYLONSTRUCT:
				return 16;
			case PATH:
				return BlockPath.PathType.list.length;
			case STRUCTSHIELD:
				return BlockStructureShield.BlockType.list.length;
			case DIMGEN:
				return BlockDimensionDeco.DimDecoTypes.list.length;
			case DIMGENTILE:
				return BlockDimensionDecoTile.DimDecoTileTypes.list.length;
			case DIMDATA:
				return 2;
			case LOCKKEY:
				return BlockLockKey.LockChannel.lockList.length;
			case HOVER:
				return BlockHoverBlock.HoverType.list.length;
			case DECOFLOWER:
				return BlockDecoFlower.Flowers.list.length;
			case LASEREFFECT:
				return BlockLaserEffector.LaserEffectType.list.length;
			case PINBALL:
				return BlockPinballTile.PinballRerouteType.list.length;
			case GRAVITY:
				return BlockGravityTile.GravityTiles.list.length;
			case ANTKEY:
				return 16;
			case SPARKLE:
				return BlockSparkle.BlockTypes.list.length;
			case LIGHTPANEL:
				return 6;
			case CLIFFSTONE:
				return Variants.list.length;
			case REPEATERLAMP:
				return BlockRepeaterLight.MODELS.length;
			case HEATLAMP:
				return 9;
			case SHIFTLOCK:
				return Passability.list.length;
			case PAD:
				return 2;
			case NETHERGATE:
				return BlockNetherBypassGate.GateLevels.list.length;
			default:
				return 1;
		}
	}

	@Override
	public Class<? extends ItemBlock> getItemBlock() {
		return itemBlock;
	}

	@Override
	public boolean hasItemBlock() {
		return itemBlock != null;
	}

	public boolean isDummiedOut() {
		return blockClass == null;
	}

	public Block getBlockInstance() {
		return ChromatiCraft.blocks[this.ordinal()];
	}

	public static ChromaBlocks getEntryByID(Block id) {
		return blockMap.get(id);
	}

	public static ChromaBlocks getEntryByItem(ItemStack is) {
		Block b = Block.getBlockFromItem(is.getItem());
		return b != null ? getEntryByID(b) : null;
	}

	public Item getItem() {
		return Item.getItemFromBlock(this.getBlockInstance());
	}

	public static void loadMappings() {
		for (int i = 0; i < blockList.length; i++) {
			Block b = blockList[i].getBlockInstance();
			blockMap.put(b, blockList[i]);
		}
	}

	public boolean match(ItemStack is) {
		return is != null && is.getItem() == Item.getItemFromBlock(this.getBlockInstance());
	}

	public ItemStack getStackOf() {
		return this.getStackOfMetadata(0);
	}

	public ItemStack getStackOf(CrystalElement e) {
		return this.getStackOfMetadata(e.ordinal());
	}

	public ItemStack getStackOfMetadata(int meta) {
		return new ItemStack(this.getBlockInstance(), 1, meta);
	}

	public boolean hasModel() {
		if (this == TILEMODELLED || this == TILEMODELLED2 || this == PYLON || this == TILECRYSTALNONCUBE)
			return true;
		return false;
	}

	public boolean isDimensionStructureBlock() {
		return blockClass.getName().startsWith("Reika.ChromatiCraft.Block.Dimension.Structure");
	}

	public boolean isMetaInCreative(int meta) {
		if (this == LIGHTPANEL)
			return meta%2 == 0;
		if (this == HEATLAMP)
			return meta == 0 || meta == 8;
		return true;
	}

	public ChromaResearch getFragment() {
		return ChromaResearch.getPageFor(this.getStackOf());
	}

}
