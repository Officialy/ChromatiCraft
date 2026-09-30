/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.aoe.effect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.AdjacencyUpgradeAPI.BlacklistReason;
import reika.chromaticraft.api.interfaces.CustomAcceleration;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.ModList;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code TileEntityAccelerator}, the light blue Tile Accelerator: each tick it runs every adjacent tile's own tick
 * {@code 2^(tier+1)-1} more times, unless that tile is blacklisted (its own family, network repeaters and pylons,
 * cables, conduits and wires, solar and wind and water generators, Applied Energistics, the listed mod tiles, and
 * whatever an API caller named). A registered custom acceleration replaces the extra ticks. If one tick's work runs past
 * the configured lag budget it stops for that tick, and the face it was on puffs cloud for a second.
 */
public class TileEntityAccelerator extends TileEntityAdjacencyUpgrade {

	/** V33a {@code MAX_LAG}, read from the config each time since a static would read it before it loads. */
	public static long getMaxLag() {
		int base = ChromaOptions.TILELAG.getValue();
		return base >= 0 ? Math.max(base, 100000L) : -1;
	}

	/** Shared by the server and client threads, hence concurrent. */
	private static final Map<Class<?>, Acceleration> actions = new ConcurrentHashMap<>();

	public static int debugLevel = 0;

	private int[] lagTimer = new int[6];

	private static AdjacencyEffectDescription blacklist;
	private static AdjacencyEffectDescription defaultEffect;

	private static final Acceleration blacklistKey = new Acceleration() {
		@Override
		public void tick(BlockEntity te, int factor, BlockEntity accelerator) {}

		@Override
		public boolean usesParentClasses() {
			return false;
		}
	};

	private static final Acceleration defaultKey = new Acceleration() {
		@Override
		public void tick(BlockEntity te, int factor, BlockEntity accelerator) {}

		@Override
		public boolean usesParentClasses() {
			return false;
		}
	};

	private static boolean doRecursiveChecks = false;

