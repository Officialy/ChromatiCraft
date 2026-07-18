package reika.chromaticraft.auxiliary.interfaces;

import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import reika.chromaticraft.auxiliary.CastingAutomationSystem;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.dragonapi.asm.apistripper.Strippable;
import reika.dragonapi.interfaces.tileentity.BreakAction;
import reika.dragonapi.interfaces.tileentity.GuiController;

import appeng.api.networking.security.IActionHost;

@Strippable(value={"appeng.api.networking.security.IActionHost"})
public interface CastingAutomationBlock<V extends CastingAutomationSystem> extends GuiController, OwnedTile, BreakAction, IActionHost {

	public Collection<CastingRecipe> getAvailableRecipes();

	public TileEntityCastingTable getTable();

	public int getInjectionTickRate();
	public boolean isAbleToRun(TileEntityCastingTable te);

	public boolean canTriggerCrafting();
	public boolean canPlaceCentralItemForMultiRecipes();
	public boolean canRecursivelyRequest(CastingRecipe c);

	public V getAutomationHandler();

	public void consumeEnergy(CastingRecipe c, TileEntityCastingTable te, ItemStack is);
	public boolean canCraft(World world, int x, int y, int z, TileEntityCastingTable te);

	public TileEntity getItemPool();

}
