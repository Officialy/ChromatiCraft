package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionDescriptions;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code GuiProgressStages} and its two subclasses: the research graph, laid out as a panned
 * field of small square nodes with lines to their prerequisites.
 *
 * <p>The port had a scrolling list of text buttons with a hand-written colour legend and a third
 * "Stages" mode. None of that is upstream — there are exactly two views, the tree and the by-level
 * ordering, and both draw the same 20x20 nodes; only the vertical ordering differs.
 *
 * <p>A node's border says where the player stands: green once reached, yellow once its prerequisites
 * are met, red while it is still locked. The border pulses, phase-shifted per stage so the field does
 * not blink in unison.
 */
final class LexiconProgressGraph {

	/** V33a {@code GuiProgressStages}: {@code elementHeight = 20}, {@code elementWidth = 20}. */
	private static final int ELEMENT = 20;
	private static final int SPACING_X = 30;
	private static final int SPACING_Y = 15;

	/** V33a {@code renderElements}: {@code posX+12, posY+36} with {@code posY} the origin less eight. */
	private static final int ORIGIN_X = 12;
	private static final int ORIGIN_Y = 28;

	/** V33a {@code super(g, ep, 256, 220, 242, 112)}. */
	static final int PANE_WIDTH = 242;
	static final int PANE_HEIGHT = 112;

	/** V33a has two progress views and no third. */
	enum Mode {
		TREE,
		BY_LEVEL
	}

	private final EnumMap<ProgressStage, Node> nodes = new EnumMap<>(ProgressStage.class);
	private final List<Edge> edges = new ArrayList<>();
	private Mode built;
	private int maxX;
	private int maxY;

	private record Node(int x, int y) {}
	private record Edge(Node child, Node parent) {}

	/**
	 * V33a {@code initMap}: nodes stack by depth, and within a depth they are laid left to right in
	 * enum order. Depth is how far a stage sits from a root in the prerequisite graph, so the tree
	 * reads top-down; the by-level view replaces that with the research level a stage belongs to.
	 */
	private void build(Mode mode) {
		if (built == mode)
			return;
		nodes.clear();
		edges.clear();
		maxX = 0;
		maxY = 0;
		Map<Integer, Integer> perDepth = new HashMap<>();
		EnumMap<ProgressStage, Integer> depths = new EnumMap<>(ProgressStage.class);
		for (ProgressStage p : ProgressStage.list) {
			if (!p.active)
				continue;
			int depth = mode == Mode.TREE ? depth(p, depths) : levelOf(p);
			int index = perDepth.merge(depth, 1, Integer::sum) - 1;
			int x = index * (ELEMENT + SPACING_X);
			int y = depth * (ELEMENT + SPACING_Y);
			nodes.put(p, new Node(x, y));
			maxX = Math.max(maxX, x + ELEMENT);
			maxY = Math.max(maxY, y + ELEMENT);
		}
		for (Map.Entry<ProgressStage, Node> entry : nodes.entrySet())
			for (ProgressStage parent : ProgressionManager.instance.getPrereqs(entry.getKey())) {
				Node parentNode = nodes.get(parent);
				if (parentNode != null)
					edges.add(new Edge(entry.getValue(), parentNode));
			}
		maxX = Math.max(0, maxX - (PANE_WIDTH - SPACING_X / 2));
		maxY = Math.max(0, maxY - (PANE_HEIGHT - SPACING_Y - 30));
		built = mode;
	}

	/** Longest path back to a stage with no prerequisites. */
	private static int depth(ProgressStage p, Map<ProgressStage, Integer> seen) {
		Integer cached = seen.get(p);
		if (cached != null)
			return cached;
		// Guard against a cycle in a datapack-authored graph: claim depth 0 while recursing, so a
		// loop terminates instead of overflowing the stack.
		seen.put(p, 0);
		int best = 0;
		for (ProgressStage parent : ProgressionManager.instance.getPrereqs(p))
			best = Math.max(best, depth(parent, seen) + 1);
		seen.put(p, best);
		return best;
	}

	private static int levelOf(ProgressStage p) {
		// V33a's by-level view orders by the earliest research level that requires the stage. That
		// mapping lives in ChromaResearchManager, which is not ported; prerequisite count is the
		// closest ordering available from what is, and keeps the two views visibly different.
		return ProgressionManager.instance.getPrereqs(p).size();
	}

	int maxScrollX(Mode mode) {
		this.build(mode);
		return maxX;
	}

	int maxScrollY(Mode mode) {
		this.build(mode);
		return maxY;
	}

