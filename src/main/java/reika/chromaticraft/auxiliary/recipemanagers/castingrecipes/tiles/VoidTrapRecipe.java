package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class VoidTrapRecipe extends PylonCastingRecipe {

	public VoidTrapRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		for (int i = -4; i <= 4; i += 2) {
			for (int k = -4; k <= 4; k += 2) {
				if (i != 0 || k != 0) {
					this.addAuxItem(ChromaStacks.crystalPowder, i, k);
				}
			}
		}

		for (int i = -4; i <= 4; i += 2) {
			if (i != 0) {
				this.addAuxItem(ChromaStacks.voidmonsterEssence, i, i);
				this.addAuxItem(ChromaStacks.voidmonsterEssence, -i, i);
				this.addAuxItem(ChromaStacks.voidmonsterEssence, i, -i);
				this.addAuxItem(ChromaStacks.voidmonsterEssence, -i, -i);
			}
		}

		this.addAuxItem(ChromaStacks.spaceIngot, 2, 0);
		this.addAuxItem(ChromaStacks.spaceIngot, -2, 0);
		this.addAuxItem(ChromaStacks.spaceIngot, 0, 2);
		this.addAuxItem(ChromaStacks.spaceIngot, 0, -2);

		this.addAuxItem(ChromaStacks.avolite, 4, 0);
		this.addAuxItem(ChromaStacks.avolite, -4, 0);
		this.addAuxItem(ChromaStacks.avolite, 0, 4);
		this.addAuxItem(ChromaStacks.avolite, 0, -4);

		this.addAuraRequirement(CrystalElement.PINK, 50000);
		this.addAuraRequirement(CrystalElement.BLACK, 20000);
		this.addAuraRequirement(CrystalElement.LIGHTGRAY, 30000);
		this.addAuraRequirement(CrystalElement.YELLOW, 40000);
		this.addAuraRequirement(CrystalElement.WHITE, 50000);
		this.addAuraRequirement(CrystalElement.MAGENTA, 10000);
	}

}
