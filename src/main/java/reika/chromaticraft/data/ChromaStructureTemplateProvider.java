package reika.chromaticraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.block.BlockCrystallineStone;

/** Generates the canonical NBT templates for every active ChromatiCraft multiblock. */
public final class ChromaStructureTemplateProvider implements DataProvider {

    public static final Identifier PYLON = id("multiblock/pylon");
    public static final Identifier CASTING_L1 = id("multiblock/casting_l1");
    public static final Identifier CASTING_L2 = id("multiblock/casting_l2");
    public static final Identifier CASTING_L3 = id("multiblock/casting_l3");
    public static final Identifier REPEATER = id("multiblock/repeater");
    public static final Identifier COMPOUND_REPEATER = id("multiblock/compound_repeater");
    public static final Identifier PYLON_BROADCAST = id("multiblock/pylon_broadcast");

    /** V33a {@code setEmpty(false, false)}: the cell must be air. */
    private static final StateDef AIR = new StateDef("minecraft:air", Map.of());
    /** V33a {@code setEmpty(true, true)}: air or a soft/non-collidable block. Placed as air. */
    private static final StateDef SOFT_AIR = new StateDef("minecraft:cave_air", Map.of());
    private static final StateDef STRUCTURE_VOID = new StateDef("minecraft:structure_void", Map.of());
    private static final StateDef SMOOTH = stone(StoneTypes.SMOOTH);
    private static final StateDef RUNE_PLACEHOLDER = new StateDef("chromaticraft:crystal_rune_black", Map.of());

    private final PackOutput output;

