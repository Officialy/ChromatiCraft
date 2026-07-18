/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.ChromaDimensionBiome;
import reika.chromaticraft.base.ChromaWorldGenerator;
import reika.chromaticraft.world.dimension.chromadimensionmanager.Biomes;
import reika.chromaticraft.world.dimension.chromadimensionmanager.SubBiomes;
import reika.chromaticraft.world.dimension.generators.WorldGenAurorae;
import reika.chromaticraft.world.dimension.generators.WorldGenChromaMeteor;
import reika.chromaticraft.world.dimension.generators.WorldGenChunkloaderBlocks;
import reika.chromaticraft.world.dimension.generators.WorldGenCrystalPit;
import reika.chromaticraft.world.dimension.generators.WorldGenCrystalShrub;
import reika.chromaticraft.world.dimension.generators.WorldGenCrystalTree;
import reika.chromaticraft.world.dimension.generators.WorldGenFireJet;
import reika.chromaticraft.world.dimension.generators.WorldGenFissure;
import reika.chromaticraft.world.dimension.generators.WorldGenFloatstone;
import reika.chromaticraft.world.dimension.generators.WorldGenGlassCliffs;
import reika.chromaticraft.world.dimension.generators.WorldGenGlowCave;
import reika.chromaticraft.world.dimension.generators.WorldGenGlowingCracks;
import reika.chromaticraft.world.dimension.generators.WorldGenIslandArch;
import reika.chromaticraft.world.dimension.generators.WorldGenLightedShrub;
import reika.chromaticraft.world.dimension.generators.WorldGenLightedTree;
import reika.chromaticraft.world.dimension.generators.WorldGenMiasma;
import reika.chromaticraft.world.dimension.generators.WorldGenMiniAltar;
import reika.chromaticraft.world.dimension.generators.WorldGenMoonPool;
import reika.chromaticraft.world.dimension.generators.WorldGenTerrainBlob;
import reika.chromaticraft.world.dimension.generators.WorldGenTerrainCrystal;
import reika.chromaticraft.world.dimension.generators.WorldGenTreeCluster;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.libraries.java.ReikaRandomHelper;


public enum DimensionGenerators {

	RIFT(WorldGenFissure.class, 					GeneratorType.TERRAIN, 		GeneratorTheme.ENERGY,			Integer.MIN_VALUE),
	METEOR(WorldGenChromaMeteor.class, 				GeneratorType.FEATURE, 		GeneratorTheme.GEOHISTORICAL,	Integer.MIN_VALUE),
	GEODE(WorldGenCrystalPit.class, 				GeneratorType.FEATURE, 		GeneratorTheme.CRYSTAL,			-100),
	JETS(WorldGenFireJet.class, 					GeneratorType.TERRAIN, 		GeneratorTheme.ENERGY,			0),
	FLOATSTONE(WorldGenFloatstone.class, 			GeneratorType.TERRAIN, 		GeneratorTheme.SKYFEATURE,		100),
	MIASMA(WorldGenMiasma.class, 					GeneratorType.FEATURE, 		GeneratorTheme.SKYFEATURE,		0),
	TREES(WorldGenLightedTree.class, 				GeneratorType.FEATURE, 		GeneratorTheme.FOLIAGE,			-50),
	FORESTS(WorldGenTreeCluster.class, 				GeneratorType.FEATURE, 		GeneratorTheme.FOLIAGE,			-50),
	MOONPOOL(WorldGenMoonPool.class, 				GeneratorType.STRUCTURE, 	GeneratorTheme.OCEANIC,			0),
	TERRAINCRYSTAL(WorldGenTerrainCrystal.class, 	GeneratorType.TERRAIN, 		GeneratorTheme.SKYFEATURE,		Integer.MAX_VALUE),
	ALTAR(WorldGenMiniAltar.class, 					GeneratorType.STRUCTURE, 	GeneratorTheme.PRECURSORS,		-500),
	//MOUNTAIN(WorldGenCrystalMountain.class,			GeneratorType.TERRAIN,		GeneratorTheme.CRYSTAL,			Integer.MIN_VALUE),
	CRYSTALTREE(WorldGenCrystalTree.class,			GeneratorType.FEATURE,		GeneratorTheme.CRYSTAL,			-100),
	GLOWBUSH(WorldGenLightedShrub.class,			GeneratorType.FEATURE,		GeneratorTheme.FOLIAGE,			500),
	CRYSBUSH(WorldGenCrystalShrub.class,			GeneratorType.FEATURE,		GeneratorTheme.FOLIAGE,			500),
	AURORA(WorldGenAurorae.class,					GeneratorType.FEATURE,		GeneratorTheme.SKYFEATURE,		Integer.MAX_VALUE),
	//CANYON(WorldGenSkylandCanyons.class,			GeneratorType.TERRAIN,		GeneratorTheme.GEOHISTORICAL,	Integer.MIN_VALUE);
	ARCH(WorldGenIslandArch.class,					GeneratorType.TERRAIN,		GeneratorTheme.OCEANIC,			Integer.MIN_VALUE),
	BLOBS(WorldGenTerrainBlob.class,				GeneratorType.TERRAIN,		GeneratorTheme.OCEANIC,			Integer.MIN_VALUE),
	GLASSCLIFFS(WorldGenGlassCliffs.class,			GeneratorType.FEATURE,		GeneratorTheme.GEOHISTORICAL,	Integer.MIN_VALUE),
	CRACKS(WorldGenGlowingCracks.class,				GeneratorType.FEATURE,		GeneratorTheme.ENERGY,			Integer.MAX_VALUE),
	CHUNKLOADER(WorldGenChunkloaderBlocks.class,	GeneratorType.FEATURE,		GeneratorTheme.PRECURSORS,		Integer.MAX_VALUE),
	GLOWCAVE(WorldGenGlowCave.class,				GeneratorType.TERRAIN,		GeneratorTheme.GEOHISTORICAL,	Integer.MIN_VALUE),
	;

