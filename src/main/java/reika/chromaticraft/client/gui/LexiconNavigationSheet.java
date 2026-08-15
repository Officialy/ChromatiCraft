package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.ResearchLevel;

/**
 * V33a {@code GuiNavigation}'s spatial layout: every section on one pannable sheet rather than a
 * paged list.
 *
 * <p>Sections stack downward, separated by {@link #SECTION_GAP}. Inside a section the entries are
 * grouped by {@link ResearchLevel} into categories laid out left to right, separated by
 * {@link #CATEGORY_GAP}, and each category is a grid of {@link #ELEMENT}-pixel cells. The column
 * count is V33a's {@code allOneLevel() ? 10 : 4}: a section whose entries share one research level
 * gets a wide, shallow block, and a section spanning several levels gets narrow columns per level so
 * the levels read as separate groups.
 *
 * <p>This is what gives the navigation its two axes — you pan down through sections and across
 * through research levels — and it is why the pane has a horizontal bound at all.
 */
public final class LexiconNavigationSheet {

	/** V33a Section.elementWidth. */
	public static final int ELEMENT = 24;
	/** V33a GuiNavigation.SectionSpacing, between stacked sections. */
	public static final int SECTION_GAP = 32;
	/** V33a Section.sectionSpacing, between research-level categories within a section. */
	public static final int CATEGORY_GAP = 64;
	/** V33a Section.margin. */
	public static final int MARGIN = 8;
	/** V33a Section.spacing, between grid cells. */
	public static final int SPACING = 4;

	/** One entry's cell, in sheet space; the pan offset is applied at draw time. */
	public static final class Cell {
		final LexiconCatalog.Entry entry;
		final int x;
		final int y;
		/** V33a SectionElement.searchAlpha: rises 0.05/frame when matching, falls 0.1/frame when not. */
		float searchAlpha = 1;

		Cell(LexiconCatalog.Entry entry, int x, int y) {
			this.entry = entry;
			this.x = x;
			this.y = y;
		}

		public LexiconCatalog.Entry entry() { return entry; }
	}

	/** One research-level group inside a section. */
	private record Category(ResearchLevel level, List<LexiconCatalog.Entry> entries) {}

	/** A drawn outline with V33a's hover ramp. */
	public static final class Frame {
		final int x;
		final int y;
		final int width;
		final int height;
		/** V33a hoverTime: 0-20, +1 per frame hovered, -1 every other frame otherwise. */
		int hoverTime;

		Frame(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		/** V33a: ReikaColorAPI.GStoHex(15 + hoverTime * 12) -- greyscale 15..255. */
		int color() {
			int grey = Math.clamp(15 + hoverTime * 12, 0, 255);
			return 0xff000000 | (grey << 16) | (grey << 8) | grey;
		}
	}

	/** One section box, in sheet space. */
	public static final class Box {
		final String title;
		final Frame frame;
		final List<Frame> categories = new ArrayList<>();
		final List<Cell> cells = new ArrayList<>();

		Box(String title, Frame frame) {
			this.title = title;
			this.frame = frame;
		}

		public List<Cell> cells() { return cells; }
	}

	private final List<Box> boxes = new ArrayList<>();
	private String layoutSignature;
	private int maxX;
	private int maxY;

	public List<Box> boxes() {
		return boxes;
	}

	public int maxX() {
		return maxX;
	}

	public int maxY() {
		return maxY;
	}

