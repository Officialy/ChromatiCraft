package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.lwjgl.glfw.GLFW;

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
import net.minecraft.core.registries.BuiltInRegistries;

/** First 26.2 rendering pass for V33a's navigation and basic-description guide screens. */
public final class ScreenChromicLexicon extends Screen {

	private static final Identifier NAVIGATION = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lexicon/navigation.png");
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
	private static final int PANE_X = 7;
	private static final int PANE_Y = -1;
	private static LexiconCatalog.Section rememberedSection = LexiconCatalog.Section.INFO;
	private static int rememberedOffset;

	private final Player player;
	private final ItemStack book;
	private final boolean fragmentInventory;
	private LexiconCatalog.Section section = rememberedSection;
	private LexiconCatalog.Entry selected;
	private final LexiconScrollPane scrollPane = new LexiconScrollPane();
	private final LexiconNavigationSheet sheet = new LexiconNavigationSheet();
	private int pageOffset = rememberedOffset;
	private View view;
	private boolean recipeMode;
	private boolean searching;
	private String search = "";
	private int textPage;
	private double structureYaw = 45;
	private double structurePitch = 35;
	private int structureZoom = 2;
	private StructureViewMode structureView = StructureViewMode.THREE_D;
	private int structureLayer;
	private final ArrayList<String> noteData;
	private final ArrayList<EditBox> noteFields = new ArrayList<>();
	private int noteScroll;
	private boolean notesDirty;
	private ProgressView progressView = ProgressView.TREE;
	private int progressOffset;
	private ProgressStage selectedStage;
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

	private enum ProgressView {
		TREE("Tree"),
		BY_LEVEL("Levels"),
		STAGES("Stages");

		private final String title;
		ProgressView(String title) { this.title = title; }
	}

