package reika.chromaticraft.modinterface.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingRecipe;
import reika.chromaticraft.client.ChromaClientRecipes;
import reika.chromaticraft.client.ChromaClientScreens;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaRecipeTypes;
import reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer;

import java.util.*;

/**
 * JEI integration for the Casting Table and Pool Recipes, added by request (V33a's NEI config deliberately left
 * casting recipes to the Lexicon). A recipe appears only once the player could read it in their Chromic Lexicon
 * ({@link ChromaClientRecipes#isVisible}); visibility is re-applied as their research and progress change. Each
 * recipe carries a button that opens its Lexicon page, shown while the player carries a Lexicon holding it.
 */
@JeiPlugin
public class ChromaJEIPlugin implements IModPlugin {

	public static final Identifier UID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "jei_plugin");

	private static IRecipeHolderType<CastingTableRecipe> castingType;
	private static IRecipeHolderType<PoolAlloyingRecipe> poolType;

	/** One V33a {@code CrystalBrewerHandler} recipe: a shard of this colour, plain or boosted. */
	public record BrewingRecipe(reika.chromaticraft.registry.CrystalElement color, boolean boosted) {}

	public static final IRecipeType<BrewingRecipe> BREWING = IRecipeType.create(ChromatiCraft.MODID, "crystal_brewing",
			BrewingRecipe.class);

	private static @Nullable IJeiRuntime runtime;
	private static boolean tickRegistered;
	/** Recipe instances this plugin has put into the current runtime, and those it has hidden there. */
	private static final Set<RecipeHolder<?>> added = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<RecipeHolder<?>> hidden = Collections.newSetFromMap(new IdentityHashMap<>());
	private static int tick;

	@Override
	public Identifier getPluginUid() {
		return UID;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		for (var holder : ChromaItems.ITEMS.getEntries()) {
			var item = holder.get();
			if (item instanceof reika.chromaticraft.items.tools.ItemCrystalPotion
					|| item instanceof reika.chromaticraft.items.tools.ItemSplashCrystalPotion)
				registration.registerFromDataComponentTypes(item, DataComponents.POTION_CONTENTS);
			else if (item instanceof reika.chromaticraft.base.ItemPoweredChromaTool
					|| item instanceof reika.chromaticraft.items.ItemStorageCrystal
					|| item instanceof reika.chromaticraft.item.ItemInfoFragment
					|| item instanceof reika.chromaticraft.item.ItemChromaBook
					|| item instanceof reika.chromaticraft.items.tools.ItemEnderCrystal)
				registration.registerFromDataComponentTypes(item, DataComponents.CUSTOM_DATA);
		}
	}

	private static IRecipeHolderType<CastingTableRecipe> castingType() {
		if (castingType == null)
			castingType = IRecipeHolderType.create(ChromaRecipeTypes.CASTING.get());
		return castingType;
	}

	private static IRecipeHolderType<PoolAlloyingRecipe> poolType() {
		if (poolType == null)
			poolType = IRecipeHolderType.create(ChromaRecipeTypes.POOL_ALLOYING.get());
		return poolType;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new CastingCategory(gui), new PoolCategory(gui), new BrewingCategory(gui));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(castingType(), ChromaBlocks.CASTING_TABLE.get());
		registration.addCraftingStation(poolType(), ChromaItems.CHROMA_BUCKET.get());
		registration.addCraftingStation(BREWING, ChromaBlocks.CRYSTAL_BREWER.get());
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		// The runtime is not up yet; recipes are added (and filtered) as soon as it is, see onRuntimeAvailable.
		// Crystal brewing is not a data recipe and V33a's NEI handler was never gated: all 32 shards, always.
		List<BrewingRecipe> brewing = new ArrayList<>();
		for (boolean boosted : new boolean[] {false, true})
			for (reika.chromaticraft.registry.CrystalElement e : reika.chromaticraft.registry.CrystalElement.elements)
				brewing.add(new BrewingRecipe(e, boosted));
		registration.addRecipes(BREWING, brewing);
	}

	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		registration.addRecipeButtonFactory(new LexiconButtonFactory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
		added.clear();
		hidden.clear();
		if (!tickRegistered) {
			tickRegistered = true;
			NeoForge.EVENT_BUS.addListener(ChromaJEIPlugin::onClientTick);
		}
		reconcile();
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
		added.clear();
		hidden.clear();
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		if (runtime != null && ++tick % 20 == 0)
			reconcile();
	}

	/** Adds any synced recipes JEI does not have yet, then hides what the player cannot see and unhides the rest. */
	private static void reconcile() {
		IJeiRuntime rt = runtime;
		Player player = Minecraft.getInstance().player;
		if (rt == null || player == null)
			return;
		IRecipeManager manager = rt.getRecipeManager();
		addMissing(manager, castingType(), ChromaClientRecipes.casting());
		addMissing(manager, poolType(), ChromaClientRecipes.pool());

		Set<String> fragments = ChromaClientRecipes.fragments(player);
		List<RecipeHolder<CastingTableRecipe>> castHide = new ArrayList<>();
		List<RecipeHolder<CastingTableRecipe>> castShow = new ArrayList<>();
		for (RecipeHolder<CastingTableRecipe> r : ChromaClientRecipes.casting())
			sort(r, ChromaClientRecipes.isVisible(player, r.value(), fragments), castHide, castShow);
		List<RecipeHolder<PoolAlloyingRecipe>> poolHide = new ArrayList<>();
		List<RecipeHolder<PoolAlloyingRecipe>> poolShow = new ArrayList<>();
		for (RecipeHolder<PoolAlloyingRecipe> r : ChromaClientRecipes.pool())
			sort(r, ChromaClientRecipes.isVisible(player, r.value(), fragments), poolHide, poolShow);
		if (!castHide.isEmpty()) manager.hideRecipes(castingType(), castHide);
		if (!castShow.isEmpty()) manager.unhideRecipes(castingType(), castShow);
		if (!poolHide.isEmpty()) manager.hideRecipes(poolType(), poolHide);
		if (!poolShow.isEmpty()) manager.unhideRecipes(poolType(), poolShow);
	}

	private static <R extends net.minecraft.world.item.crafting.Recipe<?>> void addMissing(IRecipeManager manager,
			IRecipeHolderType<R> type, List<RecipeHolder<R>> recipes) {
		List<RecipeHolder<R>> missing = new ArrayList<>();
		for (RecipeHolder<R> r : recipes) {
			if (added.add(r))
				missing.add(r);
		}
		if (!missing.isEmpty())
			manager.addRecipes(type, missing);
	}

	/** Queues only changes: a recipe is hidden once when it becomes invisible and unhidden once when it returns. */
	private static <T extends RecipeHolder<?>> void sort(T recipe, boolean visible, List<T> hide, List<T> show) {
		if (visible) {
			if (hidden.remove(recipe))
				show.add(recipe);
		}
		else if (hidden.add(recipe)) {
			hide.add(recipe);
		}
	}

	private static List<ItemStack> stacks(Ingredient ingredient, int count) {
		@SuppressWarnings("deprecation")
		List<ItemStack> list = ingredient.items().map(h -> new ItemStack(h, count)).toList();
		return list;
	}

	private static String titleCase(String name) {
		String s = name.toLowerCase(Locale.ROOT);
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

    /**
     * The casting recipe as a map, as the Lexicon draws it: the 3x3 table grid in the middle and each Item Stand at
     * its place around the table (inner ring at 38px, outer ring at 64/63px, V33a's own spacing), with the tier,
     * duration and any pylon aura or runes noted below.
     */
    private record CastingCategory(IDrawable icon) implements IRecipeCategory<RecipeHolder<CastingTableRecipe>> {

        private static final int MID_X = 72;
        private static final int MID_Y = 66;

        private CastingCategory(IGuiHelper gui) {
            this(gui.createDrawableItemLike(ChromaBlocks.CASTING_TABLE.get()));
        }

        @Override
        public IRecipeHolderType<CastingTableRecipe> getRecipeType() {
            return castingType();
        }

        @Override
        public Component getTitle() {
            return ChromaBlocks.CASTING_TABLE.get().getName();
        }

        @Override
        public int getWidth() {
            return 176;
        }

        @Override
        public int getHeight() {
            return 176;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CastingTableRecipe> holder, IFocusGroup focuses) {
            CastingTableRecipe recipe = holder.value();
            for (CastingTableRecipe.GridIngredient g : recipe.grid()) {
                int slot = g.slot();
                builder.addSlot(RecipeIngredientRole.INPUT, MID_X + (slot % 3 - 1) * 18, MID_Y + (slot / 3 - 1) * 18)
                        .setStandardSlotBackground().add(g.ingredient());
            }
            for (CastingTableRecipe.StandIngredient s : recipe.stands()) {
                int i = s.offset().getX();
                int k = s.offset().getZ();
                int tx = Math.abs(i) == 2 ? 38 : 64;
                int ty = Math.abs(k) == 2 ? 38 : 63;
                builder.addSlot(RecipeIngredientRole.INPUT, MID_X + Integer.signum(i) * tx, MID_Y + Integer.signum(k) * ty)
                        .setStandardSlotBackground().add(s.ingredient())
                        .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(
                                "Item Stand at " + i + ", " + k)));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 156, 2).setOutputSlotBackground().add(recipe.output());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<CastingTableRecipe> holder, IFocusGroup focuses) {
            CastingTableRecipe recipe = holder.value();
            List<FormattedText> lines = new ArrayList<>();
            lines.add(Component.literal(titleCase(recipe.tier().name()) + " recipe, " + recipe.duration() / 20F + "s"));
            if (!recipe.runes().isEmpty())
                lines.add(Component.literal(recipe.runes().size() + " runes (see Lexicon)"));
            if (!recipe.aura().isEmpty()) {
                StringBuilder aura = new StringBuilder("Aura:");
                for (CastingTableRecipe.AuraRequirement a : recipe.aura())
                    aura.append(' ').append(a.element().displayName).append(' ').append(a.amount());
                lines.add(Component.literal(aura.toString()));
            }
            builder.addText(lines, 2, 146).setColor(0xff404040);
        }
    }

    /**
     * A Pool Recipe: the catalyst and the counted items dropped together into a Liquid Chroma source.
     */
    private record PoolCategory(IDrawable icon) implements IRecipeCategory<RecipeHolder<PoolAlloyingRecipe>> {

        private PoolCategory(IGuiHelper gui) {
            this(gui.createDrawableItemLike(ChromaItems.CHROMA_BUCKET.get()));
        }

        @Override
        public IRecipeHolderType<PoolAlloyingRecipe> getRecipeType() {
            return poolType();
        }

        @Override
        public Component getTitle() {
            return Component.literal("Pool Alloying");
        }

        @Override
        public int getWidth() {
            return 160;
        }

        @Override
        public int getHeight() {
            return 72;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PoolAlloyingRecipe> holder, IFocusGroup focuses) {
            PoolAlloyingRecipe recipe = holder.value();
            builder.addSlot(RecipeIngredientRole.INPUT, 2, 2).setStandardSlotBackground().add(recipe.catalyst())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal("Catalyst")));
            List<PoolAlloyingRecipe.CountedIngredient> ingredients = recipe.ingredients();
            for (int i = 0; i < ingredients.size(); i++) {
                PoolAlloyingRecipe.CountedIngredient c = ingredients.get(i);
                builder.addSlot(RecipeIngredientRole.INPUT, 24 + i % 5 * 18, 2 + i / 5 * 18)
                        .setStandardSlotBackground().addItemStacks(stacks(c.ingredient(), c.count()));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 140, 20).setOutputSlotBackground().add(recipe.result());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<PoolAlloyingRecipe> holder, IFocusGroup focuses) {
            PoolAlloyingRecipe recipe = holder.value();
            builder.addRecipeArrow().setPosition(116, 20);
            List<FormattedText> lines = new ArrayList<>();
            lines.add(Component.literal("Drop into Liquid Chroma"));
            if (recipe.minimumDuration() > 0)
                lines.add(Component.literal("At least " + recipe.minimumDuration() / 20 + "s"));
            builder.addText(lines, 2, 50).setColor(0xff404040);
        }
    }

    /**
     * V33a {@code CrystalBrewerHandler} ("Crystal Brewing"): the shard over a bottle and what the Brewery makes of it.
     * The plain colours take any bottle without an effect; black and purple cycle through every potion with one, the
     * output following the input.
     */
    private record BrewingCategory(IDrawable icon) implements IRecipeCategory<BrewingRecipe> {

        private BrewingCategory(IGuiHelper gui) {
            this(gui.createDrawableItemLike(ChromaBlocks.CRYSTAL_BREWER.get()));
        }

        @Override
        public IRecipeType<BrewingRecipe> getRecipeType() {
            return BREWING;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Crystal Brewing");
        }

        @Override
        public int getWidth() {
            return 96;
        }

        @Override
        public int getHeight() {
            return 48;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BrewingRecipe recipe, IFocusGroup focuses) {
            ItemStack shard = new ItemStack(recipe.boosted() ? ChromaItems.BOOSTED_SHARDS.get(recipe.color()).get()
                    : ChromaItems.SHARDS.get(recipe.color()).get());
            builder.addSlot(RecipeIngredientRole.INPUT, 39, 0).setStandardSlotBackground().add(shard);
            List<ItemStack> in = new ArrayList<>();
            List<ItemStack> out = new ArrayList<>();
            if (CrystalPotionController.instance.isPotionModifier(recipe.color())) {
                BuiltInRegistries.POTION.listElements()
                        .filter(h -> !h.value().getEffects().isEmpty())
                        .forEach(h -> in.add(PotionContents.createItemStack(
                                Items.POTION, h)));
                for (var potion : ChromaItems.CRYSTAL_POTIONS.values())
                    in.add(potion.get().getDefaultInstance());
                for (ItemStack is : in)
                    out.add(TileEntityCrystalBrewer.getPotionStackFromColor(
                            is, recipe.color(), recipe.boosted()));
            } else {
                for (var potion : List.of(Potions.WATER,
                        Potions.AWKWARD, Potions.MUNDANE,
                        Potions.THICK))
                    in.add(PotionContents.createItemStack(
                            Items.POTION, potion));
                out.add(TileEntityCrystalBrewer.getPotionStackFromColor(
                        in.getFirst(), recipe.color(), recipe.boosted()));
            }
            var input = builder.addSlot(RecipeIngredientRole.INPUT, 12, 26).setStandardSlotBackground().addItemStacks(in);
            var output = builder.addSlot(RecipeIngredientRole.OUTPUT, 66, 26).setOutputSlotBackground().addItemStacks(out);
            if (out.size() == in.size())
                builder.createFocusLink(input, output);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, BrewingRecipe recipe,
                                       IFocusGroup focuses) {
            builder.addRecipeArrow().setPosition(34, 27);
        }
    }

    /**
     * The "open in Lexicon" button beside each ChromatiCraft recipe.
     */
    private record LexiconButtonFactory(IGuiHelper gui) implements IRecipeButtonControllerFactory {

        @Override
        public <T> @Nullable IIconButtonController createButtonController(IRecipeLayoutDrawable<T> layout) {
            ItemStack output = layout.getRecipe() instanceof RecipeHolder<?> holder ? switch (holder.value()) {
                case CastingTableRecipe c -> c.output();
                case PoolAlloyingRecipe p -> p.result();
                default -> ItemStack.EMPTY;
            } : layout.getRecipe() instanceof BrewingRecipe ? new ItemStack(ChromaBlocks.CRYSTAL_BREWER.get()) : ItemStack.EMPTY;
            LexiconCatalog.Entry page = ChromaClientRecipes.pageFor(output);
            if (page == null)
                return null;
            IDrawable book = gui.createDrawableItemLike(ChromaItems.LEXICON.get());
            return new IIconButtonController() {
                @Override
                public void initState(IButtonState state) {
                    state.setIcon(book);
                    state.setVisible(!ChromaClientRecipes.carriedLexiconWith(page).isEmpty());
                }

                @Override
                public void updateState(IButtonState state) {
                    state.setVisible(!ChromaClientRecipes.carriedLexiconWith(page).isEmpty());
                }

                @Override
                public void getTooltips(ITooltipBuilder tooltip) {
                    tooltip.add(Component.literal("Open in Chromic Lexicon: ").append(page.title()));
                }

                @Override
                public boolean onPress(IJeiUserInput input) {
                    ItemStack lexicon = ChromaClientRecipes.carriedLexiconWith(page);
                    Player player = Minecraft.getInstance().player;
                    if (lexicon.isEmpty() || player == null)
                        return false;
                    if (!input.isSimulate())
                        ChromaClientScreens.openLexiconAt(player, lexicon, page);
                    return true;
                }
            };
        }
    }

}
