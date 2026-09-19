package reika.chromaticraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuCastingTable;
import reika.chromaticraft.container.MenuRitualTable;
import reika.chromaticraft.container.MenuHeatLamp;
import reika.chromaticraft.container.MenuLexiconPages;
import reika.chromaticraft.container.MenuFragmentSelection;
import reika.chromaticraft.container.MenuCrystalCharger;
import reika.chromaticraft.container.MenuCollector;
import reika.chromaticraft.container.MenuStructurePassword;

/** Modern menu registrations for ChromatiCraft's accepted GUI slice. */
public interface ChromaMenus {

    DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, ChromatiCraft.MODID);

    static <T extends AbstractContainerMenu> Supplier<MenuType<T>> register(String id, IContainerFactory<T> factory) {
        return REGISTRY.register(id, () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
    }

    Supplier<MenuType<MenuCastingTable>> CASTING_TABLE = register("casting_table", MenuCastingTable::new);
    Supplier<MenuType<MenuRitualTable>> RITUAL_TABLE = register("ritual_table", MenuRitualTable::new);
    Supplier<MenuType<MenuHeatLamp>> HEAT_LAMP = register("heat_lamp", MenuHeatLamp::new);
	Supplier<MenuType<MenuCrystalCharger>> CRYSTAL_CHARGER = register("crystal_charger", MenuCrystalCharger::new);
	Supplier<MenuType<MenuCollector>> COLLECTOR = register("collector", MenuCollector::new);
	Supplier<MenuType<MenuStructurePassword>> STRUCTURE_PASSWORD = register(
			"structure_password", MenuStructurePassword::new);
    Supplier<MenuType<MenuLexiconPages>> LEXICON_PAGES = register("lexicon_pages", MenuLexiconPages::new);
    Supplier<MenuType<MenuFragmentSelection>> FRAGMENT_SELECTION = register("fragment_selection", MenuFragmentSelection::new);
}
