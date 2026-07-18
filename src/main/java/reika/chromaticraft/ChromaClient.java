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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.renderer.entity.RenderFireball;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSlime;
import net.minecraft.client.renderer.tileentity.RenderEnderCrystal;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

import reika.chromaticraft.auxiliary.ability.AbilityHotkeys;
import reika.chromaticraft.auxiliary.render.ChromaRenderList;
import reika.chromaticraft.auxiliary.render.DonatorPylonRender;
import reika.chromaticraft.base.ChromaRenderBase;
import reika.chromaticraft.block.blockchromaportal.TileEntityCrystalPortal;
import reika.chromaticraft.block.blockpolycrystal.TilePolyCrystal;
import reika.chromaticraft.block.decoration.blockavolamp.TileEntityAvoLamp;
import reika.chromaticraft.block.decoration.blockcoloredaltar.TileEntityColoredAltar;
import reika.chromaticraft.block.dimension.blockdimensiondecotile.TileGlowingCracks;
import reika.chromaticraft.block.dimension.blockvoidrift.TileEntityVoidRift;
import reika.chromaticraft.block.dimension.structure.antfarm.blockantkey.AntKeyTile;
import reika.chromaticraft.block.dimension.structure.gravity.blockgravitytile.GravityTile;
import reika.chromaticraft.block.dimension.structure.laser.blocklasereffector.TargetTile;
import reika.chromaticraft.block.dimension.structure.pinball.blockpinballtile.TileBouncePad;
import reika.chromaticraft.block.dimension.structure.pistontape.blockpistoncontroller.TilePistonDisplay;
import reika.chromaticraft.block.dimension.structure.water.blockrotatinglock.TileEntityRotatingLock;
import reika.chromaticraft.block.worldgen.blocklootchest.TileEntityLootChest;
import reika.chromaticraft.block.worldgen.blockwarpnode.TileEntityWarpNode;
import reika.chromaticraft.entity.EntityAbilityFireball;
import reika.chromaticraft.entity.EntityAurora;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.chromaticraft.entity.EntityChainGunShot;
import reika.chromaticraft.entity.EntityChromaEnderCrystal;
import reika.chromaticraft.entity.EntityDeathFog;
import reika.chromaticraft.entity.EntityDimensionFlare;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.entity.EntityGluon;
import reika.chromaticraft.entity.EntityLaserPulse;
import reika.chromaticraft.entity.EntityLightShot;
import reika.chromaticraft.entity.EntityLumaBurst;
import reika.chromaticraft.entity.EntityMeteorShot;
import reika.chromaticraft.entity.EntityMonsterBait;
import reika.chromaticraft.entity.EntityNukerBall;
import reika.chromaticraft.entity.EntityOverloadingPylonShock;
import reika.chromaticraft.entity.EntityParticleCluster;
import reika.chromaticraft.entity.EntityPistonSpline;
import reika.chromaticraft.entity.EntitySplashGunShot;
import reika.chromaticraft.entity.EntityTNTPinball;
import reika.chromaticraft.entity.EntityThrownGem;
import reika.chromaticraft.entity.EntityTunnelNuker;
import reika.chromaticraft.entity.EntityVacuum;
import reika.chromaticraft.items.tools.itemdatacrystal.EntityDataCrystal;
import reika.chromaticraft.modinterface.EntityChromaManaBurst;
import reika.chromaticraft.models.ColorizableSlimeModel;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaISBRH;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaShaders;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.entity.RenderAurora;
import reika.chromaticraft.render.entity.RenderBallLightning;
import reika.chromaticraft.render.entity.RenderChainGunShot;
import reika.chromaticraft.render.entity.RenderChromaManaBurst;
import reika.chromaticraft.render.entity.RenderDataCrystal;
import reika.chromaticraft.render.entity.RenderDeathFog;
import reika.chromaticraft.render.entity.RenderDimensionFlare;
import reika.chromaticraft.render.entity.RenderGlowCloud;
import reika.chromaticraft.render.entity.RenderGluon;
import reika.chromaticraft.render.entity.RenderLaserPulse;
import reika.chromaticraft.render.entity.RenderLightShot;
import reika.chromaticraft.render.entity.RenderLumaBurst;
import reika.chromaticraft.render.entity.RenderMeteorShot;
import reika.chromaticraft.render.entity.RenderMonsterBait;
import reika.chromaticraft.render.entity.RenderNukerBall;
import reika.chromaticraft.render.entity.RenderOverloadingPylonShock;
import reika.chromaticraft.render.entity.RenderParticleCluster;
import reika.chromaticraft.render.entity.RenderPistonSpline;
import reika.chromaticraft.render.entity.RenderSplashGunShot;
import reika.chromaticraft.render.entity.RenderTNTPinball;
import reika.chromaticraft.render.entity.RenderThrownGem;
import reika.chromaticraft.render.entity.RenderTunnelNuker;
import reika.chromaticraft.render.entity.RenderVacuum;
import reika.chromaticraft.render.item.AltarItemRenderer;
import reika.chromaticraft.render.item.ChromaItemRenderer;
import reika.chromaticraft.render.item.DataCrystalRenderer;
import reika.chromaticraft.render.item.EnderCrystalRenderer;
import reika.chromaticraft.render.item.LootChestRenderer;
import reika.chromaticraft.render.item.PortalItemRenderer;
import reika.chromaticraft.render.item.StructureMapRenderer;
import reika.chromaticraft.render.tesr.CrystalPlantRenderer;
import reika.chromaticraft.render.tesr.RenderAvoLamp;
import reika.chromaticraft.render.tesr.RenderColoredAltar;
import reika.chromaticraft.render.tesr.RenderCrystalPortal;
import reika.chromaticraft.render.tesr.RenderLootChest;
import reika.chromaticraft.render.tesr.RenderPolyCrystal;
import reika.chromaticraft.render.tesr.RenderWarpNode;
import reika.chromaticraft.render.tesr.dimension.RenderAntKey;
import reika.chromaticraft.render.tesr.dimension.RenderBouncePad;
import reika.chromaticraft.render.tesr.dimension.RenderGlowingCracks;
import reika.chromaticraft.render.tesr.dimension.RenderGravityTile;
import reika.chromaticraft.render.tesr.dimension.RenderLaserTarget;
import reika.chromaticraft.render.tesr.dimension.RenderPistonDisplay;
import reika.chromaticraft.render.tesr.dimension.RenderVoidRift;
import reika.chromaticraft.render.tesr.dimension.RenderWaterLock;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;
import reika.chromaticraft.world.dimension.rendering.ChromaCloudRenderer;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.trackers.DonatorController;
import reika.dragonapi.auxiliary.trackers.donatorcontroller.Donator;
import reika.dragonapi.auxiliary.trackers.KeybindHandler;
import reika.dragonapi.auxiliary.trackers.PatreonController;
import reika.dragonapi.auxiliary.trackers.PlayerSpecificRenderer;
import reika.dragonapi.auxiliary.trackers.SettingInterferenceTracker;
import reika.dragonapi.instantiable.rendering.ForcedTextureArmorModel;
import reika.dragonapi.instantiable.rendering.MultiSheetItemRenderer;
import reika.dragonapi.instantiable.rendering.TESRItemRenderer;
import reika.dragonapi.libraries.ReikaRegistryHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ChromaClient extends ChromaCommon {

	public static final MultiSheetItemRenderer items = new MultiSheetItemRenderer(ChromatiCraft.instance, ChromatiCraft.class);

	//public static final ItemMachineRenderer machineItems = new ItemMachineRenderer();

	private static final HashMap<ChromaItems, ForcedTextureArmorModel> armorTextures = new HashMap();
	private static final HashMap<ChromaItems, String> armorAssets = new HashMap();

	private static final ChromaItemRenderer placer = new ChromaItemRenderer();

	private static final EnderCrystalRenderer csr = new EnderCrystalRenderer();

	public static final RenderColoredAltar altarRenderer = new RenderColoredAltar();

	public static KeyBinding key_ability;

	public static SoundCategory chromaCategory;

	@Override
	public void initAssetLoaders() {
		dynamicAssets.addToAssetLoader();
	}

	@Override
	public void registerSounds() {
		soundLoader.register();
		chromaCategory = ReikaRegistryHelper.addSoundCategory("CHROMA_MUSIC");

		SettingInterferenceTracker.instance.registerSettingHandler(SettingInterferenceTracker.muteInterference);
	}

	@Override
	public void registerRenderers() {
		if (DragonOptions.NORENDERS.getState()) {
			ChromatiCraft.logger.log("Disabling all machine renders for FPS and lag profiling.");
		}
		else {
			this.loadModels();
		}

		ChromaShaders.registerAll();

		ReikaJavaLibrary.initClass(ChromaCloudRenderer.class, true);

		RenderingRegistry.registerEntityRenderingHandler(EntityBallLightning.class, new RenderBallLightning());
		RenderingRegistry.registerEntityRenderingHandler(EntityAbilityFireball.class, new RenderFireball(2));
		RenderingRegistry.registerEntityRenderingHandler(EntityGluon.class, new RenderGluon());
		RenderingRegistry.registerEntityRenderingHandler(EntityChainGunShot.class, new RenderChainGunShot());
		RenderingRegistry.registerEntityRenderingHandler(EntitySplashGunShot.class, new RenderSplashGunShot());
		RenderingRegistry.registerEntityRenderingHandler(EntityVacuum.class, new RenderVacuum());
		RenderingRegistry.registerEntityRenderingHandler(EntityChromaEnderCrystal.class, new RenderEnderCrystal());
		RenderingRegistry.registerEntityRenderingHandler(EntityMeteorShot.class, new RenderMeteorShot());
		RenderingRegistry.registerEntityRenderingHandler(EntityAurora.class, new RenderAurora());
		RenderingRegistry.registerEntityRenderingHandler(EntityThrownGem.class, new RenderThrownGem());
		RenderingRegistry.registerEntityRenderingHandler(EntityLaserPulse.class, new RenderLaserPulse());
		RenderingRegistry.registerEntityRenderingHandler(EntityPistonSpline.class, new RenderPistonSpline());
		RenderingRegistry.registerEntityRenderingHandler(EntityTNTPinball.class, new RenderTNTPinball());
		RenderingRegistry.registerEntityRenderingHandler(EntityDimensionFlare.class, new RenderDimensionFlare());
		RenderingRegistry.registerEntityRenderingHandler(EntityLumaBurst.class, new RenderLumaBurst());
		RenderingRegistry.registerEntityRenderingHandler(EntityParticleCluster.class, new RenderParticleCluster());
		RenderingRegistry.registerEntityRenderingHandler(EntityNukerBall.class, new RenderNukerBall());
		RenderingRegistry.registerEntityRenderingHandler(EntityGlowCloud.class, new RenderGlowCloud());
		RenderingRegistry.registerEntityRenderingHandler(EntityDataCrystal.class, new RenderDataCrystal());
		RenderingRegistry.registerEntityRenderingHandler(EntityOverloadingPylonShock.class, new RenderOverloadingPylonShock());
		RenderingRegistry.registerEntityRenderingHandler(EntityMonsterBait.class, new RenderMonsterBait());
		RenderingRegistry.registerEntityRenderingHandler(EntityTunnelNuker.class, new RenderTunnelNuker());
		if (ModList.BOTANIA.isLoaded())
			RenderingRegistry.registerEntityRenderingHandler(EntityChromaManaBurst.class, new RenderChromaManaBurst());
		RenderingRegistry.registerEntityRenderingHandler(EntityLightShot.class, new RenderLightShot());
		RenderingRegistry.registerEntityRenderingHandler(EntityDeathFog.class, new RenderDeathFog());

		this.registerSpriteSheets();
		this.registerBlockSheets();

		MinecraftForgeClient.registerItemRenderer(ChromaItems.DATACRYSTAL.getItemInstance(), new DataCrystalRenderer());
		MinecraftForgeClient.registerItemRenderer(ChromaItems.STRUCTMAP.getItemInstance(), new StructureMapRenderer(items));

		RenderSlime slimeRenderer = (RenderSlime)RenderManager.instance.entityRenderMap.get(EntitySlime.class);
		slimeRenderer.scaleAmount = new ColorizableSlimeModel(0);
		ChromatiCraft.logger.log("Overriding Slime Renderer Edge Model.");
	}

	@Override
	public void registerKeys() {
		if (ChromaOptions.KEYBINDABILITY.getState()) {
			key_ability = new KeyBinding("Use Ability", -98, "ChromatiCraft"); //Middle mouse
			//ClientRegistry.registerKeyBinding(key_ability);
			KeybindHandler.instance.addKeybind(key_ability);
		}

		for (int i = 0; i < AbilityHotkeys.SLOTS; i++) {
			AbilityHotkeys.keys[i] = new KeyBinding("Fire Ability "+i, Keyboard.KEY_NUMPAD1+i, "ChromatiCraft"); //defaults to numpad 1,2,3,0
			KeybindHandler.instance.addKeybind(AbilityHotkeys.keys[i]);
		}
	}

	@Override
	public void addArmorRenders() {
		//NVHelmet = RenderingRegistry.addNewArmourRendererPrefix("NVHelmet");
		armor = RenderingRegistry.addNewArmourRendererPrefix("CC");
		/*
		ReikaTextureHelper.forceArmorTexturePath("/Reika/RotaryCraft/Textures/Misc/bedrock_1.png");*/

		addArmorTexture(ChromaItems.FLOATBOOTS, "/Reika/ChromatiCraft/Textures/Misc/floatboots.png");
	}

	private static void addArmorTexture(ChromaItems item, String tex) {
		ChromatiCraft.logger.log("Adding armor texture for "+item+": "+tex);
		armorTextures.put(item, new ForcedTextureArmorModel(ChromatiCraft.class, tex, item.getArmorType()));
		String[] s = tex.split("/");
		String file = s[s.length-1];
		String defaultTex = "chromaticraft:textures/models/armor/"+file;
		//ReikaJavaLibrary.pConsole(defaultTex);
		armorAssets.put(item, defaultTex);
	}

	public static ForcedTextureArmorModel getArmorRenderer(ChromaItems item) {
		return armorTextures.get(item);
	}

	public static String getArmorTextureAsset(ChromaItems item) {
		return armorAssets.get(item);
	}

	public void loadModels() {
		for (int i = 0; i < ChromaTiles.TEList.length; i++) {
			ChromaTiles m = ChromaTiles.TEList[i];
			if (m.hasRender()) {
				ChromaRenderBase render = (ChromaRenderBase)ChromaRenderList.instantiateRenderer(m);
				//int[] renderLists = render.createLists();
				//GLListData.addListData(m, renderLists);
				ClientRegistry.bindTileEntitySpecialRenderer(m.getTEClass(), render);

				if (m == ChromaTiles.ADJACENCY) {
					for (int k = 0; k < 16; k++) {
						if (AdjacencyUpgrades.upgrades[k].isImplemented()) {
							ClientRegistry.bindTileEntitySpecialRenderer(AdjacencyUpgrades.upgrades[k].getTileClass(), render);
						}
					}
				}
			}
		}

		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrystalPlant.class, new CrystalPlantRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLootChest.class, new RenderLootChest());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrystalPortal.class, new RenderCrystalPortal());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityVoidRift.class, new RenderVoidRift());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityColoredAltar.class, altarRenderer);
		ClientRegistry.bindTileEntitySpecialRenderer(AntKeyTile.class, new RenderAntKey());
		ClientRegistry.bindTileEntitySpecialRenderer(TargetTile.class, new RenderLaserTarget());
		ClientRegistry.bindTileEntitySpecialRenderer(TileBouncePad.class, new RenderBouncePad());
		ClientRegistry.bindTileEntitySpecialRenderer(GravityTile.class, new RenderGravityTile());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAvoLamp.class, new RenderAvoLamp());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRotatingLock.class, new RenderWaterLock());
		ClientRegistry.bindTileEntitySpecialRenderer(TileGlowingCracks.class, new RenderGlowingCracks());
		ClientRegistry.bindTileEntitySpecialRenderer(TilePolyCrystal.class, new RenderPolyCrystal());
		ClientRegistry.bindTileEntitySpecialRenderer(TilePistonDisplay.class, new RenderPistonDisplay());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityWarpNode.class, new RenderWarpNode());

		MinecraftForgeClient.registerItemRenderer(ChromaItems.PLACER.getItemInstance(), ChromatiCraft.instance.isLocked() ? null : placer);
		MinecraftForgeClient.registerItemRenderer(ChromaItems.RIFT.getItemInstance(), ChromatiCraft.instance.isLocked() ? null : placer);
		MinecraftForgeClient.registerItemRenderer(ChromaItems.ADJACENCY.getItemInstance(), ChromatiCraft.instance.isLocked() ? null : placer);

		if (!ChromatiCraft.instance.isLocked())
			ReikaRegistryHelper.instantiateAndRegisterISBRHs(ChromatiCraft.instance, ChromaISBRH.values());

		//ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGuardianStone.class, new GuardianStoneRenderer());
		//ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrystalPlant.class, new CrystalPlantRenderer());
		//ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAccelerator.class, new AcceleratorRenderer());

		//MinecraftForgeClient.registerItemRenderer(ChromaBlocks.GUARDIAN.getItem(), teibr);
		//MinecraftForgeClient.registerItemRenderer(ChromaBlocks.ACCELERATOR.getItem(), teibr);

		MinecraftForgeClient.registerItemRenderer(ChromaItems.ENDERCRYSTAL.getItemInstance(), csr);

		MinecraftForgeClient.registerItemRenderer(ChromaBlocks.PORTAL.getItem(), new PortalItemRenderer());
		MinecraftForgeClient.registerItemRenderer(ChromaBlocks.COLORALTAR.getItem(), new AltarItemRenderer());
		MinecraftForgeClient.registerItemRenderer(ChromaBlocks.LOOTCHEST.getItem(), new LootChestRenderer());
		MinecraftForgeClient.registerItemRenderer(ChromaBlocks.AVOLAMP.getItem(), new TESRItemRenderer());
	}

	private void registerBlockSheets() {
		//RenderingRegistry.registerBlockHandler(BlockSheetTexRenderID, block);
	}

	private void registerSpriteSheets() {
		for (int i = 0; i < ChromaItems.itemList.length; i++) {
			ChromaItems c = ChromaItems.itemList[i];
			if (!c.isPlacer() && c != ChromaItems.POTION && c != ChromaItems.MANIPFOCUS && c != ChromaItems.ABILITYFOCUS)
				MinecraftForgeClient.registerItemRenderer(ChromaItems.itemList[i].getItemInstance(), items);
		}
	}

	// Override any other methods that need to be handled differently client side.

	@Override
	public World getClientWorld()
	{
		return FMLClientHandler.instance().getClient().theWorld;
	}

	@Override
	public void addDonatorRender() {
		Collection<Donator> donators = new ArrayList();
		donators.addAll(DonatorController.instance.getReikasDonators());
		donators.addAll(PatreonController.instance.getModPatrons("Reika"));
		for (Donator s : donators) {
			if (s.ingameName != null)
				PlayerSpecificRenderer.instance.registerRenderer(s.ingameName, DonatorPylonRender.instance);
			else
				ChromatiCraft.logger.logError("Donator "+s.displayName+" UUID could not be found! Cannot give special render!");
		}
	}

}
