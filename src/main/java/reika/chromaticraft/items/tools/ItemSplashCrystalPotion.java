package reika.chromaticraft.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;

import reika.chromaticraft.registry.CrystalElement;

/**
 * The splash form of {@link ItemCrystalPotion}, V33a's crystal potion with the splash bit set. Throwing and the
 * splash are vanilla's, reading the stack's {@code POTION_CONTENTS}.
 */
public class ItemSplashCrystalPotion extends SplashPotionItem {

	private final CrystalElement color;

	public ItemSplashCrystalPotion(CrystalElement color, Properties properties) {
		super(properties);
		this.color = color;
	}

	public CrystalElement getColor() {
		return color;
	}

	@Override
	public ItemStack getDefaultInstance() {
		return ItemCrystalPotion.create(color, true, false, false);
	}

	/** V33a named the splash form exactly as the drinkable one. */
	@Override
	public Component getName(ItemStack is) {
		return Component.translatable(this.getDescriptionId());
	}

}
