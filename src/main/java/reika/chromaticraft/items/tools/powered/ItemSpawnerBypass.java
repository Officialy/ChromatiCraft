package reika.chromaticraft.items.tools.powered;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import reika.chromaticraft.base.ItemPoweredChromaTool;
import reika.chromaticraft.items.tools.ItemAuraPouch;
import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.RayTracer;

import java.util.Comparator;

/** V33a SPAWNERBYPASS: a carried, charged gem requires visual line of sight to activate nearby spawners. */
public class ItemSpawnerBypass extends ItemPoweredChromaTool {
    public static final int MAX_CHARGE = 72000;
    private static final Identifier TWILIGHT_FOREST = Identifier.fromNamespaceAndPath("twilightforest", "twilight_forest");

    public ItemSpawnerBypass(Properties properties) { super(properties); }
    @Override public CrystalElement getColor(ItemStack stack) { return CrystalElement.LIGHTGRAY; }
    @Override public int getMaxCharge() { return MAX_CHARGE; }
    @Override public int getChargeStates() { return 1; }
    @Override protected boolean isActivated(Player player, ItemStack stack, boolean held) { return true; }
    @Override public int getChargeConsumptionRate(Player player, Level world, ItemStack stack) {
        return world.dimension().identifier().equals(TWILIGHT_FOREST) ? 12 : 1;
    }
    @Override protected boolean doTick(ItemStack stack, Level world, Player player, boolean held) {
        player.getPersistentData().putLong("spawnertoggle", world.getGameTime());
        return isWorking(player);
    }

    public static boolean isActive(Player player) {
        if (player.level().getGameTime()-player.getPersistentData().getLongOr("spawnertoggle", 0) < 5) return true;
        // Native inventoryTick is server-only; the local spawner's display also needs the source LOS rule.
        if (player.level().isClientSide()) {
            for (ItemStack stack : player.getInventory()) if (isCharged(stack)) return true;
            return ItemAuraPouch.containsActive(player, ItemSpawnerBypass::isCharged);
        }
        return false;
    }
    private static boolean isCharged(ItemStack stack) {
        return stack.getItem() instanceof ItemSpawnerBypass && ToolChargingSystem.instance.getCharge(stack) > 0;
    }
    public static boolean isWorking(Player player) {
        return player.level().getGameTime()-player.getPersistentData().getLongOr("spawnerpass", 0) < 20;
    }

    /** Native activation hook replacing V33a's SpawnerCheckPlayerEvent handler, for blocks and minecarts. */
    public static boolean checkSpawnerActivation(Level world, BlockPos pos, int range, boolean vanilla) {
        if (!vanilla) return false;
        double x=pos.getX()+0.5, y=pos.getY()+0.5, z=pos.getZ()+0.5;
        Player player = world.players().stream().filter(p -> p.isAlive() && !p.isSpectator())
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(x,y,z))).orElse(null);
        if (player == null || !isActive(player)) return vanilla;
        if (player.distanceToSqr(x,y,z) <= (double)range*range)
            player.getPersistentData().putLong("spawnerpass", world.getGameTime());
        return RayTracer.getVisualLOS().addTransparentBlock(Blocks.SPAWNER).addTransparentBlock(Blocks.COBWEB)
                .setOrigins(x,y,z,player.getX(),player.getY(),player.getZ()).isClearLineOfSight(world);
    }
}

