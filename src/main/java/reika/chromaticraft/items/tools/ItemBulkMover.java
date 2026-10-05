/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.container.MenuBulkMover;
import reika.dragonapi.ModList;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.List;
import java.util.function.Consumer;

/** V33a Bulk Item Mover: a component-preserving template and a separate bulk count, with native/ME inventory transfers. */
public class ItemBulkMover extends ItemChromaTool {
    public ItemBulkMover(Properties properties) { super(properties); }
    public static int getStoredItems(ItemStack tool) {
        var tag=ReikaItemHelper.getStackTag(tool);
        return tag==null ? 0 : Math.max(0,tag.getIntOr("count",0));
    }
    public static ItemStack getStoredItem(ItemStack tool) {
        var contents=tool.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        return contents.copyOne();
    }
    public static void setStoredItem(ItemStack tool, ItemStack template) {
        tool.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(
                template.isEmpty() ? List.of() : List.of(template.copyWithCount(1))));
    }
    public static int getNumberToCarry(ItemStack tool) {
        var tag=ReikaItemHelper.getStackTag(tool);
        return tag==null ? 0 : Math.max(0,tag.getIntOr("space",0));
    }
    public static void setNumberToCarry(ItemStack tool, int amount) {
        ReikaItemHelper.updateStackTag(tool,tag->tag.putInt("space",Math.max(0,amount)));
    }
    private static void setCount(ItemStack tool,int count) {
        ReikaItemHelper.updateStackTag(tool,tag->tag.putInt("count",Math.max(0,count)));
    }

    @Override public InteractionResult use(Level world,Player player,InteractionHand hand) {
        if (hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS; // V33a's held-item menu.
        var tool=player.getItemInHand(hand);
        if (getStoredItems(tool)==0 && player instanceof ServerPlayer server)
            server.openMenu(new SimpleMenuProvider((id,inventory,p)->new MenuBulkMover(id,inventory),getName(tool)));
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player) || !player.mayInteract(player.level(),context.getClickedPos()))
            return InteractionResult.PASS;
        var tool=context.getItemInHand();
        ItemStack template=getStoredItem(tool);
        int limit=getNumberToCarry(tool);
        if (template.isEmpty() || limit<=0) return InteractionResult.PASS;
        InventoryInteraction inventory=null;
        if (ModList.APPENG.isLoaded())
            inventory=reika.chromaticraft.modinterface.ae.BulkMoverAE.find(context.getLevel(),context.getClickedPos(),context.getClickedFace(),player);
        if (inventory==null) {
            var handler=context.getLevel().getCapability(Capabilities.Item.BLOCK,context.getClickedPos(),context.getClickedFace());
            if (handler!=null) inventory=new NativeInventory(handler);
        }
        if (inventory!=null) transfer(tool,inventory);
        // V33a onItemUse performed its transfer and returned false.
        return InteractionResult.PASS;
    }

    public static int transfer(ItemStack tool,InventoryInteraction inventory) {
        ItemStack template=getStoredItem(tool);
        if (template.isEmpty() || getNumberToCarry(tool)<=0) return 0;
        int stored=getStoredItems(tool);
        int moved=stored>0 ? inventory.addItems(template,stored) : inventory.drawItems(template,getNumberToCarry(tool));
        int requested=stored>0 ? stored : getNumberToCarry(tool);
        if (moved<0 || moved>requested) throw new IllegalStateException("Inventory returned an invalid bulk transfer amount: "+moved);
        setCount(tool,stored>0 ? stored-moved : moved);
        return moved;
    }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,
            Consumer<Component> tooltip,TooltipFlag flag) {
        ItemStack template=getStoredItem(stack);
        tooltip.accept(template.isEmpty() ? Component.translatable("item.chromaticraft.bulk_mover.empty")
                : Component.translatable("item.chromaticraft.bulk_mover.contents",getStoredItems(stack),template.getHoverName(),getNumberToCarry(stack)));
    }

    public interface InventoryInteraction {
        int addItems(ItemStack template,int maximum);
        int drawItems(ItemStack template,int maximum);
    }

    /** Transaction commits before the item's count changes, so rejected/partial transfers cannot duplicate items. */
    public record NativeInventory(ResourceHandler<ItemResource> handler) implements InventoryInteraction {
        @Override public int addItems(ItemStack template,int maximum) {
            try (var transaction=Transaction.openRoot()) {
                int moved=handler.insert(ItemResource.of(template),maximum,transaction);
                transaction.commit();
                return moved;
            }
        }
        @Override public int drawItems(ItemStack template,int maximum) {
            try (var transaction=Transaction.openRoot()) {
                int moved=handler.extract(ItemResource.of(template),maximum,transaction);
                transaction.commit();
                return moved;
            }
        }
    }
}

