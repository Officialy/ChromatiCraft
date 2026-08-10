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

	public LexiconImageButton(int x, int y, int width, int height, int u, int v,
			Component tooltip, Runnable action) {
		super(x, y, width, height, tooltip);
		this.u = u;
		this.v = v;
		this.action = action;
		this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
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
