/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.aura;

import java.util.ArrayList;

import net.minecraft.world.World;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.aura.auramap.AuraLocation;
import reika.chromaticraft.registry.CrystalElement;

public class AuraCalculator {

	public static final AuraCalculator instance = new AuraCalculator();

	private AuraCalculator() {

	}

	private int maxDist = 100;

	public int getAuraAt(World world, int x, int y, int z, CrystalElement e) {
		int max = 0;
		ArrayList<AuraLocation> li = AuraMap.instance.getAuraSourcesWithinDOfXYZ(world, x, y, z, maxDist);
		for (int i = 0; i < li.size(); i++) {
			AuraLocation a = li.get(i);
			int gen = a.getAuraStrengthAt(e, world, x, y, z);
			if (gen > max)
				max = gen;
		}
		return max;
	}

	public ElementTagCompound getAurasAt(World world, int x, int y, int z) {
		ElementTagCompound ele = new ElementTagCompound();
		ArrayList<AuraLocation> li = AuraMap.instance.getAuraSourcesWithinDOfXYZ(world, x, y, z, maxDist);
		for (int i = 0; i < li.size(); i++) {
			AuraLocation a = li.get(i);
			ElementTagCompound gen = a.getAuraStrengthsAt(world, x, y, z);
			ele.maximizeWith(gen);
		}
		return ele;
	}

}
