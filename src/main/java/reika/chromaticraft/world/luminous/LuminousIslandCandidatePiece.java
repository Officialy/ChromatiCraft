package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import reika.chromaticraft.world.PostPopulationWorldgen;
import reika.chromaticraft.world.dimension.structure.ProximaStructurePieces;

/** Stores the original random stream until terrain and vegetation are complete around the candidate. */
public final class LuminousIslandCandidatePiece extends StructurePiece {
    private final CompoundTag candidate;

    LuminousIslandCandidatePiece(int x, int z, double scale, long seed, int minY, int maxY) {
        // Parent radius <=36, child radius <=12, decoration reach <=8. Include a full margin.
        super(ProximaStructurePieces.LUMINOUS_ISLAND_CANDIDATE.get(), 0,
                new BoundingBox(x - 64, minY, z - 64, x + 64, maxY, z + 64));
        candidate = new CompoundTag();
        candidate.putBoolean("Candidate", true);
        candidate.putInt("X", x); candidate.putInt("Z", z);
        candidate.putDouble("Scale", scale); candidate.putLong("Seed", seed);
    }

    public LuminousIslandCandidatePiece(CompoundTag tag) {
        super(ProximaStructurePieces.LUMINOUS_ISLAND_CANDIDATE.get(), tag);
        candidate = tag.getCompoundOrEmpty("Candidate").copy();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("Candidate", candidate.copy());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos reference) {
        ChunkPos origin = new ChunkPos(candidate.getIntOr("X", 0) >> 4, candidate.getIntOr("Z", 0) >> 4);
        if (chunk.equals(origin))
            PostPopulationWorldgen.get(level.getLevel()).enqueueIsland(origin,
                    candidate.getLongOr("Seed", 0), 6, candidate);
    }
}
