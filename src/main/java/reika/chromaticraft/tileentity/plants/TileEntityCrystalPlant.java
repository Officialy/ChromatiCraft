/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.plants;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.block.BlockCrystalPlant;
import reika.chromaticraft.items.ItemCrystalSeeds;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code TileEntityCrystalPlant}: the Crystal Bloom's modifiers, sterility and shard timer.
 *
 * <p>V33a kept the growth counter here too ({@code growthTick}, 2 = freshly planted or harvested, 0 = ripe) and
 * derived the block's light from it. 26.x light comes from the block state, so the counter is the block's
 * {@link BlockCrystalPlant#GROWTH} property, alongside {@link BlockCrystalPlant#PURE} and
 * {@link BlockCrystalPlant#PRIMAL} which pick the light level and the cross texture. The flags stay here as the
 * authority and are mirrored to the state whenever they change.
 */
public class TileEntityCrystalPlant extends BlockEntity {

	private static final Random random = new Random();

	/** V33a {@code ForgeDirection.VALID_DIRECTIONS[2..5]}, in that order. */
	private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	private int flags;
	private boolean sterile;

	private long lastShardTick = -1;

	public TileEntityCrystalPlant(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CRYSTAL_PLANT.get(), pos, state);
	}

	private int growthTick() {
		return this.getBlockState().getValue(BlockCrystalPlant.GROWTH);
	}

	public boolean renderPod() {
		return this.growthTick() <= 1;
	}

	public boolean emitsLight() {
		return this.growthTick() == 0;
	}

	public void grow() {
		int growthTick = this.growthTick();
		if (growthTick > 0) {
			this.setGrowth(growthTick-1);
			if (this.isPure()) {
				for (Direction dir : HORIZONTAL) {
					double chance = this.is(Modifier.PRIMAL) ? 80 : this.is(Modifier.BOOSTED) ? 30 : 5;
					if (this.isOnMud())
						chance *= 2;
					// V33a capped a percentage at 1 and handed it to doWithChance, which reads anything above 1 as a
					// percentage; 1% or more is therefore certain, so a pure plant always pushes its neighbours on.
					if (ReikaRandomHelper.doWithChance(Math.min(1, chance))) {
						BlockPos n = worldPosition.relative(dir);
						if (level.getBlockState(n).is(this.getBlockState().getBlock())
								&& level.getBlockEntity(n) instanceof TileEntityCrystalPlant te) {
							te.grow();
						}
					}
				}
			}
		}
		else if (!this.is(Modifier.PRIMAL)) {
			this.tryGrowPrimal();
		}
		this.updateLight();
	}

	private boolean isOnMud() {
		return level.getBlockState(worldPosition.below()).is(ChromaBlocks.MUD.get());
	}

	/** V33a: stops at the first side without an Enriched plant of the same colour, so all four must be. */
	private void tryGrowPrimal() {
		if (random.nextInt(this.isOnMud() ? 40 : 60) > 0)
			return;
		for (Direction dir : HORIZONTAL) {
			boolean flag = false;
			BlockPos n = worldPosition.relative(dir);
			if (level.getBlockState(n).is(this.getBlockState().getBlock())
					&& level.getBlockEntity(n) instanceof TileEntityCrystalPlant te) {
				if (te.is(Modifier.BOOSTED)) {
					flag = true;
				}
			}
			if (!flag)
				return;
		}
		flags |= Modifier.PRIMAL.flag;
		this.setChanged();
	}

	public void makeRipe() {
		while (!this.canHarvest())
			this.grow();
	}

	/** V33a {@code updateLight}: relight and resend. The light itself now follows the mirrored state. */
	public void updateLight() {
		this.syncState(this.growthTick());
		if (level != null)
			level.sendBlockUpdated(worldPosition, this.getBlockState(), this.getBlockState(), 3);
	}

	private void setGrowth(int growth) {
		this.syncState(growth);
	}

	private void syncState(int growth) {
		if (level == null || level.isClientSide())
			return;
		BlockState state = this.getBlockState();
		BlockState updated = state.setValue(BlockCrystalPlant.GROWTH, growth)
				.setValue(BlockCrystalPlant.PURE, this.isPure())
				.setValue(BlockCrystalPlant.PRIMAL, this.is(Modifier.PRIMAL));
		if (updated != state)
			level.setBlock(worldPosition, updated, 3);
	}

	public void harvest(boolean drops) {
		this.setGrowth(2);
		if (drops) {
			ArrayList<ItemStack> li = this.getDrops();
			ReikaItemHelper.dropItems(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, li);
		}
		this.updateLight();
	}

	public ArrayList<ItemStack> getDrops() {
		ArrayList<ItemStack> li = new ArrayList<>();
		if (this.isSterile())
			return li;
		if (this.isPure() || random.nextInt(4) == 0) {
			CrystalElement color = this.getColor();
			if (this.isPure() && !this.is(Modifier.PRIMAL)) {
				int rand = random.nextInt(20);
				int num = 0;
				if (rand == 0) {
					num = 2;
				}
				else if (rand < 5) {
					num = 1;
				}
				int seedFlags = Modifier.IMPURE.flag;
				if (this.isPure() && random.nextInt(this.is(Modifier.BOOSTED) ? 4 : (this.isOnMud() ? 200 : 400)) == 0)
					seedFlags = Modifier.BOOSTED.flag;
				for (int i = 0; i < num; i++) {
					li.add(ItemCrystalSeeds.stack(color, seedFlags, false, 1));
				}
				if (random.nextInt(10) == 0) {
					li.add(ItemCrystalSeeds.stack(color, seedFlags, true, 1));
				}
			}
			long time = level.getGameTime();
			if (ChromaOptions.CRYSTALFARM.getState()) {
				if (time-lastShardTick >= (this.is(Modifier.PRIMAL) ? 60 : this.is(Modifier.BOOSTED) ? 150 : 600)) {
					if (ReikaRandomHelper.doWithChance(this.is(Modifier.PRIMAL) ? 10 : this.is(Modifier.BOOSTED) ? 5 : 2)) {
						li.add(ChromaItems.shardStack(color));
						lastShardTick = time;
						this.setChanged();
					}
				}
			}
		}
		return li;
	}

	public boolean canHarvest() {
		return this.growthTick() == 0;
	}

	public CrystalElement getColor() {
		return ((BlockCrystalPlant)this.getBlockState().getBlock()).getElement();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lastShardTick = input.getLongOr("shard", -1);
		flags = input.getIntOr("flags", 0);
		sterile = input.getBooleanOr("sterile", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("shard", lastShardTick);
		output.putInt("flags", flags);
		output.putBoolean("sterile", sterile);
	}

	public int getGrowthState() {
		return this.growthTick();
	}

	public void setStates(ItemStack item) {
		int itemFlags = ItemCrystalSeeds.getFlags(item);
		for (int i = 0; i < Modifier.list.length; i++) {
			Modifier m = Modifier.list[i];
			if (m.present(itemFlags)) {
				this.setState(m);
			}
		}
		sterile = ItemCrystalSeeds.isSterile(item);
		this.setChanged();
		this.updateLight();
	}

	public void setState(Modifier m) {
		flags |= m.flag;
	}

	public boolean is(Modifier m) {
		return m.present(flags);
	}

	public boolean isPure() {
		return !this.is(Modifier.IMPURE);
	}

	public boolean isSterile() {
		return sterile;
	}

	public static enum Modifier {
		IMPURE("Impure"),
		BOOSTED("Enriched"),
		PRIMAL("Primal");

		public final int flag;
		public final String displayName;

		public static final Modifier[] list = values();

		private Modifier(String s) {
			flag = 1 << (this.ordinal()+4);
			displayName = s;
		}

		public boolean present(int flags) {
			return (flags & flag) != 0;
		}

		public static Modifier getFromFlag(int flags) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].present(flags))
					return list[i];
			}
			return null;
		}

		public boolean showsInCreative() {
			return this != PRIMAL;
		}

		/** The lang key of V33a's literal tooltip line {@link #displayName}. */
		public String translationKey() {
			return "item.chromaticraft.crystal_seeds.modifier."+this.name().toLowerCase(java.util.Locale.ROOT);
		}
	}

}
