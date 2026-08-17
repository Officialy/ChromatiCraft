package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;

/** Proxima's structure piece types. */
public final class ProximaStructurePieces {

	public static final DeferredRegister<StructurePieceType> PIECES =
			DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE, ChromatiCraft.MODID);

	/**
	 * Implements {@code StructurePieceType} directly rather than its {@code ContextlessType} shorthand:
	 * that interface is package-private in vanilla, so a mod has to take the two-argument form and drop
	 * the serialization context it does not need.
	 */
	public static final DeferredHolder<StructurePieceType, StructurePieceType> GLASS_CLIFF =
			PIECES.register("glass_cliff",
					() -> (StructurePieceType)(context, tag) -> new GlassCliffPiece(tag));

	private ProximaStructurePieces() {}
}
