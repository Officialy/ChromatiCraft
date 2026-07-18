/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.structure.shiftmaze.part;

import reika.chromaticraft.base.DimensionStructureGenerator;
import reika.chromaticraft.base.StructurePiece;
import reika.dragonapi.instantiable.worldgen.ChunkSplicedGenerationCache;

public abstract class StructurePieceChainable<V extends DimensionStructureGenerator> extends StructurePiece {

	public StructurePieceChainable(V s) {
		super(s);
	}

	public int generateAndMoveCursor(ChunkSplicedGenerationCache world, int x, int y, int z, int cursor) {
		this.generate(world, x, y, z);
		return cursor + this.getCursorStepWidth();
	}

	public abstract int getCursorStepWidth();

}
