package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
	public record Cell(LexiconCatalog.Entry entry, int x, int y) {}

	/** One research-level group inside a section. */
	private record Category(ResearchLevel level, List<LexiconCatalog.Entry> entries) {}

	/** One section box, in sheet space. */
	public record Box(String title, int x, int y, int width, int height, List<Cell> cells) {}

	private final List<Box> boxes = new ArrayList<>();
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
			List<Cell> cells = new ArrayList<>();
			int x = 0;
			int height = 0;
			for (Category category : categories) {
				int gridHeight = subHeight(category.entries().size(), cols);
				height = Math.max(height, gridHeight);
				int index = 0;
				for (LexiconCatalog.Entry entry : category.entries()) {
					int cx = x + MARGIN + (index % cols) * (ELEMENT + SPACING);
					int cy = y + font.lineHeight + 2 + MARGIN + (index / cols) * (ELEMENT + SPACING);
					cells.add(new Cell(entry, cx, cy));
					index++;
				}
				x += subWidth(category, cols, font) + CATEGORY_GAP;
			}
			x = Math.max(0, x - CATEGORY_GAP);
			int boxHeight = height + MARGIN + font.lineHeight + 2;
			boxes.add(new Box(section.title().getString(), 0, y, x, boxHeight, cells));
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
	public void render(GuiGraphicsExtractor graphics, Font font, int leftX, int topY,
			int offsetX, int offsetY, int paneWidth, int paneHeight,
			java.util.function.Function<LexiconCatalog.Entry, ItemStack> icons,
			java.util.function.Predicate<LexiconCatalog.Entry> unlocked) {
		int originX = leftX + MARGIN - offsetX;
		int originY = topY + 1 - offsetY;
		for (Box box : boxes) {
			int bx = originX + box.x();
			int by = originY + box.y();
			outline(graphics, bx, by, bx + box.width(), by + box.height(),
					leftX, topY, paneWidth, paneHeight, 0xff5a5a5a);
			if (bx >= leftX && bx <= leftX + paneWidth - font.width(box.title())
					&& by >= topY && by <= topY + paneHeight - font.lineHeight / 2)
				graphics.text(font, Component.literal(box.title()), bx + 2, by - font.lineHeight,
						0xffd8d8d8, false);
			for (Cell cell : box.cells()) {
				int cx = originX + cell.x();
				int cy = originY + cell.y();
				if (cx < leftX || cx > leftX + paneWidth - ELEMENT)
					continue;
				if (cy < topY || cy > topY + paneHeight - ELEMENT)
					continue;
				ItemStack icon = icons.apply(cell.entry());
				if (!icon.isEmpty())
					graphics.item(icon, cx + 4, cy + 4);
				if (!unlocked.test(cell.entry()))
					graphics.text(font, Component.literal("?"), cx + 9, cy + 9, 0xffff6060, true);
			}
		}
	}

	/** The entry under the cursor, or null. Uses the same clipping the renderer does. */
	public LexiconCatalog.Entry hit(int mouseX, int mouseY, int leftX, int topY,
			int offsetX, int offsetY, int paneWidth, int paneHeight) {
		int originX = leftX + MARGIN - offsetX;
		int originY = topY + 1 - offsetY;
		for (Box box : boxes) {
			for (Cell cell : box.cells()) {
				int cx = originX + cell.x();
				int cy = originY + cell.y();
				if (cx < leftX || cx > leftX + paneWidth - ELEMENT)
					continue;
				if (cy < topY || cy > topY + paneHeight - ELEMENT)
					continue;
				if (mouseX >= cx && mouseX < cx + ELEMENT && mouseY >= cy && mouseY < cy + ELEMENT)
					return cell.entry();
			}
		}
		return null;
	}

	private static void outline(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1,
			int leftX, int topY, int paneWidth, int paneHeight, int color) {
		int cx0 = Math.clamp(x0, leftX + 2, leftX + paneWidth + 10);
		int cy0 = Math.clamp(y0, topY - 5, topY + paneHeight + 5);
		int cx1 = Math.clamp(x1, leftX + 2, leftX + paneWidth + 10);
		int cy1 = Math.clamp(y1, topY - 5, topY + paneHeight + 5);
		if (cx1 <= cx0 || cy1 <= cy0)
			return;
		graphics.fill(cx0, cy0, cx1, cy0 + 1, color);
		graphics.fill(cx0, cy1, cx1, cy1 + 1, color);
		graphics.fill(cx0, cy0, cx0 + 1, cy1, color);
		graphics.fill(cx1, cy0, cx1 + 1, cy1, color);
	}
}
