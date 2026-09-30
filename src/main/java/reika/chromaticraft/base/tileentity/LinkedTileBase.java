package reika.chromaticraft.base.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.LinkedTile;
import reika.chromaticraft.auxiliary.interfaces.SneakPop;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.base.BlockTEBase;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** The V33a two-ended link lifecycle and item drop, with dimension-aware 26.3 positions. */
public abstract class LinkedTileBase extends TileEntityChromaticBase implements SneakPop, LinkedTile {
	private WorldLocation target;
	private boolean shouldDrop;
	private boolean primary;

	protected LinkedTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (shouldDrop && !world.isClientSide()) {
			ItemEntity item = ReikaItemHelper.dropItem(world, pos.getX() + 0.5, pos.getY() + 0.5,
					pos.getZ() + 0.5, this.getTile().getCraftedProduct());
			item.setUnlimitedLifetime();
			this.delete();
		}
	}

	@Override
	public final boolean allowMining(Player player) {
		return false;
	}

	@Override
	public final void drop() {
		shouldDrop = true;
		LinkedTile other = this.getOther();
		if (other != null)
			other.markForDrop();
		this.reset();
	}

	public final boolean isLinked() {
		return target != null;
	}

	public abstract boolean canLinkTo(Level world, BlockPos pos);

	@Override
	public final boolean linkTo(Level world, BlockPos pos) {
		if (world.isClientSide() || !this.canLinkTo(world, pos))
			return false;
		this.resetOther();
		target = new WorldLocation(world, pos);
		this.createRandomLinkID();
		LinkedTile other = this.getOther();
		if (other == null) {
			target = null;
			return false;
		}
		other.setTarget(new WorldLocation(level, worldPosition));
		other.assignLinkID(this);
		this.onLink(true);
		this.onLinkTo(world, pos, other);
		this.setChanged();
		return true;
	}

	@Override
	public final boolean linkTo(WorldLocation loc) {
		Level world = loc.getWorld();
		return world != null && !loc.equals(level, worldPosition) && this.linkTo(world, loc.pos);
	}

	protected abstract void createRandomLinkID();

	protected void onLinkTo(Level world, BlockPos pos, LinkedTile other) {}

	@Override
	public final void onLink(boolean other) {
		if (level == null)
			return;
		ChromaSounds.RIFT.playSoundAtBlock(level, worldPosition);
		for (Direction dir : Direction.values()) {
			BlockPos neighbor = worldPosition.relative(dir);
			Block block = level.getBlockState(neighbor).getBlock();
			if (block instanceof BlockTEBase teBlock)
				teBlock.updateTileCache(level, neighbor);
		}
		if (other && this.isLinked()) {
			LinkedTile linked = this.getOther();
			if (linked != null)
				linked.onLink(false);
		}
		this.triggerBlockUpdate();
		ReikaWorldHelper.causeAdjacentUpdates(level, worldPosition);
		this.syncAllData(false);
	}

	@Override
	public final void reset() {
		this.resetOther();
		target = null;
		this.onReset();
		this.onLink(true);
		this.setChanged();
	}

	public final void resetOther() {
		LinkedTile other = this.getOther();
		if (other != null) {
			other.setTarget(null);
			this.onResetOther(other);
			this.onLink(true);
		}
	}

	protected void onReset() {}
	protected void onResetOther(LinkedTile other) {}

	protected LinkedTile getOther() {
		if (target == null)
			return null;
		Level world = target.getWorld();
		if (world == null || !world.isLoaded(target.pos))
			return null;
		BlockEntity te = world.getBlockEntity(target.pos);
		return te instanceof LinkedTile linked ? linked : null;
	}

	public final WorldLocation getLinkTarget() {
		return target;
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		if (target != null)
			target.saveAdditional("target", tag);
		tag.putBoolean("primary", primary);
		tag.putBoolean("drop", shouldDrop);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		target = WorldLocation.load("target", tag);
		primary = tag.getBooleanOr("primary", false);
		shouldDrop = tag.getBooleanOr("drop", false);
	}

	@Override
	public final boolean isPrimary() {
		return primary;
	}

	public final void setPrimary() {
		primary = true;
		this.syncAllData(false);
		LinkedTile other = this.getOther();
		if (other != null) {
			other.setPrimary(false);
			other.syncAllData(false);
		}
	}

	@Override
	public final void setPrimary(boolean value) {
		if (value)
			this.setPrimary();
		else {
			primary = false;
			this.setChanged();
		}
	}

	@Override
	public final void markForDrop() {
		shouldDrop = true;
		this.setChanged();
	}

	@Override
	public final void setTarget(WorldLocation loc) {
		target = loc;
		this.syncAllData(false);
	}
}
