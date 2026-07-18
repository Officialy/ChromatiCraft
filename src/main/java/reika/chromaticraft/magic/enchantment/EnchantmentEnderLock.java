/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.enchantment;

import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.player.EntityPlayer;

import reika.chromaticraft.base.ChromaticEnchantment;
import reika.chromaticraft.magic.progression.ProgressStage;


public class EnchantmentEnderLock extends ChromaticEnchantment {

	public EnchantmentEnderLock(int id) {
		super(id, EnumEnchantmentType.bow);
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public boolean isVisibleToPlayer(EntityPlayer ep, int level) {
		return ProgressStage.END.isPlayerAtStage(ep);
	}

}
