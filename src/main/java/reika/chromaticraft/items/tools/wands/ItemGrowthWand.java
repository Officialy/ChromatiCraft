/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ItemGrowthWand} (GROWTH): clicking a block starts a growth wave over the square around it (radius 4,
 * or 8 with CTM progression). Every server tick the wave takes one random column from its innermost remaining ring
 * and gives it 1-6 extra block ticks, n*(n+1) with Power on the wand, costing Black, Purple and Green each time
 * (sixteen-fold when Power-enhanced). Players within 16 blocks see each tick as a rising green mote.
 */
public class ItemGrowthWand extends ItemWandBase {

	private static final GrowthTicker ticker = new GrowthTicker();
	private static boolean tickerRegistered;

	public ItemGrowthWand(Properties properties) {
		super(properties);
		this.addEnergyCost(CrystalElement.BLACK, 2);
		this.addEnergyCost(CrystalElement.PURPLE, 10);
		this.addEnergyCost(CrystalElement.GREEN, 40);
		// V33a TickRegistry.registerTickHandler(ticker): a WORLD tick handler firing at START.
		if (!tickerRegistered) {
			tickerRegistered = true;
			NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre evt) -> {
				if (evt.getLevel() instanceof ServerLevel level)
					ticker.tick(level);
			});
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player ep = context.getPlayer();
		if (ep != null && this.sufficientEnergy(ep)) {
			Level world = context.getLevel();
			BlockPos pos = context.getClickedPos();
			int r = this.getRange(ep);
			// V33a added the wave on both sides; only the server's ever ticked, so the client copy only leaked.
			if (!world.isClientSide())
				ticker.addLocation(this, ep, world, pos, r, r/2, powerLevel(world, context.getItemInHand()) > 0);
			ChromaSounds.USE.playSound(world, pos, 1, 1);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static int powerLevel(Level world, ItemStack is) {
		var power = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.POWER);
		return power.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, is)).orElse(0);
	}

	private int getRange(Player ep) {
		return canUseBoostedEffect(ep) ? 8 : 4;
	}

	public static class GrowthTicker {

		private final Collection<GrowthLocation> locations = new ArrayList<>();
		private boolean isIterating;
		private final Collection<GrowthLocation> holdingCache = new ArrayList<>();

		public void tick(ServerLevel world) {
			isIterating = true;
			Iterator<GrowthLocation> it = locations.iterator();
			while (it.hasNext()) {
				GrowthLocation g = it.next();
				if (g.dimension == world.dimension()) {
					g.tick(world);
					if (g.isEmpty())
						it.remove();
				}
			}
			locations.addAll(holdingCache);
			holdingCache.clear();
			isIterating = false;
		}

		private void addLocation(ItemGrowthWand wand, Player ep, Level world, BlockPos pos, int w, int h, boolean e) {
			GrowthLocation g = new GrowthLocation(wand, ep, world, pos, w, h, e);
			if (isIterating)
				holdingCache.add(g);
			else
				locations.add(g);
		}

	}

	private static class GrowthLocation {

		private final ResourceKey<Level> dimension;
		private final Player player;
		private final ItemGrowthWand wand;
		private static final Random rand = new Random();
		private final boolean enhanced;
		private final TreeMap<Integer, ArrayList<BlockPos>> locations = new TreeMap<>();

		private GrowthLocation(ItemGrowthWand wand, Player ep, Level world, BlockPos origin, int r1, int r2, boolean e) {
			dimension = world.dimension();
			player = ep;
			this.wand = wand;
			for (int i = -r1; i <= r1; i++) {
				for (int k = -r1; k <= r1; k++) {
					this.addLocation(origin.offset(i, 0, k), i*i+k*k);
				}
			}
			enhanced = e;
		}

		public boolean isEmpty() {
			return locations.isEmpty();
		}

		private void addLocation(BlockPos pos, int d) {
			locations.computeIfAbsent(d, key -> new ArrayList<>()).add(pos);
		}

		private void removeLocation(int idx, ArrayList<BlockPos> li, int d) {
			li.remove(idx);
			if (li.isEmpty()) {
				locations.remove(d);
			}
		}

		private void tick(ServerLevel world) {
			int d = locations.firstKey();
			ArrayList<BlockPos> li = locations.get(d);
			int idx = rand.nextInt(li.size());
			BlockPos c = li.get(idx);
			if (c != null) {
				int n = 1+rand.nextInt(6);
				if (enhanced)
					n = n*(1+n);
				for (int i = 0; i < n; i++) {
					// V33a Coordinate.updateTick: the block's own tick at that position, which 26.x splits into the
					// random tick that grows crops, saplings and grass.
					BlockState state = world.getBlockState(c);
					state.randomTick(world, c, world.getRandom());
					ChromaNetwork.sendGrowthWand(world, c);
				}
				this.removeLocation(idx, li, d);
				int f = enhanced ? 16 : 1;
				wand.drainPlayer(player, f);
			}
		}

	}

}
