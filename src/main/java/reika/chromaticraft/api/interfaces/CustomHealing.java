/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.chromaticraft.api.AdjacencyUpgradeAPI;

/** Supply an instance of this to the {@link AdjacencyUpgradeAPI} to specify custom healing core behavior on your Block or BlockEntity. */
public interface CustomHealing extends CustomAdjacencyHandler {

	/** Whether to apply this effect clientside as well as serverside. Usually false. */
    boolean runOnClient();

	interface CustomTileHealing extends CustomHealing {
		/** This will be called once per tick, with 'te' being your TE and 'tier' being the tier of the core, 0-7 inclusive. */
        void tick(BlockEntity te, int tier);
	}

	interface CustomBlockHealing extends CustomHealing {
		/** This will be called once per tick, with your block's position and 'tier' being the tier of the core, 0-7 inclusive. */
        void tick(Level world, int x, int y, int z, int tier);
	}

}
