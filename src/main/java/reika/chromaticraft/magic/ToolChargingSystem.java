package reika.chromaticraft.magic;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import reika.chromaticraft.base.tileentity.TileEntityLocusPoint;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.PoweredItem;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.auxiliary.ChunkManager;
import reika.dragonapi.interfaces.entity.ChunkLoadingEntity;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code ToolChargingSystem}: the charge held by a {@link PoweredItem}, kept in the stack's {@code "charge"} tag.
 * A powered item lying on the ground within 32 blocks of a crystal source conducting its colour fills from it -- faster
 * at low charge (a cosine curve), 1.25x at a pylon (2x enhanced), 1.25x lying in chroma, 2x within 256 blocks of an Aura
 * Point -- draining the source four times what it gives; such items never despawn and keep the chunks around them
 * loaded. V33a stored the charge state as the item damage to pick the sprite; the {@code chromaticraft:charge_state}
 * item-model property reads the charge instead.
 */
public class ToolChargingSystem {

	public static final ToolChargingSystem instance = new ToolChargingSystem();

	private static final int CHARGE_RANGE = 32;

	/** V33a {@code ItemEfficiencyCrystal.TAG}, stamped each tick an Efficiency Crystal is carried. */
	private static final String EFFICIENCY_TAG = "last_efficiency";

	private ToolChargingSystem() {

	}

	public int getChargeRate(ItemStack is) {
		int get = this.getCharge(is);
		int base = (int)(5*Math.min(20, 1+100*ReikaMathLibrary.cosInterpolation(0, this.getItem(is).getMaxCharge(), get)));
		return this.getItem(is).getChargeRate(is, base);
	}

	public <I extends Item & PoweredItem> ItemStack getChargedItem(I item, int charge) {
		ItemStack is = new ItemStack(item);
		this.addCharge(is, charge);
		return is;
	}

	public int addCharge(ItemStack is, int amt) {
		int get = this.getCharge(is);
		amt = Math.min(amt, this.getItem(is).getMaxCharge()-get);
		this.setCharge(is, get+amt);
		return amt;
	}

	public int removeCharge(ItemStack is, int amt, Player ep) {
		if (isEfficiencyActive(ep))
			amt = Math.max(1, amt/3);
		int get = this.getCharge(is);
		amt = Math.min(get, amt);
		this.setCharge(is, get-amt);
		return amt;
	}

	/**
	 * V33a {@code ItemEfficiencyCrystal.isActive}: carried within the last second, it cuts tool drain to a third.
	 * CHROMA-PORT: the Efficiency Crystal (ENERGY tier) is not ported; it writes this same player tag when it lands.
	 */
	private static boolean isEfficiencyActive(Player ep) {
		return ep.level().getGameTime()-ep.getPersistentData().getLongOr(EFFICIENCY_TAG, Long.MIN_VALUE/2) < 20;
	}

	private void setCharge(ItemStack is, int amt) {
		int max = this.getItem(is).getMaxCharge();
		int value = Mth.clamp(amt, 0, max);
		ReikaItemHelper.updateStackTag(is, tag -> tag.putInt("charge", value));
	}

	/** The sprite index offset V33a set as the damage: {@code getChargeState(charge/max)}, or 0 without charge states. */
	public int getChargeState(ItemStack is) {
		PoweredItem pi = this.getItem(is);
		if (!pi.hasChargeStates())
			return 0;
		return pi.getChargeState(this.getCharge(is)/(float)pi.getMaxCharge());
	}

	private PoweredItem getItem(ItemStack is) {
		return (PoweredItem)is.getItem();
	}

	public final int getCharge(ItemStack is) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		return tag != null ? tag.getIntOr("charge", 0) : 0;
	}

	public boolean tickItem(ItemEntity ei) {
		ItemStack is = ei.getItem();
		PoweredItem pi = this.getItem(is);
		int charge = this.getCharge(is);
		Level world = ei.level();
		if (!world.isClientSide()) {
			if (charge < pi.getMaxCharge()) {
				BlockPos pos = ei.blockPosition();
				CrystalElement e = pi.getColor(is);
				int amt = this.getChargeRate(is);
				// V33a looked this up through a temporary receiver restricted to the colour, which only served as the
				// search origin; the colour is checked on the source below.
				CrystalSource s = CrystalNetworker.instance.getNearestTileOfType(world, pos.getX(), pos.getY(), pos.getZ(), CrystalSource.class, CHARGE_RANGE);
				if (s != null && s.isConductingElement(e)) {
					float rate = s.getDroppedItemChargeRate(is);
					if (rate > 0) {
						amt *= rate;
						s.drain(e, amt*4);
						amt = this.scaleChargeRate(amt, world, pos, s);
						ItemStack copy = is.copy();
						this.addCharge(copy, amt);
						// A fresh stack, so the entity data resyncs and the client shows the new charge.
						ei.setItem(copy);
					}
				}
			}
		}
		else {
			if (charge > 0) {
				pi.doChargeFX(ei, charge);
			}
		}
		return false;
	}

	private int scaleChargeRate(int amt, Level world, BlockPos pos, CrystalSource s) {
		if (s instanceof TileEntityCrystalPylon pylon) {
			amt *= 1.25; //25% boost
			if (pylon.isEnhanced())
				amt *= 1.6; //net 2x
		}
		FluidState fs = world.getFluidState(pos);
		if (fs.isSource() && fs.getType().isSame(ChromaFluids.CHROMA.get()))
			amt *= 1.25;
		if (TileEntityLocusPoint.isPointWithin(TileEntityAuraPoint.class, world, pos, 256))
			amt *= 2;
		return amt;
	}

	/**
	 * V33a {@code EntityChargingTool}: the item entity a powered item drops as. It keeps the 5x5 chunks around it loaded
	 * (the whole charging range) until it is picked up or destroyed, and renders at any distance.
	 */
	public static class EntityChargingTool extends ItemEntity implements ChunkLoadingEntity {

		private boolean needsLoad = true;

		public EntityChargingTool(EntityType<? extends EntityChargingTool> type, Level world) {
			super(type, world);
		}

		public EntityChargingTool(Level world, ItemEntity e, ItemStack is) {
			this(ChromaEntityTypes.CHARGING_TOOL.get(), world);
			this.restoreFrom(e);
			this.setItem(is);
			this.lifespan = is.getEntityLifespan(world);
		}

		@Override
		public void tick() {
			super.tick();
			if (needsLoad && !this.isRemoved()) {
				if (this.level() instanceof ServerLevel)
					ChunkManager.instance.loadChunks(this);
				needsLoad = false;
			}
		}

		@Override
		public boolean shouldRenderAtSqrDistance(double distsq) {
			return true;
		}

		@Override
		public Collection<ChunkPos> getChunksToLoad() {
			return ChunkManager.getChunkSquare(Mth.floor(this.getX()), Mth.floor(this.getZ()), CHARGE_RANGE/16);
		}

		@Override
		public void remove(RemovalReason reason) {
			// Unloading with the chunk (or the player) keeps the tickets, which bring it back when the world reloads.
			if (reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER)
				this.onDestroy();
			super.remove(reason);
		}

		@Override
		public void onDestroy() {
			ChunkManager.instance.unloadChunks(this);
		}

	}

}
