/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.world.item.Item;

/**
 * V33a {@code ItemPylonFinder}, the Pylon Compass: held, it shows the nearest known pylon of every colour on the HUD
 * ({@code PylonFinderOverlay}, reading the synced pylon cache). It has no behaviour of its own beyond that.
 *
 * <p>CHROMA-PORT: V33a also let it work from inside an Aura Pouch -- the pouch ticked its contents, and the compass,
 * seeing its tick come from the pouch's slot, stamped {@link #NBT_KEY} with the world time every 8 ticks, which the
 * overlay honours for 20 ticks. The overlay reads the stamp; the pouch ({@code ItemAuraPouch}, its
 * {@code WORKS_IN_POUCH} effect) has to land before anything writes it.
 */
public class ItemPylonFinder extends Item {

	public static final String NBT_KEY = "pylonoverlay";

	public ItemPylonFinder(Properties properties) {
		super(properties);
	}

}
