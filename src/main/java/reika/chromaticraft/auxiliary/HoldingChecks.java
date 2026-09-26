package reika.chromaticraft.auxiliary;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTiles;

/**
 * V33a {@code HoldingChecks}: whether a player's held item is one of the tools and blocks that reveal hidden parts of
 * the mod -- the Manipulator (network paths, trails), a Focus Crystal, a Power Crystal, a repeater, or a crystal rune --
 * with a client-side fade in and out for overlays drawn while one is held. The placer items V33a checked by metadata are
 * each tile's own block item here.
 *
 * <p>CHROMA-PORT: V33a also counted a Thaumcraft wand with the Manipulator focus as the Manipulator; DELEGATE (the
 * Automator's item) matches nothing until the Automator is ported.
 */
public enum HoldingChecks {

	MANIPULATOR(),
	FOCUSCRYSTAL(),
	DELEGATE,
	POWERCRYS,
	RUNE,
	REPEATER();

	private float fade;
	private long lastUpdate;

	public boolean isClientHolding() {
		return this.isHolding(Minecraft.getInstance().player);
	}

	public boolean isHolding(Player ep) {
		return ep != null && this.match(ep.getMainHandItem());
	}

	public float getFade() {
		Minecraft mc = Minecraft.getInstance();
		long t = mc.level != null ? mc.level.getGameTime() : 0;
		if (t != lastUpdate) {
			lastUpdate = t;
			if (this.isClientHolding()) {
				fade = Math.min(1, fade+0.125F);
			}
			else {
				fade = Math.max(0, fade-0.03125F);
			}
		}
		return fade;
	}

	private boolean match(ItemStack is) {
		if (is == null || is.isEmpty())
			return false;
		ChromaTiles tile = ChromaTiles.getTileFromItem(is);
		switch(this) {
			case MANIPULATOR:
				return is.is(ChromaItems.MANIPULATOR.get());
			case FOCUSCRYSTAL:
				return tile == ChromaTiles.FOCUSCRYSTAL;
			case DELEGATE:
				return false;
			case POWERCRYS:
				return tile == ChromaTiles.CRYSTAL;
			case REPEATER:
				return tile != null && tile.isRepeater();
			case RUNE:
				return is.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BlockCrystalRune;
			default:
				return false;
		}
	}

}
