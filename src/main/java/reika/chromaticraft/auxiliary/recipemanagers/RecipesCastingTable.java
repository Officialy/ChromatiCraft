/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBiMap;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.CastingAPI;
import reika.chromaticraft.api.crystalelementaccessor.CrystalElementProxy;
import reika.chromaticraft.auxiliary.ChromaAux;
import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.event.CastingRecipesReloadEvent;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.RecipeType;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.AvoLampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CastingInjectorFocusRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.ChromaFlowerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CompoundRelayRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CompoundRuneRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CrystalAltarRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CrystalGlassRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CrystalGlowRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CrystalLampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.CrystalStoneRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.DecoCastingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.DoorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.EnhancedRuneRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.FakeSkyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.FenceAuxRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.FloatingRelayRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.HeatLampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.HoverPadBlockRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.HoverPadLampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.LumenLampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.MusicTriggerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.PathRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.PortalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.PotionCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RFNodeRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RecipeEnderTNT;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RecipeTankBlock;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RedstonePodRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RelayFilterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RelayRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RepeaterLampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RouterNodeRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.RuneRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.SelectiveGlassRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks.TrapFloorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalCellRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalClusterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalFocusRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalGroupRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalLensRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalMirrorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalSeedRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.CrystalStarRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.DoorKeyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.ElementUnitRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.EnderEyeRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.EnergyCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.HighEnergyCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.HighTransformationCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.HighVoidCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.IridescentChunkRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.LumenChunkRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.LumenCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.RawCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.ShieldedCellRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.ThrowableGemRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.TransformationCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.VoidCoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items.VoidStorageRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.BeeConversionRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.ChargedItemPlayerBufferConnectionRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.ConfigRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.DivisionSigilActivationRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.DoubleJumpRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.EnchantmentRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.RepeaterTurboRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.RepeaterUpcraftRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.TankExpandingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special.TinkerToolPartRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.AdjacencyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.AspectFormerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.AspectJarRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.AutomatorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.AvoLaserRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.BatteryRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.BeaconRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.BeeStorageRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.BiomePainterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CastingInjectorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CaveLighterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ChromaCollectorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ChromaCrafterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CloakTowerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CobbleGenRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CompoundRepeaterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CropSpeedPlantRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CrystalBrewerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CrystalChargerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CrystalFenceRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CrystalFurnaceRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CrystalLaserRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.CrystalTankRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.DeathFogRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.EnchantDecompRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.EnchanterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.EssentiaRelayRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ExplosionShieldRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FabricatorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FarmerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FloatingLandmarkRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FluidDistributorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FluidRelayRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FluxMakerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FocusCrystalRecipes;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.focuscrystalrecipes.DefaultFocusCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.FunctionRelayRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.GlowFireRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.GuardianStoneRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.HarvestPlantRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.HeatLilyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.HoverPadRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.InfuserRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.InvTickerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.IridescentCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ItemCollectorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ItemInserterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ItemRiftRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LampControlRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LampRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LaunchPadRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LumenAlvearyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LumenBroadcastRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LumenTurretRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.LumenWireRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.MEDistributorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ManaBoosterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.MeteorTowerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.MinerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.MultiBuilderRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.MusicRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.NetworkOptimizerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.NetworkTransportRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.PageExtractorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ParticleSpawnerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.PlantAccelerationRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.PlayerInfuserRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ProgressLinkerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.PylonTurboRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RFDistributorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RecipeAreaBreaker;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RecipeCrystalRepeater;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RecipePersonalCharger;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RelaySourceRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ReversionLotusRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RiftRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RitualTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RouterHubRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.SmelteryDistributorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.SpawnerReprogrammerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.StandRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.TelePumpRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.ToolStorageRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.TransportWindowRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.VoidTrapRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.WarpGateRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.WeakRepeaterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.WirelessTransmitterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.AuraCleanerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.AuraPouchRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.BeeFrameRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.BottleneckFinderRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.BuilderWandRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.CaptureWandRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.CavePatherRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.ChainGunRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.DuplicationWandRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.EfficiencyCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.EnderBucketRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.EnderCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.EnhancedPendantRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.EtherealPendantRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.ExcavatorRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.FloatstoneBootsRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.GrowthWandRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.InventoryLinkRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.KillAuraRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.LightGunRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.LinkToolRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.MobSonarRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.MultiToolRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.NetherKeyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.OrePickRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.OreSilkerRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.OwnerKeyRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.PendantRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.ProbeRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.PurifyCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.PylonFinderRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.RecipeCacheRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.RecipeHoverWand;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.RecipeItemMover;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.SpawnerBypassRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.SplashGunRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.SplineAttackRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.StorageCrystalRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.StructureFinderRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.TeleGateLockRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.TeleportWandRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.TintedLensRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.TransitionRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.VacuumGunRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.WarpCapsuleRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools.WideCollectorRecipe;
import reika.chromaticraft.block.BlockPath;
import reika.chromaticraft.block.blockpath.PathType;
import reika.chromaticraft.block.blockpylonstructure.StoneTypes;
import reika.chromaticraft.block.crystal.blockcrystalglow.Bases;
import reika.chromaticraft.block.decoration.BlockRepeaterLight;
import reika.chromaticraft.block.worldgen.blockstructureshield.BlockType;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.progression.ChromaResearchManager;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.modinterface.CrystalBackpack;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaResearch;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityAccelerator;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.dragonapi.ModList;
import reika.dragonapi.asm.dependentmethodstripper.ModDependent;
import reika.dragonapi.instantiable.data.collections.onewaycollections.OneWayList;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.io.CustomRecipeList;
import reika.dragonapi.instantiable.io.LuaBlock;
import reika.dragonapi.libraries.ReikaRecipeHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.libraries.registry.ReikaTreeHelper;
import reika.dragonapi.modinteract.itemhandlers.AppEngHandler;
import reika.dragonapi.modinteract.itemhandlers.ForestryHandler;
import reika.dragonapi.modinteract.itemhandlers.ThaumItemHelper;
import reika.dragonapi.modinteract.itemhandlers.TinkerBlockHandler;
import reika.dragonapi.modinteract.itemhandlers.tinkertoolhandler.ToolParts;
import reika.dragonapi.modinteract.itemhandlers.tinkertoolhandler.WeaponParts;
import reika.dragonapi.modregistry.PowerTypes;
import reika.rotarycraft.registry.ItemRegistry;

import buildcraft.BuildCraftCore;
import cpw.mods.fml.common.registry.GameRegistry;

public class RecipesCastingTable implements CastingAPI {

	public static final RecipesCastingTable instance = new RecipesCastingTable();

	private final HashMap<RecipeType, OneWayList<CastingRecipe>> recipes = new HashMap();
	private final OneWayList<CastingRecipe> APIrecipes = new OneWayList();
	private final ArrayList<CastingRecipe> moddedItemRecipes = new ArrayList();

	private int maxID = 0;
	private final HashBiMap<Integer, CastingRecipe> recipeIDs;
	private final HashBiMap<String, CastingRecipe> recipeStringIDs;

	private int maxEnergyCost = 0;
	private int maxTotalEnergyCost = 0;

	private RecipesCastingTable() {
		recipeIDs = HashBiMap.create();
		recipeStringIDs = HashBiMap.create();
		this.loadRecipes();
	}

