package reika.chromaticraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the small stone-floored arena the ChromatiCraft in-world game tests run on (the
 * progression tests need no blocks — just a valid test structure to define the arena). Emitted here
 * so {@code gradlew runServerData} keeps it in sync rather than hand-authoring binary NBT. Mirrors
 * RotaryCraft's {@code RoCTestStructureProvider}. See {@link reika.chromaticraft.ChromaGameTests}.
 */
public class ChromaTestStructureProvider implements DataProvider {

	public static final Identifier ARENA = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "test_arena");
	public static final Identifier WAND_ARENA = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "wand_test_arena");
	private static final int SIZE_XZ = 5;
	private static final int SIZE_Y = 4;

	private final PackOutput output;

	public ChromaTestStructureProvider(PackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return CompletableFuture.allOf(writeArena(cache, ARENA, SIZE_XZ, SIZE_Y), writeArena(cache, WAND_ARENA, 16, 16));
	}

	private CompletableFuture<?> writeArena(CachedOutput cache, Identifier id, int width, int height) {
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());

		ListTag size = new ListTag();
		size.add(IntTag.valueOf(width));
		size.add(IntTag.valueOf(height));
		size.add(IntTag.valueOf(width));
		root.put("size", size);

		root.put("entities", new ListTag());

		ListTag palette = new ListTag();
		CompoundTag stone = new CompoundTag();
		stone.putString("id", "minecraft:stone"); // 26.3 block state NBT key (was "Name")
		palette.add(stone);
		root.put("palette", palette);

		ListTag blocks = new ListTag();
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < width; z++) {
				CompoundTag b = new CompoundTag();
				ListTag pos = new ListTag();
				pos.add(IntTag.valueOf(x));
				pos.add(IntTag.valueOf(0));
				pos.add(IntTag.valueOf(z));
				b.put("pos", pos);
				b.putInt("state", 0);
				blocks.add(b);
			}
		}
		root.put("blocks", blocks);

		Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK)
				.resolve(id.getNamespace()).resolve("structure").resolve(id.getPath() + ".nbt");

		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			HashingOutputStream hashing = new HashingOutputStream(Hashing.sha1(), bytes);
			NbtIo.writeCompressed(root, hashing);
			cache.writeIfNeeded(path, bytes.toByteArray(), hashing.hash());
		}
		catch (IOException e) {
			return CompletableFuture.failedFuture(e);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public String getName() {
		return "ChromatiCraft Test Arena Structure";
	}
}
