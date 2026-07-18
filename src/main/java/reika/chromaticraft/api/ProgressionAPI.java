/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api;

import java.util.HashSet;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;

public class ProgressionAPI {

	public static final ProgressionAPI instance = new ProgressionAPI();

	public ResearchRegistry researchManager;
	public ProgressRegistry progressManager;

	private ProgressionAPI() {

	}

	public static interface ResearchRegistry extends ProgressManager {

		public boolean lexiconHasFragment(ItemStack book, String key);

		public String getResearchLevelForPlayer(Player ep);

	}

	public static interface ProgressRegistry extends ProgressManager {

		public boolean canPlayerStepTo(Player ep, String key);

		public boolean playerDiscoveredElement(Player ep, CrystalElementProxy e);

	}

	public static interface ProgressManager {

		public boolean playerHasResearch(Player ep, String key);

		public HashSet<String> getPrerequisites(String key);

		public HashSet<String> getAllResearches();

	}

}
