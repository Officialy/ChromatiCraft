package reika.chromaticraft.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.lore.KeyAssemblyPuzzle;
import reika.chromaticraft.magic.lore.Towers;

/** V33a's five-second, three-group lore-tower discovery presentation. */
public final class LoreDiscoveryOverlay implements GuiLayer {

	private static final LoreDiscoveryOverlay INSTANCE = new LoreDiscoveryOverlay();
	private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "lore_discovery");
	private static final Identifier HEXES = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lore/colorhexes.png");
	private List<List<KeyAssemblyPuzzle.CellView>> groups = List.of();
	private long started;

	private LoreDiscoveryOverlay() {}

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.TITLE, ID, INSTANCE);
	}

	public static void trigger(Towers tower, long seed, int scannedMask) {
		KeyAssemblyPuzzle puzzle = KeyAssemblyPuzzle.generate(seed);
		List<KeyAssemblyPuzzle.CellView> cells = puzzle.cells(scannedMask).stream()
				.filter(cell -> cell.tower() == tower).toList();
		List<List<KeyAssemblyPuzzle.CellView>> groups = new ArrayList<>(3);
		for (int i = 0; i < 3; i++)
			groups.add(List.copyOf(cells.subList(i * 4, Math.min(i * 4 + 4, cells.size()))));
		INSTANCE.groups = List.copyOf(groups);
		INSTANCE.started = System.currentTimeMillis();
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (groups.isEmpty()) return;
		double age = (System.currentTimeMillis() - started) / 50D;
		if (age >= 100) {
			groups = List.of();
			return;
		}
		float alpha = age < 12.5 ? (float)(age / 12.5)
				: age > 50 ? (float)(1 - (age - 50) / 50) : 1;
		alpha = Math.clamp(alpha, 0, 1);
		int backdrop = Math.round(120 * alpha) << 24 | 0x10162c;
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), backdrop);
		int centreX = graphics.guiWidth() / 2;
		int centreY = graphics.guiHeight() / 2;
		for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
			List<KeyAssemblyPuzzle.CellView> group = groups.get(groupIndex);
			double cx = group.stream().mapToDouble(KeyAssemblyPuzzle.CellView::x).average().orElse(0);
			double cy = group.stream().mapToDouble(KeyAssemblyPuzzle.CellView::y).average().orElse(0);
			for (KeyAssemblyPuzzle.CellView cell : group) {
				int x = centreX + (groupIndex - 1) * 120 + (int)Math.round((cell.x() - cx) * 2) - 15;
				int y = centreY + (int)Math.round((cell.y() - cy) * 2) - 13;
				int ordinal = cell.color().ordinal();
				int u = 2 + 64 * (ordinal % 8);
				int v = 6 + ordinal / 8 * 128 + (cell.active() ? 64 : 0);
				int tint = Math.round(255 * alpha) << 24 | 0xffffff;
				graphics.blit(RenderPipelines.GUI_TEXTURED, HEXES, x, y, u, v,
						30, 26, 60, 53, 512, 256, tint);
			}
		}
	}
}