    public ChromaStructureTemplateProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(
                write(cache, PYLON, pylon()),
                write(cache, CASTING_L1, castingL1()),
                write(cache, CASTING_L2, castingL2()),
                write(cache, CASTING_L3, castingL3()),
                write(cache, REPEATER, repeater()),
                write(cache, COMPOUND_REPEATER, compoundRepeater()),
                write(cache, PYLON_BROADCAST, pylonBroadcast()));
    }

    private CompletableFuture<?> write(CachedOutput cache, Identifier id, TemplateData template) {
        CompoundTag root = template.toNBT();
        Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(id.getNamespace()).resolve("structure").resolve(id.getPath() + ".nbt");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            HashingOutputStream hashing = new HashingOutputStream(Hashing.sha1(), bytes);
            NbtIo.writeCompressed(root, hashing);
            cache.writeIfNeeded(path, bytes.toByteArray(), hashing.hash());
            return CompletableFuture.completedFuture(null);
        }
        catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static TemplateData castingL1() {
        TemplateData data = new TemplateData(13, 7, 13);
        int c = 6;
        for (int i = -6; i <= 6; i++) {
            for (int y = 0; y < 6; y++) {
                data.set(0, y, c + i, SOFT_AIR); data.set(12, y, c + i, SOFT_AIR);
                data.set(c + i, y, 0, SOFT_AIR); data.set(c + i, y, 12, SOFT_AIR);
            }
        }
        data.set(c, 2, c, AIR);
        for (int[] d : CARDINALS) data.set(c + d[0], 1, c + d[1], AIR);

        for (int[] d : CARDINALS) {
            for (int distance = 3; distance <= 5; distance++)
                data.set(c + d[0] * distance, 0, c + d[1] * distance, SMOOTH);
            for (int y = 1; y <= 5; y++)
                data.set(c + d[0] * 6, y, c + d[1] * 6,
                        stone(y == 1 ? StoneTypes.EMBOSSED : StoneTypes.COLUMN));
        }
        for (int i = -6; i <= 6; i++) {
            data.set(0, 0, c + i, SMOOTH); data.set(12, 0, c + i, SMOOTH);
            data.set(c + i, 0, 0, SMOOTH); data.set(c + i, 0, 12, SMOOTH);
        }
        for (int y = 1; y <= 4; y++) {
            StateDef state = stone(y == 1 ? StoneTypes.SMOOTH : StoneTypes.COLUMN);
            for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1})
                data.set(c + sx * 6, y, c + sz * 6, state);
        }
        for (int y = 1; y <= 6; y++) {
            StateDef state = stone(y == 1 || y == 5 ? StoneTypes.SMOOTH
                    : y == 6 ? StoneTypes.ENGRAVED : StoneTypes.COLUMN);
            for (int[] p : CASTING_POSTS)
                data.set(c + p[0], y, c + p[1], state);
        }
        for (int i = -5; i <= 5; i++) {
            if (i != -3 && i != 0 && i != 3) {
                int y = Math.abs(i) < 3 ? 6 : 5;
                data.set(0, y, c + i, stone(StoneTypes.BEAM)); data.set(12, y, c + i, stone(StoneTypes.BEAM));
                data.set(c + i, y, 0, stone(StoneTypes.BEAM)); data.set(c + i, y, 12, stone(StoneTypes.BEAM));
            }
        }
        for (int i = -3; i <= 3; i++) {
            for (int y = 0; y <= 1; y++) {
                if (y == 0 || Math.abs(i) % 2 == 1) {
                    data.set(c - 3, y, c + i, SMOOTH); data.set(c + 3, y, c + i, SMOOTH);
                    data.set(c + i, y, c - 3, SMOOTH); data.set(c + i, y, c + 3, SMOOTH);
                }
            }
        }
        for (int[] d : CARDINALS) {
            data.set(c + d[0], 0, c + d[1], SMOOTH);
			data.set(c + d[0], 1, c + d[1], AIR);
        }
        for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1})
            data.set(c + sx * 6, 5, c + sz * 6, new StateDef("minecraft:coal_block", Map.of()));
        data.set(c, 6, 0, new StateDef("minecraft:lapis_block", Map.of()));
        data.set(c, 6, 12, new StateDef("minecraft:lapis_block", Map.of()));
        data.set(0, 6, c, new StateDef("minecraft:lapis_block", Map.of()));
        data.set(12, 6, c, new StateDef("minecraft:lapis_block", Map.of()));
        data.remove(c, 0, c);
        return data;
    }

    private static TemplateData castingL2() {
        TemplateData data = castingL1();
        int c = 6;
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++)
            data.set(c + x, 0, c + z, SMOOTH);
        // The personal tuning-key cells are deliberately NOT written here. V33a CastingL2Structure
        // uses addBlock for them, so the eight outer ones stay smooth-or-rune and the four inner ones
        // are dropped again by its own remove loop. Runtime supplies those alternatives.
        for (int i = -5; i <= 5; i++) if (i != 0 && Math.abs(i) != 3) {
            StateDef quartz = new StateDef("minecraft:quartz_block", Map.of());
            data.set(0, 0, c + i, quartz); data.set(12, 0, c + i, quartz);
            data.set(c + i, 0, 0, quartz); data.set(c + i, 0, 12, quartz);
        }
        for (int i = -3; i <= 3; i++) {
            data.remove(c - 3, 1, c + i); data.remove(c + 3, 1, c + i);
            data.remove(c + i, 1, c - 3); data.remove(c + i, 1, c + 3);
        }
        for (int i = -2; i <= 2; i++) {
            data.remove(c - 2, 0, c + i); data.remove(c + 2, 0, c + i);
            data.remove(c + i, 0, c - 2); data.remove(c + i, 0, c + 2);
        }
        for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1})
            data.set(c + sx * 6, 5, c + sz * 6, new StateDef("minecraft:redstone_block", Map.of()));
        data.set(c, 6, 0, new StateDef("minecraft:gold_block", Map.of()));
        data.set(c, 6, 12, new StateDef("minecraft:gold_block", Map.of()));
        data.set(0, 6, c, new StateDef("minecraft:gold_block", Map.of()));
        data.set(12, 6, c, new StateDef("minecraft:gold_block", Map.of()));
        data.remove(c, 0, c);
        return data;
    }

    private static TemplateData castingL3() {
        TemplateData data = new TemplateData(17, 7, 17);
        data.copyFrom(castingL2(), 2, 0, 2);
        int c = 8;
        for (int i = -7; i <= 7; i++) {
            data.set(c - 7, 0, c + i, stone(StoneTypes.SMOOTH)); data.set(c + 7, 0, c + i, stone(StoneTypes.SMOOTH));
            data.set(c + i, 0, c - 7, stone(StoneTypes.SMOOTH)); data.set(c + i, 0, c + 7, stone(StoneTypes.SMOOTH));
        }
        StateDef obsidian = new StateDef("minecraft:obsidian", Map.of());
        for (int i = -8; i <= 8; i++) {
            data.set(0, 0, c + i, obsidian); data.set(16, 0, c + i, obsidian);
            data.set(c + i, 0, 0, obsidian); data.set(c + i, 0, 16, obsidian);
        }
        for (int y = 1; y <= 4; y++) {
            StateDef state = y == 3 ? RUNE_PLACEHOLDER : y == 4
                    ? new StateDef("chromaticraft:crystal_repeater", Map.of())
                    : stone(StoneTypes.SMOOTH);
            for (int[] p : CASTING_OUTER_POSTS) data.set(c + p[0], y, c + p[1], state);
        }
        for (int y = 1; y <= 3; y++) {
            StateDef state = stone(y == 1 ? StoneTypes.SMOOTH : y == 2 ? StoneTypes.COLUMN : StoneTypes.ENGRAVED);
            for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1})
                data.set(c + sx * 8, y, c + sz * 8, state);
        }
        for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1})
            data.set(c + sx * 6, 5, c + sz * 6, new StateDef("minecraft:glowstone", Map.of()));
        data.set(c, 6, c - 6, new StateDef("minecraft:diamond_block", Map.of()));
        data.set(c, 6, c + 6, new StateDef("minecraft:diamond_block", Map.of()));
        data.set(c - 6, 6, c, new StateDef("minecraft:diamond_block", Map.of()));
        data.set(c + 6, 6, c, new StateDef("minecraft:diamond_block", Map.of()));
        data.remove(c, 0, c);
        return data;
    }

    private static final int[][] CARDINALS = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] CASTING_POSTS = {
            {-6,-3},{-6,3},{6,-3},{6,3},{-3,-6},{3,-6},{-3,6},{3,6}
    };
    private static final int[][] CASTING_TUNING_RUNES = {
            {-3,-3},{3,-3},{-3,3},{3,3},{-6,-3},{-3,-6},{3,-6},{6,-3},
            {-6,3},{-3,6},{3,6},{6,3}
    };
    private static final int[][] CASTING_OUTER_POSTS = {
            {-2,-8},{-6,-8},{2,-8},{6,-8},{-2,8},{-6,8},{2,8},{6,8},
            {-8,-2},{-8,-6},{-8,2},{-8,6},{8,-2},{8,-6},{8,2},{8,6}
    };

    private static TemplateData pylon() {
        TemplateData data = new TemplateData(7, 10, 7);
        for (int layer = 0; layer <= 9; layer++) {
            for (int axis = 0; axis < 4; axis++) {
                int stepX = axis == 0 ? 1 : axis == 1 ? -1 : 0;
                int stepZ = axis == 2 ? 1 : axis == 3 ? -1 : 0;
                for (int distance = 0; distance <= 3; distance++) {
                    int x = 3 + stepX * distance;
                    int z = 3 + stepZ * distance;
                    StateDef state = layer == 0 ? SMOOTH : AIR;
                    data.set(x, layer, z, state);
                    if (stepX == 0) {
                        data.set(x + stepZ, layer, z, state);
                        data.set(x - stepZ, layer, z, state);
                    }
                    else {
                        data.set(x, layer, z + stepX, state);
                        data.set(x, layer, z - stepX, state);
                    }
                }
            }
        }

        for (int layer = 1; layer <= 5; layer++) {
            StateDef state = layer == 5 ? RUNE_PLACEHOLDER
                    : stone(layer == 2 || layer == 3 ? StoneTypes.COLUMN
                            : layer == 4 ? StoneTypes.ENGRAVED : StoneTypes.EMBOSSED);
            setRingEight(data, layer, state);
        }
        for (int layer = 1; layer <= 7; layer++) {
            StateDef state = stone(layer == 5 ? StoneTypes.GLOWCOL
                    : layer == 7 ? StoneTypes.FOCUS : StoneTypes.COLUMN);
            for (int x : new int[] {2, 4})
                for (int z : new int[] {2, 4})
                    data.set(x, layer, z, state);
        }

        StateDef glowBeam = stone(StoneTypes.GLOWBEAM);
        data.set(0, 4, 3, glowBeam); data.set(6, 4, 3, glowBeam);
        data.set(3, 4, 0, glowBeam); data.set(3, 4, 6, glowBeam);
        StateDef beam = stone(StoneTypes.BEAM);
        for (int[] point : new int[][] {{1,4},{1,2},{5,4},{5,2},{2,5},{2,1},{4,5},{4,1}})
            data.set(point[0], 3, point[1], beam);

        data.remove(3, 9, 3); // pylon tile anchor
        for (int[] point : POWER_CRYSTAL_POINTS)
            data.remove(point[0], 6, point[1]); // optional booster cells

        data.set(3, 0, 3, stone(StoneTypes.STABILIZER));
        for (int distance = 1; distance <= 2; distance++) {
            StateDef resonance = stone(StoneTypes.RESORING);
            data.set(3 + distance, 0, 3, resonance); data.set(3 - distance, 0, 3, resonance);
            data.set(3, 0, 3 + distance, resonance); data.set(3, 0, 3 - distance, resonance);
        }
        return data;
    }

    private static TemplateData pylonBroadcast() {
        TemplateData data = new TemplateData(11, 11, 11);
        data.copyFrom(pylon(), 2, 1, 2);
        int baseY = 1;

        for (int offset = -3; offset <= 3; offset++) {
            StateDef state = stone(Math.abs(offset) == 3 || offset == 0 ? StoneTypes.EMBOSSED : StoneTypes.BRICKS);
            data.set(5 + offset, baseY, 10, state); data.set(5 + offset, baseY, 0, state);
            data.set(10, baseY, 5 + offset, state); data.set(0, baseY, 5 + offset, state);
        }

        for (int offset = -2; offset <= 2; offset++) {
            markChroma(data, 5 + offset, baseY, 9); markChroma(data, 5 + offset, baseY, 1);
            markChroma(data, 9, baseY, 5 + offset); markChroma(data, 1, baseY, 5 + offset);
            data.set(5 + offset, baseY - 1, 9, SMOOTH); data.set(5 + offset, baseY - 1, 1, SMOOTH);
            data.set(9, baseY - 1, 5 + offset, SMOOTH); data.set(1, baseY - 1, 5 + offset, SMOOTH);
        }

        for (int offset = 3; offset <= 4; offset++) {
            StateDef bricks = stone(StoneTypes.BRICKS);
            for (int xSign : new int[] {-1, 1}) for (int zSign : new int[] {-1, 1}) {
                data.set(5 + xSign * offset, baseY, 5 + zSign * 3, bricks);
                data.set(5 + xSign * 3, baseY, 5 + zSign * offset, bricks);
            }
        }

        for (int offset = 2; offset <= 3; offset++) {
            for (int xSign : new int[] {-1, 1}) for (int zSign : new int[] {-1, 1}) {
                markChroma(data, 5 + xSign * offset, baseY, 5 + zSign * 2);
                markChroma(data, 5 + xSign * 2, baseY, 5 + zSign * offset);
                data.set(5 + xSign * offset, baseY - 1, 5 + zSign * 2, SMOOTH);
                data.set(5 + xSign * 2, baseY - 1, 5 + zSign * offset, SMOOTH);
            }
        }

        for (int height = 1; height <= 4; height++) {
            StateDef state = stone(height == 4 ? StoneTypes.MULTICHROMIC : StoneTypes.COLUMN);
            for (int xSign : new int[] {-1, 1}) for (int zSign : new int[] {-1, 1}) {
                data.set(5 + xSign * 3, baseY + height, 5 + zSign * 5, state);
                data.set(5 + xSign * 5, baseY + height, 5 + zSign * 3, state);
            }
        }

        for (int height = 1; height <= 6; height++) {
            StateDef state = stone(height == 3 ? StoneTypes.GLOWCOL
                    : height == 6 ? StoneTypes.FOCUS : StoneTypes.COLUMN);
            data.set(10, baseY + height, 5, state); data.set(0, baseY + height, 5, state);
            data.set(5, baseY + height, 10, state); data.set(5, baseY + height, 0, state);
        }
        return data;
    }

    private static void markChroma(TemplateData data, int x, int y, int z) {
        // The canonical template records these dynamic fluid cells explicitly. Runtime substitutes
        // a registry-id check until the modern chroma-fluid vertical registers chromaticraft:chroma.
        data.set(x, y, z, STRUCTURE_VOID);
    }
    private static TemplateData repeater() {
        TemplateData data = new TemplateData(1, 4, 1);
        data.set(0, 3, 0, new StateDef("chromaticraft:crystal_repeater", Map.of()));
        data.set(0, 2, 0, RUNE_PLACEHOLDER);
        data.set(0, 1, 0, SMOOTH);
        data.set(0, 0, 0, SMOOTH);
        return data;
    }

    private static TemplateData compoundRepeater() {
        TemplateData data = new TemplateData(1, 6, 1);
        data.set(0, 5, 0, new StateDef("chromaticraft:compound_repeater", Map.of()));
        data.set(0, 4, 0, stone(StoneTypes.BRICKS));
        data.set(0, 3, 0, stone(StoneTypes.COLUMN));
        data.set(0, 2, 0, stone(StoneTypes.MULTICHROMIC));
        data.set(0, 1, 0, stone(StoneTypes.COLUMN));
        data.set(0, 0, 0, stone(StoneTypes.BRICKS));
        return data;
    }

    private static void setRingEight(TemplateData data, int y, StateDef state) {
        for (int[] point : new int[][] {{0,4},{0,2},{6,4},{6,2},{2,6},{2,0},{4,6},{4,0}})
            data.set(point[0], y, point[1], state);
    }

    /**
     * Each crystalline-stone variant is its own block, so the template records a block id rather
     * than one id plus a {@code type} property. Beams additionally carry HORIZONTAL_AXIS; templates
     * pin the X orientation, which is what the V33a layouts assume.
     */
    private static StateDef stone(StoneTypes type) {
        String name = "chromaticraft:" + reika.chromaticraft.registry.ChromaBlocks.crystallineStoneName(type);
        return type.isBeam() ? new StateDef(name, Map.of("axis", "x")) : new StateDef(name, Map.of());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }

    private static final int[][] POWER_CRYSTAL_POINTS = {
            {0,2}, {2,0}, {6,2}, {4,0}, {0,4}, {2,6}, {6,4}, {4,6}
    };

    private record StateDef(String name, Map<String, String> properties) {
        CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", name);
            if (!properties.isEmpty()) {
                CompoundTag props = new CompoundTag();
                properties.forEach(props::putString);
                tag.put("Properties", props);
            }
            return tag;
        }
    }

    private static final class TemplateData {
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final LinkedHashMap<BlockPos, StateDef> blocks = new LinkedHashMap<>();

        private TemplateData(int x, int y, int z) {
            sizeX = x;
            sizeY = y;
            sizeZ = z;
        }

        void set(int x, int y, int z, StateDef state) {
            blocks.put(new BlockPos(x, y, z), state);
        }

        void copyFrom(TemplateData source, int offsetX, int offsetY, int offsetZ) {
            source.blocks.forEach((pos, state) ->
                    this.set(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ, state));
        }
        void remove(int x, int y, int z) {
            blocks.remove(new BlockPos(x, y, z));
        }

        CompoundTag toNBT() {
            CompoundTag root = new CompoundTag();
            root.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
            root.put("size", ints(sizeX, sizeY, sizeZ));
            root.put("entities", new ListTag());

            LinkedHashMap<StateDef, Integer> paletteIds = new LinkedHashMap<>();
            for (StateDef state : blocks.values())
                paletteIds.computeIfAbsent(state, ignored -> paletteIds.size());
            ListTag palette = new ListTag();
            paletteIds.keySet().forEach(state -> palette.add(state.toNBT()));
            root.put("palette", palette);

            ListTag blockList = new ListTag();
            blocks.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey((a, b) -> {
                        int y = Integer.compare(a.getY(), b.getY());
                        int x = Integer.compare(a.getX(), b.getX());
                        return y != 0 ? y : x != 0 ? x : Integer.compare(a.getZ(), b.getZ());
                    }))
                    .forEach(entry -> {
                        CompoundTag block = new CompoundTag();
                        BlockPos pos = entry.getKey();
                        block.put("pos", ints(pos.getX(), pos.getY(), pos.getZ()));
                        block.putInt("state", paletteIds.get(entry.getValue()));
                        blockList.add(block);
                    });
            root.put("blocks", blockList);
            return root;
        }
    }

    private static ListTag ints(int... values) {
        ListTag list = new ListTag();
        for (int value : values)
            list.add(IntTag.valueOf(value));
        return list;
    }

    @Override
    public String getName() {
        return "ChromatiCraft Canonical Structure Templates";
    }
}
