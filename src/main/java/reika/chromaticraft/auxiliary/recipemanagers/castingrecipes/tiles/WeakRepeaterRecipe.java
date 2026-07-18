/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import reika.chromaticraft.auxiliary.interfaces.CoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.magic.progression.ChromaResearchManager;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;


public class WeakRepeaterRecipe extends TempleCastingRecipe implements CoreRecipe {

	public WeakRepeaterRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRuneRingRune(CrystalElement.YELLOW);
		this.addRuneRingRune(CrystalElement.BLUE);

		this.addRune(CrystalElement.WHITE, 5, -1, 5);
		this.addRune(CrystalElement.WHITE, -5, -1, -5);

		this.addRune(CrystalElement.BLACK, 5, -1, 0);
		this.addRune(CrystalElement.BLACK, -5, -1, 0);
	}

	@Override
	public int getNumberProduced() {
		return 8;
	}

	@Override
	public int getDuration() {
		return 4*super.getDuration();
	}

	@Override
	public int getExperience() {
		return 2*super.getExperience();
	}

	@Override
	public boolean match(TileEntityCastingTable table) {
		return super.match(table) && table.getStackInSlot(4).stackSize == 1;
	}

	@Override
	public NBTTagCompound handleNBTResult(TileEntityCastingTable te, EntityPlayer ep, NBTTagCompound originalCenter, NBTTagCompound tag) {
		if (tag == null)
			tag = new NBTTagCompound();
		tag.setBoolean("boosted", false);
		return tag;
	}

	@Override
	public boolean canRunRecipe(TileEntity te, EntityPlayer ep) {
		return super.canRunRecipe(te, ep) && ChromaResearchManager.instance.getPlayerResearchLevel(ep).ordinal() >= ResearchLevel.ENERGY.ordinal();
	}

	@Override
	public ItemStack getCentralLeftover(ItemStack is) {
		return new ItemStack(Items.bucket, is.stackSize, 0);
	}

}
