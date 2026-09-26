/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code TileEntityChromaticBase}: every ChromatiCraft tile, with V33a's owner set. The placer becomes an owner when
 * placed, the Ownership Expansion Key adds more, and {@link #isOwnedByPlayer} (final, as in V33a) admits any owner --
 * or anyone at all while the set is empty, as for a worldgen tile. {@link OwnedTile}s carry their owners on the dropped
 * item ({@link #writeOwnerData}/{@link #readOwnerData}), merged into the set of whoever places it.
 *
 * <p>The set rides in the sync tag, which is also what the tile saves, so clients see ownership (menus check it on
 * both sides). Tiles saved before the port had the set only knew their placer, so on load an empty set with a known
 * placer takes that placer, keeping existing placed tiles private.
 */
public abstract class TileEntityChromaticBase extends BlockEntityBase {

	private static final String OWNERS_TAG = "owners";

	protected final HashSet<UUID> owners = new HashSet<>();

	protected TileEntityChromaticBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract ChromaTiles getTile();

	@Override
	public Block getBlockEntityBlockID() {
		return this.getTile().getBlock();
	}

	@Override
	public String getTEName() {
		return this.getTile().getName();
	}

	@Override
	protected void onSetPlacer(Player ep) {
		super.onSetPlacer(ep);
		if (ep != null)
			this.addOwner(ep);
	}

	public final void addOwner(Player ep) {
		if (ep != null && owners.add(ep.getUUID()))
			this.setChanged();
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	public final boolean isOwnedByPlayer(Player ep) {
		return this.isOwnedByPlayer(ep.getUUID());
	}

	public final boolean isOwnedByPlayer(UUID id) {
		return owners.isEmpty() || owners.contains(id);
	}

	/** The owners currently in this tile's level. */
	public final Collection<Player> getOwners(boolean allowFake) {
		Collection<Player> c = new ArrayList<>();
		if (this.getLevel() == null)
			return c;
		for (UUID uid : owners) {
			Player ep = this.getLevel().getPlayerByUUID(uid);
			if (ep != null && (allowFake || !ReikaPlayerAPI.isFake(ep))) {
				c.add(ep);
			}
		}
		return c;
	}

	public final Set<UUID> getOwnerIDs() {
		return Set.copyOf(owners);
	}

	public boolean onlyAllowOwnersToUse() {
		return false;
	}

	public boolean onlyAllowOwnersToMine() {
		return true;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.put(OWNERS_TAG, ownerList());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		if (NBT.contains(OWNERS_TAG)) {
			owners.clear();
			readOwnerList(NBT);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (owners.isEmpty() && this.getPlacerID() != null)
			owners.add(this.getPlacerID());
	}

	private ListTag ownerList() {
		ListTag li = new ListTag();
		for (UUID uid : owners)
			li.add(StringTag.valueOf(uid.toString()));
		return li;
	}

	/** Adds (V33a readCollectionFromNBT never cleared) every UUID in the tag's owner list. */
	private void readOwnerList(CompoundTag NBT) {
		for (Tag value : NBT.getListOrEmpty(OWNERS_TAG)) {
			try {
				owners.add(UUID.fromString(value.asString().orElse("")));
			}
			catch (IllegalArgumentException ignored) {
				// A malformed entry must not cost the remaining owners.
			}
		}
	}

	/** V33a {@code writeOwnerData}: an owned tile's owners go onto its dropped item. */
	protected final void writeOwnerData(CompoundTag NBT) {
		if (this instanceof OwnedTile && !owners.isEmpty())
			NBT.put(OWNERS_TAG, ownerList());
	}

	/** V33a {@code readOwnerData}: an owned tile placed from an item joins the item's owners to its own. */
	protected final void readOwnerData(ItemStack is) {
		CompoundTag tag = is != null ? ReikaItemHelper.getStackTag(is) : null;
		if (tag != null && tag.contains(OWNERS_TAG) && this instanceof OwnedTile) {
			this.readOwnerList(tag);
			this.setChanged();
		}
	}
}
