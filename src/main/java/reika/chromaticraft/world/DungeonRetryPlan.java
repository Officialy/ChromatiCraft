package reika.chromaticraft.world;

import net.minecraft.world.level.ChunkPos;
import reika.chromaticraft.world.OverworldStructureFeature.Type;

import java.util.ArrayList;
import java.util.List;

/** V33a tryGenerateInChunksAround, including its repeated visits to inner squares. */
public final class DungeonRetryPlan {
    private DungeonRetryPlan() {}

    public static List<ChunkPos> chunks(Type type, ChunkPos origin) {
        List<ChunkPos> chunks = new ArrayList<>();
        chunks.add(origin);
        for (int radius = 1; radius <= DungeonZones.keepTryingRange(type) / 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x != 0 || z != 0)
                        chunks.add(new ChunkPos(origin.x() + x, origin.z() + z));
                }
            }
        }
        return List.copyOf(chunks);
    }
}
