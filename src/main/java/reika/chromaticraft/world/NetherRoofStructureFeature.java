package reika.chromaticraft.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.TagValueInput;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * V33a's five structures on top of the Nether roof, now placed from canonical structure NBT.
 * The natural variant retains the original 200:15:30:24:8 weighted choice; named variants are
 * command/debug seams so each layout can be tested independently with {@code /place feature}.
 */
public final class NetherRoofStructureFeature extends Feature<NoneFeatureConfiguration> {

    public enum Type {
        HUT("hut", 200), TEMPLE("temple", 15), MAZE("maze", 30), SPIRAL("spiral", 24), DIORAMA("diorama", 8);

        private final Identifier template;
        private final int weight;

        Type(String name, int weight) {
            template = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "worldgen/nether/" + name);
            this.weight = weight;
        }

        static Type random(RandomSource random) {
            int total = 0;
            for (Type type : values())
                total += type.weight;
            int value = random.nextInt(total);
            for (Type type : values()) {
                value -= type.weight;
                if (value < 0)
                    return type;
            }
            throw new IllegalStateException("Unreachable Nether structure weight selection");
        }
    }

    private final Type forcedType;

    public NetherRoofStructureFeature() {
        this(null);
    }

    public NetherRoofStructureFeature(Type forcedType) {
        super(NoneFeatureConfiguration.CODEC);
        this.forcedType = forcedType;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        if (world.getLevel().dimension() != Level.NETHER)
            return false;
        Type type = forcedType != null ? forcedType : Type.random(context.random());
        BlockPos base = new BlockPos(context.origin().getX(), 128, context.origin().getZ());
        List<BlockPos> placed = NBTStructureLoader.place(world, type.template, base, BlockPos.ZERO,
                state -> state, 3);
        initializeContents(world, base, type, context.random());
        return !placed.isEmpty();
    }

    private static void initializeContents(WorldGenLevel world, BlockPos base, Type type, RandomSource random) {
        switch (type) {
            case HUT -> loot(world, base.offset(4, 0, 4), BuiltInLootTables.SIMPLE_DUNGEON, random);
            case TEMPLE -> {
                loot(world, base.offset(12, 0, 14), BuiltInLootTables.DESERT_PYRAMID, random,
                        ProgressStage.NETHERSTRUCT);
                loot(world, base.offset(13, 0, 14), BuiltInLootTables.DESERT_PYRAMID, random,
                        ProgressStage.NETHERSTRUCT);
            }
            case MAZE -> {
                boolean firstBlaze = random.nextBoolean();
                EntityType<?> first = firstBlaze ? EntityTypes.BLAZE : EntityTypes.ZOMBIFIED_PIGLIN;
                EntityType<?> second = firstBlaze ? EntityTypes.ZOMBIFIED_PIGLIN : EntityTypes.BLAZE;
                mazeSpawner(world, base.offset(1, 1, 14), first, random);
                mazeSpawner(world, base.offset(7, 1, 1), second, random);
                loot(world, base.offset(11, 1, 14), BuiltInLootTables.JUNGLE_TEMPLE, random);
            }
            case SPIRAL -> {
                loot(world, base.offset(9, 1, 9), BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER, random);
                loot(world, base.offset(1, 1, 5), BuiltInLootTables.JUNGLE_TEMPLE, random);
                loot(world, base.offset(5, 1, 17), BuiltInLootTables.JUNGLE_TEMPLE, random);
                loot(world, base.offset(13, 1, 1), BuiltInLootTables.JUNGLE_TEMPLE, random);
                loot(world, base.offset(17, 1, 13), BuiltInLootTables.JUNGLE_TEMPLE, random);
            }
            case DIORAMA -> loot(world, base.offset(27, 5, 19), BuiltInLootTables.DESERT_PYRAMID, random,
                    ProgressStage.NETHERSTRUCT);
        }
    }

    private static void loot(WorldGenLevel world, BlockPos pos,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table,
            RandomSource random, ProgressStage... progress) {
        if (world.getBlockEntity(pos) instanceof TileEntityLootChest chest) {
            chest.setLootTable(table, random.nextLong());
            for (ProgressStage stage : progress)
                chest.addProgress(stage);
        }
    }

    private static void spawner(WorldGenLevel world, BlockPos pos, EntityType<?> entity, RandomSource random) {
        if (world.getBlockState(pos).is(Blocks.SPAWNER)
                && world.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(entity, random);
            // Diorama's four spawners all share these timings; only the nearby cap varies for Ghasts.
            reloadSpawner(world, spawner, 0, 1, 1, entity == EntityTypes.GHAST ? 1 : 4, 12);
        }
    }

    private static void mazeSpawner(WorldGenLevel world, BlockPos pos, EntityType<?> entity, RandomSource random) {
        if (world.getBlockState(pos).is(Blocks.SPAWNER)
                && world.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(entity, random);
            // V33a configures these from the randomized mob, not from a fixed physical location.
            boolean piglin = entity == EntityTypes.ZOMBIFIED_PIGLIN;
            reloadSpawner(world, spawner, 0, piglin ? 2 : 20, 1, piglin ? 24 : 8, 12);
        }
    }

    private static void reloadSpawner(WorldGenLevel world, SpawnerBlockEntity spawner,
            int minDelay, int maxDelay, int delay, int maxNearby, int playerRange) {
        CompoundTag tag = spawner.saveCustomOnly(world.registryAccess());
        tag.putShort("MinSpawnDelay", (short)minDelay);
        tag.putShort("MaxSpawnDelay", (short)maxDelay);
        tag.putShort("Delay", (short)delay);
        tag.putShort("MaxNearbyEntities", (short)maxNearby);
        tag.putShort("RequiredPlayerRange", (short)playerRange);
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                org.slf4j.LoggerFactory.getLogger(NetherRoofStructureFeature.class))) {
            spawner.loadCustomOnly(TagValueInput.create(
                    reporter.forChild(spawner.problemPath()), world.registryAccess(), tag));
        }
        spawner.setChanged();
    }
}
