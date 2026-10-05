/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.auxiliary.FluidNetwork;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.block.BlockFluidRelay;
import reika.chromaticraft.container.MenuFluidRelay;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.*;

/**
 * V33a {@code TileEntityFluidRelay} ("Fluid Link Anchor"). It attaches to a tank face and links to every other anchor
 * within 16 blocks. Anchors with positive pressure push the fluids in their filter out of their tank into the network;
 * negative ones pull them in. Pressure is a base value plus a per-bucket term times the filtered fluid in the tank,
 * and throughput is 200 + 4|pressure| mB a tick, capped at 2000.
 *
 * <p>The facing (toward the tank) is block state, so the block's shape and survival can read it; V33a stored it on
 * the tile. V33a's {@code IFluidHandler} tank is now the neighbour's {@code Capabilities.Fluid.BLOCK} handler on the
 * touching face, with each transfer simulated in an uncommitted transaction first and then committed for the exact
 * amount, as V33a's drain(false)/drain(true) pairs did. Filters store source-fluid registry ids.
 */
public class TileEntityFluidRelay extends TileEntityChromaticBase implements BreakAction, MenuProvider {

	private final StepTimer cacheTimer = new StepTimer(100);

	public static final int RELAY_RANGE = 16;
	public static final int MAX_THROUGHPUT = 2000;

	private final HashSet<BlockPos> connections = new HashSet<>();

	private FluidNetwork network;

	private final HashSet<Fluid> fluidTypes = new HashSet<>();
	private final Fluid[] fluidAccess = new Fluid[7];

	private int basePressure;
	private int pressureFunction;

	private final StepTimer pressureUpdate = new StepTimer(8);
	private int lastPressure;
	private int currentPressure;

	public boolean autoFilter = false;

