package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class DeathFogRecipe extends TempleCastingRecipe {

	public DeathFogRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.MAGENTA, -2, 0, 5);
		this.addRune(CrystalElement.LIGHTGRAY, 4, 0, 2);
		this.addRune(CrystalElement.PINK, 3, 0, -4);
		this.addRune(CrystalElement.GRAY, -4, 0, -2);
	}

}
