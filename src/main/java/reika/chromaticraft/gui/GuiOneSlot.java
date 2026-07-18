/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.gui;

import net.minecraft.entity.player.EntityPlayer;

import reika.chromaticraft.base.GuiChromaBase;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.dragonapi.base.OneSlotContainer;

public class GuiOneSlot extends GuiChromaBase {

	public GuiOneSlot(EntityPlayer ep, TileEntityChromaticBase te) {
		super(new OneSlotContainer(ep, te), ep, te);
	}

	@Override
	public String getGuiTexture() {
		return "oneslot";
	}

}
