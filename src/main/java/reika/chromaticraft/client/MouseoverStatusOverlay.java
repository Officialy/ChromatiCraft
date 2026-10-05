package reika.chromaticraft.client;

import com.mojang.blaze3d.platform.InputConstants;
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
import reika.chromaticraft.client.hud.LumenStorageRenderState;
import reika.chromaticraft.magic.interfaces.LumenTile;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.tileentity.TileEntityDummyAux;

/**
 * V33a {@code MouseoverOverlayRenderer.renderStatusOverlay}, reached from {@code ChromaOverlays} only
 * while the player is holding the Elemental Manipulator. Looking at any {@link OperationInterval}
 * tile with the Manipulator draws its state icon beside the crosshair, and a running operation fills
 * a radial progress wedge over it.
 *
 * <p>The source used a four-block non-liquid ray, drew from row 1 of {@code infoicons.png} indexed by
 * the {@link OperationState} ordinal, and underlaid the icon at index 4 as a backing plate.
 *
 * <p>The {@code OperationInterval} and {@code LumenTile} branches are active. The other branches
 * (focus acceleration, chroma-crafter contents, Forestry bee
 * housings and Thaumcraft nodes) belong to tiles that are not in the compile slice yet, and are not
 * stubbed in.
 */
public final class MouseoverStatusOverlay implements GuiLayer {

	private static final MouseoverStatusOverlay INSTANCE = new MouseoverStatusOverlay();
	private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "mouseover_status");
	private static final Identifier ICONS =
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/infoicons.png");

	/**
	 * V33a indexes this sheet with {@code u = 0.125*idx, v = 0.25} and a {@code 0.125} extent on both
	 * axes, so it is an 8x8 grid of 32-pixel icons and the status row is row 2.
	 */
	private static final int COLUMNS = 8;
	private static final int STATUS_ROW = 2;
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

		HitResult hit = player.pick(4, delta.getGameTimeDeltaPartialTick(false), false);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK)
			return;
		BlockEntity tile = mc.level.getBlockEntity(block.getBlockPos());
		// DATANODE's visible/clickable body is four linked dummy blocks above the controller. The
		// source overlay resolves that relay before testing the tile interfaces; testing the dummy
		// itself made the activation status disappear whenever the player aimed at the actual model.
		if (tile instanceof TileEntityDummyAux dummy)
			tile = dummy.getLinkedTile();
		if (tile instanceof LumenTile lumen)
			renderStorage(gui, lumen, mc);
		if (tile instanceof reika.chromaticraft.tileentity.TileEntityLumenWire wire)
			renderDetectionMode(gui, wire);
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
	 * V33a {@code renderDetectionMode}: the wire's mode icon (row 1 of {@code infoicons.png} in 16-pixel cells, by mode)
	 * beside the crosshair at 12 pixels, pulsing between the mode colour and white.
	 */
	private static void renderDetectionMode(GuiGraphicsExtractor gui, reika.chromaticraft.tileentity.TileEntityLumenWire te) {
		int ox = gui.guiWidth()/2+12-8;
		int oy = gui.guiHeight()/2+12-8;
		var mode = te.getMode();
		float f = 0.75F+0.25F*(float)Math.sin(System.currentTimeMillis()/80D);
		int c = reika.dragonapi.libraries.rendering.ReikaColorAPI.mixColors(mode.renderColor, 0xffffff, f);
		gui.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICONS, ox, oy, 16*mode.ordinal(), 16, 12, 12, 16, 16, 256, 256, 0xff000000 | c);
	}

	private static void renderStorage(GuiGraphicsExtractor gui, LumenTile tile, Minecraft mc) {
		var energy = tile.getEnergy();
		if (energy == null)
			return;
		java.util.List<reika.chromaticraft.registry.CrystalElement> elements =
				new java.util.ArrayList<>(energy.elementSet());
		if (tile instanceof reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon pylon
				&& !elements.contains(pylon.getColor()))
			elements.add(pylon.getColor());
		int count = elements.size();
		if (count == 0)
			return;
		int[] colors = new int[count];
		float[] fractions = new float[count];
		int index = 0;
		for (var element : elements) {
			colors[index] = element.color();
			int capacity = tile.getMaxStorage(element);
			fractions[index] = capacity <= 0 ? 0 : Math.clamp(tile.getEnergy(element) / (float)capacity, 0, 1);
			index++;
		}
		int radius = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) ? 24 : 12;
		int centerX = gui.guiWidth() / 2 - 20;
		int centerY = gui.guiHeight() / 2 - 20;
		int hash = System.identityHashCode(tile);
		float rotation = (float)Math.toRadians(hash + 2L * tile.getTicksExisted() * (hash % 2 == 0 ? 1 : -1));
		gui.submitPictureInPictureRenderState(new LumenStorageRenderState(colors, fractions,
				radius, rotation, centerX - radius, centerY - radius,
				centerX + radius, centerY + radius, gui.peekScissorStack()));
	}

	/**
	 * V33a swept a triangle fan from three o'clock counter-clockwise:
	 * {@code dx = sin(a+90), dy = cos(a+90)} puts a=0 at the +X axis and increasing a moves upward on
	 * screen. A GUI layer has no tessellator, so the same wedge is produced by masking the pie icon
	 * per scanline: each row emits the maximal horizontal runs whose pixels fall inside the swept
	 * angle, and the icon's own alpha supplies the circle. That is the same reveal, and it animates
	 * because {@code getOperationFraction} is read every frame.
	 */
	private void renderProgress(GuiGraphicsExtractor gui, int x, int y, float fraction) {
		float swept = 360F * Math.clamp(fraction, 0F, 1F);
		if (swept <= 0)
			return;
		int index = OperationState.RUNNING.ordinal() + 1;
		int iconU = (index % COLUMNS) * SIZE;
		int iconV = STATUS_ROW * SIZE;
		float centre = SIZE / 2F;
		for (int row = 0; row < SIZE; row++) {
			int runStart = -1;
			for (int col = 0; col <= SIZE; col++) {
				boolean inside = col < SIZE && sweptAngle(col + 0.5F - centre, row + 0.5F - centre) <= swept;
				if (inside && runStart < 0) {
					runStart = col;
				}
				else if (!inside && runStart >= 0) {
					int width = col - runStart;
					gui.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICONS,
							x + runStart, y + row, iconU + runStart, iconV + row,
							width, 1, width, 1, 256, 256);
					runStart = -1;
				}
			}
		}
	}

	/** Degrees counter-clockwise from three o'clock, in screen space where +y points down. */
	private static float sweptAngle(float dx, float dy) {
		double degrees = Math.toDegrees(Math.atan2(-dy, dx));
		return (float)(degrees < 0 ? degrees + 360 : degrees);
	}

	private static void blit(GuiGraphicsExtractor gui, int x, int y, int size, int index) {
		int u = (index % COLUMNS) * SIZE;
		int v = STATUS_ROW * SIZE;
		// Source stays one 32-pixel icon even when the destination is inflated: passing the inflated
		// size as the source region too made the backing plate bleed into the neighbouring icons,
		// which is what drew a stray partial ring beside the status icon.
		gui.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICONS,
				x, y, u, v, size, size, SIZE, SIZE, 256, 256);
	}
}
