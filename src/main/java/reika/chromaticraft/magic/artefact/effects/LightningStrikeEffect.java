/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.artefact.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;

import reika.chromaticraft.magic.artefact.UABombingEffect;

/** V33a {@code LightningStrikeEffect}: lightning on the artefact's carrier. */
public class LightningStrikeEffect extends UABombingEffect.EntityEffect {

	@Override
	public void trigger(Entity e) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(e.level(), EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(e.getX(), e.getY(), e.getZ());
			e.level().addFreshEntity(bolt);
		}
	}

}
