package reika.chromaticraft.modinterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Optional V33a SFAPI doggo spawn-point integration. Satisforestry has no active 26.3 module here;
 * keep the real API calls available without requiring its classes on a dedicated server.
 * CHROMA-PORT: replace reflection with the modern typed SFAPI when that port lands.
 */
public final class SatisforestrySonar {
    private SatisforestrySonar() {}
    public record SpawnPoint(BlockPos pos, Identifier entityType) {}

    public static boolean isDoggo(Entity entity) {
        if (!ModList.get().isLoaded("satisforestry")) return false;
        try {
            return Class.forName("reika.satisforestry.api.LizardDoggo").isInstance(entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Satisforestry's LizardDoggo API is unavailable", e);
        }
    }

    public static Optional<SpawnPoint> nearestSpawn(ServerPlayer player) {
        if (!ModList.get().isLoaded("satisforestry")) return Optional.empty();
        try {
            Class<?> api = Class.forName("reika.satisforestry.api.SFAPI");
            Object lookups = api.getField("genericLookups").get(null);
            Class<?> doggo = (Class<?>)lookups.getClass().getMethod("getDoggoClass").invoke(lookups);
            Object spawning = api.getField("spawningHandler").get(null);
            var nearest = java.util.Arrays.stream(spawning.getClass().getMethods())
                    .filter(m -> m.getName().equals("getNearestSpawnPointOfType") && m.getParameterCount() == 3
                            && m.getParameterTypes()[0].isInstance(player)).findFirst().orElseThrow();
            Object point = nearest.invoke(spawning, player, 512, doggo);
            if (point == null) return Optional.empty();
            Class<?> type = point.getClass();
            BlockPos pos = new BlockPos(((Number)type.getMethod("getX").invoke(point)).intValue(),
                    ((Number)type.getMethod("getY").invoke(point)).intValue(),
                    ((Number)type.getMethod("getZ").invoke(point)).intValue());
            // A native EntityType lets the client reconstruct the actual doggo dimensions, just
            // as V33a created an unspawned doggo at the point before highlighting its bounding box.
            for (var entityType : BuiltInRegistries.ENTITY_TYPE) {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                if (!id.getNamespace().equals("satisforestry")) continue;
                Entity probe = entityType.create(player.level(), EntitySpawnReason.LOAD);
                if (probe != null && doggo.isInstance(probe)) return Optional.of(new SpawnPoint(pos, id));
            }
            throw new IllegalStateException("Satisforestry did not register its doggo EntityType");
        } catch (ReflectiveOperationException | java.util.NoSuchElementException e) {
            throw new IllegalStateException("Satisforestry's spawn-point API is unavailable", e);
        }
    }
}
