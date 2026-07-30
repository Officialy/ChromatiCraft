/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base;

import java.util.Random;

import net.minecraft.world.level.block.Block;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.base.StructureBase;

/**
 * Base for ChromatiCraft's multiblock structure definitions (see {@link reika.chromaticraft.registry.ChromaStructures}).
 *
 * <p>Deferred (re-add with their blocks): the structure-shield ({@code STRUCTSHIELD}) and loot-chest
 * ({@code LOOTCHEST}) helpers — those blocks aren't ported, and only worldgen structures use them.
 */
public abstract class ChromaStructureBase extends StructureBase {

	protected static final Block crystalstone = ChromaBlocks.PYLONSTRUCT.get();

	protected Random rand = new Random();

	public void setRand(Random r) {
		rand = r;
	}

	public void resetToDefaults() {
		this.setRand(new Random());
	}
}
