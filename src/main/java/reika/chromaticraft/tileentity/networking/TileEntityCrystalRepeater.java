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

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.auxiliary.interfaces.SneakPop;
import reika.chromaticraft.base.tileentity.CrystalTransmitterBase;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockPylonStructure.StoneTypes;
import reika.chromaticraft.magic.interfaces.CrystalFuse;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.magic.interfaces.LinkWatchingRepeater;
import reika.chromaticraft.magic.interfaces.NaturalCrystalSource;
import reika.chromaticraft.magic.interfaces.RegionalSensitiveRepeater;
import reika.chromaticraft.magic.network.CrystalLink;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.network.PylonFinder;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * Crystal repeater — relays network energy through a rune+crystalline-stone stalk. Extends
 * {@link CrystalTransmitterBase} and implements the repeater/fuse/multiblock contract.
 *
 * <p>The server-authoritative V33a behavior is active: structure/redstone lifecycle, attenuation and
 * throughput modifiers, regional grouping, rain-sensitive links, overload destruction, owner-aware
 * sneak-pop drops, and custom-data persistence. Client-only particles, range visualization, and surge
 * packets remain presentation work and do not alter network semantics.
 */
public class TileEntityCrystalRepeater extends CrystalTransmitterBase
		implements LinkWatchingRepeater, RegionalSensitiveRepeater, CrystalFuse, NBTTile, SneakPop, OwnedTile, MultiBlockChromaTile {

	public static final int RANGE = 32;

	protected Direction facing = Direction.DOWN;
	private int depth = -1;
	private int states;
	private int clusterCount;
	private int surgeTicks = 0;
	private CrystalElement surgeColor;
	protected int connectionRenderTick = 0;
	private boolean redstoneCache;
	private UUID casterID;

	protected enum StateFlags {
		STRUCTURE, TURBO, ENHANCEDSTRUCT, RAINABLE, RAINLOSS, TABLEGROUPED;

		public final int bitflag = 1 << this.ordinal();
	}

	public TileEntityCrystalRepeater(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.REPEATER.get(), pos, state);
	}

	protected TileEntityCrystalRepeater(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.REPEATER;
	}

	public boolean hasState(StateFlags flag) {
		return (states & flag.bitflag) != 0;
	}

	public void setState(StateFlags flag, boolean enable) {
		if (enable)
			states |= flag.bitflag;
		else
			states &= ~flag.bitflag;
	}

	@Override
	public void onAdjacentBlockUpdate() {
		if (this.getLevel() == null)
			return;
		boolean wasConducting = this.canConduct();
		redstoneCache = this.getLevel().hasNeighborSignal(this.getBlockPos().relative(facing));
		this.validateStructure();
		if (wasConducting && !this.canConduct())
			CrystalNetworker.instance.breakPaths(this);
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		this.validateStructure();
		this.onRegionUpdated();
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (connectionRenderTick > 0)
			connectionRenderTick--;
		if (!world.isClientSide())
			this.setState(StateFlags.RAINLOSS, this.hasState(StateFlags.RAINABLE) && world.isRaining());
		if (surgeTicks > 0 && --surgeTicks == 0)
			this.doSurge();
		// Deferred: rain/enhanced/surge particles and progression-catchup presentation.
	}

	@Override
	public int getSendRange() {
		return RANGE;
	}

	@Override
	public int getReceiveRange() {
		return RANGE;
	}

	@Override
	public boolean canConduct() {
		return this.hasStructure() && !redstoneCache;
	}

	@Override
	public boolean hasStructure() {
		return this.hasState(StateFlags.STRUCTURE);
	}

	@Override
	public final void validateStructure() {
		this.setState(StateFlags.STRUCTURE, this.checkForStructure());
		this.setState(StateFlags.ENHANCEDSTRUCT, this.hasStructure() && this.isTurbocharged() && this.checkEnhancedStructure());
		if (!this.hasStructure())
			CrystalNetworker.instance.breakPaths(this);
		this.syncAllData(false);
	}

	protected BlockState stateAt(int dx, int dy, int dz) {
		return this.getLevel().getBlockState(this.getBlockPos().offset(dx, dy, dz));
	}

	protected Direction getFacing() {
		return facing;
	}

	protected boolean checkForStructure() {
		Direction dir = facing;
		BlockState front = this.stateAt(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		if (!ChromaBlocks.isRune(front))
			return false;
		for (int i = 2; i < 4; i++) {
			BlockState s = this.stateAt(dir.getStepX() * i, dir.getStepY() * i, dir.getStepZ() * i);
			if (s.getBlock() != ChromaBlocks.PYLONSTRUCT.get())
				return false;
			int type = s.getValue(BlockPylonStructure.TYPE);
			int m2 = (i == 3 && this.isTurbocharged()) ? StoneTypes.RESORING.ordinal() : 0;
			if (type != 0 && type != m2)
				return false;
		}
		return true;
	}

	protected boolean checkEnhancedStructure() {
		BlockState s = this.stateAt(facing.getStepX() * 3, facing.getStepY() * 3, facing.getStepZ() * 3);
		return s.getBlock() == ChromaBlocks.PYLONSTRUCT.get() && s.getValue(BlockPylonStructure.TYPE) == StoneTypes.RESORING.ordinal();
	}

	public void redirect(int side) {
		Direction oldFacing = facing;
		facing = Direction.from3DDataValue(side).getOpposite();
		if (oldFacing != facing)
			CrystalNetworker.instance.breakPaths(this);
		this.validateStructure();
	}

	public boolean findFirstValidSide() {
		for (Direction d : Direction.values()) {
			facing = d;
			this.validateStructure();
			if (this.hasStructure())
				return true;
		}
		return false;
	}

	public final boolean isTurbocharged() {
		return this.hasState(StateFlags.TURBO);
	}

	public final boolean isEnhancedStructure() {
		return this.hasState(StateFlags.ENHANCEDSTRUCT);
	}

	public final boolean isRainAffected() {
		return this.hasState(StateFlags.RAINLOSS);
	}

	public final boolean isTableGrouped() {
		return this.hasState(StateFlags.TABLEGROUPED);
	}

	@Override
	public int maxThroughput() {
		int ret = 1000;
		if (this.isTurbocharged()) {
			ret *= 9;
			if (this.isEnhancedStructure())
				ret *= 2;
		}
		if (this.isTableGrouped())
			ret *= 2;
		else if (clusterCount > 0)
			ret *= 1 + Math.min(0.5, clusterCount * 0.5 / 6D);
		return ret;
	}

	@Override
	public int getSignalDegradation(boolean point) {
		int ret = this.isTurbocharged() ? 0 : 10;
		if (this.isRainAffected())
			ret += 5 + ret / 2;
		if (this.isTableGrouped())
			ret *= 0.75;
		else if (clusterCount > 0)
			ret *= Math.max(0.5, 1 - clusterCount * 0.5 / 3D);
		if (this.isEnhancedStructure())
			ret /= 2;
		return ret;
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return e != null && e == this.getActiveColor();
	}

	@Override
	public final int receiveElement(CrystalSource src, CrystalElement e, int amt) {
		return 1;
	}

	@Override
	public DecimalPosition getTargetRenderOffset(CrystalElement e) {
		return null;
	}

	public boolean checkConnectivity() {
		CrystalElement c = this.getActiveColor();
		return c != null && CrystalNetworker.instance.checkConnectivity(c, this);
	}

	public CrystalElement getActiveColor() {
		if (!this.canConduct())
			return null;
		BlockState s = this.stateAt(facing.getStepX(), facing.getStepY(), facing.getStepZ());
		return ChromaBlocks.isRune(s) ? BlockCrystalRune.getColor(s) : null;
	}

	@Override
	public boolean needsLineOfSightToReceiver(CrystalReceiver r) {
		return true;
	}

	@Override
	public boolean needsLineOfSightFromTransmitter(CrystalTransmitter r) {
		return true;
	}

	@Override
	public int getSignalDepth(CrystalElement e) {
		return depth;
	}

	@Override
	public void setSignalDepth(CrystalElement e, int d) {
		if (e == this.getActiveColor())
			depth = d;
	}

	@Override
	public boolean canTransmitTo(CrystalReceiver r) {
		return true;
	}

	@Override
	public boolean canReceiveFrom(CrystalTransmitter r) {
		return true;
	}

	@Override
	public boolean canBeSuppliedBy(CrystalSource te, CrystalElement e) {
		return true;
	}

	@Override
	public int getPathPriority() {
		return 10;
	}

	public int getThoughputBonus(boolean point) {
		return 0;
	}

	public int getThoughputInsurance() {
		return 0;
	}

	// ---- CrystalFuse ----

	@Override
	public float getFailureWeight(CrystalElement e) {
		return 1.5F;
	}

	@Override
	public final void overload(CrystalElement e) {
		if (e != null && surgeTicks == 0) {
			surgeColor = e;
			surgeTicks = 55;
			CrystalNetworker.instance.breakPaths(this);
			this.syncAllData(false);
		}
	}

	public final boolean isSurging() {
		return surgeTicks > 0;
	}

	public final int getSurgeTicks() {
		return surgeTicks;
	}

	private void doSurge() {
		if (this.getLevel() == null || this.getLevel().isClientSide() || surgeColor == null)
			return;
		this.removeFromCache();
		CrystalNetworker.instance.breakPaths(this);
		for (int distance = 1; distance < this.getLevel().getMaxY() - this.getLevel().getMinY(); distance++) {
			BlockPos target = this.getBlockPos().relative(facing, distance);
			BlockState state = this.getLevel().getBlockState(target);
			if (state.getBlock() != ChromaBlocks.PYLONSTRUCT.get() && !ChromaBlocks.isRune(state))
				break;
			this.getLevel().destroyBlock(target, true);
		}
		int powder = rand.nextInt(12);
		for (int i = 0; i < powder; i++) {
			ReikaItemHelper.dropItem(this.getLevel(), this.getX() + rand.nextDouble(), this.getY(),
					this.getZ() + rand.nextDouble(), new ItemStack(ChromaItems.CRYSTAL_POWDER.get()));
		}
		this.delete();
	}

	// ---- MultiBlockChromaTile ----

	@Override
	public ChromaStructures getPrimaryStructure() {
		return ChromaStructures.REPEATER;
	}

	@Override
	public Coordinate getStructureOffset() {
		return null;
	}

	@Override
	public boolean canStructureBeInspected() {
		return false;
	}

	// ---- LinkWatchingRepeater / RegionalSensitiveRepeater ----

	@Override
	public final void onLinkRecalculated(CrystalLink link) {
		boolean rainable = this.canBeRainAffected(link) && link.isRainable()
				&& PylonFinder.isRainableBiome(this.getLevel().getBiome(this.getBlockPos()));
		this.setState(StateFlags.RAINABLE, rainable);
		this.setState(StateFlags.RAINLOSS, rainable && this.getLevel().isRaining());
	}

	protected boolean canBeRainAffected(CrystalLink link) {
		if (this.isTurbocharged())
			return false;
		var other = link.getOtherEnd(this);
		if (other instanceof NaturalCrystalSource)
			return false;
		return !(other instanceof TileEntityCrystalRepeater repeater) || !repeater.isTurbocharged();
	}

	@Override
	public void onRegionUpdated() {
		clusterCount = Math.max(0, CrystalNetworker.instance
				.getNearTilesOfType(this, TileEntityCrystalRepeater.class, this.getSensitivityRadius()).size() - 1);
	}

	@Override
	public int getSensitivityRadius() {
		return 5;
	}

	public final int getClusterCount() {
		return clusterCount;
	}

	public final boolean hasClusterRender() {
		return clusterCount >= 3;
	}

	public final void markAsTableGrouped(boolean grouped) {
		this.setState(StateFlags.TABLEGROUPED, grouped);
		CrystalNetworker.instance.breakPaths(this);
	}

	// ---- SneakPop / OwnedTile ----

	@Override
	public final void drop() {
		if (this.getLevel() == null || this.getLevel().isClientSide())
			return;
		ItemStack stack = new ItemStack(this.getTile().getBlock());
		CompoundTag tag = new CompoundTag();
		this.getTagsToWriteToStack(tag);
		ReikaItemHelper.setStackTag(stack, tag);
		ReikaItemHelper.dropItem(this.getLevel(), this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5, stack);
		this.removeFromCache();
		CrystalNetworker.instance.breakPaths(this);
		this.delete();
	}

	@Override
	public boolean canDrop(Player ep) {
		return placerUUID == null || ep.getUUID().equals(placerUUID);
	}

	@Override
	public boolean allowMining(Player ep) {
		return this.canDrop(ep);
	}

	@Override
	public boolean onlyAllowOwnersToMine() {
		return true;
	}

	@Override
	public boolean onlyAllowOwnersToUse() {
		return false;
	}

	@Override
	public boolean isOwnedByPlayer(Player ep) {
		return placerUUID == null || ep.getUUID().equals(placerUUID);
	}

	// ---- NBTTile ----

	@Override
	public void getTagsToWriteToStack(CompoundTag NBT) {
		NBT.putBoolean("boosted", this.isTurbocharged());
		if (casterID != null)
			NBT.putString("caster", casterID.toString());
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		boolean repeaterItem = is != null && is.is(this.getTile().getBlock().asItem());
		CompoundTag tag = repeaterItem ? ReikaItemHelper.getStackTag(is) : null;
		this.setState(StateFlags.TURBO, repeaterItem && tag != null && tag.getBooleanOr("boosted", false));
		casterID = repeaterItem && tag != null ? readUUID(tag.getStringOr("caster", "")) : null;
		this.validateStructure();
	}

	public final UUID getCaster() {
		return casterID;
	}

	@Override
	public void addTooltipInfo(List li, boolean shift) {

	}

	// ---- NBT ----

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		facing = Direction.from3DDataValue(NBT.getIntOr("face", 0));
		depth = NBT.getIntOr("depth", 0);
		clusterCount = NBT.getIntOr("cluster", 0);
		states = NBT.getIntOr("states", 0);
		surgeTicks = NBT.getIntOr("surge", 0);
		int surgeOrdinal = NBT.getIntOr("surge_c", -1);
		surgeColor = surgeOrdinal >= 0 && surgeOrdinal < CrystalElement.elements.length ? CrystalElement.elements[surgeOrdinal] : null;
		redstoneCache = NBT.getBooleanOr("redstone", false);
		casterID = readUUID(NBT.getStringOr("caster", ""));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		if (facing != null)
			NBT.putInt("face", facing.get3DDataValue());
		NBT.putInt("depth", depth);
		NBT.putInt("cluster", clusterCount);
		NBT.putInt("states", states);
		NBT.putInt("surge", surgeTicks);
		if (surgeColor != null)
			NBT.putInt("surge_c", surgeColor.ordinal());
		NBT.putBoolean("redstone", redstoneCache);
		if (casterID != null)
			NBT.putString("caster", casterID.toString());
	}

	private static UUID readUUID(String value) {
		if (value == null || value.isEmpty())
			return null;
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	// CHROMA-PORT: Thaumcraft compatibility is intentionally dormant. V33a's adjacent pylon/network
	// integration exposed Thaumcraft node and wand hooks, including aspect reporting and wand-driven
	// pylon draining. There is no modern Thaumcraft target for Minecraft 26.2, so do not import or
	// register Thaumcraft APIs here; restore this only through an isolated optional integration layer.
}