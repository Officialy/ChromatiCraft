/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.biome;

import reika.chromaticraft.world.dimension.chromadimensionmanager.Biomes;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class MonumentBiome extends StructureBiome {

	private final NoiseGeneratorBase colorBlend = new SimplexNoiseGenerator(System.currentTimeMillis()).setFrequency(1/32D);

	public MonumentBiome(int id, String n, Biomes t) {
		super(id, n, t);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBiomeFoliageColor(int x, int y, int z) {
		int c1 = this.getBiomeGrassColor(x, y, z);
		int c2 = super.getBiomeFoliageColor(x, y, z);
		return ReikaColorAPI.mixColors(c1, c2, this.getBlendFactor(x, z));
	}

	@Override
	protected int getHighlightColor(int x, int y, int z, int c) {
		int c1 = 0xff77cc;//ReikaColorAPI.multiplyChannels(c, 2.5F, 1F, 1.5F);
		int c2 = super.getHighlightColor(x, y, z, c);
		return ReikaColorAPI.mixColors(c1, c2, this.getBlendFactor(x, z));
	}

	@Override
	protected int getBaseColor(int x, int y, int z) {
		int c1 = 0x77ccff;//ChromatiCraft.rainbowforest.getBiomeGrassColor(x, y, z);
		int c2 = super.getBaseColor(x, y, z);
		return ReikaColorAPI.mixColors(c1, c2, this.getBlendFactor(x, z));
	}

	private float getBlendFactor(int x, int z) {
		return (float)Math.pow(ReikaMathLibrary.normalizeToBounds(colorBlend.getValue(x, z), 0, 1), 1.5);
	}

}