	private final Class genClass;
	public final GeneratorType type;
	public final GeneratorTheme theme;
	public final int genTime;

	private ChromaWorldGenerator generator;

	public static final DimensionGenerators[] generators = values();

	private DimensionGenerators(Class<? extends ChromaWorldGenerator> c, GeneratorType t, GeneratorTheme h, int p) {
		genClass = c;
		type = t;
		theme = h;
		genTime = p;
	}

	public ChromaWorldGenerator getGenerator(Random rand, long seed) {
		if (generator == null) {
			try {
				Constructor<ChromaWorldGenerator> c = genClass.getConstructor(DimensionGenerators.class, Random.class, long.class);
				generator = c.newInstance(this, rand, seed);
			}
			catch (Exception e) {
				throw new RegistrationException(ChromatiCraft.instance, "Could not create generator for dimension generator "+this, e);
			}
		}
		return generator;
	}

	public boolean generateIn(ChromaDimensionBiome b) {
		if (this == CHUNKLOADER)
			return b.biomeType == Biomes.STRUCTURE || b.biomeType == Biomes.MONUMENT;
		if (b.biomeType == Biomes.STRUCTURE || b.biomeType == Biomes.MONUMENT) {
			return ReikaRandomHelper.doWithChance(25) && this.generateIn(Biomes.CENTER.getBiome()) && this.canGenerateInStructureBiome();
		}
		if (b.biomeType == Biomes.CENTER)
			return !this.isDedicatedBiomeOnly();
		if (theme == GeneratorTheme.SKYFEATURE)
			return b.biomeType == Biomes.SKYLANDS;
		//if (this == CANYON)
		//	return b.biomeType == Biomes.SKYLANDS;
		if (this == FORESTS)
			return b.biomeType == Biomes.FOREST;
		if (this == TREES || this == GLOWBUSH)
			return b.biomeType == Biomes.FOREST || b.biomeType == Biomes.PLAINS || b.getExactType() == Biomes.ISLANDS || b.biomeType == Biomes.SPARKLE || b.biomeType == Biomes.GLOWCRACKS;
		switch(this) {
			case ALTAR:
				return b.getExactType().isReasonablyFlat();
			case CRYSBUSH:
			case CRYSTALTREE:
				return b == SubBiomes.CRYSFOREST.getBiome();
			case GEODE:
				return b == Biomes.PLAINS.getBiome();
			case JETS:
				return true;
			case METEOR:
				return !b.getExactType().isWaterBiome() && b.getExactType().isReasonablyFlat() && b.biomeType != Biomes.GLOWCRACKS;
			case MIASMA:
				return b.biomeType == Biomes.PLAINS;
			case MOONPOOL:
				return b.biomeType == Biomes.ISLANDS;
				//case MOUNTAIN:
				//	return b == SubBiomes.MOUNTAINS.getBiome();
			case RIFT:
				return b == Biomes.PLAINS.getBiome() || b.biomeType == Biomes.GLOWCRACKS;
			case ARCH:
				return  b == Biomes.ISLANDS.getBiome();
			case BLOBS:
				return b == SubBiomes.DEEPOCEAN.getBiome();
			case GLASSCLIFFS:
				return b == Biomes.PLAINS.getBiome();
				//case SPARKLE:
				//	return b == Biomes.SPARKLE.getBiome();
			case CRACKS:
				return b.getExactType().isReasonablyFlat() && !b.getExactType().isWaterBiome();
			case GLOWCAVE:
				return b.biomeType == Biomes.CENTER || b.biomeType == Biomes.FOREST || b.getExactType() == Biomes.PLAINS;
			default:
				return true;
		}
	}

	private boolean canGenerateInStructureBiome() {
		return true;
	}

	private boolean isDedicatedBiomeOnly() {
		switch(this) {
			case AURORA:
			case CRYSBUSH:
			case CRYSTALTREE:
			case METEOR:
			case MOONPOOL:
			case TERRAINCRYSTAL:
			case GLOWBUSH:
			case ARCH:
			case BLOBS:
			case MIASMA:
			case GLASSCLIFFS:
				//case SPARKLE:
				return true;
			default:
				return false;
		}
	}

	public static ArrayList<ChromaWorldGenerator> getSortedList(Random rand, long seed) {
		ArrayList<ChromaWorldGenerator> ret = new ArrayList();
		ArrayList<DimensionGenerators> li = new ArrayList();
		for (int i = 0; i < DimensionGenerators.generators.length; i++) {
			DimensionGenerators gen = DimensionGenerators.generators[i];
			li.add(gen);
		}
		Collections.sort(li, generationSorter);
		for (DimensionGenerators g : li) {
			ret.add(g.getGenerator(rand, seed));
		}
		return ret;
	}

	public static enum GeneratorType {

		TERRAIN(),
		STRUCTURE(),
		FEATURE();

	}

	public static enum GeneratorTheme {

		SKYFEATURE(),
		CRYSTAL(),
		PRECURSORS(),
		ENERGY(),
		FOLIAGE(),
		GEOHISTORICAL(),
		OCEANIC();

	}

	private static final Comparator<DimensionGenerators> generationSorter = new GeneratorSorter();

	private static class GeneratorSorter implements Comparator<DimensionGenerators> {

		private GeneratorSorter() {

		}

		@Override
		public int compare(DimensionGenerators o1, DimensionGenerators o2) {
			return Integer.compare(o1.genTime, o2.genTime);
		}

	}

}
