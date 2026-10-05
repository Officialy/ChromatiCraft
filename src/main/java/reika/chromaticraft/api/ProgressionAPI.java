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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;

import java.util.HashSet;

public class ProgressionAPI {

	public static final ProgressionAPI instance = new ProgressionAPI();

	public ResearchRegistry researchManager;
	public ProgressRegistry progressManager;

	private ProgressionAPI() {

	}

	public interface ResearchRegistry extends ProgressManager {

		boolean lexiconHasFragment(ItemStack book, String key);

		String getResearchLevelForPlayer(Player ep);

	}

	public interface ProgressRegistry extends ProgressManager {

		boolean canPlayerStepTo(Player ep, String key);

		boolean playerDiscoveredElement(Player ep, CrystalElementProxy e);

	}

	public interface ProgressManager {

		boolean playerHasResearch(Player ep, String key);

		HashSet<String> getPrerequisites(String key);

		HashSet<String> getAllResearches();

	}

}
