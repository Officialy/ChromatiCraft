package reika.chromaticraft.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.auxiliary.structure.PortalStructure;
import reika.chromaticraft.block.BlockChromaPortal;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.dragonapi.base.BlockEntityBase;

/**
 * V33a {@code BlockChromaPortal.TileEntityCrystalPortal}.
 *
 * <p>Every one of the nine pad blocks carries this entity — V33a's {@code hasTileEntity} returns an
 * unconditional {@code true} and its {@code meta == 1} restriction is commented out — but only the
 * centre one does any work. "Centre" is {@link BlockChromaPortal#getPortalPosition} returning 5 with
 * a complete 3x3 around it, which is what the source's misleadingly named {@code isFull9x9()}
 * actually tests.
 *
 * <p>Charge is deliberately <em>not</em> a crystal-network cost. Nothing in V33a supplies it: while
 * the structure matches, {@code charge} simply increments once per tick until it reaches
 * {@link #MINCHARGE} (300 ticks = 15 seconds), and it keeps incrementing past that for as long as the
 * dimension generators are not ready. Losing the structure zeroes it and it must be earned again.
 *
 * <p>Tuning is separate stored energy fed by throwing Proximal Essence into the rift; it decays on
 * average once every 400 ticks and 60% of it is spent on each trip.
 */
public class TileEntityCrystalPortal extends BlockEntityBase {

	/** V33a MINCHARGE: 300 ticks of a matching structure before the rift will carry anyone. */
	public static final int MINCHARGE = 300;

	private boolean complete;
	private int charge;
	private int tuning;

