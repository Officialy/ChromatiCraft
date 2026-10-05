package reika.chromaticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import reika.chromaticraft.auxiliary.render.ChromaText;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Registers the unmodified V33a 16x16 glyph sheet with the 26.3 bitmap font provider. */
public final class ChromaFontProvider implements DataProvider {
    private final Path path;
    public ChromaFontProvider(PackOutput output) {
        path = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve("chromaticraft/font/obfuscated.json");
    }
    @Override public CompletableFuture<?> run(CachedOutput output) {
        return DataProvider.saveStable(output, definition(), path);
    }
    public static JsonObject definition() {
        JsonObject bitmap = new JsonObject();
        bitmap.addProperty("type", "bitmap");
        bitmap.addProperty("file", "chromaticraft:font/obf_font.png");
        bitmap.addProperty("height", 8);
        bitmap.addProperty("ascent", 7);
        JsonArray chars = new JsonArray();
        for (int row = 0; row < 16; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < 16; col++) line.append((char)(ChromaText.GLYPH_BASE + row * 16 + col));
            chars.add(line.toString());
        }
        bitmap.add("chars", chars);
        JsonObject space = new JsonObject();
        space.addProperty("type", "space");
        JsonObject advances = new JsonObject();
        advances.addProperty(" ", 4);
        space.add("advances", advances);
        JsonArray providers = new JsonArray();
        providers.add(space);
        providers.add(bitmap);
        JsonObject root = new JsonObject();
        root.add("providers", providers);
        return root;
    }
    @Override public String getName() { return "ChromatiCraft progression glyph font"; }
}
