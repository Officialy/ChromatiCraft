package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.PylonGridPlacement;

/** Placement policy codecs used by ChromatiCraft's data-driven world generation. */
public final class ChromaPlacementModifiers {

    public static final DeferredRegister<PlacementModifierType<?>> TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, ChromatiCraft.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<PylonGridPlacement>> PYLON_GRID =
            TYPES.register("pylon_grid", () -> () -> PylonGridPlacement.CODEC);

    private ChromaPlacementModifiers() {}
}