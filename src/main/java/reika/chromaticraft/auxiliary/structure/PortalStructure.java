package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

/**
 * Canonical NBT-backed port of V33a's 15x10x15 Portal Rift structure.
 *
 * <p>The controller is the centre of the 3x3 Portal Rift pad on the bottom layer, template
 * {@code (7,0,7)} — V33a builds from {@code i = x-7, j = y+0, k = z-7}.
 *
 * <p>Two of the original cell contracts cannot be expressed as an exact state comparison, so they
 * are restored here as an explicit {@link NBTStructureLoader.CellRule} rather than being lost:
 *
 * <ul>
 * <li>{@code array.setBlock(ch)} built a {@code BlockKey} with wildcard metadata, i.e. <em>any</em>
 *     Luma level. Those are the fountain basin cells, which a player fills with buckets and which
 *     the flow then partially drains; requiring a source there would make the built structure
 *     invalidate itself.</li>
 * <li>{@code array.setBlock(ch, 1)} required legacy quanta 1 — one step below full on a fluid whose
 *     {@code BlockEtherealLuma} declared {@code setQuantaPerBlock(16)}. 26.2's {@code LiquidBlock}
 *     has eight levels, so the legacy index has no counterpart; what the call distinguishes is
 *     flowing Luma from a source, which is what the sleeve around the fountain pillar actually is.
 *     Those cells therefore require Luma at any non-source level.</li>
 * </ul>
 *
 * <p>{@code array.setFluid(er)} is a V33a {@code FluidCheck} with {@code needsSourceBlock}, so the
 * Liquid Ender basins keep the ordinary exact-state path against the template's level-0 cells.
 *
 * <p>The eight vanilla Ender Crystals V33a additionally requires are not part of the template — they
 * are entities, they sit outside the 15x15 footprint at {@code (+-5,+5,-+9)} / {@code (+-9,+5,-+5)},
 * and their bedrock pads exist only in the handbook display. {@code TileEntityCrystalPortal} checks
 * them alongside this array, exactly as {@code getEntities()} did.
 */
public final class PortalStructure extends ChromaStructureBase {

	public static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/portal");
	public static final BlockPos ANCHOR = new BlockPos(7, 0, 7);

	/** V33a getEntities(): one vanilla Ender Crystal in each of these cells, relative to the pad. */
	public static final BlockPos[] ENDER_CRYSTALS = {
			new BlockPos(-5, 5, -9), new BlockPos(-5, 5, 9), new BlockPos(5, 5, -9), new BlockPos(5, 5, 9),
			new BlockPos(-9, 5, -5), new BlockPos(-9, 5, 5), new BlockPos(9, 5, -5), new BlockPos(9, 5, 5),
	};

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR,
				state -> state, true, PortalStructure::lumaContract);
	}

	private static BlockCheck lumaContract(BlockPos relative, BlockState state) {
		if (!state.is(ChromaBlocks.LUMA.get()))
			return null;
		boolean source = state.getValue(LiquidBlock.LEVEL) == 0;
		FilledBlockArray.MultiKey levels = new FilledBlockArray.MultiKey();
		for (int level : LiquidBlock.LEVEL.getPossibleValues()) {
			if (source || level != 0)
				levels.add(new BlockKey(ChromaBlocks.LUMA.get().defaultBlockState()
						.setValue(LiquidBlock.LEVEL, level)));
		}
		return levels;
	}
}
