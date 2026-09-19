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

	public static final DeferredHolder<StructurePieceType, StructurePieceType> GLOW_CAVE =
			PIECES.register("glow_cave",
					() -> (StructurePieceType)(context, tag) -> new GlowCavePiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> MONUMENT =
			PIECES.register("monument",
					() -> (StructurePieceType)(context, tag) -> new MonumentPiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> LIGHT_PANEL =
			PIECES.register("light_panel",
					() -> (StructurePieceType)(context, tag) -> new LightPanelPiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> THREE_D_MAZE =
			PIECES.register("three_d_maze",
					() -> (StructurePieceType)(context, tag) -> new ThreeDMazePiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> MUSIC =
			PIECES.register("music",
					() -> (StructurePieceType)(context, tag) -> new MusicStructurePiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> GOL =
			PIECES.register("gol",
					() -> (StructurePieceType)(context, tag) -> new GOLStructurePiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> PORTAL =
			PIECES.register("portal",
					() -> (StructurePieceType)(context, tag) -> new PortalStructurePiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> CASTING_TEMPLE =
			PIECES.register("casting_temple",
					() -> (StructurePieceType)(context, tag) -> new CastingTemplePiece(tag));

	public static final DeferredHolder<StructurePieceType, StructurePieceType> BURROW =
			PIECES.register("burrow",
					() -> (StructurePieceType)(context, tag) -> new BurrowCommandPiece(tag));

	private ProximaStructurePieces() {}
}
