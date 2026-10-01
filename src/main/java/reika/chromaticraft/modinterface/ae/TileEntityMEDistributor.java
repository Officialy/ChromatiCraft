/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.modinterface.ae;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.block.BlockMEDistributor;
import reika.chromaticraft.container.MenuMEDistributor;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.modinteract.DirectionalAEInterface;
import reika.dragonapi.instantiable.modinteract.MEWorkTracker;
import reika.dragonapi.interfaces.blockentity.MEGridHost;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;
import reika.dragonapi.modinteract.deepinteract.MESystemReader.ExtractedItemGroup;
import reika.dragonapi.modinteract.deepinteract.MESystemReader.MatchMode;

/**
 * V33a {@code TileEntityMEDistributor}, the ME Item Distributor: mounted on an inventory, it joins the ME network on its
 * outer face only and keeps the inventory stocked. Each of its ten rules pairs a "query" item with a "transfer" item:
 * while the network holds fewer of the query item (by the rule's {@link MatchMode}) than the rule's threshold, it
 * moves the transfer item out of the network into the inventory, as much as fits. Transfers raise its AE power draw,
 * which decays again over time; it checks less often while idle.
 *
 * <p>26.3 port: the AE node is a DragonAPI {@link DirectionalAEInterface} behind the AE-free {@link MEGridHost}; the
 * inventory is reached through the item capability, and each transfer is inserted in a transaction that only commits
 * if the same amount was really taken out of the network.
 */
public class TileEntityMEDistributor extends TileEntityChromaticBase implements MEGridHost, MenuProvider {

	public static final int NSLOTS = 10;

	/** {@code DirectionalAEInterface} when AE2 is loaded; typed Object so AE-less installs never resolve it. */
	private final Object aeGridBlock;
	private MESystemReader network;
	private int AEPowerCost = 1;
	@Nullable
	private Direction exposedFace;

	private final StepTimer checkTimer = new StepTimer(10);
	private final StepTimer cacheTimer = new StepTimer(40);

	/**
	 * V33a layout: slot i (0..NSLOTS-1) is the item counted against the threshold, slot i+NSLOTS the item moved into
	 * the inventory. V33a's GUI labels them the other way round ("Transfer" over slot i, "Query" over slot i+NSLOTS);
	 * the GUI keeps those labels, the behaviour follows the tile.
	 */
	private ItemStack[] filter = new ItemStack[NSLOTS * 2];
	private int[] threshold = new int[NSLOTS];
	private MatchMode[] match = new MatchMode[NSLOTS];

	private final MEWorkTracker hasWork = ModList.APPENG.isLoaded() ? new MEWorkTracker() : null;

