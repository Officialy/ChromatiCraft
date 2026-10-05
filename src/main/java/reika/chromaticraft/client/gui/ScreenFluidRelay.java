/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuFluidRelay;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.network.ChromaNetwork.FluidRelayControl;
import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;
import reika.dragonapi.instantiable.gui.ImagedGuiButton;
import reika.dragonapi.instantiable.rendering.GuiShapes;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.Collection;

/**
 * V33a {@code GuiFluidRelay}: the seven filter slots with their fluids, the pressure line "Pressure: base + per/B",
 * minus/plus buttons for the base pressure (upper row) and the per-bucket term (lower row) stepping by 10, 1 with
 * Control or 100 with Shift (their icons change while sneaking), a clear-filters button, "Auto" (copy the tank's
 * fluids into the filter) and the auto-filter toggle. Right-clicking a slot opens V33a's letter search over every
 * registered source fluid; the pick fills that slot.
 */
public final class ScreenFluidRelay extends ScreenLetterSearchable<MenuFluidRelay, Fluid> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/fluidrelay.png");
	private static final Identifier BUTTONS = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/buttons.png");

	private final TileEntityFluidRelay relay;
	private int lastSelSlot = -1;
	private boolean builtSneaking;
	private boolean builtAuto;

	public ScreenFluidRelay(MenuFluidRelay menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 153);
		relay = menu.relay();
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void init() {
		super.init();
		int j = leftPos;
		int k = topPos;
		int in = 22;
		int iny = 45;
		int dy = 5;
		boolean sneak = sneaking();
		builtSneaking = sneak;
		builtAuto = relay.autoFilter;
		this.addRenderableWidget(new ImagedGuiButton(0, j+in, k+iny-dy, 10, 10, 100, sneak ? 86 : 66, BUTTONS, b -> this.actionPerformed(0)));
		this.addRenderableWidget(new ImagedGuiButton(1, j+imageWidth-10-in, k+iny-dy, 10, 10, 100, sneak ? 76 : 56, BUTTONS, b -> this.actionPerformed(1)));
		this.addRenderableWidget(new ImagedGuiButton(2, j+in, k+iny+dy, 10, 10, 100, sneak ? 86 : 66, BUTTONS, b -> this.actionPerformed(2)));
		this.addRenderableWidget(new ImagedGuiButton(3, j+imageWidth-10-in, k+iny+dy, 10, 10, 100, sneak ? 76 : 56, BUTTONS, b -> this.actionPerformed(3)));
		int dx = 14;
		this.addRenderableWidget(new ImagedGuiButton(4, j+in-dx, k+iny, 10, 10, 90, 56, BUTTONS, b -> this.actionPerformed(4)));
		ImagedGuiButton auto = new ImagedGuiButton(5, j+imageWidth-10-in+dx, k+iny-dy, 10, 10, 90, 76, BUTTONS, b -> this.actionPerformed(5));
		auto.setTooltip(Tooltip.create(Component.literal("Auto")));
		this.addRenderableWidget(auto);
		this.addRenderableWidget(new ImagedGuiButton(6, j+imageWidth-10-in+dx, k+iny+dy, 10, 10, 90, relay.autoFilter ? 86 : 56, BUTTONS, b -> this.actionPerformed(6)));
	}

	private static boolean sneaking() {
		return Minecraft.getInstance().hasShiftDown();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		// V33a re-ran initGui after every press and swapped the sneak icons live.
		if (sneaking() != builtSneaking || relay.autoFilter != builtAuto)
			this.rebuildWidgets();
	}

	private void actionPerformed(int id) {
		int delta1 = 0;
		int delta2 = 0;
		Minecraft mc = Minecraft.getInstance();
		int n = mc.hasControlDown() ? 1 : (mc.hasShiftDown() ? 100 : 10);
		switch(id) {
			case 0 -> delta1 = -n;
			case 1 -> delta1 = n;
			case 2 -> delta2 = -n;
			case 3 -> delta2 = n;
			case 4 -> send(FluidRelayControl.CLEAR, 0);
			case 5 -> send(FluidRelayControl.COPY, 0);
			case 6 -> send(FluidRelayControl.AUTO, 0);
		}
		if (delta1 != 0)
			send(FluidRelayControl.BASE_PRESSURE, delta1);
		if (delta2 != 0)
			send(FluidRelayControl.FUNCTION_PRESSURE, delta2);
	}

	private void send(int action, int value) {
		ClientPacketDistributor.sendToServer(new ChromaNetwork.FluidRelayControl(relay.getBlockPos(), action, value, -1));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, (imageWidth-font.width(title))/2, 4, 0xffffffff, false);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffffffff, false);

		Fluid[] types = relay.getFluidTypes();
		int mansel = menu.getManualSelectSlot();
		if (mansel != lastSelSlot)
			this.resetFilter(true);
		for (int i = 0; i < types.length; i++) {
			int x = 14+i*22;
			int y = 17;
			Fluid f = types[i];
			if (i == mansel) {
				int c = ReikaColorAPI.mixColors(0x77d0ff, 0x22aaff, 0.5F+0.5F*Mth.sin((System.currentTimeMillis()%1000000)/250F));
				frame(g, x-2, y-2, 19, 19, 0xff000000 | c);
				f = this.getActive();
			}
			if (f != null) {
				drawFluid(g, f, x, y);
				if (mouseX-leftPos >= x && mouseX-leftPos < x+16 && mouseY-topPos >= y && mouseY-topPos < y+16)
					g.setTooltipForNextFrame(Component.literal(this.getString(f)), mouseX-leftPos, mouseY-topPos);
			}
		}
		String s = String.format("Pressure: %d + %d/B", relay.getBasePressure(), relay.getFunctionPressure());
		g.text(font, s, imageWidth/2-font.width(s)/2, 45, 0xffffffff, false);
		lastSelSlot = mansel;

		this.drawSearch(g);
	}

	/** V33a {@code ReikaLiquidRenderer.getFluidIconSafe}: the still texture, in the fluid's tint (1.7.10 icons carried it). */
	private void drawFluid(GuiGraphicsExtractor g, Fluid f, int x, int y) {
		var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState());
		TextureAtlasSprite ico = model.stillMaterial().sprite();
		int tint = 0xffffffff;
		var tintSource = model.fluidTintSource();
		if (tintSource != null && minecraft.level != null && minecraft.player != null)
			tint = 0xff000000 | tintSource.colorInWorld(f.defaultFluidState(), f.defaultFluidState().createLegacyBlock(),
					minecraft.level, minecraft.player.blockPosition());
		else if (tintSource != null)
			tint = 0xff000000 | tintSource.color(f.defaultFluidState());
		GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, ico, x, y, 16, 16, tint);
	}

	/** V33a {@code api.drawRectFrame}: a one-pixel outline. */
	private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x+w, y+1, color);
		g.fill(x, y+h-1, x+w, y+h, color);
		g.fill(x, y, x+1, y+h, color);
		g.fill(x+w-1, y, x+w, y+h, color);
	}

	@Override
	protected Identifier getGuiTexture() {
		return TEXTURE;
	}

	@Override
	protected String getString(Fluid f) {
		return f.getFluidType().getDescription().getString();
	}

	/** V33a {@code FluidRegistry.getRegisteredFluids}: every source fluid. */
	@Override
	protected Collection<Fluid> getAllEntries(Player ep) {
		ArrayList<Fluid> li = new ArrayList<>();
		for (Fluid f : BuiltInRegistries.FLUID) {
			if (f != Fluids.EMPTY && f.isSource(f.defaultFluidState()))
				li.add(f);
		}
		return li;
	}

	@Override
	protected void sortEntries(ArrayList<Fluid> li) {
		li.removeIf(f -> f == null || f == Fluids.EMPTY);
		li.sort((f1, f2) -> Integer.compare(BuiltInRegistries.FLUID.getId(f1), BuiltInRegistries.FLUID.getId(f2)));
	}

	@Override
	protected boolean isSearchActive() {
		return super.isSearchActive() && menu.getManualSelectSlot() >= 0;
	}

	@Override
	protected void onSelected(Fluid f) {
		menu.setFluid(f);
	}

}