	public TileEntityFluidRelay(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.FLUID_RELAY.get(), pos, state);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!world.isClientSide()) {
			cacheTimer.setCap(connections.isEmpty() ? 100 : 500);
			cacheTimer.update();
		}
		if (cacheTimer.checkCap() || this.getTicksExisted() == 0) {
			this.scanAndCache(world, pos);
		}
		if (!world.isClientSide()) {
			lastPressure = currentPressure;
			currentPressure = this.getBasePressure()+this.getDynamicPressure();
			if (this.getFunctionPressure() != 0) {
				pressureUpdate.update();
				if (pressureUpdate.checkCap()) {
					if (currentPressure != lastPressure) {
						network.updateState(this);
					}
				}
			}
			if (autoFilter) {
				if (this.getTicksExisted()%10 == 0) {
					this.copyFilters();
				}
			}
			network.update(world);
		}
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		if (!world.isClientSide()) {
			network = new FluidNetwork();
			network.add(this);
		}
	}

	/** Toward the tank this anchor is fixed to (V33a {@code getFacing}, default DOWN). */
	public Direction getFacing() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockFluidRelay.FACING) ? state.getValue(BlockFluidRelay.FACING) : Direction.DOWN;
	}

	/**
	 * V33a scanned every block within {@link #RELAY_RANGE} for another anchor, on both sides (the client for the
	 * beams). Unloaded positions are skipped so the scan never loads chunks.
	 */
	private void scanAndCache(Level world, BlockPos pos) {
		connections.clear();
		int r = RELAY_RANGE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int i = -r; i <= r; i++) {
			for (int k = -r; k <= r; k++) {
				for (int j = -r; j <= r; j++) {
					if ((i != 0 || k != 0 || j != 0) && ReikaMathLibrary.py3d(i, j, k) <= r) {
						p.setWithOffset(pos, i, j, k);
						if (!world.isLoaded(p) || !world.getBlockState(p).is(this.getBlockState().getBlock()))
							continue;
						if (world.getBlockEntity(p) instanceof TileEntityFluidRelay te) {
							BlockPos c = p.immutable();
							connections.add(c);
							te.connections.add(pos);
							if (!world.isClientSide()) {
								network.merge(te.network, world);
							}
						}
					}
				}
			}
		}
	}

	public Collection<BlockPos> getConnections() {
		return Collections.unmodifiableCollection(connections);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.FLUIDRELAY;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		NBT.putInt("press", basePressure);
		NBT.putInt("func", pressureFunction);
		NBT.putBoolean("auto", autoFilter);

		ListTag li = new ListTag();
		for (int i = 0; i < fluidAccess.length; i++) {
			Fluid f = fluidAccess[i];
			li.add(StringTag.valueOf(f != null ? BuiltInRegistries.FLUID.getKey(f).toString() : "empty"));
		}
		NBT.put("fluids", li);

		ListTag cl = new ListTag();
		for (BlockPos c : connections) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("x", c.getX());
			tag.putInt("y", c.getY());
			tag.putInt("z", c.getZ());
			cl.add(tag);
		}
		NBT.put("connections", cl);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		basePressure = NBT.getIntOr("press", 0);
		pressureFunction = NBT.getIntOr("func", 0);
		autoFilter = NBT.getBooleanOr("auto", false);

		fluidTypes.clear();
		ListTag li = NBT.getListOrEmpty("fluids");
		for (int i = 0; i < fluidAccess.length; i++) {
			Fluid f = i < li.size() ? fluidFromId(li.getStringOr(i, "empty")) : null;
			fluidAccess[i] = f;
			if (f != null)
				fluidTypes.add(f);
		}

		connections.clear();
		ListTag cl = NBT.getListOrEmpty("connections");
		for (int i = 0; i < cl.size(); i++) {
			CompoundTag tag = cl.getCompoundOrEmpty(i);
			connections.add(new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0)));
		}
	}

	private static @Nullable Fluid fluidFromId(String s) {
		if (s == null || s.equals("empty"))
			return null;
		Identifier id = Identifier.tryParse(s);
		if (id == null)
			return null;
		Fluid f = BuiltInRegistries.FLUID.getValue(id);
		return f == Fluids.EMPTY ? null : f;
	}

	public int getBasePressure() {
		return basePressure;
	}

	public int getFunctionPressure() {
		return pressureFunction;
	}

	private int getDynamicPressure() {
		ResourceHandler<FluidResource> ifl = this.getTank();
		if (ifl == null)
			return 0;
		int sum = 0;
		for (int i = 0; i < ifl.size(); i++) {
			FluidResource res = ifl.getResource(i);
			if (!res.isEmpty() && fluidTypes.contains(res.getFluid())) {
				sum += ifl.getAmountAsInt(i);
			}
		}
		return this.getFunctionPressure()*sum/1000;
	}

	/** V33a RELAYFLUID: players within 24 blocks see the fluid stream along every link. */
	private void renderFluid(Fluid f) {
		if (level instanceof ServerLevel sl) {
			ChromaNetwork.sendFluidRelayFlow(sl, worldPosition, f);
		}
	}

	public void pushFluids() {
		if (network == null)
			return;
		ResourceHandler<FluidResource> ifl = this.getTank();
		if (ifl == null)
			return;
		for (Fluid f : new ArrayList<>(fluidTypes)) {
			FluidResource res = FluidResource.of(f);
			int max = extract(ifl, res, Integer.MAX_VALUE, false);
			if (max > 0) {
				int ret = network.push(level, f, Math.min(max, this.getThroughput()));
				if (ret > 0) {
					extract(ifl, res, ret, true);
					this.renderFluid(f);
				}
			}
		}
	}

	public void suckFluids() {
		if (network == null)
			return;
		ResourceHandler<FluidResource> ifl = this.getTank();
		if (ifl == null)
			return;
		for (Fluid f : new ArrayList<>(fluidTypes)) {
			FluidResource res = FluidResource.of(f);
			int max = insert(ifl, res, Integer.MAX_VALUE, false);
			if (max > 0) {
				max = Math.min(this.getThroughput(), max);
				int ret = network.suck(level, f, max);
				if (ret > 0) {
					insert(ifl, res, ret, true);
					this.renderFluid(f);
				}
			}
		}
	}

	private static int extract(ResourceHandler<FluidResource> h, FluidResource res, int amt, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int n = h.extract(res, amt, tx);
			if (commit)
				tx.commit();
			return n;
		}
	}

	private static int insert(ResourceHandler<FluidResource> h, FluidResource res, int amt, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int n = h.insert(res, amt, tx);
			if (commit)
				tx.commit();
			return n;
		}
	}

	public Fluid[] getFluidTypes() {
		return Arrays.copyOf(fluidAccess, fluidAccess.length);
	}

	/** V33a: the {@code IFluidHandler} tile in the facing direction, asked through the side touching this anchor. */
	public @Nullable ResourceHandler<FluidResource> getTank() {
		if (level == null)
			return null;
		Direction dir = this.getFacing();
		return level.getCapability(Capabilities.Fluid.BLOCK, worldPosition.relative(dir), dir.getOpposite());
	}

	public int pushFluid(Fluid f, int amt) {
		if (!fluidTypes.contains(f))
			return 0;
		ResourceHandler<FluidResource> ifl = this.getTank();
		if (ifl != null) {
			int ret = insert(ifl, FluidResource.of(f), amt, true);
			if (ret > 0)
				this.renderFluid(f);
			return ret;
		}
		else {
			return 0;
		}
	}

	public int suckFluid(Fluid f, int amt) {
		if (!fluidTypes.contains(f))
			return 0;
		ResourceHandler<FluidResource> ifl = this.getTank();
		if (ifl != null) {
			int ret = extract(ifl, FluidResource.of(f), amt, true);
			if (ret > 0)
				this.renderFluid(f);
			return ret;
		}
		else {
			return 0;
		}
	}

	@Override
	public void breakBlock() {
		if (network != null)
			network.remove(this);
	}

	public void assignNetwork(FluidNetwork net) {
		network = net;
	}

	public int getThroughput() {
		return Math.min(MAX_THROUGHPUT, 200+Math.abs(4*this.getCurrentPressure()));
	}

	public int getCurrentPressure() {
		return currentPressure;
	}

	public void changeBasePressure(int dp) {
		basePressure += dp;
		currentPressure = this.getBasePressure()+this.getDynamicPressure();
		if (network != null)
			network.updateState(this);
		this.syncAllData(false);
	}

	public void changeFunctionPressure(int dp) {
		pressureFunction += dp;
		currentPressure = this.getBasePressure()+this.getDynamicPressure();
		if (network != null)
			network.updateState(this);
		this.syncAllData(false);
	}

	public void toggleAutoFilter() {
		autoFilter = !autoFilter;
		this.syncAllData(false);
	}

	public void clearFilters() {
		for (int i = 0; i < 7; i++) {
			fluidAccess[i] = null;
		}
		this.rebuildFluidLists();
	}

	/** V33a: the filter becomes the fluids currently in the tank, in tank order, up to seven. */
	public void copyFilters() {
		for (int i = 0; i < 7; i++) {
			fluidAccess[i] = null;
		}
		ResourceHandler<FluidResource> ifl = this.getTank();
		if (ifl != null) {
			int idx = 0;
			for (int i = 0; i < ifl.size(); i++) {
				FluidResource res = ifl.getResource(i);
				if (!res.isEmpty() && ifl.getAmountAsInt(i) > 0) {
					fluidAccess[idx] = res.getFluid();
					idx++;
					if (idx >= 7)
						break;
				}
			}
		}
		this.rebuildFluidLists();
	}

	public void setFluid(int slot, @Nullable Fluid f) {
		if (fluidAccess[slot] != f) {
			fluidAccess[slot] = f;
			this.rebuildFluidLists();
		}
	}

	private void rebuildFluidLists() {
		HashSet<Fluid> old = new HashSet<>(fluidTypes);
		fluidTypes.clear();
		for (int i = 0; i < fluidAccess.length; i++) {
			Fluid in = fluidAccess[i];
			if (in != null)
				fluidTypes.add(in);
		}
		if (network != null && !old.equals(fluidTypes)) {
			currentPressure = this.getBasePressure()+this.getDynamicPressure();
			network.updateState(this);
		}
		this.setChanged();
		if (level != null && !level.isClientSide())
			this.syncAllData(false);
	}

	@Override
	public Component getDisplayName() {
		return this.getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuFluidRelay(id, inventory, this);
	}

}
