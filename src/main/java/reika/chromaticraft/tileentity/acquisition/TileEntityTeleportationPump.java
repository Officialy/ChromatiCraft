/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.acquisition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import reika.chromaticraft.auxiliary.interfaces.AdjacencyUpgrade;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.ChargedCrystalPowered;
import reika.chromaticraft.container.MenuTelePump;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.auxiliary.ChunkManager;
import reika.dragonapi.instantiable.HybridTank;
import reika.dragonapi.instantiable.data.maps.CountMap;
import reika.dragonapi.instantiable.storage.HybridTankResourceHandler;
import reika.dragonapi.interfaces.blockentity.ChunkLoadingTile;
import reika.dragonapi.libraries.ReikaFluidHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.registry.ReikaParticleHelper;

/**
 * V33a Teleportation Pump: from a storage crystal's cyan/black/lime lumens it pulls fluid source blocks from anywhere
 * within 256 blocks (more with a lime Range Boost) straight into its 4-bucket tank, which empties into adjacent tanks
 * and fills a bucket.
 * <ul>
 * <li>First it scans the area layer by layer from the bottom of the world up, one layer a tick (with a Range Boost, a
 * layer only one tick in {@code ceil(range/256)}); on finishing it chimes, and each fluid's sources are shuffled and
 * sorted top first. A pump reloaded after its scan finished rebuilds its list 32 layers a tick.</li>
 * <li>Its GUI (closed while scanning) lists every fluid found with its total; picking one sets the target. Each tick
 * after that, if the top source of it is still that fluid, the tank has room and the crystal has 250 cyan, 100 black
 * and 500 lime (scaled by the black efficiency upgrade), the source is taken and removed. Water counts a tenth.</li>
 * </ul>
 * <p>26.3: the scan covers the level's build height (V33a: y 0-255). Like V33a it reads every column in range, loading
 * (and generating) chunks it reaches; it keeps none loaded (V33a only ever released them, on break). V33a scaled the
 * energy cost by the efficiency upgrade twice (here and in {@code useEnergy}); that is kept.
 */
public class TileEntityTeleportationPump extends ChargedCrystalPowered implements OwnedTile, ChunkLoadingTile, MenuProvider {

	public static final int MAXRANGE = 256;

	private final HybridTank tank = new HybridTank("telepump", 4000);
	private final ResourceHandler<FluidResource> fluidHandler = new HybridTankResourceHandler(new HybridTank[] {tank},
			(i, r) -> false, (i, r) -> ReikaFluidHelper.isFluidDrainableFromTank(r.toStack(1), tank), this::setChanged);
	private HashMap<Fluid, ArrayList<FluidSource>> fluids = new HashMap<>();
	private CountMap<Fluid> counts = new CountMap<>();

	private @Nullable Fluid selected = null;

	private boolean scanning = true;
	private boolean fastscan = false;
	private int scanY = Integer.MIN_VALUE;

	private int range = MAXRANGE;
	private int testRange = -1;

	private static final ElementTagCompound required = new ElementTagCompound();

	private static final Comparator<FluidSource> comparator = (o1, o2) -> o2.location.getY()-o1.location.getY(); //higher first

	static {
		required.addTag(CrystalElement.CYAN, 250);
		required.addTag(CrystalElement.BLACK, 100);
		required.addTag(CrystalElement.LIME, 500);
	}

