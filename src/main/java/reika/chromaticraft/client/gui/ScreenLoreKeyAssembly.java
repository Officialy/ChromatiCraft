package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.lore.KeyAssemblyPuzzle;
import reika.chromaticraft.magic.lore.RosettaStone;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaSounds;

/** V33a full-screen hex-key assembly and its completed Rosetta presentation. */
public final class ScreenLoreKeyAssembly extends Screen {

	private static final Identifier HEXES = id("textures/gui/lore/colorhexes.png");
	private static final Identifier VOIDS = id("textures/gui/lore/colorhexes-voids.png");
	private static final Identifier BACKGROUND = id("textures/gui/lore/all-back.png");
	private KeyAssemblyPuzzle puzzle;
	private RosettaStone rosetta;
	private int scannedMask;
	private boolean complete;
	private int left;
	private int top;
	private boolean awaitingServer;

	public ScreenLoreKeyAssembly(long seed, int scannedMask, boolean complete, int[] moves) {
		super(Component.translatable("chromaticraft.lore.key_assembly"));
		acceptState(seed, scannedMask, complete, moves);
	}

	public void acceptState(long seed, int scannedMask, boolean complete, int[] moves) {
		boolean newlyComplete = !this.complete && complete;
		this.puzzle = KeyAssemblyPuzzle.generate(seed);
		this.puzzle.applyPackedMoves(moves);
		this.rosetta = new RosettaStone(seed);
		this.scannedMask = scannedMask;
		this.complete = complete;
		this.awaitingServer = false;
		if (newlyComplete && minecraft != null && minecraft.player != null)
			ChromaSounds.LORECOMPLETE.playSound(minecraft.player, 1, 1);
	}

	@Override
	protected void init() {
		left = width / 2;
		top = height / 2;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (complete) renderRosetta(graphics);
		else renderBoard(graphics, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void renderBoard(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// V33a leaves the key-assembly board over the live world. An opaque replacement panel made
		// the Memory Crystal feel like an ordinary menu and hid the intended transparent backdrop.
		for (KeyAssemblyPuzzle.CellView cell : puzzle.cells(scannedMask)) {
			int x = left + (int)Math.round(cell.x()) - 7;
			int y = top + (int)Math.round(cell.y()) - 6;
			if (cell.color() == null) continue;
			if (!cell.known()) {
				int frame = (int)(System.currentTimeMillis() / 70 % 16);
				graphics.blit(RenderPipelines.GUI_TEXTURED, VOIDS, x, y,
						2, 6 + frame * 64, 15, 13, 60, 53, 128, 1024);
			}
			else {
				int ordinal = cell.color().ordinal();
				int u = 2 + 64 * (ordinal % 8);
				int v = 6 + ordinal / 8 * 128 + (cell.active() ? 64 : 0);
				graphics.blit(RenderPipelines.GUI_TEXTURED, HEXES, x, y,
						u, v, 15, 13, 60, 53, 512, 256);
			}
		}
		graphics.centeredText(font, title, width / 2, 8, 0xffb0e0ff);
		graphics.centeredText(font, Component.translatable("chromaticraft.lore.key_hint"),
				width / 2, height - 16, awaitingServer ? 0xff888888 : 0xffd0d8ff);
	}

	private void renderRosetta(GuiGraphicsExtractor graphics) {
		// all-back.png is one authored full-screen composition, not a tile. Stretch one complete
		// source image over the scaled GUI so its edge motifs do not restart every 256 pixels.
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, 0, 0,
				width, height, 256, 256, 256, 256);
		int y = 8;
		long frame = System.currentTimeMillis() / 100;
		for (String line : rosetta.lines()) {
			graphics.text(font, Component.literal(line), width - 12 - font.width(line), y,
					0xffb0e0ff, false);
			y += 8;
		}
		y += 10;
		for (String line : rosetta.lines()) {
			graphics.text(font, Component.literal(rosetta.translatedLine(line, frame)), 5, y,
					0xffffffff, false);
			y += 9;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!complete && !awaitingServer && event.button() == 0) {
			KeyAssemblyPuzzle.CellView cell = puzzle.cellAt(event.x() - left, event.y() - top, scannedMask);
			if (cell != null && cell.color() != null && cell.known()) {
				awaitingServer = true;
				ClientPacketDistributor.sendToServer(new ChromaNetwork.LorePuzzleMove(
						cell.q(), cell.r(), cell.s()));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
	}
}
