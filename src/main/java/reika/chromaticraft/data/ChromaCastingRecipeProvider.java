package reika.chromaticraft.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.AuraRequirement;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.GridIngredient;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.RuneRequirement;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.registry.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datapack-owned casting recipes, transcribed from V33a {@code RecipesCastingTable}. This first
 * family is the complete ordinary CrystalStoneRecipe set that only depends on registered 26.2
 * content. Boosted-shard stabilizer/resonance recipes follow when boosted shard components land.
 */
public final class ChromaCastingRecipeProvider {

    private ChromaCastingRecipeProvider() {}

    public static void bootstrap(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        new Recipes(recipes, advancements).buildRecipes();
    }

	private static final class Recipes extends RecipeProvider {

		private Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
		}

		@Override
		protected void buildRecipes() {
			Ingredient stone = tag(Tags.Items.STONES);
			save("crystal_stone/smooth", StoneTypes.SMOOTH, 8,
					Map.of('S', stone, 'C', tag(ChromaItemTags.PLAIN_CRYSTAL_SHARDS)),
					" S ", "SCS", " S ");

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
			// The two pylon upgrade blocks. V33a CrystalStoneRecipe derives its progression from its
			// own ingredients: a charged shard implies SHARDCHARGE and a rune block implies
			// ALLCOLORS. These no longer generate naturally, so without these recipes the aura
			// stabilizer and resonance ring are unobtainable.
			Ingredient chargedWhite = Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.WHITE).get());
			save("crystal_stone/stabilizer", StoneTypes.STABILIZER, 4,
					List.of(ProgressStage.ALLCOLORS, ProgressStage.SHARDCHARGE),
					Map.of('S', smooth, 'c', chargedWhite,
							's', Ingredient.of(ChromaBlocks.rune(CrystalElement.WHITE).get().asItem())),
					"sSs", "ScS", "sSs");
			save("crystal_stone/resonance_ring", StoneTypes.RESORING, 6,
					List.of(ProgressStage.SHARDCHARGE),
					Map.of('S', smooth, 'c', chargedWhite),
					"SSS", "ccc", "SSS");

			// V33a StandRecipe: the Casting Item Stand is itself a base-tier casting recipe, which is
			// what unlocks tier-2 (auxiliary-stand) casting.
			//   new ShapedOreRecipe(STAND, "I I", "SLS", "CCC",
			//       'I', Items.iron_ingot, 'C', "cobblestone", 'S', stoneSlab, 'L', lapisDye)
			// V33a RuneRecipe: a plain shard ringed by eight crystalline stone at the bare-table
			// tier, gated behind ALLCOLORS, for double the base 5 XP.
			//
			// V33a EnhancedRuneRecipe is NOT the same recipe with a boosted shard. It extends
			// TempleCastingRecipe, so it is a TEMPLE-tier, 20-tick recipe that additionally requires
			// RUNEUSE and its own colour's rune in the temple rune ring, and it pays out four times
			// the temple experience for eight runes instead of one.
			for (CrystalElement element : CrystalElement.elements) {
				Ingredient runeStone = Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem());
				Item rune = ChromaBlocks.RUNES.get(element.ordinal()).get().asItem();
				saveShapedProgress("crystal_rune/" + element.getEnglishName(),
						new ItemStackTemplate(rune), 5, 10,
						List.of(ProgressStage.ALLCOLORS),
						Map.of('S', runeStone, 'C', Ingredient.of(ChromaItems.SHARDS.get(element).get())),
						"SSS", "SCS", "SSS");
				saveShapedTemple("crystal_rune/" + element.getEnglishName() + "_boosted",
						new ItemStackTemplate(rune, 8), 20, 160,
						List.of(ProgressStage.ALLCOLORS), List.of(runeRingRune(element)),
						Map.of('S', runeStone, 'C', Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(element).get())),
						"SSS", "SCS", "SSS");
				// V33a CrystalGlassRecipe (a DecoCastingRecipe): sixteen from glass around the colour's shard. Its
				// typical total of 1024 over sixteen per craft gives an XP penalty threshold of 48 crafts.
				saveRecipe("crystal_glass/" + element.getEnglishName(), new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
						shapedGrid(Map.of('G', Ingredient.of(Items.GLASS),
								'S', Ingredient.of(ChromaItems.SHARDS.get(element).get())), "GSG", "SGS", "GSG"),
						List.of(), List.of(), List.of(),
						new ItemStackTemplate(ChromaBlocks.crystalGlass(element).get().asItem(), 16), 5, 5)
						.withPenaltyThreshold(48));
				// V33a CrystalGlowRecipe: the colour's shard over the base ingredient makes sixteen. It is a
				// CoreRecipe, so it is never XP-penalised (its halved threshold of MAX_VALUE is still unreachable).
				for (reika.chromaticraft.block.crystal.BlockCrystalGlow.Bases base
						: reika.chromaticraft.block.crystal.BlockCrystalGlow.Bases.baseList)
					saveRecipe("crystal_glow/" + element.getEnglishName() + "_" + base.id(),
							new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
									shapedGrid(Map.of('S', Ingredient.of(ChromaItems.SHARDS.get(element).get()),
											's', Ingredient.of(base.ingredient.get())), "S", "s"),
									List.of(), List.of(), List.of(),
									new ItemStackTemplate(ChromaBlocks.crystalGlow(element, base).get().asItem(), 16), 5, 5)
									.withPenalty(Integer.MAX_VALUE, 0.5F));
				// V33a CrystalSeedRecipe (a TempleCastingRecipe, 20 ticks and 40 XP): wheat seeds ringed by the
				// colour's shards with glowstone dust on the corners, and that colour's rune in the ring. The plain
				// shard makes one; the boosted shard ("enhanced") makes eight. Its typical crafted amount of 4 puts
				// the XP penalty after max(1, 4*3/4) = 3 crafts, and it may give double output.
				for (boolean enhanced : new boolean[] {false, true}) {
					Item shard = enhanced ? ChromaItems.BOOSTED_SHARDS.get(element).get() : ChromaItems.SHARDS.get(element).get();
					saveRecipe("crystal_seeds/" + element.getEnglishName() + (enhanced ? "_boosted" : ""),
							new CastingTableRecipe(CastingTableRecipe.Tier.TEMPLE,
									shapedGrid(Map.of('G', Ingredient.of(Items.GLOWSTONE_DUST), 'S', Ingredient.of(shard),
											's', Ingredient.of(Items.WHEAT_SEEDS)), "GSG", "SsS", "GSG"),
									List.of(), List.of(runeRingRune(element)), List.of(),
									new ItemStackTemplate(ChromaItems.CRYSTAL_SEEDS.get(element).get(), enhanced ? 8 : 1), 20, 40)
									.withPenaltyThreshold(3));
				}
				saveTintedLens(element, Items.IRON_INGOT, 1, "iron");
				saveTintedLens(element, Items.GOLD_INGOT, 2, "gold");
				saveTintedLens(element,
						ChromaItems.CRAFTING.get(ChromaCraftingItems.CHROMA_INGOT).get(), 4,
						"chroma_ingot");
			}

			// V33a SelectiveGlassRecipe (a DecoCastingRecipe): nine panes for half the crafting XP; typical total
			// 270 over nine per craft puts the XP penalty after 22 crafts.
			saveRecipe("selective_glass", new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					shapedGrid(Map.of('G', Ingredient.of(Items.GLASS), 'g', Ingredient.of(Items.GLOWSTONE_DUST),
							'e', Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.TELEPORTATION_DUST).get()),
							'v', Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.VOID_DUST).get()),
							'b', Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ICY_DUST).get())),
							"GgG", "beb", "GvG"),
					List.of(), List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.SELECTIVE_GLASS.get().asItem(), 9), 5, 2)
					.withPenaltyThreshold(22));

			saveShaped("ritual_table", new ItemStackTemplate(ChromaBlocks.RITUAL_TABLE.get().asItem()),
					5, 5,
					Map.of('C', tag(Tags.Items.COBBLESTONES_NORMAL),
							'S', tag(ChromaItemTags.CRYSTAL_SHARDS),
							'E', Ingredient.of(ChromaItems.CRAFTING.get(
								ChromaCraftingItems.ENERGY_POWDER).get())),
					"SES", "CSC", "CCC");

			// V33a StandRecipe extends TempleCastingRecipe: the Item Stand is a TEMPLE-tier, 20-tick
			// recipe worth twice the temple experience, and it wants two purple and two black runes
			// on the floor beside the table. It is NOT a base-tier recipe — a player reaches it only
			// after CASTING, ALLCOLORS and RUNEUSE, which is what makes the stands a milestone.
			saveShapedTemple("casting_item_stand",
					new ItemStackTemplate(ChromaBlocks.ITEM_STAND.get().asItem()), 20, 80,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-2, 0, 3), CrystalElement.PURPLE),
							new RuneRequirement(new net.minecraft.core.BlockPos(2, 0, -3), CrystalElement.PURPLE),
							new RuneRequirement(new net.minecraft.core.BlockPos(-2, 0, -3), CrystalElement.BLACK),
							new RuneRequirement(new net.minecraft.core.BlockPos(2, 0, 3), CrystalElement.BLACK)),
					Map.of('I', Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
							'C', tag(net.minecraft.tags.ItemTags.STONE_CRAFTING_MATERIALS),
							'S', Ingredient.of(net.minecraft.world.item.Items.STONE_SLAB),
							'L', Ingredient.of(net.minecraft.world.item.Items.LAPIS_LAZULI)),
					"I I", "SLS", "CCC");

			// V33a WeakRepeaterRecipe: eight Wooden Repeaters from a single Liquid Chroma bucket,
			// wooden planks/stick, Transmissive Dust and glowstone dust. It is a four-times-duration
			// Temple cast (80 ticks), twice the normal temple XP (80), with two ring runes and the
			// four authored outer runes. The bucket's normal crafting remainder returns the empty pail.
			// V33a canRunRecipe also demanded research tier ENERGY or above; see withMinimumResearch.
			saveShapedTemple("weak_repeater",
					new ItemStackTemplate(ChromaBlocks.WEAK_REPEATER.get().asItem(), 8), 80, 80,
					List.of(ProgressStage.CHARGE, ProgressStage.PYLON, ProgressStage.MAKECHROMA),
					List.of(runeRingRune(CrystalElement.YELLOW), runeRingRune(CrystalElement.BLUE),
							new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, 5), CrystalElement.WHITE),
							new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, -5), CrystalElement.WHITE),
							new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, 0), CrystalElement.BLACK),
							new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, 0), CrystalElement.BLACK)),
					Map.of('w', tag(net.minecraft.tags.ItemTags.PLANKS),
							'g', tiered(ChromaTieredItems.BEACON_DUST),
							's', tag(Tags.Items.RODS_WOODEN),
							'c', Ingredient.of(ChromaItems.CHROMA_BUCKET.get()),
							'r', Ingredient.of(Items.GLOWSTONE_DUST)),
					"wgw", "scs", "wrw");
			saveCrystalRepeater();
			saveEnderEye();

			// V33a RelayRecipe: the smooth Crystalline Stone centre is retained as an auxiliary
			// ingredient too, alongside three glowstone dusts and the matching boosted shard.
			// Each colour is now a registry identity; the authored output remains eight relays.
			for (CrystalElement element : CrystalElement.elements) {
				List<CastingTableRecipe.StandIngredient> stands = List.of(
						auxStand(0, -4, Ingredient.of(Items.GLOWSTONE_DUST)),
						auxStand(-2, -2, Ingredient.of(Items.GLOWSTONE_DUST)),
						auxStand(2, -2, Ingredient.of(Items.GLOWSTONE_DUST)),
						auxStand(0, -2, Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(element).get())),
						auxStand(0, 2, Ingredient.of(
								ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem())));
				saveRecipe("lumen_relay/" + element.getEnglishName(), new CastingTableRecipe(
						CastingTableRecipe.Tier.MULTIBLOCK,
						List.of(new GridIngredient(4, Ingredient.of(
								ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem()))),
						stands, List.of(runeRingRune(element)), List.of(),
						new ItemStackTemplate(ChromaBlocks.lumenRelay(element).get().asItem(), 8),
						100, 200));
			}

			// V33a CompoundRelayRecipe: diamond centre, four Infused Dust, three Transmissive Dust,
			// and any relay (the wildcard metadata also accepted an existing multichromic one).
			saveRecipe("lumen_relay/multichromic", new CastingTableRecipe(
					CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(Items.DIAMOND))),
					List.of(auxStand(-2, -2, tiered(ChromaTieredItems.ELEMENT_DUST)),
							auxStand(2, -2, tiered(ChromaTieredItems.ELEMENT_DUST)),
							auxStand(-2, 2, tiered(ChromaTieredItems.ELEMENT_DUST)),
							auxStand(2, 2, tiered(ChromaTieredItems.ELEMENT_DUST)),
							auxStand(-2, 0, tiered(ChromaTieredItems.BEACON_DUST)),
							auxStand(2, 0, tiered(ChromaTieredItems.BEACON_DUST)),
							auxStand(0, -2, tiered(ChromaTieredItems.BEACON_DUST)),
							auxStand(0, 2, tag(ChromaItemTags.LUMEN_RELAYS))),
					List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.MULTICHROMIC_RELAY.get().asItem()), 100, 200));

			// V33a EnchanterRecipe, a TempleCastingRecipe (20 ticks, 40 XP): emeralds, a gold ingot, obsidian, stone
			// bricks and an enchanting table, with black and purple runes at the ring's (+-4,+-3)/(+-3,+-4) corners.
			saveRecipe("auto_enchanter", new CastingTableRecipe(CastingTableRecipe.Tier.TEMPLE,
					shapedGrid(Map.of('e', Ingredient.of(Items.EMERALD), 'O', Ingredient.of(Items.OBSIDIAN),
							'B', Ingredient.of(Items.STONE_BRICKS), 'E', Ingredient.of(Items.ENCHANTING_TABLE),
							'G', Ingredient.of(Items.GOLD_INGOT)),
							"eGe", "OEO", "BOB"),
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-4, 0, -3), CrystalElement.BLACK),
							new RuneRequirement(new net.minecraft.core.BlockPos(-3, 0, -4), CrystalElement.PURPLE),
							new RuneRequirement(new net.minecraft.core.BlockPos(4, 0, 3), CrystalElement.BLACK),
							new RuneRequirement(new net.minecraft.core.BlockPos(3, 0, 4), CrystalElement.PURPLE)),
					List.of(), new ItemStackTemplate(ChromaBlocks.AUTO_ENCHANTER.get().asItem()), 20, 40));

			saveRecipe("function_relay", new CastingTableRecipe(CastingTableRecipe.Tier.TEMPLE,
					shapedGrid(Map.of('D', tiered(ChromaTieredItems.BEACON_DUST),
							'A', tiered(ChromaTieredItems.AURA_DUST), 'C', Ingredient.of(Items.GLOWSTONE)),
							" D ", "ACA", " D "),
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-3, 0, 3), CrystalElement.GRAY),
							new RuneRequirement(new net.minecraft.core.BlockPos(0, 0, -4), CrystalElement.YELLOW),
							new RuneRequirement(new net.minecraft.core.BlockPos(3, 0, 3), CrystalElement.LIME)),
					List.of(), new ItemStackTemplate(ChromaBlocks.FUNCTION_RELAY.get().asItem()), 20, 40)
					.withPenaltyThreshold(1));

			// Complete V33a RelaySourceRecipe. The repeated (0,-4) Focal Powder assignment in the
			// source intentionally resolves to one stand, leaving the authored full set of 24.
			List<CastingTableRecipe.StandIngredient> relayStands = List.of(
					auxStand(-4, -4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(-2, -4, tiered(ChromaTieredItems.ELEMENT_DUST)),
					auxStand(0, -4, tiered(ChromaTieredItems.FOCUS_DUST)),
					auxStand(2, -4, tiered(ChromaTieredItems.ELEMENT_DUST)),
					auxStand(4, -4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(0, -2, tiered(ChromaTieredItems.FOCUS_DUST)),
					auxStand(0, 2, tiered(ChromaTieredItems.FOCUS_DUST)),
					auxStand(-4, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_LENS).get())),
					auxStand(4, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_LENS).get())),
					auxStand(-2, 0, Ingredient.of(Items.GLOWSTONE)),
					auxStand(2, 0, Ingredient.of(Items.GLOWSTONE)),
					auxStand(-4, -2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(-2, -2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(2, -2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(4, -2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(-4, 2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(-2, 2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(2, 2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(4, 2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(-4, 4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(-2, 4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(0, 4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(2, 4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(4, 4, Ingredient.of(Items.IRON_INGOT)));
			saveRecipe("relay_source", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(
							ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_FOCUS).get()))),
					relayStands,
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-4, 0, -5), CrystalElement.BLACK),
							new RuneRequirement(new net.minecraft.core.BlockPos(5, 0, -4), CrystalElement.BLUE),
							new RuneRequirement(new net.minecraft.core.BlockPos(4, 0, 5), CrystalElement.WHITE),
							new RuneRequirement(new net.minecraft.core.BlockPos(-5, 0, 4), CrystalElement.YELLOW)),
					List.of(), new ItemStackTemplate(ChromaBlocks.RELAY_SOURCE.get().asItem()), 100, 200));

			// V33a CavePatherRecipe, a plain CastingRecipe.
			saveShaped("cave_pather", new ItemStackTemplate(ChromaItems.CAVE_PATHER.get()), 5, 5,
					Map.of('R', Ingredient.of(Items.REDSTONE), 'E', Ingredient.of(Items.DIAMOND),
							'B', Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.BEACON_DUST).get()),
							'A', Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get()),
							'S', Ingredient.of(Items.STICK), 'C', Ingredient.of(Items.QUARTZ)),
					"RAC", "BCA", "SBE");

			// V33a OwnerKeyRecipe: no XP, and the table's owner who casts it is written in as the key's owner
			// (handleNBTResult; the casting table's caster tag).
			saveRecipe("ownership_key", new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					shapedGrid(Map.of('i', Ingredient.of(Items.IRON_INGOT), 'r', Ingredient.of(Items.REDSTONE),
							'l', Ingredient.of(Items.LAPIS_LAZULI), 'g', Ingredient.of(Items.GLOWSTONE_DUST)),
							"lil", "gig", "rir"),
					List.of(), List.of(), List.of(),
					new ItemStackTemplate(ChromaItems.OWNERSHIP_KEY.get()), 5, 0,
					List.of(), 0.75F, true, false, 1, 0F,
					new CastingTableRecipe.CompletionBehavior(false, List.of(), List.of(),
							reika.chromaticraft.items.tools.ItemOwnerKey.OWNER_TAG)));

			// V33a LightGunRecipe, a plain CastingRecipe: a gold-bodied, stick-gripped gun with an aura-dust muzzle.
			saveShaped("light_gun", new ItemStackTemplate(ChromaItems.LIGHT_GUN.get()), 5, 5,
					Map.of('s', Ingredient.of(Items.STICK), 'g', Ingredient.of(Items.REDSTONE), 'G', Ingredient.of(Items.GOLD_INGOT),
							'a', Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get()),
							'b', Ingredient.of(Items.OBSIDIAN)),
					" ba", "gGb", "sg ");

			// V33a PylonFinderRecipe, a plain CastingRecipe (its runes were commented out): aura dust ringed by iron and
			// any shard.
			saveShaped("pylon_finder", new ItemStackTemplate(ChromaItems.PYLON_FINDER.get()), 5, 5,
					Map.of('S', tag(ChromaItemTags.CRYSTAL_SHARDS), 'I', Ingredient.of(Items.IRON_INGOT),
							'A', Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get())),
					"SIS", "IAI", "SIS");

			// V33a ChromaFlowerRecipe (a DecoCastingRecipe): leaves, a flower, glowstone and any shard for one flower.
			// Typical total 4 makes the penalty threshold (4*3/4)*4/5 = 2 crafts, at a quarter XP beyond it.
			saveRecipe("chroma_flower", new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					shapedGrid(Map.of('L', tag(net.minecraft.tags.ItemTags.LEAVES),
							'F', tag(Tags.Items.FLOWERS_SMALL), 'G', Ingredient.of(Items.GLOWSTONE_DUST),
							's', tag(ChromaItemTags.CRYSTAL_SHARDS)), "LFL", "GsG"),
					List.of(), List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.CHROMA_FLOWER.get().asItem()), 5, 5)
					.withPenalty(2, 0.25F));

			// V33a ToolStorageRecipe, a plain CastingRecipe: quartz, a glowstone block, a Loot Chest, stone and oak logs
			// (V33a used 16k ME storage for the two side logs with Applied Energistics loaded).
			saveShaped("tool_storage", new ItemStackTemplate(ChromaBlocks.TOOL_STORAGE.get().asItem()), 5, 5,
					Map.of('q', Ingredient.of(Items.QUARTZ), 'g', Ingredient.of(Items.GLOWSTONE),
							'a', Ingredient.of(Items.OAK_LOG), 'c', Ingredient.of(ChromaBlocks.LOOT_CHEST.get().asItem()),
							'w', Ingredient.of(Items.OAK_LOG), 's', Ingredient.of(Items.STONE)),
					"qgq", "aca", "wsw");

			// V33a LumenWireRecipe, a plain CastingRecipe: a blue shard (the exact plain shard), obsidian, glowstone dust
			// and cobblestone (the 1.7.10 "cobblestone" ore name).
			saveShaped("lumen_wire", new ItemStackTemplate(ChromaBlocks.LUMEN_WIRE.get().asItem()), 5, 5,
					Map.of('s', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.BLUE).get()),
							'O', Ingredient.of(Items.OBSIDIAN),
							'G', Ingredient.of(Items.GLOWSTONE_DUST),
							'c', tag(Tags.Items.COBBLESTONES_NORMAL)),
					" s ", "OGO", "ccc");

			// V33a LumenTurretRecipe, a plain CastingRecipe making four: a pink shard (the exact plain shard) ringed by
			// glowstone dust, on smooth crystalline stone.
			saveShaped("lumen_turret", new ItemStackTemplate(ChromaBlocks.LUMEN_TURRET.get().asItem(), 4), 5, 5,
					Map.of('s', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.PINK).get()),
							'g', Ingredient.of(Items.GLOWSTONE_DUST),
							'b', Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem())),
					" g ", "gsg", " b ");

			// V33a ParticleSpawnerRecipe, a plain CastingRecipe: any shard, glowstone dust and a block of redstone.
			saveShaped("particle_spawner", new ItemStackTemplate(ChromaBlocks.PARTICLE_SPAWNER.get().asItem()), 5, 5,
					Map.of('s', tag(ChromaItemTags.CRYSTAL_SHARDS),
							'g', Ingredient.of(Items.GLOWSTONE_DUST),
							'R', Ingredient.of(Items.REDSTONE_BLOCK)),
					"sg ", "gRg", " gs");

			// V33a ProbeRecipe, a plain CastingRecipe: black shards (the exact plain shard), glowstone dust and glass.
			saveShaped("probe", new ItemStackTemplate(ChromaItems.PROBE.get()), 5, 5,
					Map.of('s', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.BLACK).get()),
							'g', Ingredient.of(Items.GLOWSTONE_DUST),
							'G', Ingredient.of(Items.GLASS)),
					" sg", "sGs", "gs ");

			// V33a LaunchPadRecipe, a plain CastingRecipe: lime shards (the exact plain shard), aura dust, redstone and an
			// engraved crystal stone.
			saveShaped("launch_pad", new ItemStackTemplate(ChromaBlocks.LAUNCH_PAD.get().asItem()), 5, 5,
					Map.of('s', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.LIME).get()),
							'r', Ingredient.of(Items.REDSTONE),
							'a', Ingredient.of(ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get()),
							'g', Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.ENGRAVED).get())),
					"sas", "aga", "rar");

			// V33a CrystalBrewerRecipe, a plain CastingRecipe: any shard (getAnyMetaStack, boosted included) around
			// a cauldron on a stone base.
			saveShaped("crystal_brewer", new ItemStackTemplate(ChromaBlocks.CRYSTAL_BREWER.get().asItem()), 5, 5,
					Map.of('S', tag(ChromaItemTags.CRYSTAL_SHARDS), 'c', Ingredient.of(Items.CAULDRON),
							'C', tag(Tags.Items.STONES)), "S S", "ScS", "CCC");

			// V33a ChromaCollectorRecipe: any shard, an Ender Eye, glowstone, and a stone base.
			// Its one-craft threshold with a zero multiplier makes repeated casts grant no XP.
			saveRecipe("collector", new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					shapedGrid(Map.of('S', tag(ChromaItemTags.CRYSTAL_SHARDS),
							'E', Ingredient.of(Items.ENDER_EYE), 'c', Ingredient.of(Items.GLOWSTONE),
							'C', tag(Tags.Items.STONES)), "SES", "ScS", "CCC"),
					List.of(), List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.COLLECTOR.get().asItem()), 5, 5,
					List.of(), 0.75F, true, false, 1, 0F,
					CastingTableRecipe.CompletionBehavior.DEFAULT));

			saveFarmer();
			saveMagicPlants();
			savePendants();
			savePurificationGem();
			// V33a RecipesCastingTable: ChargedItemPlayerBufferConnectionRecipe for PROBE (1), NETHERKEY (3),
			// SPAWNERBYPASS (2), OREPICK (4), STRUCTUREFINDER (1), PURIFY (10) and each PENDANT3 (15). The Nether
			// Key, Spawner Bypass, Ore Pick and Structure Finder add theirs when those items are ported.
			saveBufferLink("probe", ChromaItems.PROBE.get(), CrystalElement.BLUE, 1);
			saveBufferLink("purification_gem", ChromaItems.PURIFY.get(), CrystalElement.WHITE, 10);
			for (CrystalElement e : CrystalElement.elements)
				saveBufferLink("enhanced_pendant/" + e.getEnglishName(), ChromaItems.BOOSTED_PENDANTS.get(e).get(), e, 15);

			// V33a RawCrystalRecipe: four Purification Powder around one exact plain shard. The
			// source registers the same recipe once for each of the sixteen shard metadata values;
			// the metadata-free port expresses that family as the disjoint plain-shard tag. It is a
			// CoreRecipe, so the ordinary five-tick/five-XP cast produces two and never incurs the
			// repeated-recipe XP penalty.
			saveShaped("raw_crystal",
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.RAW_CRYSTAL).get(), 2),
					5, 5,
					Map.of('F', tiered(ChromaTieredItems.PURITY_DUST),
							'S', tag(ChromaItemTags.PLAIN_CRYSTAL_SHARDS)),
					" F ", "FSF", " F ");

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
			savePowerCrystal();
			savePylonTurbo();
			savePortal();
			saveElementUnit();
			saveStorageCrystals();
			saveCrystalCharger();
			savePersonalCharger();
			saveItemAuraInfuser();
			savePlayerAuraInfuser();
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
					boosted ? 40 : 20, boosted ? 160 : 40)
					// V33a CrystalGroupRecipe is not a CoreRecipe, and its typical crafted amount
					// resolves to the base 1 (its charged branch calls itself and would recurse), so
					// max(1, 1*3/4) puts both variants at a threshold of one.
					.withPenaltyThreshold(1));
		}

		/**
		 * A shard of one colour. The plain form resolves to the colour's tag, which also contains the
		 * boosted shard -- a boosted shard is an upgraded plain one, so anywhere V33a accepted a
		 * black shard it accepts a boosted black shard. A recipe that genuinely requires the boosted
		 * form asks for the boosted tag instead.
		 */
		/**
		 * A shard of one specific colour. This deliberately names the exact item rather than the
		 * colour tag: V33a ships plain and boosted variants of the group and rune recipes that differ
		 * only in which shard they consume, so a plain recipe that also accepted boosted shards would
		 * match alongside its boosted sibling and rob it of its doubled duration and output.
		 */
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
					grid, List.of(), runes, List.of(), new ItemStackTemplate(ChromaItems.CLUSTERS.get(output).get()), 20, 40)
					.withPenaltyThreshold(1));
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
					new ItemStackTemplate(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_STAR).get(), 4), 400, 400)
					.withPenaltyThreshold(1));
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
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_MIRROR).get()), 100, 200)
					.withPenaltyThreshold(12));
		}

		private void saveCrystalFocus() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,0},{2,0}}) stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.BLUE, true)));
			for (int[] pos : new int[][] {{0,-2},{0,2}}) stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.YELLOW, true)));
			for (int[] pos : new int[][] {{-2,-2},{-2,2},{2,-2},{2,2}}) stands.add(stand(pos[0], 0, pos[1], shard(CrystalElement.PURPLE, true)));
			saveRecipe("crystal_focus", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CLUSTERS.get(ChromaClusterItems.PRIMARY_CLUSTER).get()))),
					stands, List.of(), List.of(), new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_FOCUS).get()), 100, 200)
					.withPenaltyThreshold(24));
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
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_LENS).get()), 100, 200)
					.withPenaltyThreshold(48));
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
					new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get()), 100, 200)
					.withPenaltyThreshold(576));
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
					List.of(), List.of(), new ItemStackTemplate(ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get()), 100, 200)
					.withPenaltyThreshold(12));
		}

		/** Complete seven-tier V33a StorageCrystalRecipe family. */
		private void saveStorageCrystals() {
			int[][] outer = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},
					{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
			int[][] inner = {{-2,-2},{2,-2},{-2,2},{2,2},{0,-2},{0,2},{2,0},{-2,0}};
			List<RuneRequirement> runes = List.of(
					new RuneRequirement(new net.minecraft.core.BlockPos(4,0,1), CrystalElement.BLUE),
					new RuneRequirement(new net.minecraft.core.BlockPos(4,0,-1), CrystalElement.BLUE),
					new RuneRequirement(new net.minecraft.core.BlockPos(-4,0,1), CrystalElement.GREEN),
					new RuneRequirement(new net.minecraft.core.BlockPos(-4,0,-1), CrystalElement.GREEN),
					new RuneRequirement(new net.minecraft.core.BlockPos(1,0,-4), CrystalElement.RED),
					new RuneRequirement(new net.minecraft.core.BlockPos(-1,0,-4), CrystalElement.RED),
					new RuneRequirement(new net.minecraft.core.BlockPos(1,0,4), CrystalElement.YELLOW),
					new RuneRequirement(new net.minecraft.core.BlockPos(-1,0,4), CrystalElement.YELLOW),
					new RuneRequirement(new net.minecraft.core.BlockPos(4,0,-4), CrystalElement.WHITE),
					new RuneRequirement(new net.minecraft.core.BlockPos(-4,0,4), CrystalElement.WHITE),
					new RuneRequirement(new net.minecraft.core.BlockPos(-4,0,-4), CrystalElement.BLACK),
					new RuneRequirement(new net.minecraft.core.BlockPos(4,0,4), CrystalElement.BLACK));

			for (StorageCrystalTier tier : StorageCrystalTier.list) {
				List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
				if (tier == StorageCrystalTier.NULA) {
					for (int i = 0; i < outer.length; i++)
						stands.add(stand(outer[i][0], 1, outer[i][1],
								shard(CrystalElement.elements[i], true)));
				}
				else {
					for (int[] pos : outer)
						stands.add(stand(pos[0], 1, pos[1], tiered(ChromaTieredItems.CHROMA_DUST)));
				}
				Ingredient innerDust = tiered(tier == StorageCrystalTier.NULA
						? ChromaTieredItems.ELEMENT_DUST : ChromaTieredItems.RESONANCE_DUST);
				for (int[] pos : inner)
					stands.add(stand(pos[0], 0, pos[1], innerDust));

				Ingredient center = tier.previous() == null
						? Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get())
						: Ingredient.of(ChromaItems.STORAGE_CRYSTALS.get(tier.previous()).get());
				CastingTableRecipe recipe = new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
						List.of(new GridIngredient(4, center)), stands, runes, List.of(),
						new ItemStackTemplate(ChromaItems.STORAGE_CRYSTALS.get(tier).get()),
						50 << tier.legacyMetadata(), 200)
						.withCompletionBehavior(true, List.of(ProgressStage.STORAGE), List.of(0.5F, 2F))
						.withPenaltyThreshold(3);
				saveRecipe("storage_crystal/" + tier.name().toLowerCase(java.util.Locale.ROOT), recipe);
			}
		}

		/** Exact V33a CrystalChargerRecipe: crystal core center and its asymmetric eight-stand ring. */
		private void saveCrystalCharger() {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(0, 0, 2, Ingredient.of(Items.SMOOTH_STONE_SLAB)),
					stand(2, 0, -2, Ingredient.of(Items.OBSIDIAN)),
					stand(2, 0, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(-2, 0, -2, Ingredient.of(Items.OBSIDIAN)),
					stand(-2, 0, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(2, 0, 0, shard(CrystalElement.WHITE, false)),
					stand(-2, 0, 0, shard(CrystalElement.WHITE, false)),
					stand(0, 0, -2, shard(CrystalElement.WHITE, false)));
			saveRecipe("crystal_charger", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(
							ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get()))),
					stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.CRYSTAL_CHARGER.get().asItem()), 200, 200));
		}

		private void saveTintedLens(CrystalElement element, net.minecraft.world.level.ItemLike material,
				int amount, String materialName) {
			Ingredient input = Ingredient.of(material);
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					auxStand(-2, -2, input), auxStand(-2, 2, input),
					auxStand(2, -2, input), auxStand(2, 2, input),
					auxStand(-2, 0, tiered(ChromaTieredItems.FOCUS_DUST)),
					auxStand(2, 0, tiered(ChromaTieredItems.FOCUS_DUST)),
					auxStand(0, -2, Ingredient.of(ChromaItems.SHARDS.get(element).get())),
					auxStand(0, 2, Ingredient.of(ChromaItems.SHARDS.get(element).get())));
			saveRecipe("tinted_crystal_lens/" + element.getEnglishName() + "/" + materialName,
					new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
							List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CRAFTING.get(
									ChromaCraftingItems.CRYSTAL_LENS).get()))),
							stands, List.of(), List.of(),
							new ItemStackTemplate(ChromaItems.TINTED_LENSES.get(element).get(), amount),
							100, 200).withPenaltyThreshold(48));
		}

		/** Exact V33a FarmerRecipe, including all sixteen stands and both aura channels. */
		private void saveFarmer() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] point : new int[][] {
					{-4, -2}, {-4, 2}, {4, -2}, {4, 2},
					{-2, -4}, {-2, 4}, {2, -4}, {2, 4}})
				stands.add(auxStand(point[0], point[1], tiered(ChromaTieredItems.AURA_DUST)));
			stands.add(auxStand(-4, 0,
					Ingredient.of(ChromaItems.TINTED_LENSES.get(CrystalElement.GREEN).get())));
			stands.add(auxStand(4, 0, Ingredient.of(Items.IRON_HOE)));
			for (int[] point : new int[][] {
					{-4, -4}, {0, -4}, {4, -4}, {-4, 4}, {0, 4}, {4, 4}})
				stands.add(auxStand(point[0], point[1], Ingredient.of(Items.IRON_INGOT)));
			saveRecipe("farmer", new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CRAFTING.get(
							ChromaCraftingItems.ENERGY_CORE).get()))),
					List.copyOf(stands), List.of(),
					List.of(new AuraRequirement(CrystalElement.GREEN, 12000),
							new AuraRequirement(CrystalElement.YELLOW, 6000)),
					new ItemStackTemplate(ChromaBlocks.FARMER.get().asItem(), 3),
					400, 500).withPenaltyThreshold(6));
		}

		/** Exact no-Botania V33a recipes for the six currently ported magic plants. */
		private void saveMagicPlants() {
			saveShapedTemple("heat_lily",
					new ItemStackTemplate(ChromaBlocks.HEAT_LILY.get().asItem(), 3), 20, 40,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-3, -1, 0),
								CrystalElement.ORANGE),
							new RuneRequirement(new net.minecraft.core.BlockPos(3, -1, 0),
								CrystalElement.ORANGE)),
					Map.of('L', Ingredient.of(Items.LILY_PAD),
							'F', tag(Tags.Items.FLOWERS),
							'S', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.ORANGE).get()),
							'B', Ingredient.of(Items.BLAZE_POWDER)),
					" F ", "FBF", "LSL");

			saveShapedTemple("biome_reverter",
					new ItemStackTemplate(ChromaBlocks.BIOME_REVERTER.get().asItem(), 6), 20, 40,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(2, -1, -5),
								CrystalElement.GREEN),
							new RuneRequirement(new net.minecraft.core.BlockPos(-2, -1, 5),
								CrystalElement.MAGENTA)),
					Map.of('L', Ingredient.of(Blocks.FERN),
							'F', tag(Tags.Items.FLOWERS),
							'S', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.GREEN).get()),
							'B', Ingredient.of(Items.REDSTONE)),
					" F ", "FBF", "LSL");

			saveShapedTemple("plant_accelerator",
					new ItemStackTemplate(ChromaBlocks.PLANT_ACCELERATOR.get().asItem(), 2), 20, 40,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(2, -1, -4),
								CrystalElement.LIGHTBLUE),
							new RuneRequirement(new net.minecraft.core.BlockPos(-2, -1, 4),
								CrystalElement.GREEN)),
					Map.of('L', tag(net.minecraft.tags.ItemTags.LEAVES),
							'R', Ingredient.of(Items.REDSTONE),
							'F', tag(Tags.Items.FLOWERS),
							'S', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.LIGHTBLUE).get())),
					"LSL", "FRF", "LSL");

			saveShapedTemple("crop_speed_plant",
					new ItemStackTemplate(ChromaBlocks.CROP_SPEED_PLANT.get().asItem()), 20, 40,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(2, -1, -4),
								CrystalElement.LIGHTBLUE),
							new RuneRequirement(new net.minecraft.core.BlockPos(2, -1, -5),
								CrystalElement.GREEN)),
					Map.of('L', tag(net.minecraft.tags.ItemTags.LEAVES),
							'R', Ingredient.of(Items.REDSTONE),
							'F', tag(Tags.Items.FLOWERS),
							'S', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.LIGHTBLUE).get())),
					" R ", "FRF", "LSL");

			// V33a substitutes Aura Dust and vanilla redstone when Botania is absent. The optional
			// Munchdew/mana-resource branch remains deferred until Botania's 26.2 component form exists;
			// this is the exact recipe active in the present mod set.
			saveShapedTemple("harvest_plant",
					new ItemStackTemplate(ChromaBlocks.HARVEST_PLANT.get().asItem()), 20, 40,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(-5, 0, -2),
								CrystalElement.YELLOW),
							new RuneRequirement(new net.minecraft.core.BlockPos(-5, 0, 2),
								CrystalElement.GREEN)),
					Map.of('F', Ingredient.of(ChromaItems.CRAFTING.get(
								ChromaCraftingItems.LIVING_ESSENCE).get()),
							'A', tiered(ChromaTieredItems.AURA_DUST),
							'f', tag(Tags.Items.FLOWERS),
							'E', tiered(ChromaTieredItems.AURA_DUST),
							'L', tag(net.minecraft.tags.ItemTags.LEAVES),
							'R', Ingredient.of(Items.REDSTONE)),
					"FAF", "fEf", "LRL");

			saveShapedTemple("cobble_generator",
					new ItemStackTemplate(ChromaBlocks.COBBLE_GENERATOR.get().asItem()), 20, 40,
					List.of(),
					List.of(new RuneRequirement(new net.minecraft.core.BlockPos(4, -1, -3),
								CrystalElement.ORANGE),
							new RuneRequirement(new net.minecraft.core.BlockPos(4, -1, 3),
								CrystalElement.CYAN),
							new RuneRequirement(new net.minecraft.core.BlockPos(-4, -1, -1),
								CrystalElement.BROWN)),
					Map.of('L', tag(net.minecraft.tags.ItemTags.LEAVES),
							'S', Ingredient.of(ChromaItems.SHARDS.get(CrystalElement.CYAN).get()),
							'F', tag(Tags.Items.FLOWERS),
							'B', Ingredient.of(Blocks.GLASS),
							'D', Ingredient.of(Items.GLOWSTONE_DUST)),
					"LSL", "FBF", " D ");
		}

		/** Exact V33a RecipePersonalCharger, including its full rune ring and zero repeat-XP tail. */
		private void savePersonalCharger() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] point : new int[][] {{2,0},{-2,0},{0,2},{0,-2}})
				stands.add(stand(point[0], 0, point[1], tiered(ChromaTieredItems.SPACE_DUST)));
			for (int sx : new int[] {-2, 2}) for (int sz : new int[] {-2, 2})
				stands.add(stand(sx, 0, sz, tiered(ChromaTieredItems.BEACON_DUST)));
			for (int i = -2; i <= 2; i += 2) {
				stands.add(stand(-4, 1, i, Ingredient.of(Items.GLOWSTONE_DUST)));
				stands.add(stand(4, 1, i, Ingredient.of(Items.GLOWSTONE_DUST)));
				stands.add(stand(i, 1, 4, Ingredient.of(Items.GLOWSTONE_DUST)));
				stands.add(stand(i, 1, -4, Ingredient.of(Items.GLOWSTONE_DUST)));
			}
			for (int sx : new int[] {-4, 4}) for (int sz : new int[] {-4, 4})
				stands.add(stand(sx, 1, sz, Ingredient.of(Items.DIAMOND)));

			List<RuneRequirement> runes = new ArrayList<>();
			for (CrystalElement element : CrystalElement.elements)
				runes.add(runeRingRune(element));
			saveRecipe("personal_charger", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(
							ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get()))),
					stands, runes, List.of(),
					new ItemStackTemplate(ChromaBlocks.PERSONAL_CHARGER.get().asItem()),
					2400, 800).withPenalty(16, 0F));
		}

		/** Exact V33a InfuserRecipe: Item Stand center and eight Chroma Alloy Ingots. */
		private void saveItemAuraInfuser() {
			Ingredient alloy = Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.CHROMA_INGOT).get());
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(2, 0, 2, alloy), stand(-2, 0, 2, alloy),
					stand(4, 1, 2, alloy), stand(-4, 1, 2, alloy),
					stand(4, 1, 0, alloy), stand(-4, 1, 0, alloy),
					stand(2, 0, -2, alloy), stand(-2, 0, -2, alloy));
			saveRecipe("item_aura_infuser", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(ChromaBlocks.ITEM_STAND.get()))),
					stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.ITEM_INFUSER.get().asItem()), 100, 200));
		}

		/** Exact V33a PlayerInfuserRecipe; it upgrades the Item Infuser rather than replacing its base recipe. */
		private void savePlayerAuraInfuser() {
			Ingredient aura = Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.AURA_INGOT).get());
			Ingredient diamond = Ingredient.of(Items.DIAMOND);
			Ingredient resonance = tiered(ChromaTieredItems.RESONANCE_DUST);
			Ingredient complex = Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.COMPLEX_INGOT).get());
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(2, 0, 0, aura), stand(-2, 0, 0, aura),
					stand(4, 1, -2, aura), stand(-4, 1, -2, aura),
					stand(2, 0, 2, diamond), stand(-2, 0, 2, diamond),
					stand(-4, 1, 2, diamond), stand(4, 1, 2, diamond),
					stand(2, 0, -2, resonance), stand(0, 0, -2, resonance),
					stand(-2, 0, -2, resonance), stand(0, 0, 2, complex));
			saveRecipe("player_aura_infuser", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(ChromaBlocks.ITEM_INFUSER.get()))),
					stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaBlocks.PLAYER_INFUSER.get().asItem()), 100, 200));
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

		/** V33a IridescentCrystalRecipe: the player-tuned, stackable Power Crystal cast. */
		private void savePowerCrystal() {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					stand(-2, 0, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					stand(-4, 1, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					stand(2, 0, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					stand(4, 1, 0, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					stand(0, 0, -2, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					stand(0, 1, -4, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					stand(-4, 1, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(-2, 0, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(0, 0, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(2, 0, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(4, 1, 2, Ingredient.of(Items.OBSIDIAN)),
					stand(-2, 0, -2, Ingredient.of(Items.GLOWSTONE)),
					stand(2, 0, -2, Ingredient.of(Items.GLOWSTONE)));
			List<AuraRequirement> aura = List.of(
					new AuraRequirement(CrystalElement.YELLOW, 15000),
					new AuraRequirement(CrystalElement.BLACK, 25000),
					new AuraRequirement(CrystalElement.PURPLE, 10000));
			saveRecipe("power_crystal", new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(Items.DIAMOND))), stands, List.of(), aura,
					new ItemStackTemplate(ChromaBlocks.POWER_CRYSTAL.get().asItem()), 1600, 500,
					List.of(), 0.97489F, true, true).withPenaltyThreshold(96));
		}

		/** V33a PylonTurboRecipe: nine Energy Focal Points for the complete ritual ring. */
		private void savePylonTurbo() {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					auxStand(0, -4, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get())),
					auxStand(0, -2, Ingredient.of(Items.OBSIDIAN)),
					auxStand(2, 0, tiered(ChromaTieredItems.PURITY_DUST)),
					auxStand(-2, 0, tiered(ChromaTieredItems.PURITY_DUST)),
					auxStand(4, 0, tiered(ChromaTieredItems.LUMA_BEANS)),
					auxStand(-4, 0, tiered(ChromaTieredItems.LUMA_BEANS)),
					auxStand(4, 2, Ingredient.of(Items.GLOWSTONE_DUST)),
					auxStand(-4, 2, Ingredient.of(Items.GLOWSTONE_DUST)),
					auxStand(2, -2, Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.WHITE).get())),
					auxStand(-2, -2, Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.WHITE).get())),
					auxStand(2, -4, tiered(ChromaTieredItems.LUMEN_GEM)),
					auxStand(-2, -4, tiered(ChromaTieredItems.LUMEN_GEM)),
					auxStand(-2, 2, Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem())),
					auxStand(0, 2, Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem())),
					auxStand(2, 2, Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem())),
					auxStand(0, 4, Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem())),
					auxStand(-4, 4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(4, 4, Ingredient.of(Items.IRON_INGOT)),
					auxStand(-2, 4, Ingredient.of(Items.GOLD_INGOT)),
					auxStand(2, 4, Ingredient.of(Items.GOLD_INGOT)));

			List<RuneRequirement> runes = new ArrayList<>();
			List<AuraRequirement> aura = new ArrayList<>();
			for (CrystalElement element : CrystalElement.elements) {
				runes.add(runeRingRune(element));
				int amount = switch (element) {
					case PURPLE -> 300000;
					case BLACK -> 200000;
					case BLUE -> 150000;
					case YELLOW -> 250000;
					case WHITE -> 125000;
					default -> 50000;
				};
				aura.add(new AuraRequirement(element, amount));
			}
			runes.addAll(REPEATER_RUNES);

			saveRecipe("pylon_turbocharger", new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(
							ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get()))),
					stands, runes, aura,
					new ItemStackTemplate(ChromaBlocks.PYLON_TURBOCHARGER.get().asItem(), 9),
					9600, 500, List.of(), 0.75F, true, false, 16, 0F,
					CastingTableRecipe.CompletionBehavior.DEFAULT));
		}
		/**
		 * V33a {@code PortalRecipe}: the Portal Rift cast, and the most expensive recipe in the mod.
		 *
		 * <p>An Energized Void Core in the grid centre; sixteen stands ringing it with Energetic
		 * Essence and Spatial Rifting Powder on the inner ring and glowstone, ender eyes, Chromastone,
		 * beacons, End Stone and emeralds on the outer; the complete sixteen-rune ring plus the whole
		 * Crystal Repeater rune layout; and 120,000 aura of each primary element with 60,000 of each
		 * of the rest. It runs for 2,400 ticks and yields nine rifts — exactly the 3x3 pad.
		 *
		 * <p>Experience is the source's {@code super.getExperience()*50}, i.e. the pylon tier's 500
		 * multiplied fifty-fold. The penalty threshold of 1 with a multiplier of 0 is upstream's way
		 * of saying the reward is once-only: any repeat cast is worth nothing.
		 *
		 * <p>Its required progression is every prerequisite of {@link ProgressStage#DIMENSION}, read
		 * from the live DAG rather than restated, so the recipe cannot drift from the stage gate the
		 * rift itself enforces.
		 */
		private void savePortal() {
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			for (int[] pos : new int[][] {{-2,0},{2,0},{0,2},{0,-2}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ENERGY_POWDER).get())));
			for (int[] pos : new int[][] {{-2,-2},{2,-2},{-2,2},{2,2}})
				stands.add(auxStand(pos[0], pos[1], tiered(ChromaTieredItems.SPACE_DUST)));
			for (int[] pos : new int[][] {{-4,0},{4,0}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(Items.GLOWSTONE_DUST)));
			for (int[] pos : new int[][] {{0,4},{0,-4}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(Items.ENDER_EYE)));
			for (int[] pos : new int[][] {{-4,-4},{4,4}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.COMPLEX_INGOT).get())));
			for (int[] pos : new int[][] {{-4,4},{4,-4}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(Items.BEACON)));
			for (int[] pos : new int[][] {{-2,-4},{4,-2},{2,4},{-4,2}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(Items.END_STONE)));
			for (int[] pos : new int[][] {{2,-4},{-4,-2},{-2,4},{4,2}})
				stands.add(auxStand(pos[0], pos[1], Ingredient.of(Items.EMERALD)));

			List<RuneRequirement> runes = new ArrayList<>();
			List<AuraRequirement> aura = new ArrayList<>();
			for (CrystalElement e : CrystalElement.elements) {
				aura.add(new AuraRequirement(e, e.isPrimary() ? 120000 : 60000));
				runes.add(runeRingRune(e));
			}
			runes.addAll(REPEATER_RUNES);

			saveRecipe("portal_rift", new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(
							ChromaItems.CRAFTING.get(ChromaCraftingItems.HIGH_VOID_CORE).get()))),
					stands, runes, aura,
					new ItemStackTemplate(ChromaBlocks.PORTAL.get().asItem(), 9), 2400,
					// V33a RecipeType.PYLON carries 500 experience; PortalRecipe multiplies it by 50.
					500 * 50,
					List.copyOf(ProgressionManager.instance.getPrereqs(ProgressStage.DIMENSION)),
					0.75F, false, false, 1, 0F, CastingTableRecipe.CompletionBehavior.DEFAULT));
		}

		/** V33a addAuxItem(x, z): the inner ring sits on the table's own level, the outer one higher. */
		private CastingTableRecipe.StandIngredient auxStand(int x, int z, Ingredient item) {
			return stand(x, Math.abs(x) == 4 || Math.abs(z) == 4 ? 1 : 0, z, item);
		}

		/** V33a RepeaterRecipe's own rune layout, which PortalRecipe adds on top of the ring. */
		private static final List<RuneRequirement> REPEATER_RUNES = List.of(
				new RuneRequirement(new net.minecraft.core.BlockPos(0, -1, 5), CrystalElement.BLACK),
				new RuneRequirement(new net.minecraft.core.BlockPos(0, -1, -5), CrystalElement.BLACK),
				new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, 0), CrystalElement.BLACK),
				new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, 0), CrystalElement.BLACK),
				new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, 5), CrystalElement.WHITE),
				new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, -5), CrystalElement.WHITE),
				new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, -5), CrystalElement.WHITE),
				new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, 5), CrystalElement.WHITE),
				new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, -4), CrystalElement.RED),
				new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, 4), CrystalElement.RED),
				new RuneRequirement(new net.minecraft.core.BlockPos(5, -1, -4), CrystalElement.BLUE),
				new RuneRequirement(new net.minecraft.core.BlockPos(-5, -1, 4), CrystalElement.BLUE),
				new RuneRequirement(new net.minecraft.core.BlockPos(-4, -1, -5), CrystalElement.GREEN),
				new RuneRequirement(new net.minecraft.core.BlockPos(4, -1, 5), CrystalElement.GREEN),
				new RuneRequirement(new net.minecraft.core.BlockPos(4, -1, -5), CrystalElement.YELLOW),
				new RuneRequirement(new net.minecraft.core.BlockPos(-4, -1, 5), CrystalElement.YELLOW));

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

		/**
		 * V33a PendantRecipe (a MultiBlockCastingRecipe) and EnhancedPendantRecipe (a PylonCastingRecipe), per
		 * element. The Pendant grows from the colour's cave crystal: four times the tier's 100 ticks, its 200 XP, the
		 * colour's ring rune, and the element's three-note ding (root, third, fifth) as its completion harmonics.
		 * The Enhanced Pendant grows from the colour's potion crystal with that Pendant on the north stand: eight
		 * times the tier's 400 ticks, its 500 XP, and 8000 Purple, 2000 White and 16000 of its own colour, summed
		 * where they coincide as V33a's ElementTagCompound did. Neither overrides the typical crafted amount of 1,
		 * so both are XP-penalised after max(1, 1*3/4) = 1 craft.
		 */
		private void savePendants() {
			for (CrystalElement e : CrystalElement.elements) {
				List<Float> ding = new ArrayList<>();
				for (int i = 0; i <= 2; i++)
					ding.add(reika.chromaticraft.auxiliary.CrystalMusicManager.instance.getScaledDing(e, i));
				saveRecipe("pendant/" + e.getEnglishName(), new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
						List.of(new GridIngredient(4, Ingredient.of(ChromaBlocks.caveCrystal(e).get().asItem()))),
						List.of(auxStand(-2, -2, Ingredient.of(Items.GLOWSTONE)), auxStand(0, -2, Ingredient.of(Items.STRING)),
								auxStand(2, -2, Ingredient.of(Items.GLOWSTONE)),
								auxStand(-2, 0, Ingredient.of(Items.QUARTZ)), auxStand(2, 0, Ingredient.of(Items.QUARTZ)),
								auxStand(-2, 2, Ingredient.of(Items.ENDER_PEARL)), auxStand(2, 2, Ingredient.of(Items.ENDER_PEARL)),
								auxStand(0, 2, Ingredient.of(Items.DIAMOND))),
						List.of(runeRingRune(e)), List.of(),
						new ItemStackTemplate(ChromaItems.PENDANTS.get(e).get()), 400, 200)
						.withPenaltyThreshold(1)
						.withCompletionBehavior(false, List.of(), ding));

				java.util.EnumMap<CrystalElement, Integer> aura = new java.util.EnumMap<>(CrystalElement.class);
				aura.merge(CrystalElement.PURPLE, 8000, Integer::sum);
				aura.merge(CrystalElement.WHITE, 2000, Integer::sum);
				aura.merge(e, 16000, Integer::sum);
				List<AuraRequirement> auraList = new ArrayList<>();
				aura.forEach((element, amount) -> auraList.add(new AuraRequirement(element, amount)));
				saveRecipe("enhanced_pendant/" + e.getEnglishName(), new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
						List.of(new GridIngredient(4, Ingredient.of(ChromaBlocks.superCrystal(e).get().asItem()))),
						List.of(auxStand(-2, -2, Ingredient.of(Items.DIAMOND)),
								auxStand(0, -2, Ingredient.of(ChromaItems.PENDANTS.get(e).get())),
								auxStand(2, -2, Ingredient.of(Items.DIAMOND)),
								auxStand(-2, 0, Ingredient.of(Items.GOLD_INGOT)), auxStand(2, 0, Ingredient.of(Items.GOLD_INGOT)),
								auxStand(-2, 2, Ingredient.of(Items.ENDER_EYE)), auxStand(2, 2, Ingredient.of(Items.ENDER_EYE)),
								auxStand(0, 2, Ingredient.of(Items.GHAST_TEAR))),
						List.of(), List.copyOf(auraList),
						new ItemStackTemplate(ChromaItems.BOOSTED_PENDANTS.get(e).get()), 3200, 500)
						.withPenaltyThreshold(1));
			}
		}

		/**
		 * V33a PurifyCrystalRecipe (a PylonCastingRecipe) on an Iridescent Crystal Chunk: twice the tier's 400 ticks,
		 * its 500 XP, a zero repeat-XP multiplier, four placed runes plus three ring runes, twenty-four stands, and
		 * 40000 Black, 60000 White, 20000 Red, 20000 Magenta and 10000 Blue.
		 */
		private void savePurificationGem() {
			Ingredient purity = tiered(ChromaTieredItems.PURITY_DUST);
			Ingredient raw = Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.RAW_CRYSTAL).get());
			Ingredient lapis = Ingredient.of(Items.LAPIS_LAZULI);
			Ingredient energy = Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.ENERGY_POWDER).get());
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					auxStand(-2, 0, purity), auxStand(2, 0, purity), auxStand(0, 2, purity), auxStand(0, -2, purity),
					auxStand(2, -2, raw), auxStand(-2, 2, raw),
					auxStand(2, -4, purity), auxStand(-2, 4, purity), auxStand(4, -2, purity), auxStand(-4, 2, purity),
					auxStand(0, -4, lapis), auxStand(0, 4, lapis), auxStand(4, 0, lapis), auxStand(-4, 0, lapis),
					auxStand(-2, -2, lapis), auxStand(2, 2, lapis),
					auxStand(-4, -2, energy), auxStand(-2, -4, energy), auxStand(4, 2, energy), auxStand(2, 4, energy),
					auxStand(-4, -4, Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.MULTICHROMIC).get().asItem())),
					auxStand(4, 4, Ingredient.of(ChromaBlocks.crystallineStone(StoneTypes.STABILIZER).get().asItem())),
					auxStand(-4, 4, shard(CrystalElement.BLACK, true)),
					auxStand(4, -4, shard(CrystalElement.WHITE, true)));
			List<RuneRequirement> runes = List.of(
					new RuneRequirement(new net.minecraft.core.BlockPos(-4, -1, 2), CrystalElement.WHITE),
					new RuneRequirement(new net.minecraft.core.BlockPos(3, -1, 2), CrystalElement.BLACK),
					new RuneRequirement(new net.minecraft.core.BlockPos(0, -1, -3), CrystalElement.BLACK),
					new RuneRequirement(new net.minecraft.core.BlockPos(0, -1, -4), CrystalElement.WHITE),
					runeRingRune(CrystalElement.RED), runeRingRune(CrystalElement.MAGENTA), runeRingRune(CrystalElement.BLUE));
			saveRecipe("purification_gem", new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get()))),
					stands, runes,
					List.of(new AuraRequirement(CrystalElement.BLACK, 40000), new AuraRequirement(CrystalElement.WHITE, 60000),
							new AuraRequirement(CrystalElement.RED, 20000), new AuraRequirement(CrystalElement.MAGENTA, 20000),
							new AuraRequirement(CrystalElement.BLUE, 10000)),
					new ItemStackTemplate(ChromaItems.PURIFY.get()), 800, 500)
					.withPenalty(1, 0F));
		}

		/**
		 * V33a ChargedItemPlayerBufferConnectionRecipe ("Lumen Buffer Connection"), a PylonCastingRecipe: the powered
		 * item itself in the centre, whatever its data, comes back with its data kept and {@code bufferlinked} set, so
		 * {@code PylonCharging.chargePlayerTools} tops it up from the player's lumen buffer. Half the tier's 400
		 * ticks, two fifths of its 500 XP, 6000 per cost factor of the item's own colour (its PoweredItem.getColor), twenty stands, and left
		 * off the lexicon's recipe pages ({@code isIndexed() == false}).
		 */
		private <I extends net.minecraft.world.item.Item & reika.chromaticraft.magic.interfaces.PoweredItem> void saveBufferLink(
				String name, I item, CrystalElement color, int costFactor) {
			Ingredient beans = tiered(ChromaTieredItems.LUMA_BEANS);
			Ingredient lumen = tiered(ChromaTieredItems.LUMEN_GEM);
			Ingredient redstone = Ingredient.of(Items.REDSTONE);
			Ingredient glow = Ingredient.of(Items.GLOWSTONE_DUST);
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					auxStand(-2, -2, beans), auxStand(2, -2, beans), auxStand(-2, 2, beans), auxStand(2, 2, beans),
					auxStand(0, -2, lumen), auxStand(2, 0, lumen), auxStand(0, 2, lumen), auxStand(-2, 0, lumen),
					auxStand(0, -4, redstone), auxStand(4, 0, redstone), auxStand(0, 4, redstone), auxStand(-4, 0, redstone),
					auxStand(4, -2, glow), auxStand(-2, -4, glow), auxStand(-4, 2, glow), auxStand(2, 4, glow),
					auxStand(4, 2, glow), auxStand(-2, 4, glow), auxStand(-4, -2, glow), auxStand(2, -4, glow));
			net.minecraft.nbt.CompoundTag linked = new net.minecraft.nbt.CompoundTag();
			linked.putBoolean("bufferlinked", true);
			ItemStackTemplate result = new ItemStackTemplate(item.builtInRegistryHolder(), 1,
					net.minecraft.core.component.DataComponentPatch.builder()
							.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
									net.minecraft.world.item.component.CustomData.of(linked))
							.build());
			saveRecipe("buffer_link/" + name, new CastingTableRecipe(CastingTableRecipe.Tier.PYLON,
					List.of(new GridIngredient(4, Ingredient.of(item))), stands, List.of(),
					List.of(new AuraRequirement(color, 6000*costFactor)), result, 200, 200)
					.withCompletionBehavior(true, List.of(), List.of())
					.unindexed());
		}

		private Ingredient tiered(ChromaTieredItems item) { return Ingredient.of(ChromaItems.TIERED.get(item).get()); }
		private CastingTableRecipe.StandIngredient stand(int x, int y, int z, Ingredient item) { return new CastingTableRecipe.StandIngredient(new net.minecraft.core.BlockPos(x, y, z), item); }
		private void save(String name, StoneTypes result, int count,
				Map<Character, Ingredient> ingredients, String... pattern) {
			save(name, result, count, List.of(), ingredients, pattern);
		}

		private void save(String name, StoneTypes result, int count, List<ProgressStage> progress,
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
					new ItemStackTemplate(ChromaBlocks.crystallineStone(result).get().asItem(), count), 5, 5,
					progress);
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
			saveRecipe(name, new CastingTableRecipe(CastingTableRecipe.Tier.CRAFTING,
					shapedGrid(ingredients, pattern), List.of(), List.of(), List.of(),
					result, duration, energy, progress));
		}

		private static List<GridIngredient> shapedGrid(Map<Character, Ingredient> ingredients, String... pattern) {
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
			return grid;
		}

		/** As {@link #saveShapedProgress} but at V33a's {@code TempleCastingRecipe} tier, with runes. */
		private void saveShapedTemple(String name, ItemStackTemplate result, int duration, int energy,
				List<ProgressStage> progress, List<RuneRequirement> runes,
				Map<Character, Ingredient> ingredients, String... pattern) {
			CastingTableRecipe recipe = new CastingTableRecipe(CastingTableRecipe.Tier.TEMPLE,
					shapedGrid(ingredients, pattern), List.of(), runes, List.of(),
					result, duration, energy, progress);
			// V33a WeakRepeaterRecipe.canRunRecipe: research tier ENERGY or above.
			if (name.equals("weak_repeater"))
				recipe = recipe.withMinimumResearch(ResearchLevel.ENERGY);
			saveRecipe(name, recipe);
		}

		/**
		 * V33a {@code RecipeCrystalRepeater} (a {@code RepeaterRecipe}): sixteen Crystal Repeaters from
		 * a Crystal Core, with Transmissive Dust on all sixteen outer-ring stands, charged white shards
		 * on the four inner corners and the two inner X-axis stands, and Lumenite on the two Z-axis ones.
		 *
		 * <p>Multiblock tier. Duration is the tier's 100 ticks x2.4 (RepeaterRecipe) x4 = 960; experience
		 * the tier's 200 x2 = 400. RepeaterRecipe's rune layout, its A5/D5 + F#5/D5 chord, and the
		 * tuning key. {@code canRunRecipe} also needs research tier NETWORKING (having cast any
		 * multiblock recipe, plus that tier's fragments) and BLOWREPEATER; {@code getRequiredProgress}
		 * adds every prerequisite of REPEATER, read from the live DAG; {@code onCrafted} grants REPEATER,
		 * which is what opens the PYLONCRAFT research tier. The result carries {@code boosted:false}
		 * (turbochargeable repeaters always do) and the caster's UUID (networking-tier tiles).
		 */
		/**
		 * V33a {@code EnderEyeRecipe}: three Enhanced Ender Eyes on an emerald, with three ender eyes, one
		 * Element Dust, two Transmissive Dust and two Aura Dust on the inner stands. Multiblock tier at its
		 * base 100 ticks and 200 XP; {@code getOutputTag} binds the eyes to the caster as {@code owner}.
		 */
		private void saveEnderEye() {
			List<CastingTableRecipe.StandIngredient> stands = List.of(
					auxStand(-2, -2, Ingredient.of(Items.ENDER_EYE)),
					auxStand(2, -2, Ingredient.of(Items.ENDER_EYE)),
					auxStand(0, 2, Ingredient.of(Items.ENDER_EYE)),
					auxStand(0, -2, tiered(ChromaTieredItems.ELEMENT_DUST)),
					auxStand(-2, 2, tiered(ChromaTieredItems.BEACON_DUST)),
					auxStand(2, 2, tiered(ChromaTieredItems.BEACON_DUST)),
					auxStand(-2, 0, tiered(ChromaTieredItems.AURA_DUST)),
					auxStand(2, 0, tiered(ChromaTieredItems.AURA_DUST)));
			saveRecipe("enhanced_ender_eye", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(Items.EMERALD))), stands, List.of(), List.of(),
					new ItemStackTemplate(ChromaItems.ENDER_EYE.get(), 3), 100, 200)
					.withPenaltyThreshold(1).withCasterStamp("owner"));
		}

		private void saveCrystalRepeater() {
			Ingredient dust = tiered(ChromaTieredItems.BEACON_DUST);
			Ingredient white = Ingredient.of(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.WHITE).get());
			Ingredient lumen = tiered(ChromaTieredItems.LUMEN_GEM);
			List<CastingTableRecipe.StandIngredient> stands = new ArrayList<>();
			int[][] ring = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},
					{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
			for (int[] pos : ring)
				stands.add(auxStand(pos[0], pos[1], dust));
			for (int[] pos : new int[][] {{-2,-2},{2,-2},{-2,2},{2,2},{2,0},{-2,0}})
				stands.add(auxStand(pos[0], pos[1], white));
			stands.add(auxStand(0, -2, lumen));
			stands.add(auxStand(0, 2, lumen));

			net.minecraft.nbt.CompoundTag boosted = new net.minecraft.nbt.CompoundTag();
			boosted.putBoolean("boosted", false);
			ItemStackTemplate result = new ItemStackTemplate(
					ChromaBlocks.REPEATER.get().asItem().builtInRegistryHolder(), 16,
					net.minecraft.core.component.DataComponentPatch.builder()
							.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
									net.minecraft.world.item.component.CustomData.of(boosted))
							.build());

			List<ProgressStage> progress = new ArrayList<>(ProgressionManager.instance.getPrereqs(ProgressStage.REPEATER));
			if (!progress.contains(ProgressStage.BLOWREPEATER))
				progress.add(ProgressStage.BLOWREPEATER);
			List<Float> chord = List.of(
					(float)reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.A5.getRatio(
							reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.D5),
					(float)reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.Fs5.getRatio(
							reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.D5));

			saveRecipe("crystal_repeater", new CastingTableRecipe(CastingTableRecipe.Tier.MULTIBLOCK,
					List.of(new GridIngredient(4, Ingredient.of(
							ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get()))),
					stands, REPEATER_RUNES, List.of(), result, 960, 400, List.copyOf(progress),
					0.75F, true, true, 1, 0.75F, CastingTableRecipe.CompletionBehavior.DEFAULT)
					.withCompletionBehavior(false, List.of(ProgressStage.REPEATER), chord)
					.withCasterStamp("caster")
					.withMinimumResearch(ResearchLevel.NETWORKING));
		}

		/**
		 * V33a {@code TempleCastingRecipe.runeRing}: the sixteen floor positions one block below the
		 * table, indexed by element ordinal, walking the ring from the -X/-Z corner clockwise.
		 */
		private static RuneRequirement runeRingRune(CrystalElement element) {
			int[] offset = RUNE_RING[element.ordinal()];
			return new RuneRequirement(new net.minecraft.core.BlockPos(offset[0], -1, offset[1]), element);
		}

		private static final int[][] RUNE_RING = {
				{-2,-2}, {-1,-2}, {0,-2}, {1,-2}, {2,-2}, {2,-1}, {2,0}, {2,1},
				{2,2}, {1,2}, {0,2}, {-1,2}, {-2,2}, {-2,1}, {-2,0}, {-2,-1}
		};

		/**
		 * Ported recipes whose V33a class returns {@code canGiveDoubleOutput() == true} (directly or through
		 * DecoCastingRecipe / the core-recipe bases), by recipe id or id folder: RuneRecipe, EnhancedRuneRecipe, CrystalSeedRecipe,
		 * RelayRecipe, CrystalGlassRecipe, CrystalGlowRecipe, SelectiveGlassRecipe, CrystalStoneRecipe, CrystalGroupRecipe, CrystalClusterRecipe,
		 * LowCoreRecipe and HighCoreRecipe (and their subclasses), CrystalMirrorRecipe, RawCrystalRecipe,
		 * LumenCoreRecipe, IridescentChunkRecipe, ElementUnitRecipe, CrystalStarRecipe, CrystalLensRecipe,
		 * CrystalFocusRecipe and CrystalCoreRecipe.
		 */
		private static final List<String> DOUBLE_OUTPUT = List.of("crystal_rune/", "crystal_seeds/", "lumen_relay/", "crystal_glass/",
				"crystal_glow/", "selective_glass", "chroma_flower",
				"crystal_stone/", "crystal_group/", "crystal_cluster/", "low_core/", "high_core/", "crystal_mirror/",
				"raw_crystal", "lumen_core", "iridescent_chunk", "element_unit", "crystal_star", "crystal_lens",
				"crystal_focus", "crystal_core");

		private static boolean doublesOutput(String name) {
			for (String id : DOUBLE_OUTPUT) {
				if (id.endsWith("/") ? name.startsWith(id) : name.equals(id))
					return true;
			}
			return false;
		}

		private void saveRecipe(String name, CastingTableRecipe recipe) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name));
			output.accept(key, doublesOutput(name) ? recipe.withDoubleOutput() : recipe, null);
		}
	}
}
