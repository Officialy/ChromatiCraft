package reika.chromaticraft.tileentity;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.block.dimension.structure.locks.BlockLockKey;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

/** Persistent delegate and portable-item state for {@link BlockLockKey}. */
public final class TileEntityLockKey extends BlockEntity {

	public interface Handler {
		default boolean canAccessLockKey(BlockPos keyPos, @Nullable Player player) { return true; }
		void onLockKeyChanged(BlockPos keyPos, int channel, @Nullable CrystalElement rune,
				boolean added, @Nullable Player player);
	}

	private @Nullable UUID structureId;
	private @Nullable BlockPos delegate;
	private boolean removalNotified;

	public TileEntityLockKey(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LOCK_KEY.get(), pos, state);
	}

	public void setStructureId(@Nullable UUID id) {
		structureId = id;
		setChanged();
	}

	public @Nullable UUID getStructureId() { return structureId; }

	public void setDelegate(@Nullable BlockPos pos) {
		delegate = pos != null ? pos.immutable() : null;
		setChanged();
	}

	public @Nullable BlockPos getDelegate() { return delegate; }

	public boolean canPlayerAccess(@Nullable Player player) {
		return level == null || delegate == null || !(level.getBlockEntity(delegate) instanceof Handler handler)
				|| handler.canAccessLockKey(worldPosition, player);
	}

	public void notifyDelegate(boolean added, @Nullable Player player) {
		if (added) removalNotified = false;
		else if (removalNotified) return;
		if (level == null || delegate == null || !(level.getBlockEntity(delegate) instanceof Handler handler))
			return;
		int channel = getBlockState().getValue(BlockLockKey.CHANNEL);
		handler.onLockKeyChanged(worldPosition, channel, BlockLockKey.adjacentRune(level, worldPosition),
				added, player);
		if (!added) removalNotified = true;
	}

	/** 26.2 has no general Block.onRemove hook; the BE lifecycle covers commands and replacements. */
	@Override
	public void setRemoved() {
		notifyDelegate(false, null);
		super.setRemoved();
	}

	public ItemStack createPortableStack(int channel) {
		ItemStack stack = new ItemStack(ChromaBlocks.LOCK_KEY.get());
		CompoundTag tag = new CompoundTag();
		tag.putInt("channel", channel);
		if (structureId != null) tag.putString("uid", structureId.toString());
		if (delegate != null) tag.putLong("delegate", delegate.asLong());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	public void readPortableData(CompoundTag tag) {
		String idKey = tag.contains("uid") ? "uid" : "structureId";
		structureId = tag.getString(idKey).flatMap(value -> {
			try { return java.util.Optional.of(UUID.fromString(value)); }
			catch (IllegalArgumentException ignored) { return java.util.Optional.empty(); }
		}).orElse(null);
		delegate = tag.getLong("delegate").map(BlockPos::of).orElse(null);
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (structureId != null) output.putString("structureId", structureId.toString());
		if (delegate != null) output.putLong("delegate", delegate.asLong());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		structureId = input.getString("structureId").flatMap(value -> {
			try { return java.util.Optional.of(UUID.fromString(value)); }
			catch (IllegalArgumentException ignored) { return java.util.Optional.empty(); }
		}).orElse(null);
		delegate = input.getLong("delegate").map(BlockPos::of).orElse(null);
	}
}
