/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.BiomeManager.BiomeType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import reika.chromaticraft.api.AdjacencyUpgradeAPI;
import reika.chromaticraft.api.interfaces.AdjacencyCheckHandler;
import reika.chromaticraft.api.interfaces.CustomAdjacencyHandler;
import reika.chromaticraft.auxiliary.CCTradeHandler;
import reika.chromaticraft.auxiliary.ChromaASMHandler;
import reika.chromaticraft.auxiliary.ChromaAux;
import reika.chromaticraft.auxiliary.ChromaBookSpawner;
import reika.chromaticraft.auxiliary.ChromaDescriptions;
import reika.chromaticraft.auxiliary.ChromaLock;
import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.CrystalMaterial;
import reika.chromaticraft.auxiliary.crystalnetworklogger.NetworkLoggerCommand;
import reika.chromaticraft.auxiliary.CrystalPlantHandler;
import reika.chromaticraft.auxiliary.ExplorationMonitor;
import reika.chromaticraft.auxiliary.GuardianStoneManager;
import reika.chromaticraft.auxiliary.MusicLoader;
import reika.chromaticraft.auxiliary.PylonCacheLoader;
import reika.chromaticraft.auxiliary.PylonDamage;
import reika.chromaticraft.auxiliary.tooldispenserhandlers.BucketDispenserAction;
import reika.chromaticraft.auxiliary.tooldispenserhandlers.ManipulatorDispenserAction;
import reika.chromaticraft.auxiliary.tooldispenserhandlers.ProjectileToolDispenserAction;
import reika.chromaticraft.auxiliary.apiimpl.CCAPICore;
import reika.chromaticraft.auxiliary.ability.AbilityHelper;
import reika.chromaticraft.auxiliary.ability.ChromabilityHandler;
import reika.chromaticraft.auxiliary.command.CrystalNetCommand;
import reika.chromaticraft.auxiliary.command.DimensionGeneratorCommand;
import reika.chromaticraft.auxiliary.command.GuardianCommand;
import reika.chromaticraft.auxiliary.command.NodeWrapperInspectionCommand;
import reika.chromaticraft.auxiliary.command.PlaceStructureCommand;
import reika.chromaticraft.auxiliary.command.ProgressModifyCommand;
import reika.chromaticraft.auxiliary.command.PylonCacheCommand;
import reika.chromaticraft.auxiliary.command.RecipeReloadCommand;
import reika.chromaticraft.auxiliary.command.RedecorateCommand;
import reika.chromaticraft.auxiliary.command.ReshufflePylonCommand;
import reika.chromaticraft.auxiliary.command.StructureGenCommand;
import reika.chromaticraft.auxiliary.command.StructureMapCommand;
import reika.chromaticraft.auxiliary.interfaces.ChromaDecorator;
import reika.chromaticraft.auxiliary.interfaces.LoadRegistry;
import reika.chromaticraft.auxiliary.recipemanagers.RecipesCastingTable;
import reika.chromaticraft.auxiliary.recipemanagers.TransmutationRecipes;
import reika.chromaticraft.auxiliary.render.ChromaFontRenderer;
import reika.chromaticraft.auxiliary.render.ChromaHelpHUD;
import reika.chromaticraft.auxiliary.render.ChromaOverlays;
import reika.chromaticraft.auxiliary.render.MobSonarRenderer;
import reika.chromaticraft.auxiliary.render.OreOverlayRenderer;
import reika.chromaticraft.auxiliary.render.PylonFinderOverlay;
import reika.chromaticraft.auxiliary.render.StructureErrorOverlays;
import reika.chromaticraft.auxiliary.structure.worldgen.BurrowStructure;
import reika.chromaticraft.auxiliary.tab.FragmentTab;
import reika.chromaticraft.auxiliary.tab.TabChromatiCraft;
import reika.chromaticraft.base.tileentity.TileEntityWirelessPowered;
import reika.chromaticraft.block.worldgen.blockstructureshield.BlockType;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.entity.EntityMeteorShot;
import reika.chromaticraft.items.tools.ItemAuraPouch;
import reika.chromaticraft.items.tools.wands.ItemDuplicationWand;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.magic.artefact.ArtefactSpawner;
import reika.chromaticraft.magic.artefact.UABombingEffects;
import reika.chromaticraft.magic.artefact.UATrades;
import reika.chromaticraft.magic.lore.RosettaStone;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.potions.PotionBetterSaturation;
import reika.chromaticraft.magic.potions.PotionCustomRegen;
import reika.chromaticraft.magic.potions.PotionGrowthHormone;
import reika.chromaticraft.magic.potions.PotionLumarhea;
import reika.chromaticraft.magic.potions.PotionLumenRegen;
import reika.chromaticraft.magic.potions.PotionVoidGaze;
import reika.chromaticraft.magic.progression.ProgressionLoadHandler;
import reika.chromaticraft.modinterface.IC2ReactorAcceleration;
import reika.chromaticraft.modinterface.ModInteraction;
import reika.chromaticraft.modinterface.reservoirhandlers.ChromaPrepHandler;
import reika.chromaticraft.modinterface.reservoirhandlers.PoolRecipeHandler;
import reika.chromaticraft.modinterface.reservoirhandlers.ShardBoostingHandler;
import reika.chromaticraft.modinterface.TreeCapitatorHandler;
import reika.chromaticraft.modinterface.thaumcraft.ChromaAspectManager;
import reika.chromaticraft.modinterface.thaumcraft.NodeRecharger;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityAspectFormer;
import reika.chromaticraft.modinterface.voidritual.VoidMonsterRitualClientEffects;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.ChromaEntities;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaResearch;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ExtraChromaIDs;
import reika.chromaticraft.registry.ItemMagicRegistry;
import reika.chromaticraft.render.CCParticleEngine;
import reika.chromaticraft.tileentity.TileEntityBiomePainter;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityOreCreator;
import reika.chromaticraft.tileentity.acquisition.TileEntityMiner;
import reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;
import reika.chromaticraft.world.BiomeEnderForest;
import reika.chromaticraft.world.BiomeGlowingCliffs;
import reika.chromaticraft.world.BiomeRainbowForest;
import reika.chromaticraft.world.EndOverhaulManager;
import reika.chromaticraft.world.GlowingCliffsEdge;
import reika.chromaticraft.world.RainbowRiver;
import reika.chromaticraft.world.VillagersFailChromatiCraft;
import reika.chromaticraft.world.dimension.CheatingPreventionSystem;
import reika.chromaticraft.world.dimension.ChromaDimensionManager;
import reika.chromaticraft.world.dimension.ChromaDimensionTicker;
import reika.chromaticraft.world.dimension.ChunkProviderChroma;
import reika.chromaticraft.world.dimension.DimensionJoinHandler;
import reika.chromaticraft.world.iwg.CaveIndicatorGenerator;
import reika.chromaticraft.world.iwg.ColorTreeGenerator;
import reika.chromaticraft.world.iwg.CrystalGenerator;
import reika.chromaticraft.world.iwg.DataTowerGenerator;
import reika.chromaticraft.world.iwg.DecoFlowerGenerator;
import reika.chromaticraft.world.iwg.DungeonGenerator;
import reika.chromaticraft.world.iwg.GlowingCliffsAuxGenerator;
import reika.chromaticraft.world.iwg.LumaGenerator;
import reika.chromaticraft.world.iwg.PylonGenerator;
import reika.chromaticraft.world.iwg.SkypeaterGenerator;
import reika.chromaticraft.world.iwg.TieredWorldGenerator;
import reika.chromaticraft.world.iwg.WarpNodeGenerator;
import reika.chromaticraft.world.nether.NetherStructureGenerator;
import reika.dragonapi.DragonAPICore;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.CreativeTabSorter;
import reika.dragonapi.auxiliary.IconLookupRegistry;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.auxiliary.SpecialBiomePlacementRegistry;
import reika.dragonapi.auxiliary.specialbiomeplacementregistry.Category;
import reika.dragonapi.auxiliary.VillageTradeHandler;
import reika.dragonapi.auxiliary.WorldGenInterceptionRegistry;
import reika.dragonapi.auxiliary.trackers.CommandableUpdateChecker;
import reika.dragonapi.auxiliary.trackers.ConfigMatcher;
import reika.dragonapi.auxiliary.trackers.DonatorController;
import reika.dragonapi.auxiliary.trackers.FurnaceFuelRegistry;
import reika.dragonapi.auxiliary.trackers.IDCollisionTracker;
import reika.dragonapi.auxiliary.trackers.IntegrityChecker;
import reika.dragonapi.auxiliary.trackers.ModLockController;
import reika.dragonapi.auxiliary.trackers.PackModificationTracker;
import reika.dragonapi.auxiliary.trackers.PlayerFirstTimeTracker;
import reika.dragonapi.auxiliary.trackers.PlayerHandler;
import reika.dragonapi.auxiliary.trackers.RetroGenController;
import reika.dragonapi.auxiliary.trackers.SuggestedModsTracker;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.auxiliary.trackers.VanillaIntegrityTracker;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.base.dragonapimod.loadprofiler.LoadPhase;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.extras.PseudoAirMaterial;
import reika.dragonapi.instantiable.EnhancedFluid;
import reika.dragonapi.instantiable.RayTracer;
import reika.dragonapi.instantiable.event.client.GameFinishedLoadingEvent;
import reika.dragonapi.instantiable.io.ModLogger;
import reika.dragonapi.libraries.ReikaDispenserHelper;
import reika.dragonapi.libraries.ReikaRegistryHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.libraries.world.ReikaBiomeHelper;
import reika.dragonapi.modinteract.BannedItemReader;
import reika.dragonapi.modinteract.ItemStackRepository;
import reika.dragonapi.modinteract.ReikaClimateControl;
import reika.dragonapi.modinteract.ReikaEEHelper;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;
import reika.dragonapi.modinteract.deepinteract.MTInteractionManager;
import reika.dragonapi.modinteract.deepinteract.ReikaMystcraftHelper;
import reika.dragonapi.modinteract.deepinteract.SensitiveFluidRegistry;
import reika.dragonapi.modinteract.deepinteract.SensitiveItemRegistry;
import reika.dragonapi.modinteract.deepinteract.TimeTorchHelper;
import reika.dragonapi.modinteract.itemhandlers.BloodMagicHandler;
import reika.dragonapi.modinteract.itemhandlers.ThermalHandler;
import reika.dragonapi.modinteract.itemhandlers.tinkerblockhandler.Pulses;
import reika.dragonapi.modinteract.lua.LuaMethod;
import reika.dragonapi.modregistry.ModCropList;
import reika.meteorcraft.api.MeteorSpawnAPI;
import reika.rotarycraft.api.ReservoirAPI;
import reika.voidmonster.api.DimensionAPI;
import reika.voidmonster.api.MonsterAPI;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ttftcuts.atg.api.ATGBiomes;



