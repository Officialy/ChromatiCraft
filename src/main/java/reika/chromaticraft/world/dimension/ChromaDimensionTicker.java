/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import reika.chromaticraft.ChromaClient;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.MusicLoader;
import reika.chromaticraft.base.dimensionstructuregenerator.StructurePair;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ExtraChromaIDs;
import reika.chromaticraft.world.dimension.chromadimensionalaudiohandler.DimensionMusic;
import reika.chromaticraft.world.dimension.generators.WorldGenGlowCave;
import reika.dragonapi.auxiliary.trackers.remoteassetloader.RemoteAssetsDownloadCompleteEvent;
import reika.dragonapi.auxiliary.trackers.tickregistry.TickHandler;
import reika.dragonapi.auxiliary.trackers.tickregistry.TickType;
import reika.dragonapi.io.DirectResourceManager;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ChromaDimensionTicker implements TickHandler {

	public static final ChromaDimensionTicker instance = new ChromaDimensionTicker();

	private final Random rand = new Random();

	public final int dimID = ExtraChromaIDs.DIMID.getValue();
	private final Collection<Ticket> tickets = new ArrayList();

	private ChromaDimensionTicker() {

	}

	@SideOnly(Side.CLIENT)
	public static ISound getCurrentMusic() {
		return ChromaDimensionalAudioHandler.currentMusic;
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerMusic(RemoteAssetsDownloadCompleteEvent evt) {
		Collection<String> li = MusicLoader.instance.getMusicFiles();
		ChromatiCraft.logger.log(li.size()+" music tracks available for the dimension: "+li);
		for (String path : li) {
			DimensionMusic mus = new DimensionMusic(path, path.substring(0, path.length()-4).endsWith("_c"));
			ChromaDimensionalAudioHandler.music.add(mus);
			DirectResourceManager.getInstance().registerCustomPath(mus.path, ChromaClient.chromaCategory, true);
		}
	}

	@Override
	public void tick(TickType type, Object... tickData) {
		switch(type) {
			case WORLD: {
				World world = (World)tickData[0];
				if (world.provider.dimensionId == dimID) {
					world.ambientTickCountdown = Integer.MAX_VALUE;

					if (!world.isRemote) {
						this.unloadChunks();
					}

					ChromaDimensionManager.tickPlayersInStructures(world);
					SkyRiverManager.tickSkyRiverServer(world);
					world.setAllowedSpawnTypes(false, true);
					if (!world.playerEntities.isEmpty()) {
						ChromaDimensionManager.dimensionAge++;
						if (!world.isRemote) {
							for (EntityPlayer ep : ((List<EntityPlayer>)world.playerEntities)) {
								if (ep.posY < 30 && !ReikaEntityHelper.canEntitySeeTheSky(ep) && ChromaDimensionManager.getStructurePlayerIsIn(ep) == null) {
									this.spawnVoidLumaFog(ep);
								}
							}
						}
					}
				}
				break;
			}
			case CLIENT:
				this.tickClient();
				break;
			case PLAYER:
				EntityPlayer ep = (EntityPlayer)tickData[0];
				if (ep.worldObj.provider.dimensionId == dimID) {
					if (!RegionMapper.isPointInCentralRegion(ep.posX, ep.posZ)) {
						OuterRegionsEvents.instance.tickPlayerInOuterRegion(ep);
						/*
						if (rand.nextInt(3200) == 0) {
							ChromaDimensionBiome b = BiomeDistributor.getBiome(MathHelper.floor_double(ep.posX), MathHelper.floor_double(ep.posZ));
							if (b == Biomes.SKYLANDS.getBiome() || b == SubBiomes.VOIDLANDS.getBiome()) {
								for (EntityAurora e : WorldGenAurorae.generateAurorae(ep.worldObj, rand, ep.posX, ep.posY, ep.posZ))
									ChromaDimensionManager.addAurora(e);
							}
						}*/
					}
				}
				break;
			case SERVER: {
				for (StructurePair sp : ChunkProviderChroma.structures) {
					//ReikaJavaLibrary.pConsole(sp);
					World w = DimensionManager.getWorld(sp.generatedDimension);
					if (w != null && !w.playerEntities.isEmpty())
						sp.generator.updateTick(w);
				}
			}
			default:
				break;
		}
	}

	@SideOnly(Side.CLIENT)
	private void tickClient() {
		World world = Minecraft.getMinecraft().theWorld;
		if (world != null && !world.playerEntities.isEmpty() && world.provider.dimensionId == ExtraChromaIDs.DIMID.getValue()) {
			if (!ChromaDimensionalAudioHandler.music.isEmpty())
				ChromaDimensionalAudioHandler.playMusic();
			SkyRiverManagerClient.handleSkyRiverMovementClient();
			for (StructurePair sp : ChunkProviderChroma.structures) {
				if (sp.generatedDimension == world.provider.dimensionId)
					sp.generator.updateTick(world);
			}
			EntityPlayer ep = Minecraft.getMinecraft().thePlayer;
			if (ChromaDimensionManager.getStructurePlayerIsIn(ep) == null) {
				double my = WorldGenGlowCave.MAX_BEDROCK_Y+4;
				int n = ep.posY <= 0 ? 6 : 18;
				if (ep.posY <= my && ep.ticksExisted%n == 0) {
					float mf = 0.66F;
					double denom = my-WorldGenGlowCave.FULL_BEDROCK_Y;
					double num = ep.posY-WorldGenGlowCave.FULL_BEDROCK_Y;
					float f = ep.posY <= WorldGenGlowCave.FULL_BEDROCK_Y ? mf : mf*(float)Math.pow(1D-num/denom, 1.25);
					float p = 1;
					if (ep.posY <= 0) {
						f = Math.max(0.25F, (float)Math.max(0, 1D+ep.posY/64D));
						f *= 0.5;
						p += ep.posY*0.002;
					}
					ReikaSoundHelper.playClientSound(ChromaSounds.LOWAMBIENT_SHORT, ep, f, p, false);
				}
			}
		}
	}

	private void spawnVoidLumaFog(EntityPlayer ep) {
		double x = ReikaRandomHelper.getRandomPlusMinus(ep.posX, 32);
		double z = ReikaRandomHelper.getRandomPlusMinus(ep.posZ, 32);
		double y = ReikaRandomHelper.getRandomBetween(-10, 6);
		EntityGlowCloud e = new EntityGlowCloud(ep.worldObj, x, y, z);
		if (e.getCanSpawnHere()) {
			ep.worldObj.spawnEntityInWorld(e);
		}
	}

	private void unloadChunks() {
		for (Ticket t : tickets) {
			for (ChunkCoordIntPair p : t.getChunkList()) {
				ForgeChunkManager.unforceChunk(t, p);
			}
		}
		tickets.clear();
	}

	public void scheduleTicketUnload(Ticket t) {
		tickets.add(t);
	}

	@Override
	public EnumSet<TickType> getType() {
		return EnumSet.of(TickType.WORLD, TickType.CLIENT, TickType.PLAYER);
	}

	@Override
	public boolean canFire(Phase p) {
		return p == Phase.END;
	}

	@Override
	public String getLabel() {
		return "Chroma Dimension Tag";
	}

}
