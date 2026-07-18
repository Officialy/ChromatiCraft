/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.structure.antfarm;


import reika.chromaticraft.base.StructurePiece;
import reika.chromaticraft.world.dimension.structure.AntFarmGenerator;
import reika.dragonapi.instantiable.worldgen.ChunkSplicedGenerationCache;
import reika.dragonapi.libraries.reikadirectionhelper.CubeDirections;


public class AntFarmRoom extends StructurePiece<AntFarmGenerator> {

	public final CubeDirections direction;

	protected AntFarmRoom(AntFarmGenerator s, CubeDirections dir) {
		super(s);
		direction = dir;
	}

	@Override
	public void generate(ChunkSplicedGenerationCache world, int x, int y, int z) {

	}

}
