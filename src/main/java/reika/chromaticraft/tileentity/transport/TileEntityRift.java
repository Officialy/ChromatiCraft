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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EmptyEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.api.interfaces.WorldRift;
import reika.chromaticraft.auxiliary.interfaces.LinkedTile;
import reika.chromaticraft.base.tileentity.LinkedTileBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.auxiliary.ChunkManager;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.blockentity.ChunkLoadingTile;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * V33a World Rift: one end of a linked pair. Whatever touches one face of this rift is joined to whatever sits on the
 * opposite face of the other rift, in any dimension: fluids and energy on every face, items through the one face the
 * Manipulator picked ({@link #setDirection}), redstone (mirrored, one level weaker) and block updates. With the
 * World Rifts Chunkload option on it keeps the 5x5 chunks around itself loaded.
 *
 * <p>The 26.3 capability views resolve the far side on every call, so a relink, reset or far-side change never
 * leaves a pipe holding a stale handler. A call that has passed through {@link #MAX_DEPTH} rifts in a row returns
 * nothing: two rift pairs whose far ends face each other forwarded forever in V33a (a stack overflow).
 */
public class TileEntityRift extends LinkedTileBase implements WorldRift, ChunkLoadingTile {

	/** Rift-to-rift hops one capability call may make before it gives up. */
	private static final int MAX_DEPTH = 8;
	private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	private int color = 0xffffff;
	private int[] redstoneCache = new int[6];
	private @Nullable Direction singleDirection;

	private final RiftResourceHandler<FluidResource>[] fluidViews = views(Capabilities.Fluid.BLOCK, false);
	private final RiftResourceHandler<ItemResource>[] itemViews = views(Capabilities.Item.BLOCK, true);
	private final RiftEnergyHandler[] energyViews = new RiftEnergyHandler[6];

	// CHROMA-PORT: V33a also made the rift an OpenComputers SidedEnvironment (one network node per face, joined to the
	// far rift's opposite-face node on link and split on reset), a ComputerCraft peripheral and a ProjectRed
	// IBundledTile, all passing through to the single-direction tile, plus Thaumcraft IEssentiaTransport and
	// IAspectContainer views of the far side. None of those mods target 26.3; restore them from V33a when they do.

	public TileEntityRift(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.RIFT.get(), pos, state);
		for (Direction dir : Direction.values())
			energyViews[dir.ordinal()] = new RiftEnergyHandler(dir);
	}

	@SuppressWarnings("unchecked")
	private <T extends Resource> RiftResourceHandler<T>[] views(BlockCapability<ResourceHandler<T>, @Nullable Direction> cap,
			boolean single) {
		RiftResourceHandler<T>[] arr = new RiftResourceHandler[6];
		for (Direction dir : Direction.values())
			arr[dir.ordinal()] = new RiftResourceHandler<>(cap, dir, single);
		return arr;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.RIFT;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (!world.isClientSide() && this.isLinked() && this.getOther() != null) {
			AABB box = new AABB(pos);//.inflate(0.15);
			List<Entity> li = world.getEntitiesOfClass(Entity.class, box);
			for (Entity e : li) {
				if (this.shouldTeleport(e)) {
					this.teleport(e);
				}
			}
		}
	}

	private void teleport(Entity e) {
		double rx = 1-(e.getX()-worldPosition.getX());
		double ry = 1-(e.getY()-worldPosition.getY());
		double rz = 1-(e.getZ()-worldPosition.getZ());
		TileEntityRift te = this.getOther();
		// CHROMA-PORT: for a Botania IManaBurst V33a also recoloured the burst to this rift's colour, moved its source
		// to the far rift, reset its mana to the starting amount (a fresh life) and played botania:spreaderFire at
		// the far rift (0.5 volume, pitch 1 +- 0.25).
		BlockPos to = te.getBlockPos();
		e.snapTo(to.getX()+rx, to.getY()+ry, to.getZ()+rz, e.getYRot(), e.getXRot());
		e.getPersistentData().putLong("rift_teleported", level.getGameTime());
	}

	private boolean shouldTeleport(Entity e) {
		long get = e.getPersistentData().getLongOr("rift_teleported", 0);
		if (level.getGameTime()-get < 5)
			return false;
		// CHROMA-PORT: V33a passed only Botania mana bursts (IManaBurst) through; nothing else ever teleported.
		return false;
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		if (ChromaOptions.RIFTLOAD.getState()) {
			ChunkManager.instance.loadChunks(this);
		}
	}

	/** V33a: the Manipulator picks the face items pass through; the far rift takes the opposite. */
	public void setDirection(Direction dir) {
		if (this.isLinked() && this.getOther() != null) {
			singleDirection = dir;
			this.getOther().singleDirection = dir.getOpposite();
			this.syncAllData(false);
			this.getOther().syncAllData(false);
		}
	}

	public @Nullable Direction getSingleDirection() {
		return singleDirection;
	}

	@Override
	public boolean canDrop(Player ep) {
		return ep.getUUID().equals(placerUUID);
	}

	/**
	 * V33a, on any neighbour change: record the redstone each face receives into the far rift (which emits it on the
	 * mirrored face), tell every block around the far rift, and refresh the caches of tiles around this one.
	 */
	public void passThrough() {
		if (this.isLinked()) {
			WorldLocation loc = this.getLinkTarget();
			TileEntityRift te = this.getOther();
			if (te != null) {
				Level other = te.getLevel();
				for (Direction dir : Direction.values()) {
					Direction opp = dir.getOpposite();
					BlockPos d = loc.pos.relative(dir);
					int pwr = level.getSignal(worldPosition.relative(dir), opp);
					te.redstoneCache[dir.ordinal()] = pwr;
					BlockState id = other.getBlockState(d);
					if (!id.isAir()) {
						other.neighborChanged(d, id.getBlock(), null);
					}
					BlockEntity tile = this.getAdjacentBlockEntity(dir);
					if (tile != this && tile instanceof BlockEntityBase base) {
						base.updateCache(dir.getOpposite());
					}
				}
			}
			loc.triggerBlockUpdate(true);
		}
	}

	public int getRedstoneLevel(Direction side) {
		return Math.max(redstoneCache[side.ordinal()]-1, 0);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void createRandomLinkID() {
		color = this.getRandomColor();
	}

	@Override
	public void assignLinkID(LinkedTile other) {
		TileEntityRift te = (TileEntityRift)other;
		color = te.color;
	}

	@Override
	protected void onReset() {
		color = 0xffffff;
		redstoneCache = new int[6];
		singleDirection = null;
	}

	@Override
	protected void onResetOther(LinkedTile tile) {
		TileEntityRift te = (TileEntityRift)tile;
		te.color = 0xffffff;
	}

	/** The pair's shared hue; outside a world (the item) V33a cycled through every hue in fifteen seconds. */
	public int getColor() {
		return this.isInWorld() ? color : itemColor();
	}

	public static int itemColor() {
		return Color.HSBtoRGB((System.currentTimeMillis()%15000)/15000F, 1, 1);
	}

	private int getRandomColor() {
		return Color.HSBtoRGB(level.getRandom().nextFloat(), 1, 1);
	}

	@Override
	public boolean canLinkTo(Level world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof TileEntityRift;
	}

	@Override
	public @Nullable TileEntityRift getOther() {
		return super.getOther() instanceof TileEntityRift te ? te : null;
	}

	public @Nullable WorldLocation getBlockFrom(Direction from) {
		return this.isLinked() ? this.getLinkTarget().move(from.getOpposite(), 1) : null;
	}

	/** The tile beside the far rift on {@code dir}, or null; its chunk is loaded on demand, as in V33a. */
	private @Nullable BlockEntity getAdjacentTargetTile(Direction dir) {
		TileEntityRift te = this.getOther();
		if (te == null)
			return null;
		BlockPos p = te.getBlockPos().relative(dir);
		BlockEntity tile = te.getLevel().getBlockEntity(p);
		return tile != null && !tile.isRemoved() ? tile : null;
	}

	@Override
	public @Nullable Block getBlockIDFrom(Direction dir) {
		return this.isLinked() ? this.getBlockFrom(dir).getBlock() : null;
	}

	/** 26.x blocks have states, not metadata: 0 for a linked rift, as for any single-state block in 1.7.10. */
	@Override
	public int getBlockMetadataFrom(Direction dir) {
		return this.isLinked() ? 0 : -1;
	}

	/** The state {@link #getBlockIDFrom} reads, in full. */
	public @Nullable BlockState getBlockStateFrom(Direction dir) {
		if (!this.isLinked())
			return null;
		WorldLocation loc = this.getBlockFrom(dir);
		Level world = loc.getWorld();
		return world != null ? world.getBlockState(loc.pos) : null;
	}

	/** As V33a: the far rift's neighbour on {@code dir} itself (unlike {@link #getBlockFrom}, which flips it). */
	@Override
	public @Nullable BlockEntity getTileEntityFrom(Direction dir) {
		return this.isLinked() ? this.getAdjacentTargetTile(dir) : null;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		NBT.putInt("color", color);
		NBT.putIntArray("redstone", redstoneCache);
		NBT.putInt("dir", singleDirection != null ? singleDirection.ordinal() : -1);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		color = NBT.getIntOr("color", 0xffffff);
		int[] rs = NBT.getIntArray("redstone").orElse(new int[6]);
		redstoneCache = rs.length == 6 ? rs : Arrays.copyOf(rs, 6);
		int dir = NBT.getIntOr("dir", -1);
		singleDirection = dir >= 0 && dir < 6 ? Direction.values()[dir] : null;
	}

	// CHROMA-PORT: V33a also accepted a BuildCraft power pipe on the far side (receiveEnergy/requestEnergy on its
	// PipeTransportPower) and told BuildCraft transport pipes to connect to every face. BuildCraft has no 26.3 build.

	/** V33a IFluidHandler: face {@code side} reaches the tile beside the far rift's opposite face. */
	public ResourceHandler<FluidResource> fluidHandler(@Nullable Direction side) {
		return side != null ? fluidViews[side.ordinal()] : EmptyResourceHandler.instance();
	}

	/** V33a ISidedInventory: only through the Manipulator-chosen face's far tile, whichever face asks. */
	public ResourceHandler<ItemResource> itemHandler(@Nullable Direction side) {
		return itemViews[(side != null ? side : Direction.DOWN).ordinal()];
	}

	/** V33a IEnergyHandler (RF): face {@code side} reaches the tile beside the far rift's opposite face. */
	public EnergyHandler energyHandler(@Nullable Direction side) {
		return side != null ? energyViews[side.ordinal()] : EmptyEnergyHandler.INSTANCE;
	}

	/** The far side's handler for a query on {@code side}, or null while unlinked or absent (loaded on demand). */
	private <H> @Nullable H farHandler(BlockCapability<H, @Nullable Direction> cap, Direction side, boolean single) {
		TileEntityRift te = this.getOther();
		if (te == null)
			return null;
		BlockPos target;
		if (single) {
			if (singleDirection == null)
				return null;
			target = te.getBlockPos().relative(singleDirection.getOpposite());
		}
		else {
			target = te.getBlockPos().relative(side.getOpposite());
		}
		return te.getLevel().getCapability(cap, target, side);
	}

	private static boolean enter() {
		int[] d = DEPTH.get();
		if (d[0] >= MAX_DEPTH)
			return false;
		d[0]++;
		return true;
	}

	private static void exit() {
		DEPTH.get()[0]--;
	}

	private final class RiftResourceHandler<T extends Resource> implements ResourceHandler<T> {

		private final BlockCapability<ResourceHandler<T>, @Nullable Direction> cap;
		private final Direction side;
		private final boolean single;

		private RiftResourceHandler(BlockCapability<ResourceHandler<T>, @Nullable Direction> cap, Direction side,
				boolean single) {
			this.cap = cap;
			this.side = side;
			this.single = single;
		}

		private @Nullable ResourceHandler<T> target() {
			return TileEntityRift.this.farHandler(cap, side, single);
		}

		@Override
		public int size() {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null ? h.size() : 0;
			}
			finally {
				exit();
			}
		}

		/** Callers read slots below {@link #size}; an index the far side no longer has is out of bounds. */
		@Override
		public T getResource(int index) {
			if (!enter())
				throw new IndexOutOfBoundsException(index);
			try {
				ResourceHandler<T> h = this.target();
				if (h == null || index >= h.size())
					throw new IndexOutOfBoundsException(index);
				return h.getResource(index);
			}
			finally {
				exit();
			}
		}

		@Override
		public long getAmountAsLong(int index) {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null && index < h.size() ? h.getAmountAsLong(index) : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public long getCapacityAsLong(int index, T resource) {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null && index < h.size() ? h.getCapacityAsLong(index, resource) : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public boolean isValid(int index, T resource) {
			if (!enter())
				return false;
			try {
				ResourceHandler<T> h = this.target();
				return h != null && index < h.size() && h.isValid(index, resource);
			}
			finally {
				exit();
			}
		}

		@Override
		public int insert(int index, T resource, int amount, TransactionContext transaction) {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null && index < h.size() ? h.insert(index, resource, amount, transaction) : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public int insert(T resource, int amount, TransactionContext transaction) {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null ? h.insert(resource, amount, transaction) : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public int extract(int index, T resource, int amount, TransactionContext transaction) {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null && index < h.size() ? h.extract(index, resource, amount, transaction) : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public int extract(T resource, int amount, TransactionContext transaction) {
			if (!enter())
				return 0;
			try {
				ResourceHandler<T> h = this.target();
				return h != null ? h.extract(resource, amount, transaction) : 0;
			}
			finally {
				exit();
			}
		}
	}

	private final class RiftEnergyHandler implements EnergyHandler {

		private final Direction side;

		private RiftEnergyHandler(Direction side) {
			this.side = side;
		}

		private @Nullable EnergyHandler target() {
			return TileEntityRift.this.farHandler(Capabilities.Energy.BLOCK, side, false);
		}

		@Override
		public long getAmountAsLong() {
			if (!enter())
				return 0;
			try {
				EnergyHandler h = this.target();
				return h != null ? h.getAmountAsLong() : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public long getCapacityAsLong() {
			if (!enter())
				return 0;
			try {
				EnergyHandler h = this.target();
				return h != null ? h.getCapacityAsLong() : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public int insert(int amount, TransactionContext transaction) {
			if (!enter())
				return 0;
			try {
				EnergyHandler h = this.target();
				return h != null ? h.insert(amount, transaction) : 0;
			}
			finally {
				exit();
			}
		}

		@Override
		public int extract(int amount, TransactionContext transaction) {
			if (!enter())
				return 0;
			try {
				EnergyHandler h = this.target();
				return h != null ? h.extract(amount, transaction) : 0;
			}
			finally {
				exit();
			}
		}
	}

	@Override
	public void breakBlock() {
		ChunkManager.instance.unloadChunks(this);
	}

	@Override
	public Collection<ChunkPos> getChunksToLoad() {
		return ChunkManager.getChunkSquare(worldPosition.getX(), worldPosition.getZ(), 2);
	}

}
