package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.client.gui.ScreenChromicLexicon;
import reika.chromaticraft.client.gui.ScreenFragmentSelection;

/** Client-only screen construction kept out of common item class descriptors. */
public final class ChromaClientScreens {

	private ChromaClientScreens() {}

	public static void openLexicon(Player player, ItemStack book, boolean fragmentInventory) {
		Minecraft.getInstance().gui.setScreen(new ScreenChromicLexicon(player, book, fragmentInventory));
	}

	public static void openFragmentSelection(Player player, ItemStack fragment) {
		Minecraft.getInstance().gui.setScreen(new ScreenFragmentSelection(player, fragment));
	}
}
