package reika.chromaticraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.world.*;
import reika.chromaticraft.world.OverworldStructureFeature.Type;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature;
import reika.chromaticraft.world.luminous.LuminousIslandPiece;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Tests the world writes and asynchronous admission boundary, as well as source retry order. */
final class WorldgenParityGameTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        ChromaGameTests.register(event, environment, "worldgen_parity_retry_plan", 20, WorldgenParityGameTests::retryPlan);
        ChromaGameTests.register(event, environment, "worldgen_parity_outer_square", 240, WorldgenParityGameTests::outerSquare);
        ChromaGameTests.register(event, environment, "worldgen_parity_island_collision", 30, WorldgenParityGameTests::islandCollision);
        ChromaGameTests.register(event, environment, "worldgen_parity_carver_pipeline", 10000, WorldgenParityGameTests::carverPipeline);
        ChromaGameTests.register(event, environment, "worldgen_parity_island_candidate", 3000, WorldgenParityGameTests::islandCandidate);
    }

    private static void retryPlan(GameTestHelper helper) {
        ChunkPos origin = new ChunkPos(-11, 7);
        for (Type type : Type.values()) {
            List<ChunkPos> chunks = DungeonRetryPlan.chunks(type, origin);
            int radius = DungeonZones.keepTryingRange(type) / 16;
            int expected = 1;
            for (int r = 1; r <= radius; r++) expected += (2 * r + 1) * (2 * r + 1) - 1;
            helper.assertTrue(chunks.size() == expected, "retry count must include repeated inner squares for " + type);
            helper.assertTrue(chunks.getFirst().equals(origin), "the flagged chunk must run first");
            helper.assertTrue(chunks.getLast().equals(new ChunkPos(origin.x() + radius, origin.z() + radius)),
                    "the final positive corner must be visited for " + type);
            long repeats = chunks.stream().filter(p -> p.equals(new ChunkPos(origin.x() - 1, origin.z() - 1))).count();
            helper.assertTrue(repeats == radius, "each widening square must revisit its inner corners");
        }
        SavedWorldgenRandom random = new SavedWorldgenRandom(0xD06E0L);
        var reference = new java.util.Random(0xD06E0L);
        for (int i = 0; i < 97; i++)
            helper.assertTrue(random.nextInt(37) == reference.nextInt(37), "random stream must match V33a Java Random");
        for (int i = 0; i < 97; i++)
            helper.assertTrue(random.nextDouble() == reference.nextDouble(), "double draws must retain all 53 bits");
        helper.assertTrue(random.nextGaussian() == reference.nextGaussian(),
                "Gaussian draws must match V33a Java Random");
        SavedWorldgenRandom restored = SavedWorldgenRandom.load(random.save());
        helper.assertTrue(restored.nextGaussian() == random.nextGaussian(), "cached Gaussian must survive reload");
        for (int i = 0; i < 97; i++)
            helper.assertTrue(restored.nextLong() == random.nextLong(), "retry stream must continue exactly after reload");
        helper.succeed();
    }

    /** Only the third search square has host rock. The real queued feature must reach and build there. */
    private static void outerSquare(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        List<ChunkPos> chunks = DungeonRetryPlan.chunks(Type.CAVERN, origin);
        ChunkPos target = new ChunkPos(origin.x() - 3, origin.z() - 3);
        int index = chunks.indexOf(target);
        int tries = ChromaOptions.getStructureTriesPerChunk();
        long seed = 0x0A17E25L;
        SavedWorldgenRandom preview = new SavedWorldgenRandom(seed);
        for (int i = 0; i < index * tries; i++) {
            preview.nextInt(16); preview.nextInt(16); preview.nextInt(40);
        }
        int x = target.getMinBlockX() + preview.nextInt(16);
        int z = target.getMinBlockZ() + preview.nextInt(16);
        int y = 10 + preview.nextInt(40);
        // A tight host prism: neighbouring-column probes fail rather than stealing this fixture.
        BlockPos min = new BlockPos(x - 7, 7, z - 5);
        BlockPos max = new BlockPos(x + 6, 55, z + 5);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        var queue = PostPopulationWorldgen.get(level);
        queue.enqueueDungeon(Type.CAVERN, origin, seed);
        var snapshot = PostPopulationWorldgen.CODEC.encodeStart(NbtOps.INSTANCE, queue).getOrThrow();
        var restored = PostPopulationWorldgen.CODEC.parse(NbtOps.INSTANCE, snapshot).getOrThrow();
        helper.assertTrue(PostPopulationWorldgen.CODEC.encodeStart(NbtOps.INSTANCE, restored).getOrThrow().equals(snapshot),
                "pending footprint, candidate cursor and random stream must round-trip");
        boolean wasForced = level.getChunkSource().getForceLoadedChunks().contains(origin.pack());
        level.getChunkSource().updateChunkForced(origin, true);
        helper.runAfterDelay(220, () -> {
            if (!wasForced) level.getChunkSource().updateChunkForced(origin, false);
            helper.assertTrue(queue.isDungeonFinished(Type.CAVERN, origin), "asynchronous retry job must finish");
            helper.assertTrue(level.getBlockEntity(new BlockPos(x, y, z)) instanceof
                    reika.chromaticraft.tileentity.TileEntityStructureController,
                    "a site 48 blocks away must receive the real shrine controller");
            helper.assertTrue(DungeonZones.getGenStatus(level, Type.CAVERN, x, z) == DungeonZones.Status.SUCCESS,
                    "the destination chunk must retain SUCCESS");
            helper.succeed();
        });
    }

    private static LuminousIslandPiece island(BlockPos center, long seed) {
        CompoundTag params = new CompoundTag();
        params.putInt("OX", center.getX()); params.putInt("OY", center.getY()); params.putInt("OZ", center.getZ());
        params.putDouble("Inner", 4); params.putDouble("Outer", 6);
        params.putDouble("MaxT", 8); params.putInt("Degree", 4); params.putLong("CurveSeed", seed);
        CompoundTag piece = new CompoundTag();
        piece.put("Island", params);
        piece.store("BB", BoundingBox.CODEC, new BoundingBox(center.getX() - 8, center.getY() - 16, center.getZ() - 8,
                center.getX() + 8, center.getY() + 8, center.getZ() + 8));
        return new LuminousIslandPiece(piece);
    }

    private static void islandCollision(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        BlockPos center = new BlockPos(origin.getMinBlockX() + 14,
                helper.absolutePos(new BlockPos(0, 170, 0)).getY(), origin.getMinBlockZ() + 14);
        LuminousIslandPiece island = island(center, 0x15A1DL);
        BlockPos obstruction = center.offset(3, 0, 0);
        helper.assertTrue(!ChunkPos.containing(obstruction).equals(ChunkPos.containing(center)),
                "collision fixture must occupy a different chunk from the island origin");
        for (var block : List.of(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.WATER)) {
            // Surface contours depend on absolute world coordinates. A full column intersects
            // the island at every random GameTest location; a fixed Y may lie beneath its taper.
            for (int dy = -16; dy <= 8; dy++) level.setBlock(obstruction.offset(0, dy, 0), block.defaultBlockState(), 2);
            helper.assertTrue(!island.placeAfterPopulation(level), "whole island must reject a " + block + " collision");
            helper.assertTrue(level.getBlockState(center.below(3)).isAir(), "rejection must not leave a partial island");
            helper.assertTrue(level.getBlockState(obstruction).is(block), "collision must preserve existing terrain/vegetation");
            for (int dy = -16; dy <= 8; dy++) level.setBlock(obstruction.offset(0, dy, 0), Blocks.AIR.defaultBlockState(), 2);
        }
        helper.assertTrue(island.placeAfterPopulation(level), "clear populated air must accept the island");
        boolean painted = false;
        for (int dy = -16; dy <= 8; dy++) painted |= level.getBlockState(obstruction.offset(0, dy, 0)).is(ChromaBlocks.CLIFF_STONE.get());
        helper.assertTrue(painted, "accepted island must paint its body in the neighbouring chunk");
        helper.assertTrue(!island(center.offset(2, 0, 0), 0x15A1E).placeAfterPopulation(level),
                "a neighbouring-chunk island must reject an overlap with the first island");
        helper.succeed();
    }

    /** A saved structure candidate must replay into the real island body after population. */
    private static void islandCandidate(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(12, 170, 12));
        long seed = 0;
        for (; seed < 1000; seed++) {
            var random = new java.util.Random(seed);
            reika.dragonapi.libraries.java.ReikaRandomHelper.doWithChance(33, random);
            random.nextInt(16); random.nextInt(16);
            if (random.nextInt(6) == 0) break;
        }
        CompoundTag candidate = new CompoundTag();
        candidate.putBoolean("Candidate", true);
        candidate.putInt("X", center.getX()); candidate.putInt("Z", center.getZ());
        candidate.putDouble("Scale", 1); candidate.putLong("Seed", seed);
        CompoundTag encoded = new CompoundTag();
        encoded.put("Candidate", candidate);
        encoded.store("BB", BoundingBox.CODEC, new BoundingBox(center.getX() - 64, level.getMinY(), center.getZ() - 64,
                center.getX() + 64, level.getMaxY() - 1, center.getZ() + 64));
        var piece = new reika.chromaticraft.world.luminous.LuminousIslandCandidatePiece(encoded);
        ChunkPos origin = ChunkPos.containing(center);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                net.minecraft.util.RandomSource.create(0), piece.getBoundingBox(), origin, center);
        var queue = PostPopulationWorldgen.get(level);
        boolean wasForced = level.getChunkSource().getForceLoadedChunks().contains(origin.pack());
        level.getChunkSource().updateChunkForced(origin, true);
        long key = seed;
        helper.runAfterDelay(1, () -> pollCandidate(helper, queue, origin, key, center, wasForced));
    }

    private static void pollCandidate(GameTestHelper helper, PostPopulationWorldgen queue, ChunkPos origin,
            long key, BlockPos center, boolean wasForced) {
        if (!queue.isIslandFinished(origin, key)) {
            helper.runAfterDelay(1, () -> pollCandidate(helper, queue, origin, key, center, wasForced));
            return;
        }
        var level = helper.getLevel();
        if (!wasForced) level.getChunkSource().updateChunkForced(origin, false);
        int stone = 0;
        for (int y = 60; y < 260; y++)
            if (level.getBlockState(new BlockPos(center.getX(), y, center.getZ())).is(ChromaBlocks.CLIFF_STONE.get())) stone++;
        helper.assertTrue(stone > 0, "serialized candidate must paint a floating island through the saved queue");
        helper.succeed();
    }

    /** Generate real noise terrain through the mixin, then observe carver holes in raised plateau rock. */
    private static void carverPipeline(GameTestHelper helper) {
        var level = helper.getLevel();
        var settings = level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ChromaBiomes.LUMINOUS_CLIFFS);
        var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), settings);
        long seed = 0xC11FF5L;
        var random = RandomState.create(level.registryAccess().lookupOrThrow(Registries.NOISE), seed, settings.value());
        var shaper = GlowingCliffsColumnShaper.forSeed(seed);
        var sampler = shaper.new Sampler((x, z) -> GlowingCliffsColumnShaper.BiomeKind.MAIN);
        List<ProtoChunk> chunks = new java.util.ArrayList<>();
        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();
        // Pure generated chunks: neither the test world's live terrain nor its seed is changed.
        for (int i = 0; i < 16; i++) {
            ChunkPos pos = new ChunkPos(i * 7, i * 3);
            ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                    PalettedContainerFactory.create(level.registryAccess()), null);
            chunks.add(chunk);
            futures.add(generator.buildTerrain(chunk, Blender.empty(), random, level.structureManager(),
                    new BiomeManager((x, y, z) -> biome, seed), null, Set.of(biome)));
        }
        CompletableFuture<?> all = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        helper.runAtTickTime(1, () -> pollCarvers(helper, all, chunks, sampler, generator, random));
    }

    private static void pollCarvers(GameTestHelper helper, CompletableFuture<?> all, List<ProtoChunk> chunks,
            GlowingCliffsColumnShaper.Sampler sampler, NoiseBasedChunkGenerator generator, RandomState random) {
        if (!all.isDone()) {
            helper.runAfterDelay(1, () -> pollCarvers(helper, all, chunks, sampler, generator, random));
            return;
        }
        all.join();
        int raised = 0, holes = 0;
        for (ProtoChunk chunk : chunks) {
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getPos().getMinBlockX() + dx, z = chunk.getPos().getMinBlockZ() + dz;
                int original = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), random) - 1;
                var cells = sampler.shaper().cells(sampler.column(x, z), x, z);
                for (int y = Math.max(original + 2, 63); y < cells.length; y++) {
                    if (cells[y] == GlowingCliffsColumnShaper.Cell.STONE) {
                        raised++;
                        if (chunk.getBlockState(new BlockPos(x, y, z)).isAir()) holes++;
                    }
                }
            }
            helper.assertTrue(!LuminousCliffsTerrainFeature.shapeBeforeCarvers(chunk, generator, random, random.seed()),
                    "a later terrain pass must not refill caves in a shaped chunk");
        }
        helper.assertTrue(raised > 0, "fixture must include raised plateau stone");
        helper.assertTrue(holes > 0, "vanilla carvers must cut raised cliff stone above the old ground");
        helper.succeed();
    }
}
