package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.ChromaGameTests;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.world.OverworldStructureFeature.Type;
import reika.chromaticraft.world.biome.ChromaBiomes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Survival contracts exercise the natural entry path against completed terrain, not /place. */
public final class OverworldGenerationGameTests {
    private record Fixture(Type type, ChunkPos chunk, BlockPos controller, long seed) {}

    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        ChromaGameTests.register(event, environment, "overworld_bounded_population", 4000,
                OverworldGenerationGameTests::boundedDungeons);
        ChromaGameTests.register(event, environment, "overworld_bounded_persistence", 80,
                OverworldGenerationGameTests::boundedPersistence);
        ChromaGameTests.register(event, environment, "overworld_bounded_neoforge", 4000,
                OverworldGenerationGameTests::boundedNeoForge);
        ChromaGameTests.register(event, environment, "overworld_bounded_unloaded", 4000,
                OverworldGenerationGameTests::boundedUnloaded);
        if (net.neoforged.fml.ModList.get().isLoaded("chunky"))
            ChromaGameTests.register(event, environment, "overworld_bounded_chunky", 4000,
                    OverworldGenerationGameTests::boundedChunky);
        ChromaGameTests.register(event, environment, "overworld_modern_heights", 80,
                OverworldGenerationGameTests::modernHeights);
        ChromaGameTests.register(event, environment, "overworld_survival_natural_dungeons", 4000,
                OverworldGenerationGameTests::naturalDungeons);
        ChromaGameTests.register(event, environment, "overworld_survival_ocean_rejections", 80,
                OverworldGenerationGameTests::oceanRejections);
        ChromaGameTests.register(event, environment, "overworld_survival_registry", 20,
                OverworldGenerationGameTests::registry);
        ChromaGameTests.register(event, environment, "overworld_survival_warp_grid", 30,
                OverworldGenerationGameTests::warpGrid);
        ChromaGameTests.register(event, environment, "overworld_survival_cliffs_flowers", 80,
                OverworldGenerationGameTests::cliffsFlowers);
        ChromaGameTests.register(event, environment, "overworld_survival_lore_towers", 4000,
                OverworldGenerationGameTests::loreTowers);
        ChromaGameTests.register(event, environment, "overworld_survival_rock_support", 30,
                OverworldGenerationGameTests::rockSupport);
        ChromaGameTests.register(event, environment, "overworld_survival_lore_layout_identity", 30,
                OverworldGenerationGameTests::loreLayoutIdentity);
        if (Boolean.getBoolean("chromaticraft.testNaturalWorld"))
            ChromaGameTests.register(event, environment, "overworld_survival_population", 120000,
                    OverworldGenerationGameTests::population);
    }

    /** Exercise the emitted features in actual deepslate, not just their configured numbers. */
    private static void modernHeights(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        var random = RandomSource.create(472694);
        var context = new net.minecraft.world.level.levelgen.placement.PlacementContext(level, generator, java.util.Optional.empty());
        var placement = new TieredOreHeightPlacement(false);
        int[] histogram = new int[3];
        for (int i = 0; i < 4096; i++) placement.modify(context, random, BlockPos.ZERO, at -> {
            helper.assertTrue(at.getY() >= -60 && at.getY() <= 63, "tiered ores stay above bedrock and below their cave ceiling");
            histogram[at.getY() < 0 ? 0 : 1]++;
        });
        helper.assertTrue(histogram[0] > 2 * histogram[1] && histogram[1] > 500,
                "tiered ore mixture must include both cave layers and preserve its deep bias");
        var base = helper.absolutePos(new BlockPos(0, 0, 0));
        BlockPos center = new BlockPos(base.getX() + 8, -32, base.getZ() + 8);
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-16, -28, -16), center.offset(24, 8, 24)))
            level.setBlock(at, Blocks.DEEPSLATE.defaultBlockState(), 2);
        var tiered = features.getOrThrow(ResourceKey.create(Registries.FEATURE,
                Identifier.fromNamespaceAndPath("chromaticraft", "energized_rock"))).value();
        helper.assertTrue(tiered.place(level, generator, RandomSource.create(47), center),
                "generated ChromatiCraft ore feature must actually replace deepslate at negative Y");
        helper.assertTrue(reika.dragonapi.libraries.level.LegacyOreVeins.place(level, RandomSource.create(47),
                reika.electricraft.registry.ElectriOres.SILVER.getBlock().defaultBlockState(),
                reika.electricraft.registry.ElectriOres.SILVER.getDeepslateBlock().defaultBlockState(),
                8, 0, center.getX(), center.getY(), center.getZ()), "ElectriCraft vein must place below zero");
        int deepSilver = 0;
        for (BlockPos at : BlockPos.betweenClosed(center.offset(0, -4, 0), center.offset(16, 4, 16)))
            if (level.getBlockState(at).is(reika.electricraft.registry.ElectriOres.SILVER.getDeepslateBlock())) deepSilver++;
        helper.assertTrue(deepSilver > 0, "deepslate hosts must yield the registered deepslate silver variant");
        BlockPos indicator = center.offset(-8, 0, -8);
        level.setBlock(indicator.above(), Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(CaveIndicatorFeature.canGenerateIn(level, indicator), "cave indicators accept deepslate below zero");
        var reactor = (reika.reactorcraft.world.ReactorOreFeature)features.getOrThrow(ResourceKey.create(Registries.FEATURE,
                Identifier.fromNamespaceAndPath("reactorcraft", "indium_ore"))).value();
        helper.assertTrue(reactor.config().minY() == -60 && reactor.config().maxY() == -32,
                "datagen must emit the modern indium band");
        helper.assertTrue(reactor.place(level, generator, RandomSource.create(47), center.atY(-64)),
                "ReactorCraft indium must place in its new deep band");
        String legendOutput = System.getProperty("dragonapi.biomeLegendOutput");
        if (legendOutput != null) try {
            // Headless Minecraft omits client assets. The export supplies the vanilla colormap explicitly.
            try (var input = new java.io.FileInputStream(System.getProperty("dragonapi.biomeLegendGrass"))) {
                var image = javax.imageio.ImageIO.read(input);
                net.minecraft.world.level.GrassColor.init(image.getRGB(0, 0, 256, 256, null, 0, 256));
            }
            reika.dragonapi.command.BiomeMapCommand.writeBiomeLegend(level, new java.io.File(legendOutput));
        } catch (java.io.IOException error) {
            throw new RuntimeException("Biome legend export failed", error);
        }
        helper.succeed();
    }

    private record PopulationSite(ResourceKey<Biome> biome, BlockPos sample) {}

    /** Real seed-zero noise chunks, TerraBlender biomes and decoration from the whole family. */
    private static void population(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        helper.assertTrue(generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,
                "population probe requires the disposable normal-world datapack");
        List<PopulationSite> sites = new ArrayList<>();
        HashSet<ChunkPos> forced = new HashSet<>();
        for (var key : List.of(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.ENDER_FOREST,
                ChromaBiomes.LUMINOUS_CLIFFS, Biomes.DEEP_OCEAN, Biomes.DESERT, Biomes.SNOWY_PLAINS)) {
            var found = generator.getBiomeSource().findClosestBiome3d(new BlockPos(0, 63, 0), 16384, 64, 512,
                    holder -> holder.is(key), level.getChunkSource().randomState(), level);
            helper.assertTrue(found != null, "normal biome source must offer " + key.identifier());
            sites.add(new PopulationSite(key, found.getFirst()));
            ChunkPos center = ChunkPos.containing(found.getFirst());
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                ChunkPos at = new ChunkPos(center.x() + x, center.z() + z);
                if (forced.add(at)) level.getChunkSource().updateChunkForced(at, true);
            }
        }
        helper.runAfterDelay(1, () -> pollPopulation(helper, sites, forced));
    }

    private static void pollPopulation(GameTestHelper helper, List<PopulationSite> sites, java.util.Set<ChunkPos> forced) {
        var level = helper.getLevel();
        var queue = PostPopulationWorldgen.get(level);
        if (forced.stream().anyMatch(pos -> level.getChunkSource().getChunkNow(pos.x(), pos.z()) == null
                || queue.hasPendingWork(pos))) {
            // GameTestServer runs unthrottled. Yield briefly to asynchronous chunk workers;
            // otherwise it burns thousands of timeout ticks before one noise chunk can finish.
            java.util.concurrent.locks.LockSupport.parkNanos(2_000_000L);
            helper.runAfterDelay(1, () -> pollPopulation(helper, sites, forced));
            return;
        }
        int ores = 0, cliffColumns = 0, shapedColumns = 0, chromaLeaves = 0, glowingFlowers = 0;
        var sampler = reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature.sampler(level);
        for (ChunkPos pos : forced) {
            var chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int bx = pos.getMinBlockX() + x, bz = pos.getMinBlockZ() + z;
                if (sampler.biome(bx, bz).isGlowingCliffs()) {
                    cliffColumns++;
                    if (OverworldStructureFeature.terrainTop(level, bx, bz) == sampler.column(bx, bz).top() + 1)
                        shapedColumns++;
                }
                for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                    BlockState state = chunk.getBlockState(new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                    if (state.is(reika.chromaticraft.registry.ChromaBlocks.ENERGIZED_ROCK.get())) ores++;
                    if (state.is(reika.chromaticraft.registry.ChromaBlocks.GLOW_DAISY.get())
                            || state.is(reika.chromaticraft.registry.ChromaBlocks.GLOW_ROOT.get())) glowingFlowers++;
                    if (state.is(net.minecraft.tags.BlockTags.LEAVES)
                            && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("chromaticraft"))
                        chromaLeaves++;
                }
            }
        }
        for (PopulationSite site : sites) {
            var pos = site.sample();
            helper.assertTrue(level.getUncachedNoiseBiome(net.minecraft.core.QuartPos.fromBlock(pos.getX()),
                    net.minecraft.core.QuartPos.fromBlock(pos.getY()), net.minecraft.core.QuartPos.fromBlock(pos.getZ())).is(site.biome()),
                    "populated biome must match the natural search for " + site.biome().identifier());
        }
        for (ChunkPos at : forced) level.getChunkSource().updateChunkForced(at, false);
        helper.assertTrue(ores > 0, "full family population must retain progression ores after GeoStrata decoration");
        helper.assertTrue(cliffColumns > 0 && shapedColumns * 4 >= cliffColumns * 3,
                "natural Luminous Cliffs must retain their shaped ground heights after all decoration: " + shapedColumns + "/" + cliffColumns);
        helper.assertTrue(chromaLeaves > 0, "natural custom biomes must receive ChromatiCraft trees");
        reika.chromaticraft.ChromatiCraft.LOGGER.info("Natural overworld probe: " + forced.size() + " FULL chunks; "
                + ores + " Energized Rock, " + shapedColumns + "/" + cliffColumns + " shaped cliff columns, "
                + chromaLeaves + " Chroma leaves, " + glowingFlowers + " glowing flowers/roots; explored queues drained.");
        helper.succeed();
    }

    private static void warpGrid(GameTestHelper helper) {
        long seed = 0x51A7E123L;
        var reference = new reika.dragonapi.instantiable.data.ShuffledGrid(2048, 20, 55);
        reference.calculate(new java.util.Random(seed * 7L));
        // Exhaust the grid, including its signed wrapping. This also detects a changed seed,
        // row order or discarded RNG draws rather than just checking average density.
        for (int x = 0; x < 2048; x++) for (int z = 0; z < 2048; z++)
            helper.assertTrue(WarpNodeFeature.isSelectedChunk(seed, x - 2048, z + 2048)
                    == reference.isValid(x, z), "warp candidate differs from V33a at " + x + "," + z);
        helper.assertTrue(WarpNodeFeature.minimumHeight(40, true) == 199,
                "cliff shores must retain their raised warp-node floor");
        helper.assertTrue(WarpNodeFeature.minimumHeight(40, false) == 88,
                "ordinary low terrain retains the source 48-block clearance");
        helper.assertTrue(WarpNodeFeature.minimumHeight(150, true) == 199,
                "the modern middle plateau must still set the raised floor");
        helper.assertTrue(WarpNodeFeature.minimumHeight(240, true) == 288,
                "high plateaus retain 48 blocks of clearance above the old Y255 limit");
        helper.succeed();
    }

    private static void cliffsFlowers(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos base = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        ChunkPos chunk = new ChunkPos(base.x() + 64, base.z() + 192);
        var biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ChromaBiomes.LUMINOUS_CLIFFS);
        level.getChunk(chunk.x(), chunk.z()).fillBiomesFromNoise((qx, qy, qz) -> biome);
        BlockPos origin = new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ());
        for (BlockPos at : BlockPos.betweenClosed(origin.offset(0, 4, 0), origin.offset(15, 127, 15)))
            level.setBlock(at, (at.getY() == 40 || at.getY() == 100 ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
        for (boolean root : List.of(false, true)) {
            // A single-placement seed lets the test pin down the column and every source draw,
            // including the discarded +/-25 height roll before a root chooses floor+4..80.
            long seed = 1;
            while (new SavedWorldgenRandom(seed).nextInt(5) == 0) seed++;
            var reference = new SavedWorldgenRandom(seed);
            reference.nextInt(5);
            int x = origin.getX() + reference.nextInt(16), z = origin.getZ() + reference.nextInt(16);
            int initial = 101 + reference.nextInt(51) - 25;
            int y = root ? (reika.dragonapi.libraries.level.WorldgenHeight.randomBetween(level, reference,
                    reika.dragonapi.libraries.level.WorldgenHeight.caveFloor(level), 80) < 40 ? 39 : 99) : (initial < 100 ? 41 : 101);
            helper.assertTrue(new CliffsFlowerFeature(root).placePatch(level, new SavedWorldgenRandom(seed), origin),
                    "Cliffs flower patch must find the supported floor or cave ceiling");
            BlockPos at = new BlockPos(x, y, z);
            var block = (root ? reika.chromaticraft.registry.ChromaBlocks.GLOW_ROOT
                    : reika.chromaticraft.registry.ChromaBlocks.GLOW_DAISY).get();
            helper.assertTrue(level.getBlockState(at).is(block) && level.getBlockState(at).canSurvive(level, at),
                    "flower patch must preserve V33a's column, random draws and support: " + at);
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        }
        var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        level.getChunk(chunk.x(), chunk.z()).fillBiomesFromNoise((qx, qy, qz) -> plains);
        helper.assertTrue(!new CliffsFlowerFeature(true).placePatch(level, new SavedWorldgenRandom(1), origin),
                "cliff-root attempts must reject ordinary biome columns at a boundary");
        helper.succeed();
    }

    private record TowerFixture(ChunkPos chunk, reika.chromaticraft.magic.lore.Towers tower, boolean ocean) {}

    private static void loreLayoutIdentity(GameTestHelper helper) {
        var level = helper.getLevel();
        var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        helper.assertTrue(nether != null && nether.getSeed() == level.getSeed(), "fixture must offer distinct levels with the same seed");
        reika.chromaticraft.magic.lore.Towers.getTower(level, 0, 0);
        helper.assertTrue(reika.chromaticraft.magic.lore.Towers.initialized(level), "current level must retain its initialized layout");
        helper.assertTrue(!reika.chromaticraft.magic.lore.Towers.initialized(nether),
                "equal seeds must not make a different level borrow this world's creation-dependent tower layout");
        helper.succeed();
    }

    private static void rockSupport(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        var granite = reika.geostrata.registry.RockTypes.GRANITE.getID(reika.geostrata.registry.RockShapes.SMOOTH).defaultBlockState();
        helper.assertTrue(granite.is(reika.chromaticraft.registry.ChromaBlockTags.NATURAL_STONE),
                "GeoStrata's naturally generated smooth granite must remain natural stone");
        level.setBlock(pos.above(), granite, 2);
        helper.assertTrue(reika.chromaticraft.registry.ChromaBlocks.GLOW_ROOT.get().defaultBlockState().canSurvive(level, pos),
                "Lumen Roots must survive GeoStrata cave ceilings");
        level.setBlock(pos, granite, 2);
        helper.assertTrue(CaveIndicatorFeature.canGenerateIn(level, pos), "Piezo Crystals must retain GeoStrata rock hosts");
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            level.setBlock(pos.offset(x, 0, z), granite, 2);
            for (int y = 1; y <= 10; y++) level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(pos.getX(), level.getMinY(), pos.getZ()), Blocks.BEDROCK.defaultBlockState(), 2);
        helper.assertTrue(PylonFeature.canGenerateAt(level, pos), "pylon foundations must accept a natural GeoStrata surface");
        helper.assertTrue(!reika.geostrata.registry.RockTypes.GRANITE.getID(reika.geostrata.registry.RockShapes.BRICK)
                .defaultBlockState().is(reika.chromaticraft.registry.ChromaBlockTags.NATURAL_STONE),
                "crafted GeoStrata bricks must stay outside the natural-rock support tag");
        helper.succeed();
    }

    private static void loreTowers(GameTestHelper helper) {
        var level = helper.getLevel();
        reika.chromaticraft.magic.lore.Towers.getTower(level, 0, 0);
        ChunkPos base = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        List<TowerFixture> fixtures = List.of(
                new TowerFixture(new ChunkPos(base.x() + 64, base.z() + 256), reika.chromaticraft.magic.lore.Towers.BETA, true),
                new TowerFixture(new ChunkPos(base.x() + 128, base.z() + 256), reika.chromaticraft.magic.lore.Towers.PHI, false));
        for (var fixture : fixtures) {
            ChunkPos chunk = fixture.chunk();
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x() + x, chunk.z() + z);
            BlockPos center = new BlockPos(chunk.getMinBlockX() + 8, 0, chunk.getMinBlockZ() + 8);
            int bottom = fixture.ocean() ? 36 : level.getMaxY() - 13;
            int top = fixture.ocean() ? 63 : level.getMaxY() - 2;
            for (BlockPos at : BlockPos.betweenClosed(center.offset(-20, bottom, -20), center.offset(20, top, 20)))
                level.setBlock(at, (fixture.ocean() ? (at.getY() > 40 ? Blocks.WATER : Blocks.DIRT) : Blocks.STONE).defaultBlockState(), 2);
            level.getChunkSource().updateChunkForced(chunk, true);
            PostPopulationWorldgen.get(level).enqueueTower(chunk, fixture.tower(), 0xDA7A);
        }
        helper.runAfterDelay(1, () -> pollTowers(helper, fixtures));
    }

    private static void pollTowers(GameTestHelper helper, List<TowerFixture> fixtures) {
        var level = helper.getLevel();
        var queue = PostPopulationWorldgen.get(level);
        if (fixtures.stream().anyMatch(f -> !queue.isTowerFinished(f.chunk(), f.tower()))) {
            helper.runAfterDelay(1, () -> pollTowers(helper, fixtures));
            return;
        }
        for (var fixture : fixtures) {
            BlockPos node = fixture.tower().getGeneratedLocation();
            helper.assertTrue(node != null && level.getBlockEntity(node) instanceof reika.chromaticraft.tileentity.TileEntityDataNode,
                    "queued lore tower must retain its node");
            helper.assertTrue(node.getY() + 4 < level.getMaxY(), "all four dummy relays must fit below the world ceiling");
            for (int y = 1; y <= 4; y++) helper.assertTrue(level.getBlockEntity(node.above(y))
                    instanceof reika.chromaticraft.tileentity.TileEntityDummyAux, "queued tower must place every relay");
            if (fixture.ocean()) {
                helper.assertTrue(node.getY() == 65, "ocean lore tower must stand on the source sea-level island: " + node);
                BlockPos neighbour = new BlockPos(node.getX() + 16, 41, node.getZ());
                helper.assertTrue(level.getBlockState(neighbour).is(Blocks.DIRT) && level.getBlockState(neighbour.above()).is(Blocks.WATER),
                        "island taper must replace water in the adjacent chunk and retain water above it");
                helper.assertTrue(level.getBlockState(new BlockPos(node.getX() + 19, 41, node.getZ())).is(Blocks.WATER),
                        "island must preserve water beyond the source 18-block radius");
            }
            boolean chest = false;
            for (BlockPos at : BlockPos.betweenClosed(node.offset(-1, -2, -1), node.offset(1, -2, 1))) {
                if (level.getBlockEntity(at) instanceof reika.chromaticraft.tileentity.TileEntityLootChest loot) {
                    helper.assertTrue(reika.chromaticraft.data.ChromaLootProvider.DATA_TOWER_LOOT.equals(loot.getLootTable()),
                            "tower chest must use its complete source loot table");
                    loot.unpackLootTable(null);
                    boolean any = false;
                    for (int slot = 0; slot < loot.getContainerSize(); slot++) any |= !loot.getItem(slot).isEmpty();
                    helper.assertTrue(any, "tower loot table must resolve into actual survival loot");
                    chest = true;
                }
            }
            helper.assertTrue(chest, "lore tower must retain its buried chest");
            level.getChunkSource().updateChunkForced(fixture.chunk(), false);
        }
        helper.succeed();
    }

    private static Fixture fixture(GameTestHelper helper, Type type, int slot, boolean cave) {
        ServerLevel level = helper.getLevel();
        ChunkPos base = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        ChunkPos chunk = new ChunkPos(base.x() + 64 * (slot + 1), base.z() + 64);
        long seed = 0x5A71A1L + slot;
        SavedWorldgenRandom random = new SavedWorldgenRandom(seed);
        int x = chunk.getMinBlockX() + random.nextInt(16), z = chunk.getMinBlockZ() + random.nextInt(16);
        ResourceKey<Biome> key = switch (type) {
            case OCEAN -> Biomes.DEEP_OCEAN;
            case DESERT -> Biomes.DESERT;
            case SNOW -> Biomes.SNOWY_PLAINS;
            case BIOME_FRAGMENT -> ChromaBiomes.RAINBOW_FOREST;
            default -> Biomes.PLAINS;
        };
        Holder<Biome> biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
        for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++)
            level.getChunk(chunk.x() + cx, chunk.z() + cz).fillBiomesFromNoise((qx, qy, qz) -> biome);
        int ground = type == Type.OCEAN ? 40 : 64;
        BlockState surface = (type == Type.DESERT ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState();
        for (BlockPos at : BlockPos.betweenClosed(new BlockPos(x - 16, -60, z - 16), new BlockPos(x + 48, ground, z + 48)))
            level.setBlock(at, at.getY() == ground && type != Type.OCEAN ? surface : Blocks.STONE.defaultBlockState(), 2);
        BlockPos controller = switch (type) {
            case CAVERN -> new BlockPos(x, reika.dragonapi.libraries.level.WorldgenHeight.randomBetween(level, random,
                    reika.dragonapi.libraries.level.WorldgenHeight.undergroundY(level, 10),
                    reika.dragonapi.libraries.level.WorldgenHeight.undergroundY(level, 49)), z);
            case BURROW -> new BlockPos(x - 5, ground - 8, z - 2);
            case OCEAN -> new BlockPos(x, ground - 2, z);
            case DESERT -> new BlockPos(x, ground - 7, z);
            case SNOW -> new BlockPos(x + 8, ground - 3, z + 6);
            case BIOME_FRAGMENT -> new BlockPos(x, ground - 4, z);
        };
        if (type == Type.CAVERN) {
            level.setBlock(controller.offset(17, 0, 0), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(controller.offset(17, -1, 0), Blocks.AIR.defaultBlockState(), 2);
        }
        if (type == Type.OCEAN) {
            for (BlockPos at : BlockPos.betweenClosed(new BlockPos(x - 16, ground + 1, z - 16), new BlockPos(x + 48, 63, z + 48)))
                level.setBlock(at, Blocks.WATER.defaultBlockState(), 2);
            // X source water probe blocked. Z probe and its actual mouth remain open, with
            // an obstacle only on the erroneous Z-axis probe beyond the mouth.
            for (int y = 8; y <= 10; y++) for (int side = -1; side <= 1; side++)
                level.setBlock(controller.offset(28, y, side), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(controller.offset(0, 9, 29), Blocks.STONE.defaultBlockState(), 2);
            if (cave) for (BlockPos at : BlockPos.betweenClosed(new BlockPos(x - 2, -10, z - 2), new BlockPos(x + 2, -8, z + 2)))
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        }
        if (type == Type.BIOME_FRAGMENT) {
            // A canopy above the sampled column must not lift the natural room off the ground.
            for (int y = ground + 12; y <= ground + 20; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        }
        return new Fixture(type, chunk, controller, seed);
    }

    private static void naturalDungeons(GameTestHelper helper) {
        List<Fixture> fixtures = new ArrayList<>();
        var level = helper.getLevel();
        for (Type type : Type.values()) {
            Fixture fixture = fixture(helper, type, type.ordinal(), true);
            fixtures.add(fixture);
            level.getChunkSource().updateChunkForced(fixture.chunk(), true);
            PostPopulationWorldgen.get(level).enqueueDungeon(type, fixture.chunk(), fixture.seed());
        }
        helper.runAfterDelay(1, () -> pollDungeons(helper, fixtures));
    }

    private static void boundedPersistence(GameTestHelper helper) {
        var data = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(helper.getLevel());
        ChunkPos root = new ChunkPos(12345, -12345);
        var scope = reika.dragonapi.instantiable.worldgen.PopulationScope.observedChunks();
        scope.observe(root);
        long id = data.begin("test-producer", "persistence", scope, true);
        helper.assertTrue(data.admits(root), "running producer authorizes its root");
        var tag = reika.dragonapi.auxiliary.trackers.BoundedPopulation.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, data).getOrThrow();
        var restored = reika.dragonapi.auxiliary.trackers.BoundedPopulation.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertTrue(!restored.admits(root), "an interrupted producer restores paused");
        restored.resume(id);
        helper.assertTrue(restored.admits(root), "saved roots can resume without terrain regeneration");
        helper.assertTrue(!restored.admits(new ChunkPos(root.x() + 1, root.z())), "saved roots do not admit support halo");
        restored.cancel(id);
        helper.assertTrue(!restored.admits(root), "cancel revokes admission without deleting queued jobs");
        data.cancel(id);
        helper.succeed();
    }

    private static void boundedDungeons(GameTestHelper helper) {
        var level = helper.getLevel();
        var queue = PostPopulationWorldgen.get(level);
        var population = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level);
        List<Fixture> fixtures = new ArrayList<>();
        List<Long> scopes = new ArrayList<>();
        for (Type type : Type.values()) {
            Fixture f = fixture(helper, type, 32 + type.ordinal(), true);
            fixtures.add(f);
            queue.enqueueDungeon(type, f.chunk(), f.seed());
            long id = population.begin("test-area", type.name(),
                    reika.dragonapi.instantiable.worldgen.PopulationScope.rectangle(f.chunk().x(), f.chunk().z(), f.chunk().x(), f.chunk().z()), false);
            population.pause(id); scopes.add(id);
        }
        ChunkPos inside = fixtures.getFirst().chunk();
        ChunkPos outside = new ChunkPos(inside.x() + 20, inside.z() + 20);
        var geo = reika.geostrata.level.GeoPostPopulation.get(level);
        geo.enqueue(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, inside, inside.getMinBlockX(), inside.getMinBlockZ(), 47);
        geo.enqueue(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, outside, outside.getMinBlockX(), outside.getMinBlockZ(), 47);
        queue.enqueueDungeon(Type.CAVERN, outside, 47);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(fixtures.stream().noneMatch(f -> queue.isDungeonFinished(f.type(), f.chunk())), "paused remote jobs must wait");
            helper.assertTrue(!geo.finished(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, inside), "Geo queue respects pause");
            scopes.forEach(population::resume);
            pollBounded(helper, fixtures, scopes, outside);
        });
    }

    private static void pollBounded(GameTestHelper helper, List<Fixture> fixtures, List<Long> scopes, ChunkPos outside) {
        pumpPopulationMailbox(helper);
        var level = helper.getLevel();
        var queue = PostPopulationWorldgen.get(level);
        var geo = reika.geostrata.level.GeoPostPopulation.get(level);
        var population = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level);
        helper.assertTrue(!queue.isDungeonFinished(Type.CAVERN, outside), "Chroma halo/outside origins must not recursively run");
        helper.assertTrue(!geo.finished(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, outside), "Geo outside origins must wait");
        if (fixtures.stream().anyMatch(f -> !queue.isDungeonFinished(f.type(), f.chunk()))
                || scopes.stream().anyMatch(id -> !population.finished(id))) {
            helper.runAfterDelay(1, () -> pollBounded(helper, fixtures, scopes, outside));
            return;
        }
        helper.assertTrue(geo.finished(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, fixtures.getFirst().chunk()), "shared scope completes Geo's queued work");
        for (Fixture f : fixtures) {
            helper.assertTrue(!level.getChunkSource().getForceLoadedChunks().contains(f.chunk().pack()), "bounded population must not add vanilla forced chunks");
            helper.assertTrue(level.getBlockEntity(f.controller()) instanceof TileEntityStructureController, "remote natural " + f.type() + " controller must place");
            if (f.type() == Type.SNOW) {
                var authored = reika.chromaticraft.auxiliary.structure.NBTStructureLoader.load(level,
                        reika.chromaticraft.auxiliary.structure.NBTStructureLoader.chromaTemplate("worldgen/overworld/snow"),
                        f.controller(), new BlockPos(8, 3, 6), state -> state);
                int air = 0;
                for (int i = 0; i < authored.getSize(); i++) {
                    BlockPos at = authored.getNthBlock(i);
                    if (authored.getBlockAt(at.getX(), at.getY(), at.getZ()) != Blocks.AIR) continue;
                    air++;
                    helper.assertTrue(level.getBlockState(at).isAir(), "bounded snow generation preserves room air at " + at);
                }
                helper.assertTrue(air > 450, "bounded snow regression covers the authored rooms");
            }
        }
        helper.succeed();
    }

    private static void boundedNeoForge(GameTestHelper helper) {
        var level = helper.getLevel();
        Fixture f = fixture(helper, Type.BURROW, 48, false);
        PostPopulationWorldgen.get(level).enqueueDungeon(f.type(), f.chunk(), f.seed());
        var task = new net.neoforged.neoforge.server.command.generation.GenerationTask(level, f.chunk().x(), f.chunk().z(), 1);
        task.run(new net.neoforged.neoforge.server.command.generation.GenerationTask.Listener() {
            @Override public void update(int ok, int error, int skipped, int total) {}
            @Override public void complete(int error) {}
        });
        var population = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level);
        long id = population.sessionId("neoforge", f.chunk().x() + "/" + f.chunk().z() + "/1");
        helper.assertTrue(id != 0 && population.admits(f.chunk()), "actual NeoForge task must open its area automatically");
        helper.assertTrue(!population.admits(new ChunkPos(f.chunk().x() + 2, f.chunk().z())), "native command radius is in chunks and has finite edges");
        pollPreloader(helper, f, id, null);
    }

    private static void boundedUnloaded(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos base = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        ChunkPos root = new ChunkPos(base.x() + 4096, base.z() - 4096);
        ChunkPos halo = new ChunkPos(root.x() + 1, root.z());
        helper.assertTrue(level.getChunkSource().getChunkNow(root.x(), root.z()) == null, "remote origin starts unloaded");
        var geo = reika.geostrata.level.GeoPostPopulation.get(level);
        geo.enqueue(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, root, root.getMinBlockX(), root.getMinBlockZ(), 47);
        geo.enqueue(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, halo, halo.getMinBlockX(), halo.getMinBlockZ(), 47);
        var population = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level);
        long id = population.begin("test-area", "unloaded/" + root, reika.dragonapi.instantiable.worldgen.PopulationScope.rectangle(
                root.x(), root.z(), root.x(), root.z()), false);
        pollUnloaded(helper, root, halo, id);
    }

    private static void pollUnloaded(GameTestHelper helper, ChunkPos root, ChunkPos halo, long id) {
        pumpPopulationMailbox(helper);
        var level = helper.getLevel();
        var geo = reika.geostrata.level.GeoPostPopulation.get(level);
        helper.assertTrue(!geo.finished(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, halo), "loading a job's halo must not admit halo jobs");
        if (!geo.finished(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, root)
                || !reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level).finished(id)) {
            helper.runAfterDelay(1, () -> pollUnloaded(helper, root, halo, id)); return;
        }
        helper.succeed();
    }

    private static void boundedChunky(GameTestHelper helper) {
        var level = helper.getLevel();
        Fixture f = fixture(helper, Type.CAVERN, 50, true);
        PostPopulationWorldgen.get(level).enqueueDungeon(f.type(), f.chunk(), f.seed());
        ChunkPos outside = new ChunkPos(f.chunk().x() + 2, f.chunk().z() + 2);
        var geo = reika.geostrata.level.GeoPostPopulation.get(level);
        geo.enqueue(reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, outside, outside.getMinBlockX(), outside.getMinBlockZ(), 47);
        try {
            var chunky = Class.forName("org.popcraft.chunky.ChunkyProvider").getMethod("get").invoke(null);
            var api = chunky.getClass().getMethod("getApi").invoke(chunky);
            boolean started = (boolean) Class.forName("org.popcraft.chunky.api.ChunkyAPI").getMethod("startTask",
                    String.class, String.class, double.class, double.class, double.class, double.class, String.class)
                    .invoke(api, level.dimension().identifier().toString(), "circle", f.chunk().getMinBlockX() + 8d,
                            f.chunk().getMinBlockZ() + 8d, 32d, 32d, "concentric");
            helper.assertTrue(started, "real Chunky task must start");
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException("Real Chunky integration test", ex); }
        helper.runAfterDelay(5, () -> {
            var population = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level);
            long id = population.latestSession("chunky");
            helper.assertTrue(id != 0, "actual Chunky task must open an observed-chunk scope");
            pollPreloader(helper, f, id, outside);
        });
    }

    private static void pollPreloader(GameTestHelper helper, Fixture f, long id, ChunkPos excluded) {
        pumpPopulationMailbox(helper);
        var level = helper.getLevel();
        var queue = PostPopulationWorldgen.get(level);
        var population = reika.dragonapi.auxiliary.trackers.BoundedPopulation.get(level);
        if (excluded != null) {
            helper.assertTrue(!population.admits(excluded), "Chunky's circular scope must exclude its bounding-square corners");
            helper.assertTrue(!reika.geostrata.level.GeoPostPopulation.get(level).finished(
                    reika.geostrata.level.GeoPostPopulation.Kind.ORE_CONVERSION, excluded), "excluded corner's deferred job stays pending");
        }
        if (!queue.isDungeonFinished(f.type(), f.chunk()) || !population.finished(id)) {
            helper.runAfterDelay(1, () -> pollPreloader(helper, f, id, excluded)); return;
        }
        helper.assertTrue(level.getBlockEntity(f.controller()) instanceof TileEntityStructureController, "preloader must finish the existing remote dungeon candidate");
        helper.assertTrue(!level.getChunkSource().getForceLoadedChunks().contains(f.chunk().pack()), "preloader completion must not leave a vanilla force-load entry");
        helper.succeed();
    }

    private static void pumpPopulationMailbox(GameTestHelper helper) {
        // GameTestServer fast-forwards without normal tick idle time. Give genuinely asynchronous
        // chunk loads bounded mailbox service and let background workers progress between fast ticks.
        for (int i = 0; i < 64 && helper.getLevel().getChunkSource().pollTask(); i++) {}
        java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
    }

    private static void pollDungeons(GameTestHelper helper, List<Fixture> fixtures) {
        var level = helper.getLevel();
        var queue = PostPopulationWorldgen.get(level);
        if (fixtures.stream().anyMatch(f -> !queue.isDungeonFinished(f.type(), f.chunk()))) {
            helper.runAfterDelay(1, () -> pollDungeons(helper, fixtures));
            return;
        }
        for (Fixture fixture : fixtures) {
            level.getChunkSource().updateChunkForced(fixture.chunk(), false);
            helper.assertTrue(level.getBlockEntity(fixture.controller()) instanceof TileEntityStructureController,
                    "natural " + fixture.type() + " must place at the source controller anchor " + fixture.controller());
            helper.assertTrue(DungeonZones.getGenStatus(level, fixture.type(), fixture.controller().getX(), fixture.controller().getZ())
                    == DungeonZones.Status.SUCCESS, "natural destination must be SUCCESS for " + fixture.type());
            if (fixture.type() == Type.OCEAN) {
                helper.assertTrue(level.getBlockState(fixture.controller().below(44)).isAir(),
                        "the pit must continue beyond 32 blocks and below Y=0 to its validated cave");
                helper.assertTrue(level.getBlockState(fixture.controller().offset(2, -44, 0)).getBlock()
                        instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield,
                        "deep pit slices must retain their shielding walls");
            }
            if (fixture.type() == Type.SNOW) {
                var authored = reika.chromaticraft.auxiliary.structure.NBTStructureLoader.load(level,
                        reika.chromaticraft.auxiliary.structure.NBTStructureLoader.chromaTemplate("worldgen/overworld/snow"),
                        fixture.controller(), new BlockPos(8, 3, 6), state -> state);
                int airCells = 0;
                for (int i = 0; i < authored.getSize(); i++) {
                    BlockPos cell = authored.getNthBlock(i);
                    if (authored.getBlockAt(cell.getX(), cell.getY(), cell.getZ()) == Blocks.AIR) {
                        airCells++;
                        helper.assertTrue(level.getBlockState(cell).isAir(),
                                "Snow outpost foundation supports must preserve authored interior air at " + cell);
                    }
                }
                helper.assertTrue(airCells > 450, "Snow air-space regression must inspect the complete authored room layout");
            }
        }
        helper.succeed();
    }

    private static void oceanRejections(GameTestHelper helper) {
        Fixture fixture = fixture(helper, Type.OCEAN, 7, false);
        var level = helper.getLevel();
        var feature = new OverworldStructureFeature(Type.OCEAN, true);
        helper.assertTrue(feature.tryGenerateInChunk(level, level, fixture.chunk().x(), fixture.chunk().z(),
                new SavedWorldgenRandom(fixture.seed()), 1) == null, "an ocean site without a cave must reject");
        helper.assertTrue(!(level.getBlockEntity(fixture.controller()) instanceof TileEntityStructureController),
                "a rejected ocean site must not leave a partial temple");
        helper.assertTrue(DungeonZones.Data.get(level).get(Type.OCEAN, fixture.chunk().x(), fixture.chunk().z())
                == DungeonZones.Status.INERT_GEN, "a failed retry must not advertise a GENERATING temple");
        // Add a cave, then block the last open end: both blocked entrances must still reject.
        for (BlockPos at : BlockPos.betweenClosed(fixture.controller().offset(-2, -48, -2), fixture.controller().offset(2, -46, 2)))
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(fixture.controller().offset(1, 3, 28), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(feature.tryGenerateInChunk(level, level, fixture.chunk().x(), fixture.chunk().z(),
                new SavedWorldgenRandom(fixture.seed()), 1) == null, "a buried mouth must reject even with clear source water probes");
        level.setBlock(fixture.controller().offset(1, 3, 28), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(fixture.controller().offset(1, 9, 28), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(feature.tryGenerateInChunk(level, level, fixture.chunk().x(), fixture.chunk().z(),
                new SavedWorldgenRandom(fixture.seed()), 1) == null, "both blocked source endcaps must reject");
        DungeonZones.markChunkStatus(level, fixture.chunk().x(), fixture.chunk().z(), Type.OCEAN, DungeonZones.Status.SUCCESS);
        feature.tryGenerateInChunk(level, level, fixture.chunk().x(), fixture.chunk().z(), new SavedWorldgenRandom(fixture.seed()), 1);
        helper.assertTrue(DungeonZones.Data.get(level).get(Type.OCEAN, fixture.chunk().x(), fixture.chunk().z())
                == DungeonZones.Status.SUCCESS, "an overlapping failed retry must preserve a recorded temple");
        helper.succeed();
    }

    private static void registry(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var placed = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        List<String> expected = new ArrayList<>(List.of("cave_crystal", "natural_pylon", "data_tower", "color_tree",
                "unknown_artefact", "warp_node", "luminous_cliffs_terrain", "luminous_cliffs_aux", "cave_indicator", "luma_patch",
                "glow_daisy", "glow_root",
                "energized_rock", "elemental_stones", "fused_crystals", "radiant_stone", "ender_stone", "fluid_stone",
                "firaxite_ore", "glowing_rock", "echostone", "lumenite_ore", "avolite_ore"));
        for (ChromaTieredPlants plant : ChromaTieredPlants.list) expected.add(plant.registryName());
        List<String> dungeons = List.of("natural_cavern", "natural_burrow", "natural_ocean", "natural_desert", "natural_snow", "natural_biome_fragment");
        expected.addAll(dungeons);
        for (String name : expected) helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath("chromaticraft", name))).isPresent(), "missing placed survival feature " + name);
        for (var holder : registries.lookupOrThrow(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD)) {
            HashSet<String> features = new HashSet<>();
            for (var step : holder.value().getGenerationSettings().features()) for (var feature : step)
                feature.unwrapKey().ifPresent(key -> features.add(key.identifier().toString()));
            for (String name : dungeons) helper.assertTrue(features.contains("chromaticraft:" + name),
                    "every overworld biome must admit planner-owned dungeon roots: " + holder + " lacks " + name);
            if (holder.is(ChromaBiomes.LUMINOUS_CLIFFS) || holder.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
                helper.assertTrue(features.contains("chromaticraft:glow_daisy") && features.contains("chromaticraft:glow_root"),
                        "both Cliffs biomes must receive the separate post-population flower patches");
        }
        helper.succeed();
    }
}
