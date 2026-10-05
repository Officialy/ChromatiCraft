/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface NBTTile {

	void getTagsToWriteToStack(CompoundTag NBT);

	void setDataFromItemStackTag(ItemStack is);

	void addTooltipInfo(List li, boolean shift);



}
