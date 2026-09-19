package reika.chromaticraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.block.BlockCrystallineStone;

/** Generates the canonical NBT templates for every active ChromatiCraft multiblock. */
public final class ChromaStructureTemplateProvider implements DataProvider {

    public static final Identifier PYLON = id("multiblock/pylon");
    public static final Identifier CASTING_L1 = id("multiblock/casting_l1");
    public static final Identifier CASTING_L2 = id("multiblock/casting_l2");
    public static final Identifier CASTING_L3 = id("multiblock/casting_l3");
    public static final Identifier REPEATER = id("multiblock/repeater");
    public static final Identifier WEAK_REPEATER = id("multiblock/weak_repeater");
    public static final Identifier RELAY_SOURCE = id("multiblock/relay_source");
    public static final Identifier COMPOUND_REPEATER = id("multiblock/compound_repeater");
    public static final Identifier PYLON_BROADCAST = id("multiblock/pylon_broadcast");
    public static final Identifier INFUSION = id("multiblock/infusion");
    public static final Identifier PLAYER_INFUSION = id("multiblock/player_infusion");
    public static final Identifier RITUAL_BASE = id("multiblock/ritual_base");
    public static final Identifier RITUAL_ENHANCED = id("multiblock/ritual_enhanced");
    public static final Identifier PERSONAL_CHARGER = id("multiblock/personal_charger");
    public static final Identifier PORTAL = id("multiblock/portal");
    public static final Identifier DATANODE = id("worldgen/data_node");
    public static final Identifier RAINBOW_TREE = id("worldgen/rainbow_tree");
    public static final Identifier NETHER_TEMPLE = id("worldgen/nether/temple");
    public static final Identifier NETHER_HUT = id("worldgen/nether/hut");
    public static final Identifier NETHER_MAZE = id("worldgen/nether/maze");
    public static final Identifier NETHER_SPIRAL = id("worldgen/nether/spiral");
    public static final Identifier NETHER_DIORAMA = id("worldgen/nether/diorama");
    public static final Identifier OVERWORLD_CAVERN = id("worldgen/overworld/cavern");
    public static final Identifier OVERWORLD_BURROW = id("worldgen/overworld/burrow");
    public static final Identifier OVERWORLD_BURROW_FURNACE = id("worldgen/overworld/burrow_furnace");
    public static final Identifier OVERWORLD_BURROW_LOOT = id("worldgen/overworld/burrow_loot");
    public static final Identifier OVERWORLD_OCEAN = id("worldgen/overworld/ocean");
    public static final Identifier OVERWORLD_OCEAN_PIT_SLICE = id("worldgen/overworld/ocean_pit_slice");
    public static final Identifier OVERWORLD_DESERT = id("worldgen/overworld/desert");
    public static final Identifier OVERWORLD_SNOW = id("worldgen/overworld/snow");
    public static final Identifier OVERWORLD_BIOME_FRAGMENT = id("worldgen/overworld/biome_fragment");
    public static final Identifier PROXIMA_MONUMENT = id("worldgen/proxima/monument");
    public static final Identifier PROXIMA_LIGHT_PANEL_STAIR_BOTTOM = id("worldgen/proxima/light_panel/stair_bottom");
    public static final Identifier PROXIMA_LIGHT_PANEL_STAIR_SECTION = id("worldgen/proxima/light_panel/stair_section");
    public static final Identifier PROXIMA_LIGHT_PANEL_STAIR_TOP = id("worldgen/proxima/light_panel/stair_top");
    public static final Identifier PROXIMA_LIGHT_PANEL_LOOT = id("worldgen/proxima/light_panel/loot");
    public static final Identifier PROXIMA_TD_MAZE_CELL = id("worldgen/proxima/three_d_maze/cell");
    public static final Identifier PROXIMA_TD_MAZE_SHAFT = id("worldgen/proxima/three_d_maze/shaft_slice");
    public static final Identifier PROXIMA_TD_MAZE_ENTRANCE = id("worldgen/proxima/three_d_maze/entrance");
    public static final Identifier PROXIMA_TD_MAZE_LOOT = id("worldgen/proxima/three_d_maze/loot");
    public static final Identifier PROXIMA_MUSIC_FUNNEL = id("worldgen/proxima/music/funnel");
    public static final Identifier PROXIMA_MUSIC_ROOM = id("worldgen/proxima/music/room");
    public static final Identifier PROXIMA_MUSIC_LOOT = id("worldgen/proxima/music/loot");
    public static final Identifier PROXIMA_GOL_ENTRANCE_PREFAB = id("worldgen/proxima/gol/entrance_prefab");
    public static final Identifier PROXIMA_GOL_SURFACE = id("worldgen/proxima/gol/surface");
    public static final Identifier PROXIMA_GOL_ENTRANCE_DOOR = id("worldgen/proxima/gol/entrance_door");
    public static final Identifier PROXIMA_GOL_EXIT_DOOR = id("worldgen/proxima/gol/exit_door");
    public static final Identifier PROXIMA_GOL_LOOT = id("worldgen/proxima/gol/loot");
    public static final List<String> VILLAGE_STYLES = List.of("plains", "desert", "savanna", "snowy", "taiga");

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
                write(cache, WEAK_REPEATER, weakRepeater()),
                write(cache, RELAY_SOURCE, relaySource()),
                write(cache, COMPOUND_REPEATER, compoundRepeater()),
                write(cache, PYLON_BROADCAST, pylonBroadcast()),
                write(cache, INFUSION, infusion()),
                write(cache, PLAYER_INFUSION, playerInfusion()),
                write(cache, RITUAL_BASE, ritualAltar(false)),
                write(cache, RITUAL_ENHANCED, ritualAltar(true)),
                write(cache, PERSONAL_CHARGER, personalCharger()),
                write(cache, PORTAL, importPortal()),
                write(cache, DATANODE, dataNode()),
                write(cache, RAINBOW_TREE, rainbowTree()),
                write(cache, NETHER_HUT, netherHut()),
                write(cache, NETHER_TEMPLE, importExplicitNetherStructure("NetherTemple.java", 26, 11, 26)),
                write(cache, NETHER_MAZE, importExplicitNetherStructure("NetherMaze.java", 16, 5, 16)),
                write(cache, NETHER_SPIRAL, netherSpiral()),
                write(cache, NETHER_DIORAMA, importExplicitNetherStructure("NetherDiorama.java", 32, 13, 22)),
                write(cache, OVERWORLD_CAVERN, importCavern()),
                write(cache, OVERWORLD_BURROW, importBurrowBase()),
                write(cache, OVERWORLD_BURROW_FURNACE, importBurrowAnnex(false)),
                write(cache, OVERWORLD_BURROW_LOOT, importBurrowAnnex(true)),
                write(cache, OVERWORLD_OCEAN, importOcean()),
                write(cache, OVERWORLD_OCEAN_PIT_SLICE, oceanPitSlice()),
                write(cache, OVERWORLD_DESERT, importDesert()),
                write(cache, OVERWORLD_SNOW, importSnow()),
                write(cache, OVERWORLD_BIOME_FRAGMENT, biomeFragment()),
                write(cache, PROXIMA_MONUMENT, importMonument()),
                write(cache, PROXIMA_LIGHT_PANEL_STAIR_BOTTOM,
                        importLightPanelStair("LightPanelStairBottom.java", 20, 19, 17, true)),
                write(cache, PROXIMA_LIGHT_PANEL_STAIR_SECTION,
                        importLightPanelStair("LightPanelStairSection.java", 17, 16, 17, false)),
                write(cache, PROXIMA_LIGHT_PANEL_STAIR_TOP,
                        importLightPanelStair("LightPanelStairTop.java", 17, 28, 17, false)),
                write(cache, PROXIMA_LIGHT_PANEL_LOOT, lightPanelLoot()),
                write(cache, lightPanelRoom(0), lightPanelRoomTemplate(0)),
                write(cache, lightPanelRoom(1), lightPanelRoomTemplate(1)),
                write(cache, lightPanelRoom(2), lightPanelRoomTemplate(2)),
                write(cache, lightPanelRoom(3), lightPanelRoomTemplate(3)),
                write(cache, lightPanelRoom(4), lightPanelRoomTemplate(4)),
                write(cache, lightPanelRoom(5), lightPanelRoomTemplate(5)),
                write(cache, lightPanelRoom(6), lightPanelRoomTemplate(6)),
                write(cache, PROXIMA_TD_MAZE_CELL, threeDMazeCell()),
                write(cache, PROXIMA_TD_MAZE_SHAFT, threeDMazeShaftSlice()),
                write(cache, PROXIMA_TD_MAZE_ENTRANCE, importThreeDMazeEntrance()),
                write(cache, PROXIMA_TD_MAZE_LOOT, importThreeDMazeLoot()),
                write(cache, PROXIMA_MUSIC_FUNNEL, importMusicFunnel()),
                write(cache, PROXIMA_MUSIC_ROOM, importMusicRoom()),
                write(cache, PROXIMA_MUSIC_LOOT, importMusicLoot()),
                write(cache, golChamber(1), golChamberTemplate(1)),
                write(cache, golChamber(2), golChamberTemplate(2)),
                write(cache, golChamber(3), golChamberTemplate(3)),
                write(cache, PROXIMA_GOL_ENTRANCE_PREFAB, importGOLPlacementMethod(
                        "GOLEntrance.java", "generatePrefab", 8, 8, 17, GOLTemplateKind.STONE, 595)),
                write(cache, PROXIMA_GOL_SURFACE, golSurface()),
                write(cache, PROXIMA_GOL_ENTRANCE_DOOR, importGOLPlacementMethod(
                        "GOLDoors.java", "generateEntrance", 1, 8, 17, GOLTemplateKind.CLOAK, 136)),
                write(cache, PROXIMA_GOL_EXIT_DOOR, importGOLPlacementMethod(
                        "GOLDoors.java", "generateExit", 1, 5, 9, GOLTemplateKind.EXIT, 45)),
                write(cache, PROXIMA_GOL_LOOT, importGOLLoot()),
                write(cache, villageTemplate("plains", true), importVillageStructure("plains", true)),
                write(cache, villageTemplate("desert", true), importVillageStructure("desert", true)),
                write(cache, villageTemplate("savanna", true), importVillageStructure("savanna", true)),
                write(cache, villageTemplate("snowy", true), importVillageStructure("snowy", true)),
                write(cache, villageTemplate("taiga", true), importVillageStructure("taiga", true)),
                write(cache, villageTemplate("plains", false), importVillageStructure("plains", false)),
                write(cache, villageTemplate("desert", false), importVillageStructure("desert", false)),
                write(cache, villageTemplate("savanna", false), importVillageStructure("savanna", false)),
                write(cache, villageTemplate("snowy", false), importVillageStructure("snowy", false)),
                write(cache, villageTemplate("taiga", false), importVillageStructure("taiga", false)));
    }

    public static Identifier villageTemplate(String style, boolean wooden) {
        return id("worldgen/village/" + style + "/" + (wooden ? "wooden_chroma" : "broken_chroma"));
    }

    public static Identifier lightPanelRoom(int tier) {
        if (tier < 0 || tier >= reika.chromaticraft.world.dimension.structure.lightpanel
                .LightPanelPatternLibrary.tierCount())
            throw new IllegalArgumentException("Glowing Logic tier " + tier);
        return id("worldgen/proxima/light_panel/room_tier_" + tier);
    }

    public static Identifier golChamber(int difficulty) {
        if (difficulty < 1 || difficulty > 3)
            throw new IllegalArgumentException("Cellular Automata difficulty " + difficulty);
        return id("worldgen/proxima/gol/chamber_" + difficulty);
    }

    /**
     * Mechanical NBT import of V33a {@code VillagersFailChromatiCraft}. The old placement calls are
     * deliberately kept as the single geometry authority; datagen translates their block metadata
     * into concrete 26.2 block identities and states, then supplies the jigsaw entrance which the
     * pre-jigsaw source could not have contained.
     */
    private static TemplateData importVillageStructure(String style, boolean wooden) {
        Path source = legacyVillageSource();
        final String java;
        try { java = Files.readString(source); }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a failed-casting village source " + source, e);
        }
        String classMarker = wooden ? "public static class WoodenChromaStructure"
                : "public static class BrokenChromaStructure";
        int classStart = java.indexOf(classMarker);
        int methodStart = java.indexOf("protected boolean generate", classStart);
        if (classStart < 0 || methodStart < 0)
            throw new IllegalStateException("Missing V33a village generator " + classMarker);
        String body = extractJavaBlock(java, java.indexOf('{', methodStart));

        TemplateData data = new TemplateData(wooden ? 15 : 16, wooden ? 8 : 7, wooden ? 15 : 17);
        if (wooden) {
            // V33a clearVolume() writes the full bounding box to air before building the house.
            for (int x = 0; x < 15; x++) for (int y = 0; y < 8; y++) for (int z = 0; z < 15; z++)
                data.set(x, y, z, AIR);
        }
        Pattern calls = Pattern.compile("this\\.placeBlockAt(?:Fixed|Current)Position\\(world,\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(.+)\\);");
        Matcher matcher = calls.matcher(body);
        int found = 0;
        while (matcher.find()) {
            String argument = matcher.group(4).trim();
            int split = lastTopLevelComma(argument);
            String symbol = split >= 0 ? argument.substring(0, split).trim() : argument;
            int metadata = split >= 0 ? parseLegacyInt(argument.substring(split + 1)) : 0;
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), villageState(style, symbol, metadata));
            found++;
        }
        if (found < (wooden ? 500 : 100))
            throw new IllegalStateException("Parsed only " + found + " cells from " + classMarker);

		// Legacy upper-door metadata stores hinge/power bits, not the lower half's facing. A
		// structure template has to carry a complete valid state on both halves, so inherit the
		// facing from each matching lower half after all source placement calls have been parsed.
		data.inheritDoorFacings();

        if (wooden) {
            data.set(7, 0, 7, lootChest("north"), villageLootChestNBT("village_casting"));
            data.set(7, 2, 7, standingSign(9), signNBT("Your thingy was", "weird. It's my",
                    "house now", "--Villager 19"));
            data.set(0, 0, 7, jigsaw("west_up"), villageJigsawNBT(style,
                    villageState(style, "this.getStair(world)", 0)));
        }
        else {
            data.set(7, 0, 10, lootChest("north"), villageLootChestNBT("village_casting"));
            data.set(7, 1, 11, vanillaChest("west"), vanillaLootChestNBT("village_casting_junk"));
            data.set(0, 6, 11, wallSign("west"), signNBT("guys i think my", "lapiz is broken", "", "--Villager 26"));
            data.set(1, 3, 3, wallSign("north"), signNBT("Couldn't reach.", "", "Good enough?", ""));
            data.set(6, 1, 11, wallSign("west"), signNBT("Propertee", "of Villager", "#73", "~angryface"));
            data.set(0, 0, 8, jigsaw("west_up"), villageJigsawNBT(style, AIR));
        }
        return data;
    }

    private static String extractJavaBlock(String source, int openingBrace) {
        int depth = 0;
        for (int i = openingBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return source.substring(openingBrace + 1, i);
        }
        throw new IllegalStateException("Unterminated Java method at " + openingBrace);
    }

    private static int lastTopLevelComma(String value) {
        int depth = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            char c = value.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') depth--;
            else if (c == ',' && depth == 0) return i;
        }
        return -1;
    }

    private static StateDef villageState(String style, String symbol, int metadata) {
        String wood = switch (style) {
            case "savanna" -> "acacia";
            case "snowy", "taiga" -> "spruce";
            default -> "oak";
        };
        if (symbol.equals("this.getBase(world)"))
            return new StateDef(style.equals("desert") ? "minecraft:sandstone" : "minecraft:cobblestone", Map.of());
        if (symbol.equals("this.getFloor(world)"))
            return new StateDef(style.equals("desert") ? "minecraft:smooth_sandstone" : "minecraft:" + wood + "_planks", Map.of());
        if (symbol.startsWith("this.getColumns(world")) {
            if (style.equals("desert")) return new StateDef("minecraft:cut_sandstone", Map.of());
            return new StateDef("minecraft:" + wood + "_log", Map.of("axis",
                    (metadata & 12) == 4 ? "x" : (metadata & 12) == 8 ? "z" : "y"));
        }
        if (symbol.equals("this.getStair(world)"))
            return legacyVillageStair(style.equals("desert") ? "sandstone" : wood, metadata);
        return switch (symbol) {
            case "b" -> stone(StoneTypes.list[Math.max(0, Math.min(StoneTypes.list.length - 1, metadata))]);
			case "Blocks.glass" -> new StateDef("minecraft:glass", Map.of());
            case "Blocks.glass_pane" -> new StateDef("minecraft:glass_pane", Map.of("east", "false", "north", "false", "south", "false", "waterlogged", "false", "west", "false"));
            case "Blocks.double_stone_slab" -> new StateDef("minecraft:smooth_stone_slab", Map.of("type", "double", "waterlogged", "false"));
			case "Blocks.crafting_table" -> new StateDef("minecraft:crafting_table", Map.of());
			case "Blocks.glowstone" -> new StateDef("minecraft:glowstone", Map.of());
            case "Blocks.torch" -> legacyVillageTorch(metadata);
            case "Blocks.wooden_door" -> legacyVillageDoor(wood, metadata);
            case "Blocks.coal_ore" -> new StateDef("minecraft:coal_ore", Map.of());
            case "Blocks.wool" -> new StateDef(metadata == 11 ? "minecraft:blue_wool" : "minecraft:white_wool", Map.of());
            case "Blocks.gravel" -> new StateDef("minecraft:gravel", Map.of());
            case "Blocks.flowing_water" -> new StateDef("minecraft:water", Map.of("level", "0"));
            default -> throw new IllegalStateException("Unmapped V33a village block " + symbol + " metadata " + metadata);
        };
    }

    private static StateDef legacyVillageStair(String material, int metadata) {
        String[] facings = {"east", "west", "south", "north"};
        return new StateDef("minecraft:" + material + "_stairs", Map.of(
                "facing", facings[metadata & 3], "half", (metadata & 4) != 0 ? "top" : "bottom",
                "shape", "straight", "waterlogged", "false"));
    }

    private static StateDef legacyVillageTorch(int metadata) {
        if (metadata == 0 || metadata == 5) return new StateDef("minecraft:torch", Map.of());
        String[] facings = {"east", "west", "south", "north"};
        return new StateDef("minecraft:wall_torch", Map.of("facing", facings[Math.max(1, metadata) - 1]));
    }

    private static StateDef legacyVillageDoor(String wood, int metadata) {
        String[] facings = {"east", "south", "west", "north"};
        boolean upper = (metadata & 8) != 0;
        return new StateDef("minecraft:" + wood + "_door", Map.of(
                "facing", facings[metadata & 3], "half", upper ? "upper" : "lower", "hinge", "left",
                "open", "false", "powered", "false"));
    }

    private static StateDef standingSign(int rotation) {
        return new StateDef("minecraft:oak_sign", Map.of("rotation", Integer.toString(rotation & 15), "waterlogged", "false"));
    }

    private static StateDef wallSign(String facing) {
        return new StateDef("minecraft:oak_wall_sign", Map.of("facing", facing, "waterlogged", "false"));
    }

    private static StateDef jigsaw(String orientation) {
        return new StateDef("minecraft:jigsaw", Map.of("orientation", orientation));
    }

    private static CompoundTag villageJigsawNBT(String style, StateDef finalState) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:jigsaw");
        nbt.putString("joint", "aligned");
        nbt.putString("name", "minecraft:building_entrance");
        nbt.putString("target", "minecraft:building_entrance");
        nbt.putString("pool", "minecraft:village/" + style + "/streets");
        nbt.putString("final_state", stateString(finalState));
        return nbt;
    }

    private static String stateString(StateDef state) {
        if (state.properties().isEmpty()) return state.name();
        return state.name() + "[" + state.properties().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private static CompoundTag villageLootChestNBT(String table) {
        CompoundTag nbt = vanillaLootChestNBT(table);
        nbt.putString("id", "chromaticraft:loot_chest");
        ListTag triggers = new ListTag();
        triggers.add(net.minecraft.nbt.StringTag.valueOf("VILLAGECASTING"));
        nbt.put("triggers", triggers);
        return nbt;
    }

    private static CompoundTag vanillaLootChestNBT(String table) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:chest");
        nbt.putString("LootTable", "chromaticraft:chests/" + table);
        return nbt;
    }

    private static CompoundTag signNBT(String... lines) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:sign");
        nbt.put("front_text", signText(lines));
        nbt.put("back_text", signText(new String[] {"", "", "", ""}));
        nbt.putBoolean("is_waxed", false);
        return nbt;
    }

    private static CompoundTag signText(String[] lines) {
        CompoundTag text = new CompoundTag();
        ListTag messages = new ListTag();
        for (int i = 0; i < 4; i++) messages.add(net.minecraft.nbt.StringTag.valueOf(lines[i]));
        text.put("messages", messages);
        text.putString("color", "black");
        text.putBoolean("has_glowing_text", false);
        return text;
    }

    /** Exact 15x14x15 V33a Biome Fragment shell plus all runtime-configured puzzle cells. */
    private static TemplateData biomeFragment() {
        TemplateData data = new TemplateData(15, 14, 15);
        StateDef cloak = biomeShield("cloak");
        StateDef stone = biomeShield("stone");
        StateDef cobble = biomeShield("cobble");
        StateDef glass = biomeShield("glass");
        StateDef light = biomeShield("light");

        // V33a first clears the complete upper workspace before writing its concentric shell.
        for (int y = 5; y <= 9; y++)
            for (int x = -6; x <= 6; x++)
                for (int z = -6; z <= 6; z++) biomeSet(data, x, y, z, AIR);

        for (int y = -2; y <= 6; y++) {
            int radius = y == -2 ? 1 : 2;
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    if (y == 0 && x == 0 && z == 0) continue;
                    int distance = Math.max(Math.abs(x), Math.abs(z));
                    StateDef state = cloak;
                    if (y == -1 && distance <= 1) state = AIR;
                    else if (y == 0) state = distance <= 1 ? AIR : stone;
                    else if (y == 1) state = distance <= 1
                            ? new StateDef("chromaticraft:chroma_door", Map.of("open", "false")) : cobble;
                    else if (distance <= 1) state = new StateDef("chromaticraft:hover",
                            Map.of("decay", "permanent", "type", "elevate"));
                    else if (y > 0) state = y == 6 ? cobble : AIR;
                    biomeSet(data, x, y, z, state);
                }
        }

        for (int y = 1; y <= 9; y++)
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++)
                    if (y > 6 || Math.abs(x) == 3 || Math.abs(z) == 3) {
                        StateDef state = stone;
                        if (y > 6) state = AIR;
                        else if ((x == 0 || z == 0) && y > 1 && y < 6) state = cobble;
                        else if (y == 6 && (Math.abs(x) == 2 || Math.abs(z) == 2)) state = cobble;
                        else if (y == 3 && (Math.abs(x) == 1 || Math.abs(z) == 1)) state = glass;
                        biomeSet(data, x, y, z, state);
                    }

        for (int y = 6; y <= 9; y++)
            for (int d = -4; d <= 4; d++) {
                if (y != 9 && Math.abs(d) == 4) continue;
                StateDef state = cobble;
                if (Math.abs(d) <= 1) state = y == 6 || y == 9 ? biomeStairs(d, y, 4) : AIR;
                else if (y == 9 || Math.abs(d) > 2) state = stone;
                biomeSet(data, d, y, 4, state == null ? biomeStairs(d, y, 4) : state);
                biomeSet(data, d, y, -4, Math.abs(d) <= 1 && (y == 6 || y == 9)
                        ? biomeStairs(d, y, -4) : state);
                biomeSet(data, 4, y, d, Math.abs(d) <= 1 && (y == 6 || y == 9)
                        ? biomeStairs(4, y, d) : state);
                biomeSet(data, -4, y, d, Math.abs(d) <= 1 && (y == 6 || y == 9)
                        ? biomeStairs(-4, y, d) : state);
            }

        for (int y = 5; y <= 8; y++)
            for (int d = -6; d <= 6; d++) {
                StateDef state = stone;
                if (Math.abs(d) <= 1) state = y == 5 ? biomeStairs(d, y, 7) : AIR;
                else if (Math.abs(d) == 2) state = cobble;
                else if ((y == 7 || y == 8) && Math.abs(d) >= 3 && Math.abs(d) <= 5) state = glass;
                biomeSet(data, d, y, 7, Math.abs(d) <= 1 && y == 5 ? biomeStairs(d, y, 7) : state);
                biomeSet(data, d, y, -7, Math.abs(d) <= 1 && y == 5 ? biomeStairs(d, y, -7) : state);
                biomeSet(data, 7, y, d, Math.abs(d) <= 1 && y == 5 ? biomeStairs(7, y, d) : state);
                biomeSet(data, -7, y, d, Math.abs(d) <= 1 && y == 5 ? biomeStairs(-7, y, d) : state);
            }

        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                int distance = Math.max(Math.abs(x), Math.abs(z));
                if (distance >= 4 && distance <= 6)
                    biomeSet(data, x, 5, z, Math.abs(x) == 5 && Math.abs(z) == 5 ? light : stone);
            }
        for (int x = -7; x <= 7; x++)
            for (int z = -7; z <= 7; z++) {
                int distance = Math.max(Math.abs(x), Math.abs(z));
                if (distance >= 5 && distance <= 7 && Math.abs(x) + Math.abs(z) <= 12)
                    biomeSet(data, x, 9, z, stone);
            }
        for (int[] point : new int[][] {{2,7},{-2,7},{2,-7},{-2,-7},{7,2},{7,-2},{-7,2},{-7,-2}})
            biomeSet(data, point[0], 9, point[1], cobble);

        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) > 6) continue;
                StateDef state = stone;
                if (x == 0 && Math.abs(z) == 2 || z == 0 && Math.abs(x) == 2) state = light;
                else if (Math.abs(x) == 4 || Math.abs(z) == 4
                        || Math.abs(x) == 3 && Math.abs(z) == 3) state = cobble;
                biomeSet(data, x, 10, z, state);
            }
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) biomeSet(data, x, 11, z, stone);
        for (int x : new int[] {-2, 2})
            for (int z : new int[] {-2, 2}) biomeSet(data, x, 11, z, stone);
        for (int y = 6; y <= 8; y++)
            for (int x : new int[] {-6, 6})
                for (int z : new int[] {-6, 6}) biomeSet(data, x, y, z, stone);
        for (int d = 5; d <= 7; d++) {
            for (int x : new int[] {-2, 2}) {
                biomeSet(data, -d, 10, x, cobble); biomeSet(data, d, 10, x, cobble);
                biomeSet(data, x, 10, -d, cobble); biomeSet(data, x, 10, d, cobble);
            }
        }
        for (int d = 3; d <= 5; d++)
            for (int x : new int[] {-d, d})
                for (int z : new int[] {-d, d}) biomeSet(data, x, 10, z, cobble);

        for (int y = 1; y <= 4; y++)
            for (int d = -6; d <= 6; d++) {
                biomeSet(data, d, y, 6, stone); biomeSet(data, d, y, -6, stone);
                biomeSet(data, 6, y, d, stone); biomeSet(data, -6, y, d, stone);
            }
        for (int y = 1; y <= 4; y++)
            for (int x = -5; x <= 5; x++)
                for (int z = -5; z <= 5; z++) {
                    int distance = Math.max(Math.abs(x), Math.abs(z));
                    if (distance < 4 || distance > 5) continue;
                    StateDef state = stone;
                    if ((z > 0 && x == 2) || (z < 0 && x == -2)
                            || (x < 0 && z == 2) || (x > 0 && z == -2)) {
                        if (y == 3) state = glass;
                    }
                    else if (y == 2 && ((z > 0 && x == 1) || (z < 0 && x == -1)
                            || (x < 0 && z == 1) || (x > 0 && z == -1)))
                        state = new StateDef("chromaticraft:music_trigger", Map.of());
                    else if (y > 1) state = AIR;
                    biomeSet(data, x, y, z, state);
                }

        biomeChest(data, -5, 2, 4, "east"); biomeChest(data, 4, 2, 5, "north");
        biomeChest(data, 5, 2, -4, "west"); biomeChest(data, -4, 2, -5, "south");
        biomeChest(data, -4, 6, 4, "west"); biomeChest(data, 4, 6, 4, "south");
        biomeChest(data, 4, 6, -4, "east"); biomeChest(data, -4, 6, -4, "north");
        for (int[] direction : new int[][] {{0,-1,-1,0},{0,1,1,0},{-1,0,0,1},{1,0,0,-1}})
            for (int a = 3; a <= 5; a++)
                for (int b = 4; b <= 5; b++) biomeSet(data,
                        direction[0] * b + direction[2] * a, 0,
                        direction[1] * b + direction[3] * a, cloak);

        addBiomePuzzleCells(data);
        biomeSet(data, 0, 0, 0, STRUCTURE_VOID);
        return data;
    }

    private static void addBiomePuzzleCells(TemplateData data) {
        for (int[] rune : new int[][] {{5,5,6},{6,5,5},{-5,5,6},{-6,5,5},
                {5,5,-6},{6,5,-5},{-5,5,-6},{-6,5,-5}})
            biomeSet(data, rune[0], rune[1], rune[2], RUNE_PLACEHOLDER);
        StateDef colorLock = new StateDef("chromaticraft:color_lock", Map.of("gate", "false", "open", "false"));
        for (int door = 0; door < 4; door++)
            for (int y = 2; y <= 4; y++)
                for (int d = 1; d <= 2; d++) {
                    int[][] p = {{d,y,-3},{3,y,d},{-d,y,3},{-3,y,-d}};
                    biomeSet(data, p[door][0], p[door][1], p[door][2], colorLock);
                }
        StateDef shift = new StateDef("chromaticraft:shift_lock", Map.of("passability", "closed"));
        for (int door = 0; door < 4; door++)
            for (int y = 6; y <= 8; y++)
                for (int d = 5; d <= 6; d++) {
                    int[][][] p = {{{-2,y,-d},{-d,y,-2}},{{-2,y,d},{-d,y,2}},
                            {{2,y,-d},{d,y,-2}},{{2,y,d},{d,y,2}}};
                    for (int[] at : p[door]) biomeSet(data, at[0], at[1], at[2], shift);
                }
        int[][] switches = {{-3,8,-3},{3,8,-3},{-3,8,3},{3,8,3}};
        for (int[] at : switches) {
            biomeSet(data, at[0], at[1], at[2], new StateDef("chromaticraft:panel_switch", Map.of("up", "false")));
            biomeSet(data, at[0], at[1] + 1, at[2], new StateDef("chromaticraft:light_panel",
                    Map.of("active", "true", "type", "block")));
            biomeSet(data, at[0], at[1] - 1, at[2], new StateDef("chromaticraft:light_panel",
                    Map.of("active", "false", "type", "target")));
        }
        StateDef key = new StateDef("chromaticraft:lock_key", Map.of("channel", "0"));
        biomeSet(data, 0, 5, -2, key); biomeSet(data, 0, 5, 2, key);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) biomeSet(data, x, 1, z,
                    new StateDef("chromaticraft:chroma_door", Map.of("open", "false")));
        for (int y = 2; y <= 4; y++)
            for (int d = 4; d <= 5; d++) {
                biomeSet(data, d, y, 3, new StateDef("chromaticraft:chroma_door", Map.of("open", "false")));
                biomeSet(data, -3, y, d, new StateDef("chromaticraft:chroma_door", Map.of("open", "false")));
                biomeSet(data, -d, y, -3, new StateDef("chromaticraft:chroma_door", Map.of("open", "false")));
                biomeSet(data, 3, y, -d, new StateDef("chromaticraft:chroma_door", Map.of("open", "false")));
            }
        for (int[] crystal : new int[][] {{-4,3,1},{-5,3,1},{-1,3,-4},{-1,3,-5},
                {4,3,-1},{5,3,-1},{1,3,4},{1,3,5}})
            biomeSet(data, crystal[0], crystal[1], crystal[2],
                    new StateDef("chromaticraft:crystal_lamp_black", Map.of()));
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) biomeSet(data, x, -1, z,
                    new StateDef("minecraft:water", Map.of("level", "0")));
        for (int[] direction : new int[][] {{0,-1,-1,0},{0,1,1,0},{-1,0,0,1},{1,0,0,-1}})
            for (int a = 3; a <= 5; a++)
                for (int b = 4; b <= 5; b++) {
                    if (b == 5 && a == 4) continue;
                    biomeSet(data, direction[0] * b + direction[2] * a, 1,
                            direction[1] * b + direction[3] * a,
                            new StateDef("minecraft:water", Map.of("level", "0")));
                }
        biomeSet(data, 0, 10, 0, new StateDef("chromaticraft:biome_replay", Map.of()));
    }

    private static void biomeChest(TemplateData data, int x, int y, int z, String facing) {
        biomeSet(data, x, y, z, lootChest(facing));
    }

    private static void biomeSet(TemplateData data, int x, int y, int z, StateDef state) {
        data.set(x + 7, y + 2, z + 7, state);
    }

    private static StateDef biomeShield(String type) {
        return new StateDef("chromaticraft:shielding_" + type, Map.of("reinforced", "true"));
    }

    private static StateDef biomeStairs(int x, int y, int z) {
        boolean top = y == 9;
        boolean xAxis = Math.abs(x) > Math.abs(z);
        String facing = xAxis ? x > 0 ? "west" : "east" : z > 0 ? "north" : "south";
        return new StateDef("minecraft:stone_brick_stairs", Map.of("facing", facing,
                "half", top ? "top" : "bottom", "shape", "straight", "waterlogged", "false"));
    }

	/** Exact 17x15x17 V33a Snow Temple, including its intended Wolf spawners and puzzle blocks. */
	private static TemplateData importSnow() {
		Path source = legacyWorldgenSource("SnowStructure.java");
		final String java;
		try { java = Files.readString(source); }
		catch (IOException e) { throw new IllegalStateException("Could not read V33a Snow source " + source, e); }
		TemplateData data = new TemplateData(17, 15, 17);
		int base = importSnowMethod(java, "getBaseStructure", data);
		int air = importSnowMethod(java, "getAirSpaces", data);
		if (base != 1281 || air != 552)
			throw new IllegalStateException("Expected 1281 Snow base entries and 552 air/lava entries; parsed "
					+ base + " and " + air);

		CompoundTag wolf = spawnerNBT("minecraft:wolf", 8, 30, 200, 800);
		wolf.putShort("Delay", (short)0);
		for (BlockPos pos : List.of(new BlockPos(14, 4, 14), new BlockPos(3, 4, 14), new BlockPos(2, 4, 4)))
			data.set(pos.getX(), pos.getY(), pos.getZ(), new StateDef("minecraft:spawner", Map.of()), wolf.copy());

		StateDef stone = new StateDef("chromaticraft:shielding_stone", Map.of("reinforced", "true"));
		for (int x = 7; x <= 9; x++)
			for (int z = 7; z <= 10; z++)
				data.set(x, 5, z, stone);
		StateDef locked = new StateDef("chromaticraft:shift_lock", Map.of("passability", "closed_hidden"));
		for (int y = 6; y <= 8; y++) {
			for (int z = 7; z <= 9; z++) {
				data.set(6, y, z, locked);
				data.set(10, y, z, locked);
			}
			for (int x = 7; x <= 9; x++) {
				data.set(x, y, 6, locked);
				data.set(x, y, 10, locked);
			}
		}
		data.set(8, 3, 6, STRUCTURE_VOID);
		return data;
	}

	private static int importSnowMethod(String java, String method, TemplateData data) {
		String declaration = "private void " + method;
		int start = java.indexOf(declaration);
		if (start < 0) throw new IllegalStateException("Could not isolate Snow method " + method);
		start = java.indexOf('{', start) + 1;
		int depth = 1, end = start;
		while (end < java.length() && depth > 0) {
			char c = java.charAt(end++);
			if (c == '{') depth++;
			else if (c == '}') depth--;
		}
		String body = java.substring(start, end - 1).replaceAll("(?m)^\\s*//.*$", "");
		Pattern blocks = Pattern.compile("array\\.setBlock\\(x \\+ (\\d+), y \\+ (\\d+), z \\+ (\\d+), (.+?)\\);");
		Matcher matcher = blocks.matcher(body);
		int found = 0;
		while (matcher.find()) {
			String raw = matcher.group(4).trim();
			StateDef state = switch (raw) {
				case "b2" -> new StateDef("chromaticraft:trap_floor", Map.of("disguise", "self"));
				case "Blocks.air" -> AIR;
				case "this.getLava()" -> new StateDef("geostrata:lava_rock", Map.of());
				case "shield, ms" -> new StateDef("chromaticraft:shielding_stone", Map.of("reinforced", "true"));
				case "shield, mc" -> new StateDef("chromaticraft:shielding_cobble", Map.of("reinforced", "true"));
				case "shield, mg" -> new StateDef("chromaticraft:shielding_glass", Map.of("reinforced", "true"));
				case "shield, BlockType.CLOAK.metadata" -> new StateDef("chromaticraft:shielding_cloak", Map.of("reinforced", "true"));
				case "shield, BlockType.LIGHT.metadata" -> new StateDef("chromaticraft:shielding_light", Map.of("reinforced", "true"));
				case "shield, BlockType.MOSS.metadata" -> new StateDef("chromaticraft:shielding_moss", Map.of("reinforced", "true"));
				default -> throw new IllegalStateException("Unhandled Snow state '" + raw + "'");
			};
			data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3)), state);
			found++;
		}
		Pattern chests = Pattern.compile("addLootChest\\(array, x \\+ (\\d+), y \\+ (\\d+), z \\+ (\\d+), (\\d+)\\)");
		matcher = chests.matcher(body);
		while (matcher.find()) {
			String facing = switch (Integer.parseInt(matcher.group(4))) {
				case 2 -> "north"; case 3 -> "south"; default -> "north";
			};
			data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3)), lootChest(facing));
			found++;
		}
		return found;
	}

    /** Exact V33a Desert Temple geometry and all source-programmed spawners. */
    private static TemplateData importDesert() {
        Path source = legacyWorldgenSource("DesertStructure.java");
        final String java;
        try { java = Files.readString(source); }
        catch (IOException e) { throw new IllegalStateException("Could not read V33a Desert source " + source, e); }
        TemplateData data = new TemplateData(15, 13, 16);
        Pattern blocks = Pattern.compile("array\\.setBlock\\(x(?:\\+(\\d+))?,\\s*y(?:\\+(\\d+))?,\\s*z(?:\\+(\\d+))?,\\s*([^;)]+)\\);");
        Matcher matcher = blocks.matcher(java);
        int found = 0;
        while (matcher.find()) {
            String raw = matcher.group(4).trim();
            // The helper body is replayed from its five call sites below.
            if (raw.equals("Blocks.mob_spawner"))
                continue;
            StateDef state = switch (raw) {
                case "Blocks.air" -> AIR;
                case "Blocks.sand" -> new StateDef("minecraft:sand", Map.of());
                case "Blocks.sandstone" -> new StateDef("minecraft:sandstone", Map.of());
                case "shield, ms", "shield, mcrack" -> new StateDef("chromaticraft:shielding_stone", Map.of("reinforced", "true"));
                case "shield, mc" -> new StateDef("chromaticraft:shielding_cobble", Map.of("reinforced", "true"));
                default -> throw new IllegalStateException("Unhandled Desert state '" + raw + "'");
            };
            data.set(groupInt(matcher, 1), groupInt(matcher, 2), groupInt(matcher, 3), state);
            found++;
        }
        Pattern chests = Pattern.compile("addLootChest\\(array,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*(\\d+)\\)");
        matcher = chests.matcher(java);
        while (matcher.find()) {
            int legacy = Integer.parseInt(matcher.group(4)) & 7;
            String facing = switch (legacy) { case 2 -> "north"; case 3 -> "south"; case 4 -> "west"; case 5 -> "east"; default -> "north"; };
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), lootChest(facing));
            found++;
        }
        Pattern spawners = Pattern.compile("addSpawner\\(array,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+)\\)");
        matcher = spawners.matcher(java);
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String mob = y <= 4 ? "minecraft:blaze" : Math.abs(x) == Math.abs(z) ? "minecraft:spider" : "minecraft:silverfish";
            CompoundTag nbt = spawnerNBT(mob, 6, 4, mob.equals("minecraft:blaze") ? 40 : 200,
                    mob.equals("minecraft:blaze") ? 100 : 800);
            nbt.putShort("Delay", (short)0);
            data.set(x, y, z, new StateDef("minecraft:spawner", Map.of()), nbt);
            found++;
        }
        if (found != 1863)
            throw new IllegalStateException("Expected 1863 V33a Desert cells, parsed " + found + " from " + source);
        data.set(7, 3, 7, STRUCTURE_VOID);
        return data;
    }

    /** Canonical one-block-high repeatable slice used by V33a to join the temple to a cave. */
    private static TemplateData oceanPitSlice() {
        TemplateData data = new TemplateData(7, 1, 7);
        StateDef cloak = new StateDef("chromaticraft:shielding_cloak", Map.of("reinforced", "true"));
        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 5; z++)
                data.set(x, 0, z, x == 1 || x == 5 || z == 1 || z == 5 ? cloak : AIR);
        return data;
    }

    /**
     * Exact V33a Ocean Temple, normalized around its controller at template coordinate (3,5,3).
     * The old generator assembled this from six authored methods; replaying those methods in their
     * actual call order preserves intentional later overwrites while emitting one ordinary NBT
     * structure for runtime placement.
     */
    private static TemplateData importOcean() {
        Path source = legacyWorldgenSource("OceanStructure.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Ocean source " + source, e);
        }
        TemplateData data = new TemplateData(31, 13, 31);
        int found = 0;
        for (String method : new String[] {"addChests", "addPit", "addCloakTunnel", "addCovers",
                "getArray", "addAir", "addEndCaps"})
            found += importOceanMethod(java, method, data);
        if (found != 3441)
            throw new IllegalStateException("Expected 3441 V33a Ocean cells, parsed " + found + " from " + source);
        data.set(3, 5, 3, STRUCTURE_VOID);
        return data;
    }

    private static int importOceanMethod(String java, String method, TemplateData data) {
        Pattern declaration = Pattern.compile("(?m)^\\s*(?:public|private)\\s+(?:static\\s+)?[^\\n]+\\s+" + method + "\\([^\\n]*\\)\\s*\\{");
        Matcher declarationMatcher = declaration.matcher(java);
        if (!declarationMatcher.find())
            throw new IllegalStateException("Could not isolate V33a Ocean method " + method);
        int start = declarationMatcher.end();
        int depth = 1;
        int end = start;
        while (end < java.length() && depth > 0) {
            char c = java.charAt(end++);
            if (c == '{') depth++;
            else if (c == '}') depth--;
        }
        String body = java.substring(start, end - 1);
        Pattern blocks = Pattern.compile("array\\.setBlock\\(x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*([^;)]+)\\);");
        Matcher matcher = blocks.matcher(body);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String raw = matcher.group(4).trim();
            StateDef state;
            CompoundTag blockEntity = null;
            if (raw.equals("Blocks.air")) state = AIR;
            else if (raw.equals("Blocks.torch, 5")) state = new StateDef("minecraft:torch", Map.of());
            else if (raw.equals("Blocks.mob_spawner")) {
                state = new StateDef("minecraft:spawner", Map.of());
                blockEntity = spawnerNBT("minecraft:creeper", 16, 8, 200, 400);
            }
            else if (raw.startsWith("shield,") || raw.startsWith("b,")) {
                int metadata = raw.endsWith("13") ? 13 : raw.endsWith("9") || raw.endsWith("meta") ? 9 : 8;
                String shield = switch (metadata & 7) {
                    case 0 -> "shielding_cloak";
                    case 1 -> "shielding_stone";
                    case 5 -> "shielding_glass";
                    default -> throw new IllegalStateException("Unhandled Ocean shield metadata " + metadata);
                };
                state = new StateDef("chromaticraft:" + shield, Map.of("reinforced", "true"));
            }
            else throw new IllegalStateException("Unhandled Ocean state '" + raw + "' in " + method);
            if (blockEntity == null) data.set(x, y, z, state);
            else data.set(x, y, z, state, blockEntity);
            found++;
        }
        if (method.equals("addChests")) {
            Pattern chests = Pattern.compile("addLootChest\\(array,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*8(?:\\+(\\d+))?\\)");
            matcher = chests.matcher(body);
            while (matcher.find()) {
                String facing = switch (matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4))) {
                    case 0 -> "down";
                    case 1 -> "up";
                    case 2 -> "north";
                    case 3 -> "south";
                    case 4 -> "west";
                    case 5 -> "east";
                    default -> throw new IllegalStateException("Invalid Ocean chest facing");
                };
                // Vertical legacy facings had no horizontal chest equivalent; north is the V33a
                // block's fallback visual orientation for those two entries.
                if (facing.equals("up") || facing.equals("down")) facing = "north";
                data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), lootChest(facing));
                found++;
            }
        }
        return found;
    }

    /** Exact optional V33a annex geometry; machine/key/door callbacks remain runtime-owned. */
    private static TemplateData importBurrowAnnex(boolean lootRoom) {
        Path source = legacyWorldgenSource("BurrowStructure.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Burrow source " + source, e);
        }
        String method = lootRoom ? "public FilledBlockArray getLootRoom" : "public FilledBlockArray getFurnaceRoom";
        int start = java.indexOf(method);
        int end = lootRoom ? java.indexOf("private void placeFurnace", start)
                : java.indexOf("public FilledBlockArray getLootRoom", start);
        if (start < 0 || end < 0)
            throw new IllegalStateException("Could not isolate V33a Burrow " + method + " in " + source);
        String body = java.substring(start, end).replaceAll("(?m)^\\s*//.*$", "");
        TemplateData data = lootRoom ? new TemplateData(8, 5, 7) : new TemplateData(7, 6, 11);
        Pattern blocks = Pattern.compile("array\\.setBlock\\(x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*(.+)\\);");
        Matcher matcher = blocks.matcher(body);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String raw = matcher.group(4).trim();
            StateDef state;
            if (raw.startsWith("shield,"))
                state = new StateDef(raw.contains("CRACK") ? "chromaticraft:shielding_crack"
                        : "chromaticraft:shielding_stone", Map.of("reinforced", "true"));
            else if (raw.equals("Blocks.air")) state = AIR;
            else if (raw.startsWith("Blocks.torch")) state = new StateDef("minecraft:torch", Map.of());
            else if (raw.startsWith("Blocks.chest")) state = vanillaChest("south");
            else if (raw.startsWith("getChestGen()")) state = lootChest("north");
            else throw new IllegalStateException("Unhandled Burrow annex state '" + raw + "'");
            data.set(x, y, z, state);
            found++;
        }
        if (lootRoom) {
            Pattern doors = Pattern.compile("placeDoor\\(array,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+)\\)");
            matcher = doors.matcher(body);
            while (matcher.find()) {
                data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), STRUCTURE_VOID);
                found++;
            }
            if (found != 104)
                throw new IllegalStateException("Expected 104 V33a Burrow loot-room cells, parsed " + found);
        }
        else {
            Pattern furnaces = Pattern.compile("placeFurnace\\(array,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+)\\)");
            matcher = furnaces.matcher(body);
            while (matcher.find()) {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                data.set(x, y, z, new StateDef("minecraft:furnace", Map.of("facing", "south", "lit", "true")));
                // FurnaceCallback installs the attached hot Heat Lamp one block above.
                data.set(x, y + 1, z, STRUCTURE_VOID);
                found++;
            }
            if (found != 100)
                throw new IllegalStateException("Expected 100 V33a Burrow furnace-room cells, parsed " + found);
        }
        return data;
    }

    /**
     * Exact V33a Burrow base, normalized around its controller at template coordinate (3,3,3).
     * The source-selected lamp colour and controller are runtime state, so those two cells are
     * structure-void markers in the canonical template. Optional annexes are deliberately separate
     * templates because V33a rolls them independently after the base has passed its site test.
     */
    private static TemplateData importBurrowBase() {
        Path source = legacyWorldgenSource("BurrowStructure.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Burrow source " + source, e);
        }
        int start = java.indexOf("public FilledBlockArray getArray");
        int end = java.indexOf("public FilledBlockArray getFurnaceRoom", start);
        if (start < 0 || end < 0)
            throw new IllegalStateException("Could not isolate V33a Burrow base in " + source);
        String body = java.substring(start, end).replaceAll("(?m)^\\s*//.*$", "");
        TemplateData data = new TemplateData(10, 12, 7);
        Pattern blocks = Pattern.compile("array\\.setBlock\\(x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*([^,);]+)(?:,\\s*([^);]+))?\\);");
        Matcher matcher = blocks.matcher(body);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String symbol = matcher.group(4).trim();
            String metadata = matcher.group(5) == null ? "" : matcher.group(5).trim();
            StateDef state = switch (symbol) {
                case "shield" -> new StateDef(metadata.contains("CRACK")
                        ? "chromaticraft:shielding_crack" : "chromaticraft:shielding_stone",
                        Map.of("reinforced", "true"));
                case "Blocks.stone" -> new StateDef("minecraft:stone", Map.of());
                case "Blocks.dirt" -> new StateDef("minecraft:dirt", Map.of());
                case "Blocks.grass" -> new StateDef("minecraft:grass_block", Map.of("snowy", "false"));
                case "Blocks.air" -> AIR;
                case "Blocks.torch" -> new StateDef("minecraft:torch", Map.of());
                case "ChromaBlocks.LAMP.getBlockInstance()" -> STRUCTURE_VOID;
                default -> throw new IllegalStateException("Unhandled Burrow base state '" + symbol + "'");
            };
            data.set(x, y, z, state);
            found++;
        }
        if (!body.contains("array.setBlock(x+3, y+1, z+3, ChromaBlocks.LAMP.getBlockInstance(), this.getCurrentColor().ordinal());"))
            throw new IllegalStateException("Missing V33a Burrow colour-lamp callback");
        data.set(3, 1, 3, STRUCTURE_VOID);
        found++;
        Pattern chests = Pattern.compile("addLootChest\\(array,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*ForgeDirection\\.([A-Z]+)\\)");
        matcher = chests.matcher(body);
        while (matcher.find()) {
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), lootChest(matcher.group(4).toLowerCase(Locale.ROOT)));
            found++;
        }
        if (found != 313)
            throw new IllegalStateException("Expected 313 V33a Burrow base cells, parsed " + found + " from " + source);
        data.set(3, 3, 3, STRUCTURE_VOID);
        return data;
    }

    /**
     * Exact V33a cavern shell. This source is a literal FilledBlockArray export, so datagen can
     * transcribe every authored cell without keeping a second coordinate list in sync. The template
     * anchor is the original controller position at (7,2,5); structure-void reserves that cell until
     * the modern controller is placed by the worldgen feature.
     */
    private static TemplateData importCavern() {
        Path source = legacyWorldgenSource("CavernStructure.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a cavern source " + source, e);
        }
        TemplateData data = new TemplateData(14, 6, 11);
        Pattern simple = Pattern.compile("array\\.setBlock\\(x(?:\\+(\\d+))?,\\s*y(?:\\+(\\d+))?,\\s*z(?:\\+(\\d+))?,\\s*(Blocks\\.air|shield)(?:,\\s*(\\d+))?\\);");
        Matcher matcher = simple.matcher(java);
        int found = 0;
        while (matcher.find()) {
            int x = groupInt(matcher, 1);
            int y = groupInt(matcher, 2);
            int z = groupInt(matcher, 3);
            StateDef state = matcher.group(4).equals("Blocks.air") ? AIR
                    : new StateDef("chromaticraft:shielding_cloak", Map.of("reinforced", "true"));
            data.set(x, y, z, state);
            found++;
        }
        Pattern colored = Pattern.compile("array\\.setBlock\\(x(?:\\+(\\d+))?,\\s*y(?:\\+(\\d+))?,\\s*z(?:\\+(\\d+))?,\\s*ChromaBlocks\\.(RUNE|CRYSTAL)\\.getBlockInstance\\(\\),\\s*CrystalElement\\.([A-Z_]+)\\.ordinal\\(\\)\\);");
        matcher = colored.matcher(java);
        while (matcher.find()) {
            String prefix = matcher.group(4).equals("RUNE") ? "crystal_rune_" : "cave_crystal_";
            data.set(groupInt(matcher, 1), groupInt(matcher, 2), groupInt(matcher, 3),
                    new StateDef("chromaticraft:" + prefix + matcher.group(5).toLowerCase(Locale.ROOT), Map.of()));
            found++;
        }
        Pattern chests = Pattern.compile("addLootChest\\(array,\\s*x(?:\\+(\\d+))?,\\s*y(?:\\+(\\d+))?,\\s*z(?:\\+(\\d+))?,\\s*ForgeDirection\\.([A-Z]+)\\)");
        matcher = chests.matcher(java);
        while (matcher.find()) {
            data.set(groupInt(matcher, 1), groupInt(matcher, 2), groupInt(matcher, 3),
                    lootChest(matcher.group(4).toLowerCase(Locale.ROOT)));
            found++;
        }
        if (found != 388)
            throw new IllegalStateException("Expected 388 V33a cavern cells, parsed " + found + " from " + source);
        data.set(7, 2, 5, STRUCTURE_VOID);
        return data;
    }

    /**
     * Mechanical NBT import of V33a {@code PortalStructure}. Its 581 explicit placement calls are the
     * single geometry authority; nothing here is retyped by hand.
     *
     * <p>The authored volume is 15x10x15 anchored on the controller at template {@code (7,0,7)} —
     * V33a offsets by {@code i = x-7, j = y+0, k = z-7} and puts the 3x3 Portal Rift pad on the
     * bottom layer at {@code i+6..i+8, j+0, k+6..k+8}. The four Pylon Focus cells land at
     * {@code (+-3, +5, +-3)} from the anchor, which is exactly where {@code chargingParticles()}
     * probes for them, so the anchor is self-checking.
     *
     * <p>Deliberately NOT in the template: the eight vanilla Ender Crystals at {@code (+-5,+5,-+9)}
     * and {@code (+-9,+5,-+5)}, and the bedrock pads beneath them. Both are outside the 15x15
     * footprint, the bedrock is only placed by {@code isDisplay()} and is never a match requirement,
     * and entities cannot be a structure-template match condition. The block entity checks those
     * eight positions separately, exactly as V33a's {@code getEntities()} does.
     *
     * <p>Symbol contracts carried over from the source calls:
     * <ul>
     * <li>{@code shield, 0} — Shielding, metadata 0. V33a {@code BlockType.metadata = ordinal()+8},
     *     so bit 3 is the reinforced flag and bits 0-2 are the material: metadata 0 is a plain,
     *     player-placeable Cloak Shielding, not the unbreakable worldgen form.</li>
     * <li>{@code crystalstone, N} — the Nth {@link StoneTypes}. Beams additionally carry the 26.2
     *     axis property, which V33a expressed through its renderer rather than metadata; it is
     *     recovered here from each beam run's own direction.</li>
     * <li>{@code setFluid(er)} — V33a {@code FluidCheck(needSource=true)}: the cell must hold a
     *     Liquid Ender <em>source</em>, so it is written as the exact level-0 state.</li>
     * <li>{@code setBlock(ch)} — Luma through a metadata-wildcard {@code BlockKey}: any level.</li>
     * <li>{@code setBlock(ch, 1)} — Luma at legacy quanta 1. {@code BlockEtherealLuma} used
     *     {@code setQuantaPerBlock(16)} where 26.2's {@code LiquidBlock} has 8, so the exact legacy
     *     index has no equivalent; what the call means is "flowing, not a source", which is how
     *     {@link reika.chromaticraft.auxiliary.structure.PortalStructure} matches it.</li>
     * </ul>
     * Both Luma contracts are written with their natural placement state and are recognised by
     * position through the structure's own cell rule, so the wildcard survives in the template
     * instead of collapsing into a literal state comparison.
     */
    private static TemplateData importPortal() {
        Path source = legacyStructureSource("legacy/PortalStructure.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a portal source " + source, e);
        }
        TemplateData data = new TemplateData(15, 10, 15);
        Map<BlockPos, Integer> beams = new LinkedHashMap<>();
        int found = 0;

        Pattern blocks = Pattern.compile(
                "array\\.setBlock\\(i\\+(\\d+),\\s*j\\+(\\d+),\\s*k\\+(\\d+),\\s*(shield|crystalstone|ch|p)(?:,\\s*(\\d+))?\\);");
        Matcher matcher = blocks.matcher(java);
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String symbol = matcher.group(4);
            String rawMeta = matcher.group(5);
            int meta = rawMeta == null ? -1 : Integer.parseInt(rawMeta);
            switch (symbol) {
                case "shield" -> data.set(x, y, z, new StateDef(
                        "chromaticraft:" + ChromaShieldTypes.list[meta & 7].registryName(),
                        Map.of("reinforced", String.valueOf(meta >= 8))));
                case "crystalstone" -> {
                    StoneTypes type = StoneTypes.list[meta];
                    if (type.isBeam())
                        beams.put(new BlockPos(x, y, z), meta);
                    data.set(x, y, z, stone(type));
                }
                // V33a setBlock(ch) is a wildcard-metadata BlockKey and setBlock(ch, 1) is flowing
                // Luma; both are written in their natural placed state.
                case "ch" -> data.set(x, y, z, new StateDef("chromaticraft:luma",
                        Map.of("level", meta < 0 ? "0" : "1")));
                case "p" -> data.set(x, y, z, new StateDef("chromaticraft:portal_rift", Map.of()));
                default -> throw new IllegalStateException("Unhandled V33a portal symbol " + symbol);
            }
            found++;
        }

        Pattern fluids = Pattern.compile(
                "array\\.setFluid\\(i\\+(\\d+),\\s*j\\+(\\d+),\\s*k\\+(\\d+),\\s*er\\);");
        matcher = fluids.matcher(java);
        while (matcher.find()) {
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    new StateDef("chromaticraft:liquid_ender", Map.of("level", "0")));
            found++;
        }

        if (found != 581)
            throw new IllegalStateException("Expected 581 V33a portal cells, parsed " + found + " from " + source);

        // Each beam run is a straight line of one variant; take the axis from whichever neighbour
        // shares its variant. V33a stored no axis at all, so this recovers what its renderer drew.
        beams.forEach((pos, meta) -> {
            boolean alongX = meta.equals(beams.get(pos.east())) || meta.equals(beams.get(pos.west()));
            data.set(pos.getX(), pos.getY(), pos.getZ(), stone(StoneTypes.list[meta], alongX ? "x" : "z"));
        });
        return data;
    }

    private static int groupInt(Matcher matcher, int group) {
        String value = matcher.group(group);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private static TemplateData netherHut() {
        TemplateData data = new TemplateData(9, 6, 9);
        StateDef bedrock = new StateDef("minecraft:bedrock", Map.of());
        StateDef bricks = new StateDef("minecraft:nether_bricks", Map.of());
        StateDef tnt = new StateDef("minecraft:tnt", Map.of("unstable", "false"));
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                data.set(x, 0, z, bedrock);
                if (x > 0 && x < 8 && z > 0 && z < 8)
                    data.set(x, 1, z, bedrock);
            }
        }
        for (int i = 2; i <= 6; i++) {
            data.set(i, 0, 2, tnt); data.set(i, 0, 6, tnt);
            data.set(2, 0, i, tnt); data.set(6, 0, i, tnt);
        }
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                data.set(x, 1, z, bricks);
                if ((x != 2 && x != 6) || (z != 2 && z != 6))
                    data.set(x, 5, z, bricks); // V33a omits the four roof corners.
            }
        }
        for (int y = 2; y <= 4; y++) {
            data.set(2, y, 2, bricks); data.set(6, y, 2, bricks);
            data.set(2, y, 6, bricks); data.set(6, y, 6, bricks);
        }
        data.set(5, 0, 4, tnt); data.set(3, 0, 4, tnt);
        data.set(4, 0, 5, tnt); data.set(4, 0, 3, tnt);
        data.set(4, 0, 4, lootChest("north"));
        data.set(4, 1, 4, new StateDef("minecraft:lava", Map.of("level", "0")));
        return data;
    }

    /**
     * Imports the three source-authored V33a Nether-roof layouts whose Java files are literal
     * {@code world.setBlock} exports. Keeping this as a datagen-only importer avoids a second,
     * hand-maintained 17,000-cell coordinate list while still producing ordinary canonical vanilla
     * structure NBT for the shipped mod. The checked-in generated NBT is the runtime authority; the
     * old Java generator is never loaded by Minecraft.
     */
    private static TemplateData importExplicitNetherStructure(String fileName, int sizeX, int sizeY, int sizeZ) {
        Path source = legacySource(fileName);
        if (!Files.isRegularFile(source))
            throw new IllegalStateException("Missing V33a Nether structure source " + source.toAbsolutePath());
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Nether structure source " + source, e);
        }
        Pattern call = Pattern.compile("world\\.setBlock\\(i\\s*\\+\\s*(\\d+),\\s*j\\s*\\+\\s*(\\d+),\\s*k\\s*\\+\\s*(\\d+),\\s*([^,);]+)(?:,\\s*([^,);]+),\\s*[^);]+)?\\s*\\);");
        TemplateData data = new TemplateData(sizeX, sizeY, sizeZ);
        Matcher matcher = call.matcher(java);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            int metadata = parseLegacyInt(matcher.group(5));
            data.set(x, y, z, legacyNetherState(matcher.group(4).trim(), metadata));
            found++;
        }
        if (found == 0)
            throw new IllegalStateException("No V33a setBlock cells parsed from " + source);
        if (fileName.equals("NetherTemple.java")) {
            data.set(12, 0, 14, lootChest("north"));
            data.set(13, 0, 14, lootChest("north"));
        }
        else if (fileName.equals("NetherMaze.java")) {
            data.set(1, 1, 14, new StateDef("minecraft:spawner", Map.of()), spawnerNBT("minecraft:blaze", 24));
            data.set(7, 1, 1, new StateDef("minecraft:spawner", Map.of()), spawnerNBT("minecraft:zombified_piglin", 8));
            data.set(11, 1, 14, lootChest("west"));
        }
        else if (fileName.equals("NetherDiorama.java")) {
            data.set(30, 5, 17, new StateDef("minecraft:spawner", Map.of()), spawnerNBT("minecraft:blaze", 4));
            data.set(30, 5, 8, new StateDef("minecraft:spawner", Map.of()), spawnerNBT("minecraft:blaze", 4));
            data.set(14, 4, 6, new StateDef("minecraft:spawner", Map.of()), spawnerNBT("minecraft:ghast", 1));
            data.set(12, 1, 17, new StateDef("minecraft:spawner", Map.of()), spawnerNBT("minecraft:magma_cube", 4));
            data.set(27, 5, 19, lootChest("south"));
        }
        return data;
    }

    /** V33a's spiral has one authored maze layer repeated four times plus a generated cap. */
    private static TemplateData netherSpiral() {
        Path source = legacySource("NetherSpiral.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Nether spiral source " + source, e);
        }
        int method = java.indexOf("private static void generateLayer");
        if (method < 0)
            throw new IllegalStateException("Missing V33a NetherSpiral.generateLayer");
        String layerSource = java.substring(method);
        Pattern call = Pattern.compile("world\\.setBlock\\(i\\s*\\+\\s*(\\d+),\\s*j\\s*\\+\\s*y,\\s*k\\s*\\+\\s*(\\d+),\\s*([^,);]+)(?:,\\s*([^,);]+),\\s*[^);]+)?\\s*\\);");
        TemplateData data = new TemplateData(19, 9, 19);
        Matcher matcher = call.matcher(layerSource);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int z = Integer.parseInt(matcher.group(2));
            StateDef state = legacyNetherState(matcher.group(3).trim(), parseLegacyInt(matcher.group(4)));
            for (int y = 1; y <= 4; y++)
                data.set(x, y, z, state);
            found++;
        }
        if (found == 0)
            throw new IllegalStateException("No V33a spiral layer cells parsed from " + source);

        StateDef shield = new StateDef("chromaticraft:shielding_stone", Map.of("reinforced", "false"));
        for (int x = 0; x <= 18; x++)
            for (int z = 0; z <= 18; z++)
                data.set(x, 0, z, shield);
        for (int i = 1; i <= 17; i++) {
            data.set(i, 4, 1, shield); data.set(i, 4, 17, shield);
            data.set(1, 4, i, shield); data.set(17, 4, i, shield);
        }
        for (int i = 2; i <= 16; i++) {
            data.set(i, 5, 2, shield); data.set(i, 5, 16, shield);
            data.set(2, 5, i, shield); data.set(16, 5, i, shield);
        }
        StateDef netherrack = new StateDef("minecraft:netherrack", Map.of());
        StateDef lava = new StateDef("minecraft:lava", Map.of("level", "0"));
        for (int x = 3; x <= 15; x++) {
            for (int z = 3; z <= 15; z++) {
                // This preserves the original's always-true `i != 3 || i != 15 || ...` condition.
                data.set(x, 5, z, netherrack);
                data.set(x, 6, z, netherrack);
                data.set(x, 7, z, lava);
                data.set(x, 8, z, shield);
            }
        }
        for (int i = 4; i <= 14; i++) {
            for (int y = 6; y <= 7; y++) {
                data.set(i, y, 2, shield); data.set(i, y, 16, shield);
                data.set(2, y, i, shield); data.set(16, y, i, shield);
                data.set(3, y, 3, shield); data.set(15, y, 3, shield);
                data.set(3, y, 15, shield); data.set(15, y, 15, shield);
            }
        }
        data.set(9, 1, 9, lootChest("east"));
        data.set(1, 1, 5, lootChest("east"));
        data.set(5, 1, 17, lootChest("west"));
        data.set(13, 1, 1, lootChest("south"));
        data.set(17, 1, 13, lootChest("north"));
        return data;
    }

    /**
     * V33a's Proxima monument, from {@code MonumentStructure} plus the live half of
     * {@code MonumentHighlighter}.
     *
     * <p>The geometry is unusually simple for its size: three and a half thousand cells of exactly two
     * blocks, Stone Shielding and Cloak Shielding, which upstream writes as the locals {@code ms} and
     * {@code mc}. Nothing else is in it. What makes the monument the monument is the sixteen runes and
     * the controller that the highlighter adds on top, and those are added here rather than left to a
     * runtime pass so the template is the whole structure.
     *
     * <p>The rune ring is upstream's exactly: sixteen cells at y+11 laid clockwise from
     * {@code (3, 18)}, one per element in element order. Upstream writes them as one block with
     * metadata 0-15; here they are the sixteen distinct rune blocks they became.
     *
     * <p>The 43x13x43 size is measured from the calls rather than declared, so a cell added to the
     * legacy source cannot silently fall outside the template.
     */
    private static TemplateData importMonument() {
        Path source = legacyMonumentSource("MonumentStructure.java");
        final String java;
        try {
            java = Files.readString(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a monument source " + source, e);
        }
        // Parsed by hand rather than with the shared setBlock pattern, which requires a metadata
        // *and* an update-flags argument. The monument's calls carry only metadata, so that pattern
        // matches none of them -- and silently, since a regex that does not match simply finds
        // nothing at all. These calls are perfectly uniform, so splitting them is both simpler and
        // louder about a shape it does not recognise.
        TemplateData data = new TemplateData(43, 13, 43);
        int found = 0;
        for (String line : java.split("\n")) {
            int call = line.indexOf("world.setBlock(");
            if (call < 0)
                continue;
            int close = line.lastIndexOf(')');
            if (close < call)
                continue;
            String[] args = line.substring(call + "world.setBlock(".length(), close).split(",");
            // Upstream's monument writes one block, distinguished only by which metadata local it
            // names: `mc` is Cloak Shielding, `ms` is Stone.
            if (args.length != 5 || !"sh".equals(args[3].trim()))
                continue;
            data.set(offset(args[0]), offset(args[1]), offset(args[2]),
                    shielding("mc".equals(args[4].trim())
                            ? ChromaShieldTypes.CLOAK : ChromaShieldTypes.STONE));
            found++;
        }
        if (found == 0)
            throw new IllegalStateException("No V33a monument cells parsed from " + source);

        // MonumentHighlighter: the rune ring at y+11, clockwise from (3, 18), one per element.
        int[] runeRing = {
                3, 18, 7, 13, 13, 7, 18, 3, 24, 3, 29, 7, 35, 13, 39, 18,
                39, 24, 35, 29, 29, 35, 24, 39, 18, 39, 13, 35, 7, 29, 3, 24};
        for (int i = 0; i < runeRing.length; i += 2)
            data.set(runeRing[i], 11, runeRing[i + 1], rune(CrystalElement.elements[i / 2]));
        // MonumentHighlighter: the structure controller at the monument's own centre.
        data.set(21, 5, 21, new StateDef(
                ChromatiCraft.MODID + ":structure_controller", Map.of()));
        return data;
    }

    /** Parses one {@code i + 16} style argument of a legacy setBlock call. */
    private static int offset(String argument) {
        return Integer.parseInt(argument.substring(argument.indexOf('+') + 1).trim());
    }

    private static StateDef shielding(ChromaShieldTypes type) {
        // All dimension-structure source calls use BlockType metadata with bit 3 set by the splice
        // cache. The modern state must retain that protection bit explicitly.
        return new StateDef(ChromatiCraft.MODID + ":" + type.registryName(),
                Map.of("reinforced", "true"));
    }

    /** V33a {@code addBreakable}: the same shielding material without the structure-protection bit. */
    private static StateDef breakableShielding(ChromaShieldTypes type) {
        return new StateDef(ChromatiCraft.MODID + ":" + type.registryName(),
                Map.of("reinforced", "false"));
    }

    /**
     * Converts the source-authored V33a Glowing Logic entrance components directly to NBT. The old
     * files are deliberately retained as the geometry authority: their very large generated
     * {@code setBlock} lists are split across generate2/3/etc, but each continuation is called by the
     * preceding method and parsing the file in order reproduces the final palette exactly.
     */
    private static TemplateData importLightPanelStair(String fileName, int sizeX, int sizeY,
            int sizeZ, boolean bottom) {
        Path source = legacyLightPanelSource(fileName);
        final String java;
        try { java = Files.readString(source); }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Glowing Logic entrance " + source, e);
        }
        TemplateData data = new TemplateData(sizeX, sizeY, sizeZ);
        Pattern placement = Pattern.compile("world\\.setBlock\\(x \\+ (\\d+), y \\+ (\\d+), z \\+ (\\d+), "
                + "(Blocks\\.(?:air|water)|b)(?:, (m[csml]), 3)?\\);");
        Matcher matcher = placement.matcher(java);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String block = matcher.group(4);
            String metadata = matcher.group(5);
            StateDef state = switch (block) {
                case "Blocks.air" -> AIR;
                case "Blocks.water" -> new StateDef("minecraft:water", Map.of("level", "0"));
                default -> shielding(switch (metadata) {
                    case "mc" -> ChromaShieldTypes.CLOAK;
                    case "ml" -> ChromaShieldTypes.LIGHT;
                    case "mm" -> ChromaShieldTypes.MOSS;
                    default -> ChromaShieldTypes.STONE;
                });
            };
            data.set(x, y, z, state);
            found++;
        }
        if (found < 1000)
            throw new IllegalStateException("Parsed only " + found + " Glowing Logic stair cells from " + source);
        // The only non-constant placement in the authored components is the bottom's 5x5 water pad.
        if (bottom)
            for (int x = 6; x <= 10; x++) for (int z = 6; z <= 10; z++)
                data.set(x, 1, z, new StateDef("minecraft:water", Map.of("level", "0")));
        return data;
    }

    /** Exact V33a LightPanelRoom shell, internal wall, lamps, switches, door, taper and chests. */
    private static TemplateData lightPanelRoomTemplate(int tier) {
        int switches = reika.chromaticraft.world.dimension.structure.lightpanel.LightPanelPatternLibrary
                .switchCount(tier);
        int rows = reika.chromaticraft.world.dimension.structure.lightpanel.LightPanelPatternLibrary
                .rowCount(tier);
        int radius = switches + 2;
        int height = rows + 7;
        TemplateData data = new TemplateData(19, height + 1, radius * 2 + 1);
        StateDef stone = shielding(ChromaShieldTypes.STONE);
        StateDef cloak = shielding(ChromaShieldTypes.CLOAK);
        StateDef glass = shielding(ChromaShieldTypes.GLASS);
        StateDef light = shielding(ChromaShieldTypes.LIGHT);
        int centerZ = radius;

        for (int x = 0; x <= 18; x++) for (int z = 0; z <= radius * 2; z++)
            for (int y = 0; y <= height; y++)
                data.set(x, y, z, x == 0 || x == 18 || z == 0 || z == radius * 2
                        || y == 0 || y == height ? stone : AIR);

        int panelX = 12;
        for (int z = 0; z <= radius * 2; z++) for (int y = 1; y < height; y++)
            data.set(panelX, y, z, stone);
        String[] types = {"target", "block", "cancel"};
        for (int row = 0; row < rows; row++) {
            int y = 6 + row;
            for (int type = 0; type < types.length; type++) {
                int z = centerZ - 2 + type * 2;
                data.set(panelX, y, z, new StateDef("chromaticraft:light_panel",
                        Map.of("type", types[type], "active", "false")));
                data.set(panelX, y, z + 1, cloak);
            }
        }
        for (int row = -1; row <= rows; row++) {
            int y = 6 + row;
            for (int dz : new int[] {-3, -1, 1, 3}) data.set(panelX, y, centerZ + dz, cloak);
            data.set(panelX, y, centerZ - 4, stone);
            data.set(panelX, y, centerZ + 4, stone);
        }
        for (int z = 0; z <= radius * 2; z++) {
            for (int y = 1; y <= 4; y++) data.set(panelX, y, z, stone);
            int dz = z - centerZ;
            if (Math.abs(dz) < 5) {
                StateDef cap = Math.abs(dz) == 4 ? stone : cloak;
                data.set(panelX, 5, z, cap);
                data.set(panelX, rows + 6, z, cap);
            }
        }
        StateDef door = new StateDef("chromaticraft:chroma_door",
                Map.of("open", "false", "damage", "false", "consume_key", "false",
                        "stay_open", "true", "up", "false", "down", "false",
                        "north", "false", "south", "false", "east", "false", "west", "false"));
        for (int dz = -2; dz <= 2; dz++) for (int y = 1; y <= 3; y++)
            data.set(panelX, y, centerZ + dz, door);

        int switchX = 6;
        for (int channel = 0; channel < switches; channel++) {
            int z = centerZ - switches + channel * 2 + 1;
            data.set(switchX, 1, z, stone);
            data.set(switchX, 2, z, new StateDef("chromaticraft:panel_switch", Map.of("up", "false")));
            if (channel != switches - 1) {
                data.set(switchX, 1, z + 1, stone);
                data.set(switchX, 2, z + 1, glass);
            }
        }
		if (tier == 0)
			data.set(1, 1, centerZ, new StateDef("chromaticraft:structure_password", Map.of()));
        for (int dz = -2; dz <= 2; dz++) for (int y = 1; y <= 3; y++) {
            data.set(0, y, centerZ + dz, AIR);
            data.set(18, y, centerZ + dz, AIR);
        }
        data.set(0, 4, centerZ - 1, light); data.set(0, 4, centerZ + 1, light);
        data.set(0, 2, centerZ - 3, light); data.set(0, 2, centerZ + 3, light);
        for (int z = 0; z <= radius * 2; z++) for (int x = 13; x <= 18; x++) {
            int edge = Math.abs(z - centerZ);
            int roof = edge >= radius - 1 ? 3 : edge >= radius - 3 ? 4 : 5;
            data.set(x, roof + 1, z, stone);
        }
        data.set(15, 1, 1, lootChest("south"), vanillaLootTableNBT("chromaticraft:chests/light_panel_room"));
        data.set(15, 1, radius * 2 - 1, lootChest("north"),
                vanillaLootTableNBT("chromaticraft:chests/light_panel_room"));
        data.set(15, 0, 1, light); data.set(15, 0, radius * 2 - 1, light);
        return data;
    }

    /** Exact V33a LightPanelLoot, with the modern persistent controller hidden in its ceiling. */
    private static TemplateData lightPanelLoot() {
        int radius = 10;
        int maxHeight = 21;
        TemplateData data = new TemplateData(21, 22, 21);
        StateDef stone = shielding(ChromaShieldTypes.STONE);
        StateDef cloak = shielding(ChromaShieldTypes.CLOAK);
		StateDef breakableStone = breakableShielding(ChromaShieldTypes.STONE);
		StateDef breakableLight = breakableShielding(ChromaShieldTypes.LIGHT);
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            boolean wall = Math.abs(x) == radius || Math.abs(z) == radius;
            int h = Math.max(0, radius - (Math.abs(x) + Math.abs(z)) / 2
                    - (Math.abs(x) <= 1 || Math.abs(z) <= 1 ? 6 : 5));
            for (int y = 0; y <= maxHeight; y++) {
                StateDef state = !wall && y > h && y < h + 6 ? AIR
                        : y > h && y < maxHeight && !wall ? cloak : stone;
				// LightPanelLoot calls addBreakable for every y=0 cell after placing it. In 26.2
				// that is the plain state of the same material, not a process-local generator set.
				if (y == 0) state = breakableStone;
                data.set(x + radius, y, z + radius, state);
            }
        }
        StateDef light = shielding(ChromaShieldTypes.LIGHT);
		data.set(18, 0, 18, breakableLight); data.set(18, 0, 2, breakableLight);
        data.set(10, 6, 10, new StateDef("chromaticraft:dimension_core_white", Map.of()));
        // Structure state must survive generator-object recreation; the ceiling keeps the controller
        // protected without changing the traversable authored room.
        data.set(10, 21, 10, new StateDef("chromaticraft:structure_controller", Map.of()));
        for (int dz = -2; dz <= 2; dz++) for (int y = 1; y <= 3; y++)
            data.set(0, y, 10 + dz, AIR);
        data.set(0, 4, 9, light); data.set(0, 4, 11, light);
        data.set(0, 2, 7, light); data.set(0, 2, 13, light);
        return data;
    }

    /** Canonical sealed 4-block maze cell. Its six seeded openings are carved after NBT placement. */
    private static TemplateData threeDMazeCell() {
        TemplateData data = new TemplateData(5, 5, 5);
        StateDef stone = shielding(ChromaShieldTypes.STONE);
        for (int x = 0; x <= 4; x++) for (int y = 0; y <= 4; y++) for (int z = 0; z <= 4; z++)
            data.set(x, y, z, x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4 ? stone : AIR);
        return data;
    }

    /** One repeatable layer of V33a's 5x5 entrance shaft. */
    private static TemplateData threeDMazeShaftSlice() {
        TemplateData data = new TemplateData(5, 1, 5);
        StateDef stone = shielding(ChromaShieldTypes.STONE);
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++)
            data.set(x, 0, z, x == 0 || x == 4 || z == 0 || z == 4 ? stone : AIR);
        return data;
    }

    /**
     * Mechanical import of V33a {@code TDMazeEntrance}'s authored 13x6 pavilion. The terrain-height
     * shaft and pad remain repetition/composition concerns in the structure piece.
     */
    private static TemplateData importThreeDMazeEntrance() {
        String java = readLegacyThreeDMaze("TDMazeEntrance.java");
        TemplateData data = new TemplateData(13, 6, 13);
        for (int x = 0; x <= 12; x++) for (int y = 0; y <= 5; y++) for (int z = 0; z <= 12; z++)
            data.set(x, y, z, AIR);
        Pattern placement = Pattern.compile("world\\.setBlock\\(x\\+(\\d+), y\\+(\\d+), z\\+(\\d+), "
                + "(sh|ps)(?:, (m[ls]|\\d+), 3)?\\);");
        Matcher matcher = placement.matcher(java);
        int found = 0;
        while (matcher.find()) {
            int metadata = parseThreeDMazeMetadata(matcher.group(5));
            StateDef state = "sh".equals(matcher.group(4))
                    ? shielding(metadata == 6 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE)
                    : crystallineStone(metadata);
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), state);
            found++;
        }
        if (found < 150)
            throw new IllegalStateException("Parsed only " + found + " Three-Dimensional Maze entrance cells");
        return data;
    }

    /** Exact authored V33a reward chamber, shifted three cells upward so an NBT origin stays positive. */
    private static TemplateData importThreeDMazeLoot() {
        String java = readLegacyThreeDMaze("LootRoom.java");
        TemplateData data = new TemplateData(11, 9, 11);
        StateDef stone = shielding(ChromaShieldTypes.STONE);
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 10; z++) for (int y = 0; y <= 2; y++)
            data.set(x, y, z, stone);
        Pattern placement = Pattern.compile("world\\.setBlock\\(x\\+(\\d+), y\\+(\\d+), z\\+(\\d+), "
                + "(sh|Blocks\\.air)(?:, (m[slg]))?[^;]*\\);");
        Matcher matcher = placement.matcher(java);
        int found = 0;
        while (matcher.find()) {
            StateDef state = switch (matcher.group(4)) {
                case "Blocks.air" -> AIR;
                default -> shielding(switch (matcher.group(5)) {
                    case "ml" -> ChromaShieldTypes.LIGHT;
                    case "mg" -> ChromaShieldTypes.GLASS;
                    default -> ChromaShieldTypes.STONE;
                });
            };
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)) + 3,
                    Integer.parseInt(matcher.group(3)), state);
            found++;
        }
        if (found < 700)
            throw new IllegalStateException("Parsed only " + found + " Three-Dimensional Maze reward cells");
        data.set(5, 6, 5, shielding(ChromaShieldTypes.GLASS));
        // V33a explicitly marks these two perimeter layers as breakable unless the upper cell is light.
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 10; z++) {
            if (x != 0 && x != 10 && z != 0 && z != 10) continue;
            if (!data.is( x, 5, z, shielding(ChromaShieldTypes.LIGHT))) {
                data.makePlainShield(x, 4, z);
                data.makePlainShield(x, 5, z);
            }
        }
        return data;
    }

    private static String readLegacyThreeDMaze(String fileName) {
        Path source = legacyThreeDMazeSource(fileName);
        try { return Files.readString(source); }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Three-Dimensional Maze source " + source, e);
        }
    }

    private static int parseThreeDMazeMetadata(String value) {
        if (value == null || value.equals("ms")) return 1;
        if (value.equals("ml")) return 6;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Unknown maze metadata " + value, e); }
    }

    private static StateDef crystallineStone(int metadata) {
        BlockCrystallineStone.StoneTypes[] types = BlockCrystallineStone.StoneTypes.list;
        int index = Math.floorMod(metadata, types.length);
        return new StateDef(ChromatiCraft.MODID + ":" +
                reika.chromaticraft.registry.ChromaBlocks.crystallineStoneName(types[index]), Map.of());
    }

    private static CompoundTag vanillaLootTableNBT(String table) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "chromaticraft:loot_chest");
        nbt.putString("LootTable", table);
        return nbt;
    }

    private static Path legacyLightPanelSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/dimension/structure/lightpanel", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct)) return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module)) return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private static Path legacyThreeDMazeSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/dimension/structure/tdmaze", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct)) return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module)) return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    /**
     * Complete fixed underground chamber for one V33a Cellular Automata difficulty. The board is
     * deliberately a full NBT component rather than thousands of runtime-authored cell columns:
     * structure placement can clip one canonical 27/35/51-wide template to each generating chunk.
     */
    private static TemplateData golChamberTemplate(int difficulty) {
        reika.chromaticraft.world.dimension.structure.GOLPuzzleLayout layout =
                reika.chromaticraft.world.dimension.structure.GOLPuzzleLayout.forDifficulty(difficulty);
        int radius = layout.radius();
        int boardWidth = layout.width();
        int size = boardWidth + 2;
        TemplateData data = new TemplateData(size, 15, size);
        StateDef floor = new StateDef("chromaticraft:gol_tile",
                Map.of("active", "false", "memory", "false"));
        StateDef memory = new StateDef("chromaticraft:gol_tile",
                Map.of("active", "false", "memory", "true"));
        StateDef cloak = shielding(ChromaShieldTypes.CLOAK);
        StateDef light = shielding(ChromaShieldTypes.LIGHT);

        for (int x = 1; x <= boardWidth; x++) for (int z = 1; z <= boardWidth; z++) {
            data.set(x, 0, z, cloak);
            data.set(x, 1, z, floor);
            for (int y = 2; y <= 12; y++) data.set(x, y, z, AIR);
            data.set(x, 13, z, memory);
            data.set(x, 14, z, cloak);
        }
        for (int y = 0; y < 15; y++) {
            int relativeY = y - 1; // V33a k=-1..ROOM_HEIGHT+1
            for (int relative = -radius - 1; relative <= radius + 1; relative++) {
                int along = relative + radius + 1;
                StateDef wall = Math.abs(relativeY % 8) == Math.abs(relative % 8) ? light : cloak;
                data.set(along, y, 0, wall);
                data.set(along, y, size - 1, wall);
                data.set(0, y, along, wall);
                data.set(size - 1, y, along, wall);
            }
        }
        return data;
    }

    /** Exact fixed surface pavilion from the loop-authored half of V33a {@code GOLEntrance}. */
    private static TemplateData golSurface() {
        TemplateData data = new TemplateData(13, 7, 13);
        StateDef stone = shielding(ChromaShieldTypes.STONE);
        StateDef moss = shielding(ChromaShieldTypes.MOSS);
        StateDef light = shielding(ChromaShieldTypes.LIGHT);
        for (int x = 0; x < 13; x++) for (int z = 0; z < 13; z++) {
            int dx = x - 6;
            int dz = z - 6;
            if (Math.abs(dx) > 2 || Math.abs(dz) > 2)
                data.set(x, 0, z, Math.abs(dx) == 6 || Math.abs(dz) == 6 ? moss : stone);
            for (int y = 1; y <= 6; y++) data.set(x, y, z, AIR);
        }
        for (int offset = 3; offset < 6; offset++) {
            data.set(6 - offset, 0, 6, moss); data.set(6 + offset, 0, 6, moss);
            data.set(6, 0, 6 - offset, moss); data.set(6, 0, 6 + offset, moss);
        }
        for (int[] point : new int[][] {{0,6},{12,6},{6,0},{6,12},{0,0},{12,0},{0,12},{12,12}})
            for (int y = 1; y <= 6; y++) data.set(point[0], y, point[1], y == 6 ? light : stone);
        return data;
    }

    /** Mechanical import of one explicit V33a GOL component method. */
    private static TemplateData importGOLPlacementMethod(String fileName, String methodName,
            int sizeX, int sizeY, int sizeZ, GOLTemplateKind kind, int expectedPlacements) {
        Path source = legacyGOLSource(fileName);
        final String java;
        try { java = Files.readString(source); }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Cellular Automata source " + source, e);
        }
        int method = java.indexOf("void " + methodName);
        if (method < 0) throw new IllegalStateException("Missing " + methodName + " in " + source);
        String body = extractJavaBlock(java, java.indexOf('{', method));
        TemplateData data = new TemplateData(sizeX, sizeY, sizeZ);
        Pattern placement = Pattern.compile("world\\.setBlock\\(x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*([^;]+)\\);");
        Matcher matcher = placement.matcher(body);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            String value = matcher.group(4).trim();
            StateDef state = value.startsWith("Blocks.air") ? AIR
                    : value.contains("ml") ? shielding(ChromaShieldTypes.LIGHT)
                    : shielding(kind == GOLTemplateKind.STONE
                            ? ChromaShieldTypes.STONE : ChromaShieldTypes.CLOAK);
            data.set(x, y, z, state);
            found++;
        }
        if (kind == GOLTemplateKind.EXIT) {
            matcher = Pattern.compile("this\\.placeGate\\(world,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+)\\)")
                    .matcher(body);
            while (matcher.find()) {
                data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)),
                        new StateDef("chromaticraft:chroma_door", Map.of()));
                found++;
            }
        }
        if (found != expectedPlacements)
            throw new IllegalStateException("Expected " + expectedPlacements + " " + methodName
                    + " placements, parsed " + found + " from " + source);
        return data;
    }

    /** Exact V33a reward shell; breakable shielding is retained as non-reinforced NBT state. */
    private static TemplateData importGOLLoot() {
        Path source = legacyGOLSource("GOLLoot.java");
        final String java;
        try { java = Files.readString(source); }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Cellular Automata loot source " + source, e);
        }
        int method = java.indexOf("void generate");
        String body = extractJavaBlock(java, java.indexOf('{', method));
        TemplateData data = new TemplateData(10, 7, 7);
        Pattern placement = Pattern.compile("world\\.setBlock\\(x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*([^;]+)\\);");
        Matcher matcher = placement.matcher(body);
        int found = 0;
        while (matcher.find()) {
            String value = matcher.group(4).trim();
            StateDef state = value.startsWith("Blocks.air") ? AIR
                    : value.contains("mg") ? shielding(ChromaShieldTypes.GLASS)
                    : value.contains("ml") ? shielding(ChromaShieldTypes.LIGHT)
                    : shielding(ChromaShieldTypes.STONE);
            data.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), state);
            found++;
        }
        if (found != 358)
            throw new IllegalStateException("Expected 358 GOLLoot placements, parsed " + found);
        matcher = Pattern.compile("this\\.addBreakable\\(world,\\s*x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+)\\)")
                .matcher(body);
        int breakable = 0;
        while (matcher.find()) {
            data.makePlainShield(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
            breakable++;
        }
		if (breakable != 51)
			throw new IllegalStateException("Expected 51 GOLLoot breakable cells, parsed " + breakable);
        data.set(6, 3, 3, STRUCTURE_VOID);
        return data;
    }

    private static Path legacyGOLSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/dimension/structure/gol", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct)) return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module)) return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private enum GOLTemplateKind { STONE, CLOAK, EXIT }

    /** Exact V33a surface pavilion for Crystal Music; the terrain shaft remains piece composition. */
    private static TemplateData importMusicFunnel() {
        return importMusicTemplate("MusicFunnel.java", 15, 6, 15, MusicTemplateKind.FUNNEL, 850);
    }

    /** Exact V33a room shell and its sixteen independently registered coloured lamp blocks. */
    private static TemplateData importMusicRoom() {
        return importMusicTemplate("MusicPuzzleBlocks.java", 11, 7, 20, MusicTemplateKind.ROOM, 950);
    }

    /** Exact V33a Crystal Music reward room; the core is bound at runtime by the structure piece. */
    private static TemplateData importMusicLoot() {
        TemplateData data = importMusicTemplate("MusicLoot.java", 7, 6, 8, MusicTemplateKind.LOOT, 300);
        for (int x : new int[] {0, 6}) for (int y : new int[] {1, 2})
            for (int z : new int[] {4, 5}) data.makePlainShield(x, y, z);
        return data;
    }

    private static TemplateData importMusicTemplate(String fileName, int sizeX, int sizeY, int sizeZ,
            MusicTemplateKind kind, int minimumPlacements) {
        Path source = legacyMusicSource(fileName);
        final String java;
        try { java = Files.readString(source); }
        catch (IOException e) {
            throw new IllegalStateException("Could not read V33a Crystal Music source " + source, e);
        }
        TemplateData data = new TemplateData(sizeX, sizeY, sizeZ);
        for (int x = 0; x < sizeX; x++) for (int y = 0; y < sizeY; y++)
            for (int z = 0; z < sizeZ; z++) data.set(x, y, z, AIR);
        Pattern placement = Pattern.compile("world\\.setBlock\\(x\\+(\\d+),\\s*y\\+(\\d+),\\s*z\\+(\\d+),\\s*"
                + "(Blocks\\.air|sh|cry|b)(?:,\\s*([^,\\)]+))?[^;]*\\);");
        Matcher matcher = placement.matcher(java);
        int found = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            if (x >= sizeX || y >= sizeY || z >= sizeZ)
                throw new IllegalStateException(fileName + " wrote outside declared template at "
                        + x + "," + y + "," + z);
            data.set(x, y, z, musicState(kind, matcher.group(4), matcher.group(5)));
            found++;
        }
        if (found < minimumPlacements)
            throw new IllegalStateException("Parsed only " + found + " Crystal Music cells from " + fileName);
        return data;
    }

    private static StateDef musicState(MusicTemplateKind kind, String block, String metadata) {
        if (block.equals("Blocks.air")) return AIR;
        String value = metadata == null ? "" : metadata.trim();
        if (block.equals("cry")) {
            int ordinal;
            try { ordinal = Integer.parseInt(value); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unknown Crystal Music lamp metadata " + value, e);
            }
            CrystalElement element = CrystalElement.elements[Math.floorMod(ordinal, CrystalElement.elements.length)];
            return new StateDef(ChromatiCraft.MODID + ":crystal_lamp_" + element.getEnglishName(), Map.of());
        }
        ChromaShieldTypes type = switch (value) {
            case "BlockType.LIGHT.metadata" -> ChromaShieldTypes.LIGHT;
            case "BlockType.GLASS.metadata" -> ChromaShieldTypes.GLASS;
            case "m1" -> ChromaShieldTypes.CLOAK;
            case "m2", "m", "meta", "" -> ChromaShieldTypes.STONE;
            default -> throw new IllegalArgumentException("Unknown " + kind + " shield metadata " + value);
        };
        return shielding(type);
    }

    private static Path legacyMusicSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/dimension/structure/music", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct)) return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module)) return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private enum MusicTemplateKind { FUNNEL, ROOM, LOOT }

    private static StateDef rune(CrystalElement element) {
        return new StateDef(ChromatiCraft.MODID + ":crystal_rune_" + element.getEnglishName().toLowerCase(java.util.Locale.ROOT), Map.of());
    }

    private static Path legacyMonumentSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/dimension/structure/monument", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct))
                return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module))
                return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private static Path legacySource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/nether", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct))
                return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module))
                return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private static Path legacyStructureSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/auxiliary/structure", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct))
                return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module))
                return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private static Path legacyWorldgenSource(String fileName) {
        Path relative = Path.of("src/main/java/reika/chromaticraft/auxiliary/structure/worldgen", fileName);
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct))
                return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module))
                return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private static Path legacyVillageSource() {
        Path relative = Path.of("src/main/java/reika/chromaticraft/world/VillagersFailChromatiCraft.java");
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && cursor != null; depth++, cursor = cursor.getParent()) {
            Path direct = cursor.resolve(relative);
            if (Files.isRegularFile(direct)) return direct;
            Path module = cursor.resolve("ChromatiCraft").resolve(relative);
            if (Files.isRegularFile(module)) return module;
        }
        return Path.of("").toAbsolutePath().resolve(relative);
    }

    private static int parseLegacyInt(String value) {
        if (value == null)
            return 0;
        value = value.trim();
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored) {
            return value.equals("mc") ? 1 : 0;
        }
    }

    private static StateDef lootChest(String facing) {
        return new StateDef("chromaticraft:loot_chest", Map.of("facing", facing));
    }

    private static StateDef vanillaChest(String facing) {
        return new StateDef("minecraft:chest", Map.of(
                "facing", facing, "type", "single", "waterlogged", "false"));
    }

    private static CompoundTag spawnerNBT(String entity, int maxNearby) {
        return spawnerNBT(entity, maxNearby, 12, 0,
                entity.equals("minecraft:zombified_piglin") ? 2
                        : entity.equals("minecraft:blaze") && maxNearby > 8 ? 20 : 1);
    }

    private static CompoundTag spawnerNBT(String entity, int maxNearby, int playerRange,
            int minimumDelay, int maximumDelay) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:mob_spawner");
        nbt.putShort("Delay", (short)1);
        nbt.putShort("MinSpawnDelay", (short)minimumDelay);
        nbt.putShort("MaxSpawnDelay", (short)maximumDelay);
        nbt.putShort("SpawnCount", (short)4);
        nbt.putShort("MaxNearbyEntities", (short)maxNearby);
        nbt.putShort("RequiredPlayerRange", (short)playerRange);
        nbt.putShort("SpawnRange", (short)4);
        CompoundTag spawnData = new CompoundTag();
        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", entity);
        spawnData.put("entity", entityData);
        nbt.put("SpawnData", spawnData);
        return nbt;
    }

    private static StateDef legacyNetherState(String symbol, int metadata) {
        return switch (symbol) {
            case "b" -> new StateDef(metadata == 1 ? "chromaticraft:shielding_cracks" : "chromaticraft:shielding_stone",
                    Map.of("reinforced", "false"));
            case "bd", "Blocks.bedrock" -> new StateDef("minecraft:bedrock", Map.of());
            case "Blocks.air" -> AIR;
            case "Blocks.nether_brick" -> new StateDef("minecraft:nether_bricks", Map.of());
            case "Blocks.nether_brick_fence" -> new StateDef("minecraft:nether_brick_fence", Map.of(
                    "east", "false", "north", "false", "south", "false", "waterlogged", "false", "west", "false"));
            case "Blocks.netherrack" -> new StateDef("minecraft:netherrack", Map.of());
            case "Blocks.gravel" -> new StateDef("minecraft:gravel", Map.of());
            case "Blocks.glowstone" -> new StateDef("minecraft:glowstone", Map.of());
            case "Blocks.soul_sand" -> new StateDef("minecraft:soul_sand", Map.of());
            case "Blocks.flowing_lava", "Blocks.lava" -> new StateDef("minecraft:lava", Map.of("level", Integer.toString(metadata & 15)));
            case "Blocks.nether_wart" -> new StateDef("minecraft:nether_wart", Map.of("age", Integer.toString(Math.min(3, metadata))));
            case "Blocks.obsidian" -> new StateDef("minecraft:obsidian", Map.of());
            case "Blocks.redstone_block" -> new StateDef("minecraft:redstone_block", Map.of());
            case "Blocks.brewing_stand" -> new StateDef("minecraft:brewing_stand", Map.of(
                    "has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "false"));
            case "Blocks.fire" -> new StateDef("minecraft:fire", Map.of("age", Integer.toString(Math.min(15, metadata)),
                    "east", "false", "north", "false", "south", "false", "up", "false", "west", "false"));
            case "Blocks.tnt" -> new StateDef("minecraft:tnt", Map.of("unstable", "false"));
            case "Blocks.stone_pressure_plate" -> new StateDef("minecraft:stone_pressure_plate", Map.of("powered", "false"));
            // Authored as a cross, not a dot, and that is load-bearing rather than cosmetic. 1.7.10 had
            // no dot shape at all: a wire with no wire neighbours rendered as a cross and powered every
            // block around it, which is what the temple's puzzle wiring relies on. 26.2 added the dot,
            // and RedStoneWireBlock.getConnectionState preserves one that already exists —
            // "if (wasDot && isDot(state)) return state;" short-circuits before the auto-connect logic.
            // A template that stored "none" on all four sides is therefore a dot that no placement,
            // neighbour update or shape-resolution pass will ever open up. Storing "side" makes wasDot
            // false, so vanilla runs its normal resolution and each wire settles into the line or cross
            // its neighbours call for.
            case "Blocks.redstone_wire" -> new StateDef("minecraft:redstone_wire", Map.of("east", "side", "north", "side",
                    "power", Integer.toString(metadata & 15), "south", "side", "west", "side"));
            case "Blocks.portal" -> new StateDef("minecraft:nether_portal", Map.of("axis", metadata == 2 ? "z" : "x"));
            case "Blocks.quartz_stairs" -> legacyStairs(metadata);
            case "Blocks.sticky_piston" -> legacyPiston(metadata, true, false);
            case "Blocks.piston_head" -> legacyPiston(metadata, true, true);
            case "Blocks.redstone_torch", "Blocks.unlit_redstone_torch" -> legacyTorch(metadata, symbol.equals("Blocks.redstone_torch"));
            case "Blocks.unpowered_repeater", "Blocks.powered_repeater" -> legacyRepeater(metadata, symbol.equals("Blocks.powered_repeater"));
            default -> throw new IllegalStateException("Unmapped V33a Nether structure block " + symbol + " metadata " + metadata);
        };
    }

    private static StateDef legacyStairs(int metadata) {
        String[] facings = {"east", "west", "south", "north"};
        return new StateDef("minecraft:quartz_stairs", Map.of("facing", facings[metadata & 3],
                "half", (metadata & 4) != 0 ? "top" : "bottom", "shape", "straight", "waterlogged", "false"));
    }

    private static String legacyDirection(int metadata) {
        return switch (metadata & 7) {
            case 0 -> "down";
            case 1 -> "up";
            case 2 -> "north";
            case 3 -> "south";
            case 4 -> "west";
            default -> "east";
        };
    }

    private static StateDef legacyPiston(int metadata, boolean sticky, boolean head) {
        String facing = legacyDirection(metadata);
        return head
                ? new StateDef("minecraft:piston_head", Map.of("facing", facing, "short", "false", "type", sticky ? "sticky" : "normal"))
                : new StateDef(sticky ? "minecraft:sticky_piston" : "minecraft:piston",
                        Map.of("extended", Boolean.toString((metadata & 8) != 0), "facing", facing));
    }

    private static StateDef legacyTorch(int metadata, boolean lit) {
        String id = "minecraft:redstone";
        if (metadata == 5 || metadata == 0)
            return new StateDef(id + "_torch", Map.of("lit", Boolean.toString(lit)));
        String[] facings = {"east", "west", "south", "north"};
        return new StateDef(id + "_wall_torch", Map.of("facing", facings[Math.max(1, metadata) - 1], "lit", Boolean.toString(lit)));
    }

    private static StateDef legacyRepeater(int metadata, boolean powered) {
        String[] facings = {"south", "west", "north", "east"};
        return new StateDef("minecraft:repeater", Map.of("delay", Integer.toString(((metadata >> 2) & 3) + 1),
                "facing", facings[metadata & 3], "locked", "false", "powered", Boolean.toString(powered)));
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

	/** V33a DataTowerStructure geometry; worldgen randomizes the stone/moss shield palette. */
	private static TemplateData dataNode() {
		TemplateData data = new TemplateData(3, 6, 3);
		StateDef shield = new StateDef("chromaticraft:shielding_stone", Map.of("reinforced", "true"));
		for (int x = 0; x < 3; x++)
			for (int z = 0; z < 3; z++)
				data.set(x, 0, z, shield);
		data.set(0, 1, 1, shield);
		data.set(2, 1, 1, shield);
		data.set(1, 1, 0, shield);
		data.set(1, 1, 2, shield);
		data.set(1, 1, 1, new StateDef("chromaticraft:data_node", Map.of()));
		for (int y = 2; y <= 5; y++)
			data.set(1, y, 1, new StateDef("chromaticraft:dummy_aux", Map.of()));
		return data;
	}

    /**
     * Exact V33a {@code RainbowTreeBlueprint} geometry, stored as a vanilla structure template.
     * Coordinates are byte-packed triples transcribed mechanically from all 1,020 source calls;
     * Y is shifted up one because the original four root blocks extend one cell below its blueprint
     * origin. Oak logs are only palette placeholders -- runtime replaces the entire tree with one
     * randomly selected natural overworld log while preserving each authored log axis.
     */
    private static TemplateData rainbowTree() {
        TemplateData data = new TemplateData(10, 32, 10);
        decodeTreeCells(data, TREE_LEAVES,
                new StateDef("chromaticraft:rainbow_leaves", Map.of("persistent", "false", "distance", "7")));
        decodeTreeCells(data, TREE_LOG_Y, new StateDef("minecraft:oak_log", Map.of("axis", "y")));
        decodeTreeCells(data, TREE_LOG_X, new StateDef("minecraft:oak_log", Map.of("axis", "x")));
        decodeTreeCells(data, TREE_LOG_Z, new StateDef("minecraft:oak_log", Map.of("axis", "z")));
        return data;
    }

    private static void decodeTreeCells(TemplateData data, String encoded, StateDef state) {
        byte[] coordinates = java.util.Base64.getDecoder().decode(encoded);
        if (coordinates.length % 3 != 0)
            throw new IllegalStateException("Malformed packed rainbow-tree blueprint");
        for (int i = 0; i < coordinates.length; i += 3)
            data.set(Byte.toUnsignedInt(coordinates[i]), Byte.toUnsignedInt(coordinates[i + 1]),
                    Byte.toUnsignedInt(coordinates[i + 2]), state);
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
        // Liquid chroma is now a real registered 26.2 block, so these are canonical NBT cells rather
        // than the old fail-closed structure-void placeholders. PylonBroadcastStructure still
        // replaces them with a registry-identity BlockCheck for exact matching/display semantics.
        data.set(x, y, z, new StateDef("chromaticraft:liquid_chroma", Map.of("level", "0")));
    }

    /** Exact V33a {@code InfusionStructure}; the machine is anchored at template (3,2,3). */
    private static TemplateData infusion() {
        TemplateData data = new TemplateData(7, 3, 7);
        int c = 3;
        setInfusionCircle(data, c, 1, 0.6, stone(StoneTypes.BRICKS));
        setInfusionCircle(data, c, 1, 2,
                new StateDef("chromaticraft:liquid_chroma", Map.of("level", "0")));
        setInfusionCircle(data, c, 0, 2, stone(StoneTypes.SMOOTH));
        setInfusionCircle(data, c, 1, 3.2, stone(StoneTypes.BRICKS));
        return data;
    }

	/** Exact V33a {@code PlayerInfusionStructure}; controller anchor is template (4,3,4). */
	private static TemplateData playerInfusion() {
		TemplateData data = new TemplateData(9, 4, 9);
		int c = 4;
		StateDef chroma = new StateDef("chromaticraft:liquid_chroma", Map.of("level", "0"));
		for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
			data.set(c + x, 0, c + z, stone(StoneTypes.SMOOTH));
			data.set(c + x, 1, c + z, chroma);
		}

		data.set(c, 1, c, stone(StoneTypes.COLUMN));
		data.set(c, 2, c, stone(StoneTypes.FOCUS));
		data.set(c, 3, c, new StateDef("chromaticraft:player_aura_infuser", Map.of()));
		for (int[] point : new int[][] {{2,0},{-2,0},{0,2},{0,-2}})
			data.set(c + point[0], 1, c + point[1], stone(StoneTypes.STABILIZER));

		for (int i = -4; i <= 4; i++) {
			if (i == 0) continue;
			StoneTypes type = Math.abs(i) <= 1 || Math.abs(i) == 4 ? StoneTypes.CORNER : StoneTypes.BEAM;
			data.set(c + i, 1, c + 4, stone(type, "x"));
			data.set(c + i, 1, c - 4, stone(type, "x"));
			data.set(c - 4, 1, c + i, stone(type, "z"));
			data.set(c + 4, 1, c + i, stone(type, "z"));
		}
		for (int i = -1; i <= 1; i++) {
			StoneTypes type = i == 0 ? StoneTypes.BRICKS : StoneTypes.CORNER;
			data.set(c + i, 1, c + 3, stone(type));
			data.set(c + i, 1, c - 3, stone(type));
			data.set(c - 3, 1, c + i, stone(type));
			data.set(c + 3, 1, c + i, stone(type));
		}
		return data;
	}

	private static TemplateData ritualAltar(boolean enhanced) {
		TemplateData data = new TemplateData(11, 5, 11);
		int c = 5;
		for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
			for (int y = 1; y <= 4; y++) data.set(c + x, y, c + z, SOFT_AIR);
		for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++)
			data.set(c + x, 0, c + z, SMOOTH);
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
			data.set(c + x, 1, c + z, SMOOTH);
		StoneTypes outer = enhanced ? StoneTypes.GLOWBEAM : StoneTypes.BEAM;
		for (int i = -4; i <= 4; i++) {
			data.set(c - 4, 1, c + i, stone(outer));
			data.set(c + 4, 1, c + i, stone(outer));
			data.set(c + i, 1, c - 4, stone(outer));
			data.set(c + i, 1, c + 4, stone(outer));
		}
		for (int i = -3; i <= 3; i++) {
			data.set(c - 3, 2, c + i, stone(StoneTypes.BEAM));
			data.set(c + 3, 2, c + i, stone(StoneTypes.BEAM));
			data.set(c + i, 2, c - 3, stone(StoneTypes.BEAM));
			data.set(c + i, 2, c + 3, stone(StoneTypes.BEAM));
		}
		for (int x : new int[] {-2, 2}) for (int z : new int[] {-2, 2}) {
			data.set(c + x, 2, c + z, stone(enhanced ? StoneTypes.GLOWCOL : StoneTypes.COLUMN));
			data.set(c + x, 3, c + z, stone(StoneTypes.ENGRAVED));
		}
		for (int x : new int[] {-3, 3}) for (int z : new int[] {-3, 3})
			data.set(c + x, 2, c + z, stone(StoneTypes.EMBOSSED));
		for (int x : new int[] {-4, 4}) for (int z : new int[] {-4, 4})
			data.set(c + x, 1, c + z, stone(StoneTypes.EMBOSSED));
		StateDef chroma = new StateDef("chromaticraft:liquid_chroma", Map.of("level", "0"));
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
			if (x != 0 || z != 0) data.set(c + x, 1, c + z, chroma);
		data.set(c, 0, c, STRUCTURE_VOID);
		data.set(c, 2, c, STRUCTURE_VOID);
		return data;
	}

	/**
	 * Exact V33a {@code PersonalChargerStructure}. The old origin is template {@code (2,0,2)} and
	 * the charger itself is six blocks above it at {@code (2,6,2)}. The black runes are palette
	 * placeholders; {@code PersonalChargerStructure} replaces all four with the charger's colour.
	 */
	private static TemplateData personalCharger() {
		TemplateData data = new TemplateData(5, 7, 5);
		int c = 2;
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
			data.set(c + x, 0, c + z, stone(StoneTypes.SMOOTH));
		data.set(c, 0, c, stone(StoneTypes.STABILIZER));

		for (int y = 1; y <= 4; y++) {
			StateDef state = stone(y == 4 ? StoneTypes.ENGRAVED : StoneTypes.COLUMN);
			for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1})
				data.set(c + sx, y, c + sz, state);
		}

		for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1}) {
			data.set(c + sx * 2, 0, c + sz * 2, stone(StoneTypes.BRICKS));
			data.set(c + sx * 2, 1, c + sz * 2, stone(StoneTypes.COLUMN));
			data.set(c + sx * 2, 2, c + sz * 2, RUNE_PLACEHOLDER);
		}

		for (int i = -1; i <= 1; i++) {
			data.set(c - 2, 0, c + i, stone(StoneTypes.GROOVE2));
			data.set(c + 2, 0, c + i, stone(StoneTypes.GROOVE2));
			data.set(c + i, 0, c - 2, stone(StoneTypes.GROOVE1));
			data.set(c + i, 0, c + 2, stone(StoneTypes.GROOVE1));
		}
		data.set(c, 6, c, new StateDef("chromaticraft:personal_charger", Map.of()));
		return data;
	}

    private static void setInfusionCircle(TemplateData data, int center, int y, double radius,
            StateDef state) {
        for (int angle = 0; angle < 360; angle += 15) {
            int x = (int)Math.floor(center + 0.5 + radius * Math.sin(Math.toRadians(angle)));
            int z = (int)Math.floor(center + 0.5 + radius * Math.cos(Math.toRadians(angle)));
            data.set(x, y, z, state);
        }
    }
    private static TemplateData repeater() {
        TemplateData data = new TemplateData(1, 4, 1);
        data.set(0, 3, 0, new StateDef("chromaticraft:crystal_repeater", Map.of()));
        data.set(0, 2, 0, RUNE_PLACEHOLDER);
        data.set(0, 1, 0, SMOOTH);
        data.set(0, 0, 0, SMOOTH);
        return data;
    }

	private static TemplateData weakRepeater() {
		TemplateData data = new TemplateData(1, 2, 1);
		data.set(0, 0, 0, new StateDef("minecraft:oak_log", Map.of("axis", "y")));
		data.set(0, 1, 0, new StateDef("chromaticraft:weak_repeater", Map.of()));
		return data;
	}

	/** Exact 5x4x5 V33a BoostedRelayStructure; anchor (2,3,2) is the Relay Source. */
	private static TemplateData relaySource() {
		TemplateData data = new TemplateData(5, 4, 5);
		int c = 2;
		for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
			data.set(x, 1, z, stone(StoneTypes.SMOOTH));
			data.set(x, 2, z, AIR);
		}
		for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1}) {
			data.set(c + sx, 0, c + sz, stone(StoneTypes.SMOOTH));
			data.set(c + sx, 1, c + sz, new StateDef("chromaticraft:liquid_chroma", Map.of("level", "0")));
		}
		data.set(c - 1, 1, c, stone(StoneTypes.GROOVE1));
		data.set(c + 1, 1, c, stone(StoneTypes.GROOVE1));
		data.set(c, 1, c - 1, stone(StoneTypes.GROOVE2));
		data.set(c, 1, c + 1, stone(StoneTypes.GROOVE2));
		data.set(c, 2, c, stone(StoneTypes.FOCUSFRAME));
		data.set(c - 2, 2, c, stone(StoneTypes.BRICKS));
		data.set(c + 2, 2, c, stone(StoneTypes.BRICKS));
		data.set(c, 2, c - 2, stone(StoneTypes.BRICKS));
		data.set(c, 2, c + 2, stone(StoneTypes.BRICKS));
		for (int sx : new int[] {-2, 2}) for (int sz : new int[] {-2, 2})
			data.set(c + sx, 2, c + sz, stone(StoneTypes.EMBOSSED));
		data.set(c, 3, c, new StateDef("chromaticraft:relay_source", Map.of()));
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

	private static StateDef stone(StoneTypes type, String axis) {
		String name = "chromaticraft:" + reika.chromaticraft.registry.ChromaBlocks.crystallineStoneName(type);
		return type.isBeam() ? new StateDef(name, Map.of("axis", axis)) : new StateDef(name, Map.of());
	}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }

    private static final int[][] POWER_CRYSTAL_POINTS = {
            {0,2}, {2,0}, {6,2}, {4,0}, {0,4}, {2,6}, {6,4}, {4,6}
    };

    private static final String TREE_LEAVES =
            "AAsEAAsFAAwDAAwEAAwFAAwGAA0EAA0FAA4DAA4EAA4FAA4GAA8DAA8EAA8FAA8GABAEABAFABEDABEEABEFABEGABIEABIFAQkE" +
            "AQkFAQoDAQoEAQoFAQoGAQsDAQsEAQsFAQsGAQwCAQwDAQwGAQwHAQ0DAQ0EAQ0FAQ0GAQ4BAQ4CAQ4DAQ4EAQ4FAQ4GAQ4HAQ4I" +
            "AQ8BAQ8CAQ8DAQ8EAQ8FAQ8GAQ8HAQ8IARADARAEARAFARAGARECAREDAREGAREHARIDARIEARIFARIGARMEARMFARQEARQFARUE" +
            "ARUFAggEAggFAgkDAgkEAgkFAgkGAgoCAgoDAgoEAgoFAgoGAgoHAgsCAgsDAgsEAgsFAgsGAgsHAgwBAgwCAgwDAgwGAgwHAgwI" +
            "Ag0CAg0DAg0EAg0FAg0GAg0HAg4BAg4CAg4DAg4EAg4FAg4GAg4HAg4IAg8BAg8CAg8DAg8EAg8FAg8GAg8HAg8IAhACAhADAhAE" +
            "AhAFAhAGAhAHAhEBAhECAhEDAhEGAhEHAhEIAhICAhIDAhIEAhIFAhIGAhIHAhMDAhMEAhMFAhMGAhQDAhQEAhQFAhQGAhUDAhUE" +
            "AhUFAhUGAhYEAhYFAhcEAhcFAhgEAhgFAhkEAhkFAwcEAwcFAwgDAwgEAwgFAwgGAwkCAwkDAwkEAwkFAwkGAwkHAwoBAwoCAwoD" +
            "AwoEAwoFAwoGAwoHAwoIAwsBAwsCAwsDAwsEAwsFAwsGAwsHAwsIAwwAAwwBAwwCAwwDAwwGAwwHAwwIAwwJAw0BAw0CAw0DAw0E" +
            "Aw0FAw0GAw0HAw0IAw4AAw4BAw4CAw4DAw4EAw4FAw4GAw4HAw4IAw4JAw8AAw8BAw8CAw8DAw8EAw8FAw8GAw8HAw8IAw8JAxAB" +
            "AxACAxADAxAEAxAFAxAGAxAHAxAIAxEAAxEBAxECAxEDAxEGAxEHAxEIAxEJAxIBAxICAxIDAxIEAxIFAxIGAxIHAxIIAxMCAxMD" +
            "AxMEAxMFAxMGAxMHAxQCAxQDAxQEAxQFAxQGAxQHAxUCAxUDAxUEAxUFAxUGAxUHAxYDAxYEAxYFAxYGAxcDAxcEAxcFAxcGAxgD" +
            "AxgEAxgFAxgGAxkDAxkEAxkFAxkGAxoEAxoFAxsEAxsFAxwEAxwFBAcDBAcGBAgCBAgDBAgGBAgHBAkBBAkCBAkDBAkGBAkHBAkI" +
            "BAoBBAoCBAoDBAoGBAoHBAoIBAsABAsBBAsCBAsDBAsGBAsHBAsIBAsJBAwABAwJBA0ABA0BBA0CBA0DBA0GBA0HBA0IBA0JBA4A" +
            "BA4BBA4CBA4DBA4GBA4HBA4IBA4JBA8ABA8BBA8CBA8DBA8GBA8HBA8IBA8JBBAABBABBBACBBADBBAGBBAHBBAIBBAJBBEABBEJ" +
            "BBIABBIBBBICBBIDBBIGBBIHBBIIBBIJBBMBBBMCBBMDBBMGBBMHBBMIBBQBBBQCBBQDBBQGBBQHBBQIBBUBBBUCBBUDBBUGBBUH" +
            "BBUIBBYCBBYDBBYGBBYHBBcCBBcDBBcGBBcHBBgCBBgDBBgGBBgHBBkCBBkDBBkGBBkHBBoDBBoGBBsDBBsGBBwDBBwEBBwFBBwG" +
            "BB0EBB0FBB4EBB4FBB8EBB8FBQcDBQcGBQgCBQgDBQgGBQgHBQkBBQkCBQkDBQkGBQkHBQkIBQoBBQoCBQoDBQoGBQoHBQoIBQsA" +
            "BQsBBQsCBQsDBQsGBQsHBQsIBQsJBQwABQwJBQ0ABQ0BBQ0CBQ0DBQ0GBQ0HBQ0IBQ0JBQ4ABQ4BBQ4CBQ4DBQ4GBQ4HBQ4IBQ4J" +
            "BQ8ABQ8BBQ8CBQ8DBQ8GBQ8HBQ8IBQ8JBRAABRABBRACBRADBRAGBRAHBRAIBRAJBREABREJBRIABRIBBRICBRIDBRIGBRIHBRII" +
            "BRIJBRMBBRMCBRMDBRMGBRMHBRMIBRQBBRQCBRQDBRQGBRQHBRQIBRUBBRUCBRUDBRUGBRUHBRUIBRYCBRYDBRYGBRYHBRcCBRcD" +
            "BRcGBRcHBRgCBRgDBRgGBRgHBRkCBRkDBRkGBRkHBRoDBRoGBRsDBRsGBRwDBRwEBRwFBRwGBR0EBR0FBR4EBR4FBR8EBR8FBgcE" +
            "BgcFBggDBggEBggFBggGBgkCBgkDBgkEBgkFBgkGBgkHBgoBBgoCBgoDBgoEBgoFBgoGBgoHBgoIBgsBBgsCBgsDBgsEBgsFBgsG" +
            "BgsHBgsIBgwABgwBBgwCBgwDBgwGBgwHBgwIBgwJBg0BBg0CBg0DBg0EBg0FBg0GBg0HBg0IBg4ABg4BBg4CBg4DBg4EBg4FBg4G" +
            "Bg4HBg4IBg4JBg8ABg8BBg8CBg8DBg8EBg8FBg8GBg8HBg8IBg8JBhABBhACBhADBhAEBhAFBhAGBhAHBhAIBhEABhEBBhECBhED" +
            "BhEGBhEHBhEIBhEJBhIBBhICBhIDBhIEBhIFBhIGBhIHBhIIBhMCBhMDBhMEBhMFBhMGBhMHBhQCBhQDBhQEBhQFBhQGBhQHBhUC" +
            "BhUDBhUEBhUFBhUGBhUHBhYDBhYEBhYFBhYGBhcDBhcEBhcFBhcGBhgDBhgEBhgFBhgGBhkDBhkEBhkFBhkGBhoEBhoFBhsEBhsF" +
            "BhwEBhwFBwgEBwgFBwkDBwkEBwkFBwkGBwoCBwoDBwoEBwoFBwoGBwoHBwsCBwsDBwsEBwsFBwsGBwsHBwwBBwwCBwwDBwwGBwwH" +
            "BwwIBw0CBw0DBw0EBw0FBw0GBw0HBw4BBw4CBw4DBw4EBw4FBw4GBw4HBw4IBw8BBw8CBw8DBw8EBw8FBw8GBw8HBw8IBxACBxAD" +
            "BxAEBxAFBxAGBxAHBxEBBxECBxEDBxEGBxEHBxEIBxICBxIDBxIEBxIFBxIGBxIHBxMDBxMEBxMFBxMGBxQDBxQEBxQFBxQGBxUD" +
            "BxUEBxUFBxUGBxYEBxYFBxcEBxcFBxgEBxgFBxkEBxkFCAkECAkFCAoDCAoECAoFCAoGCAsDCAsECAsFCAsGCAwCCAwDCAwGCAwH" +
            "CA0DCA0ECA0FCA0GCA4BCA4CCA4DCA4ECA4FCA4GCA4HCA4ICA8BCA8CCA8DCA8ECA8FCA8GCA8HCA8ICBADCBAECBAFCBAGCBEC" +
            "CBEDCBEGCBEHCBIDCBIECBIFCBIGCBMECBMFCBQECBQFCBUECBUFCQsECQsFCQwDCQwECQwFCQwGCQ0ECQ0FCQ4DCQ4ECQ4FCQ4G" +
            "CQ8DCQ8ECQ8FCQ8GCRAECRAFCREDCREECREFCREGCRIECRIF";
    private static final String TREE_LOG_Y =
            "AwIDAwIEAwIFAwIGAwMDAwMEAwMFAwMGAwQEAwQFAwUEAwUFBAAEBAAFBAEEBAEFBAIDBAIEBAIFBAIGBAMDBAMEBAMFBAMGBAQD" +
            "BAQEBAQFBAQGBAUDBAUEBAUFBAUGBAYEBAYFBAcEBAcFBAgEBAgFBAkEBAkFBAoEBAoFBAsEBAsFBAwEBAwFBA0EBA0FBA4EBA4F" +
            "BA8EBA8FBBAEBBAFBBEEBBEFBBIEBBIFBBMEBBMFBBQEBBQFBBUEBBUFBBYEBBYFBBcEBBcFBBgEBBgFBBkEBBkFBBoEBBoFBBsE" +
            "BBsFBQAEBQAFBQEEBQEFBQIDBQIEBQIFBQIGBQMDBQMEBQMFBQMGBQQDBQQEBQQFBQQGBQUDBQUEBQUFBQUGBQYEBQYFBQcEBQcF" +
            "BQgEBQgFBQkEBQkFBQoEBQoFBQsEBQsFBQwEBQwFBQ0EBQ0FBQ4EBQ4FBQ8EBQ8FBRAEBRAFBREEBREFBRIEBRIFBRMEBRMFBRQE" +
            "BRQFBRUEBRUFBRYEBRYFBRcEBRcFBRgEBRgFBRkEBRkFBRoEBRoFBRsEBRsFBgIDBgIEBgIFBgIGBgMDBgMEBgMFBgMGBgQEBgQF" +
            "BgUEBgUF";
    private static final String TREE_LOG_X =
            "AQwEAQwFAREEAREFAgMEAgMFAgwEAgwFAhEEAhEFAwwEAwwFAxEEAxEFBgwEBgwFBhEEBhEFBwMEBwMFBwwEBwwFBxEEBxEFCAwE" +
            "CAwFCBEECBEF";
    private static final String TREE_LOG_Z =
            "BAMCBAMHBAwBBAwCBAwDBAwGBAwHBAwIBBEBBBECBBEDBBEGBBEHBBEIBQMCBQMHBQwBBQwCBQwDBQwGBQwHBQwIBREBBRECBRED" +
            "BREGBREHBREI";

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
        private final LinkedHashMap<BlockPos, Cell> blocks = new LinkedHashMap<>();

        private TemplateData(int x, int y, int z) {
            sizeX = x;
            sizeY = y;
            sizeZ = z;
        }

        void set(int x, int y, int z, StateDef state) {
            this.set(x, y, z, state, null);
        }

        void set(int x, int y, int z, StateDef state, CompoundTag nbt) {
            blocks.put(new BlockPos(x, y, z), new Cell(state, nbt));
        }

        void copyFrom(TemplateData source, int offsetX, int offsetY, int offsetZ) {
            source.blocks.forEach((pos, cell) ->
                    this.set(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ,
                            cell.state(), cell.nbt() == null ? null : cell.nbt().copy()));
        }
        void remove(int x, int y, int z) {
            blocks.remove(new BlockPos(x, y, z));
        }

        boolean is(int x, int y, int z, StateDef state) {
            Cell cell = blocks.get(new BlockPos(x, y, z));
            return cell != null && cell.state().equals(state);
        }

        void makePlainShield(int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            Cell cell = blocks.get(pos);
            if (cell == null || !cell.state().name().startsWith(ChromatiCraft.MODID + ":shielding_")) return;
            Map<String, String> properties = new LinkedHashMap<>(cell.state().properties());
            properties.put("reinforced", "false");
            blocks.put(pos, new Cell(new StateDef(cell.state().name(), Map.copyOf(properties)), cell.nbt()));
        }

		void inheritDoorFacings() {
			for (Map.Entry<BlockPos, Cell> entry : List.copyOf(blocks.entrySet())) {
				StateDef upper = entry.getValue().state();
				if (!upper.name().endsWith("_door") || !"upper".equals(upper.properties().get("half")))
					continue;
				Cell below = blocks.get(entry.getKey().below());
				if (below == null || !upper.name().equals(below.state().name())
						|| !"lower".equals(below.state().properties().get("half")))
					continue;
				Map<String, String> properties = new LinkedHashMap<>(upper.properties());
				properties.put("facing", below.state().properties().get("facing"));
				blocks.put(entry.getKey(), new Cell(new StateDef(upper.name(), Map.copyOf(properties)),
						entry.getValue().nbt()));
			}
		}

        CompoundTag toNBT() {
            CompoundTag root = new CompoundTag();
            root.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
            root.put("size", ints(sizeX, sizeY, sizeZ));
            root.put("entities", new ListTag());

            LinkedHashMap<StateDef, Integer> paletteIds = new LinkedHashMap<>();
            for (Cell cell : blocks.values())
                paletteIds.computeIfAbsent(cell.state(), ignored -> paletteIds.size());
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
                        block.putInt("state", paletteIds.get(entry.getValue().state()));
                        if (entry.getValue().nbt() != null)
                            block.put("nbt", entry.getValue().nbt().copy());
                        blockList.add(block);
                    });
            root.put("blocks", blockList);
            return root;
        }

        private record Cell(StateDef state, CompoundTag nbt) {}
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
