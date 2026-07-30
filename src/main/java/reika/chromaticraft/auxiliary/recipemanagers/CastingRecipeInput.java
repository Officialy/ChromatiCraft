package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import reika.chromaticraft.registry.CrystalElement;

/** Complete immutable server-side snapshot used to match all four V33a casting tiers. */
public final class CastingRecipeInput implements RecipeInput {
	private final List<ItemStack> grid;
	private final Map<BlockPos, ItemStack> stands;
	private final Map<BlockPos, CrystalElement> runes;
	private final Map<CrystalElement, Integer> aura;

	public CastingRecipeInput(List<ItemStack> grid, Map<BlockPos, ItemStack> stands,
			Map<BlockPos, CrystalElement> runes, Map<CrystalElement, Integer> aura) {
		if (grid.size() != 9)
			throw new IllegalArgumentException("A casting table input must contain exactly nine grid slots");
		this.grid = grid.stream().map(ItemStack::copy).toList();
		java.util.HashMap<BlockPos, ItemStack> standCopy = new java.util.HashMap<>();
		stands.forEach((position, stack) -> standCopy.put(position.immutable(), stack.copy()));
		this.stands = Map.copyOf(standCopy);
		this.runes = Map.copyOf(runes);
		this.aura = Map.copyOf(aura);
	}

	public static CastingRecipeInput grid(List<ItemStack> grid) {
		return new CastingRecipeInput(grid, Map.of(), Map.of(), Map.of());
	}

	@Override public ItemStack getItem(int index) { return grid.get(index); }
	@Override public int size() { return grid.size(); }
	public Map<BlockPos, ItemStack> stands() { return stands; }
	public CrystalElement runeAt(BlockPos offset) { return runes.get(offset); }
	public int auraAvailable(CrystalElement element) { return aura.getOrDefault(element, 0); }
	public Map<BlockPos, CrystalElement> runes() { return runes; }
	public Map<CrystalElement, Integer> aura() { return aura; }
}
