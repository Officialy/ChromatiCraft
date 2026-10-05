/*
package reika.chromaticraft.world;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.ChromaGameTests;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.chromaticraft.world.biome.ChromaBiomeWeather;
import reika.dragonapi.instantiable.math.JavaRandomSource;
import reika.dragonapi.instantiable.io.ControlledConfig;
import reika.dragonapi.libraries.level.LegacyOreVeins;
import reika.electricraft.registry.ElectriOres;
import reika.electricraft.world.BasicElectriOreGenerator;
import reika.geostrata.GeoStrata;
import reika.geostrata.level.GeoBiomePainter;
import reika.geostrata.level.GeoBiomes;
import reika.geostrata.level.GeoOreConversion;
import reika.geostrata.level.generators.ArcticSpiresGenerator;
import reika.geostrata.level.generators.GlowCrystalGenerator;
import reika.geostrata.level.generators.GlowingVineGenerator;
import reika.geostrata.registry.GeoOptions;
import reika.geostrata.registry.OreTypes;
import reika.geostrata.registry.RockShapes;
import reika.geostrata.registry.RockTypes;
import reika.reactorcraft.registry.ReactorOreType;
import reika.reactorcraft.world.ReactorOreFeature;
import reika.reactorcraft.world.ReactorOreSettings;
import reika.rotarycraft.ExtraIronGenerator;
import reika.rotarycraft.registry.ExtractOres;
import reika.rotarycraft.registry.ModExtractOres;

*/
/** Contracts for the restored source distributions, actual weather ticks and completed-chunk painting. *//*

public final class OverworldGenerationFixTests {
    public static void register(RegisterGameTestsEvent event,Holder<TestEnvironmentDefinition<?>> env) {
        ChromaGameTests.register(event,env,"overworld_fixes_weather",100,OverworldGenerationFixTests::weather);
        ChromaGameTests.register(event,env,"overworld_fixes_registry_copper",30,OverworldGenerationFixTests::registry);
        ChromaGameTests.register(event,env,"overworld_fixes_ore_placement",30,OverworldGenerationFixTests::ores);
        ChromaGameTests.register(event,env,"overworld_fixes_ore_conversion",30,OverworldGenerationFixTests::conversion);
        ChromaGameTests.register(event,env,"overworld_fixes_biome_paint",30,OverworldGenerationFixTests::painting);
        ChromaGameTests.register(event,env,"overworld_fixes_admission",30,OverworldGenerationFixTests::admission);
    }

    private static ChunkPos site(GameTestHelper h,int offset) {
        var base=ChunkPos.containing(h.absolutePos(BlockPos.ZERO));
        return new ChunkPos(base.x()+offset,base.z()+256);
    }
    private static void biome(ServerLevel level,ChunkPos chunk,ResourceKey<Biome> key) {
        var holder=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
        level.getChunk(chunk.x(),chunk.z()).fillBiomesFromNoise((x,y,z)->holder);
    }
    private static Feature feature(ServerLevel level,String namespace,String path) {
        return level.registryAccess().lookupOrThrow(Registries.FEATURE)
                .getOrThrow(ResourceKey.create(Registries.FEATURE,Identifier.fromNamespaceAndPath(namespace,path))).value();
    }

    private static void weather(GameTestHelper h) {
        var level=h.getLevel();var chunk=site(h,64);biome(level,chunk,ChromaBiomes.ENDER_FOREST);
        var pos=new BlockPos(chunk.getMinBlockX()+8,100,chunk.getMinBlockZ()+8);
        for(var at:BlockPos.betweenClosed(pos.offset(-8,-1,-8),pos.offset(7,24,7)))
            level.setBlock(at,(at.getY()==99?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        var ender=level.getBiome(pos).value();
        h.assertTrue(ender.getPrecipitationAt(pos,level.getSeaLevel())==Biome.Precipitation.NONE,"Ender Forest stays dry");
        boolean oldRain=level.isRaining(),oldThunder=level.isThundering();
        level.weather.setWeatherParameters(0,6000,true,true);level.setRainLevel(1);level.setThunderLevel(1);
        h.assertTrue(!level.isRainingAt(pos) && ChromaBiomeWeather.canSpawnLightning(level,pos),"storm lightning must be eligible despite no rainfall");
        level.setBlock(pos.above(3),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(!ChromaBiomeWeather.canSpawnLightning(level,pos),"lightning cannot pass a roof");
        level.setBlock(pos.above(3),Blocks.AIR.defaultBlockState(),2);
        long seed=0;while(RandomSource.create(seed).nextInt(100000)!=0) seed++;
        var box=new AABB(chunk.getMinBlockX(),0,chunk.getMinBlockZ(),chunk.getMinBlockX()+16,320,chunk.getMinBlockZ()+16);
        int before=level.getEntitiesOfClass(LightningBolt.class,box).size();
        level.random.setSeed(seed);level.tickThunder(level.getChunk(chunk.x(),chunk.z()));
        h.assertTrue(level.getEntitiesOfClass(LightningBolt.class,box).size()>before,"actual tickThunder must spawn a bolt in the dry Ender Forest");
        for(var bolt:level.getEntitiesOfClass(LightningBolt.class,box)) bolt.discard();
        level.setWeatherParameters(6000,0,false,false);level.setRainLevel(0);level.setThunderLevel(0);
        h.assertTrue(!ChromaBiomeWeather.canSpawnLightning(level,pos),"clear weather cannot spawn Ender lightning");
        var rainbow=site(h,68);biome(level,rainbow,ChromaBiomes.RAINBOW_FOREST);
        var sample=new BlockPos(rainbow.getMinBlockX()+8,64,rainbow.getMinBlockZ()+8);
        var b=level.getBiome(sample).value();
        h.assertTrue(b.getPrecipitationAt(sample,63)==Biome.Precipitation.RAIN,"low Rainbow Forest stays rainy");
        h.assertTrue(b.getPrecipitationAt(sample.atY(192),63)==Biome.Precipitation.SNOW,"Rainbow Forest snows above 191");
        h.assertTrue(Math.abs(ChromaBiomeWeather.rainbowTemperature(1,192)-(.15F+.025F*(192-255)/64F))<.00001,"original upper cooling ramp");
        h.assertTrue(ChromaBiomeWeather.rainbowTemperature(1,255)==.125F,"original temperature floor at the height cap");
        var snow=sample.atY(250);var water=snow.offset(3,0,0);
        for(var at:BlockPos.betweenClosed(snow.offset(-2,-2,-2),snow.offset(5,20,2)))
            level.setBlock(at,(at.getY()==249?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        level.setBlock(water,Blocks.WATER.defaultBlockState(),2);
        level.setWeatherParameters(0,6000,true,false);level.setRainLevel(1);
        h.runAfterDelay(10,()-> {
            try {
                level.tickPrecipitation(snow);level.tickPrecipitation(water);
                h.assertTrue(level.getBlockState(snow).is(Blocks.SNOW),"actual precipitation tick lays high-altitude snow");
                h.assertTrue(level.getBlockState(water).is(Blocks.ICE),"actual precipitation tick freezes high-altitude water");
                h.succeed();
            } finally {
                level.setWeatherParameters(oldRain?0:6000,oldRain?6000:0,oldRain,oldThunder);
                level.setRainLevel(oldRain?1:0);level.setThunderLevel(oldThunder?1:0);
            }
        });
    }

    private static void registry(GameTestHelper h) {
        for(var ore:ElectriOres.oreList) if(ore!=ElectriOres.COPPER) {
            var f=(BasicElectriOreGenerator)feature(h.getLevel(),"electricraft",ore.name().toLowerCase(java.util.Locale.ROOT)+"_ore");
            h.assertTrue(f.count()==ore.perChunk && f.size()==ore.veinSize && f.minY()==ore.minY && f.maxY()==ore.maxY,"original Electri distribution for "+ore);
        }
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("electricraft","copper_ingot")),"no duplicate Electri copper ingot");
        h.assertTrue(!BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath("electricraft","copper_ore")),"no duplicate Electri copper ore");
        h.assertTrue(!ElectriOres.COPPER.isOreEnabled() && ElectriOres.COPPER.getProduct().is(Items.COPPER_INGOT),"Electri uses vanilla copper");
        h.assertTrue(ExtractOres.COPPER.getSmeltProduct().is(Items.COPPER_INGOT) && ModExtractOres.COPPER.getSmeltProduct().is(Items.COPPER_INGOT)
                && ModExtractOres.NETHERCOPPER.getSmeltProduct().is(Items.COPPER_INGOT),"all Rotary copper chains output vanilla copper");
        var iron=(ExtraIronGenerator)feature(h.getLevel(),"rotarycraft","extra_iron");
        h.assertTrue(iron.passes(1)==0 && iron.passes(1.5F)==5 && iron.passes(2)==10 && iron.minY()==4 && iron.maxY()==67,"extra iron factor and inclusive original height band");
        var thorium=(ReactorOreFeature)feature(h.getLevel(),"reactorcraft",ReactorOreType.THORIUM.featureName());
        h.assertTrue(thorium.config().dimType()==1 && thorium.config().minY()==0 && thorium.config().maxY()==32,"thorium belongs in Nether netherrack at original heights");
        var extra=net.minecraft.tags.TagKey.create(Registries.BIOME,Identifier.fromNamespaceAndPath("reactorcraft","extra_pitchblende_biomes"));
        var biomes=h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        h.assertTrue(biomes.getOrThrow(Biomes.MUSHROOM_FIELDS).is(extra) && biomes.getOrThrow(ChromaBiomes.RAINBOW_FOREST).is(extra),"mushroom islands and Rainbow Forest admit pitchblende");
        h.assertTrue(ReactorOreSettings.passes(6,1,1,false)==6 && ReactorOreSettings.passes(6,1.5F,3,false)==27
                && ReactorOreSettings.passes(6,1,1,true)==1,"density, discrete compensation and rainbow fluorite truncation");
        h.succeed();
    }

    private static void ores(GameTestHelper h) {
        var level=h.getLevel();var center=new BlockPos(site(h,72).getMinBlockX()+8,24,site(h,72).getMinBlockZ()+8);
        for(var p:BlockPos.betweenClosed(center.offset(-12,-5,-12),center.offset(12,5,12))) level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(LegacyOreVeins.place(level,RandomSource.create(47),ElectriOres.TIN.getBlock().defaultBlockState(),8,0,center.getX(),center.getY(),center.getZ()),"original ellipsoid must actually place ore");
        h.assertTrue(!LegacyOreVeins.place(level,RandomSource.create(47),ElectriOres.TIN.getBlock().defaultBlockState(),8,1,center.getX(),center.getY(),center.getZ()),"Nether ore cannot replace stone");
        h.assertTrue(LegacyOreVeins.hasEquivalent(ElectriOres.SILVER.getBlock(),"silver",ElectriOres.SILVER.getDeepslateBlock()),"Reactor silver counts as an equivalent, Geo camouflage alone does not");
        h.succeed();
    }

    private static Object[] controls(ControlledConfig config) {
        try { var f=ControlledConfig.class.getDeclaredField("controls");f.setAccessible(true);return (Object[])f.get(config); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void conversion(GameTestHelper h) {
        var controls=controls(GeoStrata.config);int index=GeoOptions.GEOORE.ordinal();Object old=controls[index];
        var pos=h.absolutePos(new BlockPos(4,2,4));var level=h.getLevel();
        try {
            controls[index]=0;level.setBlock(pos,Blocks.COPPER_ORE.defaultBlockState(),2);
            h.assertTrue(!GeoOreConversion.convert(level,pos,RockTypes.GRANITE) && level.getBlockState(pos).is(Blocks.COPPER_ORE),"mode 0 leaves ores alone");
            controls[index]=1;
            h.assertTrue(GeoOreConversion.convert(level,pos,RockTypes.GRANITE) && level.getBlockState(pos).is(GeoOreConversion.host(RockTypes.GRANITE,OreTypes.COPPER)),"mode 1 creates the matching rock-hosted copper");
            h.assertTrue(ExtractOres.getByOreBlock(new net.minecraft.world.item.ItemStack(level.getBlockState(pos).getBlock()))==ExtractOres.COPPER,"converted ore stays processable by RotaryCraft");
            level.setBlock(pos,Blocks.IRON_ORE.defaultBlockState(),2);controls[index]=-1;
            h.assertTrue(GeoOreConversion.convert(level,pos,RockTypes.GRANITE) && level.getBlockState(pos).is(RockTypes.GRANITE.getID(RockShapes.SMOOTH)),"mode -1 replaces ore with host rock");
            controls[index]=2;level.setBlock(pos,Blocks.DIAMOND_ORE.defaultBlockState(),2);level.setBlock(pos.east(),RockTypes.GRANITE.getID(RockShapes.SMOOTH).defaultBlockState(),2);
            GeoOreConversion.convertExposedToRock(level,ChunkPos.containing(pos));
            h.assertTrue(level.getBlockState(pos).is(GeoOreConversion.host(RockTypes.GRANITE,OreTypes.DIAMOND)),"mode 2 converts exposed deposits next to rock");
        } finally { controls[index]=old; }
        h.succeed();
    }

    private static void painting(GameTestHelper h) {
        var level=h.getLevel();var chunk=site(h,76);biome(level,chunk,Biomes.PLAINS);
        var pos=new BlockPos(chunk.getMinBlockX()+3,64,chunk.getMinBlockZ()+3);
        GeoBiomePainter.paint(level,Set.of(GeoBiomePainter.cell(pos.getX(),pos.getZ())),GeoBiomes.KELP_FOREST);
        h.assertTrue(level.getBiome(pos).is(GeoBiomes.KELP_FOREST) && level.getBiome(pos.atY(200)).is(GeoBiomes.KELP_FOREST),"successful grove paints the biome column");
        h.assertTrue(level.getBiome(pos.offset(8,0,8)).is(Biomes.PLAINS),"painting preserves other columns");
        var reference=new java.util.Random(719);var adapter=new JavaRandomSource(719);
        for(int i=0;i<100;i++) h.assertTrue(reference.nextInt(129)==adapter.nextInt(129) && reference.nextDouble()==adapter.nextDouble(),"shared spire helper random stream must retain Java draws");
        h.succeed();
    }

    private static void admission(GameTestHelper h) {
        var set=Set.of(new BlockPos(0,60,0));
        h.assertTrue(!ArcticSpiresGenerator.separated(set,8,8) && ArcticSpiresGenerator.separated(set,64,0),"wide scatter admits a second spire while preserving 24-block spacing");
        int dense=0,sparse=0,treeless=0;
        for(int i=0;i<300;i++) {
            if(GlowCrystalGenerator.admitted(10,true,RandomSource.create(i))) dense++;
            if(GlowCrystalGenerator.admitted(2,false,RandomSource.create(i))) sparse++;
            if(GlowCrystalGenerator.admitted(0,false,RandomSource.create(i))) treeless++;
        }
        h.assertTrue(dense==300 && sparse>150 && sparse<250 && treeless>50 && treeless<150,"original tree admission probabilities");
        var level=h.getLevel();var chunk=site(h,80);biome(level,chunk,ChromaBiomes.LUMINOUS_CLIFFS);
        var pos=new BlockPos(chunk.getMinBlockX()+8,100,chunk.getMinBlockZ()+8);
        for(var dir:net.minecraft.core.Direction.values()) level.setBlock(pos.relative(dir),Blocks.AIR.defaultBlockState(),2);
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);var vine=new GlowingVineGenerator();
        h.assertTrue(!vine.canGenerateAt(level,pos.getX(),pos.getY(),pos.getZ()),"cliff vines require an adjacent solid face");
        level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(vine.canGenerateAt(level,pos.getX(),pos.getY(),pos.getZ()),"cliff vines accept solid support");
        biome(level,chunk,Biomes.FOREST);
        h.assertTrue(!vine.canGenerateAt(level,pos.getX(),pos.getY(),pos.getZ()),"loaded Chroma restricts Geo vines to cliffs");
        h.succeed();
    }
}
*/
