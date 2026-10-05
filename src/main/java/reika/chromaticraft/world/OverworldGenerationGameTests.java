package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
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
        helper.assertTrue(WarpNodeFeature.minimumHeight(40, true) == 128,
                "cliff shores must retain their raised warp-node floor");
        helper.assertTrue(WarpNodeFeature.minimumHeight(40, false) == 88,
                "ordinary low terrain retains the source 48-block clearance");
        helper.assertTrue(WarpNodeFeature.minimumHeight(150, true) == 198,
                "high plateaus must still retain 48 blocks of clearance");
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
            // including the discarded +/-25 height roll before a root chooses 4..80.
            long seed = 1;
            while (new SavedWorldgenRandom(seed).nextInt(5) == 0) seed++;
            var reference = new SavedWorldgenRandom(seed);
            reference.nextInt(5);
            int x = origin.getX() + reference.nextInt(16), z = origin.getZ() + reference.nextInt(16);
            int initial = 101 + reference.nextInt(51) - 25;
            int y = root ? (4 + reference.nextInt(77) < 40 ? 39 : 99) : (initial < 100 ? 41 : 101);
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
            case CAVERN -> new BlockPos(x, 10 + random.nextInt(40), z);
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