	public TileEntityTeleportationPump(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.TELEPUMP.get(), pos, state);
	}

	/** V33a {@code liquidProgress}: Ethereal Luma shows as unknown until the player has touched it. */
	public static boolean isFluidDiscovered(Fluid f, Player ep) {
		ProgressStage p = f == ChromaFluids.LUMA.get() ? ProgressStage.LUMA : null;
		return p == null || p.isPlayerAtStage(ep);
	}

	@Override
	public ElementTagCompound getRequiredEnergy() {
		return required.copy();
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		this.updateRange();
	}

	@Override
	public void onAdjacentUpdate(Level world, BlockPos pos, Block b) {
		this.updateRange();
		super.onAdjacentUpdate(world, pos, b);
	}

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/** V33a IFluidHandler: drain-only, any side. */
	public ResourceHandler<FluidResource> fluidHandler() {
		return fluidHandler;
	}

	@Override
	protected boolean canExtractOtherItem(int slot, ItemStack is, Direction side) {
		switch(slot) {
			case 1:
				return is.getItem() instanceof BucketItem && !is.is(Items.BUCKET);
			default:
				return false;
		}
	}

	@Override
	protected boolean isItemValidForOtherSlot(int slot, ItemStack is) {
		switch(slot) {
			case 1:
				return is.is(Items.BUCKET);
			default:
				return false;
		}
	}

	public Set<Fluid> getFluids() {
		return Collections.unmodifiableSet(counts.keySet());
	}

	public int getFluidCount(Fluid f) {
		return counts.containsKey(f) ? counts.get(f) : 0;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.TELEPUMP;
	}

	public void setTargetedFluid(@Nullable Fluid f) {
		selected = f;
		this.setChanged();
	}

	public @Nullable Fluid getTargetedFluid() {
		return selected;
	}

	public boolean isScanning() {
		return scanning;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (scanning && world.isClientSide()) {
			this.doScanFX(world, pos);
		}
		if (world.isClientSide())
			return;
		int minY = world.getMinY();
		int maxY = world.getMaxY();
		if (scanY < minY)
			scanY = minY;
		int boost = (int)Math.ceil(this.getRange()/(float)MAXRANGE);
		if (boost == 1 && !scanning && fluids.isEmpty() && this.getTicksExisted() == 0) {
			fastscan = true;
		}
		if (scanning || fastscan) {
			int n = world.getRandom().nextInt(boost);
			while (n == 0 || (fastscan && n < 32)) {
				int r = this.getRange();
				BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
				for (int i = -r; i <= r; i++) {
					for (int k = -r; k <= r; k++) {
						at.set(pos.getX()+i, scanY, pos.getZ()+k);
						FluidStack fs = this.getDrainableFluid(world, at);
						if (fs != null) {
							this.addFluidBlock(at.immutable(), fs);
						}
					}
				}
				scanY++;
				if (scanY > maxY) {
					scanning = false;
					fastscan = false;
					ChromaSounds.CRAFTDONE.playSoundAtBlock(this, 1, 1.25F);
					for (ArrayList<FluidSource> li : fluids.values()) {
						Collections.shuffle(li);
						li.sort(comparator);
					}
					this.syncAllData(false);
					break;
				}
				n++;
			}
		}
		else {
			if (selected != null) {
				ArrayList<FluidSource> li = fluids.get(selected);
				if (li != null && !li.isEmpty()) {
					FluidSource src = li.get(0);
					if (src.matchInWorld(world)) {
						if (this.canAddFluid(src.amount, selected) && this.hasEnergy(required)) {
							tank.addLiquid(src.amount, selected);
							world.setBlock(src.location, Blocks.AIR.defaultBlockState(), 3);
							this.useEnergy(required.copy().scale(this.getEnergyCostScale()));
							li.remove(0);
							this.decrFluid(src.amount, selected);
						}
					}
				}

				FluidStack f = tank.getActualFluid();
				if (f != null && !f.isEmpty()) {
					if (tank.getFluidLevel() >= 1000) {
						this.fillBucket(f.getFluid());
					}
					if (!tank.isEmpty()) {
						for (Direction dir : Direction.values()) {
							ResourceHandler<FluidResource> ifl = world.getCapability(Capabilities.Fluid.BLOCK,
									pos.relative(dir), dir.getOpposite());
							if (ifl != null) {
								int amt;
								try (Transaction tx = Transaction.openRoot()) {
									amt = ifl.insert(FluidResource.of(tank.getFluid()), tank.getFluidLevel(), tx);
									tx.commit();
								}
								if (amt > 0) {
									tank.removeLiquid(amt);
									if (tank.isEmpty())
										break;
								}
							}
						}
					}
				}
			}
		}
	}

	private @Nullable FluidStack getDrainableFluid(Level world, BlockPos pos) {
		FluidStack ret = ReikaWorldHelper.getDrainableFluid(world, pos);
		if (ret == null)
			return null;
		ret = ret.copy();
		if (ret.getFluid() == Fluids.WATER)
			ret.setAmount(ret.getAmount()/10);
		return ret.getAmount() > 0 ? ret : null;
	}

	private void fillBucket(Fluid f) {
		ItemStack in = inv.get(1);
		if (in.is(Items.BUCKET)) {
			ItemStack is = new ItemStack(f.getBucket());
			if (!is.isEmpty() && is.getItem() != Items.AIR) {
				inv.set(1, is);
				tank.removeLiquid(1000);
				this.setChanged();
			}
		}
	}

	private void decrFluid(int rem, Fluid f) {
		counts.increment(f, -rem);
	}

	@Override
	public boolean isPlayerAccessible(Player var1) {
		return !scanning && super.isPlayerAccessible(var1);
	}

	/** V33a doScanFX: portal particles about the block, and on Minimal particles a falling purple fade ray. */
	private void doScanFX(Level world, BlockPos pos) {
		ReikaParticleHelper.PORTAL.spawnAroundBlock(world, pos, 8);
		if (Minecraft.getInstance().options.particles().get().ordinal() == 2)
			ChromaParticle.spawnTelePumpScan(world, pos, world.getRandom());
	}

	public int getRange() {
		return range;
	}

	/** GameTests only: the real range is 256 blocks and more, which would load thousands of chunks in a test. */
	public void overrideRangeForTest(int r) {
		testRange = r;
		range = r;
	}

	private void addFluidBlock(BlockPos pos, FluidStack fs) {
		Fluid f = fs.getFluid();
		ArrayList<FluidSource> li = fluids.get(f);
		if (li == null) {
			li = new ArrayList<>();
			fluids.put(f, li);
			counts.set(f, 0);
		}
		li.add(new FluidSource(pos, f, fs.getAmount()));
		counts.increment(f, fs.getAmount());
	}

	public int getLiquidScaled(int a) {
		return a * tank.getFluidLevel() / tank.getCapacity();
	}

	public @Nullable Fluid getTankFluid() {
		FluidStack fs = tank.getActualFluid();
		return fs != null && !fs.isEmpty() ? fs.getFluid() : null;
	}

	public int getTankLevel() {
		return tank.getFluidLevel();
	}

	private boolean canAddFluid(int amt, Fluid f) {
		return tank.canTakeIn(amt);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		tank.writeToNBT(NBT);

		NBT.putBoolean("scan", scanning);
		NBT.putString("fluid", selected != null ? BuiltInRegistries.FLUID.getKey(selected).toString() : "null");

		CompoundTag tag = new CompoundTag();
		for (Fluid f : counts.keySet()) {
			tag.putInt(BuiltInRegistries.FLUID.getKey(f).toString(), counts.get(f));
		}
		NBT.put("counts", tag);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		tank.readFromNBT(NBT);

		scanning = NBT.getBooleanOr("scan", true);
		String fid = NBT.getStringOr("fluid", "null");
		selected = fid.equals("null") ? null : fluidOf(fid);

		CompoundTag tag = NBT.getCompoundOrEmpty("counts");
		counts.clear();
		for (String s : tag.keySet()) {
			Fluid f = fluidOf(s);
			if (f != null) {
				counts.set(f, tag.getIntOr(s, 0));
			}
		}
	}

	private static @Nullable Fluid fluidOf(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null)
			return null;
		Fluid f = BuiltInRegistries.FLUID.getValue(key);
		return f == Fluids.EMPTY ? null : f;
	}

	@Override
	public float getCostModifier() {
		return 1;
	}

	@Override
	public boolean usesColor(CrystalElement e) {
		return required.contains(e);
	}

	@Override
	public void breakBlock() {
		ChunkManager.instance.unloadChunks(this);
	}

	@Override
	public Collection<ChunkPos> getChunksToLoad() {
		return ChunkManager.getChunkSquare(worldPosition.getX(), worldPosition.getZ(), range >> 4);
	}

	/** V33a: {@code MAXRANGE} times the lime upgrade's factor for the tier beside it; a rescan starts if it changes mid-scan. */
	private void updateRange() {
		if (level == null)
			return;
		int oldrange = range;
		double r = 1;
		int val = AdjacencyUpgrade.getAdjacentTier(level, worldPosition, CrystalElement.LIME);
		if (val > 0)
			r = reika.chromaticraft.tileentity.aoe.effect.TileEntityRangeBoost.getFactor(val-1);
		range = testRange > 0 ? testRange : (int)(MAXRANGE*r);
		if (scanning && oldrange != range) {
			scanY = level.getMinY();
			fluids.clear();
			counts.clear();
		}
	}

	@Override
	public void getTagsToWriteToStack(CompoundTag NBT) {
		this.writeOwnerData(NBT);
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		this.readOwnerData(is);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void addTooltipInfo(java.util.List li, boolean shift) {

	}

	@Override
	public Component getDisplayName() {
		return this.getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuTelePump(id, inventory, this);
	}

	private static final class FluidSource {

		private final BlockPos location;
		private final Fluid fluid;
		private final int amount;

		private FluidSource(BlockPos pos, Fluid f, int amt) {
			location = pos;
			fluid = f;
			amount = amt;
		}

		/**
		 * V33a: the block there is still this fluid's liquid block, at any level (V33a matched the block, not its
		 * metadata). A waterlogged block now standing there is not taken.
		 */
		public boolean matchInWorld(Level world) {
			BlockState state = world.getBlockState(location);
			if (!(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock))
				return false;
			Fluid found = ReikaFluidHelper.lookupFluidForBlock(state);
			return found != null && found.isSame(fluid);
		}

	}

}
