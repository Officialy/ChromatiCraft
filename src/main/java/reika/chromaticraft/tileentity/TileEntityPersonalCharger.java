package reika.chromaticraft.tileentity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.auxiliary.interfaces.ColoredMultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.CrystalReceiverBase;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.interfaces.WeakRepeaterSafeReceiver;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

/**
 * Full 26.2 port of V33a's colour-bound Personal Charger. Its four equal rune sockets choose the
 * colour; the complete NBT-backed multiblock turns the receiver on, after which it requests that one
 * colour from the crystal network and supplies held-manipulator charging at 40% pylon rate.
 */
public final class TileEntityPersonalCharger extends CrystalReceiverBase
		implements ChargingPoint, OwnedTile, ColoredMultiBlockChromaTile, WeakRepeaterSafeReceiver {

	public static final int CAPACITY = 60_000;
	private static final int RECEIVE_RANGE = 32;
	private static final int MAX_THROUGHPUT = 200;

	private CrystalElement color = CrystalElement.WHITE;
	private boolean hasMultiblock;

	public TileEntityPersonalCharger(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PERSONAL_CHARGER.get(), pos, state);
	}

	@Override
	protected int getCooldownLength() {
		return 800;
	}

	@Override
	public boolean allowsEfficiencyBoost() {
		return false;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (!world.isClientSide()) {
			// V33a receives an explicit multiblock callback on every structure edit. Periodic validation
			// is the 26.2 equivalent and also repairs a structure changed while this chunk was unloaded.
			if (this.getTicksExisted() % 20 == 0)
				this.validateStructure();
			if (this.canConduct() && this.getCooldown() == 0 && checkTimer.checkCap())
				this.checkAndRequest();
			if (this.canConduct()) {
				float pitch = 0.75F;
				if (TileEntityCrystalPylon.TUNED_PYLONS)
					pitch *= (float)CrystalMusicManager.instance.getDingPitchScale(color);
				int interval = Math.max(1, (int)(72F / pitch));
				if (this.getTicksExisted() % interval == 0)
					ChromaSounds.POWER.playSoundAtBlock(this, 0.33F, pitch);
			}
		}
		else if (this.canConduct()) {
			ChromaParticle.spawnPersonalCharger(world, pos, color, this.rand);
		}
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		if (!world.isClientSide()) this.validateStructure();
	}

	/** Re-evaluates both the four-colour key and every authored NBT multiblock cell. */
	@Override
	public void validateStructure() {
		Level world = this.getLevel();
		if (world == null || world.isClientSide()) return;
		CrystalElement keyed = this.readRuneKey(world);
		boolean valid = keyed != null
				&& ChromaStructures.PERSONAL.getArray(world, this.getX(), this.getY(), this.getZ(), keyed)
						.matchInWorld();
		boolean changed = valid != hasMultiblock || valid && keyed != color;
		if (changed) {
			if (valid) {
				color = keyed;
				ChromaSounds.CAST.playSoundAtBlock(this, 1F, 0.5F);
			}
			else {
				ChromaSounds.POWERDOWN.playSoundAtBlock(this, 1F, 0.5F);
				energy.clear();
				checkTimer.setTick(checkTimer.getCap());
			}
			hasMultiblock = valid;
			this.setChanged();
			this.syncAllData(true);
		}
	}

	private CrystalElement readRuneKey(Level world) {
		CrystalElement found = null;
		for (int x : new int[] {-2, 2}) for (int z : new int[] {-2, 2}) {
			BlockState state = world.getBlockState(this.getBlockPos().offset(x, -4, z));
			if (!ChromaBlocks.isRune(state)) return null;
			CrystalElement element = BlockCrystalRune.getColor(state);
			if (found != null && found != element) return null;
			found = element;
		}
		return found;
	}

	private void checkAndRequest() {
		if (this.getEnergy(color) < CAPACITY * 3 / 4)
			this.requestEnergy(color, this.getRemainingSpace(color));
	}

	@Override public boolean isConductingElement(CrystalElement element) {
		return this.canConduct() && element == color;
	}
	@Override public int maxThroughput() { return MAX_THROUGHPUT; }
	@Override public boolean canConduct() { return color != null && hasMultiblock; }
	@Override public int getReceiveRange() { return RECEIVE_RANGE; }
	@Override public boolean allowCharging(Player player, CrystalElement element) { return true; }
	@Override public int getMaxStorage(CrystalElement element) { return element == color ? CAPACITY : 0; }
	@Override public ChromaTiles getTile() { return ChromaTiles.PERSONAL; }
	@Override protected void animateWithTick(Level world, BlockPos pos) {}
	@Override public float getChargeRateMultiplier(Player player, CrystalElement element) { return 0.4F; }
	@Override public void onUsedBy(Player player, CrystalElement element) {}
	@Override public CrystalElement getDeliveredColor(Player player, Level world, int x, int y, int z) { return color; }

	@Override
	public boolean drain(CrystalElement element, int amount) {
		int available = this.getEnergy(element);
		if (available <= 0 || amount <= 0) return false;
		this.drainEnergy(element, Math.min(amount, available));
		this.setChanged();
		return true;
	}

	@Override public CrystalElement getColor() { return color; }
	public int getRenderColor() { return color.getColor(); }
	@Override public Coordinate getChargeParticleOrigin(Player player, CrystalElement element) {
		return new Coordinate(this);
	}
	@Override public ChromaStructures getPrimaryStructure() { return ChromaStructures.PERSONAL; }
	@Override public Coordinate getStructureOffset() { return new Coordinate(0, -6, 0); }
	@Override public boolean canStructureBeInspected() { return true; }
	@Override public boolean hasStructure() { return hasMultiblock; }
	@Override public float getHeldToolChargingPower(Player player, CrystalElement element, ItemStack stack) { return 0; }

	@Override public boolean onlyAllowOwnersToMine() { return true; }
	@Override public boolean onlyAllowOwnersToUse() { return false; }
	@Override public boolean isOwnedByPlayer(Player player) {
		return placerUUID == null || placerUUID.equals(player.getUUID());
	}
	@Override public void getTagsToWriteToStack(CompoundTag tag) {
		super.getTagsToWriteToStack(tag);
	}
	@Override public void setDataFromItemStackTag(ItemStack stack) {
		super.setDataFromItemStackTag(stack);
	}
	@Override public void addTooltipInfo(List list, boolean shift) {}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		int ordinal = Math.clamp(tag.getIntOr("color", CrystalElement.WHITE.ordinal()),
				0, CrystalElement.elements.length - 1);
		color = CrystalElement.elements[ordinal];
		hasMultiblock = tag.getBooleanOr("multi", false);
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("color", color.ordinal());
		tag.putBoolean("multi", hasMultiblock);
	}
}
