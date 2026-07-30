package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.dragonapi.base.CoreContainer;

/** Ten-slot casting-table menu: the V33a 3x3 grid, protected output, and player inventory. */
public final class MenuCastingTable extends CoreContainer<TileEntityCastingTable> {

    private final int[] displayState = new int[1];

    public MenuCastingTable(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, (TileEntityCastingTable)inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public MenuCastingTable(int id, Inventory inventory, TileEntityCastingTable table) {
        super(ChromaMenus.CASTING_TABLE.get(), id, inventory, table);
        boolean multiblock = table.getTier().ordinal() >= TileEntityCastingTable.TableTier.MULTIBLOCK.ordinal();
        int gridY = multiblock ? 57 : 37;
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 3; column++)
                this.addSlot(new Slot(table, row * 3 + column,
                        62 + column * 18, gridY + row * 18));
        this.addSlot(new Slot(table, 9, 189, 12) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
        });
        this.addPlayerInventoryWithOffset(inventory, 0, multiblock ? 74 : 43);
        this.addDataSlot(new DataSlot() {
            @Override public int get() {
                return table.getLevel() != null && !table.getLevel().isClientSide()
                        ? (table.canRunDisplayedRecipe(inventory.player) ? 1 : 0)
                        : displayState[0];
            }
            @Override public void set(int value) { displayState[0] = value; }
        });
    }

    public boolean canRunDisplayedRecipe() { return displayState[0] != 0; }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && tile.isOwnedByPlayer(player)
                && player.distanceToSqr(tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5,
                        tile.getBlockPos().getZ() + 0.5) <= 64;
    }
}


