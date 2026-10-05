package reika.chromaticraft.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Serialized biome weather rules; syncable so client precipitation uses the same cooling. */
public final class ChromaWeatherAttributes {
    public static final DeferredRegister<EnvironmentAttribute<?>> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ENVIRONMENT_ATTRIBUTE, "chromaticraft");
    public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Boolean>> RAINBOW_COOLING =
            ATTRIBUTES.register("rainbow_height_cooling", () -> EnvironmentAttribute.builder(AttributeTypes.BOOLEAN)
                    .defaultValue(false).syncable().build());
    public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Boolean>> RAINLESS_THUNDER =
            ATTRIBUTES.register("rainless_thunder", () -> EnvironmentAttribute.builder(AttributeTypes.BOOLEAN)
                    .defaultValue(false).syncable().build());
    private ChromaWeatherAttributes() {}
}