	public TileEntityCrystalPortal(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PORTAL.get(), pos, state);
	}

	@Override
	public Block getBlockEntityBlockID() {
		return ChromaBlocks.PORTAL.get();
	}

	@Override
	public String getTEName() {
		return "portal";
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		this.validateStructure();
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		int ticks = this.getTicksExisted();
		if (complete) {
			if (charge < MINCHARGE || !BlockChromaPortal.areGeneratorsReady(world))
				charge++;
		}
		else {
			charge = 0;
		}
		if (world.isClientSide())
			return;
		if (!this.isPadCentre())
			return;
		if (ticks % 20 == 0)
			this.validateStructure();
		if (complete) {
			if (ticks % 90 == 0)
				ChromaSounds.PORTAL.playSoundAtBlock(this);
			if (tuning > 0 && rand.nextInt(400) == 0) {
				tuning--;
				this.syncAllData(false);
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (!world.isClientSide())
			return;
		int ticks = this.getTicksExisted();
		if (complete && (charge < MINCHARGE || !BlockChromaPortal.areGeneratorsReady(world)))
			ChromaParticle.spawnPortalCharging(world, pos, this.getFocusPositions(), ticks, world.getRandom());
		if (!this.isPadCentre())
			return;
		ChromaParticle.spawnPortalIdle(world, pos, ticks, world.getRandom());
		if (complete && charge >= MINCHARGE)
			ChromaParticle.spawnPortalActive(world, pos, ticks, world.getRandom());
	}

	/**
	 * V33a probed {@code (x+-3, y+5, z+-3)} for {@code PYLONSTRUCT} metadata 5 before emitting a
	 * charging bolt, which is the Pylon Focus corner of the fountain arch.
	 */
	private List<BlockPos> getFocusPositions() {
		List<BlockPos> found = new ArrayList<>(4);
		Block focus = ChromaBlocks.crystallineStone(StoneTypes.FOCUS).get();
		for (int dx = -3; dx <= 3; dx += 6) {
			for (int dz = -3; dz <= 3; dz += 6) {
				BlockPos check = worldPosition.offset(dx, 5, dz);
				if (level.getBlockState(check).is(focus))
					found.add(check);
			}
		}
		return found;
	}

	/** V33a getPortalPosition()==5 && isFull9x9(): this block is the middle of a complete 3x3 pad. */
	public boolean isPadCentre() {
		return level != null && BlockChromaPortal.getPortalPosition(level, worldPosition) == 5
				&& BlockChromaPortal.isFullPad(level, worldPosition);
	}

	/**
	 * V33a validateStructure: a return rift is unconditionally complete and needs no structure at
	 * all; every other portal must match the whole authored multiblock <em>and</em> have its eight
	 * Ender Crystals in place.
	 */
	public void validateStructure() {
		if (level == null || level.isClientSide())
			return;
		boolean last = complete;
		if (this.isReturnPortal()) {
			complete = true;
		}
		else {
			ChromaStructures.PORTAL.getStructure().resetToDefaults();
			complete = ChromaStructures.PORTAL
					.getArray(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
					.matchInWorld();
			complete &= this.hasEnderCrystals();
		}
		if (last != complete)
			this.syncAllData(false);
	}

	/** V33a getEntities(): exactly one vanilla Ender Crystal in each of the eight ring cells. */
	private boolean hasEnderCrystals() {
		for (BlockPos relative : PortalStructure.ENDER_CRYSTALS) {
			BlockPos check = worldPosition.offset(relative);
			AABB box = new AABB(check);
			if (level.getEntitiesOfClass(EndCrystal.class, box).size() != 1)
				return false;
		}
		return true;
	}

	public boolean isReturnPortal() {
		return this.getBlockState().getBlock() instanceof BlockChromaPortal portal && portal.isReturnPortal();
	}

	/**
	 * V33a addTuningEnergy: {@code (tier2 ? 150 : 1) * stackSize^(tier2 ? 0.85 : 0.5)}. Ordinary
	 * Proximal Essence therefore scales with the square root of the stack and one Pure Proximal
	 * Essence is worth 150 of it.
	 */
	public void addTuningEnergy(ItemStack is) {
		boolean pure = is.is(ChromaItems.TIERED.get(ChromaTieredItems.PURE_PROXIMAL_ESSENCE).get());
		tuning += (int)((pure ? 150 : 1) * Math.pow(is.getCount(), pure ? 0.85 : 0.5));
		this.syncAllData(false);
	}

	/** V33a: the tuning a trip carries, after which 60% of it is spent. */
	public int consumeTuningForTrip() {
		int carried = tuning;
		tuning = (int)(tuning * 0.4);
		this.syncAllData(false);
		return carried;
	}

	/**
	 * V33a ownedBy: a portal with no recorded placer belongs to everyone, so a naturally occurring
	 * rift can be dismantled by any player holding the Manipulator.
	 */
	public boolean ownedBy(Player ep) {
		UUID owner = this.getPlacerID();
		return owner == null || owner.equals(ep.getUUID());
	}

	/**
	 * V33a canPlayerUse: the dimension has to be reachable at all, the rift has to have finished
	 * charging, and the player must hold every prerequisite of
	 * {@link ProgressStage#DIMENSION} — the stage itself is granted by arriving.
	 */
	public boolean canPlayerUse(Player ep) {
		return BlockChromaPortal.isPortalFunctional(level) && charge >= MINCHARGE
				&& ProgressStage.DIMENSION.playerHasPrerequisites(ep);
	}

	public int getCharge() {
		return charge;
	}

	public int getTuning() {
		return tuning;
	}

	public boolean isComplete() {
		return complete;
	}

	/** Test seam: lets a focused GameTest drive charge without waiting 300 ticks in real time. */
	public void setChargeForTest(int value) {
		charge = value;
		this.syncAllData(false);
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putBoolean("built", complete);
		tag.putInt("charge", charge);
		tag.putInt("tuning", tuning);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		complete = tag.getBooleanOr("built", false);
		charge = tag.getIntOr("charge", 0);
		tuning = tag.getIntOr("tuning", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("built", complete);
		output.putInt("charge", charge);
		output.putInt("tuning", tuning);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		complete = input.getBooleanOr("built", false);
		charge = input.getIntOr("charge", 0);
		tuning = input.getIntOr("tuning", 0);
	}

	/**
	 * V33a getRenderBoundingBox: the block AABB expanded by eight in every direction, because the
	 * rift's own effect volume is far larger than its block.
	 */
	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(8);
	}

	/** Convenience for the block: the centre entity of the pad this position belongs to. */
	public static TileEntityCrystalPortal findCentre(Level world, BlockPos pos) {
		if (!(world.getBlockEntity(pos) instanceof TileEntityCrystalPortal te))
			return null;
		return te.isPadCentre() ? te : null;
	}

	/** Server-only: the entity that should receive a player's trip, or null if it is not usable. */
	public ServerLevel serverLevel() {
		return level instanceof ServerLevel server ? server : null;
	}

	public static boolean isPortalEntity(Entity e) {
		return e instanceof ServerPlayer;
	}
}
