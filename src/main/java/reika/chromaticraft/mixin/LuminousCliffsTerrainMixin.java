package reika.chromaticraft.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature;

/** 26.3's unified terrain pipeline: surface, Chroma column shaping, then vanilla carvers. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class LuminousCliffsTerrainMixin {
    @Inject(method = "generateCarvers", at = @At("HEAD"))
    private void chroma$shapeBeforeCarvers(ChunkAccess chunk, Blender blender, NoiseChunk noise,
            RandomState randomState, BiomeManager biomes, WorldGenRegion region,
            MaterialRule rule, CallbackInfo ci) {
        if (region == null || region.getLevel().dimension() == Level.OVERWORLD) {
            NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator)(Object)this;
            LuminousCliffsTerrainFeature.shapeBeforeCarvers(chunk, generator, randomState, randomState.seed());
        }
    }
}