	public TileEntityMEDistributor(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ME_DISTRIBUTOR.get(), pos, state);
		aeGridBlock = ModList.APPENG.isLoaded() ? this.createAEInterface() : null;
		for (int i = 0; i < match.length; i++) {
			match[i] = MatchMode.EXACT;
		}
		java.util.Arrays.fill(filter, ItemStack.EMPTY);
	}

	private Object createAEInterface() {
		return new DirectionalAEInterface(this, new ItemStack(ChromaBlocks.ME_DISTRIBUTOR.get()));
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.MEDISTRIBUTOR;
	}

	/** V33a getFacing: the side it was placed on, i.e. pointing away from the inventory, toward the ME cable. */
	public Direction getFacing() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockMEDistributor.FACING) ? state.getValue(BlockMEDistributor.FACING) : Direction.UP;
	}

	@Nullable
	@Override
	public Object getAEInterface() {
		return aeGridBlock;
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (aeGridBlock != null) {
			this.updateExposedFace();
			((DirectionalAEInterface)aeGridBlock).onReady();
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (aeGridBlock != null)
			((DirectionalAEInterface)aeGridBlock).destroy();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (aeGridBlock != null)
			((DirectionalAEInterface)aeGridBlock).destroy();
	}

	/** V33a re-ran disconnectAll().connect(facing) every cache pass; re-exposing unchanged sides makes AE re-path. */
	private void updateExposedFace() {
		Direction f = this.getFacing();
		if (f != exposedFace) {
			exposedFace = f;
			((DirectionalAEInterface)aeGridBlock).disconnectAll().connect(f);
		}
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) {
			reika.chromaticraft.render.particle.ChromaParticle.spawnMEDistributorAmbient(world, pos, this.getFacing(), world.getRandom());
			return;
		}
		cacheTimer.update();
		if (cacheTimer.checkCap()) {
			this.buildCache();
		}

		if (aeGridBlock != null) {
			((DirectionalAEInterface)aeGridBlock).setPowerCost(AEPowerCost);
		}
		if (AEPowerCost > 1)
			AEPowerCost -= Math.max(1, AEPowerCost / 40);

		if (network != null) {
			network.tick();
			hasWork.tick();
			checkTimer.update();
			if (hasWork.hasWork()) {
				if (checkTimer.checkCap()) {
					checkTimer.setCap(Math.min(40, checkTimer.getCap() + 2));
					ResourceHandler<ItemResource> out = this.getOutput();
					if (out != null) {
						for (int i = 0; i < NSLOTS; i++) {
							ItemStack f1 = filter[i];
							ItemStack f2 = filter[i + NSLOTS];
							if (!f1.isEmpty() && !f2.isEmpty()) {
								int fit = this.simulateFit(out, f2.copyWithCount(f2.getMaxStackSize()));
								if (fit > 0) {
									hasWork.reset();
									MatchMode mode = this.getMode(i);
									long has = mode.countItems(network, f1);
									int missing = this.getThreshold(i) - (has > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)has);
									if (missing > 0) {
										this.transferItem(f2.copyWithCount(Math.min(Math.min(fit, missing), f2.getMaxStackSize())), out, mode);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/** The inventory it is mounted on. */
	@Nullable
	private ResourceHandler<ItemResource> getOutput() {
		Direction toInv = this.getFacing().getOpposite();
		return level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(toInv), toInv.getOpposite());
	}

	private int simulateFit(ResourceHandler<ItemResource> out, ItemStack is) {
		try (Transaction tx = Transaction.openRoot()) {
			return out.insert(ItemResource.of(is), is.getCount(), tx); //not committed
		}
	}

	public int getThreshold(int i) {
		return threshold[i];
	}

	public MatchMode getMode(int i) {
		return match[i];
	}

	private void buildCache() {
		if (ModList.APPENG.isLoaded() && aeGridBlock != null) {
			this.updateExposedFace();
			MESystemReader was = network;
			network = ((DirectionalAEInterface)aeGridBlock).updateReader(network);
			if (network != was)
				this.buildCallbacks();
		}
	}

	private void buildCallbacks() {
		if (network != null) {
			network.clearCallbacks();
			for (ItemStack pattern : filter) {
				if (!pattern.isEmpty()) {
					network.addCallback(pattern, hasWork);
				}
			}
		}
		if (hasWork != null)
			hasWork.markDirty();
	}

	private void transferItem(ItemStack is, ResourceHandler<ItemResource> out, MatchMode mode) {
		ExtractedItemGroup rem = mode.removeItems(network, is, true, false);
		if (rem != null) {
			ItemStack add = rem.getBiggest().getItem();
			add = add.copyWithCount(Math.min(add.getCount(), is.getCount()));
			int moved = 0;
			try (Transaction tx = Transaction.openRoot()) {
				int inserted = out.insert(ItemResource.of(add), add.getCount(), tx);
				if (inserted > 0) {
					long taken = network.removeItem(add.copyWithCount(inserted), false, true);
					if (taken == inserted) {
						tx.commit();
						moved = inserted;
					}
					else if (taken > 0) { //could not take all of it: give the part already taken back
						network.addItem(add.copyWithCount((int)taken), false);
					}
				}
			}
			if (moved > 0) {
				ChromaNetwork.sendMETransfer((ServerLevel)level, worldPosition, BuiltInRegistries.ITEM.getId(add.getItem()));
				AEPowerCost = Math.min(500, AEPowerCost + Math.max(1, moved / 4));
				checkTimer.setCap(Math.max(4, checkTimer.getCap() - 4));
			}
		}
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		CompoundTag fil = new CompoundTag();
		ReikaNBTHelper.writeInvToNBT(filter, fil);
		for (int i = 0; i < threshold.length; i++) {
			fil.putInt("thresh_" + i, threshold[i]);
			fil.putInt("match_" + i, match[i].ordinal());
		}
		NBT.put("filter", fil);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		CompoundTag fil = NBT.getCompoundOrEmpty("filter");
		ItemStack[] in = ReikaNBTHelper.getInvFromNBT(fil);
		filter = new ItemStack[NSLOTS * 2];
		for (int i = 0; i < filter.length; i++)
			filter[i] = i < in.length && in[i] != null ? in[i] : ItemStack.EMPTY;
		threshold = new int[NSLOTS];
		match = new MatchMode[NSLOTS];
		for (int i = 0; i < NSLOTS; i++) {
			threshold[i] = fil.getIntOr("thresh_" + i, 0);
			match[i] = MatchMode.list[Math.min(fil.getIntOr("match_" + i, 0), MatchMode.list.length - 1)];
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (aeGridBlock != null)
			((DirectionalAEInterface)aeGridBlock).saveNode(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		if (aeGridBlock != null)
			((DirectionalAEInterface)aeGridBlock).loadNode(input);
	}

	public void setMapping(int slot, ItemStack is) {
		filter[slot] = is.isEmpty() ? ItemStack.EMPTY : is.copyWithCount(1);
		if (ModList.APPENG.isLoaded())
			this.buildCallbacks();
		this.syncAllData(true);
	}

	public ItemStack getMapping(int slot) {
		return filter[slot].copy();
	}

	public void setThreshold(int slot, int thresh) {
		threshold[slot] = Math.max(0, thresh);
		if (ModList.APPENG.isLoaded())
			this.buildCallbacks();
		this.syncAllData(true);
	}

	public void toggleFuzzy(int slot) {
		match[slot] = match[slot].next();
		if (ModList.APPENG.isLoaded())
			this.buildCallbacks();
		this.syncAllData(true);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public Component getDisplayName() {
		return this.getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuMEDistributor(id, inventory, this);
	}

}
