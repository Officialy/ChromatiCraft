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
import java.util.LinkedList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.relay.BlockRelayBase;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDecoFlowers;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;
import reika.dragonapi.auxiliary.ModularLogger;

/**
 * V33a {@code RelayNetworker}: how a Lumen Relay beam finds its way back to a Relay Source.
 *
 * <p>The search runs <em>backwards</em>, from the machine that wants power toward whatever is feeding
 * it. From the consumer it walks in a straight line in one direction, up to the configured range. What
 * it meets decides what happens next: a Relay Source ends the search successfully; a relay that will
 * carry this colour turns the beam onto <em>that</em> relay's input side and recurses, which is what
 * lets a beam round corners; anything else must be transparent to the beam or the line is dead.
 *
 * <p>Depth is bounded at 48 hops, so a mirror loop terminates rather than hanging the server.
 *
 * <h2>What a relay that refuses the colour does</h2>
 *
 * <p>It <b>blocks</b>, and by a route worth spelling out because it looks like a bug in the original.
 * Upstream's chain adds the relay to the path and then, if {@code canTransmit} is false, simply falls
 * out of the {@code else if} without returning — landing in the passability test below, which a relay
 * block fails. So a wrongly-tuned relay stops the beam exactly as a stone block would. That fall-through
 * is reproduced here deliberately.
 *
 * <h2>Not ported</h2>
 *
 * <p>Three branches of upstream's search are absent because the blocks they test for are not ported, so
 * they cannot occur in a world and the branches are unreachable rather than silently wrong: the <b>Relay
 * Filter</b> (which passes or stops a beam by colour), the <b>Rift</b> (which lets a beam continue in
 * another dimension entirely, gated on the target world being one pylons may generate in), and the
 * <b>Router Node</b> in the passability test. Each is marked at its site.
 *
 * <p>The path's client notification is also absent. Upstream sends a {@code RELAYCONNECT} packet listing
 * every coordinate of the completed path so {@code ChromaFX.spawnRelayParticle} can draw the beam along
 * it; neither that packet nor {@code ChromaFX} is ported. <b>Routing is unaffected</b> — the power moves
 * either way — but until it lands a working beam is invisible.
 */
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

	/**
	 * V33a findRelaySource: the entry point a relay-powered machine calls.
	 *
	 * <p>{@code dist} is the caller's own reach, which is clamped to the network's configured range —
	 * a machine cannot see further than relays can carry.
	 *
	 * @return the source found, having already drained it, or null if the beam did not reach one
	 */
	public TileEntityRelaySource findRelaySource(Level world, BlockPos pos, Direction dir,
			CrystalElement e, int amt, int dist) {
		if (amt <= 0)
			return null;
		RelayFinder finder = new RelayFinder(pos, Math.min(dist, maxRange), maxDepth, e, amt, dir);
		RelayPath path = finder.find(world);
		if (path == null)
			return null;
		// Upstream tests the source's stock before transmitting but returns it either way, so a machine
		// still learns which source it is connected to even when that source is empty.
		if (path.source.getEnergy(e) > 0)
			path.transmit(e);
		return path.source;
	}

	/** A completed route, from the consumer at one end to the source at the other. */
	private static final class RelayPath {

		private final TileEntityRelaySource source;
		private final BlockPos target;
		private final List<BlockPos> path;

		private RelayPath(TileEntityRelaySource src, BlockPos c, CrystalElement e,
				LinkedList<BlockPos> found) {
			source = src;
			target = c;
			path = new ArrayList<>(found.size());
			// The search built this from the consumer outward, so it is reversed into the order the beam
			// actually travels: source first, consumer last.
			while (!found.isEmpty())
				path.add(found.removeLast());
			ModularLogger.instance.log(LOGGER_ID,
					"Relay pathfinding complete from " + source + " to " + target + " for " + e);
		}

		private void transmit(CrystalElement e) {
			if (source.getLevel() == null || source.getLevel().isClientSide())
				return;
			// CHROMA-PORT: upstream sends ChromaPackets.RELAYCONNECT here -- every coordinate of `path`
			// followed by the element ordinal -- to every player within 64 blocks of any point on it, and
			// the client draws the beam with ChromaFX.spawnRelayParticle. Neither the packet nor ChromaFX
			// is ported. Only the visual is missing; the routing above is what moves the power.
			ModularLogger.instance.log(LOGGER_ID,
					"Relay beam of " + e + " along " + path.size() + " points (visual not yet ported)");
		}
	}

	/** V33a RelayFinder: the straight-line walk, and the turns relays put in it. */
	private static final class RelayFinder {

		private final BlockPos target;
		private final int maxRange;
		private final int maxDepth;
		private final CrystalElement color;
		private final int amount;

		/**
		 * The direction the beam is currently travelling. Upstream initialises this to
		 * {@code ForgeDirection.UNKNOWN} and then always overwrites it from the caller before use; 26.2
		 * has no such sentinel, so it is simply required up front.
		 */
		private Direction look;

		private final LinkedList<BlockPos> path = new LinkedList<>();

		private RelayFinder(BlockPos loc, int r, int d, CrystalElement e, int amt, Direction dir) {
			target = loc;
			maxRange = r;
			maxDepth = d;
			color = e;
			amount = amt;
			look = dir;
			path.addFirst(target);
			ModularLogger.instance.log(LOGGER_ID,
					"Relay pathfinding start @ " + loc + " for " + amt + " of " + e);
		}

		private RelayPath find(Level world) {
			return this.findFrom(world, target, 0);
		}

		private RelayPath findFrom(Level world, BlockPos start, int depth) {
			if (depth > maxDepth)
				return null;
			for (int i = 1; i < maxRange; i++) {
				BlockPos c = start.relative(look, i);
				BlockState state = world.getBlockState(c);

				if (world.getBlockEntity(c) instanceof TileEntityRelaySource source) {
					path.addLast(c);
					return new RelayPath(source, target, color, path);
				}
				if (state.getBlock() instanceof BlockRelayBase) {
					path.addLast(c);
					if (world.getBlockEntity(c) instanceof BlockRelayBase.TileRelayBase relay
							&& relay.canTransmit(color)) {
						// The beam leaves along the relay's input side, which is what turns a corner.
						look = relay.getInput();
						return this.findFrom(world, c, depth + 1);
					}
					// Deliberately no return and no continue: a relay that will not carry this colour
					// falls through to the passability test below, which it fails. See the class note.
				}
				// CHROMA-PORT: upstream's Relay Filter branch sits here -- it passes the beam through
				// when the filter accepts the colour and kills it when it does not. BlockRelayFilter and
				// its tile are not ported, so no such block can be in the world.
				// CHROMA-PORT: upstream's Rift branch sits here too, continuing the search from the
				// rift's link target -- in another dimension, if pylons may generate there. TileEntityRift
				// is not ported.

				if (!instance.isRelayPassable(world, c))
					return null;
			}
			return null;
		}
	}

	/** V33a isRelayPassable: what a beam may cross. */
	public boolean isRelayPassable(Level world, BlockPos pos) {
		return PylonFinder.isBlockPassable(world, pos.getX(), pos.getY(), pos.getZ())
				|| this.isBlockRelayTransparent(world, pos);
	}

	/**
	 * V33a isBlockRelayTransparent: the short list of solid-ish things a beam is allowed through anyway,
	 * so decoration does not sever a relay line.
	 *
	 * <p>This test is <b>permissive</b>, which is why each entry matters: anything not reproduced here
	 * makes the beam more blocked than upstream, not less.
	 */
	private boolean isBlockRelayTransparent(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		// CHROMA-PORT: upstream also passes ChromaBlocks.ROUTERNODE, which is not ported.
		if (state.is(ChromaBlocks.decoFlower(ChromaDecoFlowers.AURA_IVY).get())
				|| state.is(ChromaBlocks.GLOW_DAISY.get()))
			return true;
		// Upstream names yellow_flower and red_flower, which in 1.7.10 were the two metadata blocks
		// holding every small flower in the game; the tag is that same set.
		if (state.is(BlockTags.SMALL_FLOWERS))
			return true;
		// Upstream tests `meta % 8 == GLASS.ordinal()`, and the modulo is the point: metadata 8 and up is
		// the reinforced flag, so reinforced glass shielding passes exactly as plain glass does. Reading
		// the material and ignoring the REINFORCED property is that same test.
		if (state.getBlock() instanceof BlockStructureShield shield
				&& shield.getShieldType() == ChromaShieldTypes.GLASS)
			return true;
		// CHROMA-PORT: upstream additionally passes any Biomes o' Plenty flower. Mod integration, and
		// the same rule as Thaumcraft's -- recorded, not reproduced.
		return false;
	}
}
