/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.networking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.DynamicRepeater;
import reika.chromaticraft.magic.interfaces.ReactiveRepeater;
import reika.chromaticraft.magic.interfaces.WeakRepeaterSafeReceiver;
import reika.chromaticraft.magic.network.CrystalLink;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.block.BlockWeakRepeater;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a's obtainable wooden repeater: short range, high attenuation, and deliberately safe only for
 * the early Relay Source, Ritual Table, and Personal Charger receiver family. Attempting to use it
 * outside that family blocks throughput; a transfer that becomes unsafe while already in flight has
 * the source-authored one-in-eight chance to start the 320-tick burn-down and weighted terminal
 * failure sequence.
 */
public final class TileEntityWeakRepeater extends TileEntityCrystalRepeater
		implements DynamicRepeater, ReactiveRepeater {

	public static final int WEAK_RANGE = 16;
	public static final int WEAK_RECEIVE_RANGE = 24;
	private static final int FAILURE_TIME = 320;

	private CrystalElement overloadColor;
	private int eolTicks;
	private boolean ruptured;

	public TileEntityWeakRepeater(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.WEAK_REPEATER.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.WEAKREPEATER;
	}

	public boolean isRuptured() {
		return ruptured;
	}

	public int getFailureTicks() {
		return eolTicks;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (!world.isClientSide()) {
			BlockState state = world.getBlockState(pos);
			if (state.hasProperty(BlockWeakRepeater.RUPTURED)
					&& state.getValue(BlockWeakRepeater.RUPTURED) != ruptured)
				world.setBlock(pos, state.setValue(BlockWeakRepeater.RUPTURED, ruptured), 3);
		}
		if (ruptured)
			return;

		if (world.isClientSide()) {
			if (eolTicks > 0) {
				eolTicks++;
				ChromaParticle.spawnWeakRepeaterBreakdown(world, pos, overloadColor, eolTicks, rand);
			}
			return;
		}

		if (eolTicks > 0) {
			eolTicks++;
			BlockPos fire = pos.above();
			if (world.getBlockState(fire).canBeReplaced())
				world.setBlockAndUpdate(fire, Blocks.FIRE.defaultBlockState());
			if (eolTicks > FAILURE_TIME)
				this.finishFailure((ServerLevel)world);
		}
	}

	private void beginFailure(CrystalElement element) {
		if (eolTicks > 0 || ruptured || this.getLevel() == null || this.getLevel().isClientSide())
			return;
		overloadColor = element;
		eolTicks = 1 + rand.nextInt(40);
		BlockPos fire = this.getBlockPos().above();
		if (this.getLevel().getBlockState(fire).canBeReplaced())
			this.getLevel().setBlockAndUpdate(fire, Blocks.FIRE.defaultBlockState());
		ChromaSounds.REPEATERSURGE_WEAK.playSoundAtBlock(this, 1, 1.1035F);
		this.syncAllData(false);
	}

	private void finishFailure(ServerLevel world) {
		if (this.getPlacer() != null)
			ProgressStage.BLOWREPEATER.stepPlayerTo(this.getPlacer());
		// V33a releases ownership at the terminal failure so anyone can clear the wreckage.
		placerUUID = null;
		FailureMode mode = FailureMode.choose(rand.nextInt(FailureMode.TOTAL_WEIGHT));
		if (overloadColor != null)
			reika.chromaticraft.network.ChromaNetwork.sendWeakRepeaterFailureBurst(
					world, this.getBlockPos(), overloadColor);
		switch (mode) {
			case BURN -> {
				this.removeFromCache();
				CrystalNetworker.instance.breakPaths(this);
				this.igniteNeighbours(world);
				world.setBlockAndUpdate(this.getBlockPos(), ChromaBlocks.CHROMA.get().defaultBlockState());
				world.playSound(null, this.getBlockPos(), SoundEvents.FIRE_EXTINGUISH,
						SoundSource.BLOCKS, 2, 0.5F);
			}
			case EXPLOSION -> {
				this.removeFromCache();
				CrystalNetworker.instance.breakPaths(this);
				this.igniteNeighbours(world);
				this.delete();
				world.explode(null, this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5,
						2, true, Level.ExplosionInteraction.BLOCK);
			}
			case RUPTURE -> {
				world.playSound(null, this.getBlockPos(), SoundEvents.FIRE_EXTINGUISH,
						SoundSource.BLOCKS, 2, 0.5F);
				world.sendParticles(ParticleTypes.LAVA, this.getX() + 0.5, this.getY() + 0.75,
						this.getZ() + 0.5, 40, 0.5, 0.75, 0.5, 0.05);
				world.setBlockAndUpdate(this.getBlockPos().above(), Blocks.AIR.defaultBlockState());
				world.explode(null, this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5,
						2, Level.ExplosionInteraction.NONE);
				ruptured = true;
				eolTicks = 0;
				BlockState state = world.getBlockState(this.getBlockPos());
				if (state.hasProperty(BlockWeakRepeater.RUPTURED))
					world.setBlock(this.getBlockPos(), state.setValue(BlockWeakRepeater.RUPTURED, true), 3);
				CrystalNetworker.instance.breakPaths(this);
				this.syncAllData(false);
			}
		}
	}

	private void igniteNeighbours(Level world) {
		for (Direction direction : Direction.values())
			ReikaWorldHelper.ignite(world, this.getBlockPos().relative(direction));
	}

	@Override
	public void onTransfer(CrystalSource source, CrystalReceiver receiver,
			CrystalElement element, int amount) {
		if (!this.canSafelySupply(receiver) && rand.nextInt(8) == 0)
			this.beginFailure(element);
	}

	public boolean canSafelySupply(CrystalReceiver receiver) {
		return receiver instanceof WeakRepeaterSafeReceiver;
	}

	@Override
	public int getSendRange() {
		return WEAK_RANGE;
	}

	@Override
	public int getReceiveRange() {
		return WEAK_RECEIVE_RANGE;
	}

	@Override
	protected boolean checkForStructure() {
		return this.getLevel() != null
				&& this.getLevel().getBlockState(this.getBlockPos().relative(facing)).is(BlockTags.LOGS);
	}

	@Override
	protected boolean checkEnhancedStructure() {
		return false;
	}

	@Override
	public boolean canConduct() {
		// The original remains part of the network during its visible 320-tick burn-down; only a
		// terminal rupture disables it. Unsafe receiver throughput is still clamped to zero below.
		return super.canConduct() && !ruptured;
	}

	@Override
	public CrystalElement getActiveColor() {
		return null;
	}

	@Override
	public boolean isConductingElement(CrystalElement element) {
		return element != null;
	}

	@Override
	public int maxThroughput() {
		return 120;
	}

	@Override
	public int getSignalDegradation(boolean point) {
		return 250;
	}

	@Override
	public double getIncomingBeamRadius() {
		return 0.125;
	}

	@Override
	public double getOutgoingBeamRadius() {
		return 0.125;
	}

	@Override
	public float getFailureWeight(CrystalElement element) {
		return 30;
	}

	@Override
	public int getModifiedThoughput(int baseThroughput, CrystalSource source,
			CrystalReceiver receiver) {
		return this.canSafelySupply(receiver) ? baseThroughput : 0;
	}

	@Override
	protected boolean canBeRainAffected(CrystalLink link) {
		return false;
	}

	@Override
	public ChromaStructures getPrimaryStructure() {
		return ChromaStructures.WEAKREPEATER;
	}

	@Override
	public Coordinate getStructureOffset() {
		return null;
	}

	@Override
	public void getTagsToWriteToStack(CompoundTag tag) {
		super.getTagsToWriteToStack(tag);
		// V33a intentionally does not preserve a burning/ruptured weak repeater as a healthy item.
	}

	/** Source-exact salvage: 3-8 sticks, 1-3 crystal dust, and a 50% glowstone-dust roll. */
	public List<ItemStack> createBrokenDrops() {
		List<ItemStack> drops = new ArrayList<>();
		int sticks = 3 + rand.nextInt(6);
		for (int i = 0; i < sticks; i++)
			drops.add(new ItemStack(Items.STICK));
		int powder = 1 + rand.nextInt(3);
		for (int i = 0; i < powder; i++)
			drops.add(new ItemStack(ChromaItems.CRYSTAL_POWDER.get()));
		if (rand.nextBoolean())
			drops.add(new ItemStack(Items.GLOWSTONE_DUST));
		return drops;
	}

	@Override
	protected List<ItemStack> getSneakPopDrops() {
		return eolTicks == 0 && !ruptured ? super.getSneakPopDrops() : this.createBrokenDrops();
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		eolTicks = tag.getIntOr("eol", 0);
		int color = tag.getIntOr("overload", -1);
		overloadColor = color >= 0 && color < CrystalElement.elements.length
				? CrystalElement.elements[color] : null;
		ruptured = tag.getBooleanOr("rupture", false);
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("eol", eolTicks);
		if (overloadColor != null)
			tag.putInt("overload", overloadColor.ordinal());
		tag.putBoolean("rupture", ruptured);
	}

	private enum FailureMode {
		EXPLOSION(50), BURN(20), RUPTURE(40);

		private static final int TOTAL_WEIGHT = 110;
		private final int weight;

		FailureMode(int weight) {
			this.weight = weight;
		}

		private static FailureMode choose(int value) {
			for (FailureMode mode : values()) {
				if (value < mode.weight)
					return mode;
				value -= mode.weight;
			}
			return RUPTURE;
		}
	}
}
