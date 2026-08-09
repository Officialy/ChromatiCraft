package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;

/** V33a inscription registry. V33a ships one built-in recipe: Smooth Crystal Stone to Pylon Link. */
public final class InscriptionRecipes {

	public static final InscriptionRecipes instance = new InscriptionRecipes();
	private final List<InscriptionRecipe> recipes = List.of(new InscriptionRecipe(
			ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get(), ChromaBlocks.PYLON_LINK.get(), 100, 0));

	private InscriptionRecipes() {}

	public InscriptionRecipe getInscriptionRecipe(BlockState state) {
		for (InscriptionRecipe recipe : recipes)
			if (state.is(recipe.input())) return recipe;
		return null;
	}

	public InscriptionRecipe getRecipeByID(int id) {
		return id >= 0 && id < recipes.size() ? recipes.get(id) : null;
	}

	public List<InscriptionRecipe> getAllInscriptionRecipes() {
		return recipes;
	}

	public record InscriptionRecipe(Block input, Block output, int duration, int referenceIndex) {
		public void place(ServerLevel level, BlockPos pos, Player player) {
			level.setBlock(pos, output.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(pos) instanceof TileEntityChromaticBase tile)
				tile.setPlacer(player);
		}
	}
}
