package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.item.ItemChromaBook;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.LexiconDescriptions;
import reika.chromaticraft.magic.progression.LexiconData;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ResearchProgress;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionDescriptions;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderer;
import net.minecraft.core.registries.BuiltInRegistries;

/** First 26.2 rendering pass for V33a's navigation and basic-description guide screens. */
public final class ScreenChromicLexicon extends Screen {

	private static final Identifier NAVIGATION = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lexicon/navigation2.png");
	/**
	 * V33a {@code GuiBookSection.PageType}: each page kind has its own frame art, and the casting
	 * view swaps it per subpage. Only the types the port can currently reach are listed.
	 */
	private static Identifier page(String type) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"textures/gui/lexicon/handbook" + (type.isEmpty() ? "" : "_" + type) + ".png");
	}

	private static final Identifier PAGE_PLAIN = page("");
	private static final Identifier PAGE_CAST = page("cast");
	private static final Identifier PAGE_RUNES = page("runes");
	private static final Identifier PAGE_MULTICAST = page("multicast");
	private static final Identifier PAGE_PYLONCAST = page("pyloncast2");
	private static final Identifier PAGE_STRUCTURE = page("structure");

	/** V33a RuneShapeRenderer draws its floor from the pylon structure block and the table's top. */
	private static final Identifier FLOOR_TILE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/pylon/block_0.png");
	private static final Identifier TABLE_TILE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/tile/table_top.png");

	/** V33a GuiNavigation.getScrollingTexture: the pannable backdrop beneath the frame. */
	private static final Identifier NAV_SCROLL = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lexicon/navbcg.png");
	private static final Identifier HANDBOOK = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lexicon/handbook.png");
	private static final int WIDTH = 256;
	private static final int HEIGHT = 220;
	private static final int PAGE_SIZE = 8;
	/** V33a navigation row pitch, still used by the stored-fragment list. */
	private static final int ENTRY_HEIGHT = 23;
	/** V33a GuiScrollingPage draws the pane at (left + 7, top - 1). */
	/**
	 * V33a draws the scrolling backdrop at {@code leftX+7, topY-1} but lays the sections out from
	 * {@code leftX+11, topY+11} -- two different origins. The port used the backdrop's for both, which
	 * put every section header eight pixels above the frame instead of inside it.
	 */
	private static final int PANE_X = 11;
	private static final int PANE_Y = 11;
	private static LexiconCatalog.Section rememberedSection = LexiconCatalog.Section.INFO;
	private static int rememberedOffset;

	private final Player player;
	private final ItemStack book;
	private final boolean fragmentInventory;
	private LexiconCatalog.Section section = rememberedSection;
	private LexiconCatalog.Entry selected;
	private final LexiconScrollPane scrollPane = new LexiconScrollPane();
	private final LexiconNavigationSheet sheet = new LexiconNavigationSheet();
	private long guiTick;
	private int pageOffset = rememberedOffset;
	private View view;
	private boolean recipeMode;
	private boolean searching;
	private String search = "";
	private int textPage;
	/** Entries whose V33a structure extends {@code FragmentStructureBase}; see the N# button. */
	private static final java.util.Set<String> FRAGMENT_STRUCTURES = java.util.Set.of();

	/** V33a GuiStructure {@code mode}: 0 is the 3D view, 1 the flat slice, 2 the block tally. */
	private int structureMode;
	private StructureRenderer structureRender;
	private String structureRenderFor;
	private final LexiconMachineRender machineRender = new LexiconMachineRender();
	private final ArrayList<String> noteData;
	private final ArrayList<EditBox> noteFields = new ArrayList<>();
	private int noteScroll;
	private boolean notesDirty;
	private LexiconProgressGraph.Mode progressView = LexiconProgressGraph.Mode.TREE;
	private final LexiconProgressGraph progressGraph = new LexiconProgressGraph();
	private final LexiconScrollPane progressPane = new LexiconScrollPane();
	private boolean castingRecipeView;
	private int recipeIndex;
	private int recipeSubpage;
	private final java.util.Map<String, List<CastingTableRecipe>> castingRecipeCache = new java.util.HashMap<>();
	private final java.util.Set<String> requestedCastingRecipes = new java.util.HashSet<>();

	private enum View {
		NAVIGATION,
		PROGRESS,
		RECOVERY,
		NOTES,
		STORED_PAGES
	}


	public ScreenChromicLexicon(Player player, ItemStack book, boolean fragmentInventory) {
		super(Component.literal("Chromic Lexicon"));
		this.player = player;
		this.book = book;
		this.fragmentInventory = fragmentInventory;
		noteData = new ArrayList<>(LexiconData.read(book).notes());
		view = fragmentInventory ? View.STORED_PAGES : View.NAVIGATION;
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		if (selected == null && (view == View.NAVIGATION || view == View.STORED_PAGES)) {
			// V33a has no section or entry buttons: both live on the scrolling sheet, which is
			// clicked directly. The only navigation widgets are the side tabs added below.
			if (view == View.STORED_PAGES)
				addStoredPageButtons(left, top);
		}
		else if (selected != null) {
			// V33a GuiBookSection button 50: back to the navigation sheet.
			addRenderableWidget(Button.builder(Component.literal("X"), button -> {
				selected = null;
				rebuildWidgets();
			}).bounds(left + WIDTH - 27, top - 2, 20, 20).build());
			List<CastingTableRecipe> recipes = castingRecipes();
			if (recipes.isEmpty() && !craftingRecipes().isEmpty()) {
				addRenderableWidget(Button.builder(
						Component.literal(castingRecipeView ? "Description" : "Crafting Recipe"), button -> {
					castingRecipeView = !castingRecipeView;
					recipeIndex = 0;
					rebuildWidgets();
				}).bounds(left + 8, top + 194, 92, 18).build());
				if (castingRecipeView && craftingRecipes().size() > 1) {
					addRenderableWidget(Button.builder(Component.literal("‹"), button -> {
						recipeIndex--;
						rebuildWidgets();
					}).bounds(left + 104, top + 194, 18, 18).build());
					addRenderableWidget(Button.builder(Component.literal("›"), button -> {
						recipeIndex++;
						rebuildWidgets();
					}).bounds(left + 124, top + 194, 18, 18).build());
				}
			}
			if (!recipes.isEmpty()) {
				addRenderableWidget(Button.builder(Component.literal(castingRecipeView ? "Description" : "Casting Recipe"), button -> {
					castingRecipeView = !castingRecipeView;
					recipeSubpage = 0;
					rebuildWidgets();
				}).bounds(left + 8, top + 194, 92, 18).build());
				if (castingRecipeView)
					addCastingRecipeButtons(left, top, recipes);
			}
			if (view == View.STORED_PAGES && !LexiconData.read(book).creative())
				addRenderableWidget(Button.builder(Component.literal("Eject Fragment"), button -> {
					ClientPacketDistributor.sendToServer(new ChromaNetwork.TransferLexiconPage(selected.id(), false));
					selected = null;
					rebuildWidgets();
				}).bounds(left + 76, top + 194, 104, 18).build());
			if (!castingRecipeView)
				addTextPageButtons(left, top);
			if (selected.section() == LexiconCatalog.Section.STRUCTURES && !castingRecipeView)
				addStructureViewButtons(left, top);
		}
		else if (view == View.RECOVERY) {
			addRecoveryButtons(left, top);
		}
		else if (view == View.PROGRESS) {
			addProgressWidgets(left, top);
		}
		else if (view == View.NOTES) {
			addNotebookWidgets(left, top);
		}
		if (!fragmentInventory) {
			// V33a GuiNavigation.initGui, verbatim geometry. The Items/Recipes tabs are 13x88 strips
			// down the left edge whose v swaps to show which mode is active, and Search is a 13x35
			// stub below them. Nothing here is a vanilla widget upstream.
			// The two tabs are 13x88 and overlap by 54 pixels. 26.2 hit-tests children in order and
			// the first hit wins, so the add order decides which tab owns the shared band -- upstream
			// adds the INACTIVE one first for the same reason. v=4 is the raised, active look and
			// v=95 the recessed one; the port had both inverted, so the tab you were on drew and
			// clicked as though it were the other.
			Runnable toItems = () -> {
				recipeMode = false;
				rebuildWidgets();
			};
			Runnable toRecipes = () -> {
				recipeMode = true;
				rebuildWidgets();
			};
			if (recipeMode) {
				addRenderableWidget(new LexiconImageButton(left - 13, top + 27, 13, 88,
						15, 4, Component.literal("Recipes"), toRecipes));
				addRenderableWidget(new LexiconImageButton(left - 13, top - 7, 13, 88,
						15, 95, Component.literal("Items"), toItems));
			}
			else {
				addRenderableWidget(new LexiconImageButton(left - 13, top + 27, 13, 88,
						15, 95, Component.literal("Recipes"), toRecipes));
				addRenderableWidget(new LexiconImageButton(left - 13, top - 7, 13, 88,
						15, 4, Component.literal("Items"), toItems));
				// V33a only offers Search out of recipe mode.
				addRenderableWidget(new LexiconImageButton(left - 13, top + 160, 13, 35,
						15, 221, Component.literal("Search"), () -> {
					searching = !searching;
					if (searching) setFocused(null);
					rebuildWidgets();
				}));
			}
			// V33a: three 22x39 image buttons stacked down the right edge at k, k+40, k+80.
			addRenderableWidget(new LexiconImageButton(left + WIDTH, top, 22, 39,
					42, 84, Component.literal("Progress"), () -> setView(View.PROGRESS)));
			addRenderableWidget(new LexiconImageButton(left + WIDTH, top + 40, 22, 39,
					65, 168, Component.literal("Recovery"), () -> setView(View.RECOVERY)));
			addRenderableWidget(new LexiconImageButton(left + WIDTH, top + 80, 22, 39,
					88, 168, Component.literal("Notebook"), () -> setView(View.NOTES)));
			// V33a GuiBookSection: Save & Exit. It goes BELOW the other three rather than five pixels
			// down from the top -- the port's offset put it on top of Progress and Recovery, and since
			// it was added last it could never be clicked at all.
			if (view != View.NAVIGATION)
				addRenderableWidget(new LexiconImageButton(left + WIDTH, top + 120, 22, 39,
						42, 210, Component.literal("Save & Exit"), this::onClose));
		}
		if (selected == null && view == View.NAVIGATION)
			addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
					.bounds(left + WIDTH - 27, top - 2, 20, 20).build());
	}


	private void addTextPageButtons(int left, int top) {
		int count = descriptionPageCount();
		if (textPage > 0)
			addRenderableWidget(Button.builder(Component.literal("‹ Page"), button -> {
				textPage--;
				rebuildWidgets();
			}).bounds(left + 8, top + 194, 52, 18).build());
		if (textPage + 1 < count)
			addRenderableWidget(Button.builder(Component.literal("Page ›"), button -> {
				textPage++;
				rebuildWidgets();
			}).bounds(left + 196, top + 194, 52, 18).build());
	}

	/**
	 * V33a {@code getGuiLayout()}. The casting view is the interesting one: upstream returns a
	 * different PageType per subpage, so the frame changes as you page through Grid, Runes, Stands
	 * and Aura rather than staying on one background.
	 */
	private Identifier pageBackground() {
		if (selected == null)
			return PAGE_PLAIN;
		if (castingRecipeView && !castingRecipes().isEmpty())
			return switch (recipeSubpage) {
				case 0 -> PAGE_CAST;
				case 1 -> PAGE_RUNES;
				case 2 -> PAGE_MULTICAST;
				case 3 -> PAGE_PYLONCAST;
				default -> PAGE_PLAIN;
			};
		// V33a PageType.CRAFTING reuses the casting frame.
		if (castingRecipeView && !craftingRecipes().isEmpty())
			return PAGE_CAST;
		if (selected.section() == LexiconCatalog.Section.STRUCTURES)
			return PAGE_STRUCTURE;
		return PAGE_PLAIN;
	}

	private void openEntry(LexiconCatalog.Entry entry) {
		selected = entry;
		textPage = 0;
		recipeIndex = 0;
		recipeSubpage = 0;
		castingRecipeView = recipeMode;
		structureMode = 0;
		structureRender = null;
		structureRenderFor = null;
		machineRender.reset();
		rebuildWidgets();
	}

	/**
	 * The structure viewer's state, kept across frames so a rotation survives a widget rebuild but is
	 * dropped when the page changes. V33a keeps one {@code StructureRenderer} per {@code GuiStructure}
	 * and each page is its own screen, so this is where the equivalent lifetime lands.
	 */
	private StructureRenderer structureRenderer() {
		LexiconStructurePreview preview = LexiconStructurePreview.get(selected, minecraft);
		if (!preview.available())
			return null;
		if (structureRender != null && selected.sourceId().equals(structureRenderFor))
			return structureRender;
		List<StructureRenderer.Entry> entries = new ArrayList<>(preview.blocks().size());
		for (LexiconStructurePreview.PreviewBlock block : preview.blocks())
			entries.add(new StructureRenderer.Entry(block.pos(), block.state(), block.icon(), block.shared()));
		structureRender = new StructureRenderer(entries, preview.sizeX(), preview.sizeY(), preview.sizeZ());
		// V33a GuiStructure: a rune in the guide is not any one element. Upstream hooks the rune block
		// to getElementByTick(), which walks the sixteen elements on a four-second cycle, so the page
		// shows that any rune will do rather than implying the black one specifically. The templates
		// are all generated with the black rune as the placeholder, so every one of the sixteen has to
		// be hooked, not just that one.
		for (CrystalElement element : CrystalElement.elements)
			structureRender.addBlockHook(ChromaBlocks.rune(element).get(),
					() -> ChromaBlocks.rune(elementByTick()).get().defaultBlockState());
		structureRenderFor = selected.sourceId();
		return structureRender;
	}

	/** V33a {@code GuiStructure.getElementByTick}. */
	private static CrystalElement elementByTick() {
		return CrystalElement.elements[(int)(System.currentTimeMillis() / 4000 % 16)];
	}

	/**
	 * V33a {@code GuiStructure.initGui}, verbatim geometry: two 20x20 mode buttons at the top right,
	 * a block-tally button beside them, and the slice stepper only while the slice view is up.
	 */
	private void addStructureViewButtons(int left, int top) {
		StructureRenderer render = structureRenderer();
		if (render == null)
			return;
		addRenderableWidget(Button.builder(Component.literal("3D"), button -> {
			structureMode = 0;
			render.reset();
			rebuildWidgets();
		}).bounds(left + 185, top - 2, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("2D"), button -> {
			structureMode = 1;
			rebuildWidgets();
		}).bounds(left + 205, top - 2, 20, 20).build());
		// V33a suppresses the tally for a FragmentStructureBase -- those pages describe a fragment of
		// a structure, so a bill of materials for them would be misleading. No fragment structure has
		// a modern template yet, so nothing is currently suppressed; the condition belongs here the
		// moment one is added rather than being rediscovered then.
		if (!FRAGMENT_STRUCTURES.contains(selected.sourceId()))
			addRenderableWidget(Button.builder(Component.literal("N#"), button -> {
				structureMode = 2;
				rebuildWidgets();
			}).bounds(left + (structureMode == 1 ? 125 : 165), top - 2, 20, 20).build());
		if (structureMode == 1) {
			addRenderableWidget(Button.builder(Component.literal("+"), button -> {
				render.incrementStepY();
				rebuildWidgets();
			}).bounds(left + 165, top - 2, 20, 20).build());
			addRenderableWidget(Button.builder(Component.literal("-"), button -> {
				render.decrementStepY();
				rebuildWidgets();
			}).bounds(left + 145, top - 2, 20, 20).build());
		}
	}

	private void addRecoveryButtons(int left, int top) {
		List<LexiconCatalog.Entry> pages = recoverablePages();
		int end = Math.min(pageOffset + PAGE_SIZE, pages.size());
		for (int index = pageOffset; index < end; index++) {
			LexiconCatalog.Entry entry = pages.get(index);
			int row = index - pageOffset;
			addRenderableWidget(Button.builder(entry.title(), button -> {
				ClientPacketDistributor.sendToServer(new ChromaNetwork.RecoverResearchPage(entry.id()));
				rebuildWidgets();
			}).bounds(left + 26, top + 56 + row * 18, 204, 16).build());
		}
		if (pageOffset > 0)
			addRenderableWidget(Button.builder(Component.literal("↑"), button -> {
				pageOffset = Math.max(0, pageOffset - PAGE_SIZE);
				rebuildWidgets();
			}).bounds(left + 26, top + 204, 28, 14).build());
		if (end < pages.size())
			addRenderableWidget(Button.builder(Component.literal("↓"), button -> {
				pageOffset += PAGE_SIZE;
				rebuildWidgets();
			}).bounds(left + 202, top + 204, 28, 14).build());
	}

	private void addStoredPageButtons(int left, int top) {
		List<LexiconCatalog.Entry> pages = transferablePages();
		int end = Math.min(pageOffset + PAGE_SIZE, pages.size());
		for (int index = pageOffset; index < end; index++) {
			LexiconCatalog.Entry entry = pages.get(index);
			boolean stored = LexiconData.read(book).pages().contains(entry.id());
			Component label = Component.literal(stored ? "Book: " : "Insert: ").append(entry.title());
			int row = index - pageOffset;
			addRenderableWidget(Button.builder(label, button -> {
				if (stored) {
					if (isEntryActive(entry))
						openEntry(entry);
					else
						playLockedEntrySound();
				}
				else {
					ClientPacketDistributor.sendToServer(new ChromaNetwork.TransferLexiconPage(entry.id(), true));
					rebuildWidgets();
				}
			}).bounds(left + 136, top + 16 + row * 23, 108, 18).build());
		}
		if (pageOffset > 0)
			addRenderableWidget(Button.builder(Component.literal("↑"), button -> {
				pageOffset = Math.max(0, pageOffset - PAGE_SIZE);
				rebuildWidgets();
			}).bounds(left + 112, top + 202, 28, 16).build());
		if (end < pages.size())
			addRenderableWidget(Button.builder(Component.literal("↓"), button -> {
				pageOffset += PAGE_SIZE;
				rebuildWidgets();
			}).bounds(left + 216, top + 202, 28, 16).build());
	}

	private void setView(View next) {
		if (view == View.NOTES && next != View.NOTES)
			saveNotes();
		view = next;
		selected = null;
		pageOffset = 0;
		rebuildWidgets();
	}

	private void addNotebookWidgets(int left, int top) {
		noteFields.clear();
		for (int row = 0; row < 10; row++) {
			int index = noteScroll + row;
			EditBox field = new EditBox(font, left + 10, top + 30 + row * 16, 236, 15,
					Component.literal("Lexicon note " + (index + 1)));
			field.setMaxLength(1024);
			field.setBordered(false);
			field.setTextColor(0xff202020);
			if (index < noteData.size())
				field.setValue(noteData.get(index));
			final int target = index;
			field.setResponder(value -> {
				while (noteData.size() <= target)
					noteData.add("");
				noteData.set(target, value);
				notesDirty = true;
			});
			noteFields.add(addRenderableWidget(field));
		}
		addRenderableWidget(Button.builder(Component.literal("↑"), button -> scrollNotes(-1))
				.bounds(left - 20, top + 18, 18, 18).build());
		addRenderableWidget(Button.builder(Component.literal("↓"), button -> scrollNotes(1))
				.bounds(left - 20, top + 38, 18, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+"), button -> appendNote())
				.bounds(left - 20, top + 68, 18, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Clear"), button -> clearNotes())
				.bounds(left - 42, top + 89, 40, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveNotes())
				.bounds(left + 102, top + 194, 52, 18).build());
	}

	/**
	 * V33a {@code GuiProgressStages.initGui}: a Return tab on the right and two 13x35 mode tabs down
	 * the left. There is no row of text buttons and no third mode -- upstream has only the tree and
	 * the by-level ordering.
	 */
	private void addProgressWidgets(int left, int top) {
		addRenderableWidget(new LexiconImageButton(left + WIDTH, top, 22, 39,
				42, 126, Component.literal("Return"), () -> setView(View.NAVIGATION)));
		addRenderableWidget(new LexiconImageButton(left - 13, top - 7, 13, 35,
				1, 185, Component.literal("Tree"), () -> setProgressView(LexiconProgressGraph.Mode.TREE)));
		addRenderableWidget(new LexiconImageButton(left - 13, top + 27, 13, 35,
				1, 221, Component.literal("Levels"), () -> setProgressView(LexiconProgressGraph.Mode.BY_LEVEL)));
	}

	private void setProgressView(LexiconProgressGraph.Mode mode) {
		if (progressView == mode)
			return;
		progressView = mode;
		progressPane.reset();
		rebuildWidgets();
	}


	private void scrollNotes(int direction) {
		int max = Math.max(0, noteData.size() - 10);
		int next = Math.max(0, Math.min(noteScroll + direction, max));
		if (next != noteScroll) {
			noteScroll = next;
			rebuildWidgets();
		}
	}

	private void appendNote() {
		noteData.add("");
		noteScroll = Math.max(0, noteData.size() - 10);
		notesDirty = true;
		rebuildWidgets();
		if (!noteFields.isEmpty())
			setFocused(noteFields.getLast());
	}

	private void clearNotes() {
		noteData.clear();
		noteScroll = 0;
		notesDirty = true;
		rebuildWidgets();
	}

	private void saveNotes() {
		if (!notesDirty)
			return;
		ClientPacketDistributor.sendToServer(new ChromaNetwork.UpdateLexiconNotes(List.copyOf(noteData)));
		notesDirty = false;
	}

	private void rememberNavigation() {
		if (view == View.NAVIGATION) {
			rememberedSection = section;
			rememberedOffset = pageOffset;
		}
	}

	private void addCastingRecipeButtons(int left, int top, List<CastingTableRecipe> recipes) {
		CastingTableRecipe recipe = recipes.get(Math.min(recipeIndex, recipes.size() - 1));
		if (recipes.size() > 1) {
			addRenderableWidget(Button.builder(Component.literal("‹"), button -> {
				recipeIndex = Math.floorMod(recipeIndex - 1, recipes.size());
				recipeSubpage = 0;
				rebuildWidgets();
			}).bounds(left + 202, top + 6, 18, 18).build());
			addRenderableWidget(Button.builder(Component.literal("›"), button -> {
				recipeIndex = (recipeIndex + 1) % recipes.size();
				recipeSubpage = 0;
				rebuildWidgets();
			}).bounds(left + 222, top + 6, 18, 18).build());
		}
		String[] labels = {"Grid", "Runes", "Stands", "Aura"};
		int max = recipe.tier().ordinal();
		for (int i = 0; i <= max; i++) {
			int subpage = i;
			addRenderableWidget(Button.builder(Component.literal(labels[i]), button -> {
				recipeSubpage = subpage;
				rebuildWidgets();
			}).bounds(left + 106 + i * 35, top + 194, 34, 18).build());
		}
	}

	/**
	 * V33a GuiCraftingRecipe's recipe set. 26.2 no longer exposes {@code IRecipe} to the client at
	 * all: recipes arrive as {@code RecipeDisplayEntry} records in the player's recipe book, so the
	 * page is built from the same displays vanilla's own recipe book renders rather than from a
	 * re-derived recipe list.
	 */
	private List<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> craftingRecipes() {
		if (selected == null || minecraft == null || minecraft.level == null || minecraft.player == null)
			return List.of();
		ItemStack icon = LexiconIconResolver.icon(selected);
		if (icon.isEmpty())
			return List.of();
		net.minecraft.util.context.ContextMap context =
				net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);
		List<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> out = new java.util.ArrayList<>();
		for (net.minecraft.client.gui.screens.recipebook.RecipeCollection collection
				: minecraft.player.getRecipeBook().getCollections()) {
			for (net.minecraft.world.item.crafting.display.RecipeDisplayEntry entry : collection.getRecipes()) {
				boolean crafting = entry.display() instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
						|| entry.display() instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
				if (!crafting)
					continue;
				for (ItemStack result : entry.resultItems(context))
					if (result.is(icon.getItem())) {
						out.add(entry);
						break;
					}
			}
		}
		return out;
	}

	private List<CastingTableRecipe> castingRecipes() {
		if (selected == null || player.level() == null)
			return List.of();
		ItemStack icon = LexiconIconResolver.icon(selected);
		if (icon.isEmpty())
			return List.of();
		String itemId = BuiltInRegistries.ITEM.getKey(icon.getItem()).toString();
		List<CastingTableRecipe> cached = castingRecipeCache.get(itemId);
		if (cached != null)
			return cached;
		if (requestedCastingRecipes.add(itemId))
			ClientPacketDistributor.sendToServer(new ChromaNetwork.RequestGuideCastingRecipes(itemId));
		return List.of();
	}

	/** Called only by the client payload handler after the server filters its authoritative recipes. */
	public void acceptCastingRecipes(String itemId, List<CastingTableRecipe> recipes) {
		castingRecipeCache.put(itemId, List.copyOf(recipes));
		recipeIndex = 0;
		recipeSubpage = 0;
		if (recipeMode && !recipes.isEmpty())
			castingRecipeView = true;
		rebuildWidgets();
	}

	private List<LexiconCatalog.Entry> visiblePages() {
		ArrayList<LexiconCatalog.Entry> pages = new ArrayList<>();
		List<LexiconCatalog.Entry> candidates = search.isBlank()
				? LexiconCatalog.entries(section) : LexiconCatalog.pages();
		for (LexiconCatalog.Entry entry : candidates) {
			if (entry.parent())
				continue;
			if (ItemChromaBook.hasPage(book, entry) && matchesSearch(entry))
				pages.add(entry);
		}
		return pages;
	}

	private boolean matchesSearch(LexiconCatalog.Entry entry) {
		return search.isBlank() || entry.title().getString().toLowerCase(Locale.ROOT)
				.contains(search.toLowerCase(Locale.ROOT));
	}

	private boolean isEntryActive(LexiconCatalog.Entry entry) {
		return LexiconData.read(book).creative() || entry.readableWithoutFragment()
				|| PlayerResearch.hasFragment(player, entry);
	}

	private void playLockedEntrySound() {
		if (minecraft.level == null)
			return;
		minecraft.level.playLocalSound(player, ChromaSounds.ERROR.getSoundEvent(),
				ChromaSounds.ERROR.getCategory(), 0.35F, 0.8F);
		minecraft.level.playLocalSound(player, ChromaSounds.ERROR.getSoundEvent(),
				ChromaSounds.ERROR.getCategory(), 0.35F, 1.2F);
	}

	private List<LexiconCatalog.Entry> storedPages() {
		return LexiconData.read(book).pages().stream().map(LexiconCatalog::byId)
				.filter(java.util.Objects::nonNull).toList();
	}

	private List<LexiconCatalog.Entry> transferablePages() {
		ArrayList<LexiconCatalog.Entry> pages = new ArrayList<>(storedPages());
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!stack.is(reika.chromaticraft.registry.ChromaItems.INFO_FRAGMENT.get()))
				continue;
			LexiconCatalog.Entry page = reika.chromaticraft.magic.progression.ResearchFragmentData.read(stack).page();
			if (page != null && !pages.contains(page))
				pages.add(page);
		}
		pages.sort(java.util.Comparator.comparingInt(LexiconCatalog.Entry::ordinal));
		return List.copyOf(pages);
	}

	private List<LexiconCatalog.Entry> recoverablePages() {
		return PlayerResearch.fragments(player).stream().map(LexiconCatalog::byId)
				.filter(java.util.Objects::nonNull).filter(page -> !ItemChromaBook.hasPage(book, page)).toList();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		guiTick++;
		boolean scrolling = selected == null && (view == View.NAVIGATION || view == View.STORED_PAGES);
		if (scrolling) {
			// V33a GuiScrollingPage: poll the held movement keys, then lay the backdrop down first so
			// the frame's cut-out window sits over it. The vertical offset also drives the entry list,
			// so holding S slides list and backdrop together rather than stepping a page at a time.
			buildSheet();
			scrollPane.setBounds(sheet.maxX(), sheet.maxY());
			scrollPane.pan();
			scrollPane.render(graphics, NAV_SCROLL, left, top);
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, selected == null ? NAVIGATION : pageBackground(),
				left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
		if (selected == null && (view == View.NAVIGATION || view == View.STORED_PAGES)) {
			// V33a GuiNavigation draws NO title and NO section caption here: the frame art carries the
			// book's identity and each section labels itself inside the sheet. The two captions that
			// used to be here overlapped each other on screen.
			sheet.render(graphics, font, left + PANE_X, top + PANE_Y,
					scrollPane.offsetX(), scrollPane.offsetY(),
					LexiconScrollPane.PANE_WIDTH, LexiconScrollPane.PANE_HEIGHT,
					mouseX, mouseY, search, guiTick,
					LexiconIconResolver::icon, this::isEntryActive);
			LexiconCatalog.Entry hovered = sheetHit(mouseX, mouseY);
			if (hovered != null)
				graphics.text(font, hovered.title(), mouseX + 8, mouseY - 10,
						isEntryActive(hovered) ? 0xffffffff : 0xffff8080, true);
			if (!search.isBlank())
				graphics.text(font, Component.literal((searching ? "> " : "Search: ") + search),
						left - 58, top + 184, searching ? 0xff80dfff : 0xffb0b0b0, false);
		}
		else if (selected != null) {
			if (castingRecipeView && !castingRecipes().isEmpty()) {
				renderCastingRecipe(graphics, left, top);
				super.extractRenderState(graphics, mouseX, mouseY, partialTick);
				return;
			}
			// V33a shows an entry's ordinary grid recipe on its own page. When an entry has no
			// casting recipe but does have a crafting one, Recipes mode lands here instead.
			if (castingRecipeView && !craftingRecipes().isEmpty()) {
				graphics.centeredText(font, selected.title(), left + WIDTH / 2, top + 4, 0xffffffff);
				renderCraftingRecipe(graphics, left, top);
				super.extractRenderState(graphics, mouseX, mouseY, partialTick);
				return;
			}
			// V33a GuiBookSection.drawScreen: the page title is left-aligned at
			// (posX + getTitleOffset(), posY + 6) with posY already shifted up eight, i.e.
			// (left + 6, top - 2), in white. It was centred at top+18 here, with an invented section
			// subtitle under it -- neither is upstream, and on the structure page the subtitle sat
			// where GuiStructure puts its size caption.
			graphics.text(font, selected.title(), left + 6, top - 2, 0xffffffff, false);
			if (selected.section() == LexiconCatalog.Section.STRUCTURES) {
				// GuiStructure.drawScreen adds the size string and nothing else.
				renderStructureViewer(graphics, left, top, mouseX, mouseY, partialTick);
				super.extractRenderState(graphics, mouseX, mouseY, partialTick);
				return;
			}
			renderSpecialistHeader(graphics, selected, left, top, partialTick);
			graphics.text(font, Component.literal("Research: " + selected.level().name()), left + 8, top + 69,
					0xffb0b0b0, false);
			renderDescriptionPage(graphics, left, top);
		}
		else if (view == View.PROGRESS) {
			renderProgress(graphics, left, top, mouseX, mouseY);
		}
		else if (view == View.RECOVERY) {
			graphics.centeredText(font, "Fragment Recovery", left + WIDTH / 2, top + 16, 0xffffffff);
			graphics.text(font, Component.literal("Recovered pages cost one paper and one black dye."),
					left + 10, top + 38, 0xffc0c0c0, false);
		}
		else if (view == View.NOTES) {
			graphics.centeredText(font, "Notebook", left + WIDTH / 2, top + 16, 0xffffffff);
			graphics.text(font, Component.literal("Lines " + (noteScroll + 1) + "–" + (noteScroll + 10)),
					left + 188, top + 5, 0xff909090, false);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	/**
	 * V33a's progress screen is a panned field of nodes, not a list. The pane scrolls with the same
	 * held movement keys the navigation sheet uses.
	 */
	private void renderProgress(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
		progressPane.setBounds(progressGraph.maxScrollX(progressView), progressGraph.maxScrollY(progressView));
		progressPane.pan();
		progressGraph.render(graphics, font, player, progressView, left, top,
				progressPane.offsetX(), progressPane.offsetY(), mouseX, mouseY);
	}


	/** The list the navigation view is currently showing. */
	private List<LexiconCatalog.Entry> currentPages() {
		return view == View.STORED_PAGES ? transferablePages() : visiblePages();
	}

	/** Rebuilds the spatial sheet for whatever the navigation view is currently showing. */
	private void buildSheet() {
		if (view == View.STORED_PAGES || !search.isBlank()) {
			// Search results and the fragment list are a flat set, so they get one synthetic section
			// rather than the per-section layout.
			sheet.build(List.of(section), s -> currentPages(), font,
					LexiconScrollPane.PANE_WIDTH, LexiconScrollPane.PANE_HEIGHT);
			return;
		}
		sheet.build(List.of(LexiconCatalog.Section.values()), this::pagesOf, font,
				LexiconScrollPane.PANE_WIDTH, LexiconScrollPane.PANE_HEIGHT);
	}

	/** The visible entries of one section, filtered exactly as the old paged list filtered them. */
	private List<LexiconCatalog.Entry> pagesOf(LexiconCatalog.Section target) {
		return LexiconCatalog.entries(target).stream()
				.filter(entry -> !entry.parent())
				.filter(entry -> ItemChromaBook.hasPage(book, entry))
				.toList();
	}


	private static int researchLevelColor(ResearchLevel level) {
		if (level == null)
			return 0xff808080;
		return switch (level) {
			case ENTRY -> 0xff6a6a6a;
			case RAWEXPLORE -> 0xff45a060;
			case BASICCRAFT -> 0xff4d80c8;
			case RUNECRAFT -> 0xff9055c8;
			case ENERGY -> 0xffcf4eaa;
			case MULTICRAFT -> 0xffe17e38;
			case NETWORKING -> 0xfff0d84a;
			case PYLONCRAFT -> 0xffff9d38;
			case CTM -> 0xff70e8ff;
			case ENDGAME -> 0xffffffff;
		};
	}

	private void renderDescriptionPage(GuiGraphicsExtractor graphics, int left, int top) {
		List<net.minecraft.util.FormattedCharSequence> lines = descriptionLines();
		int linesPerPage = 10;
		int first = Math.min(textPage * linesPerPage, Math.max(0, lines.size() - 1));
		int end = Math.min(first + linesPerPage, lines.size());
		int y = top + 94;
		for (int i = first; i < end; i++) {
			graphics.text(font, lines.get(i), left + 8, y, 0xffffffff, false);
			y += 10;
		}
		if (descriptionPageCount() > 1)
			graphics.centeredText(font, Component.literal((textPage + 1) + " / " + descriptionPageCount()),
					left + WIDTH / 2, top + 204, 0xff909090);
	}

	private List<net.minecraft.util.FormattedCharSequence> descriptionLines() {
		ArrayList<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>(font.split(
				Component.literal(LexiconDescriptions.description(selected)), 238));
		String notes = LexiconDescriptions.notes(selected);
		if (!notes.isBlank()) {
			lines.add(net.minecraft.util.FormattedCharSequence.EMPTY);
			lines.addAll(font.split(Component.literal(notes), 238));
		}
		return lines;
	}

	private int descriptionPageCount() {
		return selected == null ? 1 : Math.max(1, (descriptionLines().size() + 9) / 10);
	}

	/** Restores the distinct specialist presentations used by V33a's four description screens. */
	private void renderSpecialistHeader(GuiGraphicsExtractor graphics, LexiconCatalog.Entry entry,
			int left, int top, float partialTick) {
		ItemStack icon = LexiconIconResolver.icon(entry);
		switch (entry.section()) {
			case MACHINES -> {
				// V33a GuiMachineDescription draws the construct itself, slowly turning, and no caption.
				// The "Crystal network construct" / "Casting-system construct" strings that used to be
				// here were invented; upstream has nothing of the sort.
				if (!machineRender.render(graphics, icon, left, top, width, height, partialTick)
						&& !icon.isEmpty())
					graphics.item(icon, left + 120, top + 43);
			}
			case TOOLS -> {
				if (!icon.isEmpty()) graphics.item(icon, left + 120, top + 43);
				graphics.text(font, Component.literal("Tool: " + entry.sourceId()), left + 148, top + 48,
						0xff80dfff, false);
			}
			case ABILITIES -> {
				Identifier texture = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
						"textures/ability/" + entry.sourceId() + ".png");
				graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left + 103, top + 39,
						0, 0, 50, 50, 256, 256);
			}
			case STRUCTURES -> {
				if (!icon.isEmpty()) graphics.item(icon, left + 120, top + 43);
				graphics.text(font, Component.literal("NBT Structure Template"), left + 140, top + 47,
						0xff80dfff, false);
			}
			default -> {
				if (!icon.isEmpty()) graphics.item(icon, left + 120, top + 43);
			}
		}
	}

	private void renderStructureViewer(GuiGraphicsExtractor graphics, int left, int top, int mouseX,
			int mouseY, float partialTick) {
		LexiconStructurePreview preview = LexiconStructurePreview.get(selected, minecraft);
		if (!preview.available()) {
			graphics.centeredText(font, "No modern NBT preview available", left + WIDTH / 2, top + 103,
					0xffff8080);
			graphics.centeredText(font, preview.error(), left + WIDTH / 2, top + 118, 0xffa0a0a0);
			graphics.centeredText(font, "The V33a structure still needs an NBT template.",
					left + WIDTH / 2, top + 138, 0xff80dfff);
			return;
		}
		StructureRenderer render = structureRenderer();
		if (render == null)
			return;
		// V33a drawScreen: the footprint sits in the page's top-left corner, nothing else.
		graphics.text(font, Component.literal("(" + render.getSizeX() + "x" + render.getSizeY()
				+ "x" + render.getSizeZ() + ")"), left + 6, top + 10, 0xffffffff, false);
		switch (structureMode) {
			case 1 -> render.drawSlice(graphics, font, left, top, mouseX, mouseY);
			case 2 -> renderStructureTally(graphics, render, left, top, mouseX, mouseY);
			default -> {
				spinStructure(render);
				// V33a draw3D translates to the *screen* centre, not the page's, and sets no scissor:
				// a tall structure deliberately overflows the book. The viewport is the whole screen
				// for the same reason -- the page window is 242x181, and the pylon alone stands 229
				// pixels tall once tipped by the default -30 degrees, so anything page-sized clips it.
				render.draw3D(graphics, 0, 0, width, height, partialTick);
			}
		}
	}

	/**
	 * V33a {@code GuiStructure.draw3d}'s input poll. Dragging with the left button spins the model,
	 * the right button snaps it back, and A/D/W/S nudge it while held -- all read every frame rather
	 * than on a key event, which is what makes the rotation continuous.
	 *
	 * <p>The pitch increments are the opposite sign to upstream's. Upstream's numbers, transplanted
	 * literally, made the control invert on screen; flipping them reproduces upstream's behaviour.
	 * The orientation is composed differently here than in V33a, so the two are not expected to agree
	 * on signs -- but do not read a specific mechanism into this, it is not one that was verified.
	 */
	private void spinStructure(StructureRenderer render) {
		Window window = minecraft.getWindow();
		if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A))
			render.rotate(0, 0.75, 0);
		else if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D))
			render.rotate(0, -0.75, 0);
		else if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W))
			render.rotate(0.75, 0, 0);
		else if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S))
			render.rotate(-0.75, 0, 0);
	}

	/**
	 * V33a {@code drawTally}: every block the structure needs and how many, in two columns of eight.
	 */
	private void renderStructureTally(GuiGraphicsExtractor graphics, StructureRenderer render,
			int left, int top, int mouseX, int mouseY) {
		List<StructureRenderer.TallyEntry> tally = render.tally();
		ItemStack hovered = ItemStack.EMPTY;
		for (int i = 0; i < tally.size(); i++) {
			StructureRenderer.TallyEntry entry = tally.get(i);
			int x = left + 10 + i / 8 * 50;
			int y = top + 30 + i % 8 * 22;
			graphics.item(entry.icon(), x, y);
			graphics.text(font, Component.literal("x" + entry.count()), x + 19, y + 5, 0xffffffff, true);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
				hovered = entry.icon();
		}
		if (!hovered.isEmpty())
			graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (searching) {
			if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				pageOffset = 0;
				rebuildWidgets();
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
				searching = false;
				rebuildWidgets();
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				searching = false;
				rebuildWidgets();
				return true;
			}
		}
		int key = event.key();
		// The progress graph pans with the held movement keys through LexiconScrollPane, the same way
		// the navigation sheet does, so W/A/S/D are deliberately not consumed as discrete steps here.
		if (selected != null && castingRecipeView && !castingRecipes().isEmpty()) {
			if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) return moveRecipe(-1);
			if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) return moveRecipe(1);
			if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) return moveRecipeSubpage(-1);
			if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) return moveRecipeSubpage(1);
		}
		if (selected != null && selected.section() == LexiconCatalog.Section.STRUCTURES && !castingRecipeView) {
			// W/A/S/D are NOT consumed here in the 3D view: V33a polls them every frame so the model
			// spins for as long as the key is held (see spinStructure). Consuming them as discrete
			// events would turn that back into a per-press step.
			StructureRenderer render = structureRenderer();
			if (render != null && structureMode == 1) {
				if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
					render.incrementStepY();
					return true;
				}
				if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) {
					render.decrementStepY();
					return true;
				}
			}
			return super.keyPressed(event);
		}
		if (selected != null) {
			if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) return moveEntry(-1);
			if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) return moveEntry(1);
			if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) return moveTextPage(-1);
			if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) return moveTextPage(1);
		}
		else if (view == View.NAVIGATION || view == View.STORED_PAGES) {
			// W/A/S/D and the arrows are NOT handled here: V33a pans the navigation sheet with the
			// held movement keys every frame (see LexiconScrollPane), and consuming them as discrete
			// steps here would fight that. Sections move on the tab buttons, pages on the wheel.
			if (key == GLFW.GLFW_KEY_PAGE_UP) return moveSection(-1);
			if (key == GLFW.GLFW_KEY_PAGE_DOWN) return moveSection(1);
			if (key == GLFW.GLFW_KEY_SLASH) {
				searching = true;
				rebuildWidgets();
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (searching && event.isAllowedChatCharacter()) {
			if (search.isEmpty() && event.codepoint() == '/')
				return true; // `/` is the search shortcut, not part of the query.
			search += event.codepointAsString();
			pageOffset = 0;
			rebuildWidgets();
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY == 0)
			return super.mouseScrolled(x, y, scrollX, scrollY);
		int direction = scrollY > 0 ? -1 : 1;
		// V33a's 3D view has no zoom -- the size tier is fixed by the structure. Only the slice view
		// responds to the wheel, and it steps the layer the +/- buttons step.
		if (selected != null && selected.section() == LexiconCatalog.Section.STRUCTURES && !castingRecipeView) {
			StructureRenderer render = structureRenderer();
			if (render != null && structureMode == 1) {
				if (direction < 0)
					render.incrementStepY();
				else
					render.decrementStepY();
			}
			return true;
		}
		if (selected == null && view == View.PROGRESS) {
			progressPane.scrollBy(0, direction * ENTRY_HEIGHT);
			return true;
		}
		if (selected != null && castingRecipeView && !castingRecipes().isEmpty())
			return moveRecipe(direction);
		if (selected != null)
			return moveTextPage(direction);
		if (view == View.NAVIGATION || view == View.STORED_PAGES) {
			// Drive the same offset the movement keys do, so wheel and WASD cannot disagree.
			scrollPane.scrollBy(0, direction * ENTRY_HEIGHT);
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		// V33a drawMachineRender: holding the left button over the model tips it, one degree per unit
		// of travel, clamped either side of level. There is no yaw drag -- that runs off a clock.
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && selected != null && !castingRecipeView
				&& selected.section() == LexiconCatalog.Section.MACHINES) {
			int left = (width - WIDTH) / 2;
			int top = (height - HEIGHT) / 2;
			if (machineRender.isOver(event.x(), event.y(), left, top)) {
				machineRender.drag(dy);
				return true;
			}
		}
		// V33a draw3d: rotate(0.25*dY, 0.25*dX, 0) while the left button is held. The pitch sign is
		// flipped for the same reason spinStructure's is -- see the note there.
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && structureMode == 0
				&& isInsideStructurePreview(event.x(), event.y())) {
			StructureRenderer render = structureRenderer();
			if (render != null) {
				render.rotate(0.25 * dy, 0.25 * dx, 0);
				return true;
			}
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && selected == null
				&& (view == View.NAVIGATION || view == View.STORED_PAGES)) {
			LexiconCatalog.Entry hit = sheetHit((int)event.x(), (int)event.y());
			if (hit != null) {
				openEntry(hit);
				return true;
			}
		}
		// V33a draw3d: the right button snaps the model back to its default orientation.
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && structureMode == 0
				&& isInsideStructurePreview(event.x(), event.y())) {
			StructureRenderer render = structureRenderer();
			if (render != null) {
				render.resetRotation();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** The sheet entry under the cursor, in the same coordinate space the sheet renders in. */
	private LexiconCatalog.Entry sheetHit(int mouseX, int mouseY) {
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		return sheet.hit(mouseX, mouseY, left + PANE_X, top + PANE_Y,
				scrollPane.offsetX(), scrollPane.offsetY(),
				LexiconScrollPane.PANE_WIDTH, LexiconScrollPane.PANE_HEIGHT);
	}

	private boolean isInsideStructurePreview(double x, double y) {
		if (selected == null || selected.section() != LexiconCatalog.Section.STRUCTURES || castingRecipeView)
			return false;
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		// The 3D view renders over the whole screen, but only the page reacts to a drag. V33a's
		// Mouse.isButtonDown(0) is unconditional, so upstream spins the model even while you are
		// pressing one of its own buttons; that is not worth reproducing.
		return x >= left + 7 && x < left + WIDTH - 7 && y >= top + 24 && y < top + 205;
	}

	private boolean moveSection(int direction) {
		LexiconCatalog.Section[] values = LexiconCatalog.Section.values();
		section = values[Math.floorMod(section.ordinal() + direction, values.length)];
		pageOffset = 0;
		rememberNavigation();
		rebuildWidgets();
		return true;
	}



	private boolean moveTextPage(int direction) {
		int next = Math.max(0, Math.min(textPage + direction, descriptionPageCount() - 1));
		if (next == textPage)
			return false;
		textPage = next;
		rebuildWidgets();
		return true;
	}

	private boolean moveRecipe(int direction) {
		List<CastingTableRecipe> recipes = castingRecipes();
		if (recipes.size() < 2)
			return false;
		recipeIndex = Math.floorMod(recipeIndex + direction, recipes.size());
		recipeSubpage = 0;
		rebuildWidgets();
		return true;
	}

	private boolean moveRecipeSubpage(int direction) {
		CastingTableRecipe recipe = castingRecipes().get(recipeIndex);
		int max = recipe.tier().ordinal();
		int next = Math.max(0, Math.min(recipeSubpage + direction, max));
		if (next == recipeSubpage)
			return false;
		recipeSubpage = next;
		rebuildWidgets();
		return true;
	}

	private boolean moveEntry(int direction) {
		List<LexiconCatalog.Entry> pages = view == View.STORED_PAGES ? transferablePages() : visiblePages();
		int at = pages.indexOf(selected);
		if (at < 0 || pages.isEmpty())
			return false;
		openEntry(pages.get(Math.floorMod(at + direction, pages.size())));
		return true;
	}

	@Override
	public void onClose() {
		saveNotes();
		rememberNavigation();
		super.onClose();
	}

	/**
	 * V33a GuiCraftingRecipe.drawAuxData/drawAuxGraphics: the grid sits at (posX+54, posY+10) on an
	 * 18-pixel pitch with the output at (posX+7, posY+5), where posX/posY are the frame origin offset
	 * by (-2, -8), and the ingredient tally is an alphabetically sorted "name: xN" list capped at ten
	 * rows.
	 *
	 * <p>A shaped display carries its own width/height, so it is placed into the 3x3 at its true
	 * shape rather than packed from index 0 -- a 2x2 recipe reads as 2x2, as it does upstream.
	 */
	private void renderCraftingRecipe(GuiGraphicsExtractor graphics, int left, int top) {
		List<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> recipes = craftingRecipes();
		if (recipes.isEmpty() || minecraft == null || minecraft.level == null)
			return;
		int index = Math.floorMod(recipeIndex, recipes.size());
		net.minecraft.world.item.crafting.display.RecipeDisplayEntry entry = recipes.get(index);
		net.minecraft.util.context.ContextMap context =
				net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);
		int posX = left - 2;
		int posY = top - 8;

		ItemStack[] grid = new ItemStack[9];
		java.util.Arrays.fill(grid, ItemStack.EMPTY);
		if (entry.display() instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
			for (int y = 0; y < shaped.height() && y < 3; y++)
				for (int x = 0; x < shaped.width() && x < 3; x++) {
					int from = y * shaped.width() + x;
					if (from < shaped.ingredients().size())
						grid[y * 3 + x] = cycle(shaped.ingredients().get(from), context);
				}
		}
		else if (entry.display() instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
			for (int i = 0; i < shapeless.ingredients().size() && i < 9; i++)
				grid[i] = cycle(shapeless.ingredients().get(i), context);
		}
		else {
			return;
		}

		for (int i = 0; i < 9; i++)
			if (!grid[i].isEmpty())
				graphics.item(grid[i], posX + 54 + (i % 3) * 18, posY + 10 + (i / 3) * 18);
		ItemStack result = cycle(entry.display().result(), context);
		if (!result.isEmpty())
			graphics.item(result, posX + 7, posY + 5);

		java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
		for (ItemStack stack : grid)
			if (!stack.isEmpty())
				counts.merge(stack.getHoverName().getString(), 1, Integer::sum);
		int row = 0;
		for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
			if (row > 9)
				break;
			graphics.text(font, Component.literal(e.getKey() + ": x" + e.getValue()),
					left + 8, top + 80 + row * (font.lineHeight + 2), 0xffffffff, false);
			row++;
		}
		if (recipes.size() > 1)
			graphics.text(font, Component.literal((index + 1) + " / " + recipes.size()),
					left + 200, top + 4, 0xffb0b0b0, false);
	}

	/**
	 * One slot's shown stack. A slot can accept several items (a tag), and vanilla's recipe book
	 * cycles them on a timer; this does the same so a tag ingredient does not read as one arbitrary
	 * item.
	 */
	private ItemStack cycle(net.minecraft.world.item.crafting.display.SlotDisplay slot,
			net.minecraft.util.context.ContextMap context) {
		List<ItemStack> options = slot.resolveForStacks(context);
		if (options.isEmpty())
			return ItemStack.EMPTY;
		return options.get((int)(guiTick / 20 % options.size()));
	}

	private void renderCastingRecipe(GuiGraphicsExtractor graphics, int left, int top) {
		List<CastingTableRecipe> recipes = castingRecipes();
		recipeIndex = Math.min(recipeIndex, recipes.size() - 1);
		CastingTableRecipe recipe = recipes.get(recipeIndex);
		ItemStack output = recipe.output();
		graphics.centeredText(font, output.getHoverName(), left + WIDTH / 2, top + 16, 0xffffffff);
		graphics.centeredText(font, Component.literal(recipe.tier().name() + " Casting  " + (recipeIndex + 1)
				+ "/" + recipes.size()), left + WIDTH / 2, top + 30, 0xff80dfff);
		graphics.item(output, left + 20, top + 48);

		switch (recipeSubpage) {
			case 0 -> renderCastingGrid(graphics, recipe, left, top);
			case 1 -> renderRuneRequirements(graphics, recipe, left, top);
			case 2 -> renderStandRequirements(graphics, recipe, left, top);
			case 3 -> renderAuraRequirements(graphics, recipe, left, top);
			default -> recipeSubpage = 0;
		}
		graphics.text(font, Component.literal("Time: " + recipe.duration() + " ticks"), left + 12, top + 170,
				0xffb0b0b0, false);
		graphics.text(font, Component.literal("Experience: " + recipe.experience()), left + 12, top + 181,
				0xffb0b0b0, false);
	}

	/**
	 * V33a {@code ChromaBookData.drawCastingRecipe}, subpage 0: the 3x3 sits at (posX+54, posY+10) on
	 * an 18-pixel pitch and the output at (posX+7, posY+5), against the plain frame origin -- the -8
	 * shift upstream applies only to the page title and the text list, not to the grid. The frame art
	 * already draws the slots and the arrow, so no labels are drawn over it.
	 */
	private void renderCastingGrid(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		for (CastingTableRecipe.GridIngredient ingredient : recipe.grid()) {
			int slot = ingredient.slot();
			ItemStack stack = ingredientStack(ingredient.ingredient());
			if (!stack.isEmpty())
				graphics.item(stack, left + 54 + slot % 3 * 18, top + 10 + slot / 3 * 18);
		}
		graphics.item(recipe.output(), left + 7, top + 5);
	}

	/**
	 * V33a {@code RuneShapeRenderer}, drawn from {@code drawCastingRecipe} at (posX+128, posY+110).
	 *
	 * <p>It is a top-down map, not a list: an 11x11 floor of 16-pixel crystalline-stone tiles centred
	 * on the casting table, with the recipe's runes laid on the tiles at their real offsets. Only one
	 * Y layer is shown at a time and it cycles every five seconds, which is how a multi-layer rune
	 * pattern reads in the book. The "y=" label sits at (midx+93, midy-4).
	 */
	private void renderRuneRequirements(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		int midX = left + 128;
		int midY = top + 110;
		final int w = 16;
		int dx = midX - w / 2;
		int dy = midY - w / 2;

		// V33a animates through the pattern's own Y range, one layer per five seconds.
		int minY = 0;
		int maxY = 0;
		for (CastingTableRecipe.RuneRequirement rune : recipe.runes()) {
			minY = Math.min(minY, rune.offset().getY());
			maxY = Math.max(maxY, rune.offset().getY());
		}
		int span = Math.max(1, maxY - minY + 1);
		int layer = recipe.runes().isEmpty() ? 0
				: minY + (int)((System.currentTimeMillis() / 5000) % span);

		for (int x = -5; x <= 5; x++)
			for (int z = -5; z <= 5; z++)
				graphics.blit(RenderPipelines.GUI_TEXTURED,
						x == 0 && z == 0 ? TABLE_TILE : FLOOR_TILE,
						dx + x * w, dy + z * w, 0, 0, w, w, w, w);

		for (CastingTableRecipe.RuneRequirement rune : recipe.runes()) {
			if (rune.offset().getY() != layer)
				continue;
			Identifier tex = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"textures/block/runes/real/tile4_" + rune.element().ordinal() + ".png");
			graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
					dx + rune.offset().getX() * w, dy + rune.offset().getZ() * w, 0, 0, w, w, w, w);
		}

		if (!recipe.runes().isEmpty())
			graphics.text(font, Component.literal("y=" + layer), midX + 93, midY - 4, 0xffffffff, false);
	}

	/**
	 * V33a {@code ChromaBookData.drawCastingRecipe}, subpage 2. The stands are not a list: they are
	 * drawn at their real positions around the table, so the page reads as a map of where to build
	 * them. Upstream places each at {@code posX+120 + sign(i)*tx}, {@code posY+94 + sign(k)*ty} with
	 * {@code tx = |i| == 2 ? 38 : 64} and {@code ty = |k| == 2 ? 38 : 63} -- the inner ring sits
	 * further out on screen than its block distance suggests, which is what keeps the outer ring
	 * legible. The central 3x3 repeats at (posX+102, posY+76).
	 */
	private void renderStandRequirements(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		for (CastingTableRecipe.GridIngredient ingredient : recipe.grid()) {
			int slot = ingredient.slot();
			ItemStack stack = ingredientStack(ingredient.ingredient());
			if (!stack.isEmpty())
				graphics.item(stack, left + 102 + slot % 3 * 18, top + 76 + slot / 3 * 18);
		}
		for (CastingTableRecipe.StandIngredient stand : recipe.stands()) {
			int i = stand.offset().getX();
			int k = stand.offset().getZ();
			int sx = Integer.signum(i);
			int sy = Integer.signum(k);
			int tx = Math.abs(i) == 2 ? 38 : 64;
			int ty = Math.abs(k) == 2 ? 38 : 63;
			ItemStack stack = ingredientStack(stand.ingredient());
			if (!stack.isEmpty())
				graphics.item(stack, left + 120 + sx * tx, top + 94 + sy * ty);
		}
	}

	private void renderAuraRequirements(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		graphics.text(font, Component.literal("Required Pylon Aura"), left + 62, top + 47, 0xffffffff, false);
		if (recipe.aura().isEmpty()) {
			graphics.text(font, Component.literal("No pylon aura required."), left + 62, top + 65, 0xffb0b0b0, false);
			return;
		}
		for (int i = 0; i < recipe.aura().size(); i++) {
			CastingTableRecipe.AuraRequirement aura = recipe.aura().get(i);
			CrystalElement element = aura.element();
			graphics.item(new ItemStack(ChromaBlocks.rune(element).get()), left + 62, top + 64 + i * 19);
			graphics.text(font, Component.literal(element.displayName + ": " + aura.amount() + " Lumens"),
					left + 82, top + 68 + i * 19, 0xff000000 | element.getColor(), false);
		}
	}

	@SuppressWarnings("deprecation")
	private static ItemStack ingredientStack(net.minecraft.world.item.crafting.Ingredient ingredient) {
		return ingredient.items().findFirst().map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
