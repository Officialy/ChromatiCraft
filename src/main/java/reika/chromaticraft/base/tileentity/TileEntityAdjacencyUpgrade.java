/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import com.google.common.base.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.interfaces.AdjacencyCheckHandler;
import reika.chromaticraft.auxiliary.interfaces.AdjacencyUpgrade;
import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.SneakPop;
import reika.chromaticraft.magic.interfaces.AdjacencyUpgradeProvider;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.maps.NestedMap;
import reika.dragonapi.interfaces.registry.TileEnum;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;
import java.util.function.Supplier;

/**
 * V33a {@code TileEntityAdjacencyUpgrade}: the sixteen-colour family of "Tile Accelerator" cores. Each is placed
 * from its own colour's item, carries a tier from 0 to {@link #MAX_TIER}-1, and either acts on the six blocks around
 * it each tick ({@link #tickDirection}) or is read passively by the machines it touches (the
 * {@link AdjacencyCheckHandlerImpl}s and {@link AdjacencyUpgrade#getAdjacentTier}). A redstone signal stops it; with
 * "Adjacency Upgrades Require Energy" on (the default) it pulls its own colour wirelessly and burns
 * {@link #getConsumedEnergy} every eight ticks, and only counts as present to a machine while holding at least 100.
 */
public abstract class TileEntityAdjacencyUpgrade extends TileEntityWirelessPowered
		implements NBTTile, SneakPop, AdjacencyUpgrade, AdjacencyUpgradeProvider {

	public static final int MAX_TIER = 8;

	private static final NestedMap<CrystalElement, String, AdjacencyEffectDescription> effectMap = new NestedMap<>();
	private static final EnumMap<CrystalElement, AdjacencyCheckHandlerImpl> adjacencyChecks = new EnumMap<>(CrystalElement.class);

	private int tier;

	private boolean particles = true;
	private int soundtick = 0;

	protected TileEntityAdjacencyUpgrade(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final int getTier() {
		return tier;
	}

	@Override
	public int getMaxStorage(CrystalElement e) {
		return e == this.getColor() ? 6000+(1+tier)*(1+tier)*1000 : 0;
	}

	@Override
	protected int getReceiveRange(CrystalElement e) {
		return e == this.getColor() ? 16 : 0;
	}

	public final boolean hasSufficientEnergy() {
		return energy.getValue(this.getColor()) >= this.getConsumedEnergy();
	}

	public final int getConsumedEnergy() {
		return ChromaOptions.POWEREDACCEL.getState() ? Math.max(1, ReikaMathLibrary.intpow2(tier+1, 2)/4) : 0;
	}

	@Override
	public final void updateEntity(Level world, BlockPos pos) {
		if (!this.canRun(world, pos)) {
			soundtick = 0;
			return;
		}

		if (particles && world.isClientSide()) {
			ChromaParticle.spawnAdjacencyUpgrade(world, pos, this.getTier(), world.getRandom());
		}

		soundtick++;

		float f = 1+(1+this.getTier())/(float)MAX_TIER;
		int l = (int)(221/f);
		if (soundtick%l == 0)
			ChromaSounds.DRONE.playSoundAtBlock(world, pos, 0.25F, f);

		if (ChromaOptions.POWEREDACCEL.getState()) {
			CrystalElement e = this.getColor();
			// 1.7.10 ran the request and the drain on both sides; the client's copy of the energy is the server's,
			// synced, so only the server may take from a source or burn its own store.
			if (!world.isClientSide() && this.getTicksExisted()%8 == 0) {
				if (energy.getValue(e) < this.getMaxStorage(e))
					this.requestEnergy(this.getColor(), this.getMaxStorage(e)-energy.getValue(e));
			}

			if (this.hasSufficientEnergy()) {
				if (!world.isClientSide() && this.getTicksExisted()%8 == 0) {
					energy.subtract(e, this.getConsumedEnergy());
					this.setChanged();
				}
			}
			else
				return;
		}

		if (this.ticksIndividually()) {
			long time = System.nanoTime();
			for (int i = 0; i < 6; i++) {
				Direction dir = dirs[i];
				EffectResult res = this.tickDirection(world, pos, dir, time);
				if (res.didAction) {
					if (world.isClientSide() && world.getRandom().nextInt(4) == 0)
						ChromaParticle.spawnAdjacencyAction(world, pos, dir, this.getColor().color(), world.getRandom());
				}
				if (!res.shouldContinue)
					break;
			}
		}
		else {
			this.doCollectiveTick(world, pos);
		}
	}

	/**
	 * V33a: the tile on that face, relayed through a Thaumic Tinkerer transvector and resolved to its multiblock's
	 * controller.
	 * DRAGONAPI-PORT: {@code TransvectorHandler} (Thaumic Tinkerer) and {@code MultiblockControllerFinder} (the 1.7.10
	 * Immersive Engineering and Railcraft multiblock part classes) have no 26.3 targets; the face's own tile is used.
	 */
	protected final BlockEntity getEffectiveTileOnSide(Direction dir) {
		return this.getAdjacentBlockEntity(dir);
	}

	protected abstract EffectResult tickDirection(Level world, BlockPos pos, Direction dir, long startTime);

	public boolean canRun(Level world, BlockPos pos) {
		return !this.hasRedstoneSignal();
	}

	protected boolean ticksIndividually() {
		return true;
	}

	protected void doCollectiveTick(Level world, BlockPos pos) {

	}

	@Override
	public abstract CrystalElement getColor();

	/** {@link AdjacencyUpgrade}: V33a's provider gates, no redstone and (when powered) at least 100 of its colour. */
	@Override
	public final boolean isOperating() {
		return this.canRun(level, worldPosition) && (!ChromaOptions.POWEREDACCEL.getState() || energy.containsAtLeast(this.getColor(), 100));
	}

	@Override
	public final CrystalElement getAdjacencyColor() {
		return this.getColor();
	}

	@Override
	public final int getAdjacencyTier() {
		return this.getTier();
	}

	@Override
	public final boolean isAdjacencyUpgradeActive() {
		return this.isOperating();
	}

	/** Test hook: tops this upgrade's own colour up to its capacity, as a wireless source would. */
	public final void fillEnergyForTest() {
		energy.setTag(this.getColor(), this.getMaxStorage(this.getColor()));
		this.setChanged();
	}

	/** The stored {@code tier}, from the stack's tier component (V33a's {@code tier} stack tag). */
	public static int getTier(ItemStack is) {
		Integer t = is.get(ChromaDataComponents.ADJACENCY_TIER.get());
		return t != null ? t : 0;
	}

	@Override
	public final void setDataFromItemStackTag(ItemStack is) {
		super.setDataFromItemStackTag(is);
		if (is.getItem() instanceof reika.chromaticraft.items.ItemAdjacencyPlacer) {
			tier = getTier(is);
			this.syncAllData(true);
		}
	}

	/** V33a {@code getTagsToWriteToStack} wrote the tier beside the energy and owners; here it is the component. */
	public final ItemStack writeToStack(ItemStack is) {
		CompoundTag tag = new CompoundTag();
		this.getTagsToWriteToStack(tag);
		if (!tag.isEmpty())
			ReikaItemHelper.setStackTag(is, tag);
		is.set(ChromaDataComponents.ADJACENCY_TIER.get(), this.getTier());
		return is;
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		tier = NBT.getIntOr("tier", 0);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		NBT.putInt("tier", tier);
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);

		NBT.putBoolean("particle", particles);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);

		particles = NBT.getBooleanOr("particle", true);
	}

	/** V33a {@code drop}: the colour's item with this tile's data, then the block goes. */
	@Override
	public final void drop() {
		ItemStack is = this.writeToStack(AdjacencyUpgrades.upgrades[this.getColor().ordinal()].getStackOfTier(this.getTier()));
		ReikaItemHelper.dropItem(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, is);
		this.delete();
	}

	@Override
	public final boolean canDrop(Player ep) {
		return ep.getUUID().equals(placerUUID);
	}

	@Override
	public boolean allowMining(Player ep) {
		return true;
	}

	@Override
	public final ChromaTiles getTile() {
		return ChromaTiles.ADJACENCY;
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition);
	}

	protected static int getAdjacentUpgrade(BlockEntity core, CrystalElement color) {
		Integer ret = getAdjacentUpgrades(core.getLevel(), core.getBlockPos()).get(color);
		return ret != null ? ret.intValue() : 0;
	}

	protected static int getAdjacentUpgrade(Level world, BlockPos pos, CrystalElement color) {
		Integer ret = getAdjacentUpgrades(world, pos).get(color);
		return ret != null ? ret.intValue() : 0;
	}

	/**
	 * V33a {@code getAdjacentUpgrades}: for each colour, the highest {@code 1+tier} among operating upgrades on the six
	 * faces.
	 * DRAGONAPI-PORT: V33a relayed each face through a Thaumic Tinkerer transvector first; that mod has no 26.3 port.
	 */
	public static HashMap<CrystalElement, Integer> getAdjacentUpgrades(Level world, BlockPos pos) {
		HashMap<CrystalElement, Integer> set = new HashMap<>();
		for (Direction dir : Direction.values()) {
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof TileEntityAdjacencyUpgrade ta && ta.isOperating()) {
				Integer get = set.get(ta.getColor());
				int has = get != null ? get.intValue() : 0;
				set.put(ta.getColor(), Math.max(1+ta.getTier(), has));
			}
		}
		return set;
	}

	public static AdjacencyCheckHandler createAdjacencyCheckHandler(CrystalElementProxy color, String desc, ItemStack... items) {
		List<Supplier<ItemStack>> li = new ArrayList<>();
		for (ItemStack is : items)
			li.add(() -> is);
		return getOrCreateAdjacencyCheckHandler(CrystalElement.elements[color.ordinal()], desc, li);
	}

	/**
	 * V33a took {@code te.getCraftedProduct()} here, but its callers are static field initialisers that run before
	 * item components are bound, so the product is resolved when the description is first read.
	 */
	public static AdjacencyCheckHandlerImpl getOrCreateAdjacencyCheckHandler(CrystalElement color, String desc, TileEnum te) {
		return getOrCreateAdjacencyCheckHandler(color, desc, List.of(te::getCraftedProduct));
	}

	@SafeVarargs
	public static AdjacencyCheckHandlerImpl getOrCreateAdjacencyCheckHandler(CrystalElement color, String desc, Supplier<ItemStack>... items) {
		return getOrCreateAdjacencyCheckHandler(color, desc, List.of(items));
	}

	public static synchronized AdjacencyCheckHandlerImpl getOrCreateAdjacencyCheckHandler(CrystalElement color, String desc, List<Supplier<ItemStack>> items) {
		AdjacencyCheckHandlerImpl check = adjacencyChecks.get(color);
		if (check == null) {
			check = new AdjacencyCheckHandlerImpl(color);
			adjacencyChecks.put(color, check);
		}
		registerEffectDescription(color, desc).addItems(items);
		return check;
	}

	public static AdjacencyEffectDescription registerEffectDescription(CrystalElement color, String desc) {
		return registerEffectDescription(color, desc, 0);
	}

	public static synchronized AdjacencyEffectDescription registerEffectDescription(CrystalElement color, String desc, int index) {
		AdjacencyEffectDescription ef = effectMap.get(color, desc);
		if (ef == null) {
			ef = new AdjacencyEffectDescription(color, desc).setOrderIndex(index);
			effectMap.put(color, desc, ef);
		}
		return ef;
	}

	public static final class AdjacencyCheckHandlerImpl implements AdjacencyCheckHandler {

		public final CrystalElement color;

		private AdjacencyCheckHandlerImpl(CrystalElement e) {
			color = e;
		}

		@Override
		public int getAdjacentUpgradeTier(Level world, int x, int y, int z) {
			Integer get = TileEntityAdjacencyUpgrade.getAdjacentUpgrades(world, new BlockPos(x, y, z)).get(color);
			return get != null ? get.intValue() : 0;
		}

		@Override
		public double getFactorSimple(Level world, int x, int y, int z) {
			int tier = this.getAdjacentUpgradeTier(world, x, y, z);
			// V33a went through ChromatiAPI.getAPI().adjacency().getFactor, which is AdjacencyUpgrades.getFactor.
			return tier > 0 ? AdjacencyUpgrades.upgrades[color.ordinal()].getFactor(tier) : 1;
		}

		public int getAdjacentUpgrade(BlockEntity te) {
			return TileEntityAdjacencyUpgrade.getAdjacentUpgrade(te, color);
		}

		public int getAdjacentUpgrade(Level world, BlockPos pos) {
			return TileEntityAdjacencyUpgrade.getAdjacentUpgrade(world, pos, color);
		}
	}

	/**
	 * One kind of target in the lexicon's effect list for a colour: V33a's DragonAPI {@code ItemListEffectDescription}
	 * with its {@code GuiItemDisplay} entries. The displays are data here (item suppliers resolved when first read, an
	 * icon with its tooltip, or a struck-through blacklist group) for the lexicon's adjacency page to draw.
	 */
	public static final class AdjacencyEffectDescription implements Comparable<AdjacencyEffectDescription> {

		public final CrystalElement color;
		public final String description;
		private final List<EffectDisplay> displays = new ArrayList<>();
		private int ordering;

		private AdjacencyEffectDescription(CrystalElement e, String s) {
			color = e;
			description = s;
		}

		public AdjacencyEffectDescription setOrderIndex(int index) {
			ordering = index;
			return this;
		}

		public synchronized AdjacencyEffectDescription addItems(Collection<Supplier<ItemStack>> items) {
			for (Supplier<ItemStack> is : items)
				displays.add(new EffectDisplay.Stack(is));
			return this;
		}

		public synchronized AdjacencyEffectDescription addItems(ItemStack... items) {
			for (ItemStack is : items)
				displays.add(new EffectDisplay.Stack(() -> is));
			return this;
		}

		public synchronized AdjacencyEffectDescription addDisplays(EffectDisplay... set) {
			for (EffectDisplay d : set) {
				if (!displays.contains(d))
					displays.add(d);
			}
			return this;
		}

		public synchronized AdjacencyEffectDescription addDisplays(Collection<EffectDisplay> set) {
			return this.addDisplays(set.toArray(new EffectDisplay[0]));
		}

		public synchronized List<EffectDisplay> getRelevantItems() {
			return Collections.unmodifiableList(new ArrayList<>(displays));
		}

		@Override
		public int compareTo(AdjacencyEffectDescription o) {
			if (o.ordering != ordering)
				return Integer.compare(ordering, o.ordering);
			if (Strings.isNullOrEmpty(description) && Strings.isNullOrEmpty(o.description))
				return 0;
			else if (Strings.isNullOrEmpty(description))
				return -1;
			else if (Strings.isNullOrEmpty(o.description))
				return 1;
			else
				return description.compareToIgnoreCase(o.description);
		}

		@Override
		public String toString() {
			return color+": "+description+": "+displays;
		}
	}

	/** V33a's three {@code GuiItemDisplay} kinds used by the adjacency effect lists. */
	public sealed interface EffectDisplay {

		/** {@code GuiStackDisplay}: one item. */
		record Stack(Supplier<ItemStack> item) implements EffectDisplay {}

		/** {@code GuiIconDisplay}, drawn additively, with a tooltip. */
		record Icon(ChromaIcons icon, String tooltip) implements EffectDisplay {}

		/** The accelerator's {@code BlacklistDisplay}: a cycling stack list with a half-alpha red X over it. */
		record Blacklist(List<Supplier<ItemStack>> items) implements EffectDisplay {}
	}

	public static Collection<CrystalElement> getSpecificEffectColors() {
		return Collections.unmodifiableCollection(effectMap.keySet());
	}

	public static Collection<AdjacencyEffectDescription> getSpecificEffects(CrystalElement e) {
		java.util.Map<String, AdjacencyEffectDescription> c = effectMap.getMap(e);
		if (c == null)
			return new ArrayList<>();
		ArrayList<AdjacencyEffectDescription> ret = new ArrayList<>(c.values());
		Collections.sort(ret);
		return ret;
	}

	protected enum EffectResult {
		ACTION(true, true),
		FINAL_ACTION(true, false),
		CONTINUE(false, true),
		STOP(false, false);

		public final boolean didAction;
		public final boolean shouldContinue;

		EffectResult(boolean act, boolean con) {
			didAction = act;
			shouldContinue = con;
		}
	}

}
