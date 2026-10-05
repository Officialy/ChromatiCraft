package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

/** Registry data remains available when the optional VoidMonster runtime is absent. */
public final class VoidRitualDamageTypes {
    public static final ResourceKey<DamageType> KEY = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath("chromaticraft", "void_monster_ritual"));
    private VoidRitualDamageTypes() {}
    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(KEY, new DamageType("voidmonster.ritual", DamageScaling.NEVER, .1F));
    }
}
