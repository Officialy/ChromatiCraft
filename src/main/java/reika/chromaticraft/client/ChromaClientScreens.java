package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.client.gui.ScreenChromicLexicon;

/** Client-only screen construction kept out of common item class descriptors. */
public final class ChromaClientScreens {

	private ChromaClientScreens() {}

	public static void openLexicon(Player player, ItemStack book, boolean fragmentInventory) {
		Minecraft.getInstance().gui.setScreen(new ScreenChromicLexicon(player, book, fragmentInventory));
	}

	/** Opens the Lexicon straight to a page's recipes, as a recipe viewer's "open in Lexicon" button does. */
	public static void openLexiconAt(Player player, ItemStack book,
			reika.chromaticraft.magic.progression.LexiconCatalog.Entry page) {
		ScreenChromicLexicon screen = new ScreenChromicLexicon(player, book, false);
		screen.openAtOnInit(page, true);
		Minecraft.getInstance().gui.setScreen(screen);
	}

}
