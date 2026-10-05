package reika.chromaticraft.data;

import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** The frame graph provides the warped target; this chain writes its result into the main target. */
public final class ChromaVoidVisualData implements DataProvider {
    private final Path output;
    public ChromaVoidVisualData(PackOutput pack) {
        output = pack.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("chromaticraft/post_effect/void_ritual.json");
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        return DataProvider.saveStable(cache, JsonParser.parseString("""
            {"targets":{},"passes":[{
              "vertex_shader":"minecraft:core/screenquad","fragment_shader":"minecraft:post/blit",
              "inputs":[{"sampler_name":"In","target":"chromaticraft:void_ritual_warped"}],
              "uniforms":{"BlitConfig":[{"name":"ColorModulate","type":"vec4","value":[1.0,1.0,1.0,1.0]}]},
              "output":"minecraft:main"
            }]}
            """), output);
    }
    @Override public String getName() { return "ChromatiCraft void ritual post chain"; }
}
