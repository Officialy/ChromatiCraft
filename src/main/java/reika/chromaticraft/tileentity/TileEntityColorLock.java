package reika.chromaticraft.tileentity;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.block.dimension.structure.locks.BlockColoredLock;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.CrystalElement;

/** Required/open color persistence for {@link BlockColoredLock}. */
public final class TileEntityColorLock extends BlockEntity {

	private final EnumSet<CrystalElement> colors = EnumSet.noneOf(CrystalElement.class);
	private final EnumSet<CrystalElement> closedColors = EnumSet.noneOf(CrystalElement.class);
	private int channel;
	private int queueTick;

	public TileEntityColorLock(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.COLOR_LOCK.get(), pos, state);
	}

	public TileEntityColorLock addColor(CrystalElement element) {
		colors.add(element);
		closedColors.add(element);
		updateState();
		return this;
	}

	public void setColors(CrystalElement... elements) {
		colors.clear();
		closedColors.clear();
		Collections.addAll(colors, elements);
		closedColors.addAll(colors);
		updateState();
	}

	public void clearColors() {
		colors.clear();
		closedColors.clear();
		updateState();
	}

	public void setOpenColors(Collection<CrystalElement> open) {
		closedColors.clear();
		closedColors.addAll(colors);
		closedColors.removeAll(open);
		updateState();
	}

	public void setGateOpen(boolean open) {
		if (getBlockState().getValue(BlockColoredLock.GATE)) {
			closedColors.clear();
			if (!open) closedColors.addAll(colors);
			updateState();
		}
	}

	private void updateState() {
		if (level == null) return;
		boolean open = closedColors.isEmpty();
		BlockState state = getBlockState();
		if (state.getValue(BlockColoredLock.OPEN) != open) {
			level.setBlock(worldPosition, state.setValue(BlockColoredLock.OPEN, open), Block.UPDATE_ALL);
			level.playSound(null, worldPosition, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2, 1);
			level.playSound(null, worldPosition, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2, 1);
		}
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public CrystalElement randomColor(RandomSource random) {
		if (colors.isEmpty()) return null;
		int index = random.nextInt(colors.size());
		for (CrystalElement element : colors)
			if (index-- == 0) return element;
		return null;
	}

	public Collection<CrystalElement> getColors() { return Collections.unmodifiableSet(colors); }
	public Collection<CrystalElement> getClosedColors() { return Collections.unmodifiableSet(closedColors); }
	public boolean isHeldOpen() { return getBlockState().getValue(BlockColoredLock.OPEN) && queueTick > 0; }
	public int getChannel() { return channel; }
	public void setChannel(int value) { channel = value; setChanged(); }
	public void queueTick(int time) { queueTick = Math.max(queueTick, time); setChanged(); }

	public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
			TileEntityColorLock lock) {
		if (lock.queueTick > 0 && --lock.queueTick == 0) lock.updateState();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("channel", channel);
		output.putInt("queueTick", queueTick);
		ValueOutput.TypedOutputList<String> required = output.list("colors", com.mojang.serialization.Codec.STRING);
		for (CrystalElement element : colors) required.add(element.name());
		ValueOutput.TypedOutputList<String> closed = output.list("closedColors", com.mojang.serialization.Codec.STRING);
		for (CrystalElement element : closedColors) closed.add(element.name());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		channel = input.getIntOr("channel", 0);
		queueTick = input.getIntOr("queueTick", 0);
		colors.clear();
		closedColors.clear();
		for (String name : input.listOrEmpty("colors", com.mojang.serialization.Codec.STRING)) add(colors, name);
		for (String name : input.listOrEmpty("closedColors", com.mojang.serialization.Codec.STRING)) add(closedColors, name);
	}

	private static void add(EnumSet<CrystalElement> set, String name) {
		try { set.add(CrystalElement.valueOf(name)); }
		catch (IllegalArgumentException ignored) {}
	}

	@Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveCustomOnly(provider); }
	@Override public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
