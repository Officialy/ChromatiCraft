package reika.chromaticraft.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval.OperationState;
import reika.chromaticraft.registry.ChromaItems;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/**
 * V33a {@code MouseoverOverlayRenderer.renderStatusOverlay}, reached from {@code ChromaOverlays} only
 * while the player is holding the Elemental Manipulator. Looking at any {@link OperationInterval}
 * tile with the Manipulator draws its state icon beside the crosshair, and a running operation fills
 * a radial progress wedge over it.
 *
 * <p>The source used a four-block non-liquid ray, drew from row 1 of {@code infoicons.png} indexed by
 * the {@link OperationState} ordinal, and underlaid the icon at index 4 as a backing plate.
 *
 * <p>Only the {@code OperationInterval} branch of the original renderer is ported here; its other
 * branches (lumen storage, focus acceleration, chroma-crafter contents, lumen wire, Forestry bee
 * housings and Thaumcraft nodes) belong to tiles that are not in the compile slice yet, and are not
 * stubbed in.
 */
public final class MouseoverStatusOverlay implements GuiLayer {

	private static final MouseoverStatusOverlay INSTANCE = new MouseoverStatusOverlay();
	private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "mouseover_status");
	private static final Identifier ICONS =
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/infoicons.png");

	/** The sheet is 8x4 icons; the status row is the second one. */
	private static final int COLUMNS = 8;
	private static final int ROWS = 4;
	private static final int STATUS_ROW = 1;
	private static final int BACKING_INDEX = 4;
	/** V33a: a 32-pixel icon offset ar=12 from the screen centre, with a 3-pixel backing bleed. */
	private static final int SIZE = 32;
	private static final int OFFSET = 12 - 8;
	private static final int BACKING_BLEED = 3;

	private MouseoverStatusOverlay() {}

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.CROSSHAIR, ID, INSTANCE);
	}

	@Override
	public void render(GuiGraphicsExtractor gui, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null)
			return;
		if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(ChromaItems.MANIPULATOR.get()))
			return;

		HitResult hit = ReikaPlayerAPI.getLookedAtBlock(player, 4, false);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK)
			return;
		BlockEntity tile = mc.level.getBlockEntity(block.getBlockPos());
		if (!(tile instanceof OperationInterval operation))
			return;

		int x = gui.guiWidth() / 2 + OFFSET;
		int y = gui.guiHeight() / 2 + OFFSET;
		OperationState state = operation.getState();

		blit(gui, x - BACKING_BLEED, y - BACKING_BLEED, SIZE + BACKING_BLEED * 2, BACKING_INDEX);
		blit(gui, x, y, SIZE, state.ordinal());
		if (state == OperationState.RUNNING)
			this.renderProgress(gui, x, y, operation.getOperationFraction());
	}

	/**
	 * V33a drew the progress as a triangle fan sweeping clockwise from twelve o'clock. A GUI layer
	 * has no tessellator, so the same wedge is built from the icon that follows RUNNING on the sheet,
	 * clipped to the swept fraction by drawing it in horizontal bands — visually the same fill, and
	 * it stays inside the blit API the rest of this overlay uses.
	 */
	private void renderProgress(GuiGraphicsExtractor gui, int x, int y, float fraction) {
		int filled = Math.round(SIZE * Math.clamp(fraction, 0F, 1F));
		if (filled <= 0)
			return;
		int index = OperationState.RUNNING.ordinal() + 1;
		int u = (index % COLUMNS) * (256 / COLUMNS);
		int v = STATUS_ROW * (256 / ROWS) + (SIZE - filled) * (256 / ROWS) / SIZE;
		gui.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICONS,
				x, y + SIZE - filled, u, v, SIZE, filled, 256, 256);
	}

	private static void blit(GuiGraphicsExtractor gui, int x, int y, int size, int index) {
		int u = (index % COLUMNS) * (256 / COLUMNS);
		int v = STATUS_ROW * (256 / ROWS);
		gui.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICONS,
				x, y, u, v, size, size, 256, 256);
	}
}
