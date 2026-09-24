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

import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.magic.artefact.UABombingEffect;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/** V33a {@code DropItemsEffect}: flings one random non-artefact stack out of the container. */
public class DropItemsEffect extends UABombingEffect.BlockEffect {

	@Override
	public void trigger(Container inv, BlockEntity te) {
		ArrayList<Integer> slots = new ArrayList<>();
		for (int i = 0; i < inv.getContainerSize(); i++)
			slots.add(i);
		Collections.shuffle(slots);
		for (int slot : slots) {
			ItemStack in = inv.getItem(slot);
			if (!in.isEmpty() && !in.is(ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get())) {
				var pos = te.getBlockPos();
				ItemEntity ei = new ItemEntity(te.getLevel(), pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, in);
				ei.setDeltaMovement(ReikaRandomHelper.getRandomPlusMinus(0, 0.5), ReikaRandomHelper.getRandomPlusMinus(0, 0.5),
						ReikaRandomHelper.getRandomPlusMinus(0, 0.5));
				ei.hurtMarked = true;
				te.getLevel().addFreshEntity(ei);
				inv.setItem(slot, ItemStack.EMPTY);
				break;
			}
		}
	}

}
