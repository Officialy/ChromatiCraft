package reika.chromaticraft.client.render;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

/** Derive coverage before atlas upload so shader packs receive ordinary, alpha-backed glow sprites. */
public record LegacyGlowSpriteSource(Identifier resourceId, Identifier spriteId, boolean core) implements SpriteSource {
    public static final MapCodec<LegacyGlowSpriteSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("resource").forGetter(LegacyGlowSpriteSource::resourceId),
            Identifier.CODEC.fieldOf("sprite").forGetter(LegacyGlowSpriteSource::spriteId),
            Codec.BOOL.optionalFieldOf("core", false).forGetter(LegacyGlowSpriteSource::core)
    ).apply(instance, LegacyGlowSpriteSource::new));

    @Override
    public void run(ResourceManager resources, Output output) {
        var location = TEXTURE_ID_CONVERTER.idToFile(resourceId);
        resources.getResource(location).ifPresentOrElse(resource -> output.add(spriteId,
                loader -> loader.loadSprite(spriteId, resource, (name, size, image, animation, metadata, texture) -> {
                    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                        int color = image.getPixel(x, y);
                        float brightness = Math.max(ARGB.red(color), Math.max(ARGB.green(color), ARGB.blue(color)))/255F;
                        float coverage = core ? Math.clamp((brightness-0.18F)*1.7F, 0F, 1F) : brightness;
                        int alpha = Math.round(ARGB.alpha(color)*coverage);
                        // Keep the old RGB contribution under conventional alpha blending: RGB * coverage
                        // equals the original additive colour. The core retains the original pylon_core shader's RGB.
                        float factor = !core && brightness > 0 ? 1/brightness : 1;
                        image.setPixel(x, y, ARGB.color(alpha, Math.min(255, Math.round(ARGB.red(color)*factor)),
                                Math.min(255, Math.round(ARGB.green(color)*factor)), Math.min(255, Math.round(ARGB.blue(color)*factor))));
                    }
                    return new SpriteContents(name, size, image, animation, metadata, texture);
                })), () -> LogUtils.getLogger().warn("Missing legacy glow sprite: {}", location));
    }

    @Override public MapCodec<LegacyGlowSpriteSource> codec() { return CODEC; }
}
