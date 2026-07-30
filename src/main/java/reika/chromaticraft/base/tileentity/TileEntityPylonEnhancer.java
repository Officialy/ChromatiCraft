package reika.chromaticraft.base.tileentity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** Shared V33a multi-owner and item-NBT behavior for pylon enhancement blocks. */
public abstract class TileEntityPylonEnhancer extends TileEntityChromaticBase implements OwnedTile, BreakAction {

    private static final String OWNERS_TAG = "owners";
    private final Set<UUID> owners = new HashSet<>();

    protected TileEntityPylonEnhancer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void onSetPlacer(Player ep) {
        super.onSetPlacer(ep);
        if (ep != null)
            owners.add(ep.getUUID());
    }

    public Set<UUID> getOwners() {
        return Set.copyOf(owners);
    }

    public void addOwner(Player ep) {
        if (ep != null && owners.add(ep.getUUID()))
            this.setChanged();
    }

    @Override
    public boolean isOwnedByPlayer(Player ep) {
        return owners.isEmpty() || ep != null && owners.contains(ep.getUUID());
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
    public void getTagsToWriteToStack(CompoundTag tag) {
        this.writeOwners(tag);
    }

    @Override
    public void setDataFromItemStackTag(ItemStack stack) {
        CompoundTag tag = ReikaItemHelper.getStackTag(stack);
        if (tag != null)
            this.readOwners(tag);
    }

    @Override
    public void addTooltipInfo(List li, boolean shift) {
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // V33a worlds created before the owner-list format still carry the base placer UUID.
        if (owners.isEmpty() && this.getPlacerID() != null)
            owners.add(this.getPlacerID());
    }

    @Override
    protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag);
        this.writeOwners(tag);
    }

    @Override
    protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        this.readOwners(tag);
    }

    private void writeOwners(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID id : owners)
            list.add(StringTag.valueOf(id.toString()));
        tag.put(OWNERS_TAG, list);
    }

    private void readOwners(CompoundTag tag) {
        owners.clear();
        ListTag list = tag.getListOrEmpty(OWNERS_TAG);
        for (Tag value : list) {
            try {
                owners.add(UUID.fromString(value.asString().orElse("")));
            }
            catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy/custom item data without losing remaining owners.
            }
        }
        if (owners.isEmpty() && this.getPlacerID() != null)
            owners.add(this.getPlacerID());
    }
}
