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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.CrystalNetworkLogger;
import reika.chromaticraft.auxiliary.CrystalNetworkLogger.LoggingLevel;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalRepeater;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.magic.interfaces.TemporaryCrystalReceiverProvider;
import reika.chromaticraft.magic.interfaces.WrapperTile;
import reika.chromaticraft.magic.network.NetworkSorters.TransmitterDistanceSorter;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCreativeSource;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater.NodeClass;
import reika.dragonapi.auxiliary.ModularLogger;
import reika.dragonapi.instantiable.RayTracer;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.instantiable.data.maps.MultiMap.CollectionType;



public class PylonFinder {

	public static final String LOGGER_ID = "PylonFinder";

	private final LinkedList<WorldLocation> nodes = new LinkedList();
	private final Collection<WorldLocation> blacklist = new HashSet();
	private final MultiMap<WorldLocation, WorldLocation> duplicates = new MultiMap(CollectionType.HASHSET);

	private final CrystalNetworker net;

	private static final RayTracer tracer;
	private static final Collection<Predicate<Level>> pylonWorldPredicates = new ArrayList<>();
	private static final Collection<BiFunction<BlockEntity, WorldLocation, CrystalNetworkTile>> networkTileAdapters = new ArrayList<>();
	private static final TagKey<Biome> GLOWING_CLIFFS = TagKey.create(
			Registries.BIOME, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "glowing_cliffs"));

	//private final int stepRange;
	private final CrystalReceiver target;
	private final CrystalElement element;
	private final Player user;

	private int maxSteps = Integer.MAX_VALUE;
	private int steps = 0;
	private int stepsThisTick = 0;
	private boolean suspended = false;
	public static final int MAX_STEPS_PER_TICK = 1000;
	private final boolean isValidWorld;

	private NodeClass skypeaterEntry;
	private NodeClass lastSkypeaterType;

	private static boolean invalid = false;

	private static final HashMap<WorldLocation, EnumMap<CrystalElement, ArrayList<CrystalPath>>> paths = new HashMap();

	//private final HashMap<ChunkPos, ChunkCopy> chunkCache = new HashMap();

	static {
		ModularLogger.instance.addLogger(ChromatiCraft.instance, LOGGER_ID);
	}

	PylonFinder(CrystalElement e, CrystalReceiver r, Player ep) {
		element = e;
		target = r;
		//stepRange = r;
		net = CrystalNetworker.instance;
		blacklist.add(this.getLocation(r));
		user = ep;
		isValidWorld = r.getWorld().dimension() == Level.OVERWORLD
				|| pylonWorldPredicates.stream().anyMatch(test -> test.test(r.getWorld()));
	}

	CrystalPath findPylon() {
		return this.findPylonWith(SourceValidityRule.ALWAYS);
	}

	CrystalPath findPylonWith(SourceValidityRule rule) {
		if (!isValidWorld)
			return null;
		invalid = false;
		CrystalPath p = this.checkExistingPaths();
		//ReikaJavaLibrary.pConsole(p != null ? p.nodes.size() : "null", Side.SERVER);
		if (p != null)
			return p;
		if (!this.anyConnectedSources())
			return null;

		this.findFrom(target, rule);
		//ReikaJavaLibrary.pConsole(this.toString());
		if (this.isComplete()) {
			ArrayList<WorldLocation> li = new ArrayList(nodes);
			CrystalPath path = new CrystalPath(net, !(target instanceof WrapperTile), element, CrystalPath.cleanLocRoute(net, element, li));
			if (!(target instanceof WrapperTile))
				this.addValidPath(path);
			return path;
		}
		return null;
	}

	CrystalFlow findPylon(int amount, int maxthru) {
		return this.findPylon(amount, maxthru, SourceValidityRule.ALWAYS);
	}

	CrystalFlow findPylon(int amount, int maxthru, SourceValidityRule rule) {
		if (!isValidWorld)
			return null;
		invalid = false;
		CrystalPath p = this.checkExistingPaths();
		//ReikaJavaLibrary.pConsole(element+" to "+this.getLocation(target)+": "+p);
		if (p != null)
			return new CrystalFlow(net, p, target, amount, maxthru);
		if (!this.anyConnectedSources())
			return null;
		this.findFrom(target, rule);
		//ReikaJavaLibrary.pConsole(this.toString());
		if (this.isComplete()) {
			ArrayList<WorldLocation> li = new ArrayList(nodes);
			if (lastSkypeaterType != null) {
				this.optimizeSkypeaterRoute(li);
			}
			CrystalFlow flow = new CrystalFlow(net, target, element, amount, CrystalPath.cleanLocRoute(net, element, li), maxthru);
			//ReikaJavaLibrary.pConsole(flow.checkLineOfSight()+":"+flow);
			if (!(target instanceof WrapperTile))
				this.addValidPath(flow.asPath());
			return flow;
		}
		return null;
	}

	private void optimizeSkypeaterRoute(ArrayList<WorldLocation> path) {
		this.optimizeRoute(net, CrystalPath.createNodeList(path), Integer.MAX_VALUE, new JumpOptimizationCheck() {

			@Override
			public boolean canDirectLink(CrystalNetworkTile t1, CrystalNetworkTile t2) {
				return t1 instanceof TileEntitySkypeater && t2 instanceof TileEntitySkypeater;
			}});
	}

	static boolean optimizeRoute(CrystalNetworker net, ArrayList<PathNode> path, int nsteps, JumpOptimizationCheck joc) {
		ArrayList<PathNode> li = new ArrayList(path);
		boolean flag = true;
		int cycles = 0;
		boolean limit = false;
		while (flag) {
			flag = false;
			for (int i = 0; i < li.size() && !flag; i++) {
				for (int k = i; k < li.size() && !flag; k++) {
					if (i < k && Math.abs(i-k) > 1) {
						PathNode loc = li.get(i);
						PathNode loc2 = li.get(k);
						CrystalNetworkTile te1 = getNetTileAt(loc.location, true);
						CrystalNetworkTile te2 = getNetTileAt(loc2.location, true);
						if (te1 instanceof CrystalReceiver && te2 instanceof CrystalTransmitter && joc.canDirectLink(te1, te2)) {
							double d = Math.min(((CrystalReceiver)te1).getReceiveRange(), ((CrystalTransmitter)te2).getSendRange());
							if (te1.getDistanceSqTo(te2.getX(), te2.getY(), te2.getZ()) <= d*d && net.checkLOS((CrystalTransmitter)te2, (CrystalReceiver)te1)) {
								while (k > i+1) {
									li.remove(k-1);
									k--;
								}
								flag = true;
								cycles++;
							}
						}
					}
				}
			}
			if (cycles >= nsteps) {
				flag = false;
				limit = true;
			}
		}
		path.clear();
		path.addAll(li);
		return !limit;
	}
	/*
	private boolean anyConnectedSources() {
		return net.activeSourceCount(element) != 0;
	}*/


	private boolean anyConnectedSources() {
		if (net.size() > 100) //slower than just pathfinding
			return true;
		ArrayList<CrystalSource> li = net.getAllSourcesFor(element, true);
		if (ModularLogger.instance.isEnabled(LOGGER_ID))
			ModularLogger.instance.log(LOGGER_ID, "Found "+li.size()+" sources for "+element+": "+li);
		for (CrystalSource s : li) {
			if (this.isSourceConnected(s))
				return true;
		}
		return false;
	}


	private boolean isSourceConnected(CrystalSource s) {
		return target instanceof WrapperTile || s instanceof TileEntityCreativeSource || !net.getNearbyReceivers(s, element).isEmpty();
	}

	private CrystalPath checkExistingPaths() {
		EnumMap<CrystalElement, ArrayList<CrystalPath>> map = paths.get(getLocation(target));
		if (map != null) {
			ArrayList<CrystalPath> c = map.get(element);
			if (c != null) {
				Iterator<CrystalPath> it = c.iterator();
				while (it.hasNext()) {
					CrystalPath p = it.next();
					CachedPathValidity state = p.stillValid();
					if (!state.shouldKeep())
						it.remove();
					if (state.canConduct())
						return p;
				}
			}
		}
		return null;
	}

	private void addValidPath(CrystalPath p) {
		EnumMap<CrystalElement, ArrayList<CrystalPath>> map = paths.get(p.origin);
		if (map == null) {
			ArrayList<CrystalPath> c = new ArrayList();
			map = new EnumMap(CrystalElement.class);
			c.add(p);
			map.put(element, c);
			paths.put(p.origin, map);
		}
		else {
			ArrayList<CrystalPath> c = map.get(p.element);
			if (c == null) {
				c = new ArrayList();
				c.add(p);
				map.put(element, c);
			}
			else {
				if (!c.contains(p))
					c.add(p);
				//ReikaJavaLibrary.pConsole(c.size(), Side.SERVER);
			}
		}
		for (int i = 0; i < p.nodes.size(); i++) {
			PathNode loc = p.nodes.get(i);
			CrystalNetworkTile te = loc.getTile(true);
			if (te instanceof CrystalRepeater) {
				CrystalRepeater tile = (CrystalRepeater)te;
				tile.setSignalDepth(p.element, p.nodes.size()-1-i);
			}
		}
		//ReikaJavaLibrary.pConsole(paths, Side.SERVER);
		Collections.sort(paths.get(p.origin).get(p.element));
	}

	static void removePathsWithTile(CrystalNetworkTile te) {
		if (te == null)
			return;
		Iterator<EnumMap<CrystalElement, ArrayList<CrystalPath>>> maps = paths.values().iterator();
		while (maps.hasNext()) {
			EnumMap<CrystalElement, ArrayList<CrystalPath>> map = maps.next();
			map.values().removeIf(list -> {
				list.removeIf(path -> path.contains(te));
				return list.isEmpty();
			});
			if (map.isEmpty())
				maps.remove();
		}
	}

	@Override
	public String toString() {
		return element+": "+target;//+" by "+stepRange;
	}

	public boolean isComplete() {
		return nodes.size() >= 2 && this.getSourceAt(nodes.getLast(), false) != null;
	}

	private void findFrom(CrystalReceiver r, SourceValidityRule rule) {
		if (invalid)
			return;
		WorldLocation loc = getLocation(r);
		if (nodes.contains(loc)) {
			return;
		}
		if (this.isComplete()) {
			/*
			StringBuilder sb = new StringBuilder();
			sb.append(nodes.size());
			sb.append(":[");
			int i = 0;
			for (WorldLocation l : nodes) {
				sb.append(i);
				sb.append("=");
				sb.append(l.getBlockEntity());
				sb.append(";");
				i++;
			}
			sb.append("]");
			ReikaJavaLibrary.pConsole("Returning with "+sb.toString());
			 */
			return;
		}
		steps++;
		stepsThisTick++;
		if (steps > maxSteps) {
			//return;
		}
		/*
		if (stepsThisTick >= MAX_STEPS_PER_TICK) {
			stepsThisTick = 0;
			this.suspendUntilNextTick(r);
			return;
		}
		 */
		//ReikaJavaLibrary.pConsole("Stepped in, Receiver="+r);
		nodes.add(loc);
		ArrayList<CrystalTransmitter> li = net.getTransmittersTo(r, element);
		if (CrystalNetworkLogger.getLogLevel().isAtLeast(LoggingLevel.PATHFIND))
			CrystalNetworkLogger.logPathFind(target, element, r, li.toString(), nodes.toString());
		if (ChromaOptions.SHORTPATH.getState() || skypeaterEntry != null) {
			Collections.sort(li, new TransmitterDistanceSorter(r)); //basic "start with closest and work outwards" logic; A* too complex and expensive
		}
		Collections.sort(li, NetworkSorters.prioritizer[element.ordinal()]);
		//ReikaJavaLibrary.pConsole("Found "+li.size()+" for "+r+": "+li);
		for (CrystalTransmitter te : li) {
			if (this.isComplete())
				return;
			WorldLocation loc2 = getLocation(te);
			if (!blacklist.contains(loc2) && !duplicates.containsValue(loc2)) {
				CrystalLink l = net.getLink(loc2, loc);

				if (te != target) {
					if ((te.needsLineOfSightToReceiver(r) || r.needsLineOfSightFromTransmitter(te)) && !l.hasLineOfSight()) {
						continue;
					}

					/*
					ReikaJavaLibrary.pConsole("Data for "+te+":");
					if (te instanceof CrystalSource)
						ReikaJavaLibrary.pConsole("Source: "+(te instanceof CrystalSource)+" && "+this.isConnectableSource((CrystalSource)te, thresh));
					else
						ReikaJavaLibrary.pConsole("Source: false");

					ReikaJavaLibrary.pConsole("Repeater: "+(te instanceof CrystalRepeater));
					 */

					if (te instanceof CrystalSource && this.isConnectableSource((CrystalSource)te, rule)) {
						net.addLink(l, true);
						nodes.add(loc2);
						//ReikaJavaLibrary.pConsole("Found source for "+element+" > "+r+", returning: "+this.isComplete());
						return;
					}
					else if (te instanceof CrystalRepeater) {
						net.addLink(l, true);
						Collection<WorldLocation> others = new ArrayList(li);
						others.remove(te);
						duplicates.put(loc2, others);
						if (te instanceof TileEntitySkypeater) {
							NodeClass c = ((TileEntitySkypeater)te).getNodeType();
							if (skypeaterEntry == null) {
								skypeaterEntry = c;
							}
							else {
								if (c.isAbove(skypeaterEntry))
									;//continue;
							}
							lastSkypeaterType = c;
						}
						else {
							if (lastSkypeaterType != null && skypeaterEntry != null && skypeaterEntry != lastSkypeaterType)
								;//continue;
						}
						this.findFrom((CrystalRepeater)te, rule);
					}
				}
			}
		}
		if (!this.isComplete()) {
			nodes.removeLast();
			blacklist.add(loc);
			duplicates.remove(loc);
		}
	}

	private boolean isConnectableSource(CrystalSource te, SourceValidityRule rule) {
		if (te instanceof TileEntityCrystalPylon)
			if (((TileEntityCrystalPylon)te).enhancing)
				return false;
		return te.canSupply(target, element) && target.canBeSuppliedBy(te, element) && (user == null || te.playerCanUse(user)) && rule.isValid(te, element);
	}

	/*
	private void suspendUntilNextTick(CrystalReceiver r) {
		suspended = true;
	}
	 */

	static final CrystalReceiver getReceiverAt(WorldLocation loc, boolean exception) {
		BlockEntity te = loc.getBlockEntity();
		if (te instanceof CrystalReceiver) {
			return (CrystalReceiver)te;
		}
		CrystalNetworkTile adapted = getAdaptedNetworkTile(te, loc);
		if (adapted instanceof CrystalReceiver receiver) {
			return receiver;
		}
		if (exception)
			throw new IllegalStateException("How did a non-receiver tile ("+te+") get put in the network here ("+loc+")?");
		else
			return null;
	}

	static final CrystalTransmitter getTransmitterAt(WorldLocation loc, boolean exception) {
		BlockEntity te = loc.getBlockEntity();
		if (te instanceof CrystalTransmitter) {
			return (CrystalTransmitter)te;
		}
		if (exception)
			throw new IllegalStateException("How did a non-transmitter tile ("+te+") get put in the network here ("+loc+")?");
		else
			return null;
	}

	static final CrystalSource getSourceAt(WorldLocation loc, boolean exception) {
		BlockEntity te = loc.getBlockEntity();
		if (te instanceof CrystalSource) {
			return (CrystalSource)te;
		}
		if (exception)
			throw new IllegalStateException("How did a non-source tile ("+te+") get put in the network here ("+loc+")?");
		else
			return null;
	}

	static final CrystalNetworkTile getNetTileAt(WorldLocation loc, boolean exception) {
		BlockEntity te = loc.getBlockEntity();
		if (te instanceof CrystalNetworkTile) {
			return (CrystalNetworkTile)te;
		}
		CrystalNetworkTile adapted = getAdaptedNetworkTile(te, loc);
		if (adapted != null) {
			return adapted;
		}
		if (exception)
			throw new IllegalStateException("How did a non-network tile ("+te+") get put in the network here ("+loc+")?");
		else
			return null;
	}

	private static CrystalNetworkTile getAdaptedNetworkTile(BlockEntity tile, WorldLocation location) {
		if (tile instanceof TemporaryCrystalReceiverProvider provider) {
			return provider.createTemporaryReceiver();
		}
		for (BiFunction<BlockEntity, WorldLocation, CrystalNetworkTile> adapter : networkTileAdapters) {
			CrystalNetworkTile result = adapter.apply(tile, location);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/** Registers a compatibility adapter such as the original Thaumcraft node wrapper. */
	public static void registerNetworkTileAdapter(BiFunction<BlockEntity, WorldLocation, CrystalNetworkTile> adapter) {
		networkTileAdapters.add(adapter);
	}

	/** Registers a non-overworld dimension accepted by the pylon world-generation subsystem. */
	public static void registerPylonWorldPredicate(Predicate<Level> predicate) {
		pylonWorldPredicates.add(predicate);
	}


	public static CrystalPath convertTileListToPath(LinkedList<CrystalNetworkTile> li, CrystalElement e) {
		LinkedList<WorldLocation> li2 = new LinkedList();
		for (CrystalNetworkTile te : li) {
			li2.add(new WorldLocation(te.getWorld(), te.getX(), te.getY(), te.getZ()));
		}
		return new CrystalPath(CrystalNetworker.instance, false, e, li2);
	}

	static void replacePath(CrystalSource src, WorldLocation tgt, CrystalElement color, CrystalPath p) {
		EnumMap<CrystalElement, ArrayList<CrystalPath>> data = paths.get(tgt);
		ArrayList<CrystalPath> li = data.get(color);
		for (int i = 0; i < li.size(); i++) {
			CrystalPath at = li.get(i);
			if (at.hasSameEndpoints(p) && at.element == p.element) {
				li.set(i, p);
				return;
			}
		}
		ChromatiCraft.LOGGER.error("Tried to replace a "+color+" path from "+src+" to "+tgt+", but no such path exists!");
	}

	static Collection<? extends CrystalPath> getCachedPaths(CrystalElement e) {
		ArrayList<CrystalPath> li = new ArrayList();
		for (EnumMap<CrystalElement, ArrayList<CrystalPath>> map : paths.values()) {
			ArrayList<CrystalPath> list = map.get(e);
			if (list != null)
				li.addAll(list);
		}
		return li;
	}

	static LOSData lineOfSight(WorldLocation l1, WorldLocation l2) {
		return lineOfSight(l1.getWorld(), l1.pos.getX(), l1.pos.getY(), l1.pos.getZ(), l2.pos.getX(), l2.pos.getY(), l2.pos.getZ());
	}

	private LOSData lineOfSight(CrystalNetworkTile te1, CrystalNetworkTile te) {
		return lineOfSight(te1.getWorld(), te1.getX(), te1.getY(), te1.getZ(), te.getX(), te.getY(), te.getZ());
	}

	//private boolean lineOfSight(Level world, int x, int y, int z, CrystalNetworkTile te) {
	//	return lineOfSight(world, x, y, z, te.getX(), te.getY(), te.getZ());
	//}

	public static LOSData lineOfSight(Level world, int x, int y, int z, Entity e, Block... extra) {
		return lineOfSight(world, x, y, z, Mth.floor(e.getX()), Mth.floor(e.getY()), Mth.floor(e.getZ()), extra);
	}

	public static LOSData lineOfSight(Level world, int x1, int y1, int z1, int x2, int y2, int z2, Block... extra) {
		tracer.setOrigins(x1, y1, z1, x2, y2, z2);
		tracer.offset(0.5, 0.5, 0.5);
		if (extra.length > 0) {
			for (Block b : extra)
				tracer.addOneTimeIgnoredBlock(b);
		}
		boolean los = tracer.isClearLineOfSight(world);
		Set<BlockPos> set = tracer.getRayBlocks();
		return new LOSData(los, canRainOn(world, set), set);
	}

	private static boolean canRainOn(Level world, Set<BlockPos> set) {
		for (BlockPos pos : set) {
			Holder<Biome> biome = world.getBiome(pos);
			if (isRainableBiome(biome) && world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY())
				return true;
		}
		return false;
	}

	public static boolean isRainableBiome(Holder<Biome> biome) {
		return !biome.is(GLOWING_CLIFFS) && (biome.is(Tags.Biomes.IS_DESERT) || biome.value().hasPrecipitation()); //deserts because sandstorms
	}

	static {
		tracer = new RayTracer(0, 0, 0, 0, 0, 0);
		tracer.softBlocksOnly = true;
		tracer.allowFluids = false;
		tracer.uniDirectionalChecks = true;
		tracer.addTransparentBlock(Blocks.GLASS);
		tracer.addTransparentBlock(Blocks.GLASS_PANE);
		tracer.addTransparentBlock(Blocks.SNOW);

		tracer.addOpaqueBlock(Blocks.DEAD_BUSH);
		tracer.addOpaqueBlock(Blocks.SHORT_GRASS);
		tracer.addOpaqueBlock(Blocks.FERN);
		tracer.addOpaqueBlock(Blocks.FIRE);
		tracer.addOpaqueBlock(Blocks.VINE);

		tracer.cacheBlockRay = true;
	}

	/**
	 * Registers glass-like blocks ignored by network rays. The legacy integrations for selective
	 * glass, GeoStrata glow vines, Extra Utilities, Tinkers' Construct, and Ender IO register here
	 * when their modern content adapters are present.
	 */
	public static void registerTransparentBlock(Block block) {
		tracer.addTransparentBlock(block);
	}

	/** Registers a non-solid decorative block that must nevertheless interrupt a network ray. */
	public static void registerOpaqueBlock(Block block) {
		tracer.addOpaqueBlock(block);
	}

	public static final WorldLocation getLocation(CrystalNetworkTile te) {
		return new WorldLocation(te.getWorld(), te.getX(), te.getY(), te.getZ());
	}

	static void stopAllSearches() {
		invalid = true;
	}

	public static boolean isBlockPassable(Level world, int x, int y, int z) {
		return tracer.isBlockPassable(world, x, y, z);
	}
	/*
	void receiveChunk(Chunk c) {
		ChunkPos key = c.getChunkCoordIntPair();
		ChunkCopy copy = new ChunkCopy(c);
		chunkCache.put(key, copy);
	}

	private static class ChunkCopy extends Chunk/*implements IBlockAccess*//* {

		private final short[][][] data = new short[16][256][16];

		private ChunkCopy(Chunk c) {
			super(c.worldObj, c.xPosition, c.zPosition);
			for (int i = 0; i < 16; i++) {
				for (int k = 0; k < 16; k++) {
					for (int j = 0; j < 256; j++) {
						data[i][j][k] = encode(c.getBlock(i, j, k), c.getBlockMetadata(i, j, k));
					}
				}
			}
		}

		private static short encode(Block b, int meta) {
			return (short)(Block.getIdFromBlock(b)+(meta << 12));
		}

		private static BlockKey decode(short id) {
			return new BlockKey(Block.getBlockById(id & 4095), (id >> 12));
		}

		@Override
		public Block getBlock(int x, int y, int z) {
			return this.decode(data[x&15][y][z&15]).blockID;
		}

		@Override
		public int getBlockMetadata(int x, int y, int z) {
			return this.decode(data[x&15][y][z&15]).metadata;
		}
		/*
		@Override
		public boolean isAirBlock(int x, int y, int z) {
			return this.getBlock(x, y, z).getMaterial() == Material.air;
		}

		@Override
		public BlockEntity getTileEntity(int x, int y, int z) {return null;}

		@Override
		@SideOnly(Side.CLIENT)
		public int getLightBrightnessForSkyBlocks(int x, int y, int z, int p_72802_4_) {return 0;}

		@Override
		public int isBlockProvidingPowerTo(int x, int y, int z, int side) {return 0;}

		@Override
		@SideOnly(Side.CLIENT)
		public Biome getBiomeGenForCoords(int x, int z) {return null;}

		@Override
		@SideOnly(Side.CLIENT)
		public int getHeight() {return 256;}

		@Override
		@SideOnly(Side.CLIENT)
		public boolean extendedLevelsInChunkCache() {return false;}

		@Override
		public boolean isSideSolid(int x, int y, int z, Direction side, boolean _default) {return false;}
	 *//*
}

static class ChunkRequest {

	final PylonFinder pathfinder;
	final WorldChunk chunk;

	private ChunkRequest(PylonFinder f, WorldChunk wc) {
		pathfinder = f;
		chunk = wc;
	}

}
	  */

	public static int getSourcePriority(CrystalSource src, CrystalElement e) {
		return src.getEnergy(e);
	}
}
