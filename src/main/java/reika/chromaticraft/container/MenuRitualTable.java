package reika.chromaticraft.container;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;

public final class MenuRitualTable extends AbstractContainerMenu {

    private final TileEntityRitualTable table;
    private boolean fullyEnhanced;

    public MenuRitualTable(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, (TileEntityRitualTable) inventory.player.level()
                .getBlockEntity(data.readBlockPos()));
    }

    public MenuRitualTable(int id, Inventory inventory, TileEntityRitualTable table) {
        super(ChromaMenus.RITUAL_TABLE.get(), id);
        this.table = table;
        this.addDataSlot(new DataSlot() {
            @Override public int get() {
                return table.getLevel() != null && !table.getLevel().isClientSide()
                        ? table.isFullyEnhanced() ? 1 : 0 : fullyEnhanced ? 1 : 0;
            }
            @Override public void set(int value) { fullyEnhanced = value != 0; }
        });
    }

    public TileEntityRitualTable table() { return table; }
    public boolean isFullyEnhanced() { return fullyEnhanced; }

    public List<Ability> selectableAbilities(Player player) {
        List<Ability> options = new ArrayList<>();
        for (Ability ability : Chromabilities.getAbilities()) {
            if (ChromaAbilityData.hasAbility(player, ability)
                    || !ability.isAvailableToPlayer(player))
                continue;
            if (ability instanceof Chromabilities nativeAbility) {
                LexiconCatalog.Entry page = LexiconCatalog.entries().stream()
                        .filter(entry -> entry.section() == LexiconCatalog.Section.ABILITIES
                                && entry.sourceId().equals(nativeAbility.getID()))
                        .findFirst().orElse(null);
                if (page == null || !PlayerResearch.hasFragment(player, page))
                    continue;
                if (!(table.getLevel() != null && !table.getLevel().isClientSide()
                        ? table.isFullyEnhanced() : fullyEnhanced)
                        && (page.level() != null && page.level().ordinal() >= ResearchLevel.CTM.ordinal()
                        || page.requiredProgress().stream()
                                .anyMatch(stage -> stage.isGatedAfter(ProgressStage.DIMENSION))))
                    continue;
            }
            options.add(ability);
        }
        return List.copyOf(options);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!stillValid(player))
            return false;
        List<Ability> registered = Chromabilities.getAbilities();
        if (buttonId < 0 || buttonId >= registered.size())
            return false;
        Ability chosen = registered.get(buttonId);
        if (!selectableAbilities(player).contains(chosen))
            return false;
        table.setChosenAbility(chosen);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return table != null && !table.isRemoved() && table.isOwnedByPlayer(player)
                && player.level() == table.getLevel()
                && player.distanceToSqr(table.getBlockPos().getX() + 0.5,
                        table.getBlockPos().getY() + 0.5,
                        table.getBlockPos().getZ() + 0.5) <= 64;
    }
}
