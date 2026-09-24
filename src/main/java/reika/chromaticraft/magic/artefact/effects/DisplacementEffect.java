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

import reika.chromaticraft.magic.artefact.UABombingEffect;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/** V33a {@code DisplacementEffect}: a random shove, always somewhat upward. */
public class DisplacementEffect extends UABombingEffect.EntityEffect {

	@Override
	public void trigger(Entity e) {
		e.setDeltaMovement(e.getDeltaMovement().add(ReikaRandomHelper.getRandomPlusMinus(0, 0.5),
				ReikaRandomHelper.getRandomPlusMinus(0.5, 0.25), ReikaRandomHelper.getRandomPlusMinus(0, 0.5)));
		e.hurtMarked = true;
	}

}
