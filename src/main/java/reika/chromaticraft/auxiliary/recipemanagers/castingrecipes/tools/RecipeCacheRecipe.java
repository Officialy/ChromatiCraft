package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.dragonapi.libraries.registry.ReikaItemHelper;


public class RecipeCacheRecipe extends MultiBlockCastingRecipe {

	public RecipeCacheRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.auraDust, 0, 2);
		this.addAuxItem(ChromaStacks.auraDust, 0, -2);
		this.addAuxItem(ChromaStacks.auraDust, -2, 0);
		this.addAuxItem(ChromaStacks.auraDust, 2, 0);

		this.addAuxItem(Items.iron_ingot, -2, 2);
		this.addAuxItem(Items.iron_ingot, 2, -2);

		this.addAuxItem(ReikaItemHelper.lapisDye, -4, 4);
		this.addAuxItem(ReikaItemHelper.lapisDye, 4, -4);
		this.addAuxItem(ChromaStacks.spaceDust, -4, 2);
		this.addAuxItem(ChromaStacks.spaceDust, -2, 4);
		this.addAuxItem(ChromaStacks.spaceDust, 2, -4);
		this.addAuxItem(ChromaStacks.spaceDust, 4, -2);

		this.addAuxItem(Items.gold_ingot, -4, -4);
		this.addAuxItem(Items.gold_ingot, 4, 4);
	}

	@Override
	public int getNumberProduced() {
		return 3;
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 9;
	}

}
