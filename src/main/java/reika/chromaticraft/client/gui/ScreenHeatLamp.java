package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuHeatLamp;
import reika.chromaticraft.network.ChromaNetwork;

/** Exact compact 176x48 Heat Lamp editor adapted to the 26.2 extracted-GUI pipeline. */
public final class ScreenHeatLamp extends AbstractContainerScreen<MenuHeatLamp> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/heatlamp.png");
	private EditBox input;
	private boolean settingInitialValue;
	/** Last value delivered by the menu DataSlot, not the locally typed value. */
	private int lastSyncedTemperature;

	public ScreenHeatLamp(MenuHeatLamp menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 48);
	}

	@Override
	protected void init() {
		super.init();
		settingInitialValue = true;
		input = new EditBox(font, leftPos + 88, topPos + 21, 60, 16, Component.literal("Temperature"));
		input.setMaxLength(4);
		input.setFilter(value -> value.isEmpty() || value.equals("-") || value.matches("-?\\d+"));
		lastSyncedTemperature = menu.temperature();
		input.setValue(Integer.toString(lastSyncedTemperature));
		input.setResponder(this::temperatureChanged);
		addRenderableWidget(input);
		settingInitialValue = false;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		int synced = menu.temperature();
		if (synced != lastSyncedTemperature) {
			lastSyncedTemperature = synced;
			// The client-side block entity still contains its construction default when the screen
			// opens. The authoritative DataSlot arrives just afterwards, so mirror every newly
			// delivered value into the editor without echoing it back as another request.
			settingInitialValue = true;
			input.setValue(Integer.toString(synced));
			settingInitialValue = false;
		}
	}

	private void temperatureChanged(String value) {
		if (settingInitialValue || value.isEmpty() || value.equals("-")) return;
		try {
			ClientPacketDistributor.sendToServer(new ChromaNetwork.SetHeatLampTemperature(
					menu.lamp().getBlockPos(), Integer.parseInt(value)));
		}
		catch (NumberFormatException ignored) { }
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
				imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, (imageWidth - font.width(title)) / 2, 5, 0xffffffff, false);
		graphics.text(font, Component.literal("Temperature:"), 16, 25, 0xffffffff, false);
	}
}
