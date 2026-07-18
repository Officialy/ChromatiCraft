/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world;

import net.minecraft.world.biome.BiomeGenBase;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.instantiable.io.ModLogger;
import reika.dragonapi.instantiable.worldgen.StackableBiomeDecorator;

public class DecoratorEnderForest extends StackableBiomeDecorator {

	EnderPoolGenerator gen = new EnderPoolGenerator();

	public DecoratorEnderForest() {
		super();
	}

	@Override
	protected void genDecorations(BiomeGenBase biome) {
		super.genDecorations(biome);

		int i;
		int j;
		int k;
		int i1;
		int l;

		j = chunk_X + randomGenerator.nextInt(16) + 8;
		k = chunk_Z + randomGenerator.nextInt(16) + 8;

		int arg;
		switch(ChromaOptions.ENDERPOOLS.getValue()) {
			case 1:
				arg = 12;
				break;
			case 2:
				arg = 6;
				break;
			case 3:
				arg = 3;
				break;
			default:
				arg = 6;
				break;
		}
		if (randomGenerator.nextInt(arg) == 0)
			gen.generate(currentWorld, randomGenerator, j, currentWorld.getTopSolidOrLiquidBlock(j, k), k);
	}

	@Override
	protected ModLogger getLogger() {
		return ChromatiCraft.logger;
	}


}
