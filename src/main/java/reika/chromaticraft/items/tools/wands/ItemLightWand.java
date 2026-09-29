/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.entity.EntityFlyingLight;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ItemLightWand} (LIGHTWAND): fires a flying light from just above the eyes. V33a listed a Blue energy
 * cost but never drained it, so a shot is free; that is kept. V33a also shipped no lang entry for
 * {@code chroma.lightwand}, so the item showed its raw key.
 */
public class ItemLightWand extends ItemWandBase {

	public ItemLightWand(Properties properties) {
		super(properties);
		this.addEnergyCost(CrystalElement.BLUE, 1);
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		if (!world.isClientSide()) {
			EntityFlyingLight e = new EntityFlyingLight(world, ep);
			Vec3 vec = ep.getLookAngle();
			// V33a: posY+vec.y+1.5 from the server player's feet.
			e.snapTo(ep.getX()+vec.x, ep.getY()+vec.y+1.5, ep.getZ()+vec.z, 0, 0);
			world.addFreshEntity(e);
			world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2, 0.7F);
		}
		return InteractionResult.SUCCESS;
	}

}