@Mod( modid = "ChromatiCraft", name="ChromatiCraft", version = "v@MAJOR_VERSION@@MINOR_VERSION@", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI;before:climatecontrol")

public class ChromatiCraft extends DragonAPIMod {
	public static final String packetChannel = "ChromaData";

	public static final TabChromatiCraft tabChroma = new TabChromatiCraft("ChromatiCraft");
	public static final TabChromatiCraft tabChromaDeco = new TabChromatiCraft("ChromatiCraft Deco");
	public static final TabChromatiCraft tabChromaGen = new TabChromatiCraft("ChromatiCraft Worldgen");
	public static final TabChromatiCraft tabChromaTools = new TabChromatiCraft("ChromatiCraft Tools");
	public static final TabChromatiCraft tabChromaItems = new TabChromatiCraft("ChromatiCraft Items");
	public static final FragmentTab tabChromaFragments = new FragmentTab("CC Info Fragments");

	public static final ArmorMaterial FLOATSTONE = EnumHelper.addArmorMaterial("Floatstone", 65536, new int[]{0, 0, 0, 0}, ArmorMaterial.GOLD.getEnchantability()*3/2);

	static final Random rand = new Random();

	private boolean isOfflineMode = false;

	public static final EnhancedFluid chroma = (EnhancedFluid)new EnhancedFluid("chroma").setColor(0x00aaff).setViscosity(300).setTemperature(288).setDensity(300).setLuminosity(15);
	//public static final EnhancedFluid activechroma = (EnhancedFluid)new EnhancedFluid("activechroma").setColor(0x00aaff).setViscosity(300).setTemperature(500).setDensity(300);
	public static EnhancedFluid crystal = (EnhancedFluid)new EnhancedFluid("potion crystal").setColor(0x66aaff).setLuminosity(15).setTemperature(500).setUnlocalizedName("potioncrystal").setViscosity(1500);
	public static final Fluid ender = new Fluid("ender").setViscosity(2000).setDensity(1500).setTemperature(270).setUnlocalizedName("endere").setLuminosity(4);
	public static final Fluid luma = new Fluid("luma").setViscosity(50).setDensity(1).setTemperature(250);
	public static final Fluid lumen = new Fluid("lumen").setViscosity(500).setDensity(300).setTemperature(300).setLuminosity(15);
	//public static final Fluid lifewater = new Fluid("lifewater").setViscosity(400).setDensity(700).setTemperature(300);

	public static final Block[] blocks = new Block[ChromaBlocks.blockList.length];
	public static final Item[] items = new Item[ChromaItems.itemList.length];
	public static final Enchantment[] enchants = new Enchantment[ChromaEnchants.enchantmentList.length];

	public static final Material crystalMat = new CrystalMaterial();
	public static final Material airMat = PseudoAirMaterial.instance;

	private static final HashMap<String, ChromaDecorator> decorators = new HashMap();

	public static PotionGrowthHormone growth;
	public static PotionBetterSaturation betterSat;
	public static PotionCustomRegen betterRegen;
	public static PotionLumarhea lumarhea;
	public static PotionVoidGaze voidGaze;
	public static PotionLumenRegen lumenRegen;

	public static final PylonDamage[] pylonDamage = new PylonDamage[17];

	public static EnumCreatureType glowCloudType = EnumHelper.addCreatureType("glowcloud", EntityGlowCloud.class, 24, Material.air, true, false);

	@Instance("ChromatiCraft")
	public static ChromatiCraft instance = new ChromatiCraft();

	public static final ChromaConfig config = new ChromaConfig(instance, ChromaOptions.optionList, ExtraChromaIDs.idList);

	public static ModLogger logger;

	public static BiomeRainbowForest rainbowforest;
	public static RainbowRiver rainbowRiver;
	public static BiomeEnderForest enderforest;
	public static BiomeGlowingCliffs glowingcliffs;
	public static GlowingCliffsEdge glowingcliffsEdge;

	@SidedProxy(clientSide="Reika.ChromatiCraft.ChromaClient", serverSide="Reika.ChromatiCraft.ChromaCommon")
	public static ChromaCommon proxy;

	private boolean dimensionLoadable = true;

	public boolean isDimensionLoadable() {
		return dimensionLoadable && !this.isLocked();
	}

	public final boolean isLocked() {
		return !ModLockController.instance.verify(this);
	}

	private final boolean checkForLock() {
		for (int i = 0; i < ChromaItems.itemList.length; i++) {
			ChromaItems r = ChromaItems.itemList[i];
			if (!r.isDummiedOut()) {
				Item id = r.getItemInstance();
				if (BannedItemReader.instance.containsID(id))
					return true;
			}
		}
		for (int i = 0; i < ChromaBlocks.blockList.length; i++) {
			ChromaBlocks r = ChromaBlocks.blockList[i];
			if (!r.isDummiedOut()) {
				Block id = r.getBlockInstance();
				if (BannedItemReader.instance.containsID(id))
					return true;
			}
		}
		return false;
	}

	public boolean isOfflineMode() {
		return isOfflineMode;
	}

	@Override
	@EventHandler
	public void preload(FMLPreInitializationEvent evt) {
		this.startTiming(LoadPhase.PRELOAD);
		this.verifyInstallation();

		config.loadSubfolderedConfigFile(evt);
		config.initProps(evt);
		ModLockController.instance.registerMod(this);

		logger = new ModLogger(instance, false);
		if (DragonOptions.FILELOG.getState())
			logger.setOutput("**_Loading_Log.log");

		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			MusicLoader.instance.registerAssets();
		}

		proxy.initAssetLoaders();
		proxy.registerSounds();

		for (int i = 0; i < CrystalElement.elements.length; i++) {
			pylonDamage[i] = new PylonDamage(CrystalElement.elements[i]);
		}
		pylonDamage[16] = new PylonDamage(null);

		MinecraftForge.EVENT_BUS.register(GuardianStoneManager.instance);
		MinecraftForge.EVENT_BUS.register(ChromaticEventManager.instance);
		MinecraftForge.EVENT_BUS.register(ChromaDimensionTicker.instance);
		FMLCommonHandler.instance().bus().register(ChromaticEventManager.instance);
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			MinecraftForge.EVENT_BUS.register(ChromaClientEventController.instance);
			FMLCommonHandler.instance().bus().register(ChromaClientEventController.instance);
			MinecraftForge.EVENT_BUS.register(ChromaHelpHUD.instance);
			MinecraftForge.EVENT_BUS.register(PylonFinderOverlay.instance);
		}

		this.setupClassFiles();

		int id = ExtraChromaIDs.GROWTHID.getValue();
		IDCollisionTracker.instance.addPotionID(instance, id, PotionGrowthHormone.class);
		growth = new PotionGrowthHormone(id);

		id = ExtraChromaIDs.SATID.getValue();
		IDCollisionTracker.instance.addPotionID(instance, id, PotionBetterSaturation.class);
		betterSat = new PotionBetterSaturation(id);

		id = ExtraChromaIDs.REGENID.getValue();
		IDCollisionTracker.instance.addPotionID(instance, id, PotionCustomRegen.class);
		betterRegen = new PotionCustomRegen(id);

		id = ExtraChromaIDs.LUMARHEAID.getValue();
		IDCollisionTracker.instance.addPotionID(instance, id, PotionLumarhea.class);
		lumarhea = new PotionLumarhea(id);

		id = ExtraChromaIDs.VOIDGAZEID.getValue();
		IDCollisionTracker.instance.addPotionID(instance, id, PotionVoidGaze.class);
		voidGaze = new PotionVoidGaze(id);

		id = ExtraChromaIDs.LUMENREGENID.getValue();
		IDCollisionTracker.instance.addPotionID(instance, id, PotionLumenRegen.class);
		lumenRegen = new PotionLumenRegen(id);

		lumen.setBlock(ChromaBlocks.MOLTENLUMEN.getBlockInstance());
		ender.setBlock(ChromaBlocks.ENDER.getBlockInstance());
		luma.setBlock(ChromaBlocks.LUMA.getBlockInstance());
		//lifewater.setBlock(ChromaBlocks.LIFEWATER.getBlockInstance());

		IDCollisionTracker.instance.addBiomeID(instance, ExtraChromaIDs.RAINBOWFOREST.getValue(), BiomeRainbowForest.class);
		IDCollisionTracker.instance.addBiomeID(instance, ExtraChromaIDs.ENDERFOREST.getValue(), BiomeEnderForest.class);
		IDCollisionTracker.instance.addBiomeID(instance, ExtraChromaIDs.LUMINOUSCLIFFS.getValue(), BiomeGlowingCliffs.class);

		//ChromaResearch.loadCache();

		ReikaPacketHelper.registerPacketHandler(instance, packetChannel, new ChromatiPackets());

		proxy.registerKeys();

		tabChroma.setIcon(ChromaItems.RIFT.getStackOf());
		tabChromaDeco.setIcon(ChromaBlocks.COLORALTAR.getStackOfMetadata(CrystalElement.BLUE.ordinal()));
		tabChromaGen.setIcon(ChromaBlocks.RAINBOWSAPLING.getStackOf());
		tabChromaTools.setIcon(ChromaItems.TOOL.getStackOf());
		tabChromaItems.setIcon(ChromaStacks.getShard(CrystalElement.RED));
		CreativeTabSorter.instance.registerCreativeTabAfter(tabChromaDeco, tabChroma);
		CreativeTabSorter.instance.registerCreativeTabAfter(tabChromaGen, tabChroma);
		CreativeTabSorter.instance.registerCreativeTabAfter(tabChromaItems, tabChroma);
		CreativeTabSorter.instance.registerCreativeTabAfter(tabChromaTools, tabChroma);
		CreativeTabSorter.instance.registerCreativeTabAfter(tabChromaFragments, tabChroma);

		if (!this.isLocked()) {
			//if (ConfigRegistry.ACHIEVEMENTS.getState()) {
			//		achievements = new Achievement[RotaryAchievements.list.length];
			//		RotaryAchievements.registerAchievements();
			//	}
		}

		//CompatibilityTracker.instance.registerIncompatibility(ModList.CHROMATICRAFT, ModList.OPTIFINE, CompatibilityTracker.Severity.GLITCH, "Optifine is known to break some rendering and cause framerate drops.");

		if (ModList.FORESTRY.isLoaded()) {
			ModInteraction.addCrystalBackpack();
		}

		FMLInterModComms.sendMessage("zzzzzcustomconfigs", "blacklist-mod-as-output", this.getModContainer().getModId());

		ConfigMatcher.instance.addConfigList(this, ChromaOptions.optionList);
		ConfigMatcher.instance.addConfigList(this, ExtraChromaIDs.idList);

		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.TANK.getBlockInstance());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.GLASS.getBlockInstance());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.SELECTIVEGLASS.getBlockInstance());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.DOOR.getBlockInstance());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.PYLON.getBlockInstance());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.STRUCTSHIELD.getBlockInstance(), BlockType.GLASS.ordinal());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.STRUCTSHIELD.getBlockInstance(), BlockType.GLASS.metadata);
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.SPECIALSHIELD.getBlockInstance(), BlockType.GLASS.ordinal());
		RayTracer.addVisuallyTransparentBlock(ChromaBlocks.SPECIALSHIELD.getBlockInstance(), BlockType.GLASS.metadata);

		this.basicSetup(evt);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void load(FMLInitializationEvent event) {
		this.startTiming(LoadPhase.LOAD);

		if (this.checkForLock()) {
			ModLockController.instance.unverify(this);
		}
		if (this.isLocked()) {
			ReikaJavaLibrary.pConsole("");
			ReikaJavaLibrary.pConsole("\t========================================= ChromatiCraft ===============================================");
			ReikaJavaLibrary.pConsole("\tNOTICE: It has been detected that third-party plugins are being used to disable parts of ChromatiCraft.");
			ReikaJavaLibrary.pConsole("\tBecause this is frequently done to sell access to mod content, which is against the Terms of Use");
			ReikaJavaLibrary.pConsole("\tof both Mojang and the mod, the mod has been functionally disabled. No damage will occur to worlds,");
			ReikaJavaLibrary.pConsole("\tand all machines (including contents) and items already placed or in inventories will remain so,");
			ReikaJavaLibrary.pConsole("\tbut its machines will not function, recipes will not load, and no renders or textures will be present.");
			ReikaJavaLibrary.pConsole("\tAll other mods in your installation will remain fully functional.");
			ReikaJavaLibrary.pConsole("\tTo regain functionality, unban the ChromatiCraft content, and then reload the game. All functionality");
			ReikaJavaLibrary.pConsole("\twill be restored. You may contact Reika for further information on his forum thread.");
			ReikaJavaLibrary.pConsole("\t=====================================================================================================");
			ReikaJavaLibrary.pConsole("");
		}

		CCAPICore.load();

		ChromaRecipes.loadDictionary();
		if (this.isLocked())
			PlayerHandler.instance.registerTracker(ChromaLock.instance);
		if (!this.isLocked()) {
			proxy.addArmorRenders();
			proxy.registerRenderers();
		}

		ItemStackRepository.instance.registerClass(this, ChromaStacks.class);

		TransmutationRecipes.instance.getClass();

		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			MinecraftForge.EVENT_BUS.register(ChromaOverlays.instance);
			MinecraftForge.EVENT_BUS.register(OreOverlayRenderer.instance);
			MinecraftForge.EVENT_BUS.register(StructureErrorOverlays.instance);
			FMLCommonHandler.instance().bus().register(OreOverlayRenderer.instance);
			FMLCommonHandler.instance().bus().register(MobSonarRenderer.instance);
			FMLCommonHandler.instance().bus().register(StructureErrorOverlays.instance);
			MinecraftForge.EVENT_BUS.register(MobSonarRenderer.instance);
			CCParticleEngine.instance.register();
		}

		rainbowforest = new BiomeRainbowForest(ExtraChromaIDs.RAINBOWFOREST.getValue());
		BiomeManager.addBiome(BiomeType.WARM, new BiomeEntry(rainbowforest, ChromaOptions.getRainbowForestWeight()));
		BiomeManager.addBiome(BiomeType.COOL, new BiomeEntry(rainbowforest, ChromaOptions.getRainbowForestWeight()));
		BiomeManager.addSpawnBiome(rainbowforest);
		BiomeManager.addStrongholdBiome(rainbowforest);
		BiomeManager.addVillageBiome(rainbowforest, true);
		BiomeDictionary.registerBiomeType(rainbowforest, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.HILLS);

		rainbowRiver = new RainbowRiver(ExtraChromaIDs.RAINBOWRIVER.getValue());
		BiomeDictionary.registerBiomeType(rainbowRiver, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.RIVER);

		enderforest = new BiomeEnderForest(ExtraChromaIDs.ENDERFOREST.getValue());
		BiomeManager.addBiome(BiomeType.COOL, new BiomeEntry(enderforest, ChromaOptions.getEnderForestWeight()));
		BiomeManager.addBiome(BiomeType.WARM, new BiomeEntry(enderforest, ChromaOptions.getEnderForestWeight()));
		BiomeManager.addSpawnBiome(enderforest);
		BiomeManager.addStrongholdBiome(enderforest);
		BiomeManager.addVillageBiome(enderforest, true);
		BiomeDictionary.registerBiomeType(enderforest, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.MAGICAL);

		glowingcliffs = new BiomeGlowingCliffs(ExtraChromaIDs.LUMINOUSCLIFFS.getValue(), true);
		//BiomeManager.addBiome(BiomeType.COOL, new BiomeEntry(glowingcliffs, ChromaOptions.getGlowingCliffsWeight()));
		//BiomeManager.addBiome(BiomeType.WARM, new BiomeEntry(glowingcliffs, ChromaOptions.getGlowingCliffsWeight()));
		//BiomeManager.addSpawnBiome(glowingcliffs);
		BiomeManager.addStrongholdBiome(glowingcliffs);

		glowingcliffsEdge = new GlowingCliffsEdge(ExtraChromaIDs.LUMINOUSEDGE.getValue());

		ReikaBiomeHelper.addChildBiome(glowingcliffs, glowingcliffsEdge);

		//replace 1/8 of jungle and 1/8 of Mega Taiga, for a total of 4/(16-2) = 28% net spawn rate of either Jungle or MT
		//revised to 1/16th of each for 2/15 = 13% spawn rate
		SpecialBiomePlacementRegistry.instance.registerID(this, Category.WARM, 2, glowingcliffs.biomeID);
		//SpecialBiomePlacementRegistry.instance.registerID(this, Category.WARM, 3, glowingcliffs.biomeID);
		SpecialBiomePlacementRegistry.instance.registerID(this, Category.COOL, 2, glowingcliffs.biomeID);
		//SpecialBiomePlacementRegistry.instance.registerID(this, Category.COOL, 3, glowingcliffs.biomeID);
		BiomeDictionary.registerBiomeType(glowingcliffs, BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.LUSH, BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.BEACH, BiomeDictionary.Type.WET);
		BiomeDictionary.registerBiomeType(glowingcliffsEdge, BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.LUSH, BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.BEACH, BiomeDictionary.Type.WET);

		if (ModList.CLIMATECONTROL.isLoaded()) {
			ReikaClimateControl.registerBiome(rainbowforest, 4, true, "MEDIUM");
			ReikaClimateControl.registerBiome(enderforest, 5, true, "MEDIUM");
			ReikaClimateControl.registerBiome(glowingcliffs, 1, false, "MEDIUM");
		}

		ChromaDimensionManager.initialize();

		ChromaStructures.buildStructures();

		RetroGenController.instance.addHybridGenerator(PylonGenerator.instance, Integer.MIN_VALUE);
		RetroGenController.instance.addHybridGenerator(DungeonGenerator.instance, Integer.MAX_VALUE);
		RetroGenController.instance.addHybridGenerator(DataTowerGenerator.instance, Integer.MAX_VALUE);
		RetroGenController.instance.addHybridGenerator(DataTowerGenerator.instance, Integer.MIN_VALUE);
		RetroGenController.instance.addHybridGenerator(NetherStructureGenerator.instance, Integer.MAX_VALUE);
		RetroGenController.instance.addHybridGenerator(EndOverhaulManager.instance, Integer.MIN_VALUE);
		RetroGenController.instance.addHybridGenerator(GlowingCliffsAuxGenerator.instance, Integer.MIN_VALUE);
		RetroGenController.instance.addHybridGenerator(SkypeaterGenerator.instance, Integer.MAX_VALUE);
		//RetroGenController.instance.addHybridGenerator(UnknownArtefactGenerator.instance, Integer.MIN_VALUE);
		RetroGenController.instance.addHybridGenerator(WarpNodeGenerator.instance, Integer.MAX_VALUE);

		this.addRerunnableDecorator(CrystalGenerator.instance, 0);
		this.addRerunnableDecorator(ColorTreeGenerator.instance, -10);
		this.addRerunnableDecorator(TieredWorldGenerator.instance, Integer.MIN_VALUE);
		this.addRerunnableDecorator(DecoFlowerGenerator.instance, Integer.MIN_VALUE);
		this.addRerunnableDecorator(LumaGenerator.instance, Integer.MAX_VALUE);
		this.addRerunnableDecorator(CaveIndicatorGenerator.instance, Integer.MAX_VALUE);

		//ReikaEntityHelper.overrideEntity(EntityChromaEnderCrystal.class, "EnderCrystal", 0);

		VillagersFailChromatiCraft.register();
		VillageTradeHandler.instance.addHandler(CCTradeHandler.instance);

		ChromaChests.addToChests();

		if (!this.isLocked())
			;//RotaryNames.addNames();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new ChromaGuiHandler());
		this.addTileEntities();
		this.addEntities();

		if (!this.isLocked()) {
			ChromaRecipes.addRecipes();
		}

		//if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
		ChromaDescriptions.loadData();
		//}

		PackModificationTracker.instance.addMod(this, config);

		if (!this.isLocked())
			IntegrityChecker.instance.addMod(instance, ChromaBlocks.blockList, ChromaItems.itemList);

		if (!this.isLocked()) {
			TickRegistry.instance.registerTickHandler(ChromabilityHandler.instance);
			TickRegistry.instance.registerTickHandler(CrystalNetworker.instance);
			TickRegistry.instance.registerTickHandler(ExplorationMonitor.instance);
			TickRegistry.instance.registerTickHandler(ChromaDimensionTicker.instance);
			//TickRegistry.instance.registerTickHandler(LightingRerenderer.instance);
			//TickRegistry.instance.registerTickHandler(ChunkResetter.instance);
			TickRegistry.instance.registerTickHandler(ArtefactSpawner.instance);
			//TickRegistry.instance.registerTickHandler(TunnelNukerSpawner.instance);
			if (ModList.THAUMCRAFT.isLoaded())
				TickRegistry.instance.registerTickHandler(NodeRecharger.instance);
			MinecraftForge.EVENT_BUS.register(AbilityHelper.instance);
			FMLCommonHandler.instance().bus().register(AbilityHelper.instance);
			AbilityHelper.instance.register();
			PlayerHandler.instance.registerTracker(PylonCacheLoader.instance);
			PlayerHandler.instance.registerTracker(DimensionJoinHandler.instance);
			PlayerHandler.instance.registerTracker(ProgressionLoadHandler.instance);
			if (ModList.VOIDMONSTER.isLoaded() && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
				TickRegistry.instance.registerTickHandler(VoidMonsterRitualClientEffects.instance);
		}

		if (ChromaOptions.HANDBOOK.getState())
			PlayerFirstTimeTracker.addTracker(new ChromaBookSpawner());

		for (int i = 0; i < 16; i++) {
			ReikaDyeHelper dye = ReikaDyeHelper.dyes[i];
			ItemStack used = ChromaOptions.isVanillaDyeMoreCommon(i) ? dye.getStackOf() : ChromaItems.DYE.getStackOfMetadata(i);
			ItemStack sapling = new ItemStack(ChromaBlocks.DYESAPLING.getBlockInstance(), 1, i);
			ItemStack flower = new ItemStack(ChromaBlocks.DYEFLOWER.getBlockInstance(), 1, i);
			ItemStack leaf = new ItemStack(ChromaBlocks.DYELEAF.getBlockInstance(), 1, i);
			if (!ReikaItemHelper.matchStacks(used, ReikaItemHelper.lapisDye))
				GameRegistry.addRecipe(new ItemStack(ChromaBlocks.DYE.getBlockInstance(), 1, i), "ddd", "ddd", "ddd", 'd', used);
			GameRegistry.addShapelessRecipe(used, flower);
			OreDictionary.registerOre(dye.getOreDictName(), ChromaItems.DYE.getStackOfMetadata(i));
			OreDictionary.registerOre("treeSapling", sapling);
			OreDictionary.registerOre("treeLeaves", leaf);
			OreDictionary.registerOre("plant"+dye.colorNameNoSpaces, flower);
			OreDictionary.registerOre("flower"+dye.colorNameNoSpaces, flower);
			OreDictionary.registerOre("flower", flower);
			FurnaceFuelRegistry.instance.registerItemSimple(sapling, 0.5F);
		}

		FurnaceFuelRegistry.instance.registerItemSimple(ChromaStacks.firaxite, 24);
		FurnaceFuelRegistry.instance.registerItemSimple(ChromaStacks.thermiticCrystal, 32);

		this.addDyeCompat();

		VanillaIntegrityTracker.instance.addWatchedBlock(instance, Blocks.leaves);

		if (ModList.ATG.isLoaded()) {
			ATGBiomes.addBiome(ATGBiomes.BiomeType.LAND, "Forest", rainbowforest, 1.0);
			ATGBiomes.addBiome(ATGBiomes.BiomeType.LAND, "Forest", enderforest, 1.0);
			ATGBiomes.addBiome(ATGBiomes.BiomeType.LAND, "Cliffs", glowingcliffs, 0.125);
		}

		if (ModList.BLUEPOWER.isLoaded()) { //prevent what is nearly an exploit by uncrafting gold apples
			ModInteraction.blacklistGoldAppleUncrafting();
		}

		LuaMethod.registerMethods("Reika.ChromatiCraft.ModInterface.Lua");

		//ReikaEEHelper.blacklistRegistry(ChromaBlocks.blockList);
		//ReikaEEHelper.blacklistRegistry(ChromaItems.itemList);

		SuggestedModsTracker.instance.addSuggestedMod(instance, ModList.FORESTRY, "Access to crystal bees which have valuable genetics");
		SuggestedModsTracker.instance.addSuggestedMod(instance, ModList.TWILIGHT, "Dense crystal generation and other worldgen hooks");
		SuggestedModsTracker.instance.addSuggestedMod(instance, ModList.THAUMCRAFT, "High crystal aspect values and extensive mod interaction");

		FMLInterModComms.sendMessage("aura", "lootblacklist", ChromaItems.FRAGMENT.getStackOf());
		FMLInterModComms.sendMessage("aura", "lootblacklist", ChromaItems.SHARD.getStackOf());

		FMLInterModComms.sendMessage(ModList.ARSMAGICA.modLabel, "dsb", "EntityDryad|"+ExtraChromaIDs.DIMID.getValue());
		FMLInterModComms.sendMessage(ModList.ARSMAGICA.modLabel, "dwg", String.valueOf(ExtraChromaIDs.DIMID.getValue()));

		FMLInterModComms.sendMessage("Randomod", "blacklist", this.getModContainer().getModId());

		ModInteraction.addMicroblocks();

		DonatorController.instance.registerMod(this, DonatorController.reikaURL);

		if (ModList.MYSTCRAFT.isLoaded()) {
			ModInteraction.addMystCraft();
		}

		if (ModList.RFTOOLS.isLoaded()) {
			ModInteraction.modifyRFToolsPages();
		}

		if (ModList.THAUMCRAFT.isLoaded()) {
			ChromaAspectManager.instance.PUZZLE.getName(); //init to register the two aspects
		}

		for (int i = 0; i < ChromaItems.itemList.length; i++) {
			ChromaItems ir = ChromaItems.itemList[i];
			if (!ir.isDummiedOut() && ir != ChromaItems.TOOL) {
				SensitiveItemRegistry.instance.registerItem(this, ir.getItemInstance(), false);
			}
		}

		for (int i = 0; i < ChromaBlocks.blockList.length; i++) {
			ChromaBlocks ir = ChromaBlocks.blockList[i];
			SensitiveItemRegistry.instance.registerItem(this, ir.getBlockInstance(), false);
		}

		if (MTInteractionManager.isMTLoaded()) {
			MTInteractionManager.instance.blacklistRecipeRemovalFor(ChromaTiles.TABLE.getCraftedProduct());

			MTInteractionManager.instance.blacklistOreDictTagsFor(ChromaItems.SHARD.getItemInstance());
			MTInteractionManager.instance.blacklistOreDictTagsFor(ChromaItems.TIERED.getItemInstance());
			MTInteractionManager.instance.blacklistOreDictTagsFor(ChromaBlocks.CRYSTAL.getBlockInstance());
		}

		SensitiveFluidRegistry.instance.registerFluid("chroma");
		//SensitiveFluidRegistry.instance.registerFluid("ender");
		//SensitiveFluidRegistry.instance.registerFluid("potion crystal");
		SensitiveFluidRegistry.instance.registerFluid("luma");
		SensitiveFluidRegistry.instance.registerFluid("lumen");

		ReikaEEHelper.blacklistEntry(ChromaItems.TIERED);
		ReikaEEHelper.blacklistEntry(ChromaItems.SHARD);
		ReikaEEHelper.blacklistEntry(ChromaBlocks.TIEREDORE);
		ReikaEEHelper.blacklistEntry(ChromaBlocks.TIEREDPLANT);
		ReikaEEHelper.blacklistEntry(ChromaBlocks.CRYSTAL);

		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
			ReikaJavaLibrary.initClassWithSubs(ChromaFontRenderer.class, true);

		if (ModList.ENDERIO.isLoaded()) {
			ModInteraction.blacklistTravelStaff();
		}

		if (ModList.APPENG.isLoaded()) {
			MESystemReader.registerMESystemEffect(UABombingEffects.instance.createMESystemEffect());
		}

		if (ModList.AGRICRAFT.isLoaded()) {
			ModInteraction.registerCliffSoil();
		}

		ChunkProviderChroma.regenerateGenerators();
		RosettaStone.init.test();

		ReikaJavaLibrary.initClass(AdjacencyUpgradeAPI.class, true);
		ReikaJavaLibrary.initClass(AdjacencyCheckHandler.class, true);
		ReikaJavaLibrary.initClass(CustomAdjacencyHandler.class, true);

		this.finishTiming();
	}

	private static void addRerunnableDecorator(ChromaDecorator d, int wt) {
		RetroGenController.instance.addHybridGenerator(d, wt);
		decorators.put(d.getCommandID().toLowerCase(Locale.ENGLISH), d);
	}

	private void addEntities() {
		ReikaRegistryHelper.registerModEntities(instance, ChromaEntities.entityList);
		//ReikaEntityHelper.removeEntityFromRegistry(EntityEnderCrystal.class);
		//EntityList.addMapping(EntityChromaEnderCrystal.class, "EnderCrystal", 200);
	}

	private void addDyeCompat() {
		if (ModList.TREECAPITATOR.isLoaded()) {
			TreeCapitatorHandler.register();
		}

		if (ModList.ROTARYCRAFT.isLoaded()) {
			ModInteraction.addRCGPRColors();
		}

		for (int i = 0; i < ReikaDyeHelper.dyes.length; i++) {
			ItemStack is = new ItemStack(ChromaBlocks.DYE.getBlockInstance(), 1, i);
			FMLInterModComms.sendMessage("ForgeMicroblock", "microMaterial", is);
		}
	}

	@Override
	@EventHandler
	public void postload(FMLPostInitializationEvent evt) {
		this.startTiming(LoadPhase.POSTLOAD);

		if (!this.isLocked()) {
			ChromaRecipes.addPostLoadRecipes();
		}

		proxy.addDonatorRender();

		ModCropList.addCustomCropType(new CrystalPlantHandler());

		ChromaTiles.loadPostMappings();
		TileEntityBiomePainter.buildBiomeList();
		TileEntityOreCreator.initOreMap();
		TileEntityTeleportationPump.buildProgressionMap();
		for (int i = 0; i < 16; i++) {
			AdjacencyUpgrades a = AdjacencyUpgrades.upgrades[i];
			if (a.isImplemented()) {
				try {
					Method m = a.getTileClass().getDeclaredMethod("initHandlers");
					m.setAccessible(true);
					m.invoke(null);
				}
				catch (NoSuchMethodException ex) {

				}
				catch (Exception e) {
					throw new RegistrationException(instance, "Could not initialize adjacency handlers for "+a, e);
				}
			}
		}
		TileEntityMiner.loadCustomMappings();
		ItemDuplicationWand.loadMappings();
		BurrowStructure.buildLootCache();
		TileEntityWirelessPowered.loadAdjacencyHandler();
		EntityMeteorShot.registerAdjacency();
		ItemAuraPouch.setDefaultSpecialEffects();
		TileEntityFunctionRelay.setDefaultEffects();

		ReikaDispenserHelper.addDispenserAction(ChromaItems.TOOL, new ManipulatorDispenserAction());
		ReikaDispenserHelper.addDispenserAction(ChromaItems.BUCKET, new BucketDispenserAction());
		ProjectileToolDispenserAction proj = new ProjectileToolDispenserAction();
		ReikaDispenserHelper.addDispenserAction(ChromaItems.SPLASHGUN, proj);
		ReikaDispenserHelper.addDispenserAction(ChromaItems.VACUUMGUN, proj);
		ReikaDispenserHelper.addDispenserAction(ChromaItems.CHAINGUN, proj);
		ReikaDispenserHelper.addDispenserAction(ChromaItems.LIGHTGUN, proj);

		MinecraftForge.EVENT_BUS.register(CheatingPreventionSystem.instance);
		FMLCommonHandler.instance().bus().register(CheatingPreventionSystem.instance);

		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i] instanceof LoadRegistry) {
				((LoadRegistry)blocks[i]).onLoad();
			}
		}

		//CliffFogRenderer.instance.initialize();
		GlowingCliffsAuxGenerator.instance.initialize();

		UATrades.instance.loadData();

		WorldGenInterceptionRegistry.instance.addWatcher(ChromaAux.populationWatcher);
		WorldGenInterceptionRegistry.instance.addIWGWatcher(ChromaAux.slimeIslandBlocker);
		WorldGenInterceptionRegistry.instance.addException(ChromaAux.dimensionException);

		IconLookupRegistry.instance.registerIcons(instance, ChromaIcons.class);
		IconLookupRegistry.instance.registerIcons(instance, CrystalElement.class);

		ItemMagicRegistry.instance.addPostload();

		if (ModList.THAUMCRAFT.isLoaded()) {
			ModInteraction.addThaumCraft();

			TileEntityAspectFormer.initCapacity();
		}

		if (ModList.BLOODMAGIC.isLoaded() && BloodMagicHandler.getInstance().soulFrayID != -1) {
			CrystalPotionController.instance.addBadPotionForIgnore(Potion.potionTypes[BloodMagicHandler.getInstance().soulFrayID]);
		}

		if (Loader.isModLoaded("TardisMod")) {
			ModInteraction.blacklistTardisFromDimension();
		}

		if (Loader.isModLoaded("dsurround")) {
			ModInteraction.setDynSurroundSettings();
		}

		if (ModList.MEKANISM.isLoaded()) {
			ModInteraction.blacklistMekBoxes();
		}

		if (ModList.FORESTRY.isLoaded()) {
			ModInteraction.addForestry();
		}

		if (ModList.IC2.isLoaded()) {
			IC2ReactorAcceleration.instance.register();
		}
		//MultiblockAcceleration.instance.register();

		if (ModList.TWILIGHT.isLoaded()) {
			ModInteraction.addTFLoot();
		}

		if (Loader.isModLoaded("chocolateQuest")) {
			ModInteraction.addChocoDungeonLoot();
		}

		if (ModList.BOTANIA.isLoaded()) {
			ModInteraction.blacklistLoonium();
		}

		if (ModList.ROTARYCRAFT.isLoaded()) {
			ReservoirAPI.registerHandler(new ChromaPrepHandler());
			ReservoirAPI.registerHandler(new ShardBoostingHandler());
			ReservoirAPI.registerHandler(new PoolRecipeHandler());
		}

		if (ModList.METEORCRAFT.isLoaded()) {
			MeteorSpawnAPI.blacklistDimension(ExtraChromaIDs.DIMID.getValue());
		}

		if (ModList.VOIDMONSTER.isLoaded()) {
			DimensionAPI.blacklistDimensionForSounds(ExtraChromaIDs.DIMID.getValue());
			DimensionAPI.blacklistBiomeForSounds(ExtraChromaIDs.RAINBOWFOREST.getValue());
			DimensionAPI.blacklistBiomeForSounds(ExtraChromaIDs.LUMINOUSCLIFFS.getValue());
			DimensionAPI.blacklistBiomeForSounds(ExtraChromaIDs.LUMINOUSEDGE.getValue());
			DimensionAPI.setDimensionRuleForSpawning(ExtraChromaIDs.DIMID.getValue(), false);
			MonsterAPI.addDrop(ChromaStacks.voidmonsterEssence, 24, 64);
		}

		if (ModList.MINEFACTORY.isLoaded()) {
			ModInteraction.blacklistMFRSafariNet();
		}

		if (ModList.CARPENTER.isLoaded()) {
			ModInteraction.addCarpenterCovers();
		}

		if (ModList.RFTOOLS.isLoaded()) {
			ModInteraction.blacklistRFToolsTeleport();
		}

		if (ModList.TINKERER.isLoaded() && (Pulses.TOOLS.isLoaded() || Pulses.WEAPONS.isLoaded())) {
			ModInteraction.addChromastoneTools();
		}

		for (int i = 0; i < ChromaTiles.TEList.length; i++) {
			ChromaTiles m = ChromaTiles.TEList[i];
			TimeTorchHelper.blacklistTileEntity(m.getTEClass());
		}

		ChromaResearch.loadPostCache();

		this.finishTiming();
	}

	public static boolean isRainbowForest(BiomeGenBase b) {
		return b instanceof BiomeRainbowForest || (ModList.MYSTCRAFT.isLoaded() && ReikaMystcraftHelper.getMystParentBiome(b) instanceof BiomeRainbowForest);
	}

	public static boolean isEnderForest(BiomeGenBase b) {
		return b instanceof BiomeEnderForest || (ModList.MYSTCRAFT.isLoaded() && ReikaMystcraftHelper.getMystParentBiome(b) instanceof BiomeEnderForest);
	}

	public static boolean isCCBiome(BiomeGenBase b) {
		return isRainbowForest(b) || isEnderForest(b) || BiomeGlowingCliffs.isGlowingCliffs(b);
	}

	@EventHandler
	public void preServer(FMLServerAboutToStartEvent evt) {
		DungeonGenerator.instance.updateStatusCacheFile();
	}

	@EventHandler
	public void registerCommands(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new GuardianCommand());
		evt.registerServerCommand(new ProgressModifyCommand());
		evt.registerServerCommand(new NetworkLoggerCommand());
		evt.registerServerCommand(new StructureGenCommand());
		evt.registerServerCommand(new DimensionGeneratorCommand());
		evt.registerServerCommand(new RecipeReloadCommand());
		evt.registerServerCommand(new PylonCacheCommand());
		evt.registerServerCommand(new CrystalNetCommand());
		evt.registerServerCommand(new ReshufflePylonCommand());
		evt.registerServerCommand(new RedecorateCommand());
		evt.registerServerCommand(new PlaceStructureCommand());
		evt.registerServerCommand(new NodeWrapperInspectionCommand());
		evt.registerServerCommand(new StructureMapCommand());

		if (MinecraftServer.getServer() != null && !MinecraftServer.getServer().isServerInOnlineMode()) {
			isOfflineMode = true;
			PopupWriter.instance.addMessage("ChromatiCraft does not properly work in offline mode! Ownership data is not properly read/saved and some things will not work!");
		}
		else {
			isOfflineMode = false;
		}

		ProgressionLoadHandler.instance.initLevelData(evt.getServer());
		ProgressionLoadHandler.instance.load();
		OreOverlayRenderer.instance.loadOres();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onGameLoaded(GameFinishedLoadingEvent evt) {
		OreOverlayRenderer.instance.loadOres();
	}

	@EventHandler
	public void overrideRecipes(FMLServerStartedEvent evt) {
		if (!this.isLocked()) {
			RecipesCastingTable.instance.reload();
		}
	}

	@EventHandler
	public void serverShutdown(FMLServerStoppingEvent evt) {
		ProgressionLoadHandler.instance.saveAll();
		//if (MinecraftServer.getServer().isDedicatedServer())
		ChromaDimensionManager.serverStopping = true;
	}

	@EventHandler
	public void singlePlayerLogout(FMLServerStoppedEvent evt) {
		ChromaticEventManager.instance.clearCaches();
	}

	private void setupClassFiles() {
		setupLiquids();

		ReikaRegistryHelper.instantiateAndRegisterBlocks(instance, ChromaBlocks.blockList, blocks);
		ReikaRegistryHelper.instantiateAndRegisterItems(instance, ChromaItems.itemList, items);
		ReikaRegistryHelper.instantiateAndRegisterEnchantments(instance, ChromaEnchants.enchantmentList, enchants);

		ChromaTiles.loadMappings();
		ChromaBlocks.loadMappings();
		ChromaItems.loadMappings();

		setupLiquidContainers();

		//Block b = Blocks.mob_spawner;
		//Items.itemsList[b.blockID] = new ItemNBTSpawner(b.blockID).setUnlocalizedName(Items.itemsList[b.blockID].getUnlocalizedName());
	}

	private static void setupLiquids() {
		logger.log("Loading And Registering Liquids");
		FluidRegistry.registerFluid(chroma);
		//FluidRegistry.registerFluid(activechroma);
		FluidRegistry.registerFluid(crystal);
		FluidRegistry.registerFluid(ender);
		FluidRegistry.registerFluid(luma);
		FluidRegistry.registerFluid(lumen);
		//FluidRegistry.registerFluid(lifewater);
	}

	private static void setupLiquidContainers() {
		logger.log("Loading And Registering Liquid Containers");
		FluidContainerRegistry.registerFluidContainer(new FluidStack(chroma, FluidContainerRegistry.BUCKET_VOLUME), ChromaItems.BUCKET.getStackOf(), new ItemStack(Items.bucket));
		FluidContainerRegistry.registerFluidContainer(new FluidStack(luma, FluidContainerRegistry.BUCKET_VOLUME), ChromaItems.BUCKET.getStackOfMetadata(3), new ItemStack(Items.bucket));
		if (!ModList.THERMALFOUNDATION.isLoaded())
			FluidContainerRegistry.registerFluidContainer(new FluidStack(ender, FluidContainerRegistry.BUCKET_VOLUME), ChromaItems.BUCKET.getStackOfMetadata(1), new ItemStack(Items.bucket));
		FluidContainerRegistry.registerFluidContainer(new FluidStack(crystal, FluidContainerRegistry.BUCKET_VOLUME), ChromaItems.BUCKET.getStackOfMetadata(2), new ItemStack(Items.bucket));
		FluidContainerRegistry.registerFluidContainer(new FluidStack(lumen, FluidContainerRegistry.BUCKET_VOLUME), ChromaItems.BUCKET.getStackOfMetadata(4), new ItemStack(Items.bucket));
		//FluidContainerRegistry.registerFluidContainer(new FluidStack(lifewater, FluidContainerRegistry.BUCKET_VOLUME), ChromaItems.BUCKET.getStackOfMetadata(5), new ItemStack(Items.bucket));
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void setupExtraIcons(TextureStitchEvent.Pre event) {
		if (!this.isLocked()) {
			logger.log("Loading Additional Icons");

			if (event.map.getTextureType() == 0) {
				IIcon sicon = event.map.registerIcon("ChromatiCraft:fluid/chroma");
				IIcon ficon = event.map.registerIcon("ChromatiCraft:fluid/chroma_flowing");
				chroma.setIcons(sicon, ficon);

				IIcon cry = event.map.registerIcon("ChromatiCraft:fluid/liqcrystal3");
				crystal.setIcons(cry);

				IIcon lum = event.map.registerIcon("ChromatiCraft:fluid/lumen");
				lumen.setIcons(lum);

				IIconRegister ico = event.map;
				ender.setStillIcon(ico.registerIcon("ChromatiCraft:fluid/ender"));
				ender.setFlowingIcon(ico.registerIcon("ChromatiCraft:fluid/flowingender"));

				luma.setIcons(ico.registerIcon("ChromatiCraft:fluid/aether/aether_full"), ico.registerIcon("ChromatiCraft:fluid/aether/aether_flow2"));
				//aether.setFlowingIcon(ico.registerIcon("ChromatiCraft:fluid/flowingender"));

				//lifewater.setIcons(ico.registerIcon("ChromatiCraft:fluid/lifewater_still_trans"), ico.registerIcon("ChromatiCraft:fluid/lifewater_flow_trans"));

				for (int i = 0; i < CrystalElement.elements.length; i++) {
					CrystalElement.elements[i].setIcons(event.map);
				}

				ChromaClientEventController.instance.loadTreeOverlays(event.map);
			}
			ChromaIcons.registerAll(event.map);
		}
	}

	public static Block getEnderBlockToGenerate() {
		if (ModList.THERMALFOUNDATION.isLoaded() && ThermalHandler.getInstance().enderID != null) {
			return ThermalHandler.getInstance().enderID;
		}
		return ChromaBlocks.ENDER.getBlockInstance();
	}

	private void addTileEntities() {
		for (int i = 0; i < ChromaTiles.TEList.length; i++) {
			ChromaTiles c = ChromaTiles.TEList[i];
			if (c == ChromaTiles.ADJACENCY) {
				for (int k = 0; k < 16; k++) {
					if (AdjacencyUpgrades.upgrades[k].isImplemented()) {
						String label = "CC"+c.getUnlocalizedName().toLowerCase(Locale.ENGLISH).replaceAll("\\s","")+"_"+k;
						Class cl = AdjacencyUpgrades.upgrades[k].getTileClass();
						GameRegistry.registerTileEntity(cl, label);
						ReikaJavaLibrary.initClass(cl, true);
					}
				}
			}
			else {
				String label = "CC"+c.getUnlocalizedName().toLowerCase(Locale.ENGLISH).replaceAll("\\s","");
				GameRegistry.registerTileEntity(c.getTEClass(), label);
				ReikaJavaLibrary.initClass(c.getTEClass(), true);
			}
		}
		for (int i = 0; i < 16; i++) {
			AdjacencyUpgrades a = AdjacencyUpgrades.upgrades[i];
			if (a.isImplemented())
				ReikaJavaLibrary.initClass(a.getTileClass(), true);
		}
		for (int i = 0; i < ChromaBlocks.blockList.length; i++) {
			ChromaBlocks b = ChromaBlocks.blockList[i];
			Class c = b.getObjectClass();
			Class[] cs = c.getClasses();
			if (cs != null) {
				for (int k = 0; k < cs.length; k++) {
					Class in = cs[k];
					if (TileEntity.class.isAssignableFrom(in) && (in.getModifiers() & Modifier.ABSTRACT) == 0) {
						String s = "CC"+in.getSimpleName();
						GameRegistry.registerTileEntity(in, s);
					}
				}
			}
		}
		GameRegistry.registerTileEntity(TileEntityCrystalPlant.class, "CCCrystalPlant");
	}

	@Override
	public String getDisplayName() {
		return "ChromatiCraft";
	}

	@Override
	public String getModAuthorName() {
		return "Reika";
	}

	@Override
	public URL getDocumentationSite() {
		return DragonAPICore.getReikaForumPage();
	}

	@Override
	public URL getBugSite() {
		return DragonAPICore.getReikaGithubPage();
	}

	@Override
	public String getWiki() {
		return "http://ChromatiCraft.wikia.com/wiki/ChromatiCraft_Wiki";
	}

	@Override
	public ModLogger getModLogger() {
		return logger;
	}

	@Override
	public String getUpdateCheckURL() {
		return CommandableUpdateChecker.reikaURL;
	}

	public static ChromaDecorator getDecorator(String id) {
		return decorators.get(id);
	}

	@Override
	public File getConfigFolder() {
		return config.getConfigFolder();
	}

	@Override
	protected Class<? extends IClassTransformer> getASMClass() {
		return ChromaASMHandler.ASMExecutor.class;
	}
}
