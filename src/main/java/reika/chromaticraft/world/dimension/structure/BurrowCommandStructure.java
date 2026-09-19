package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Vanilla-command entry for V33a's NBT-backed Crystal Burrow.
 *
 * <p>The structure is deliberately absent from every structure set: natural rarity, terrain checks
 * and optional-room rolls remain owned by {@code natural_burrow}. This entry exists so inspection
 * uses Minecraft's native {@code /place structure} command and always exposes both optional rooms.
 */
public final class BurrowCommandStructure extends Structure {

	public static final MapCodec<BurrowCommandStructure> CODEC = simpleCodec(BurrowCommandStructure::new);

	public BurrowCommandStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		// V33a receives a grass-surface coordinate and puts its controller at (-5,-8,-2). Keeping
		// that relationship makes the pavilion meet the terrain while the chambers remain buried.
		int surfaceX = context.chunkPos().getMiddleBlockX();
		int surfaceZ = context.chunkPos().getMiddleBlockZ();
		int surfaceY = context.chunkGenerator().getFirstFreeHeight(surfaceX, surfaceZ,
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		BlockPos controller = new BlockPos(surfaceX - 5, surfaceY - 9, surfaceZ - 2);
		return Optional.of(new GenerationStub(controller,
				builder -> builder.addPiece(new BurrowCommandPiece(controller))));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.BURROW_TYPE.get();
	}
}
