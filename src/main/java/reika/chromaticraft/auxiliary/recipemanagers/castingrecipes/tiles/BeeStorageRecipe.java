package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;


public class BeeStorageRecipe extends TempleCastingRecipe {

	public BeeStorageRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.GREEN, -3, 0, -1);
		this.addRune(CrystalElement.LIGHTGRAY, 4, 0, 2);
	}

}
