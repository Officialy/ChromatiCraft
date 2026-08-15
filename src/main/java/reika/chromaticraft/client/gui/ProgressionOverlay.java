package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionDescriptions;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaSounds;

/**
 * Modern render-state implementation of V33a {@code ProgressOverlayRenderer}.
 *
 * <p>This intentionally is not a vanilla toast. ChromatiCraft's original notifications are
 * independently stacked, charcoal double-framed cards which reveal vertically at the upper-right
 * and remain visible for the configured progression duration.</p>
 */
public final class ProgressionOverlay implements GuiLayer {

	private static final ProgressionOverlay INSTANCE = new ProgressionOverlay();
	private static final net.minecraft.resources.Identifier ID =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "progression_notes");
	private static final int DURATION = Math.max(100, ChromaOptions.PROGRESSDURATION.getValue());

	private final TreeMap<Integer, Note> notes = new TreeMap<>();
	private long soundCooldownUntil;

	private ProgressionOverlay() {}

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.TITLE, ID, INSTANCE);
	}

	public static void add(ProgressStage stage) {
		INSTANCE.add(new Note(1_000_000 + stage.ordinal(),
				Component.literal(ProgressionDescriptions.title(stage)),
				Component.literal(ProgressionDescriptions.description(stage)),
				ProgressStageIconResolver.icon(stage), 0));
	}

	public static void add(ResearchLevel level) {
		INSTANCE.add(new Note(2_000_000 + level.ordinal(), level.getDisplayName(),
				Component.literal("More of the world becomes visible to you."),
				new ItemStack(ChromaItems.INFO_FRAGMENT.get()), 0));
	}

	private void add(Note note) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;
		long now = mc.level.getGameTime();
		notes.put(note.order(), new Note(note.order(), note.title(), note.description(),
				note.icon(), now + DURATION));
		if (now >= soundCooldownUntil) {
			mc.level.playLocalSound(mc.player, ChromaSounds.GAINPROGRESS.getSoundEvent(),
					ChromaSounds.GAINPROGRESS.getCategory(), 0.5F, 1F);
			soundCooldownUntil = now + 24;
		}
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || notes.isEmpty())
			return;
		long now = mc.level.getGameTime();
		Font font = mc.font;
		int y = 0;
		List<Integer> expired = new ArrayList<>();
		for (Map.Entry<Integer, Note> entry : notes.entrySet()) {
			Note note = entry.getValue();
			int remaining = (int)Math.clamp(note.expiresAt() - now, 0, DURATION);
			if (remaining <= 0) {
				expired.add(entry.getKey());
				continue;
			}
			int titleWidth = Math.max(40, font.width(note.title()));
			List<FormattedCharSequence> description = font.split(note.description(), titleWidth * 2);
			int fullHeight = 24 + (Math.max(1, description.size()) - 1) * 4;
			int age = DURATION - remaining;
			int height = remaining > DURATION - fullHeight ? age
					: remaining < fullHeight ? remaining : fullHeight;
			if (height <= 0)
				continue;
			int width = titleWidth + 28;
			int x = graphics.guiWidth() - width - 1;
			graphics.fill(x, y, x + width, y + height, 0xff444444);
			graphics.outline(x + 1, y + 1, width - 2, height - 2, 0xffcccccc);
			graphics.outline(x + 2, y + 2, width - 4, height - 4, 0xffcccccc);
			if (height == fullHeight) {
				graphics.text(font, note.title(), x + width - 4 - titleWidth, y + 4,
						0xffffffff, false);
				graphics.pose().pushMatrix();
				graphics.pose().scale(0.5F, 0.5F);
				for (int line = 0; line < description.size(); line++)
					graphics.text(font, description.get(line),
							(x + 28) * 2, (y + 12 + line * 4) * 2, 0xffffffff, false);
				graphics.pose().popMatrix();
				if (!note.icon().isEmpty())
					graphics.item(note.icon(), x + 4, y + 4);
			}
			y += height + 4;
		}
		expired.forEach(notes::remove);
	}

	private record Note(int order, Component title, Component description, ItemStack icon,
			long expiresAt) {}
}
