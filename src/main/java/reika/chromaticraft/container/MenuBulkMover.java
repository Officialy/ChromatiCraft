package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import reika.chromaticraft.items.tools.ItemBulkMover;
import reika.chromaticraft.registry.ChromaMenus;

/** One ghost template slot over the original 117-pixel player inventory, bound to the opened empty mover. */
public final class MenuBulkMover extends AbstractContainerMenu {
    private final Player player;
    private final ItemStack boundTool;
    private final SimpleContainer ghost=new SimpleContainer(1);
    private int number;
    public MenuBulkMover(int id,Inventory inventory,FriendlyByteBuf ignored) { this(id,inventory); }
    public MenuBulkMover(int id,Inventory inventory) {
        super(ChromaMenus.BULK_MOVER.get(),id);
        player=inventory.player;
        boundTool=player.getMainHandItem();
        number=ItemBulkMover.getNumberToCarry(boundTool);
        ghost.setItem(0,ItemBulkMover.getStoredItem(boundTool));
        addSlot(new Slot(ghost,0,8,10) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row=0;row<3;row++) for (int column=0;column<9;column++)
            addSlot(new Slot(inventory,column+row*9+9,8+column*18,34+row*18));
        for (int column=0;column<9;column++) addSlot(new Slot(inventory,column,8+column*18,92));
        // Native menu data is a signed short on the wire; retain the source's five-digit quantity.
        for (int index=0;index<2;index++) {
            final int shift=index*16;
            addDataSlot(new DataSlot() {
                @Override public int get() { return (number>>>shift)&0xffff; }
                @Override public void set(int value) { number=(number&~(0xffff<<shift))|((value&0xffff)<<shift); }
            });
        }
    }
    public int numberToCarry() { return number; }
    public boolean setNumberToCarry(Player sender,int amount) {
        if (sender!=player || !stillValid(sender) || amount<0 || amount>99999) return false;
        number=amount;
        ItemBulkMover.setNumberToCarry(boundTool,amount);
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player sender) {
        return sender==player && sender.getMainHandItem().getItem() instanceof ItemBulkMover
                && ItemBulkMover.getStoredItems(sender.getMainHandItem())==0
                && (sender.level().isClientSide() || sender.getMainHandItem()==boundTool);
    }
    @Override public ItemStack quickMoveStack(Player sender,int slot) { return ItemStack.EMPTY; }
    @Override public void clicked(int slot,int button,ContainerInput input,Player sender) {
        if (!stillValid(sender)) return;
        if (slot==0) {
            if (input!=ContainerInput.PICKUP) return;
            ItemStack selected=getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1);
            ghost.setItem(0,selected);
            if (!sender.level().isClientSide()) ItemBulkMover.setStoredItem(boundTool,selected);
            broadcastChanges();
            return;
        }
        if (input==ContainerInput.SWAP && button>=0 && button<9
                && sender.getInventory().getItem(button).getItem() instanceof ItemBulkMover) return;
        if (slot>=1 && slot<slots.size() && getSlot(slot).getItem().getItem() instanceof ItemBulkMover) return;
        super.clicked(slot,button,input,sender);
    }
    @Override public void removed(Player sender) {
        super.removed(sender);
        if (!sender.level().isClientSide() && stillValid(sender)) ItemBulkMover.setStoredItem(boundTool,ghost.getItem(0));
    }
}
