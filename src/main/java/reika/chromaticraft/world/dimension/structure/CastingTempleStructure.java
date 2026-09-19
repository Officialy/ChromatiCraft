package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Command-placeable NBT form of one of the three player-built casting multiblocks. */
public final class CastingTempleStructure extends Structure {

	public static final MapCodec<CastingTempleStructure> L1_CODEC =
			simpleCodec(settings -> new CastingTempleStructure(settings, 1));
	public static final MapCodec<CastingTempleStructure> L2_CODEC =
			simpleCodec(settings -> new CastingTempleStructure(settings, 2));
	public static final MapCodec<CastingTempleStructure> L3_CODEC =
			simpleCodec(settings -> new CastingTempleStructure(settings, 3));

	private final int tier;

	public CastingTempleStructure(StructureSettings settings, int tier) {
		super(settings);
		this.tier = tier;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int x = context.chunkPos().getMiddleBlockX();
		int z = context.chunkPos().getMiddleBlockZ();
		int radius = tier == 3 ? 8 : 6;
		int y = context.heightAccessor().getMinY();
		// Command structures are demonstrations, not buried worldgen ruins. Put the entire footprint
		// above the highest terrain cell instead of replacing the top block beneath the chunk centre.
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				y = Math.max(y, context.chunkGenerator().getFirstFreeHeight(x + dx, z + dz,
						Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
			}
		}
		BlockPos support = new BlockPos(x, y, z);
		return Optional.of(new GenerationStub(support,
				builder -> builder.addPiece(new CastingTemplePiece(support, tier))));
	}

	@Override
	public StructureType<?> type() {
		return switch (tier) {
			case 1 -> ProximaStructures.CASTING_TEMPLE_L1_TYPE.get();
			case 2 -> ProximaStructures.CASTING_TEMPLE_L2_TYPE.get();
			case 3 -> ProximaStructures.CASTING_TEMPLE_L3_TYPE.get();
			default -> throw new IllegalStateException("Invalid casting tier " + tier);
		};
	}
}