	/**
	 * @return the stage under the cursor, so the caller can put a tooltip on it
	 */
	ProgressStage render(GuiGraphicsExtractor graphics, Font font, Player player, Mode mode,
			int left, int top, int offsetX, int offsetY, int mouseX, int mouseY) {
		this.build(mode);
		int baseX = left + ORIGIN_X - offsetX;
		int baseY = top + ORIGIN_Y - offsetY;
		graphics.enableScissor(left + 5, top + 18, left + 251, top + 112);

		// V33a renderLines: every node is joined to each of its prerequisites, drawn under the nodes.
		for (Edge edge : edges) {
			int x1 = baseX + edge.child.x() + ELEMENT / 2;
			int y1 = baseY + edge.child.y();
			int x2 = baseX + edge.parent.x() + ELEMENT / 2;
			int y2 = baseY + edge.parent.y() + ELEMENT;
			if (lineOnScreen(left, top, x1, y1, x2, y2))
				ReikaGuiLine.draw(graphics, x1, y1, x2, y2, 0xff707070);
		}

		ProgressStage hovered = null;
		for (Map.Entry<ProgressStage, Node> e : nodes.entrySet()) {
			ProgressStage p = e.getKey();
			int x = baseX + e.getValue().x();
			int y = baseY + e.getValue().y();
			if (!onScreen(left, top, x, y))
				continue;
			graphics.fill(x, y, x + ELEMENT, y + ELEMENT, 0xff444444);

			boolean has = p.isPlayerAtStage(player);
			boolean visible = p.playerHasPrerequisites(player);
			int border = has ? 0x00ff00 : visible ? 0xffff00 : 0xff0000;
			// V33a phases the pulse off the stage's hash so the field does not blink in unison.
			double t = (System.currentTimeMillis() / 5D + p.hashCode() * 23L) % 360;
			border = ReikaColorAPI.mixColors(border, 0xffffff,
					0.5F + 0.25F * (float)Math.sin(Math.toRadians(t)));
			frame(graphics, x, y, ELEMENT, ELEMENT, 0xff000000 | border);
			if (has || visible) {
				ItemStack icon = ProgressStageIconResolver.icon(p);
				if (!icon.isEmpty())
					graphics.item(icon, x + 2, y + 2);
			}
			else {
				graphics.centeredText(font, "?", x + ELEMENT / 2, y + 6, 0xffb0b0b0);
			}

			if (mouseX >= x && mouseX < x + ELEMENT && mouseY >= y && mouseY < y + ELEMENT)
				hovered = p;
		}
		graphics.disableScissor();

		if (hovered != null) {
			List<Component> lines = new ArrayList<>();
			lines.add(Component.literal(ProgressionDescriptions.title(hovered)));
			String text = hovered.isPlayerAtStage(player)
					? ProgressionDescriptions.reveal(hovered)
					: ProgressionDescriptions.hint(hovered);
			if (!text.isBlank())
				lines.add(Component.literal(text));
			graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		}
		return hovered;
	}

	ProgressStage hit(Mode mode, int left, int top, int offsetX, int offsetY, int mouseX, int mouseY) {
		this.build(mode);
		int baseX = left + ORIGIN_X - offsetX;
		int baseY = top + ORIGIN_Y - offsetY;
		for (Map.Entry<ProgressStage, Node> entry : nodes.entrySet()) {
			int x = baseX + entry.getValue().x();
			int y = baseY + entry.getValue().y();
			if (onScreen(left, top, x, y) && mouseX >= x && mouseX < x + ELEMENT
					&& mouseY >= y && mouseY < y + ELEMENT)
				return entry.getKey();
		}
		return null;
	}

	/** V33a elementOnScreen, in terms of the pane rather than the whole frame. */
	private static boolean onScreen(int left, int top, int x, int y) {
		return x >= left + 8 && x + ELEMENT <= left + 248
				&& y >= top + 18 && y + ELEMENT <= top + 112;
	}

	private static boolean lineOnScreen(int left, int top, int x1, int y1, int x2, int y2) {
		return Math.max(x1, x2) >= left + 5 && Math.min(x1, x2) <= left + 251
				&& Math.max(y1, y2) >= top + 18 && Math.min(y1, y2) <= top + 112;
	}

	private static void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
		graphics.fill(x, y, x + w, y + 1, color);
		graphics.fill(x, y + h - 1, x + w, y + h, color);
		graphics.fill(x, y, x + 1, y + h, color);
		graphics.fill(x + w - 1, y, x + w, y + h, color);
	}

	/** One transformed rectangle per edge, rather than one GUI render-state object per line pixel. */
	private static final class ReikaGuiLine {
		static void draw(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
			float dx = x2 - x1;
			float dy = y2 - y1;
			float length = (float)Math.sqrt(dx * dx + dy * dy);
			if (length < 0.5F)
				return;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x1, y1);
			graphics.pose().rotate((float)Math.atan2(dy, dx));
			graphics.fill(0, 0, Math.round(length), 1, color);
			graphics.pose().popMatrix();
		}
	}
}
