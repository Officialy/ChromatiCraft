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

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import reika.chromaticraft.base.ChromaDimensionBiome;
import reika.chromaticraft.base.ChromaWorldGenerator;
import reika.chromaticraft.block.worldgen.BlockSparkle;
import reika.chromaticraft.block.worldgen.blocksparkle.BlockTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.dimension.ChunkProviderChroma;
import reika.chromaticraft.world.dimension.DimensionGenerators;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

@Deprecated
public class WorldGenSparkle extends ChromaWorldGenerator {

	public WorldGenSparkle(DimensionGenerators g, Random r, long s) {
		super(g, r, s);
	}

	@Override
	public float getGenerationChance(World world, int cx, int cz, ChromaDimensionBiome biome) {
		return 1;
	}

	@Override
	public boolean generate(World world, Random rand, int ax, int y, int az) {
		for (int n = 0; n < 32; n++) {
			int x = ax-ax%16+rand.nextInt(16);
			int z = az-az%16+rand.nextInt(16);
			y = 4+rand.nextInt(64+ChunkProviderChroma.VERTICAL_OFFSET+32);
			BlockTypes type = BlockTypes.list[rand.nextInt(BlockSparkle.BlockTypes.list.length)];
			double r1 = 8D+rand.nextDouble()*24;
			double r2 = 8D+rand.nextDouble()*24;
			double r3 = 8D+rand.nextDouble()*24;
			for (int i = -(int)r1-1; i <= r1+1; i++) {
				for (int j = -(int)r2-1; j <= r2+1; j++) {
					for (int k = -(int)r3-1; k <= r3+1; k++) {
						if (ReikaMathLibrary.isPointInsideEllipse(i, j, k, r1, r2, r3)) {
							int dx = x+i;
							int dy = y+j;
							int dz = z+k;
							Block b = world.getBlock(dx, dy, dz);
							if (b == type.getBlockProxy() || (b == Blocks.grass && type.isGround())) {
								world.setBlock(dx, dy, dz, ChromaBlocks.SPARKLE.getBlockInstance(), type.ordinal(), 3);
							}
						}
					}
				}
			}
		}
		return true;
	}

}
