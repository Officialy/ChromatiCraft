/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.artefact;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** V33a {@code UABombingEffect}: what an Unknown Artefact does to its holder or its container. */
public abstract class UABombingEffect {

	public static abstract class EntityEffect extends UABombingEffect {

		public abstract void trigger(Entity e);

	}

	public static abstract class BlockEffect extends UABombingEffect {

		public abstract void trigger(Container inv, BlockEntity te);

	}

}
