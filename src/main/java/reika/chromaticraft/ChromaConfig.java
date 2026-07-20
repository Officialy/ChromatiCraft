/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft;

import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.instantiable.io.ControlledConfig;
import reika.dragonapi.interfaces.configuration.ConfigList;
import reika.dragonapi.interfaces.registry.IDRegistry;

/**
 * ChromatiCraft's config, built on DragonAPI's {@link ControlledConfig} (the pattern GeoStrata's
 * {@code GeoConfig} uses) so {@link reika.chromaticraft.registry.ChromaOptions} can read typed values
 * via {@code getControl(ordinal)}.
 *
 * <p>Port-in-progress: the 1.7.10 original also registered additional options and helpers for deferred
 * subsystems — per-element/per-tree vanilla-dye chances ({@code getVanillaDyeChance}, dye trees), the
 * superbuild keybind ({@code getSuperbuildKey}, ability GUI), guardian-stone / miner-block exception
 * lists, and the structure dimension blacklist (worldgen). None are used by the crystal-network or
 * casting-table path, so they are re-added as those subsystems port. Block/item ID assignment is
 * dropped (modern MC assigns dynamically), so the {@code IDRegistry[]} is {@code null} as in GeoStrata.
 */
public class ChromaConfig extends ControlledConfig {

	public ChromaConfig(DragonAPIMod mod, ConfigList[] option, IDRegistry[] id) {
		super(mod, option, id);
	}
}
