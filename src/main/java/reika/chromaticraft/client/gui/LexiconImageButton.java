package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;

/**
 * V33a's {@code CustomSoundImagedGuiButton}: the guide's buttons are cut straight out of
 * {@code buttons.png} rather than being vanilla widgets.
 *
 * <p>Upstream's {@code renderButton} is a plain {@code drawTexturedModalRect(x, y, u, v, w, h)} with
 * no hover offset — the pressed/selected look comes from the caller passing a different {@code v},
 * which is exactly how the Items/Recipes tabs swap when the mode changes. Hovering only raises the
 * tooltip, so that is all this does too.
 */
public final class LexiconImageButton extends AbstractButton {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/lexicon/buttons.png");
	private static final int SHEET = 256;

	private final int u;
	private final int v;
	private final Runnable action;
	/** How much of this button's height actually takes clicks; see {@link #clickHeight}. */
	private int clickHeight;

	public LexiconImageButton(int x, int y, int width, int height, int u, int v,
			Component tooltip, Runnable action) {
		super(x, y, width, height, tooltip);
		this.u = u;
		this.v = v;
		this.action = action;
		this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
	}

	/**
	 * V33a's Items and Recipes tabs are both 13x88 but sit only 34 pixels apart, so 54 pixels of each
	 * strip is hidden behind the other. The art is drawn at full height -- that overlap is what makes
	 * the tabs look stacked -- but a click has to go to the tab you can actually see, so the hit
	 * region is trimmed to the visible part.
	 *
	 * @param height the clickable height from this button's top edge
	 */
	public LexiconImageButton clickHeight(int height) {
		clickHeight = height;
		return this;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (!this.visible)
			return false;
		int h = clickHeight > 0 ? clickHeight : this.height;
		return mouseX >= this.getX() && mouseX < this.getX() + this.width
				&& mouseY >= this.getY() && mouseY < this.getY() + h;
	}

	@Override
	public void onPress(net.minecraft.client.input.InputWithModifiers input) {
		action.run();
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
				this.getX(), this.getY(), u, v, this.width, this.height, SHEET, SHEET);
	}
}
