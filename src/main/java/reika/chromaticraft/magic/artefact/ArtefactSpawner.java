/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.artefact;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.UnknownArtefactFeature;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

/**
 * V33a {@code ArtefactSpawner}: in the artefact rings around the lore towers, the ground hides more relics
 * than worldgen placed.
 *
 * <p>While a player stands in an artefact chunk, one tick in two sends them the zone signal (which keeps
 * the UAZONE screen shader alive, see {@code ArtefactZoneScreenEffect}), and one tick in a thousand,
 * if fewer than three are pending, a buried artefact is imagined 32 blocks away in a random direction.
 * Pending artefacts are only a shimmer seen through the ground, marked to every player each tick; one
 * vanishes when the player it was spawned for walks 64 blocks off. Mining out the block just above one
 * makes it real: the Unknown Artefact appears beneath.
 *
 * <p>As in V33a the pending list is shared by all players, so the limit of three is a server-wide one.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public class ArtefactSpawner {

	private static final int SPAWN_DISTANCE = 32;
	private static final int DESPAWN_DISTANCE = 64;

	private static final int SPAWN_LIMIT = 3;
	private static final int FORCED_SPAWN_LIMIT = 20;

	private static final Random rand = new Random();

	public static final ArtefactSpawner instance = new ArtefactSpawner();

	private final ArrayList<SpawnedArtefact> artefacts = new ArrayList<>();

	private ArtefactSpawner() {}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (event.getEntity() instanceof ServerPlayer ep)
			instance.tick(ep);
	}

	@SubscribeEvent
	public static void onBreak(BreakBlockEvent event) {
		instance.checkPlayerBreak(event);
	}

	private void tick(ServerPlayer ep) {
		ServerLevel world = ep.level();
		if (!UnknownArtefactFeature.canGenerateIn(world)) {
			artefacts.clear();
			return;
		}
		boolean canSpawn = this.canSpawnArtefactNearPlayer(ep);
		if (canSpawn && rand.nextInt(2) == 0)
			ChromaNetwork.sendArtefactZone(ep);
		if (canSpawn && artefacts.size() < SPAWN_LIMIT && rand.nextInt(1000) == 0) {
			double a = Math.toRadians(rand.nextDouble() * 360);
			double dx = ep.getX() + SPAWN_DISTANCE * Math.cos(a);
			double dz = ep.getZ() + SPAWN_DISTANCE * Math.sin(a);
			this.addArtefact(dx, dz, ep, Integer.MAX_VALUE);
		}
		if (!artefacts.isEmpty()) {
			Iterator<SpawnedArtefact> it = artefacts.iterator();
			while (it.hasNext()) {
				if (it.next().tick(ep))
					it.remove();
			}
		}
	}

	public void addArtefact(double dx, double dz, ServerPlayer ep, int life) {
		if (artefacts.size() < FORCED_SPAWN_LIMIT) {
			int x = Mth.floor(dx);
			int z = Mth.floor(dz);
			int y = this.getY(ep.level(), x, z);
			if (y > ep.level().getMinY() && UnknownArtefactFeature.canGenerateArtefactAt(ep.level(), new BlockPos(x, y, z)))
				artefacts.add(new SpawnedArtefact(dx, y - 0.5, dz, ep, life));
		}
	}

	private int getY(ServerLevel world, int x, int z) {
		if (world.getBiome(new BlockPos(x, world.getSeaLevel(), z)).is(BiomeTags.IS_OCEAN)) {
			int y = ReikaWorldHelper.getTopSolidOrLiquidBlock(world, x, z);
			while (y >= world.getMinY() + 4 && (world.getBlockState(new BlockPos(x, y, z)).is(Blocks.WATER)
					|| world.getBlockState(new BlockPos(x, y, z)).isAir()))
				y--;
			BlockState at = world.getBlockState(new BlockPos(x, y, z));
			if (at.is(Blocks.GRAVEL) || at.is(Blocks.SAND) || at.is(Blocks.DIRT) || at.is(Blocks.CLAY))
				return y;
			return world.getMinY() - 1;
		}
		return ReikaWorldHelper.getTopSolidOrLiquidBlock(world, x, z) - 1;
	}

	private boolean canSpawnArtefactNearPlayer(ServerPlayer ep) {
		if (ReikaPlayerAPI.isFake(ep))
			return false;
		int cx = Mth.floor(ep.getX()) & ~15;
		int cz = Mth.floor(ep.getZ()) & ~15;
		return UnknownArtefactFeature.isUAChunk(ep.level(), cx, cz);
	}

	public boolean isOnTopOfArtefact(int x, int y, int z) {
		Iterator<SpawnedArtefact> it = artefacts.iterator();
		while (it.hasNext()) {
			SpawnedArtefact a = it.next();
			if (Mth.floor(a.x) == x && Mth.floor(a.y) == y - 1 && Mth.floor(a.z) == z) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	/** V33a {@code checkPlayerBreak}: mining out the block above a pending artefact makes it real. */
	private void checkPlayerBreak(BreakBlockEvent evt) {
		if (evt.getPlayer() instanceof ServerPlayer ep && !ReikaPlayerAPI.isFake(ep)) {
			BlockPos pos = evt.getPos();
			if (this.isOnTopOfArtefact(pos.getX(), pos.getY(), pos.getZ()))
				ep.level().setBlock(pos.below(), ChromaBlocks.UNKNOWN_ARTEFACT.get().defaultBlockState(), 3);
		}
	}

	private static final class SpawnedArtefact {

		private final double x;
		private final double y;
		private final double z;
		private final UUID spawnedByPlayer;
		private final int maxLife;

		private int age;

		private SpawnedArtefact(double x, double y, double z, ServerPlayer ep, int l) {
			this.x = x;
			this.y = y;
			this.z = z;
			spawnedByPlayer = ep.getUUID();
			maxLife = l;
		}

		private boolean tick(ServerPlayer ep) {
			ChromaNetwork.sendArtefactFx(ep, x, y, z);
			age++;
			return age >= maxLife || (ep.getUUID().equals(spawnedByPlayer) && ep.distanceToSqr(x, y, z) >= DESPAWN_DISTANCE * DESPAWN_DISTANCE);
		}

	}

}
