/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.biome;

import reika.chromaticraft.base.ChromaDimensionBiome;
import reika.chromaticraft.entity.EntityTunnelNuker;
import reika.chromaticraft.world.dimension.chromadimensionmanager.Biomes;


public class BiomeGenCentral extends ChromaDimensionBiome {

	public BiomeGenCentral(int id, String n, Biomes t) {
		super(id, n, t);
	}

	@Override
	protected void initSpawnRules() {
		spawnableCaveCreatureList.add(new SpawnListEntry(EntityTunnelNuker.class, 1, 1, 1));
	}

}