	private void loadRecipes() {
		if (ChromatiCraft.instance.isLocked())
			return;

		CrystalGroupRecipe redgroup = (CrystalGroupRecipe)this.addRecipe(new CrystalGroupRecipe(ChromaStacks.redGroup, CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA, ChromaStacks.auraDust, false));
		CrystalGroupRecipe greengroup = (CrystalGroupRecipe)this.addRecipe(new CrystalGroupRecipe(ChromaStacks.greenGroup, CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN, ChromaStacks.livingEssence, false));
		CrystalGroupRecipe orangegroup = (CrystalGroupRecipe)this.addRecipe(new CrystalGroupRecipe(ChromaStacks.orangeGroup, CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE, ChromaStacks.chromaDust, false));
		CrystalGroupRecipe whitegroup = (CrystalGroupRecipe)this.addRecipe(new CrystalGroupRecipe(ChromaStacks.whiteGroup, CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE, ChromaStacks.icyDust, false));

		this.addRecipe(new CrystalGroupRecipe(ChromaStacks.redGroup, CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA, ChromaStacks.auraDust, true));
		this.addRecipe(new CrystalGroupRecipe(ChromaStacks.greenGroup, CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN, ChromaStacks.livingEssence, true));
		this.addRecipe(new CrystalGroupRecipe(ChromaStacks.orangeGroup, CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE, ChromaStacks.chromaDust, true));
		this.addRecipe(new CrystalGroupRecipe(ChromaStacks.whiteGroup, CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE, ChromaStacks.icyDust, true));

		this.addRecipe(new CrystalClusterRecipe(ChromaStacks.primaryCluster, redgroup, greengroup));
		this.addRecipe(new CrystalClusterRecipe(ChromaStacks.secondaryCluster, orangegroup, whitegroup));
		this.addRecipe(new CrystalCoreRecipe(ChromaStacks.crystalCore, new ItemStack(Items.diamond)));
		CrystalStarRecipe star = new CrystalStarRecipe(ChromaStacks.crystalStar, new ItemStack(Items.nether_star));
		this.addRecipe(star);

		this.addRecipe(new VoidCoreRecipe(ChromaStacks.voidCore));
		this.addRecipe(new EnergyCoreRecipe(ChromaStacks.energyCore));
		this.addRecipe(new TransformationCoreRecipe(ChromaStacks.transformCore));

		this.addRecipe(new HighVoidCoreRecipe(ChromaStacks.voidCoreHigh));
		this.addRecipe(new HighEnergyCoreRecipe(ChromaStacks.energyCoreHigh));
		this.addRecipe(new HighTransformationCoreRecipe(ChromaStacks.transformCoreHigh));

		this.addRecipe(new LumenChunkRecipe(ChromaStacks.glowChunk, new ItemStack(Items.diamond)));
		this.addRecipe(new LumenCoreRecipe(ChromaStacks.lumenCore, ChromaStacks.crystalStar));

		this.addRecipe(new CrystalLensRecipe(ChromaStacks.crystalLens, new ItemStack(Blocks.glass)));

		this.addRecipe(new StorageCrystalRecipe(ChromaItems.STORAGE.getStackOf(), ChromaStacks.elementUnit));
		for (int i = 0; i < ChromaItems.STORAGE.getNumberMetadatas()-1; i++)
			this.addRecipe(new StorageCrystalRecipe(ChromaItems.STORAGE.getStackOfMetadata(i+1), ChromaItems.STORAGE.getStackOfMetadata(i)));

		for (int i = 0; i < 16; i++) {
			CrystalElement e = CrystalElement.elements[i];

			this.addRecipe(new RuneRecipe(e));
			this.addRecipe(new EnhancedRuneRecipe(e));
			ItemStack shard = ChromaItems.SHARD.getStackOfMetadata(i);
			ItemStack seed = ChromaItems.SEED.getStackOfMetadata(i);
			ItemStack block = new ItemStack(ChromaBlocks.PYLONSTRUCT.getBlockInstance(), 8, 0);
			ItemStack lamp = new ItemStack(ChromaBlocks.LAMP.getBlockInstance(), 1, i);

			IRecipe sr = new ShapedOreRecipe(block, " S ", "SCS", " S ", 'S', "stone", 'C', shard);
			this.addRecipe(new CrystalStoneRecipe(StoneTypes.SMOOTH, 8, sr));

			this.addRecipe(new CrystalSeedRecipe(seed, e, false));
			this.addRecipe(new CrystalSeedRecipe(seed, e, true));

			this.addRecipe(new LumenLampRecipe(ChromaBlocks.LAMPBLOCK.getStackOfMetadata(i), e));

			this.addRecipe(new TintedLensRecipe(e, ChromaStacks.crystalLens, Items.iron_ingot, 1));
			this.addRecipe(new TintedLensRecipe(e, ChromaStacks.crystalLens, Items.gold_ingot, 2));
			this.addRecipe(new TintedLensRecipe(e, ChromaStacks.crystalLens, ChromaStacks.chromaIngot, 4));

			sr = ReikaRecipeHelper.getShapedRecipeFor(lamp, " s ", "sss", "SSS", 's', shard, 'S', ReikaItemHelper.stoneSlab);
			this.addRecipe(new CrystalLampRecipe(lamp, sr));

			this.addRecipe(new RelayRecipe(e));

			this.addRecipe(new ThrowableGemRecipe(e));

			for (int k = 0; k < Bases.baseList.length; k++) {
				Bases b = Bases.baseList[k];
				ItemStack glow = ChromaBlocks.GLOW.getStackOfMetadata(i+k*16);
				sr = ReikaRecipeHelper.getShapedRecipeFor(glow, "S", "s", 'S', shard, 's', b.ingredient);
				this.addRecipe(new CrystalGlowRecipe(glow, sr));
			}

			this.addRecipe(new CrystalAltarRecipe(e));

			this.addRecipe(new CrystalGlassRecipe(e));
		}
		this.addRecipe(new CompoundRelayRecipe(ChromaBlocks.RELAY.getStackOfMetadata(16), new ItemStack(Items.diamond)));

		this.addRecipe(new FloatingRelayRecipe(ChromaBlocks.FLOATINGRELAY.getStackOf(), ChromaBlocks.RELAY.getStackOfMetadata(16)));

		Block block = ChromaBlocks.PYLONSTRUCT.getBlockInstance();
		ItemStack smooth = new ItemStack(block, 1, 0);
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.COLUMN, 2, "S", "S", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.BRICKS, 4, "SS", "SS", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.BEAM, 2, "SS", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.ENGRAVED, 4, " S ", "S S", " S ", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.EMBOSSED, 5, " S ", "SSS", " S ", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.CORNER, 5, "SSS", "S  ", "S  ", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.GROOVE1, 3, "S", "S", "S", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.GROOVE2, 3, "SSS", 'S', smooth));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.STABILIZER, 4, "sSs", "ScS", "sSs", 'S', smooth, 'c', ChromaStacks.chargedWhiteShard, 's', ChromaBlocks.RUNE.getStackOf(CrystalElement.WHITE)));
		this.addRecipe(new CrystalStoneRecipe(StoneTypes.RESORING, 6, "SSS", "ccc", "SSS", 'S', smooth, 'c', ChromaStacks.chargedWhiteShard));

		this.addRecipe(new CompoundRuneRecipe(ChromaBlocks.PYLONSTRUCT.getStackOfMetadata(StoneTypes.MULTICHROMIC.ordinal()), ChromaStacks.iridCrystal));

		IRecipe sr = ReikaRecipeHelper.getShapedRecipeFor(ChromaItems.OREPICK.getStackOf(), "EPE", "cSc", "cSc", 'c', ChromaStacks.chromaDust, 'S', Items.stick, 'E', Items.ender_eye, 'P', Items.iron_pickaxe);
		this.addRecipe(new OrePickRecipe(ChromaItems.OREPICK.getStackOf(), sr));

		sr = ReikaRecipeHelper.getShapedRecipeFor(ChromaItems.ORESILK.getStackOf(), "EPE", "cdc", "cSc", 'c', ChromaStacks.chromaDust, 'S', Items.stick, 'E', Items.emerald, 'P', Items.diamond_pickaxe, 'd', Items.diamond);
		this.addRecipe(new OreSilkerRecipe(ChromaItems.ORESILK.getStackOf(), sr));

		sr = ReikaRecipeHelper.getShapedRecipeFor(ChromaItems.MULTITOOL.getStackOf(), "APS", "csc", "csc", 'c', ChromaStacks.chromaDust, 's', Items.stick, 'A', Items.iron_axe, 'S', Items.iron_shovel, 'P', Items.iron_pickaxe);
		this.addRecipe(new MultiToolRecipe(ChromaItems.MULTITOOL.getStackOf(), sr));

		this.addRecipe(new GuardianStoneRecipe(ChromaTiles.GUARDIAN.getCraftedProduct(), ChromaStacks.crystalStar));

		for (int i = 0; i < 16; i++) {
			if (AdjacencyUpgrades.upgrades[i].isImplemented()) {
				CrystalElement e = CrystalElement.elements[i];
				for (int k = 0; k < TileEntityAccelerator.MAX_TIER; k++) {
					this.addRecipe(new AdjacencyRecipe(e, k));
				}
			}
		}

		ItemStack is = ChromaTiles.STAND.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "I I", "SLS", "CCC", 'I', Items.iron_ingot, 'C', "cobblestone", 'S', ReikaItemHelper.stoneSlab, 'L', ReikaItemHelper.lapisDye);
		this.addRecipe(new StandRecipe(is, sr));

		is = ChromaTiles.ENCHANTER.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "eGe", "OEO", "BOB", 'e', Items.emerald, 'O', Blocks.obsidian, 'B', ReikaItemHelper.stoneBricks, 'E', Blocks.enchanting_table, 'G', Items.gold_ingot);
		this.addRecipe(new EnchanterRecipe(is, sr));

		is = ChromaTiles.BREWER.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "S S", "ScS", "CCC", 'C', "stone", 'c', Items.cauldron, 'S', ChromaItems.SHARD.getAnyMetaStack());
		this.addRecipe(new CrystalBrewerRecipe(is, sr));

		is = ChromaTiles.CHROMAFLOWER.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "LFL", "GsG", 'L', "treeLeaves", 'F', "flower", 'G', Items.glowstone_dust, 's', ChromaItems.SHARD.getAnyMetaStack());
		this.addRecipe(new ChromaFlowerRecipe(is, sr));

		is = ChromaBlocks.SELECTIVEGLASS.getStackOf();
		sr = new ShapedOreRecipe(is, "GgG", "beb", "GvG", 'G', Blocks.glass, 'g', Items.glowstone_dust, 'e', ChromaStacks.teleDust, 'v', ChromaStacks.voidDust, 'b', ChromaStacks.icyDust);
		this.addRecipe(new SelectiveGlassRecipe(is, sr, false));
		//sr = new ShapedOreRecipe(is, "GgG", "beb", "GvG", 'G', Blocks.glass, 'g', Blocks.glowstone, 'e', ChromaStacks.enderDust, 'v', ChromaStacks.spaceDust, 'b', ChromaStacks.);
		//this.addRecipe(new SelectiveGlassRecipe(is, sr, true));

		//sr = ReikaRecipeHelper.getShapedRecipeFor(ChromaStacks.crystalMirror, "GWI", "GWI", "GWI", 'G', Blocks.glass, 'I', Items.iron_ingot, 'W', ChromaItems.SHARD.getStackOfMetadata(15));
		this.addRecipe(new CrystalMirrorRecipe(ChromaStacks.crystalMirror, new ItemStack(Blocks.glass)));

		this.addRecipe(new RiftRecipe(ChromaTiles.RIFT.getCraftedProduct(), ChromaStacks.voidCore));
		this.addRecipe(new CrystalTankRecipe(ChromaTiles.TANK.getCraftedProduct(), ChromaStacks.voidCore));
		this.addRecipe(new RecipeTankBlock(new ItemStack(ChromaBlocks.TANK.getBlockInstance()), new ItemStack(Items.diamond)));
		this.addRecipe(new CrystalFurnaceRecipe(ChromaTiles.FURNACE.getCraftedProduct(), ChromaStacks.energyCore));
		this.addRecipe(new CrystalLaserRecipe(ChromaTiles.LASER.getCraftedProduct(), ChromaStacks.energyCore));
		this.addRecipe(new CrystalChargerRecipe(ChromaTiles.CHARGER.getCraftedProduct(), ChromaStacks.crystalCore));

		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			ItemStack shard = ChromaItems.SHARD.getStackOfMetadata(i);
			ItemStack lamp = new ItemStack(ChromaBlocks.LAMP.getBlockInstance(), 1, i);
			ItemStack cave = new ItemStack(ChromaBlocks.CRYSTAL.getBlockInstance(), 1, i);
			ItemStack supercry = new ItemStack(ChromaBlocks.SUPER.getBlockInstance(), 1, i);
			ItemStack seed = ChromaItems.SEED.getStackOfMetadata(i);

			this.addRecipe(new PendantRecipe(e));
			this.addRecipe(new EnhancedPendantRecipe(e));
			this.addRecipe(new PotionCrystalRecipe(e));

			sr = ReikaRecipeHelper.getShapedRecipeFor(ChromaStacks.rawCrystal, " F ", "FSF", " F ", 'F', ChromaStacks.purityDust, 'S', shard);
			this.addRecipe(new RawCrystalRecipe(ChromaStacks.rawCrystal, sr));
		}

		this.addRecipe(new CrystalFocusRecipe(ChromaStacks.crystalFocus, ChromaStacks.primaryCluster));
		this.addRecipe(new CrystalMirrorRecipe(ChromaStacks.crystalMirror, ChromaStacks.getChargedShard(CrystalElement.WHITE)));

		RecipeCrystalRepeater repeater = new RecipeCrystalRepeater(ChromaStacks.crystalCore);
		this.addRecipe(repeater);

		this.addRecipe(new TransitionRecipe(ChromaItems.TRANSITION.getStackOf(), ChromaStacks.transformCore));
		this.addRecipe(new ExcavatorRecipe(ChromaItems.EXCAVATOR.getStackOf(), ChromaStacks.energyCore));

		this.addRecipe(new ElementUnitRecipe(ChromaStacks.elementUnit, ChromaStacks.bindingCrystal));

		is = ChromaTiles.HEATLILY.getCraftedProduct();
		sr = new ShapedOreRecipe(is, " F ", "FBF", "LSL", 'L', Blocks.waterlily, 'F', "flower", 'S', ChromaStacks.orangeShard, 'B', Items.blaze_powder);
		HeatLilyRecipe hr = new HeatLilyRecipe(is, sr);
		this.addRecipe(hr);

		is = ChromaTiles.REVERTER.getCraftedProduct();
		sr = new ShapedOreRecipe(is, " F ", "FBF", "LSL", 'L', ReikaItemHelper.fern, 'F', "flower", 'S', ChromaStacks.greenShard, 'B', Items.redstone);
		this.addRecipe(new ReversionLotusRecipe(is, sr));

		is = ChromaTiles.COBBLEGEN.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "LSL", "FBF", " D ", 'L', "treeLeaves", 'D', Items.glowstone_dust, 'F', "flower", 'S', ChromaStacks.cyanShard, 'B', Blocks.glass);
		this.addRecipe(new CobbleGenRecipe(is, sr));

		is = ChromaTiles.PLANTACCEL.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "LSL", "FRF", "LSL", 'L', "treeLeaves", 'R', Items.redstone, 'F', "flower", 'S', ChromaStacks.lightBlueShard);
		this.addRecipe(new PlantAccelerationRecipe(is, sr));

		is = ChromaTiles.CROPSPEED.getCraftedProduct();
		sr = new ShapedOreRecipe(is, " R ", "FRF", "LSL", 'L', "treeLeaves", 'R', Items.redstone, 'F', "flower", 'S', ChromaStacks.lightBlueShard);
		this.addRecipe(new CropSpeedPlantRecipe(is, sr));

		is = ChromaTiles.RITUAL.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "SES", "CSC", "CCC", 'C', "cobblestone", 'S', ChromaItems.SHARD.getAnyMetaStack(), 'E', ChromaStacks.energyPowder);
		this.addRecipe(new RitualTableRecipe(is, sr));

		is = ChromaTiles.COLLECTOR.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "SES", "ScS", "CCC", 'E', Items.ender_eye, 'C', "stone", 'c', Blocks.glowstone, 'S', ChromaItems.SHARD.getAnyMetaStack());
		this.addRecipe(new ChromaCollectorRecipe(is, sr));

		this.addRecipe(new SpawnerReprogrammerRecipe(ChromaTiles.REPROGRAMMER.getCraftedProduct(), ChromaStacks.transformCoreHigh));

		this.addRecipe(new TelePumpRecipe(ChromaTiles.TELEPUMP.getCraftedProduct(), ChromaStacks.energyCoreHigh));

		this.addRecipe(new CompoundRepeaterRecipe(ChromaStacks.crystalFocus));

		//is = ChromaTiles.FIBER.getCraftedProduct();
		//sr = ReikaRecipeHelper.getShapedRecipeFor(is, "GgG", "GDG", "GgG", 'G', Blocks.glass, 'D', Items.diamond, 'g', Items.glowstone_dust);
		//this.addRecipe(new FiberRecipe(is, sr));

		this.addRecipe(new IridescentChunkRecipe(ChromaStacks.iridChunk, ChromaStacks.bindingCrystal));

		this.addRecipe(new IridescentCrystalRecipe(ChromaTiles.CRYSTAL.getCraftedProduct(), new ItemStack(Items.diamond)));

		InfuserRecipe ir = new InfuserRecipe(ChromaTiles.INFUSER.getCraftedProduct(), ChromaTiles.STAND.getCraftedProduct());
		this.addRecipe(ir);
		this.addRecipe(new PlayerInfuserRecipe(ChromaTiles.PLAYERINFUSER.getCraftedProduct(), ir));

		if (ModList.THAUMCRAFT.isLoaded()) {
			this.addRecipe(new AspectFormerRecipe(ChromaTiles.ASPECT.getCraftedProduct(), ChromaStacks.transformCore));
			this.addRecipe(new AspectJarRecipe(ChromaTiles.ASPECTJAR.getCraftedProduct(), ChromaStacks.voidCore));

			is = ChromaTiles.ESSENTIARELAY.getCraftedProduct();
			sr = ReikaRecipeHelper.getShapedRecipeFor(is, "DSD", "SOS", "DSD", 'D', ChromaStacks.energyPowder, 'S', ChromaStacks.auraDust, 'O', ChromaStacks.blackShard);
			this.addRecipe(new EssentiaRelayRecipe(is, sr));

			is = ChromaItems.WARP.getStackOf();
			this.addRecipe(new AuraCleanerRecipe(is, new ItemStack(Items.potionitem)));
		}

		this.addRecipe(new InventoryLinkRecipe(ChromaItems.LINK.getStackOf(), new ItemStack(Items.ender_pearl)));

		this.addRecipe(new LampRecipe(ChromaTiles.LAMP.getCraftedProduct(), new ItemStack(Blocks.glowstone)));

		this.addRecipe(new BeaconRecipe(ChromaTiles.BEACON.getCraftedProduct(), new ItemStack(Items.nether_star)));

		is = ChromaItems.FINDER.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "SIS", "IAI", "SIS", 'A', ChromaStacks.auraDust, 'I', Items.iron_ingot, 'S', ChromaItems.SHARD.getAnyMetaStack());
		this.addRecipe(new PylonFinderRecipe(is, sr));

		this.addRecipe(new BatteryRecipe(ChromaTiles.POWERTREE.getCraftedProduct(), ChromaStacks.crystalStar));

		this.addRecipe(new InvTickerRecipe(ChromaTiles.TICKER.getCraftedProduct(), new ItemStack(Blocks.chest)));

		this.addRecipe(new MinerRecipe(ChromaTiles.MINER.getCraftedProduct(), ChromaStacks.energyCoreHigh));

		this.addRecipe(new FabricatorRecipe(ChromaTiles.FABRICATOR.getCraftedProduct(), ChromaStacks.transformCoreHigh));

		is = ChromaTiles.LAMPCONTROL.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "DED", "RQR", "ScS", 'c', ChromaStacks.chromaDust, 'D', ChromaStacks.auraDust, 'E', Items.ender_pearl, 'R', Items.redstone, 'Q', Items.quartz, 'S', ReikaItemHelper.stoneSlab);
		this.addRecipe(new LampControlRecipe(is, sr));
		/*
		this.addRecipe(new UpgradeRecipe(ChromaStacks.silkUpgrade, new ItemStack(Items.diamond)));
		this.addRecipe(new UpgradeRecipe(ChromaStacks.speedUpgrade, new ItemStack(Items.redstone)));
		this.addRecipe(new UpgradeRecipe(ChromaStacks.efficiencyUpgrade, new ItemStack(Items.emerald)));
		 */

		if (ChromaOptions.ENDERTNT.getState())
			this.addRecipe(new RecipeEnderTNT(ChromaBlocks.TNT.getStackOf(), new ItemStack(Items.nether_star)));

		this.addRecipe(new TeleportWandRecipe(ChromaItems.TELEPORT.getStackOf(), ChromaStacks.energyCore));

		this.addRecipe(new BuilderWandRecipe(ChromaItems.BUILDER.getStackOf(), ChromaStacks.transformCore));

		this.addRecipe(new CaptureWandRecipe(ChromaItems.CAPTURE.getStackOf(), new ItemStack(Blocks.web)));

		this.addRecipe(new GrowthWandRecipe(ChromaItems.GROWTH.getStackOf(), new ItemStack(Items.diamond)));

		this.addRecipe(new DuplicationWandRecipe(ChromaItems.DUPLICATOR.getStackOf(), ChromaStacks.transformCore));

		if (ModList.APPENG.isLoaded()) {
			this.addRecipe(new VoidStorageRecipe(ChromaItems.VOIDCELL.getStackOf(), ChromaStacks.voidCore));
			this.addRecipe(new CrystalCellRecipe(ChromaItems.CRYSTALCELL.getStackOf(), ChromaStacks.crystalFocus));
			this.addRecipe(new ShieldedCellRecipe(ChromaItems.SHIELDEDCELL.getStackOf()));
		}

		for (int i = 0; i < BlockPath.PathType.list.length; i++) {
			PathType p = BlockPath.PathType.list[i];
			this.addRecipe(new PathRecipe(ChromaItems.ELEMENTAL.getStackOf(CrystalElement.LIME), i, p.getBlock()));
		}

		//if (ChromaOptions.BIOMEPAINTER.getState())
		this.addRecipe(new BiomePainterRecipe(ChromaTiles.BIOMEPAINTER.getCraftedProduct(), ChromaStacks.transformCoreHigh));

		this.addRecipe(new AuraPouchRecipe(ChromaItems.AURAPOUCH.getStackOf(), ChromaStacks.voidCore));

		this.addRecipe(new FarmerRecipe(ChromaTiles.FARMER.getCraftedProduct(), ChromaStacks.energyCore));

		this.addRecipe(new RelaySourceRecipe(ChromaTiles.RELAYSOURCE.getCraftedProduct(), ChromaStacks.crystalFocus));

		this.addRecipe(new ItemCollectorRecipe(ChromaTiles.ITEMCOLLECTOR.getCraftedProduct(), ChromaStacks.voidCore));

		this.addRecipe(new PortalRecipe(ChromaBlocks.PORTAL.getStackOf(), ChromaStacks.voidCoreHigh, repeater));

		Object[] metal = new Object[]{Items.iron_ingot, 1, Items.gold_ingot, 4, "ingotCopper", 1, "ingotSilver", 2, ChromaStacks.conductiveIngot, 6, ChromaStacks.fieryIngot, 8};
		for (int i = 0; i < metal.length; i += 2) {
			if (metal[i] instanceof String && !ReikaItemHelper.oreItemExists((String)metal[i]))
				continue;
			int n = (int)metal[i+1];
			this.addRecipe(new HeatLampRecipe(metal[i], n, hr, false, false));
			this.addRecipe(new HeatLampRecipe(metal[i], n*2, hr, false, true));
			this.addRecipe(new HeatLampRecipe(metal[i], n, hr, true, false));
		}

		metal = new Object[]{Items.iron_ingot, 1, Items.gold_ingot, 4, "ingotCopper", 1, "ingotSilver", 2, ChromaStacks.conductiveIngot, 6, "ingotRedAlloy", 4};
		for (int i = 0; i < metal.length; i += 2) {
			if (metal[i] instanceof String && !ReikaItemHelper.oreItemExists((String)metal[i]))
				continue;
			is = ReikaItemHelper.getSizedItemStack(ChromaBlocks.REDSTONEPOD.getStackOf(), (int)metal[i+1]);
			sr = new ShapedOreRecipe(is, "fbf", "dad", "fbf", 'f', Items.redstone, 'a', metal[i], 'd', ChromaStacks.auraDust, 'b', ChromaStacks.beaconDust);
			this.addRecipe(new RedstonePodRecipe(is, sr));
		}

		metal = new Object[]{Items.iron_ingot, 1, Items.gold_ingot, 4, "ingotCopper", 1, "ingotSilver", 2, ChromaStacks.conductiveIngot, 6, "ingotEnderium", 6, "ingotVibrantAlloy", 8, "RotaryCraft:ingotTungstenAlloy", 12, "ingotSuperconducting", 12};
		for (int i = 0; i < metal.length; i += 2) {
			if (metal[i] instanceof String && !ReikaItemHelper.oreItemExists((String)metal[i]))
				continue;
			is = ReikaItemHelper.getSizedItemStack(ChromaBlocks.RFPOD.getStackOf(), (int)metal[i+1]);
			sr = new ShapedOreRecipe(is, "bfb", "faf", "dad", 'f', Items.redstone, 'a', metal[i], 'd', ChromaStacks.auraDust, 'b', ChromaStacks.beaconDust);
			this.addRecipe(new RFNodeRecipe(is, sr));
		}

		this.addRecipe(new EnderCrystalRecipe(ChromaItems.ENDERCRYSTAL.getStackOf(), ChromaStacks.crystalStar));

		is = ChromaItems.BULKMOVER.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "OCO", "ODO", " I ", 'O', Blocks.obsidian, 'D', Items.diamond, 'C', Blocks.chest, 'I', Items.iron_ingot);
		this.addRecipe(new RecipeItemMover(is, sr));

		this.addRecipe(new AutomatorRecipe(ChromaTiles.AUTOMATOR.getCraftedProduct(), ChromaStacks.transformCoreHigh));

		this.addRecipe(new ChainGunRecipe(ChromaItems.CHAINGUN.getStackOf(), ChromaStacks.crystalCore));
		this.addRecipe(new SplashGunRecipe(ChromaItems.SPLASHGUN.getStackOf(), ChromaStacks.crystalCore));
		this.addRecipe(new VacuumGunRecipe(ChromaItems.VACUUMGUN.getStackOf(), ChromaStacks.voidCoreHigh));

		if (ModList.APPENG.isLoaded()) {
			this.addRecipe(new MEDistributorRecipe(ChromaTiles.MEDISTRIBUTOR.getCraftedProduct(), ChromaStacks.transformCore));
		}

		this.addRecipe(new RecipeHoverWand(ChromaItems.HOVERWAND.getStackOf(), ChromaStacks.voidCore));

		if (PowerTypes.RF.isLoaded()) {
			is = ChromaTiles.RFDISTRIBUTOR.getCraftedProduct();
			sr = ReikaRecipeHelper.getShapedRecipeFor(is, "rir", "iGi", "rir", 'i', Items.iron_ingot, 'r', Blocks.redstone_block, 'G', Blocks.glowstone);
			this.addRecipe(new RFDistributorRecipe(is, sr));
		}

		is = ChromaTiles.FLUIDDISTRIBUTOR.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "rir", "iGi", "rir", 'i', Items.iron_ingot, 'r', Items.water_bucket, 'G', Blocks.glowstone);
		this.addRecipe(new FluidDistributorRecipe(is, sr));

		this.addRecipe(new TransportWindowRecipe(ChromaTiles.WINDOW.getCraftedProduct(), new ItemStack(Items.diamond)));

		is = ChromaItems.LINKTOOL.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "  l", " s ", "g  ", 'g', ChromaStacks.grayShard, 'l', ChromaStacks.limeShard, 's', Items.stick);
		this.addRecipe(new LinkToolRecipe(is, sr));

		this.addRecipe(new RecipePersonalCharger(ChromaTiles.PERSONAL.getCraftedProduct(), ChromaStacks.crystalCore));

		is = ChromaTiles.MUSIC.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "sns", "non", "sns", 's', ChromaItems.SHARD.getAnyMetaStack(), 'n', Blocks.noteblock, 'o', Items.clock);
		this.addRecipe(new MusicRecipe(is, sr));

		this.addRecipe(new PylonTurboRecipe(ChromaTiles.PYLONTURBO.getCraftedProduct(), ChromaStacks.lumenCore, repeater));

		is = ChromaTiles.TURRET.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " g ", "gsg", " b ", 's', ChromaItems.SHARD.getStackOf(CrystalElement.PINK), 'g', Items.glowstone_dust, 'b', new ItemStack(block));
		this.addRecipe(new LumenTurretRecipe(is, sr));

		is = ChromaBlocks.DOOR.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "grg", "rer", "lpl", 'g', Items.glowstone_dust, 'r', Items.redstone, 'l', ReikaItemHelper.lapisDye, 'e', Items.ender_pearl, 'p', Blocks.piston);
		this.addRecipe(new DoorRecipe(is, sr));

		is = ChromaItems.KEY.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "e i", " il", "ir ", 'i', Items.iron_ingot, 'r', Items.redstone, 'l', ReikaItemHelper.lapisDye, 'e', Items.ender_pearl);
		this.addRecipe(new DoorKeyRecipe(is, sr));

		is = ChromaItems.SHARE.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "lil", "gig", "rir", 'i', Items.iron_ingot, 'r', Items.redstone, 'l', ReikaItemHelper.lapisDye, 'g', Items.glowstone_dust);
		this.addRecipe(new OwnerKeyRecipe(is, sr));

		this.addRecipe(new CrystalFenceRecipe(ChromaTiles.FENCE.getCraftedProduct(), new ItemStack(Blocks.gold_block)));

		is = ChromaBlocks.FENCE.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "crc", "cgc", "cic", 'i', Items.iron_ingot, 'r', Items.redstone, 'c', Blocks.cobblestone, 'g', Items.glowstone_dust);
		this.addRecipe(new FenceAuxRecipe(is, sr));

		this.addRecipe(new LumenBroadcastRecipe(ChromaStacks.crystalStar));

		this.addRecipe(new CloakTowerRecipe(ChromaTiles.CLOAKING.getCraftedProduct(), ChromaStacks.crystalFocus));

		is = ChromaTiles.LIGHTER.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "sas", "ala", "sbs", 's', ChromaStacks.blueShard, 'b', ChromaStacks.chromaDust, 'a', ChromaStacks.auraDust, 'l', ChromaBlocks.LAMPBLOCK.getStackOfMetadata(CrystalElement.WHITE.ordinal()));
		this.addRecipe(new CaveLighterRecipe(is, sr));

		this.addRecipe(new EnchantmentRecipe(ChromaResearch.EXCAVATOR, ChromaItems.EXCAVATOR.getStackOf(), ChromaStacks.auraDust, ChromaStacks.chargedPurpleShard, ChromaStacks.chromaDust, Enchantment.silkTouch, 1));
		this.addRecipe(new EnchantmentRecipe(ChromaResearch.EXCAVATOR, ChromaItems.EXCAVATOR.getStackOf(), ReikaItemHelper.lapisDye.copy(), ChromaStacks.chargedPurpleShard, ChromaStacks.focusDust, Enchantment.fortune, 5));
		this.addRecipe(new EnchantmentRecipe(ChromaResearch.EXCAVATOR, ChromaItems.EXCAVATOR.getStackOf(), ChromaStacks.enderDust, ChromaStacks.chargedLimeShard, ChromaStacks.beaconDust, ChromaEnchants.AUTOCOLLECT.getEnchantment(), 1));
		this.addRecipe(new EnchantmentRecipe(ChromaResearch.EXCAVATOR, ChromaItems.EXCAVATOR.getStackOf(), ChromaStacks.energyPowder, ChromaStacks.chargedBrownShard, new ItemStack(Items.diamond), ChromaEnchants.MINETIME.getEnchantment(), 1));
		this.addRecipe(new EnchantmentRecipe(ChromaResearch.EXCAVATOR, ChromaItems.EXCAVATOR.getStackOf(), ChromaStacks.teleDust, ChromaStacks.chargedBlueShard, ChromaStacks.lumenGem, Enchantment.flame, 1));
		this.addRecipe(new EnchantmentRecipe(ChromaResearch.EXCAVATOR, ChromaItems.EXCAVATOR.getStackOf(), ChromaStacks.voidDust, ChromaStacks.chargedLightBlueShard, ChromaStacks.floatstone, ChromaEnchants.AIRMINER.getEnchantment(), 1));

		this.addRecipe(new EnchantmentRecipe(ChromaResearch.TRANSITION, ChromaItems.TRANSITION.getStackOf(), ChromaStacks.bindingCrystal, ChromaStacks.chargedGrayShard, ChromaStacks.resonanceDust, Enchantment.silkTouch, 1));

		this.addRecipe(new EnchantmentRecipe(ChromaResearch.GROWTH, ChromaItems.GROWTH.getStackOf(), ReikaItemHelper.bonemeal.copy(), ChromaStacks.chargedGreenShard, ChromaStacks.elementDust, Enchantment.power, 1));

		is = new ItemStack(ChromaBlocks.MUSICTRIGGER.getBlockInstance(), 4, 0);
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "scs", "sas", "iri", 's', ChromaBlocks.STRUCTSHIELD.getStackOfMetadata(BlockType.STONE.ordinal()), 'c', ChromaStacks.chromaDust, 'a', ChromaStacks.auraDust, 'r', Items.redstone, 'i', Items.gold_ingot);
		this.addRecipe(new MusicTriggerRecipe(is, sr));

		//this.addRecipe(new GlowFireRecipe(ChromaTiles.GLOWFIRE.getCraftedProduct(), ChromaStacks.transformCore));

		is = ChromaTiles.INSERTER.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "dsd", "cec", "rcr", 's', ChromaStacks.limeShard, 'd', ChromaStacks.teleDust, 'c', "cobblestone", 'r', Items.redstone, 'e', Items.ender_pearl);
		this.addRecipe(new ItemInserterRecipe(is, sr));

		is = ChromaTiles.WEAKREPEATER.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "wgw", "scs", "wrw", 'r', Items.glowstone_dust, 'g', ChromaStacks.beaconDust, 'w', "plankWood", 's', "stickWood", 'c', ChromaItems.BUCKET.getStackOfMetadata(0));
		this.addRecipe(new WeakRepeaterRecipe(is, sr));

		is = ChromaTiles.ENCHANTDECOMP.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "sgs", "GbG", "ccc", 'G', Blocks.glass, 'b', Items.bucket, 'g', Blocks.glowstone, 's', ChromaItems.SHARD.getAnyMetaStack(), 'c', "stone");
		this.addRecipe(new EnchantDecompRecipe(is, sr));

		is = ChromaTiles.LUMENWIRE.getCraftedProduct();
		sr = new ShapedOreRecipe(is, " s ", "OGO", "ccc", 'O', Blocks.obsidian, 'G', Items.glowstone_dust, 's', ChromaItems.SHARD.getStackOf(CrystalElement.BLUE), 'c', "cobblestone");
		this.addRecipe(new LumenWireRecipe(is, sr));

		this.addRecipe(new PurifyCrystalRecipe(ChromaItems.PURIFY.getStackOf(), ChromaStacks.iridChunk));
		this.addRecipe(new EfficiencyCrystalRecipe(ChromaItems.EFFICIENCY.getStackOf(), ChromaBlocks.SUPER.getStackOfMetadata(CrystalElement.BLACK.ordinal())));

		is = ChromaTiles.PARTICLES.getCraftedProduct();
		sr = new ShapedOreRecipe(is, "sg ", "gRg", " gs", 'R', Blocks.redstone_block, 'g', Items.glowstone_dust, 's', ChromaItems.SHARD.getAnyMetaStack());
		this.addRecipe(new ParticleSpawnerRecipe(is, sr));

		this.addRecipe(new KillAuraRecipe(ChromaItems.KILLAURAGUN.getStackOf(), ChromaStacks.energyCoreHigh));

		for (int i = 0; i < 3; i++)
			this.addRecipe(new MeteorTowerRecipe(i));

		this.addRecipe(new RecipeAreaBreaker(ChromaTiles.AREABREAKER.getCraftedProduct(), ChromaStacks.crystalFocus));

		this.addRecipe(new WirelessTransmitterRecipe(ChromaTiles.WIRELESS.getCraftedProduct(), ChromaStacks.elementUnit));

		this.addRecipe(new WarpGateRecipe(ChromaTiles.TELEPORT.getCraftedProduct(), ChromaStacks.voidCoreHigh));

		this.addRecipe(new FloatstoneBootsRecipe(ChromaItems.FLOATBOOTS.getStackOf(), new ItemStack(Items.iron_boots), false));
		if (ModList.ROTARYCRAFT.isLoaded()) {
			this.addRecipe(new FloatstoneBootsRecipe(ChromaItems.FLOATBOOTS.getStackOf(), ItemRegistry.BEDBOOTS.getStackOf(), true));
			this.addRecipe(new FloatstoneBootsRecipe(ChromaItems.FLOATBOOTS.getStackOf(), ItemRegistry.BEDJUMP.getStackOfMetadata(OreDictionary.WILDCARD_VALUE), true));
		}

		is = ChromaTiles.FLUIDRELAY.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " D ", "LAL", "IGI", 'D', ChromaStacks.beaconDust, 'A', ChromaStacks.auraDust, 'L', ReikaItemHelper.lapisDye, 'I', Items.iron_ingot, 'G', Blocks.glass);
		this.addRecipe(new FluidRelayRecipe(is, sr));

		is = ChromaItems.WARPCAPSULE.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " ig", "aea", "li", 'e', ChromaItems.ELEMENTAL.getStackOf(CrystalElement.LIME), 'g', ChromaStacks.limeShard, 'l', ChromaStacks.lightBlueShard, 'a', ChromaStacks.auraDust, 'i', Items.iron_ingot);
		this.addRecipe(new WarpCapsuleRecipe(is, sr));

		if (ModList.MYSTCRAFT.isLoaded()) {
			is = ChromaTiles.BOOKDECOMP.getCraftedProduct();
			sr = ReikaRecipeHelper.getShapedRecipeFor(is, " g ", "dcd", "SSS", 'g', Items.glowstone_dust, 'd', Items.diamond, 'S', ReikaItemHelper.stoneSlab, 'c', ChromaStacks.enderDust);
			this.addRecipe(new PageExtractorRecipe(is, sr));
		}

		is = ChromaTiles.HARVESTPLANT.getCraftedProduct();
		Object root = ModList.BOTANIA.isLoaded() ? ReikaItemHelper.lookupItem(ModList.BOTANIA, "manaResource", 6) : null;
		if (root == null)
			root = Items.redstone;
		ItemStack in = ModList.BOTANIA.isLoaded() ? ReikaItemHelper.lookupBlock(ModList.BOTANIA, "specialFlower", 0) : null;
		if (in != null) {
			in.stackTagCompound = new NBTTagCompound();
			in.stackTagCompound.setString("type", "munchdew");
		}
		if (in == null)
			in = ChromaStacks.auraDust;
		sr = new ShapedOreRecipe(is, "FAF", "fEf", "LRL", 'E', in, 'f', "flower", 'L', "treeLeaves", 'R', root, 'F', ChromaStacks.livingEssence, 'A', ChromaStacks.auraDust);
		this.addRecipe(new HarvestPlantRecipe(is, sr));

		this.addRecipe(new GlowFireRecipe(ChromaTiles.GLOWFIRE.getCraftedProduct(), ChromaStacks.transformCore));

		this.addRecipe(new AvoLampRecipe(ChromaBlocks.AVOLAMP.getStackOf(), ChromaStacks.avolite));

		this.addRecipe(new AvoLaserRecipe(ChromaTiles.AVOLASER.getCraftedProduct(), ChromaStacks.avolite));

		this.addRecipe(new RelayFilterRecipe(ChromaBlocks.RELAYFILTER.getStackOf(), ChromaItems.LENS.getAnyMetaStack()));

		this.addRecipe(new RouterNodeRecipe(ChromaBlocks.ROUTERNODE.getStackOfMetadata(0), false));
		this.addRecipe(new RouterNodeRecipe(ChromaBlocks.ROUTERNODE.getStackOfMetadata(1), true));

		this.addRecipe(new RouterHubRecipe(ChromaTiles.ROUTERHUB.getCraftedProduct(), ChromaStacks.elementUnit));

		this.addRecipe(new FocusCrystalRecipes.FlawedFocusCrystalRecipe());
		DefaultFocusCrystalRecipe fr = new FocusCrystalRecipes.DefaultFocusCrystalRecipe();
		this.addRecipe(fr);
		this.addRecipe(new FocusCrystalRecipes.RefinedFocusCrystalRecipe(fr));
		this.addRecipe(new FocusCrystalRecipes.ExquisiteFocusCrystalRecipe(fr));
		this.addRecipe(new FocusCrystalRecipes.TurboFocusCrystalRecipe(fr));

		is = ChromaItems.STRUCTUREFINDER.getStackOf();
		sr = new ShapedOreRecipe(is, "IAs", "ASA", "sAO", 'A', ChromaStacks.auraDust, 'I', Items.iron_ingot, 'O', Blocks.obsidian, 'S', "stone", 's', ChromaItems.SHARD.getAnyMetaStack());
		this.addRecipe(new StructureFinderRecipe(is, sr));

		is = ChromaItems.MOBSONAR.getStackOf();
		sr = new ShapedOreRecipe(is, "sas", "aba", "sas", 'a', ChromaStacks.auraDust, 's', Items.stick, 'b', Items.diamond);
		this.addRecipe(new MobSonarRecipe(is, sr));

		if (ModList.THAUMCRAFT.isLoaded()) {
			is = ChromaTiles.FLUXMAKER.getCraftedProduct();
			sr = new ShapedOreRecipe(is, " g ", "asa", " g ", 'g', ChromaStacks.voidDust, 'a', ChromaStacks.auraDust, 's', ThaumItemHelper.ItemEntry.VOIDSEED.getItem());
			this.addRecipe(new FluxMakerRecipe(is, sr));
		}

		is = ChromaTiles.FUNCTIONRELAY.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " D ", "ACA", " D ", 'D', ChromaStacks.beaconDust, 'A', ChromaStacks.auraDust, 'C', Blocks.glowstone);
		this.addRecipe(new FunctionRelayRecipe(is, sr));

		this.addRecipe(new ChromaCrafterRecipe(ChromaTiles.CHROMACRAFTER.getCraftedProduct(), ChromaStacks.transformCoreHigh));

		for (int i = 0; i < BlockRepeaterLight.MODELS.length; i++) {
			this.addRecipe(new RepeaterLampRecipe(BlockRepeaterLight.MODELS[i]));
		}

		is = ChromaItems.CAVEPATHER.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "RAC", "BCA", "SBE", 'R', Items.redstone, 'E', Items.diamond, 'B', ChromaStacks.beaconDust, 'A', ChromaStacks.auraDust, 'S', Items.stick, 'C', Items.quartz);
		this.addRecipe(new CavePatherRecipe(is, sr));

		is = ChromaItems.SPLINEATTACK.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " sa", "sis", "bs ", 'i', Items.iron_ingot, 's', ChromaStacks.whiteShard, 'a', ChromaStacks.blueShard, 'b', ChromaStacks.pinkShard);
		this.addRecipe(new SplineAttackRecipe(is, sr));

		is = ChromaBlocks.FAKESKY.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "tGt", "gGg", "tGt", 'g', Blocks.glowstone, 'G', Blocks.glass, 't', ChromaStacks.beaconDust, 'B', ChromaStacks.beaconDust, 'A', ChromaStacks.auraDust, 'S', Items.stick, 'C', Items.quartz);
		this.addRecipe(new FakeSkyRecipe(is, sr));

		this.addRecipe(new MultiBuilderRecipe(ChromaTiles.MULTIBUILDER.getCraftedProduct(), ChromaStacks.elementUnit));

		this.addRecipe(new ExplosionShieldRecipe(ChromaTiles.EXPLOSIONSHIELD.getCraftedProduct(), ChromaStacks.crystalStar));

		this.addRecipe(new BottleneckFinderRecipe(ChromaItems.BOTTLENECK.getStackOf(), ChromaStacks.auraIngot));

		is = ChromaTiles.PROGRESSLINK.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "ada", "gCg", "CPC", 'P', ChromaBlocks.CRYSTAL.getStackOfMetadata(CrystalElement.PURPLE.ordinal()), 'C', ChromaBlocks.PYLONSTRUCT.getStackOf(), 'd', Items.diamond, 'a', ChromaStacks.auraDust, 'g', Items.gold_ingot);
		this.addRecipe(new ProgressLinkerRecipe(is, sr));

		if (ModList.BOTANIA.isLoaded())
			this.addRecipe(new ManaBoosterRecipe(ChromaTiles.MANABOOSTER.getCraftedProduct()));

		this.addRecipe(new NetworkOptimizerRecipe(ChromaTiles.OPTIMIZER.getCraftedProduct(), ChromaStacks.lumenCore));

		is = ChromaItems.SPAWNERBYPASS.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "ptq", "ada", "rvp", 'd', Items.diamond, 'v', ChromaStacks.voidDust, 'a', ChromaStacks.auraDust, 't', ChromaStacks.beaconDust, 'p', Items.ender_pearl, 'r', Items.redstone, 'l', ReikaItemHelper.lapisDye, 'q', Items.quartz);
		this.addRecipe(new SpawnerBypassRecipe(is, sr));

		//is = ChromaBlocks.SPAWNERCONTROL.getStackOf();
		//sr = ReikaRecipeHelper.getShapedRecipeFor(is, "fef", "ege", "rgr", 'f', ChromaStacks.bindingCrystal, 'g', Blocks.glowstone, 'r', Blocks.redstone_block, 'e', Items.emerald);
		//this.addRecipe(new SpawnerShutdownRecipe(is, sr));

		is = ChromaBlocks.TRAPFLOOR.getStackOf();
		sr = new ShapedOreRecipe(is, "bab", "svs", "bvb", 's', "slimeball", 'b', Blocks.cobblestone, 'a', ChromaStacks.auraDust, 'v', ChromaStacks.voidDust);
		this.addRecipe(new TrapFloorRecipe(is, 1, sr));
		sr = new ShapedOreRecipe(is, "bab", "svs", "bvb", 's', "slimeball", 'b', Blocks.stonebrick, 'a', ChromaStacks.auraDust, 'v', ChromaStacks.voidDust);
		this.addRecipe(new TrapFloorRecipe(is, 4, sr));
		sr = new ShapedOreRecipe(is, "bab", "svs", "bvb", 's', "slimeball", 'b', ChromaBlocks.STRUCTSHIELD.getStackOfMetadata(BlockType.STONE.ordinal()), 'a', ChromaStacks.auraDust, 'v', ChromaStacks.voidDust);
		this.addRecipe(new TrapFloorRecipe(is, 12, sr));

		this.addRecipe(new EnderEyeRecipe(ChromaItems.ENDEREYE.getCraftedProduct(3), new ItemStack(Items.emerald)));

		this.addRecipe(new RepeaterUpcraftRecipe(repeater));

		is = ChromaItems.LIGHTGUN.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " ba", "gGb", "sg ", 's', Items.stick, 'g', Items.redstone, 'G', Items.gold_ingot, 'a', ChromaStacks.auraDust, 'b', Blocks.obsidian);
		this.addRecipe(new LightGunRecipe(is, sr));

		is = ChromaItems.PROBE.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, " sg", "sGs", "gs ", 'G', Blocks.glass, 'g', Items.glowstone_dust, 's', ChromaStacks.blackShard);
		this.addRecipe(new ProbeRecipe(is, sr));

		is = ChromaBlocks.INJECTORAUX.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "cbc", "ama", "ccc", 'c', ChromaBlocks.PYLONSTRUCT.getStackOfMetadata(0), 'm', ChromaBlocks.PYLONSTRUCT.getStackOfMetadata(StoneTypes.MULTICHROMIC.ordinal()), 'b', ChromaStacks.bindingCrystal, 'a', ChromaStacks.spaceDust);
		this.addRecipe(new CastingInjectorFocusRecipe(is, sr, redgroup, greengroup, orangegroup, whitegroup, star));

		is = ChromaTiles.INJECTOR.getCraftedProduct();
		this.addRecipe(new CastingInjectorRecipe(is, ChromaStacks.elementUnit));

		is = ChromaTiles.HOVERPAD.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "cac", "dmd", "crc", 'r', Blocks.redstone_torch, 'd', ChromaStacks.enderDust, 'a', ChromaStacks.auraDust, 'c', ChromaBlocks.STRUCTSHIELD.getStackOfMetadata(BlockType.STONE.ordinal()), 'm', Items.diamond);
		HoverPadRecipe pad = new HoverPadRecipe(is, sr);
		this.addRecipe(pad);

		is = ChromaBlocks.PAD.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "cac", "ama", "clc", 'l', Blocks.redstone_lamp, 'a', ChromaStacks.auraDust, 'c', ChromaBlocks.STRUCTSHIELD.getStackOfMetadata(BlockType.STONE.ordinal()), 'm', Items.ender_pearl);
		this.addRecipe(new HoverPadBlockRecipe(is, sr, pad));

		this.addRecipe(new HoverPadLampRecipe(ChromaBlocks.PAD));

		if (ModList.TINKERER.isLoaded()) {
			is = ChromaTiles.SMELTERYDISTRIBUTOR.getCraftedProduct();
			sr = ReikaRecipeHelper.getShapedRecipeFor(is, "fif", "aGa", "iei", 'f', ChromaStacks.firaxite, 'e', ChromaStacks.beaconDust, 'a', ChromaStacks.auraDust, 'i', Items.blaze_powder, 'G', Blocks.glowstone);
			this.addRecipe(new SmelteryDistributorRecipe(is, sr));
		}

		if (ModList.VOIDMONSTER.isLoaded()) {
			is = ChromaTiles.DEATHFOG.getCraftedProduct();
			sr = ReikaRecipeHelper.getShapedRecipeFor(is, "cac", "epe", "ccc", 'p', Items.bucket, 'e', ChromaStacks.voidmonsterEssence, 'a', ChromaStacks.auraDust, 'c', Blocks.cobblestone);
			this.addRecipe(new DeathFogRecipe(is, sr));

			is = ChromaTiles.VOIDTRAP.getCraftedProduct();
			this.addRecipe(new VoidTrapRecipe(is, ChromaStacks.voidCoreHigh));
		}

		is = ChromaTiles.LAUNCHPAD.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "sas", "aga", "rar", 's', ChromaStacks.limeShard, 'r', Items.redstone, 'a', ChromaStacks.auraDust, 'g', ChromaBlocks.PYLONSTRUCT.getStackOfMetadata(StoneTypes.ENGRAVED.ordinal()));
		this.addRecipe(new LaunchPadRecipe(is, sr));

		this.addRecipe(new TeleGateLockRecipe(ChromaItems.TELEGATELOCK.getStackOf(), ChromaStacks.resocrystal));

		is = ChromaItems.ENDERBUCKET.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "eae", "sbs", "gsg", 'g', Items.gold_ingot, 'e', ChromaStacks.enderDust, 'b', Items.bucket, 's', ChromaStacks.spaceDust, 'a', ChromaStacks.auraDust);
		this.addRecipe(new EnderBucketRecipe(is, sr));

		is = ChromaTiles.TOOLSTORAGE.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "qgq", "aca", "wsw", 'a', ModList.APPENG.isLoaded() ? AppEngHandler.getInstance().get16KStorage() : ReikaTreeHelper.OAK.getLog(), 'q', Items.quartz, 'g', Blocks.glowstone, 'c', ChromaBlocks.LOOTCHEST.getBlockInstance(), 'w', ReikaTreeHelper.OAK.getLog(), 's', Blocks.stone);
		this.addRecipe(new ToolStorageRecipe(is, sr));

		this.addRecipe(new WideCollectorRecipe(ChromaItems.WIDECOLLECTOR.getStackOf(), ChromaTiles.FUNCTIONRELAY.getCraftedProduct()));

		this.addRecipe(new NetworkTransportRecipe(ChromaTiles.NETWORKITEM.getCraftedProduct(), ChromaStacks.transformCore));

		is = ChromaTiles.ITEMRIFT.getCraftedProduct();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "rtr", "tlt", "cqc", 'c', ChromaStacks.chromaDust, 'q', ReikaItemHelper.quartzPillar.asItemStack(), 'r', Items.redstone, 'l', Blocks.redstone_lamp, 't', ChromaStacks.beaconDust);
		this.addRecipe(new ItemRiftRecipe(is, sr));

		is = ChromaItems.NETHERKEY.getStackOf();
		sr = ReikaRecipeHelper.getShapedRecipeFor(is, "bab", "aca", "bab", 'c', ReikaItemHelper.quartz.asItemStack(), 'a', Blocks.obsidian, 'b', ChromaStacks.auraDust);
		this.addRecipe(new NetherKeyRecipe(is, sr));

		this.addRecipe(new RecipeCacheRecipe(ChromaItems.RECIPECACHE.getStackOf(), new ItemStack(Blocks.obsidian)));

		this.addRecipe(new EtherealPendantRecipe(ChromaItems.ETHERPENDANT.getStackOf(), ChromaStacks.voidCore));

		this.addSpecialRecipes();
	}

	private void addSpecialRecipes() {
		this.addRecipe(new RepeaterTurboRecipe(ChromaTiles.REPEATER, 5000));
		this.addRecipe(new RepeaterTurboRecipe(ChromaTiles.COMPOUND, 12500));
		this.addRecipe(new RepeaterTurboRecipe(ChromaTiles.BROADCAST, 30000));

		this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.PROBE.getStackOf(), 1));
		this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.NETHERKEY.getStackOf(), 3));
		this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.SPAWNERBYPASS.getStackOf(), 2));
		this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.OREPICK.getStackOf(), 4));
		this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.STRUCTUREFINDER.getStackOf(), 1));
		this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.PURIFY.getStackOf(), 10));
		for (int i = 0; i < 16; i++)
			this.addRecipe(new ChargedItemPlayerBufferConnectionRecipe(ChromaItems.PENDANT3.getStackOfMetadata(i), 15));

		this.addRecipe(new TankExpandingRecipe());
	}

	public void addPostLoadRecipes() {
		if (ModList.THAUMCRAFT.isLoaded()) {
			ItemStack is = ReikaItemHelper.getSizedItemStack(ThaumItemHelper.BlockEntry.ANCIENTROCK.getItem(), 32);
			IRecipe sr = new ShapedOreRecipe(is, "SdS", "dOd", "SdS", 'S', "stone", 'd', ChromaStacks.auraDust, 'O', Blocks.obsidian);
			this.addModRecipe(new DecoCastingRecipe(is, sr));
			is = ReikaItemHelper.getSizedItemStack(ThaumItemHelper.BlockEntry.ANCIENTROCK.getItem(), 32);
			sr = new ShapedOreRecipe(is, "SvS", "dOd", "SvS", 'S', "stone", 'v', ChromaStacks.voidDust, 'd', ChromaStacks.spaceDust, 'O', Blocks.obsidian);
			this.addModRecipe(new DecoCastingRecipe(is, sr));

			is = ThaumItemHelper.ItemEntry.ZOMBIEBRAIN.getItem();
			sr = ReikaRecipeHelper.getShapedRecipeFor(is, "lll", "lbl", "lll", 'b', ThaumItemHelper.ItemEntry.DEFUNCTBRAIN.getItem(), 'l', ChromaStacks.lifegel);
			this.addModRecipe(new CastingRecipe(is, sr));
		}

		if (ModList.FORESTRY.isLoaded()) {
			ItemStack is = new ItemStack(CrystalBackpack.instance.getItem1());
			if (is != null && is.getItem() != null) { //disabled
				IRecipe sr = ReikaRecipeHelper.getShapedRecipeFor(is, "SWS", "cCc", "SWS", 'S', Items.string, 'C', Blocks.chest, 'W', Blocks.wool, 'c', ChromaItems.SHARD.getAnyMetaStack());
				this.addModRecipe(new CastingRecipe(is, sr));
			}

			ItemStack is2 = new ItemStack(CrystalBackpack.instance.getItem2());
			if (is2 != null && is2.getItem() != null) {
				IRecipe sr = ReikaRecipeHelper.getShapedRecipeFor(is2, "WDW", "WCW", "WWW", 'D', Items.diamond, 'C', is, 'W', ForestryHandler.CraftingMaterials.WOVENSILK.getItem());
				this.addModRecipe(new CastingRecipe(is2, sr));
			}

			ItemStack frame = ChromaItems.BEEFRAME.getStackOf();
			IRecipe sr = ReikaRecipeHelper.getShapedRecipeFor(frame, "agb", "gfg", "bga", 'a', ChromaStacks.auraDust, 'b', ChromaStacks.chromaDust, 'g', ChromaStacks.lightBlueShard, 'f', ForestryHandler.ItemEntry.IMPREGFRAME.getItem());
			this.addRecipe(new BeeFrameRecipe(frame, sr));

			Block ctr = GameRegistry.findBlock("MagicBees", "magicApiary");
			if (ctr == null)
				ctr = GameRegistry.findBlock(ModList.FORESTRY.modLabel, "alveary");
			this.addRecipe(new LumenAlvearyRecipe(ChromaTiles.ALVEARY.getCraftedProduct(), new ItemStack(ctr)));

			this.addModRecipe(new BeeConversionRecipe());

			is = ChromaTiles.BEESTORAGE.getCraftedProduct();
			Object g2 = ReikaItemHelper.lookupItem("gendustry:BeeReceptacle");
			Object g = ReikaItemHelper.lookupItem("gendustry:GeneticsProcessor");
			if (g == null)
				g = ForestryHandler.CraftingMaterials.WOVENSILK.getItem();
			if (g2 == null)
				g2 = ChromaStacks.bindingCrystal;
			if (g instanceof Item)
				g = new ItemStack((Item)g, 1, OreDictionary.WILDCARD_VALUE);
			if (g2 instanceof Item)
				g2 = new ItemStack((Item)g2, 1, OreDictionary.WILDCARD_VALUE);
			sr = new ShapedOreRecipe(is, "aga", "pcp", "qwq", 'q', new ItemStack(Blocks.quartz_block), 'p', ModList.APPENG.isLoaded() ? AppEngHandler.getInstance().get64KStorage() : "ingotBronze", 'a', g2, 'g', g, 'c', ChromaTiles.TOOLSTORAGE.getCraftedProduct(), 'w', "ingotBronze");
			this.addRecipe(new BeeStorageRecipe(is, sr));
		}

		if (ModList.TINKERER.isLoaded()) {
			if (TinkerBlockHandler.Pulses.TOOLS.isLoaded()) {
				for (int i = 0; i < ToolParts.partList.length; i++) {
					this.addRecipe(new TinkerToolPartRecipe(ToolParts.partList[i]));
				}
			}
			if (TinkerBlockHandler.Pulses.WEAPONS.isLoaded()) {
				for (int i = 0; i < WeaponParts.partList.length; i++) {
					this.addRecipe(new TinkerToolPartRecipe(WeaponParts.partList[i]));
				}
			}
		}

		if (ModList.EXTRAUTILS.isLoaded() && DivisionSigilActivationRecipe.isLoadable()) {
			this.addModRecipe(new DivisionSigilActivationRecipe());
		}

		if (ModList.BUILDCRAFT.isLoaded()) {
			this.addLandmarkRecipe();
		}

		for (Object o : Item.itemRegistry.getKeys()) {
			String s = (String)o;
			Item i = (Item)Item.itemRegistry.getObject(s);
			if (i instanceof ItemArmor) {
				if (((ItemArmor)i).armorType == 3) {
					this.addRecipe(new DoubleJumpRecipe(i));
				}
			}
		}
	}

	@ModDependent(ModList.BUILDCRAFT)
	private void addLandmarkRecipe() {
		ItemStack is = ChromaTiles.LANDMARK.getCraftedProduct();
		IRecipe sr = ReikaRecipeHelper.getShapelessRecipeFor(is, ChromaStacks.beaconDust, ChromaStacks.beaconDust, new ItemStack(Items.redstone), ChromaStacks.auraDust, new ItemStack(BuildCraftCore.markerBlock), new ItemStack(Items.quartz));
		this.addRecipe(new FloatingLandmarkRecipe(is, sr));
	}

	private CastingRecipe addModRecipe(CastingRecipe r) {
		this.addRecipe(r);
		moddedItemRecipes.add(r);
		Collections.sort(moddedItemRecipes, (r1, r2) -> ReikaItemHelper.comparator.compare(r1.getOutput(), r2.getOutput()));
		return r;
	}

	private CastingRecipe addRecipe(CastingRecipe r) {
		r.validate();

		OneWayList<CastingRecipe> li = recipes.get(r.type);
		if (li == null) {
			li = new OneWayList();
			recipes.put(r.type, li);
		}
		li.add(r);

		recipeIDs.put(maxID, r);
		String id = r.getIDString();
		CastingRecipe conflict = recipeStringIDs.get(id);
		if (conflict != null && !APIrecipes.contains(conflict) && !conflict.equals(r)) {
			throw new RuntimeException("Recipe "+r+" has a recipe ID which conflicts with "+conflict);
		}
		recipeStringIDs.put(id, r);
		//ChromatiCraft.logger.log("Registering recipe "+r+" with IDs "+maxID+" & "+id);
		maxID++;
		//ChromaResearchManager.instance.register(r);

		if (r instanceof PylonCastingRecipe) {
			ElementTagCompound tag = ((PylonCastingRecipe)r).getRequiredAura();
			maxEnergyCost = Math.max(maxEnergyCost, tag.getMaximumValue());
			maxTotalEnergyCost = Math.max(maxTotalEnergyCost, tag.getTotalEnergy());
		}

		return r;
	}

	public void addModdedRecipe(CastingRecipe r) {
		try {
			if (ChromaAux.verifyCustomRecipeOutputItem(r.getOutput(), false))
				this.addCustomRecipe(r);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private CastingRecipe addCustomRecipe(CastingRecipe r) {
		APIrecipes.add(r);
		Collections.sort(APIrecipes, (r1, r2) -> ReikaItemHelper.comparator.compare(r1.getOutput(), r2.getOutput()));
		return this.addRecipe(r);
	}

	public List<CastingRecipe> getAllAPIRecipes() {
		return Collections.unmodifiableList(APIrecipes);
	}

	public List<CastingRecipe> getAllModdedItemRecipes() {
		return Collections.unmodifiableList(moddedItemRecipes);
	}

	public CastingRecipe getRecipe(TileEntityCastingTable table, ArrayList<RecipeType> type) {
		ArrayList<CastingRecipe> li = new ArrayList();
		Collections.sort(type);
		Collections.reverse(type);
		for (RecipeType rt : type) {
			ArrayList<CastingRecipe> list = recipes.get(rt);
			if (list == null)
				continue;
			for (CastingRecipe r : list) {
				if (r.match(table))
					return r;
			}
		}
		return null;
	}

	public ArrayList<CastingRecipe> getAllRecipesMaking(ItemStack result) {
		ArrayList<CastingRecipe> li = new ArrayList();
		for (ArrayList<CastingRecipe> ir : recipes.values()) {
			for (CastingRecipe r : ir) {
				if (!(r instanceof EnchantmentRecipe)) {
					if (ReikaItemHelper.matchStacks(result, r.getOutput()) && (result.stackTagCompound == null || ItemStack.areItemStackTagsEqual(result, r.getOutput())))
						li.add(r);
				}
			}
		}
		return li;
	}

	public ArrayList<CastingRecipe> getAllRecipesUsing(ItemStack ingredient) {
		ArrayList<CastingRecipe> li = new ArrayList();
		for (RecipeType type : recipes.keySet()) {
			ArrayList<CastingRecipe> ir = recipes.get(type);
			if (ir != null) {
				for (CastingRecipe r : ir) {
					if (r.usesItem(ingredient))
						li.add(r);
				}
			}
		}
		return li;
	}

	public Collection<EnchantmentRecipe> getAllEnchantingRecipes() {
		ArrayList<EnchantmentRecipe> li = new ArrayList();
		for (RecipeType type : recipes.keySet()) {
			ArrayList<CastingRecipe> ir = recipes.get(type);
			if (ir != null) {
				for (CastingRecipe r : ir) {
					if (r instanceof EnchantmentRecipe)
						li.add((EnchantmentRecipe)r);
				}
			}
		}
		return li;
	}

	public static boolean playerHasCrafted(EntityPlayer ep, RecipeType type) {
		NBTTagCompound nbt = ChromaResearchManager.instance.getRootNBTTag(ep);
		NBTTagCompound cast = nbt.getCompoundTag("castingprog");
		return cast.getBoolean(type.name().toLowerCase(Locale.ENGLISH));
	}

	public static void setPlayerHasCrafted(EntityPlayer ep, RecipeType type) {
		NBTTagCompound nbt = ChromaResearchManager.instance.getRootNBTTag(ep);
		NBTTagCompound cast = nbt.getCompoundTag("castingprog");
		cast.setBoolean(type.name().toLowerCase(Locale.ENGLISH), true);
		nbt.setTag("castingprog", cast);
		ChromaResearchManager.instance.checkForUpgrade(ep);
	}

	/** DO NOT USE FOR CLIENT/SERVER COMMS, AS LIST ORDERING MAY BE DIFFERENT! */
	public CastingRecipe getRecipeByID(int id) {
		return recipeIDs.get(id);
	}

	/** DO NOT USE FOR CLIENT/SERVER COMMS, AS LIST ORDERING MAY BE DIFFERENT! */
	public int getIDForRecipe(CastingRecipe cr) {
		return recipeIDs.inverse().get(cr);
	}

	public CastingRecipe getRecipeByStringID(String id) {
		return recipeStringIDs.get(id);
	}

	public String getStringIDForRecipe(CastingRecipe cr) {
		return recipeStringIDs.inverse().get(cr);
	}

	public Collection<CastingRecipe> getAllRecipes() {
		ArrayList<CastingRecipe> li = new ArrayList();
		for (OneWayList oli : recipes.values()) {
			li.addAll(oli);
		}
		return li;
	}

	public void reload() {
		ChromatiCraft.logger.log("Reloading casting recipes.");
		recipes.clear();
		moddedItemRecipes.clear();
		recipeIDs.clear();
		recipeStringIDs.clear();

		DoubleJumpRecipe.clearCache();

		maxID = 0;
		maxEnergyCost = 0;
		maxTotalEnergyCost = 0;

		this.loadRecipes();
		this.addPostLoadRecipes();

		for (CastingRecipe c : APIrecipes) {
			this.addRecipe(c);
		}

		//this.loadCustomRecipeFiles();

		ChromaResearch.loadPostCache();
		MinecraftForge.EVENT_BUS.post(new CastingRecipesReloadEvent());

		ChromatiCraft.logger.log("Finished reloading casting recipes.");
	}

	public int getMaxRecipeEnergyCost() {
		return maxEnergyCost;
	}

	public int getMaxRecipeTotalEnergyCost() {
		return maxTotalEnergyCost;
	}

	public final void loadCustomRecipeFiles() {
		CustomRecipeList crl = new CustomRecipeList(ChromatiCraft.instance, "castingtable");
		if (crl.load()) {
			for (LuaBlock lb : crl.getEntries()) {
				Exception e = null;
				boolean flag = false;
				try {
					CastingRecipe cr = this.addCustomRecipe(lb, crl);
					flag = cr != null;
					if (flag) {
						LuaBlock c = lb.getChild("effectHook");
						if (c != null) {
							cr.effectCallback = ConfigRecipe.constructFXFromLuaBlock(c);
						}
					}
				}
				catch (Exception ex) {
					e = ex;
					flag = false;
				}
				if (flag) {
					ChromatiCraft.logger.log("Loaded custom casting recipe '"+lb.getString("type")+"'");
				}
				else {
					ChromatiCraft.logger.logError("Could not load custom casting recipe '"+lb.getString("type")+"'");
					if (e != null)
						e.printStackTrace();
				}
			}
		}
		else {/*
			crl.createFolders();
			for (Collection<CastingRecipe> c : recipes.values())
				crl.addToExample(createLuaBlock(c.iterator().next()));
			crl.createExampleFile();*/
		}
	}

	private CastingRecipe addCustomRecipe(LuaBlock lb, CustomRecipeList crl) throws Exception {
		ItemStack out = crl.parseItemString(lb.getString("output"), lb.getChild("output_nbt"), false);
		ChromaAux.verifyCustomRecipeOutputItem(out, true);
		String lvl = lb.getString("level");
		int duration = lb.containsKey("duration") ? lb.getInt("duration") : -1;
		int typical = lb.containsKey("typical_crafted_amount") ? lb.getInt("typical_crafted_amount") : 1;
		ItemStack leftover = lb.containsKey("central_leftover") ? crl.parseItemString(lb.getString("central_leftover"), lb.getChild("central_leftover_nbt"), true) : null;
		float autocost = lb.containsKey("automation_cost_factor") ? (float)lb.getDouble("automation_cost_factor") : 1;
		float xp = lb.containsKey("experience_factor") ? MathHelper.clamp_float((float)lb.getDouble("experience_factor"), 0, 1) : 1;
		boolean stack = true;
		boolean setStack = false;
		if (lb.containsKey("can_be_stacked")) {
			stack = lb.getBoolean("can_be_stacked");
			setStack = true;
		}
		Collection<ProgressStage> progress = new HashSet();
		if (lb.hasChild("progress")) {
			for (String s : lb.getChild("progress").getDataValues()) {
				progress.add(ProgressStage.valueOf(s.toUpperCase(Locale.ENGLISH)));
			}
		}

		HashMap<Coordinate, CrystalElement> runes = new HashMap();
		if (lb.hasChild("runes")) {
			for (LuaBlock entry : lb.getChild("runes").getChildren()) {
				Coordinate c = new Coordinate(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
				CrystalElement e = CrystalElement.valueOf(entry.getString("color").toUpperCase(Locale.ENGLISH));
				runes.put(c, e);
			}
		}

		HashMap<CrystalElement, Integer> energy = new HashMap();
		if (lb.hasChild("energy")) {
			for (LuaBlock entry : lb.getChild("energy").getChildren()) {
				CrystalElement e = CrystalElement.valueOf(entry.getString("color").toUpperCase(Locale.ENGLISH));
				int amt = entry.getInt("amount");
				energy.put(e, amt);
			}
		}

		if (lvl.equals("basic") || lvl.equals("rune")) {
			IRecipe ir = crl.parseCraftingRecipe(lb.getChild("recipe"), out);
			if (lvl.equals("rune")) {
				return this.addCustomRecipe(new ConfigRecipe.Rune(ir, duration, typical, leftover, autocost, xp, runes, progress));
			}
			else {
				return this.addCustomRecipe(new ConfigRecipe.Basic(ir, duration, typical, leftover, autocost, xp, progress));
			}
		}
		else if (lvl.equals("multi") || lvl.equals("pylon")) {
			LuaBlock recipe = lb.getChild("recipe");
			ItemStack center = crl.parseItemString(recipe.getString("center"), recipe.getChild("center_nbt"), false);
			HashMap<Coordinate, Object> items = new HashMap();
			for (LuaBlock entry : recipe.getChild("items").getChildren()) {
				Coordinate c = new Coordinate(entry.getInt("x"), 0, entry.getInt("z"));
				Object item = crl.parseObjectString(entry.getString("item"));
				items.put(c, item);
			}

			if (lvl.equals("pylon")) {
				if (!setStack)
					stack = false;
				return this.addCustomRecipe(new ConfigRecipe.Pylon(out, center, duration, typical, leftover, autocost, xp, runes, items, energy, progress));
			}
			else {
				return this.addCustomRecipe(new ConfigRecipe.Multi(out, center, duration, typical, leftover, autocost, xp, runes, items, progress));
			}
		}
		else {
			throw new IllegalArgumentException("Invalid recipe tier '"+lvl+"'!");
		}
	}

	@Override
	public APICastingRecipe addCastingRecipe(IRecipe ir) {
		CastingRecipe cr = new CastingRecipe(ir.getRecipeOutput(), ir);
		this.addModdedRecipe(cr);
		return cr;
	}

	@Override
	public RuneTempleRecipe addTempleCastingRecipe(IRecipe ir, Map<List<Integer>, CrystalElementProxy> runes) {
		TempleCastingRecipe cr = new TempleCastingRecipe(ir.getRecipeOutput(), ir);
		for (Entry<List<Integer>, CrystalElementProxy> e : runes.entrySet()) {
			List<Integer> li = e.getKey();
			cr.addRune(e.getValue(), li.get(0), li.get(1), li.get(2));
		}
		this.addModdedRecipe(cr);
		return cr;
	}

	@Override
	public MultiRecipe addMultiBlockCastingRecipe(ItemStack out, ItemStack ctr, Map<List<Integer>, CrystalElementProxy> runes, Map<List<Integer>, ItemStack> items) {
		MultiBlockCastingRecipe cr = new MultiBlockCastingRecipe(out, ctr);
		if (runes != null) {
			for (Entry<List<Integer>, CrystalElementProxy> e : runes.entrySet()) {
				List<Integer> li = e.getKey();
				cr.addRune(e.getValue(), li.get(0), li.get(1), li.get(2));
			}
		}
		for (Entry<List<Integer>, ItemStack> e : items.entrySet()) {
			List<Integer> li = e.getKey();
			cr.addAuxItem(e.getValue(), li.get(0), li.get(1));
		}
		this.addModdedRecipe(cr);
		return cr;
	}

	@Override
	public LumenRecipe addPylonCastingRecipe(ItemStack out, ItemStack ctr, Map<List<Integer>, CrystalElementProxy> runes, Map<List<Integer>, ItemStack> items, Map<CrystalElementProxy, Integer> energy) {
		PylonCastingRecipe cr = new PylonCastingRecipe(out, ctr);
		if (runes != null) {
			for (Entry<List<Integer>, CrystalElementProxy> e : runes.entrySet()) {
				List<Integer> li = e.getKey();
				cr.addRune(e.getValue(), li.get(0), li.get(1), li.get(2));
			}
		}
		for (Entry<List<Integer>, ItemStack> e : items.entrySet()) {
			List<Integer> li = e.getKey();
			cr.addAuxItem(e.getValue(), li.get(0), li.get(1));
		}
		for (Entry<CrystalElementProxy, Integer> e : energy.entrySet()) {
			cr.addAuraRequirement(e.getKey(), e.getValue());
		}
		this.addModdedRecipe(cr);
		return cr;
	}

}
