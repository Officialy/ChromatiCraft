/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.generators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMinable;

import reika.chromaticraft.base.ChromaDimensionBiome;
import reika.chromaticraft.base.ChromaWorldGenerator;
import reika.chromaticraft.block.dimension.blockdimensiondeco.DimDecoTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.dimension.chromadimensionmanager.Biomes;
import reika.chromaticraft.world.dimension.chromadimensionmanager.SubBiomes;
import reika.chromaticraft.world.dimension.DimensionGenerators;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

public class WorldGenFloatstone extends ChromaWorldGenerator {

	public WorldGenFloatstone(DimensionGenerators g, Random rand, long seed) {
		super(g, rand, seed);
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {

		int y2 = y+12+rand.nextInt(12);

		int n = 2+rand.nextInt(6);
		for (int i = 0; i < n; i++) {
			int s = 24+rand.nextInt(16);
			if (n >= 4)
				s *= 1-n/16D;
			int dy = ReikaRandomHelper.getRandomPlusMinus(y2, 3);
			int dx = ReikaRandomHelper.getRandomPlusMinus(x, 8);
			int dz = ReikaRandomHelper.getRandomPlusMinus(z, 8);
			new WorldGenMinable(ChromaBlocks.DIMGEN.getBlockInstance(), DimDecoTypes.FLOATSTONE.ordinal(), s, Blocks.air).generate(world, rand, dx, dy, dz);
		}

		return true;
	}

	@Override
	public float getGenerationChance(World world, int cx, int cz, ChromaDimensionBiome biome) {
		return biome == Biomes.SKYLANDS.getBiome() || biome == SubBiomes.VOIDLANDS.getBiome() ? 0.1F : 0.02F;
	}

}
