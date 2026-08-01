package reika.chromaticraft.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.AuraRequirement;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.GridIngredient;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.RuneRequirement;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.block.BlockCrystallineStone;

/**
 * Datapack-owned casting recipes, transcribed from V33a {@code RecipesCastingTable}. This first
 * family is the complete ordinary CrystalStoneRecipe set that only depends on registered 26.2
 * content. Boosted-shard stabilizer/resonance recipes follow when boosted shard components land.
 */
public final class ChromaCastingRecipeProvider extends RecipeProvider.Runner {

	public ChromaCastingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override public String getName() { return "ChromatiCraft Casting Recipes"; }

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
		return new Recipes(registries, output);
	}

	private static final class Recipes extends RecipeProvider {
		private final RecipeOutput output;

		private Recipes(HolderLookup.Provider registries, RecipeOutput output) {
			super(registries, output);
			this.output = output;
		}

		@Override
		protected void buildRecipes() {
			Ingredient stone = tag(ItemTags.STONE_CRAFTING_MATERIALS);
			for (CrystalElement element : CrystalElement.elements) {
				Map<Character, Ingredient> ingredients = Map.of(
						'S', stone,
						'C', Ingredient.of(ChromaItems.SHARDS.get(element).get()));
				save("crystal_stone/smooth_"+element.getEnglishName(),
						StoneTypes.SMOOTH, 8, ingredients, " S ", "SCS", " S ");
			}

			Ingredient smooth = Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem());
			Map<Character, Ingredient> smoothOnly = Map.of('S', smooth);
			save("crystal_stone/column", StoneTypes.COLUMN, 2, smoothOnly, "S", "S");
			save("crystal_stone/bricks", StoneTypes.BRICKS, 4, smoothOnly, "SS", "SS");
			save("crystal_stone/beam", StoneTypes.BEAM, 2, smoothOnly, "SS");
			save("crystal_stone/engraved", StoneTypes.ENGRAVED, 4, smoothOnly, " S ", "S S", " S ");
			save("crystal_stone/embossed", StoneTypes.EMBOSSED, 5, smoothOnly, " S ", "SSS", " S ");
			save("crystal_stone/corner", StoneTypes.CORNER, 5, smoothOnly, "SSS", "S  ", "S  ");
			save("crystal_stone/groove1", StoneTypes.GROOVE1, 3, smoothOnly, "S", "S", "S");
			save("crystal_stone/groove2", StoneTypes.GROOVE2, 3, smoothOnly, "SSS");

			// V33a StandRecipe: the Casting Item Stand is itself a base-tier casting recipe, which is
			// what unlocks tier-2 (auxiliary-stand) casting.
			//   new ShapedOreRecipe(STAND, "I I", "SLS", "CCC",
			//       'I', Items.iron_ingot, 'C', "cobblestone", 'S', stoneSlab, 'L', lapisDye)
			// V33a RuneRecipe / EnhancedRuneRecipe: a shard ringed by eight crystalline stone, at the
			// bare-table tier but gated behind ALLCOLORS. Enhanced uses the boosted shard and is the
			// same output; both give double the base experience and can double their output.
			for (CrystalElement element : CrystalElement.elements) {
				Ingredient runeStone = Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem());
				saveShapedProgress("crystal_rune/" + element.getEnglishName(),
						new ItemStackTemplate(ChromaBlocks.RUNES.get(element.ordinal()).get().asItem()), 5, 10,
						List.of(ProgressStage.ALLCOLORS),
						Map.of('S', runeStone, 'C', Ingredient.of(ChromaItems.SHARDS.get(element).get())),
						"SSS", "SCS", "SSS");
				saveShapedProgress("crystal_rune/" + element.getEnglishName() + "_boosted",
						new ItemStackTemplate(ChromaBlocks.RUNES.get(element.ordinal()).get().asItem()), 5, 10,
						List.of(ProgressStage.ALLCOLORS),
						Map.of('S', runeStone, 'C', Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(element).get())),
						"SSS", "SCS", "SSS");
			}

			saveShaped("casting_item_stand",
					new ItemStackTemplate(ChromaBlocks.ITEM_STAND.get().asItem()), 5, 5,
					Map.of('I', Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
							'C', tag(net.minecraft.tags.ItemTags.STONE_CRAFTING_MATERIALS),
							'S', Ingredient.of(net.minecraft.world.item.Items.STONE_SLAB),
							'L', Ingredient.of(net.minecraft.world.item.Items.LAPIS_LAZULI)),
					"I I", "SLS", "CCC");

			// Complete V33a CrystalGroupRecipe family: ordinary and boosted inputs.
			saveGroup("red", ChromaClusterItems.RED_GROUP, Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get()), new CrystalElement[] {CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA}, false);
			saveGroup("green", ChromaClusterItems.GREEN_GROUP, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.LIVING_ESSENCE).get()), new CrystalElement[] {CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN}, false);
			saveGroup("orange", ChromaClusterItems.ORANGE_GROUP, Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get()), new CrystalElement[] {CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE}, false);
			saveGroup("white", ChromaClusterItems.WHITE_GROUP, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ICY_DUST).get()), new CrystalElement[] {CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE}, false);
			saveGroup("red_boosted", ChromaClusterItems.RED_GROUP, Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get()), new CrystalElement[] {CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA}, true);
			saveGroup("green_boosted", ChromaClusterItems.GREEN_GROUP, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.LIVING_ESSENCE).get()), new CrystalElement[] {CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN}, true);
			saveGroup("orange_boosted", ChromaClusterItems.ORANGE_GROUP, Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get()), new CrystalElement[] {CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE}, true);
			saveGroup("white_boosted", ChromaClusterItems.WHITE_GROUP, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ICY_DUST).get()), new CrystalElement[] {CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE}, true);

			CrystalElement[] red = {CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA};
			CrystalElement[] green = {CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN};
			CrystalElement[] orange = {CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE};
			CrystalElement[] white = {CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE};
			saveCluster("primary", ChromaClusterItems.PRIMARY_CLUSTER, ChromaClusterItems.RED_GROUP, ChromaClusterItems.GREEN_GROUP, CrystalElement.WHITE, combinedRunes(red, green));
			saveCluster("secondary", ChromaClusterItems.SECONDARY_CLUSTER, ChromaClusterItems.ORANGE_GROUP, ChromaClusterItems.WHITE_GROUP, CrystalElement.BLACK, combinedRunes(orange, white));
			saveCore();
			saveStar();
			saveLowCore("void", ChromaCraftingItems.VOID_CORE, CrystalElement.BLACK, CrystalElement.WHITE, new net.minecraft.core.BlockPos(3,-1,-2), new net.minecraft.core.BlockPos(-3,-1,2), ChromaCraftingItems.VOID_DUST);
			saveLowCore("transformation", ChromaCraftingItems.TRANSFORMATION_CORE, CrystalElement.GRAY, CrystalElement.BLACK, new net.minecraft.core.BlockPos(3,0,-2), new net.minecraft.core.BlockPos(-3,0,2), ChromaCraftingItems.TELEPORTATION_DUST);
			saveLowCore("energy", ChromaCraftingItems.ENERGY_CORE, CrystalElement.YELLOW, CrystalElement.WHITE, new net.minecraft.core.BlockPos(-3,0,-2), new net.minecraft.core.BlockPos(3,0,2), ChromaCraftingItems.ENERGY_POWDER);
			saveCrystalMirror("glass", Ingredient.of(Items.GLASS));
			saveCrystalMirror("boosted_white_shard", shard(CrystalElement.WHITE, true));
			saveCrystalFocus();
			saveCrystalLens();
			saveIridescentChunk();
			saveLumenCore();
			saveElementUnit();
			saveHighCore("transformation", ChromaCraftingItems.HIGH_TRANSFORMATION_CORE, CrystalElement.GRAY, CrystalElement.BLACK, new net.minecraft.core.BlockPos(3,0,-2), new net.minecraft.core.BlockPos(-3,0,2), ChromaCraftingItems.TELEPORTATION_DUST);
			saveHighCore("void", ChromaCraftingItems.HIGH_VOID_CORE, CrystalElement.BLACK, CrystalElement.WHITE, new net.minecraft.core.BlockPos(3,-1,-2), new net.minecraft.core.BlockPos(-3,-1,2), ChromaCraftingItems.VOID_DUST);
			saveHighCore("energy", ChromaCraftingItems.HIGH_ENERGY_CORE, CrystalElement.YELLOW, CrystalElement.WHITE, new net.minecraft.core.BlockPos(-3,0,-2), new net.minecraft.core.BlockPos(3,0,2), ChromaCraftingItems.ENERGY_POWDER);
		}

		private void saveGroup(String name, ChromaClusterItems result, Ingredient center, CrystalElement[] shards, boolean boosted) {
			List<GridIngredient> grid = List.of(
					new GridIngredient(1, shard(shards[0], boosted)), new GridIngredient(3, shard(shards[1], boosted)),
					new GridIngredient(4, center), new GridIngredient(5, shard(shards[2], boosted)),
					new GridIngredient(7, shard(shards[3], boosted)));
			saveRecipe("crystal_group/" + name, new CastingTableRecipe(CastingTableRecipe.Tier.TEMPLE,
					grid, List.of(), groupRunes(shards), List.of(),
					new ItemStackTemplate(ChromaItems.CLUSTERS.get(result).get(), boosted ? 4 : 1),
					boosted ? 40 : 20, boosted ? 160 : 40));
		}

		private Ingredient shard(CrystalElement element, boolean boosted) {
			return Ingredient.of((boosted ? ChromaItems.BOOSTED_SHARDS : ChromaItems.SHARDS).get(element).get());
		}

		private List<RuneRequirement> groupRunes(CrystalElement[] shards) {
			int group = shards[0] == CrystalElement.RED ? 0 : shards[0] == CrystalElement.YELLOW ? 1 : shards[0] == CrystalElement.BROWN ? 2 : 3;
			int radius = group == 0 ? 3 : 4;
			int diagonal = group == 2 ? -2 : group == 3 ? 2 : 0;
			return List.of(new RuneRequirement(new net.minecraft.core.BlockPos(diagonal, 0, -radius), shards[0]),
					new RuneRequirement(new net.minecraft.core.BlockPos(-radius, 0, -diagonal), shards[1]),
					new RuneRequirement(new net.minecraft.core.BlockPos(radius, 0, diagonal), shards[2]),
					new RuneRequirement(new net.minecraft.core.BlockPos(-diagonal, 0, radius), shards[3]));
		}

		private List<RuneRequirement> combinedRunes(CrystalElement[] first, CrystalElement[] second) {
			List<RuneRequirement> runes = new ArrayList<>(groupRunes(first));
			runes.addAll(groupRunes(second));
			return runes;
		}

		private void saveCluster(String name, ChromaClusterItems output, ChromaClusterItems first, ChromaClusterItems second, CrystalElement center, List<RuneRequirement> runes) {
			List<GridIngredient> grid = List.of(new GridIngredient(1, Ingredient.of(ChromaItems.CLUSTERS.get(first).get())),
					new GridIngredient(3, Ingredient.of(ChromaItems.CLUSTERS.get(second).get())),
					new GridIngredient(4, Ingredient.of(ChromaItems.SHARDS.get(center).get())),
					new GridIngredient(5, Ingredient.of(ChromaItems.CLUSTERS.get(second).get())),
					new GridIngredient(7, Ingredient.of(ChromaItems.CLUSTERS.get(first).get())));
			saveRecipe("crystal_cluster/" + name, new CastingTableRecipe(CastingTableRecipe.Tier.TEMPLE,
					grid, List.of(), runes, List.of(), new ItemStackTemplate(ChromaItems.CLUSTERS.get(output).get()), 20, 40));
		}

		private void saveCore() {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(-2, 0, ChromaClusterItems.PRIMARY_CLUSTER), stand(2, 0, ChromaClusterItems.PRIMARY_CLUSTER),
					stand(0, -2, ChromaClusterItems.SECONDARY_CLUSTER), stand(0, 2, ChromaClusterItems.SECONDARY_CLUSTER));
			saveRecipe("crystal_core", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(Items.DIAMOND))), stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get(), 4), 200, 200));
		}

		private CastingTableRecipe.StandIngredient stand(int x, int z, ChromaClusterItems item) {
			return new CastingTableRecipe.StandIngredient(new net.minecraft.core.BlockPos(x, 0, z), Ingredient.of(ChromaItems.CLUSTERS.get(item).get()));
		}

		private void saveStar() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,0},{2,0},{0,-2},{0,2}}) stands.add(stand(pos[0], pos[1], ChromaClusterItems.CRYSTAL_CORE));
			for (int[] pos : new int[][] {{-2,-2},{2,-2},{-2,2},{2,2}}) stands.add(new CastingTableRecipe.StandIngredient(new net.minecraft.core.BlockPos(pos[0], 0, pos[1]), Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get())));
			int[][] ring = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
			for (int i = 0; i < ring.length; i++) stands.add(new CastingTableRecipe.StandIngredient(new net.minecraft.core.BlockPos(ring[i][0], 1, ring[i][1]), Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.elements[i]).get())));
			List<RuneRequirement> runes = List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-3,-1,-3), CrystalElement.BLACK), new RuneRequirement(new net.minecraft.core.BlockPos(3,-1,-3), CrystalElement.BLACK), new RuneRequirement(new net.minecraft.core.BlockPos(-3,-1,3), CrystalElement.BLACK), new RuneRequirement(new net.minecraft.core.BlockPos(3,-1,3), CrystalElement.BLACK));
			saveRecipe("crystal_star", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(Items.NETHER_STAR))), stands, runes, List.of(),
					new ItemStackTemplate(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_STAR).get(), 4), 400, 400));
		}

		private void saveLowCore(String name, ChromaCraftingItems output, CrystalElement primary,
				CrystalElement secondary, net.minecraft.core.BlockPos rune1, net.minecraft.core.BlockPos rune2,
				ChromaCraftingItems dust) {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,-2},{-4,-4},{2,-2},{4,-4},{-2,2},{-4,4},{2,2},{4,4}})
				stands.add(stand(pos[0], Math.abs(pos[0]) == 4 || Math.abs(pos[1]) == 4 ? 1 : 0, pos[1], shard(primary, true)));
			for (int[] pos : new int[][] {{2,0},{-2,0},{0,2},{0,-2}})
				stands.add(stand(pos[0], 0, pos[1], shard(secondary, true)));
			for (int[] pos : new int[][] {{4,0},{-4,0},{0,4},{0,-4}})
				stands.add(stand(pos[0], 1, pos[1], Ingredient.of(ChromaItems.CRAFTING.get(dust).get())));
			List<RuneRequirement> runes = List.of(new RuneRequirement(rune1, primary), new RuneRequirement(rune2, primary));
			saveRecipe("low_core/" + name, new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get()))),
					stands, runes, List.of(), new ItemStackTemplate(ChromaItems.CRAFTING.get(output).get()), 100, 200));
		}

		private void saveCrystalMirror(String name, Ingredient center) {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(-2, 0, -2, Ingredient.of(Items.GLASS)),
					stand(0, 0, -2, Ingredient.of(Items.GLASS)),
					stand(2, 0, -2, Ingredient.of(Items.GLASS)),
					stand(-2, 0, 0, shard(CrystalElement.BLUE, true)),
					stand(2, 0, 0, shard(CrystalElement.BLUE, true)),
					stand(-2, 0, 2, Ingredient.of(Items.IRON_INGOT)),
					stand(0, 0, 2, Ingredient.of(Items.IRON_INGOT)),
					stand(2, 0, 2, Ingredient.of(Items.IRON_INGOT)));
			saveRecipe("crystal_mirror/" + name, new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, center)), stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_MIRROR).get()), 100, 200));
		}

		private void saveCrystalFocus() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,0},{2,0}}) stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.BLUE, true)));
			for (int[] pos : new int[][] {{0,-2},{0,2}}) stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.YELLOW, true)));
			for (int[] pos : new int[][] {{-2,-2},{-2,2},{2,-2},{2,2}}) stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.PURPLE, true)));
			saveRecipe("crystal_focus", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CLUSTERS.get(ChromaClusterItems.PRIMARY_CLUSTER).get()))),
					stands, List.of(), List.of(), new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_FOCUS).get()), 100, 200));
		}

		private void saveCrystalLens() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,-2},{-2,2},{2,-2},{2,2}})
				stands.add(stand(pos[0], 0, pos[1], tiered(ChromaTieredItems.FOCUS_DUST)));
			for (int[] pos : new int[][] {{-2,0},{2,0}})
				stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.WHITE, false)));
			for (int[] pos : new int[][] {{0,-2},{0,2}})
				stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.BLUE, false)));
			saveRecipe("crystal_lens", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(Items.GLASS))), stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_LENS).get()), 100, 200));
		}

		private void saveIridescentChunk() {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(-2, 0, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())),
					stand(2, 0, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())),
					stand(0, 0, -2, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())),
					stand(0, 0, 2, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())),
					stand(-2, 0, -2, tiered(ChromaTieredItems.RESONANCE_DUST)),
					stand(2, 0, -2, tiered(ChromaTieredItems.RESONANCE_DUST)),
					stand(-2, 0, 2, tiered(ChromaTieredItems.FOCUS_DUST)),
					stand(2, 0, 2, tiered(ChromaTieredItems.BEACON_DUST)));
			saveRecipe("iridescent_chunk", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, tiered(ChromaTieredItems.BINDING_CRYSTAL))), stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get()), 100, 200));
		}

		private void saveElementUnit() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			int[][] ring = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},
					{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
			for (int i = 0; i < ring.length; i++) {
				int[] pos = ring[i];
				stands.add(stand(pos[0], 1, pos[1], Ingredient.of(
						ChromaItems.ELEMENTAL_STONES.get(CrystalElement.elements[i]).get())));
			}
			saveRecipe("element_unit", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, tiered(ChromaTieredItems.BINDING_CRYSTAL))), stands,
					List.of(), List.of(), new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get()), 100, 200));
		}

		private void saveLumenCore() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int x = -4; x <= 4; x += 2) for (int z = -4; z <= 4; z += 2) {
				if (x == 0 && z == 0) continue;
				boolean cardinal = (x == 0 && Math.abs(z) == 2) || (z == 0 && Math.abs(x) == 2);
				Ingredient ingredient = cardinal ? tiered(ChromaTieredItems.PURITY_DUST)
						: Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.GLOW_CHUNK).get());
				stands.add(stand(x, Math.abs(x) == 4 || Math.abs(z) == 4 ? 1 : 0, z, ingredient));
			}
			List<AuraRequirement> aura = List.of(new AuraRequirement(CrystalElement.BLACK, 60000),
					new AuraRequirement(CrystalElement.YELLOW, 60000), new AuraRequirement(CrystalElement.BLUE, 60000));
			saveRecipe("lumen_core", new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_STAR).get()))),
					stands, List.of(), aura, new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.LUMEN_CORE).get()), 400, 500));
		}

		private void saveHighCore(String name, ChromaCraftingItems output, CrystalElement primary,
				CrystalElement secondary, net.minecraft.core.BlockPos rune1, net.minecraft.core.BlockPos rune2,
				ChromaCraftingItems dust) {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,-2},{2,-2},{-2,2},{2,2}})
				stands.add(stand(pos[0], 0, pos[1], Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(primary).get())));
			for (int[] pos : new int[][] {{2,0},{-2,0},{0,2},{0,-2}})
				stands.add(stand(pos[0], 0, pos[1], Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(secondary).get())));
			for (int[] pos : new int[][] {{-4,-4},{4,-4},{-4,4},{4,4}})
				stands.add(stand(pos[0], 1, pos[1], Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())));
			stands.add(stand(-2, 1, -4, tiered(ChromaTieredItems.FIRE_ESSENCE)));
			stands.add(stand(4, 1, -2, Ingredient.of(ChromaItems.CRAFTING.get(dust).get())));
			stands.add(stand(2, 1, 4, tiered(ChromaTieredItems.FIRE_ESSENCE)));
			stands.add(stand(-4, 1, 2, Ingredient.of(ChromaItems.CRAFTING.get(dust).get())));
			stands.add(stand(2, 1, -4, tiered(ChromaTieredItems.ENDER_DUST)));
			stands.add(stand(-4, 1, -2, tiered(ChromaTieredItems.SPACE_DUST)));
			stands.add(stand(-2, 1, 4, tiered(ChromaTieredItems.ENDER_DUST)));
			stands.add(stand(4, 1, 2, tiered(ChromaTieredItems.SPACE_DUST)));
			stands.add(stand(-4, 1, 0, Ingredient.of(Items.DIAMOND)));
			stands.add(stand(4, 1, 0, Ingredient.of(Items.ENDER_PEARL)));
			stands.add(stand(0, 1, -4, Ingredient.of(Items.EMERALD)));
			stands.add(stand(0, 1, 4, Ingredient.of(Items.GUNPOWDER)));
			List<RuneRequirement> runes = List.of(new RuneRequirement(rune1, primary), new RuneRequirement(rune2, primary));
			saveRecipe("high_core/" + name, new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_STAR).get()))), stands, runes,
					List.of(new AuraRequirement(primary, 5000)), new ItemStackTemplate(ChromaItems.CRAFTING.get(output).get()), 400, 500));
		}

		private Ingredient tiered(ChromaTieredItems item) { return Ingredient.of(ChromaItems.TIERED.get(item).get()); }
		private CastingTableRecipe.StandIngredient stand(int x, int y, int z, Ingredient item) { return new CastingTableRecipe.StandIngredient(new net.minecraft.core.BlockPos(x, y, z), item); }
		private void save(String name, StoneTypes result, int count,
				Map<Character, Ingredient> ingredients, String... pattern) {
			if (pattern.length < 1 || pattern.length > 3)
				throw new IllegalArgumentException("Casting pattern must contain one to three rows");
			int width = pattern[0].length();
			if (width < 1 || width > 3)
				throw new IllegalArgumentException("Casting pattern must contain one to three columns");
			List<GridIngredient> grid = new ArrayList<>();
			for (int row = 0; row < pattern.length; row++) {
				if (pattern[row].length() != width) throw new IllegalArgumentException("Unequal casting pattern rows");
				int xOffset = (3-width)/2;
				int yOffset = (3-pattern.length)/2;
				for (int column = 0; column < width; column++) {
					char symbol = pattern[row].charAt(column);
					if (symbol == ' ') continue;
					Ingredient ingredient = ingredients.get(symbol);
					if (ingredient == null) throw new IllegalArgumentException("Undefined casting symbol '"+symbol+"'");
					grid.add(new GridIngredient((row+yOffset)*3+column+xOffset, ingredient));
				}
			}
			CastingTableRecipe recipe = new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					grid, List.of(), List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.crystallineStone(result).get().asItem(), count), 5, 5);
			saveRecipe(name, recipe);
		}

		/**
		 * A base-tier (bare table, no runes) shaped casting recipe with an arbitrary result. V33a
		 * expresses these as a {@code ShapedOreRecipe} wrapped in a {@code CastingRecipe} subclass.
		 */
		private void saveShaped(String name, ItemStackTemplate result, int duration, int energy,
				Map<Character, Ingredient> ingredients, String... pattern) {
			this.saveShapedProgress(name, result, duration, energy, List.of(), ingredients, pattern);
		}

		private void saveShapedProgress(String name, ItemStackTemplate result, int duration, int energy,
				List<ProgressStage> progress, Map<Character, Ingredient> ingredients, String... pattern) {
			if (pattern.length < 1 || pattern.length > 3)
				throw new IllegalArgumentException("Casting pattern must contain one to three rows");
			int width = pattern[0].length();
			if (width < 1 || width > 3)
				throw new IllegalArgumentException("Casting pattern must contain one to three columns");
			List<GridIngredient> grid = new ArrayList<>();
			for (int row = 0; row < pattern.length; row++) {
				if (pattern[row].length() != width) throw new IllegalArgumentException("Unequal casting pattern rows");
				int xOffset = (3-width)/2;
				int yOffset = (3-pattern.length)/2;
				for (int column = 0; column < width; column++) {
					char symbol = pattern[row].charAt(column);
					if (symbol == ' ') continue;
					Ingredient ingredient = ingredients.get(symbol);
					if (ingredient == null) throw new IllegalArgumentException("Undefined casting symbol '"+symbol+"'");
					grid.add(new GridIngredient((row+yOffset)*3+column+xOffset, ingredient));
				}
			}
			saveRecipe(name, new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					grid, List.of(), List.of(), List.of(), result, duration, energy, progress));
		}

		private void saveRecipe(String name, CastingTableRecipe recipe) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name));
			output.accept(key, recipe, null);
		}
	}
}
