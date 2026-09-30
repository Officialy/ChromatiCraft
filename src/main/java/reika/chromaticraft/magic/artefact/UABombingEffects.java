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

import reika.chromaticraft.magic.artefact.effects.DisplacementEffect;
import reika.chromaticraft.magic.artefact.effects.DrainPowerEffect;
import reika.chromaticraft.magic.artefact.effects.DropItemsEffect;
import reika.chromaticraft.magic.artefact.effects.LightningStrikeEffect;
import reika.dragonapi.instantiable.data.WeightedRandom;

/**
 * V33a {@code UABombingEffects}: the weighted misfortunes an Unknown Artefact visits on whoever or whatever
 * holds it. Carried, it rolls an entity effect (displacement 50, lightning 5); stored, a container effect
 * (drain nearby power 100, fling an item 20, explode 2).
 *
 * <p>With Applied Energistics present, V33a also drained any ME network whose unshielded storage cells held an
 * artefact: see {@code modinterface.ae.UAMESystemEffect}, registered from ChromatiCraft's constructor.
 */
public class UABombingEffects {

	public static final UABombingEffects instance = new UABombingEffects();

	public static final double TRADE_BOMBING_CHANCE = 20;

	private final WeightedRandom<UABombingEffect.BlockEffect> blockEffects = new WeightedRandom<>();
	private final WeightedRandom<UABombingEffect.EntityEffect> entityEffects = new WeightedRandom<>();

	private UABombingEffects() {
		blockEffects.addEntry(new DropItemsEffect(), 20);
		blockEffects.addEntry(new DrainPowerEffect(), 100);
		blockEffects.addEntry(new ExplodeEffect(), 2);

		entityEffects.addEntry(new LightningStrikeEffect(), 5);
		entityEffects.addEntry(new DisplacementEffect(), 50);
	}

	public void trigger(BlockEntity te, Container inv) {
		blockEffects.getRandomEntry().trigger(inv, te);
	}

	public void trigger(Entity e) {
		entityEffects.getRandomEntry().trigger(e);
	}

}