	/**
	 * Lays the whole sheet out. {@code paneWidth}/{@code paneHeight} are the visible window, used for
	 * V33a's bound subtraction so panning stops when the last content is on screen.
	 */
	public void build(List<LexiconCatalog.Section> sections,
			java.util.function.Function<LexiconCatalog.Section, List<LexiconCatalog.Entry>> lookup,
			Font font, int paneWidth, int paneHeight) {
		// Rebuilding every frame would reset the hover and search ramps, so only lay out again when
		// the inputs actually change.
		String signature = sections + "|" + paneWidth + "x" + paneHeight;
		if (signature.equals(layoutSignature))
			return;
		layoutSignature = signature;
		boxes.clear();
		maxX = 0;
		maxY = 0;
		int y = 0;
		for (LexiconCatalog.Section section : sections) {
			List<LexiconCatalog.Entry> entries = lookup.apply(section);
			if (entries.isEmpty())
				continue;
			List<Category> categories = group(entries);
			int cols = categories.size() == 1 ? 10 : 4;
			int x = 0;
			int height = 0;
			List<Frame> catFrames = new ArrayList<>();
			List<Cell> cells = new ArrayList<>();
			int gridTop = y + font.lineHeight + 2;
			for (Category category : categories) {
				int gridHeight = subHeight(category.entries().size(), cols);
				height = Math.max(height, gridHeight);
				catFrames.add(new Frame(x + 4, gridTop + 4, subWidth(category, cols, font), gridHeight));
				int index = 0;
				for (LexiconCatalog.Entry entry : category.entries()) {
					int cx = x + MARGIN + (index % cols) * (ELEMENT + SPACING);
					int cy = gridTop + MARGIN + (index / cols) * (ELEMENT + SPACING);
					cells.add(new Cell(entry, cx, cy));
					index++;
				}
				x += subWidth(category, cols, font) + CATEGORY_GAP;
			}
			x = Math.max(0, x - CATEGORY_GAP);
			int boxHeight = height + MARGIN + font.lineHeight + 2;
			Box box = new Box(section.title().getString(), new Frame(0, y, x, boxHeight));
			box.categories.addAll(catFrames);
			box.cells.addAll(cells);
			boxes.add(box);
			maxX = Math.max(maxX, x);
			y += boxHeight + SECTION_GAP;
		}
		// V33a: the bounds stop scrolling once the final content edge reaches the pane edge.
		maxX = Math.max(0, maxX - paneWidth + MARGIN * 2);
		maxY = Math.max(0, y - SECTION_GAP / 2 - paneHeight);
	}

	/** V33a Section.getSubSectionHeight. */
	private static int subHeight(int size, int cols) {
		int rows = 1 + (size - 1) / cols;
		return rows * ELEMENT + (rows - 1) * SPACING + MARGIN - 1;
	}

	/** V33a squarefog.png: the haze a non-matching search entry recedes behind. */
	private static final Identifier SEARCH_FOG = Identifier.fromNamespaceAndPath(
			reika.chromaticraft.ChromatiCraft.MODID, "textures/gui/lexicon/squarefog.png");
	private static final int FOG_SIZE = 48;

	/** V33a Section.getSubsectionWidth, with the level title taken into account. */
	private static int subWidth(Category category, int cols, Font font) {
		int size = category.entries().size();
		int num = size >= cols ? cols : size % cols;
		if (num == 0)
			num = cols;
		int w = num * ELEMENT + (num - 1) * SPACING + MARGIN - 1;
		return Math.max(w, font.width(displayName(category.level())) - CATEGORY_GAP + ELEMENT);
	}

	private static List<Category> group(List<LexiconCatalog.Entry> entries) {
		Map<ResearchLevel, List<LexiconCatalog.Entry>> map = new EnumMap<>(ResearchLevel.class);
		for (LexiconCatalog.Entry entry : entries) {
			ResearchLevel level = entry.level() == null ? ResearchLevel.ENTRY : entry.level();
			map.computeIfAbsent(level, k -> new ArrayList<>()).add(entry);
		}
		List<Category> out = new ArrayList<>();
		map.forEach((level, list) -> out.add(new Category(level, list)));
		return out;
	}

