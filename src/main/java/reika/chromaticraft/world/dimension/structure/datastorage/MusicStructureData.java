/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.structure.datastorage;

import java.util.HashMap;

import reika.chromaticraft.base.DimensionStructureGenerator;
import reika.chromaticraft.base.StructureData;
import reika.chromaticraft.world.dimension.structure.MusicPuzzleGenerator;

public class MusicStructureData extends StructureData {

	public MusicStructureData(DimensionStructureGenerator gen) {
		super(gen);
	}

	@Override
	public void load(HashMap<String, Object> map) {
		MusicPuzzleGenerator mus = (MusicPuzzleGenerator)generator;

	}

}
