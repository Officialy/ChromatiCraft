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
	/** Last syntactically valid editor value; 26.2 removed EditBox#setFilter. */
	private String lastValidInput = "";
	/** Last value delivered by the menu DataSlot, not the locally typed value. */
	private int lastSyncedTemperature;
	/** Completed numeric edit waiting for the short typing debounce to expire. */
	private Integer pendingTemperature;
	private int sendDelay;
	private boolean locallyEditing;
	private static final int EDIT_DEBOUNCE_TICKS = 6;

	public ScreenHeatLamp(MenuHeatLamp menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 48);
	}

	@Override
	protected void init() {
		super.init();
		settingInitialValue = true;
		input = new EditBox(font, leftPos + 88, topPos + 21, 60, 16, Component.literal("Temperature"));
		input.setMaxLength(4);
		lastSyncedTemperature = menu.temperature();
		lastValidInput = Integer.toString(lastSyncedTemperature);
		input.setValue(lastValidInput);
		input.setResponder(this::temperatureChanged);
		addRenderableWidget(input);
		settingInitialValue = false;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (sendDelay > 0 && --sendDelay == 0)
			flushTemperature();
		int synced = menu.temperature();
		if (synced != lastSyncedTemperature && !locallyEditing) {
			lastSyncedTemperature = synced;
			// The client-side block entity still contains its construction default when the screen
			// opens. The authoritative DataSlot arrives just afterwards, so mirror every newly
			// delivered value into the editor without echoing it back as another request.
			settingInitialValue = true;
			lastValidInput = Integer.toString(synced);
			input.setValue(lastValidInput);
			settingInitialValue = false;
		}
	}

	private void temperatureChanged(String value) {
		if (settingInitialValue) return;
		if (!(value.isEmpty() || value.equals("-") || value.matches("-?\\d+"))) {
			// EditBox#setFilter was removed in 26.2. Restore the last accepted value from the
			// responder instead, covering typed and pasted text through the one mutation path.
			settingInitialValue = true;
			input.setValue(lastValidInput);
			settingInitialValue = false;
			return;
		}
		lastValidInput = value;
		locallyEditing = true;
		pendingTemperature = null;
		sendDelay = 0;
		if (value.isEmpty() || value.equals("-")) return;
		try {
			pendingTemperature = Integer.parseInt(value);
			sendDelay = EDIT_DEBOUNCE_TICKS;
		}
		catch (NumberFormatException ignored) { }
	}

	private void flushTemperature() {
		if (pendingTemperature == null)
			return;
		ClientPacketDistributor.sendToServer(new ChromaNetwork.SetHeatLampTemperature(
				menu.lamp().getBlockPos(), pendingTemperature));
		pendingTemperature = null;
		locallyEditing = false;
	}

	@Override
	public void onClose() {
		flushTemperature();
		super.onClose();
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