	private static String displayName(ResearchLevel level) {
		String name = level.name().toLowerCase(java.util.Locale.ENGLISH);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	/**
	 * Draws the sheet. V33a outlines each section and each category, clamping the outline to the pane
	 * so a partly-scrolled box still reads as a box rather than spilling over the frame, and skips any
	 * title or icon that would fall outside the window.
	 */
	public void render(GuiGraphicsExtractor graphics, Font font, int frameLeft, int frameTop,
			int offsetX, int offsetY, int paneWidth, int paneHeight, int mouseX, int mouseY,
			String search, long tick,
			java.util.function.Function<LexiconCatalog.Entry, ItemStack> icons,
			java.util.function.Predicate<LexiconCatalog.Entry> unlocked) {
		// GuiNavigation calls drawSections(frameLeft + 11, frameTop + 11), whose
		// first section frame begins another (4, 1) pixels in. Keep the frame origin
		// separate from that content origin; conflating them shifted everything right
		// and made V33a's clipping bounds extend outside the book.
		int originX = frameLeft + 15 - offsetX;
		int originY = frameTop + 12 - offsetY;
		graphics.enableScissor(frameLeft + 7, frameTop - 1,
				frameLeft + 7 + paneWidth, frameTop - 1 + paneHeight);
		for (Box box : boxes) {
			int bx = originX + box.frame.x;
			int by = originY + box.frame.y;
			boolean anyHover = false;

			for (Frame category : box.categories) {
				int cxa = originX + category.x;
				int cya = originY + category.y;
				boolean hover = mouseX >= cxa && mouseX < cxa + category.width
						&& mouseY >= cya && mouseY < cya + category.height;
				ramp(category, hover, tick);
				anyHover |= hover;
				outline(graphics, cxa, cya, cxa + category.width, cya + category.height,
						frameLeft, frameTop, paneWidth, paneHeight, category.color());
			}

			ramp(box.frame, anyHover, tick);
			outline(graphics, bx, by, bx + box.frame.width, by + box.frame.height,
					frameLeft, frameTop, paneWidth, paneHeight, box.frame.color());
			if (bx >= frameLeft && bx <= frameLeft + paneWidth - font.width(box.title)
					&& by >= frameTop && by <= frameTop + paneHeight - font.lineHeight / 2)
				graphics.text(font, Component.literal(box.title), bx + 2, by - font.lineHeight,
						mix(box.frame.color(), 0xffffffff, 0.675F), false);

			for (Cell cell : box.cells) {
				// V33a updateSearch: matching entries fade in at 0.05/frame, others out at 0.1/frame,
				// so a search dims the rest of the sheet rather than removing it.
				boolean matches = search == null || search.isBlank()
						|| cell.entry.title().getString().toLowerCase(java.util.Locale.ENGLISH)
								.contains(search.toLowerCase(java.util.Locale.ENGLISH));
				cell.searchAlpha = matches ? Math.min(1, cell.searchAlpha + 0.05F)
						: Math.max(0, cell.searchAlpha - 0.1F);
				if (cell.searchAlpha <= 0.02F)
					continue;
				int cx = originX + cell.x;
				int cy = originY + cell.y;
				if (cx < frameLeft || cx > frameLeft + paneWidth - ELEMENT)
					continue;
				if (cy < frameTop || cy > frameTop + paneHeight - ELEMENT)
					continue;
				ItemStack icon = icons.apply(cell.entry);
				if (!icon.isEmpty())
					graphics.item(icon, cx + 4, cy + 4);
				else
					renderMissingIcon(graphics, font, cx + 4, cy + 4);
				if (!unlocked.test(cell.entry))
					graphics.text(font, Component.literal("?"), cx + 9, cy + 9, 0xffff6060, true);
				if (cell.searchAlpha < 1) {
					// V33a fades a non-matching entry behind squarefog.png at alpha 1 - searchAlpha,
					// over the icon's box grown two pixels on every side. A flat black scrim stood here
					// before, which dimmed the icon rather than fogging it.
					int alpha = Math.round((1 - cell.searchAlpha) * 255);
					graphics.blit(RenderPipelines.GUI_TEXTURED, SEARCH_FOG,
							cx + 2, cy + 2, 0, 0, ELEMENT - 4, ELEMENT - 4, FOG_SIZE, FOG_SIZE,
							FOG_SIZE, FOG_SIZE, alpha << 24 | 0xffffff);
				}
			}
		}
		graphics.disableScissor();
	}

	/**
	 * A loud development placeholder for catalog entries whose source binding has not landed yet.
	 * Leaving the cell empty made an unported icon indistinguishable from missing navigation data;
	 * this checker remains entirely runtime-drawn so it cannot accidentally ship as a block/item
	 * texture or conceal which resolver case still needs implementing.
	 */
	private static void renderMissingIcon(GuiGraphicsExtractor graphics, Font font, int x, int y) {
		final int cell = 4;
		for (int dx = 0; dx < 16; dx += cell)
			for (int dy = 0; dy < 16; dy += cell)
				graphics.fill(x + dx, y + dy, x + dx + cell, y + dy + cell,
						((dx + dy) / cell & 1) == 0 ? 0xffff00ff : 0xff101010);
		graphics.text(font, Component.literal("!"), x + 6, y + 4, 0xffffffff, true);
	}

	/** V33a: hoverTime climbs one per frame to 20, and decays one every other frame. */
	private static void ramp(Frame frame, boolean hovered, long tick) {
		if (hovered) {
			if (frame.hoverTime < 20)
				frame.hoverTime++;
		}
		else if (frame.hoverTime > 0 && tick % 2 == 0) {
			frame.hoverTime--;
		}
	}

	/** V33a ReikaColorAPI.mixColors. */
	private static int mix(int a, int b, float f) {
		int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
		int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
		int r = (int)(ar * f + br * (1 - f));
		int g = (int)(ag * f + bg * (1 - f));
		int bl = (int)(ab * f + bb * (1 - f));
		return 0xff000000 | (r << 16) | (g << 8) | bl;
	}

	/** The entry under the cursor, or null. Uses the same clipping the renderer does. */
	public LexiconCatalog.Entry hit(int mouseX, int mouseY, int frameLeft, int frameTop,
			int offsetX, int offsetY, int paneWidth, int paneHeight) {
		int originX = frameLeft + 15 - offsetX;
		int originY = frameTop + 12 - offsetY;
		for (Box box : boxes) {
			for (Cell cell : box.cells) {
				int cx = originX + cell.x;
				int cy = originY + cell.y;
				if (cx < frameLeft + 7 || cx > frameLeft + 7 + paneWidth - ELEMENT)
					continue;
				if (cy < frameTop - 1 || cy > frameTop - 1 + paneHeight - ELEMENT)
					continue;
				if (mouseX >= cx && mouseX < cx + ELEMENT && mouseY >= cy && mouseY < cy + ELEMENT)
					return cell.entry;
			}
		}
		return null;
	}

	private static void outline(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1,
			int frameLeft, int frameTop, int paneWidth, int paneHeight, int color) {
		int cx0 = Math.clamp(x0, frameLeft + 2, frameLeft + paneWidth + 10);
		int cy0 = Math.clamp(y0, frameTop - 5, frameTop + paneHeight + 5);
		int cx1 = Math.clamp(x1, frameLeft + 2, frameLeft + paneWidth + 10);
		int cy1 = Math.clamp(y1, frameTop - 5, frameTop + paneHeight + 5);
		if (cx1 <= cx0 || cy1 <= cy0)
			return;
		graphics.fill(cx0, cy0, cx1, cy0 + 1, color);
		graphics.fill(cx0, cy1, cx1, cy1 + 1, color);
		graphics.fill(cx0, cy0, cx0 + 1, cy1, color);
		graphics.fill(cx1, cy0, cx1 + 1, cy1, color);
	}
}
