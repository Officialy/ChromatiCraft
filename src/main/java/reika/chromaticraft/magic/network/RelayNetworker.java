/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.relay.BlockRelayBase;
import reika.chromaticraft.block.relay.blockrelaybase.TileRelayBase;
import reika.chromaticraft.block.relay.blockrelayfilter.TileEntityRelayFilter;
import reika.chromaticraft.block.worldgen.blockdecoflower.Flowers;
import reika.chromaticraft.block.worldgen.blockstructureshield.BlockType;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;
import reika.chromaticraft.tileentity.transport.TileEntityRift;
import reika.chromaticraft.world.iwg.PylonGenerator;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.ModularLogger;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.instantiable.io.packettarget.CompoundPlayerTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.modinteract.itemhandlers.BoPBlockHandler;

public final class RelayNetworker {

	public static final RelayNetworker instance = new RelayNetworker(getConfigurableRange(), 48);

	private static final String LOGGER_ID = "lumenrelay";

	public final int maxRange;
	public final int maxDepth;

	static {
		ModularLogger.instance.addLogger(ChromatiCraft.instance, LOGGER_ID);
	}

	private RelayNetworker(int r, int d) {
		maxRange = r;
		maxDepth = d;
	}

	private static int getConfigurableRange() {
		return Mth.clamp(ChromaOptions.RELAYRANGE.getValue(), 8, 24);
	}

	public TileEntityRelaySource findRelaySource(Level world, int x, int y, int z, Direction dir, CrystalElement e, int amt, int dist) {
		if (amt <= 0)
			return null;
		RelayFinder rf = new RelayFinder(new Coordinate(x, y, z), Math.min(dist, maxRange), maxDepth, e, amt);
		rf.look = dir;
		RelayPath path = rf.find(world);
		if (path != null) {
			if (path.source.getEnergy(e) > 0)
				path.transmit(e);
			return path.source;
		}
		return null;
	}

	private static class RelayPath {

		public final TileEntityRelaySource source;
		public final Coordinate target;
		public final CrystalElement color;

		private final ArrayList<Coordinate> path;

		private RelayPath(TileEntityRelaySource src, Coordinate c, CrystalElement e, LinkedList<Coordinate> li) {
			source = src;
			target = c;
			color = e;
			path = new ArrayList();
			while (!li.isEmpty()) {
				path.add(li.removeLast()); //reverse list
			}
			ModularLogger.instance.log(LOGGER_ID, "Relay pathfinding complete from "+source+" to "+target+" for "+e);
		}

		public void transmit(CrystalElement e) {
			if (!source.worldObj.isClientSide()) {
				List<Integer> dat = new ArrayList();
				for (Coordinate c : path) {
					dat.add(c.xCoord);
					dat.add(c.yCoord);
					dat.add(c.zCoord);
				}
				dat.add(e.ordinal());

				ReikaPacketHelper.sendNIntPacket(ChromatiCraft.packetChannel, ChromaPackets.RELAYCONNECT.ordinal(), this.getTarget(), dat);
			}
		}

		private PacketTarget getTarget() {
			Collection<ServerPlayer> li = new ArrayList();
			for (Object o : source.worldObj.playerEntities) {
				ServerPlayer ep = (ServerPlayer)o;
				for (Coordinate c : path) {
					if (c.getDistanceTo(ep) <= 64) {
						li.add(ep);
						break;
					}
				}
			}
			return new CompoundPlayerTarget(li);
		}

	}

	private static class RelayFinder {

		private final Coordinate target;
		private final int maxRange;
		private final int maxDepth;
		private final CrystalElement color;
		private final int amount;

		private Direction look = Direction.UNKNOWN;

		private final LinkedList<Coordinate> path = new LinkedList();

		private RelayFinder(Coordinate loc, int r, int d, CrystalElement e, int amt) {
			target = loc;
			maxRange = r;
			maxDepth = d;
			color = e;
			amount = amt;
			path.addFirst(target);
			ModularLogger.instance.log(LOGGER_ID, "Relay pathfinding start @ "+loc+" for "+amt+" of "+e);
		}

		private RelayPath find(Level world) {
			return this.findFrom(world, target, 0);
		}

		private RelayPath findFrom(Level world, Coordinate start, int depth) {
			if (depth > maxDepth)
				return null;
			for (int i = 1; i < maxRange; i++) {
				Coordinate c = start.offset(look, i);
				Block b = c.getBlock(world);
				int meta = c.getBlockMetadata(world);
				if (ChromaTiles.getTileFromIDandMetadata(b, meta) == ChromaTiles.RELAYSOURCE) {
					path.addLast(c);
					return new RelayPath((TileEntityRelaySource)c.getTileEntity(world), target, color, path);
				}
				else if (b instanceof BlockRelayBase) {
					path.addLast(c);
					TileRelayBase te = (TileRelayBase)c.getTileEntity(world);
					if (te.canTransmit(color)) {
						look = te.getInput();
						return this.findFrom(world, c, depth+1);
					}
				}
				else if (b == ChromaBlocks.RELAYFILTER.getBlockInstance()) {
					TileEntityRelayFilter te = (TileEntityRelayFilter)c.getTileEntity(world);
					//ReikaJavaLibrary.pConsole(color+": "+te.canTransmit(color));
					if (!te.canTransmit(color)) {
						return null;
					}
					continue;
				}
				if (ChromaTiles.getTileFromIDandMetadata(b, meta) == ChromaTiles.RIFT) {
					TileEntityRift te = (TileEntityRift)c.getTileEntity(world);
					WorldLocation loc = te.getLinkTarget();
					if (loc != null) {
						Level world2 = loc.getWorld();
						if (world2.provider.dimensionId == world.provider.dimensionId || PylonGenerator.instance.canGenerateIn(world2)) {
							path.addLast(c);
							return this.findFrom(world2, new Coordinate(loc), depth+1);
						}
						else {
							return null;
						}
					}
					else {
						return null;
					}
				}
				else {
					/*
					if (b.isOpaqueCube())
						return null;
					else if (b.getLightOpacity(world, c.xCoord, c.yCoord, c.zCoord) > 0)
						return null;
					 */
					if (!instance.isRelayPassable(world, c.xCoord, c.yCoord, c.zCoord)) {
						return null;
					}
				}
			}
			return null;
		}

	}

	public boolean isRelayPassable(Level world, int x, int y, int z) {
		return PylonFinder.isBlockPassable(world, x, y, z) || this.isBlockRelayTransparent(world, x, y, z);
	}

	private boolean isBlockRelayTransparent(Level world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		if (b == ChromaBlocks.ROUTERNODE.getBlockInstance())
			return true;
		if (b == ChromaBlocks.DECOFLOWER.getBlockInstance() && (meta == Flowers.FLOWIVY.ordinal() || meta == Flowers.GLOWDAISY.ordinal()))
			return true;
		if (b == Blocks.yellow_flower || b == Blocks.red_flower)
			return true;
		if (ModList.BOP.isLoaded() && BoPBlockHandler.getInstance().isFlower(b))
			return true;
		if (b == ChromaBlocks.STRUCTSHIELD.getBlockInstance() && meta%8 == BlockType.GLASS.ordinal())
			return true;
		return false;
	}

}
