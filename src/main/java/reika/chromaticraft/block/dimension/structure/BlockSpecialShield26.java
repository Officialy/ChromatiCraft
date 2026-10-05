package reika.chromaticraft.block.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaShieldTypes;

/**
 * V33a {@code BlockSpecialShield} (SPECIALSHIELD, "Shielding Stone Variant"): the structure shield drawn with
 * its own {@code basic/specialshield_N} art by {@code SpecialShieldRenderer} (see {@code SpecialShieldModel}).
 * It inherits every rule of {@link BlockStructureShield}: hardness, the reinforced bit, piston immobility, the
 * TNT trap and dropping the plain <em>structure</em> shield of the same material. Cloak and Stone draw unlit
 * with bright edge glints; Cracks draws unlit; Cobble has two alternating overlays.
 *
 * <p>Only five materials exist: V33a's jar ships art for Cloak, Stone, Cobble, Glass and Cracks, and nothing
 * upstream ever places the other three, so they are not registered rather than drawn with a missing texture.
 * V33a's {@code onNeighborBlockChange} forwarded to the owning generator's {@code onBlockUpdate}; Crystal
 * Interference (RAYBLEND) is the only generator that overrides it, and that hook arrives with that puzzle.
 */
public final class BlockSpecialShield26 extends BlockStructureShield {

	public BlockSpecialShield26(BlockBehaviour.Properties properties, ChromaShieldTypes type) {
		super(properties, type);
	}

	/** Materials V33a shipped art for. */
	public static boolean hasArt(ChromaShieldTypes type) {
		return type == ChromaShieldTypes.CLOAK || type == ChromaShieldTypes.STONE || type == ChromaShieldTypes.COBBLE
				|| type == ChromaShieldTypes.GLASS || type == ChromaShieldTypes.CRACKS;
	}

	/** V33a {@code useNoLighting}: metadata 0, 1 and 7 draw at full brightness. */
	public static boolean useNoLighting(ChromaShieldTypes type) {
		return type == ChromaShieldTypes.CLOAK || type == ChromaShieldTypes.STONE || type == ChromaShieldTypes.CRACKS;
	}

	/** V33a {@code shouldSideBeRendered}: a face is hidden only against the identical shield. */
	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
		return neighbour == state;
	}

	/** V33a {@code onBlockActivated}: Cloak and Stone swallow the click. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		return useNoLighting(getShieldType()) && getShieldType() != ChromaShieldTypes.CRACKS
				? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/** True for any special shield, whatever its material: DragonAPI's edge renderer matched without metadata. */
	public static boolean isSpecialShield(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof BlockSpecialShield26;
	}
}
