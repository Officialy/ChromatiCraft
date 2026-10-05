package reika.chromaticraft.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import reika.chromaticraft.auxiliary.interfaces.TieredItem;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaTieredItems;

/** V33a ItemTieredResource: each former metadata variant retains its discovery stage. */
public final class ItemTieredResource extends Item implements TieredItem {
    private final ChromaTieredItems type;

    public ItemTieredResource(Properties properties, ChromaTieredItems type) {
        super(properties);
        this.type = type;
    }

    @Override
    public ProgressStage getDiscoveryTier(ItemStack stack) {
        return switch (type) {
            case CHROMA_DUST, AURA_DUST -> ProgressStage.CRYSTALS;
            case PURITY_DUST -> ProgressStage.RUNEUSE;
            case FOCUS_DUST, RESONANCE_DUST -> ProgressStage.MULTIBLOCK;
            case ELEMENT_DUST, BINDING_CRYSTAL -> ProgressStage.CHARGE;
            case BEACON_DUST -> ProgressStage.PYLON;
            case ENDER_DUST, THERMITIC_CRYSTAL -> ProgressStage.END;
            case WATER_DUST, OCEAN_FRAGMENT -> ProgressStage.OCEAN;
            case FIRAXITE -> ProgressStage.NETHER;
            case LUMA_DUST -> ProgressStage.STRUCTCOMPLETE;
            case ECHO_CRYSTAL -> ProgressStage.CTM;
            case CAVERN_FRAGMENT -> ProgressStage.CAVERN;
            case BURROW_FRAGMENT -> ProgressStage.BURROW;
            case FIRE_ESSENCE -> ProgressStage.LINK;
            case RESONANT_CRYSTAL -> ProgressStage.ABILITY;
            case SPACE_DUST -> ProgressStage.KILLDRAGON;
            case DESERT_FRAGMENT -> ProgressStage.DESERTSTRUCT;
            case LUMA_BEANS -> ProgressStage.ALLOY;
            case BOOST_ROOT -> ProgressStage.TURBOCHARGE;
            case LUMEN_GEM -> ProgressStage.USEENERGY;
            case AVOLITE -> ProgressStage.POWERCRYSTAL;
            case SNOW_FRAGMENT -> ProgressStage.SNOWSTRUCT;
            case PROXIMAL_ESSENCE, PURE_PROXIMAL_ESSENCE -> ProgressStage.DIMENSION;
        };
    }

    @Override public boolean isTiered(ItemStack stack) { return true; }
}
