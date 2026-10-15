package reika.chromaticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaSounds;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Generates the client event-to-OGG map for every registered ChromatiCraft sound. */
public final class ChromaSoundProvider implements DataProvider {

    private final Path output;

    public ChromaSoundProvider(PackOutput packOutput) {
        output = packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(ChromatiCraft.MODID).resolve("sounds.json");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject root = new JsonObject();
        for (ChromaSounds sound : ChromaSounds.values()) {
            JsonObject definition = new JsonObject();
            JsonArray sounds = new JsonArray();
            JsonObject file = new JsonObject();
            file.addProperty("name", ChromatiCraft.MODID + ":" + sound.getSoundFile());
            file.addProperty("type", "file");
            if (sound.preload())
                file.addProperty("preload", true);
            if (sound.isStreamed())
                file.addProperty("stream", true);
            float distance = sound.getAudibleDistance();
            if (distance > 0)
                file.addProperty("attenuation_distance", distance);
            sounds.add(file);
            definition.add("sounds", sounds);
            root.add(sound.getEventName(), definition);
        }
        // V33a ChromaSoundVariant: the two-octave LO/HI recordings a wide-pitch sound switches to.
        for (ChromaSounds.Variant variant : ChromaSounds.variants()) {
            JsonObject definition = new JsonObject();
            JsonArray sounds = new JsonArray();
            JsonObject file = new JsonObject();
            file.addProperty("name", ChromatiCraft.MODID + ":" + variant.getSoundFile());
            file.addProperty("type", "file");
            sounds.add(file);
            definition.add("sounds", sounds);
            root.add(variant.getEventName(), definition);
        }
        return DataProvider.saveStable(cache, root, output);
    }

    @Override
    public String getName() {
        return "ChromatiCraft Sound Definitions";
    }
}