	private enum StructureViewMode {
		THREE_D,
		TWO_D
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
			int y = top + 16;
			for (LexiconCatalog.Section value : LexiconCatalog.Section.values()) {
				addRenderableWidget(Button.builder(value.title(), button -> {
					section = value;
					pageOffset = 0;
					rememberNavigation();
					rebuildWidgets();
				}).bounds(left + 10, y, 94, 18).build());
				y += 26;
			}
			if (view == View.STORED_PAGES)
				addStoredPageButtons(left, top);
			else
				addPageButtons(left, top, visiblePages());
		}
		else if (selected != null) {
			addRenderableWidget(Button.builder(Component.literal("<"), button -> {
				selected = null;
				rebuildWidgets();
			}).bounds(left - 20, top + 8, 20, 20).build());
			List<CastingTableRecipe> recipes = castingRecipes();
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
			addRenderableWidget(Button.builder(Component.literal(recipeMode ? "Items" : "Items ✓"), button -> {
				recipeMode = false;
				rebuildWidgets();
			}).bounds(left - 58, top + 4, 56, 18).build());
			addRenderableWidget(Button.builder(Component.literal(recipeMode ? "Recipes ✓" : "Recipes"), button -> {
				recipeMode = true;
				rebuildWidgets();
			}).bounds(left - 58, top + 25, 56, 18).build());
			addRenderableWidget(Button.builder(Component.literal(searching ? "Finish" : "Search"), button -> {
				searching = !searching;
				if (searching) setFocused(null);
				rebuildWidgets();
			}).bounds(left - 58, top + 160, 56, 18).build());
			addRenderableWidget(Button.builder(Component.literal("Guide"), button -> setView(View.NAVIGATION))
					.bounds(left + WIDTH, top + 4, 58, 18).build());
			addRenderableWidget(Button.builder(Component.literal("Progress"), button -> setView(View.PROGRESS))
					.bounds(left + WIDTH, top + 25, 58, 18).build());
			addRenderableWidget(Button.builder(Component.literal("Recovery"), button -> setView(View.RECOVERY))
					.bounds(left + WIDTH, top + 46, 58, 18).build());
			addRenderableWidget(Button.builder(Component.literal("Notes"), button -> setView(View.NOTES))
					.bounds(left + WIDTH, top + 67, 58, 18).build());
		}
		addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
				.bounds(left + WIDTH - 24, top + 6, 18, 18).build());
	}

	private void addPageButtons(int left, int top, List<LexiconCatalog.Entry> pages) {
		int end = Math.min(pageOffset + PAGE_SIZE, pages.size());
		for (int index = pageOffset; index < end; index++) {
			LexiconCatalog.Entry entry = pages.get(index);
			int row = index - pageOffset;
			addRenderableWidget(Button.builder(entry.title(), button -> {
				if (isEntryActive(entry))
					openEntry(entry);
				else
					playLockedEntrySound();
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

	private void openEntry(LexiconCatalog.Entry entry) {
		selected = entry;
		textPage = 0;
		recipeIndex = 0;
		recipeSubpage = 0;
		castingRecipeView = recipeMode;
		structureYaw = 45;
		structurePitch = 35;
		structureZoom = 2;
		structureView = StructureViewMode.THREE_D;
		structureLayer = 0;
		rebuildWidgets();
	}

	private void addStructureViewButtons(int left, int top) {
		LexiconStructurePreview preview = LexiconStructurePreview.get(selected, minecraft);
		addRenderableWidget(Button.builder(Component.literal(structureView == StructureViewMode.THREE_D ? "3D ✓" : "3D"),
				button -> {
					structureView = StructureViewMode.THREE_D;
					rebuildWidgets();
				}).bounds(left + 164, top + 38, 38, 17).build());
		addRenderableWidget(Button.builder(Component.literal(structureView == StructureViewMode.TWO_D ? "2D ✓" : "2D"),
				button -> {
					structureView = StructureViewMode.TWO_D;
					structureLayer = Math.clamp(structureLayer, 0, Math.max(0, preview.sizeY() - 1));
					rebuildWidgets();
				}).bounds(left + 204, top + 38, 38, 17).build());
		if (structureView == StructureViewMode.TWO_D && preview.available()) {
			addRenderableWidget(Button.builder(Component.literal("−"), button -> {
				structureLayer = Math.max(0, structureLayer - 1);
				rebuildWidgets();
			}).bounds(left + 84, top + 38, 24, 17).build());
			addRenderableWidget(Button.builder(Component.literal("+"), button -> {
				structureLayer = Math.min(preview.sizeY() - 1, structureLayer + 1);
				rebuildWidgets();
			}).bounds(left + 110, top + 38, 24, 17).build());
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

	private void addProgressWidgets(int left, int top) {
		for (int i = 0; i < ProgressView.values().length; i++) {
			ProgressView mode = ProgressView.values()[i];
			addRenderableWidget(Button.builder(Component.literal(mode == progressView ? mode.title + " ✓" : mode.title),
					button -> {
						progressView = mode;
						progressOffset = 0;
						selectedStage = null;
						rebuildWidgets();
					}).bounds(left + 25 + i * 70, top + 31, 66, 18).build());
		}
		if (selectedStage != null) {
			addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
				selectedStage = null;
				rebuildWidgets();
			}).bounds(left + 10, top + 194, 44, 18).build());
			return;
		}
		if (progressView == ProgressView.STAGES || progressView == ProgressView.TREE) {
			List<ProgressStage> stages = progressStages();
			int pageSize = progressView == ProgressView.STAGES ? 8 : 7;
			int end = Math.min(progressOffset + pageSize, stages.size());
			for (int index = progressOffset; index < end; index++) {
				ProgressStage stage = stages.get(index);
				int row = index - progressOffset;
				addRenderableWidget(Button.builder(Component.literal(ProgressionDescriptions.title(stage)), button -> {
					selectedStage = stage;
					rebuildWidgets();
				}).bounds(left + (progressView == ProgressView.TREE ? 12 : 30),
						top + 55 + row * (progressView == ProgressView.TREE ? 19 : 17),
						progressView == ProgressView.TREE ? 98 : 196, 15).build());
			}
		}
	}

	private static List<ProgressStage> progressStages() {
		return java.util.Arrays.stream(ProgressStage.list).filter(stage -> stage.active).toList();
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
		graphics.blit(RenderPipelines.GUI_TEXTURED, selected == null ? NAVIGATION : HANDBOOK,
				left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
		if (selected == null && (view == View.NAVIGATION || view == View.STORED_PAGES)) {
			graphics.centeredText(font, view == View.STORED_PAGES ? "Stored Research Fragments" : "Chromic Lexicon",
					left + WIDTH / 2, top + 4, 0xffffffff);
			graphics.text(font, search.isBlank() ? section.title() : Component.literal("Search Results"),
					left + 116, top + 4, 0xff7fffff, false);
			sheet.render(graphics, font, left + PANE_X, top + PANE_Y,
					scrollPane.offsetX(), scrollPane.offsetY(),
					LexiconScrollPane.PANE_WIDTH, LexiconScrollPane.PANE_HEIGHT,
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
			graphics.centeredText(font, selected.title(), left + WIDTH / 2, top + 18, 0xffffffff);
			graphics.centeredText(font, selected.section().title(), left + WIDTH / 2, top + 33, 0xff80dfff);
			renderSpecialistHeader(graphics, selected, left, top);
			if (selected.section() == LexiconCatalog.Section.STRUCTURES) {
				renderStructureViewer(graphics, left, top, mouseX, mouseY);
				super.extractRenderState(graphics, mouseX, mouseY, partialTick);
				return;
			}
			graphics.text(font, Component.literal("Research: " + selected.level().name()), left + 8, top + 69,
					0xffb0b0b0, false);
			renderDescriptionPage(graphics, left, top);
		}
		else if (view == View.PROGRESS) {
			renderProgress(graphics, left, top);
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

	private void renderProgress(GuiGraphicsExtractor graphics, int left, int top) {
		graphics.centeredText(font, "Research Progress", left + WIDTH / 2, top + 16, 0xffffffff);
		if (selectedStage != null) {
			renderProgressStageDetail(graphics, left, top, selectedStage);
			return;
		}
		switch (progressView) {
			case TREE -> renderProgressTree(graphics, left, top);
			case BY_LEVEL -> renderProgressLevels(graphics, left, top);
			case STAGES -> renderProgressStages(graphics, left, top);
		}
	}

	private void renderProgressLevels(GuiGraphicsExtractor graphics, int left, int top) {
		ResearchLevel current = ResearchProgress.getLevel(player);
		for (int i = 0; i < ResearchLevel.levelList.length; i++) {
			ResearchLevel level = ResearchLevel.levelList[i];
			int column = i / 5;
			int row = i % 5;
			int x = left + 18 + column * 116;
			int y = top + 61 + row * 25;
			boolean reached = current.isAtLeast(level);
			graphics.fill(x, y, x + 8, y + 8, reached ? 0xff40d060 : 0xff803030);
			graphics.text(font, level.getDisplayName(), x + 13, y, researchLevelColor(level), false);
		}
		graphics.centeredText(font, Component.literal("Current: ").append(current.getDisplayName()),
				left + WIDTH / 2, top + 188, 0xff80dfff);
	}

	private void renderProgressStages(GuiGraphicsExtractor graphics, int left, int top) {
		List<ProgressStage> stages = progressStages();
		int end = Math.min(progressOffset + 8, stages.size());
		for (int index = progressOffset; index < end; index++) {
			ProgressStage stage = stages.get(index);
			int y = top + 59 + (index - progressOffset) * 17;
			boolean reached = stage.isPlayerAtStage(player);
			graphics.fill(left + 17, y, left + 24, y + 7, reached ? 0xff40d060 : 0xff803030);
		}
		graphics.centeredText(font, Component.literal((progressOffset + 1) + "–" + end + " / " + stages.size()),
				left + WIDTH / 2, top + 194, 0xff909090);
	}

	private void renderProgressTree(GuiGraphicsExtractor graphics, int left, int top) {
		List<ProgressStage> stages = progressStages();
		int end = Math.min(progressOffset + 7, stages.size());
		for (int index = progressOffset; index < end; index++) {
			ProgressStage stage = stages.get(index);
			int y = top + 58 + (index - progressOffset) * 19;
			boolean reached = stage.isPlayerAtStage(player);
			int color = reached ? 0xff50e070 : stage.playerHasPrerequisites(player) ? 0xffffc050 : 0xffa06060;
			graphics.fill(left + 5, y + 3, left + 10, y + 10, color);
			String prereqs = ProgressionManager.instance.getPrereqs(stage).stream()
					.map(ProgressionDescriptions::title).collect(java.util.stream.Collectors.joining(", "));
			if (!prereqs.isBlank())
				graphics.text(font, Component.literal(font.plainSubstrByWidth("← " + prereqs, 132)),
						left + 116, y + 3, 0xff909090, false);
		}
		graphics.text(font, Component.literal("Green: reached  Gold: available  Red: locked"),
				left + 10, top + 194, 0xffb0b0b0, false);
	}

	private void renderProgressStageDetail(GuiGraphicsExtractor graphics, int left, int top, ProgressStage stage) {
		boolean reached = stage.isPlayerAtStage(player);
		graphics.centeredText(font, ProgressionDescriptions.title(stage), left + WIDTH / 2, top + 58,
				reached ? 0xff50e070 : 0xffffc050);
		String authored = reached ? ProgressionDescriptions.reveal(stage) : ProgressionDescriptions.hint(stage);
		int y = top + 78;
		for (var line : font.split(Component.literal(authored), 228)) {
			if (y > top + 167) break;
			graphics.text(font, line, left + 14, y, 0xffffffff, false);
			y += 10;
		}
		String desc = ProgressionDescriptions.description(stage);
		if (!desc.isBlank()) {
			int dy = top + 174;
			for (var line : font.split(Component.literal(desc), 228)) {
				if (dy > top + 194) break;
				graphics.text(font, line, left + 14, dy, 0xff80dfff, false);
				dy += 10;
			}
		}
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

	/** The list the navigation view is currently showing. */
	private List<LexiconCatalog.Entry> currentPages() {
		return view == View.STORED_PAGES ? transferablePages() : visiblePages();
	}

	private void renderNavigationEntries(GuiGraphicsExtractor graphics, int left, int top) {
		List<LexiconCatalog.Entry> pages = currentPages();
		int end = Math.min(pageOffset + PAGE_SIZE, pages.size());
		for (int index = pageOffset; index < end; index++) {
			LexiconCatalog.Entry entry = pages.get(index);
			int y = top + 17 + (index - pageOffset) * 23;
			ItemStack icon = LexiconIconResolver.icon(entry);
			if (!icon.isEmpty())
				graphics.item(icon, left + 114, y);
			if (!isEntryActive(entry))
				graphics.text(font, Component.literal("?"), left + 121, y + 5, 0xffff6060, true);
			int color = researchLevelColor(entry.level());
			graphics.fill(left + 132, y, left + 134, y + 16, color);
			if (recipeMode)
				graphics.text(font, Component.literal("✦"), left + 118, y + 5, 0xffffd060, true);
		}
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
			int left, int top) {
		ItemStack icon = LexiconIconResolver.icon(entry);
		switch (entry.section()) {
			case MACHINES -> {
				if (!icon.isEmpty()) graphics.item(icon, left + 120, top + 43);
				String power = switch (entry.sourceId()) {
					case "pylon", "repeater", "skypeater", "compound", "pylonlink" -> "Crystal network construct";
					case "table", "stand", "focuscrystal" -> "Casting-system construct";
					default -> "ChromatiCraft construct";
				};
				graphics.text(font, Component.literal(power), left + 148, top + 48, 0xff80dfff, false);
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

	private void renderStructureViewer(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
		LexiconStructurePreview preview = LexiconStructurePreview.get(selected, minecraft);
		if (!preview.available()) {
			graphics.centeredText(font, "No modern NBT preview available", left + WIDTH / 2, top + 103,
					0xffff8080);
			graphics.centeredText(font, preview.error(), left + WIDTH / 2, top + 118, 0xffa0a0a0);
			graphics.centeredText(font, "The V33a structure still needs an NBT template.",
					left + WIDTH / 2, top + 138, 0xff80dfff);
			return;
		}
		graphics.text(font, Component.literal(preview.templateId().toString()), left + 8, top + 58,
				0xff909090, false);
		graphics.text(font, Component.literal(preview.sizeX() + " × " + preview.sizeY() + " × " + preview.sizeZ()
				+ "  " + preview.blocks().size() + " visible blocks"), left + 8, top + 69, 0xffb0b0b0, false);
		graphics.enableScissor(left + 7, top + 80, left + WIDTH - 7, top + 191);
		if (structureView == StructureViewMode.THREE_D)
			renderStructure3D(graphics, preview, left, top, mouseX, mouseY);
		else
			renderStructureLayer(graphics, preview, left, top, mouseX, mouseY);
		graphics.disableScissor();
	}

	private void renderStructure3D(GuiGraphicsExtractor graphics, LexiconStructurePreview preview,
			int left, int top, int mouseX, int mouseY) {
		double yaw = Math.toRadians(structureYaw);
		double pitch = Math.toRadians(structurePitch);
		double cosY = Math.cos(yaw);
		double sinY = Math.sin(yaw);
		double cosP = Math.cos(pitch);
		double sinP = Math.sin(pitch);
		double footprint = Math.hypot(preview.sizeX(), preview.sizeZ());
		double fitX = 210D / Math.max(1, footprint);
		double fitY = 92D / Math.max(1, preview.sizeY() * sinP + footprint * cosP);
		double unit = Math.max(2, Math.min(10, Math.min(fitX, fitY) * (0.72 + structureZoom * 0.14)));
		double centerX = left + WIDTH / 2D;
		double centerY = top + 139D;
		ArrayList<ProjectedBlock> projected = new ArrayList<>();
		for (LexiconStructurePreview.PreviewBlock block : preview.blocks()) {
			double x = block.pos().getX() - (preview.sizeX() - 1) / 2D;
			double y = block.pos().getY() - (preview.sizeY() - 1) / 2D;
			double z = block.pos().getZ() - (preview.sizeZ() - 1) / 2D;
			double rx = x * cosY - z * sinY;
			double rz = x * sinY + z * cosY;
			double screenX = centerX + rx * unit;
			double screenY = centerY + (rz * cosP - y * sinP) * unit;
			double depth = rz * sinP + y * cosP;
			projected.add(new ProjectedBlock(block, screenX, screenY, depth));
		}
		projected.sort(java.util.Comparator.comparingDouble(ProjectedBlock::depth));
		float iconSize = (float)Math.clamp(unit * 1.45, 4, 12);
		for (ProjectedBlock projectedBlock : projected)
			renderStructureIcon(graphics, projectedBlock.block(), projectedBlock.x(), projectedBlock.y(), iconSize,
					mouseX, mouseY);
		graphics.text(font, Component.literal("Drag: rotate  Right click: reset  Wheel: zoom"),
				left + 12, top + 180, 0xff80dfff, false);
	}

	private void renderStructureLayer(GuiGraphicsExtractor graphics, LexiconStructurePreview preview,
			int left, int top, int mouseX, int mouseY) {
		structureLayer = Math.clamp(structureLayer, 0, preview.sizeY() - 1);
		int rotation = Math.floorMod((int)Math.round(structureYaw / 90D), 4);
		int gridX = rotation % 2 == 0 ? preview.sizeX() : preview.sizeZ();
		int gridZ = rotation % 2 == 0 ? preview.sizeZ() : preview.sizeX();
		double cell = Math.max(3, Math.min(12, Math.min(204D / gridX, 88D / gridZ)));
		double startX = left + WIDTH / 2D - gridX * cell / 2D;
		double startY = top + 88 + (88 - gridZ * cell) / 2D;
		for (LexiconStructurePreview.PreviewBlock block : preview.blocks()) {
			if (block.pos().getY() != structureLayer) continue;
			int tx = switch (rotation) {
				case 1 -> preview.sizeZ() - 1 - block.pos().getZ();
				case 2 -> preview.sizeX() - 1 - block.pos().getX();
				case 3 -> block.pos().getZ();
				default -> block.pos().getX();
			};
			int tz = switch (rotation) {
				case 1 -> block.pos().getX();
				case 2 -> preview.sizeZ() - 1 - block.pos().getZ();
				case 3 -> preview.sizeX() - 1 - block.pos().getX();
				default -> block.pos().getZ();
			};
			double x = startX + tx * cell + cell / 2;
			double y = startY + tz * cell + cell / 2;
			renderStructureIcon(graphics, block, x, y, (float)Math.min(12, cell), mouseX, mouseY);
		}
		graphics.centeredText(font, Component.literal("Layer " + structureLayer + " / " + (preview.sizeY() - 1)
				+ "  Rotation " + rotation * 90 + "°"),
				left + WIDTH / 2, top + 180, 0xff80dfff);
	}

	private void renderStructureIcon(GuiGraphicsExtractor graphics, LexiconStructurePreview.PreviewBlock block,
			double x, double y, float size, int mouseX, int mouseY) {
		if (block.displayOverride())
			graphics.fill((int)(x - size / 2 - 1), (int)(y - size / 2 - 1),
					(int)(x + size / 2 + 1), (int)(y + size / 2 + 1), 0x9060e8ff);
		graphics.pose().pushMatrix();
		graphics.pose().translate((float)(x - size / 2), (float)(y - size / 2));
		graphics.pose().scale(size / 16F, size / 16F);
		graphics.item(block.icon(), 0, 0);
		graphics.pose().popMatrix();
		if (mouseX >= x - size / 2 && mouseX <= x + size / 2
				&& mouseY >= y - size / 2 && mouseY <= y + size / 2)
			graphics.setTooltipForNextFrame(font, block.icon(), mouseX, mouseY);
	}

	private record ProjectedBlock(LexiconStructurePreview.PreviewBlock block, double x, double y, double depth) {}

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
		if (selected == null && view == View.PROGRESS && selectedStage == null) {
			if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) return moveProgressView(-1);
			if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) return moveProgressView(1);
			if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) return moveProgressPage(-1);
			if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) return moveProgressPage(1);
		}
		if (selected != null && castingRecipeView && !castingRecipes().isEmpty()) {
			if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) return moveRecipe(-1);
			if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) return moveRecipe(1);
			if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) return moveRecipeSubpage(-1);
			if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) return moveRecipeSubpage(1);
		}
		if (selected != null && selected.section() == LexiconCatalog.Section.STRUCTURES && !castingRecipeView) {
			if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT)
				structureYaw -= structureView == StructureViewMode.THREE_D ? 15 : 90;
			else if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT)
				structureYaw += structureView == StructureViewMode.THREE_D ? 15 : 90;
			else if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
				if (structureView == StructureViewMode.THREE_D)
					structurePitch = Math.max(10, structurePitch - 10);
				else {
					LexiconStructurePreview preview = LexiconStructurePreview.get(selected, minecraft);
					if (preview.available())
						structureLayer = Math.min(preview.sizeY() - 1, structureLayer + 1);
				}
			}
			else if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) {
				if (structureView == StructureViewMode.THREE_D)
					structurePitch = Math.min(80, structurePitch + 10);
				else
					structureLayer = Math.max(0, structureLayer - 1);
			}
			else return super.keyPressed(event);
			return true;
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
		if (selected != null && selected.section() == LexiconCatalog.Section.STRUCTURES && !castingRecipeView) {
			if (structureView == StructureViewMode.THREE_D)
				structureZoom = Math.clamp(structureZoom - direction, 1, 5);
			else {
				LexiconStructurePreview preview = LexiconStructurePreview.get(selected, minecraft);
				if (preview.available())
					structureLayer = Math.clamp(structureLayer - direction, 0, preview.sizeY() - 1);
			}
			return true;
		}
		if (selected == null && view == View.PROGRESS && selectedStage == null)
			return moveProgressPage(direction);
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
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInsideStructurePreview(event.x(), event.y())
				&& structureView == StructureViewMode.THREE_D) {
			structureYaw += dx * 0.75;
			structurePitch = Math.clamp(structurePitch + dy * 0.75, 10, 80);
			return true;
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
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isInsideStructurePreview(event.x(), event.y())) {
			structureYaw = 45;
			structurePitch = 35;
			structureZoom = 2;
			return true;
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
		return x >= left + 7 && x < left + WIDTH - 7 && y >= top + 80 && y < top + 191;
	}

	private boolean moveSection(int direction) {
		LexiconCatalog.Section[] values = LexiconCatalog.Section.values();
		section = values[Math.floorMod(section.ordinal() + direction, values.length)];
		pageOffset = 0;
		rememberNavigation();
		rebuildWidgets();
		return true;
	}

	private boolean moveProgressView(int direction) {
		ProgressView[] values = ProgressView.values();
		progressView = values[Math.floorMod(progressView.ordinal() + direction, values.length)];
		progressOffset = 0;
		rebuildWidgets();
		return true;
	}

	private boolean moveProgressPage(int direction) {
		if (progressView == ProgressView.BY_LEVEL)
			return false;
		int pageSize = progressView == ProgressView.STAGES ? 8 : 7;
		int count = progressStages().size();
		int next = Math.max(0, Math.min(progressOffset + direction * pageSize,
				Math.max(0, ((count - 1) / pageSize) * pageSize)));
		if (next == progressOffset)
			return false;
		progressOffset = next;
		rebuildWidgets();
		return true;
	}

	private boolean moveListPage(int direction) {
		List<LexiconCatalog.Entry> pages = view == View.STORED_PAGES ? transferablePages() : visiblePages();
		int next = Math.max(0, Math.min(pageOffset + direction * PAGE_SIZE,
				Math.max(0, ((pages.size() - 1) / PAGE_SIZE) * PAGE_SIZE)));
		if (next == pageOffset)
			return false;
		pageOffset = next;
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

	private void renderCastingGrid(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		graphics.text(font, Component.literal("Casting Grid"), left + 92, top + 47, 0xffffffff, false);
		for (CastingTableRecipe.GridIngredient ingredient : recipe.grid()) {
			int slot = ingredient.slot();
			ItemStack stack = ingredientStack(ingredient.ingredient());
			if (!stack.isEmpty())
				graphics.item(stack, left + 94 + slot % 3 * 20, top + 62 + slot / 3 * 20);
		}
		graphics.text(font, Component.literal("→"), left + 160, top + 82, 0xffffffff, false);
		graphics.item(recipe.output(), left + 178, top + 76);
	}

	private void renderRuneRequirements(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		graphics.text(font, Component.literal("Temple Runes"), left + 72, top + 47, 0xffffffff, false);
		if (recipe.runes().isEmpty()) {
			graphics.text(font, Component.literal("No rune pattern required."), left + 72, top + 65, 0xffb0b0b0, false);
			return;
		}
		int row = 0;
		for (CastingTableRecipe.RuneRequirement rune : recipe.runes()) {
			ItemStack stack = new ItemStack(ChromaBlocks.rune(rune.element()).get());
			graphics.item(stack, left + 72 + row % 2 * 86, top + 62 + row / 2 * 20);
			graphics.text(font, Component.literal(rune.offset().toShortString()),
					left + 90 + row % 2 * 86, top + 66 + row / 2 * 20,
					0xff000000 | rune.element().getColor(), false);
			row++;
		}
	}

	private void renderStandRequirements(GuiGraphicsExtractor graphics, CastingTableRecipe recipe, int left, int top) {
		graphics.text(font, Component.literal("Auxiliary Item Stands"), left + 58, top + 47, 0xffffffff, false);
		if (recipe.stands().isEmpty()) {
			graphics.text(font, Component.literal("No item stands required."), left + 58, top + 65, 0xffb0b0b0, false);
			return;
		}
		for (int i = 0; i < Math.min(12, recipe.stands().size()); i++) {
			CastingTableRecipe.StandIngredient stand = recipe.stands().get(i);
			ItemStack stack = ingredientStack(stand.ingredient());
			int column = i / 6;
			int row = i % 6;
			graphics.item(stack, left + 58 + column * 96, top + 62 + row * 18);
			graphics.text(font, Component.literal(stand.offset().toShortString()), left + 76 + column * 96,
					top + 66 + row * 18, 0xffd0d0d0, false);
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
