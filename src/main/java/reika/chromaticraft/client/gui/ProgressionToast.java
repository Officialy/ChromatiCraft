package reika.chromaticraft.client.gui;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionDescriptions;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.registry.ChromaItems;

/**
 * Modern presentation of V33a {@code ProgressOverlayRenderer}'s progression note.
 *
 * <p>The original slides a charcoal, double-framed card in at the top right, showing the authored
 * progression title, short description and stage icon. Minecraft's toast manager supplies the same
 * top-right slide/stack lifetime on 26.2 while this class retains ChromatiCraft's own card rather
 * than disguising progression as an advancement or a system error.
 */
public final class ProgressionToast implements Toast {

	private static final int WIDTH = 180;
	private static final int HEIGHT = 44;
	private static final long DURATION_MS = 5000;

	private final Component title;
	private final Component description;
	private final ItemStack icon;
	private final Object token;
	private Visibility visibility = Visibility.SHOW;

	public ProgressionToast(ProgressStage stage) {
		title = Component.literal(ProgressionDescriptions.title(stage));
		description = Component.literal(ProgressionDescriptions.description(stage));
		icon = ProgressStageIconResolver.icon(stage);
		token = stage;
	}

	/** V33a ResearchLevel: fragment icon and the authored generic world-reveal caption. */
	public ProgressionToast(ResearchLevel level) {
		title = level.getDisplayName();
		description = Component.literal("More of the world becomes visible to you.");
		icon = new ItemStack(ChromaItems.INFO_FRAGMENT.get());
		token = level;
	}

	@Override
	public Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		visibility = fullyVisibleForMs < DURATION_MS * manager.getNotificationDisplayTimeMultiplier()
				? Visibility.SHOW : Visibility.HIDE;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
		graphics.fill(0, 0, WIDTH, HEIGHT, 0xff444444);
		graphics.outline(1, 1, WIDTH - 2, HEIGHT - 2, 0xffcccccc);
		graphics.outline(2, 2, WIDTH - 4, HEIGHT - 4, 0xffcccccc);
		if (!icon.isEmpty())
			graphics.item(icon, 6, 6);
		graphics.text(font, title, 28, 6, 0xffffffff, false);
		List<FormattedCharSequence> lines = font.split(description, WIDTH - 34);
		for (int i = 0; i < Math.min(2, lines.size()); i++)
			graphics.text(font, lines.get(i), 28, 18 + i * 9, 0xffe0e0e0, false);
	}

	@Override
	public Object getToken() {
		return token;
	}

	@Override public int width() { return WIDTH; }
	@Override public int height() { return HEIGHT; }
}
