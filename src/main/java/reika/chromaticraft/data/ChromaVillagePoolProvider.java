package reika.chromaticraft.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Appends the two V33a failed-casting buildings to every ordinary vanilla village house pool.
 *
 * <p>NeoForge 26.2 exposes modifiers for biomes and structures, but not jigsaw template pools. The
 * only data-driven mechanism is therefore an overlay at the original pool id. Datagen reads the
 * exact vanilla 26.2 JSON and adds two elements without reproducing or hand-maintaining any vanilla
 * entry. Existing ChromatiCraft entries are removed first so repeated datagen is idempotent.
 */
public final class ChromaVillagePoolProvider implements DataProvider {

	private final PackOutput output;

	public ChromaVillagePoolProvider(PackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return CompletableFuture.allOf(ChromaStructureTemplateProvider.VILLAGE_STYLES.stream()
				.map(style -> writePool(cache, style)).toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<?> writePool(CachedOutput cache, String style) {
		String resource = "data/minecraft/worldgen/template_pool/village/" + style + "/houses.json";
		JsonObject pool;
		try (InputStream stream = openMinecraftResource(resource)) {
			pool = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read vanilla 26.2 village pool " + resource, e);
		}
		JsonArray elements = pool.getAsJsonArray("elements");
		for (int i = elements.size() - 1; i >= 0; i--) {
			JsonObject wrapper = elements.get(i).getAsJsonObject();
			JsonObject element = wrapper.getAsJsonObject("element");
			if (element != null && element.has("location")
					&& element.get("location").getAsString().startsWith("chromaticraft:worldgen/village/"))
				elements.remove(i);
		}
		elements.add(element(style, true, 4));
		elements.add(element(style, false, 1));
		Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK)
				.resolve("minecraft/worldgen/template_pool/village/" + style + "/houses.json");
		return DataProvider.saveStable(cache, pool, path);
	}

	/**
	 * The dev transforming class loader intentionally masks Minecraft data resources from a mod
	 * class' resource lookup. Resolve the archive/directory which actually supplied Minecraft's
	 * {@link net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool} class and
	 * read the pool there. This also works from an exploded game classes directory.
	 */
	private static InputStream openMinecraftResource(String resource) throws IOException {
		InputStream direct = net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.class
				.getResourceAsStream("/" + resource);
		if (direct != null)
			return direct;
		final Path source;
		try {
			source = Path.of(net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.class
					.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (URISyntaxException | NullPointerException e) {
			throw new IOException("Could not resolve the Minecraft code source for " + resource, e);
		}
		if (Files.isDirectory(source)) {
			Path file = source.resolve(resource);
			if (Files.isRegularFile(file))
				return Files.newInputStream(file);
		}
		else if (Files.isRegularFile(source)) {
			ZipFile zip = new ZipFile(source.toFile());
			ZipEntry entry = zip.getEntry(resource);
			if (entry != null) {
				InputStream stream = zip.getInputStream(entry);
				return new java.io.FilterInputStream(stream) {
					@Override
					public void close() throws IOException {
						try { super.close(); }
						finally { zip.close(); }
					}
				};
			}
			zip.close();
		}
		throw new IOException("Missing vanilla 26.2 village pool " + resource + " in " + source);
	}

	private static JsonObject element(String style, boolean wooden, int weight) {
		JsonObject value = new JsonObject();
		value.addProperty("element_type", "minecraft:single_pool_element");
		value.addProperty("location", ChromaStructureTemplateProvider.villageTemplate(style, wooden).toString());
		JsonObject processors = new JsonObject();
		processors.add("processors", new JsonArray());
		value.add("processors", processors);
		value.addProperty("projection", "rigid");
		JsonObject wrapper = new JsonObject();
		wrapper.add("element", value);
		wrapper.addProperty("weight", weight);
		return wrapper;
	}

	@Override
	public String getName() {
		return "ChromatiCraft V33a Village Jigsaw Pool Additions";
	}
}
