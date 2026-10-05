package reika.chromaticraft.modinterface.ae;

import appeng.api.AECapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import reika.chromaticraft.items.tools.ItemBulkMover.InventoryInteraction;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;

/** AE-typed bridge, loaded only with AE2; the native node-host capability includes DragonAPI grid hosts. */
public final class BulkMoverAE {
    private BulkMoverAE() {}
    public static InventoryInteraction find(Level level,BlockPos pos,Direction side,Player player) {
        var host=level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST,pos,null);
        if (host==null || host.getGridNode(side)==null) return null;
        return new NetworkInventory(new MESystemReader(host.getGridNode(side),player));
    }
    private record NetworkInventory(MESystemReader network) implements InventoryInteraction {
        @Override public int addItems(ItemStack template,int maximum) {
            return maximum-(int)network.addItem(template.copyWithCount(maximum),false);
        }
        @Override public int drawItems(ItemStack template,int maximum) {
            return (int)network.removeItem(template.copyWithCount(maximum),false,true);
        }
    }
}
