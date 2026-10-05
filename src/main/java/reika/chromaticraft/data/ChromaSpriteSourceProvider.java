package reika.chromaticraft.data;

import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.AtlasIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.LegacyGlowSpriteSource;

import java.util.concurrent.CompletableFuture;

/** Atlas data preserves the V33a originals and supplies separate alpha-backed sprites for Iris. */
public final class ChromaSpriteSourceProvider extends SpriteSourceProvider {
    public ChromaSpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, ChromatiCraft.MODID);
    }

    @Override protected void gather() {
        var blocks = atlas(AtlasIds.BLOCKS).addSource(new DirectoryLister("block/icons", "block/icons/"));
        for (String name : new String[] {"roundflare", "sunflare", "turbo"}) {
            blocks.addSource(new LegacyGlowSpriteSource(icon(name), icon(name+"_alpha"), false));
        }
        blocks.addSource(new LegacyGlowSpriteSource(icon("roundflare"), icon("roundflare_core_alpha"), true));
    }

    private static Identifier icon(String name) { return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/"+name); }
}