	public TileEntityAccelerator(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ADJACENCY.get(), pos, state);
	}

	/** V33a {@code initHandlers}, run from common setup (V33a postInit) and before any other registration here. */
	public static synchronized void initHandlers() {
		if (blacklist != null)
			return;
		defaultEffect = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIGHTBLUE, "Accelerates operations");
		defaultEffect.setOrderIndex(Integer.MIN_VALUE).addDisplays(new EffectDisplay.Icon(ChromaIcons.FADE_STAR, "Almost anything (Default Effect)"));
		blacklist = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIGHTBLUE, "Does nothing");

		blacklistTile("icbm.sentry.turret.Blocks.TileTurret", ModList.ICBM, "", BlacklistReason.BUGS); //by request
		blacklistTile("bluedart.tile.decor.TileForceTorch", ModList.DARTCRAFT, "", BlacklistReason.CRASH); //StackOverflow
		blacklistTile("com.sci.torcherino.tile.TileTorcherino", null, "Torcherino:tile.torcherino", BlacklistReason.CRASH); //StackOverflow
		blacklistTile("com.sci.torcherino.tile.TileTorcherino", null, "Torcherino:tile.inverse_torcherino", BlacklistReason.CRASH); //StackOverflow
		blacklistTile("com.sci.torcherino.tile.TileTorcherino", null, "Torcherino:tile.compressed_torcherino", BlacklistReason.CRASH); //StackOverflow
		blacklistTile("com.sci.torcherino.tile.TileTorcherino", null, "Torcherino:tile.compressed_inverse_torcherino", BlacklistReason.CRASH); //StackOverflow
		blacklistTile("com.sci.torcherino.tile.TileTorcherino", null, "Torcherino:tile.double_compressed_inverse_torcherino", BlacklistReason.CRASH); //StackOverflow

		blacklistTile("mrtjp.projectred.integration.Timer", ModList.PROJRED, "ProjRed|Integration:projectred.integration.gate:17", BlacklistReason.BUGS);
		blacklistTile("mrtjp.projectred.integration.Sequencer", ModList.PROJRED, "ProjRed|Integration:projectred.integration.gate:18", BlacklistReason.BUGS);
		blacklistTile("mrtjp.projectred.integration.Repeater", ModList.PROJRED, "ProjRed|Integration:projectred.integration.gate:10", BlacklistReason.BUGS);
		blacklistTile("mrtjp.projectred.integration.PulseFormer", ModList.PROJRED, "ProjRed|Integration:projectred.integration.gate:9", BlacklistReason.BUGS);
		blacklistTile("mrtjp.projectred.integration.StateCell", ModList.PROJRED, "ProjRed|Integration:projectred.integration.gate:20", BlacklistReason.BUGS);
		blacklistTile("mrtjp.projectred.integration.Synchronizer", ModList.PROJRED, "ProjRed|Integration:projectred.integration.gate:21", BlacklistReason.BUGS);

		for (int i = 0; i < ChromaTiles.TEList.length; i++) {
			ChromaTiles c = ChromaTiles.TEList[i];
			if (!c.allowsAcceleration()) {
				if (c == ChromaTiles.ADJACENCY) {
					for (int r = 0; r < 16; r++) {
						AdjacencyUpgrades a = AdjacencyUpgrades.upgrades[r];
						if (a.isImplemented()) {
							blacklistTile(c.getTEClass(), () -> a.getStackOfTier(2));
						}
					}
				}
				else {
					blacklistTile(c.getTEClass(), c::getCraftedProduct);
				}
			}
		}
	}

	private static void addBlacklistItem(@Nullable Supplier<ItemStack> is) {
		if (is != null)
			addBlacklistItems(List.of(is));
	}

	public static void addBlacklistItems(List<Supplier<ItemStack>> is) {
		blacklist.addDisplays(new EffectDisplay.Blacklist(is));
	}

	public static void blacklistTile(Class<? extends BlockEntity> cl, @Nullable ItemStack is) {
		blacklistTile(cl, is == null || is.isEmpty() ? null : (Supplier<ItemStack>)() -> is);
	}

	public static void blacklistTile(Class<? extends BlockEntity> cl, @Nullable Supplier<ItemStack> is) {
		if (cl == BlockEntity.class)
			throw new IllegalArgumentException("You cannot blacklist the core TE class!");
		initHandlers();
		actions.put(cl, blacklistKey);
		addBlacklistItem(is);
	}

	public static void customizeTile(Class<?> c, Acceleration a) {
		initHandlers();
		actions.put(c, a);
		doRecursiveChecks |= a.usesParentClasses();
	}

	public static void customizeTile(Class<?> c, CustomAcceleration a) {
		initHandlers();
		actions.put(c, new APIAcceleration(a));
		doRecursiveChecks |= a.usesParentClasses();
	}

	/**
	 * V33a's by-name blacklist of other mods' tiles. The names are their 1.7.10 classes; a mod with no loaded class of
	 * that name logs the miss, as V33a did when a mod had moved its class.
	 */
	private static void blacklistTile(String name, @Nullable ModList mod, String s, BlacklistReason r) {
		initHandlers();
		Class<?> cl;
		if (mod != null && !mod.isLoaded())
			return;
		try {
			cl = Class.forName(name);
			if (cl == BlockEntity.class)
				throw new IllegalArgumentException("You cannot blacklist the core TE class!");
			ChromatiCraft.LOGGER.info("TileEntity \""+name+"\" has been blacklisted from the TileEntity Accelerator, because "+r.message);
			actions.put(cl, blacklistKey);
			addBlacklistItem(lookupItem(s));
		}
		catch (ClassNotFoundException e) {
			ChromatiCraft.LOGGER.info("Could not add "+name+" to the Accelerator blacklist: Class not found!");
		}
	}

	/** V33a {@code ReikaItemHelper.lookupItem("modid:name[:meta]")}; metadata has no 26.x meaning and is dropped. */
	private static @Nullable Supplier<ItemStack> lookupItem(String s) {
		if (s == null || s.isEmpty())
			return null;
		String[] parts = s.split(":");
		if (parts.length < 2)
			return null;
		Identifier id = Identifier.tryParse(parts[0].toLowerCase(Locale.ROOT)+":"+parts[1].toLowerCase(Locale.ROOT));
		if (id == null)
			return null;
		return BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR)
				.<Supplier<ItemStack>>map(item -> () -> new ItemStack(item)).orElse(null);
	}

	public static int getAccelFromTier(int tier) {
		return ReikaMathLibrary.intpow2(2, tier+1)-1;
	}

	public int getAccel() {
		return getAccelFromTier(this.getTier());
	}

	@Override
	protected EffectResult tickDirection(Level world, BlockPos pos, Direction dir, long time) {
		if (lagTimer[dir.ordinal()] > 0) {
			// V33a ReikaParticleHelper.CLOUD.spawnAroundBlock(world, x+dx, y+dy, z+dz, 4).
			if (world.isClientSide()) {
				BlockPos at = pos.relative(dir);
				for (int i = 0; i < 4; i++)
					world.addParticle(ParticleTypes.CLOUD, at.getX()+world.getRandom().nextDouble(),
							at.getY()+world.getRandom().nextDouble(), at.getZ()+world.getRandom().nextDouble(), 0, 0, 0);
			}
			lagTimer[dir.ordinal()]--;
		}
		BlockEntity te = this.getEffectiveTileOnSide(dir);
		Acceleration a = getAccelerate(te);
		if (debugLevel > 0) {
			ChromatiCraft.LOGGER.info(te+": "+(a != null ? a.getClass() : null));
		}
		if (a != blacklistKey) {
			int max = this.getAccel();
			if (debugLevel > 1) {
				ChromatiCraft.LOGGER.info(this+" > "+max);
			}
			long maxLag = getMaxLag();
			if (a != defaultKey) {
				try {
					te = a.getActingTileEntity(te);
					a.tick(te, max, this);
				}
				catch (Exception e) {
					e.printStackTrace();
					this.writeError(e);
				}
				if (maxLag > 0 && System.nanoTime()-time >= maxLag) {
					this.logLagWarning(time, dir, te);
					return EffectResult.FINAL_ACTION;
				}
			}
			else {
				// V33a gated this inside TileEntityBase.updateEntity: an accelerated (non-natural) tick of a tile that
				// disallows acceleration returned at once. The port's tile tickers run the real body after that gate
				// regardless, so the gate is applied here, per tile (RotaryCraft's answer depends on the machine), and
				// never cached by class. The core still reports acting on it, as V33a's no-op calls did.
				boolean gated = te instanceof BlockEntityBase base && !base.allowTickAcceleration();
				BlockEntityTicker<BlockEntity> ticker = gated ? null : tickerOf(world, te);
				for (int k = 0; k < max; k++) {
					if (debugLevel > 2) {
						ChromatiCraft.LOGGER.info("Ticked "+te+" "+k+"th time");
					}
					// V33a te.updateEntity(): a tile with no tick simply does nothing. A tile that removed itself
					// on an extra tick is not ticked again (1.7.10 invalid tiles ignored their update calls).
					if (ticker != null && !te.isRemoved())
						ticker.tick(world, te.getBlockPos(), te.getBlockState(), te);
					if (maxLag > 0 && System.nanoTime()-time >= maxLag) {
						this.logLagWarning(time, dir, te);
						return EffectResult.FINAL_ACTION;
					}
				}
			}
			return EffectResult.ACTION;
		}
		return EffectResult.CONTINUE;
	}

	/** The tile's own ticker on this side, as its block hands it to the level (1.7.10 {@code updateEntity}). */
	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> @Nullable BlockEntityTicker<BlockEntity> tickerOf(Level world, T te) {
		BlockEntityTicker<T> t = te.getBlockState().getTicker(world, (BlockEntityType<T>)te.getType());
		return (BlockEntityTicker<BlockEntity>)t;
	}

	private void logLagWarning(long time, Direction dir, BlockEntity te) {
		lagTimer[dir.ordinal()] = 20;
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		if (NBT.contains("lag")) {
			int[] lag = NBT.getIntArray("lag").orElse(null);
			if (lag != null && lag.length == 6)
				lagTimer = lag;
		}
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		NBT.putIntArray("lag", lagTimer);
	}

	private static Acceleration getAccelerate(@Nullable BlockEntity te) {
		if (te == null)
			return blacklistKey;
		if (te instanceof TileEntityAdjacencyUpgrade)
			return blacklistKey;
		if (te.isRemoved())
			return blacklistKey;
		return getAccelerate(te.getClass());
	}

	private static Acceleration getAccelerate(Class<?> c) {
		Acceleration a = actions.get(c);
		if (a != null)
			return a;
		a = calculateAccelerate(c);
		if (a == blacklistKey)
			ChromatiCraft.LOGGER.info("Calculated acceleration blacklist "+a+" for tile class "+c);
		else
			ChromatiCraft.LOGGER.info("Calculated acceleration mode "+a+" for tile class "+c);
		actions.put(c, a);
		return a;
	}

	private static Acceleration calculateAccelerate(Class<?> c) {
		if (TileEntityChromaticBase.class.isAssignableFrom(c))
			return defaultKey;
		Class<?> parent = c.getSuperclass();
		if (parent == null || parent == BlockEntity.class || parent == Object.class)
			return defaultKey;
		Acceleration a = getAccelerate(parent);
		if (a != null)
			return a;
		String s = c.getSimpleName().toLowerCase(Locale.ENGLISH);
		if (s.contains("conduit") || (s.contains("wire") && !s.contains("wireless")) || s.contains("cable")) { //almost always part of a network object
			return blacklistKey;
		}
		if (s.contains("solar")) { //power exploit
			return blacklistKey;
		}
		if (s.contains("windturbine") || s.contains("wind turbine") || s.contains("windmill") || s.contains("wind mill")) { //power exploit
			return blacklistKey;
		}
		if (s.contains("watermill") || s.contains("water mill")) { //power exploit
			return blacklistKey;
		}
		if (c.getName().contains("appeng")) { //AE will crash
			return blacklistKey;
		}
		return defaultKey;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public CrystalElement getColor() {
		return CrystalElement.LIGHTBLUE;
	}

	public static abstract class Acceleration {

		protected Acceleration() {

		}

		protected abstract void tick(BlockEntity te, int factor, BlockEntity accelerator) throws Exception;

		public abstract boolean usesParentClasses();

		protected final void registerClass(String sg) {
			try {
				this.registerClass(Class.forName(sg));
			}
			catch (Exception e) {
				//e.printStackTrace();
			}
		}

		protected final void registerClass(Class<?> c) {
			TileEntityAccelerator.customizeTile(c, this);
		}

		protected BlockEntity getActingTileEntity(BlockEntity root) throws Exception {
			return root;
		}

	}

	public static abstract class SpecialAcceleration extends Acceleration {

		protected SpecialAcceleration() {
			TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIGHTBLUE, this.getDescription()).addDisplays(this.getRelevantItems());
		}

		public abstract String getDescription();

		public abstract Collection<EffectDisplay> getRelevantItems();

	}

	private static final class APIAcceleration extends Acceleration {

		private final CustomAcceleration accel;

		private APIAcceleration(CustomAcceleration acc) {
			accel = acc;
			List<Supplier<ItemStack>> li = new ArrayList<>();
			for (ItemStack is : acc.getItems())
				li.add(() -> is);
			TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIGHTBLUE, acc.getDescription()).addItems(li);
		}

		@Override
		protected void tick(BlockEntity te, int factor, BlockEntity accelerator) {
			accel.tick(factor);
		}

		@Override
		public boolean usesParentClasses() {
			return accel.usesParentClasses();
		}

	}

}
