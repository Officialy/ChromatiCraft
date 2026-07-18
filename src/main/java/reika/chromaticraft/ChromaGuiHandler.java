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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import reika.chromaticraft.auxiliary.interfaces.CastingAutomationBlock;
import reika.chromaticraft.auxiliary.interfaces.ItemOnRightClick;
import reika.chromaticraft.base.ChromaBookGui;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.block.blockendertnt.TileEntityEnderTNT;
import reika.chromaticraft.block.blockheatlamp.TileEntityHeatLamp;
import reika.chromaticraft.block.blockrouternode.RouterFilter;
import reika.chromaticraft.block.decoration.blockrangedlamp.TileEntityRangedLamp;
import reika.chromaticraft.block.dimension.structure.blockstructuredatastorage.TileEntityStructurePassword;
import reika.chromaticraft.block.relay.blockrelayfilter.TileEntityRelayFilter;
import reika.chromaticraft.container.ContainerAuraPouch;
import reika.chromaticraft.container.ContainerAutoEnchanter;
import reika.chromaticraft.container.ContainerBookEmpties;
import reika.chromaticraft.container.ContainerBookPages;
import reika.chromaticraft.container.ContainerBulkMover;
import reika.chromaticraft.container.ContainerCastingAuto;
import reika.chromaticraft.container.ContainerCastingTable;
import reika.chromaticraft.container.ContainerCrystalBrewer;
import reika.chromaticraft.container.ContainerCrystalCharger;
import reika.chromaticraft.container.ContainerCrystalFurnace;
import reika.chromaticraft.container.ContainerCrystalTank;
import reika.chromaticraft.container.ContainerEnchantDecomposer;
import reika.chromaticraft.container.ContainerFluidRelay;
import reika.chromaticraft.container.ContainerFragmentSelect;
import reika.chromaticraft.container.ContainerInventoryTicker;
import reika.chromaticraft.container.ContainerItemBurner;
import reika.chromaticraft.container.ContainerItemCollector;
import reika.chromaticraft.container.ContainerItemFabricator;
import reika.chromaticraft.container.ContainerItemInserter;
import reika.chromaticraft.container.ContainerItemWithFilter;
import reika.chromaticraft.container.ContainerMiner;
import reika.chromaticraft.container.ContainerNetworkItemTransporter;
import reika.chromaticraft.container.ContainerRouterFilter;
import reika.chromaticraft.container.ContainerSpawnerProgrammer;
import reika.chromaticraft.container.ContainerStructurePassword;
import reika.chromaticraft.container.ContainerTelePump;
import reika.chromaticraft.gui.GuiAbilitySelect;
import reika.chromaticraft.gui.GuiAuraPouch;
import reika.chromaticraft.gui.GuiBulkMover;
import reika.chromaticraft.gui.GuiEnderBucket;
import reika.chromaticraft.gui.GuiFlightWand;
import reika.chromaticraft.gui.GuiFragmentSelect;
import reika.chromaticraft.gui.GuiItemBurner;
import reika.chromaticraft.gui.GuiItemWithFilter;
import reika.chromaticraft.gui.GuiLoreKeyAssembly;
import reika.chromaticraft.gui.GuiOneSlot;
import reika.chromaticraft.gui.GuiTeleportAbility;
import reika.chromaticraft.gui.GuiTransitionWand;
import reika.chromaticraft.gui.book.GuiAbilityDesc;
import reika.chromaticraft.gui.book.GuiAdjacencyDescription;
import reika.chromaticraft.gui.book.GuiBasicInfo;
import reika.chromaticraft.gui.book.GuiBookEmpties;
import reika.chromaticraft.gui.book.GuiBookPages;
import reika.chromaticraft.gui.book.GuiCastingRecipe;
import reika.chromaticraft.gui.book.GuiCraftableDesc;
import reika.chromaticraft.gui.book.GuiCraftingRecipe;
import reika.chromaticraft.gui.book.GuiFragmentRecovery;
import reika.chromaticraft.gui.book.GuiMachineDescription;
import reika.chromaticraft.gui.book.GuiNavigation;
import reika.chromaticraft.gui.book.GuiNotes;
import reika.chromaticraft.gui.book.GuiPackChanges;
import reika.chromaticraft.gui.book.GuiPoolRecipe;
import reika.chromaticraft.gui.book.GuiProgressByLevel;
import reika.chromaticraft.gui.book.GuiProgressTree;
import reika.chromaticraft.gui.book.GuiRitual;
import reika.chromaticraft.gui.book.GuiStructure;
import reika.chromaticraft.gui.book.GuiToolDescription;
import reika.chromaticraft.gui.tile.GuiBiomeChanger;
import reika.chromaticraft.gui.tile.GuiCastingAuto;
import reika.chromaticraft.gui.tile.GuiCrystalMusic;
import reika.chromaticraft.gui.tile.GuiCrystalTank;
import reika.chromaticraft.gui.tile.GuiEnderTNT;
import reika.chromaticraft.gui.tile.GuiFluidRelay;
import reika.chromaticraft.gui.tile.GuiHeatLamp;
import reika.chromaticraft.gui.tile.GuiLampController;
import reika.chromaticraft.gui.tile.GuiLumenAlveary;
import reika.chromaticraft.gui.tile.GuiParticleSpawner;
import reika.chromaticraft.gui.tile.GuiRangedLamp;
import reika.chromaticraft.gui.tile.GuiRelayFilter;
import reika.chromaticraft.gui.tile.GuiRitualTable;
import reika.chromaticraft.gui.tile.GuiRouterFilter;
import reika.chromaticraft.gui.tile.GuiTeleportGate;
import reika.chromaticraft.gui.tile.inventory.GuiAutoEnchanter;
import reika.chromaticraft.gui.tile.inventory.GuiCastingTable;
import reika.chromaticraft.gui.tile.inventory.GuiCrystalBrewer;
import reika.chromaticraft.gui.tile.inventory.GuiCrystalCharger;
import reika.chromaticraft.gui.tile.inventory.GuiCrystalFurnace;
import reika.chromaticraft.gui.tile.inventory.GuiEnchantDecomposer;
import reika.chromaticraft.gui.tile.inventory.GuiInventoryTicker;
import reika.chromaticraft.gui.tile.inventory.GuiItemCollector;
import reika.chromaticraft.gui.tile.inventory.GuiItemFabricator;
import reika.chromaticraft.gui.tile.inventory.GuiItemInserter;
import reika.chromaticraft.gui.tile.inventory.GuiMiner;
import reika.chromaticraft.gui.tile.inventory.GuiNetworkItemTransporter;
import reika.chromaticraft.gui.tile.inventory.GuiSpawnerProgrammer;
import reika.chromaticraft.gui.tile.inventory.GuiStructurePassword;
import reika.chromaticraft.gui.tile.inventory.GuiTelePump;
import reika.chromaticraft.modinterface.ae.ContainerMEDistributor;
import reika.chromaticraft.modinterface.ae.ContainerPatternCache;
import reika.chromaticraft.modinterface.ae.ContainerRemoteTerminal;
import reika.chromaticraft.modinterface.ae.GuiMEDistributor;
import reika.chromaticraft.modinterface.ae.GuiPatternCache;
import reika.chromaticraft.modinterface.ae.GuiRemoteTerminal;
import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;
import reika.chromaticraft.modinterface.ae.TileEntityPatternCache;
import reika.chromaticraft.modinterface.bees.TileEntityLumenAlveary;
import reika.chromaticraft.modinterface.thaumcraft.ContainerFluxMaker;
import reika.chromaticraft.modinterface.thaumcraft.GuiAbilityFocus;
import reika.chromaticraft.modinterface.thaumcraft.GuiAspectFormer;
import reika.chromaticraft.modinterface.thaumcraft.GuiFluxMaker;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityAspectFormer;
import reika.chromaticraft.modinterface.thaumcraft.TileEntityFluxMaker;
import reika.chromaticraft.registry.ChromaGuis;
import reika.chromaticraft.registry.ChromaResearch;
import reika.chromaticraft.tileentity.TileEntityBiomePainter;
import reika.chromaticraft.tileentity.aoe.TileEntityItemCollector;
import reika.chromaticraft.tileentity.aoe.TileEntityItemInserter;
import reika.chromaticraft.tileentity.aoe.TileEntityLampController;
import reika.chromaticraft.tileentity.acquisition.TileEntityCollector;
import reika.chromaticraft.tileentity.acquisition.TileEntityItemFabricator;
import reika.chromaticraft.tileentity.acquisition.TileEntityMiner;
import reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.chromaticraft.tileentity.decoration.TileEntityCrystalMusic;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner;
import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter;
import reika.chromaticraft.tileentity.processing.TileEntityCrystalFurnace;
import reika.chromaticraft.tileentity.processing.TileEntityEnchantDecomposer;
import reika.chromaticraft.tileentity.processing.TileEntityInventoryTicker;
import reika.chromaticraft.tileentity.processing.TileEntitySpawnerReprogrammer;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;
import reika.chromaticraft.tileentity.storage.TileEntityCrystalTank;
import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;
import reika.chromaticraft.tileentity.transport.TileEntityNetworkItemTransporter;
import reika.chromaticraft.tileentity.transport.TileEntityRift;
import reika.chromaticraft.tileentity.transport.TileEntityTeleportGate;
import reika.dragonapi.base.CoreContainer;
import reika.dragonapi.base.OneSlotContainer;
import reika.dragonapi.base.OneSlotMachine;
import reika.dragonapi.interfaces.tileentity.GuiController;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ChromaGuiHandler implements IGuiHandler {

	public static final ChromaGuiHandler instance = new ChromaGuiHandler();

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		ChromaGuis gui = ChromaGuis.guiList[id];
		switch(gui) {
			case ITEMWITHFILTER:
				return new ContainerItemWithFilter(player, world);
			case BOOKPAGES:
				return new ContainerBookPages(player, x);
			case BOOKEMPTIES:
				return new ContainerBookEmpties(player);
			case TILE:
				TileEntity te = world.getTileEntity(x, y, z);

				if (te instanceof TileEntityAutoEnchanter)
					return new ContainerAutoEnchanter(player, (TileEntityAutoEnchanter)te);
				if (te instanceof TileEntityCollector)
					return null;//return new ContainerCollector(player, (TileEntityCollector)te);
				if (te instanceof TileEntitySpawnerReprogrammer)
					return new ContainerSpawnerProgrammer(player, (TileEntitySpawnerReprogrammer)te);
				if (te instanceof TileEntityCrystalBrewer)
					return new ContainerCrystalBrewer(player, (TileEntityCrystalBrewer)te);
				if (te instanceof TileEntityCastingTable)
					return new ContainerCastingTable(player, te);
				if (te instanceof TileEntityCrystalCharger)
					return new ContainerCrystalCharger(player, (TileEntityCrystalCharger)te);
				if (te instanceof TileEntityCrystalFurnace)
					return new ContainerCrystalFurnace(player, (TileEntityCrystalFurnace)te);
				if (te instanceof TileEntityItemCollector)
					return new ContainerItemCollector(player, (TileEntityItemCollector)te);
				if (te instanceof TileEntityItemFabricator)
					return new ContainerItemFabricator(player, (TileEntityItemFabricator)te);
				if (te instanceof TileEntityTeleportationPump)
					return new ContainerTelePump(player, (TileEntityTeleportationPump)te);
				if (te instanceof TileEntityMiner)
					return new ContainerMiner(player, (TileEntityMiner)te);
				if (te instanceof TileEntityCrystalTank)
					return new ContainerCrystalTank(player, (TileEntityCrystalTank)te);
				if (te instanceof TileEntityInventoryTicker)
					return new ContainerInventoryTicker(player, (TileEntityInventoryTicker)te);
				if (te instanceof CastingAutomationBlock)
					return new ContainerCastingAuto((CastingAutomationBlock)te, player);
				if (te instanceof TileEntityMEDistributor)
					return new ContainerMEDistributor(player, (TileEntityMEDistributor)te);
				if (te instanceof TileEntityPatternCache)
					return new ContainerPatternCache(player, (TileEntityPatternCache)te);
				if (te instanceof TileEntityItemInserter)
					return new ContainerItemInserter(player, (TileEntityItemInserter)te);
				if (te instanceof TileEntityEnchantDecomposer)
					return new ContainerEnchantDecomposer(player, (TileEntityEnchantDecomposer)te);
				if (te instanceof TileEntityFluidRelay)
					return new ContainerFluidRelay(player, (TileEntityFluidRelay)te);
				if (te instanceof RouterFilter)
					return new ContainerRouterFilter(player, (RouterFilter)te);
				if (te instanceof TileEntityFluxMaker)
					return new ContainerFluxMaker(player, (TileEntityFluxMaker)te);
				if (te instanceof TileEntityNetworkItemTransporter)
					return new ContainerNetworkItemTransporter(player, (TileEntityNetworkItemTransporter)te);

				if (te instanceof ItemOnRightClick)
					return null;
				if (te instanceof TileEntityRift)
					return null;
				if (te instanceof OneSlotMachine)
					return new OneSlotContainer(player, te);
				if (te instanceof GuiController)
					return new CoreContainer(player, te);

				//if (te instanceof IInventory && !(te instanceof InertIInv))
				//	return new ContainerBasicStorage(player, te);
				break;
			case AURAPOUCH:
				return new ContainerAuraPouch(player);
			case REMOTETERMINAL:
				return new ContainerRemoteTerminal(player);
			case BULKMOVER:
				return new ContainerBulkMover(player);
			case BURNERINV:
				return new ContainerItemBurner(player);
			case STRUCTUREPASS:
				return new ContainerStructurePassword(player, (TileEntityStructurePassword)world.getTileEntity(x, y, z));
			case FRAGSELECT:
				return new ContainerFragmentSelect(player);
			default:
				return null;
		}
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		ChromaGuis gui = ChromaGuis.guiList[id];
		if (ChromaBookGui.lastGui != null && gui.isLexiconGUI() && z == 0) {
			Object ret = ChromaBookGui.lastGui;
			ChromaBookGui.lastGui = null;
			return ret;
		}
		switch(gui) {
			case ITEMWITHFILTER:
				return new GuiItemWithFilter(player, world);
			case LOREKEY:
				return new GuiLoreKeyAssembly(player);
				//case LORE:
				//	return new GuiLore(player);
			case TILE:
				TileEntity te = world.getTileEntity(x, y, z);

				if (te instanceof TileEntityAutoEnchanter)
					return new GuiAutoEnchanter(player, (TileEntityAutoEnchanter)te);
				if (te instanceof TileEntityCollector)
					return null;//return new GuiCollector(player, (TileEntityCollector)te);
				if (te instanceof TileEntitySpawnerReprogrammer)
					return new GuiSpawnerProgrammer(player, (TileEntitySpawnerReprogrammer)te);
				if (te instanceof TileEntityCrystalBrewer)
					return new GuiCrystalBrewer(player, (TileEntityCrystalBrewer)te);
				if (te instanceof TileEntityCastingTable)
					return new GuiCastingTable(player, (TileEntityCastingTable)te);
				if (te instanceof TileEntityCrystalCharger)
					return new GuiCrystalCharger(player, (TileEntityCrystalCharger)te);
				if (te instanceof TileEntityRitualTable)
					return new GuiRitualTable(player, (TileEntityRitualTable)te);
				if (te instanceof TileEntityCrystalFurnace)
					return new GuiCrystalFurnace(player, (TileEntityCrystalFurnace)te);
				if (te instanceof TileEntityItemCollector)
					return new GuiItemCollector(player, (TileEntityItemCollector)te);
				if (te instanceof TileEntityItemFabricator)
					return new GuiItemFabricator(player, (TileEntityItemFabricator)te);
				if (te instanceof TileEntityTeleportationPump)
					return new GuiTelePump(player, (TileEntityTeleportationPump)te);
				if (te instanceof TileEntityAspectFormer)
					return new GuiAspectFormer(player, (TileEntityAspectFormer)te);
				if (te instanceof TileEntityMiner)
					return new GuiMiner(player, (TileEntityMiner)te);
				if (te instanceof TileEntityLampController)
					return new GuiLampController(player, (TileEntityLampController)te);
				if (te instanceof TileEntityRangedLamp)
					return new GuiRangedLamp(player, (TileEntityRangedLamp)te);
				if (te instanceof TileEntityCrystalTank)
					return new GuiCrystalTank(player, (TileEntityCrystalTank)te);
				if (te instanceof TileEntityEnderTNT)
					return new GuiEnderTNT(player, (TileEntityEnderTNT)te);
				if (te instanceof TileEntityInventoryTicker)
					return new GuiInventoryTicker(player, (TileEntityInventoryTicker)te);
				if (te instanceof TileEntityBiomePainter)
					return new GuiBiomeChanger(player, (TileEntityBiomePainter)te);
				if (te instanceof TileEntityHeatLamp)
					return new GuiHeatLamp((TileEntityHeatLamp)te, player);
				if (te instanceof CastingAutomationBlock)
					return new GuiCastingAuto((CastingAutomationBlock)te, player);
				if (te instanceof TileEntityMEDistributor)
					return new GuiMEDistributor(player, (TileEntityMEDistributor)te);
				if (te instanceof TileEntityCrystalMusic)
					return new GuiCrystalMusic(player, (TileEntityCrystalMusic)te);
				if (te instanceof TileEntityPatternCache)
					return new GuiPatternCache(player, (TileEntityPatternCache)te);
				if (te instanceof TileEntityItemInserter)
					return new GuiItemInserter(player, (TileEntityItemInserter)te);
				if (te instanceof TileEntityEnchantDecomposer)
					return new GuiEnchantDecomposer(player, (TileEntityEnchantDecomposer)te);
				if (te instanceof TileEntityParticleSpawner)
					return new GuiParticleSpawner(player, (TileEntityParticleSpawner)te);
				if (te instanceof TileEntityTeleportGate)
					return new GuiTeleportGate(player, (TileEntityTeleportGate)te);
				if (te instanceof TileEntityFluidRelay)
					return new GuiFluidRelay(player, (TileEntityFluidRelay)te);
				if (te instanceof TileEntityRelayFilter)
					return new GuiRelayFilter(player, (TileEntityRelayFilter)te);
				if (te instanceof RouterFilter)
					return new GuiRouterFilter(player, (RouterFilter)te);
				if (te instanceof TileEntityFluxMaker)
					return new GuiFluxMaker(player, (TileEntityFluxMaker)te);
				if (te instanceof TileEntityLumenAlveary)
					return new GuiLumenAlveary(player, (TileEntityLumenAlveary)te);
				if (te instanceof TileEntityNetworkItemTransporter)
					return new GuiNetworkItemTransporter(player, (TileEntityNetworkItemTransporter)te);

				if (te instanceof OneSlotMachine) {
					return new GuiOneSlot(player, (TileEntityChromaticBase)te);
				}/*
			if (te instanceof IInventory && !(te instanceof InertIInv))
				return new GuiBasicStorage(player, (RotaryCraftTileEntity)te);
				 */
				break;
			case ABILITY:
				return new GuiAbilitySelect(player);
			case ABILITYFOCUS:
				return new GuiAbilityFocus(player);
			case BOOKNAV:
				return new GuiNavigation(player);
			case BOOKPAGES:
				return new GuiBookPages(player, x);
			case BOOKEMPTIES:
				return new GuiBookEmpties(player);
			case MACHINEDESC: {
				ChromaResearch r = ChromaResearch.researchList[x];
				return r == ChromaResearch.ACCEL ? new GuiAdjacencyDescription(player) : new GuiMachineDescription(player, r);
			}
			case TOOLDESC:
				return new GuiToolDescription(player, ChromaResearch.researchList[x]);
			case BASICDESC:
				return new GuiCraftableDesc(player, ChromaResearch.researchList[x]);
			case CRAFTING:
				return new GuiCraftingRecipe(player, ChromaResearch.researchList[x].getVanillaRecipes(), y);
			case RECIPE:
				return new GuiCastingRecipe(player, ChromaResearch.researchList[x].getCraftingRecipes(), y, z > 0);
			case ALLOYING:
				return new GuiPoolRecipe(player, y, z > 0);
			case RITUAL:
				return new GuiRitual(player, ChromaResearch.researchList[x].getAbility());
			case ABILITYDESC:
				return new GuiAbilityDesc(player, ChromaResearch.researchList[x]);
			case INFO: {
				ChromaResearch r = ChromaResearch.researchList[x];
				return r == ChromaResearch.PACKCHANGES ? new GuiPackChanges(player) : new GuiBasicInfo(player, r);
			}
			case STRUCTURE:
				return new GuiStructure(player, ChromaResearch.researchList[x]);
			case PROGRESS:
				return new GuiProgressTree(player);
			case PROGRESSBYTIER:
				return new GuiProgressByLevel(player);
			case REFRAGMENT:
				return new GuiFragmentRecovery(player);
			case NOTES:
				return new GuiNotes(player);
			case AURAPOUCH:
				return new GuiAuraPouch(player);
			case TRANSITION:
				return new GuiTransitionWand(player);
			case TELEPORT:
				return new GuiTeleportAbility(player);
			case REMOTETERMINAL:
				return new GuiRemoteTerminal(player);
			case BULKMOVER:
				return new GuiBulkMover(player);
			case HOVER:
				return new GuiFlightWand(player);
			case BURNERINV:
				return new GuiItemBurner(player);
			case STRUCTUREPASS:
				return new GuiStructurePassword(player, (TileEntityStructurePassword)world.getTileEntity(x, y, z));
			case ENDERBUCKET:
				return new GuiEnderBucket(player);
			case FRAGSELECT:
				return new GuiFragmentSelect(player);
		}
		return null;
	}
}
