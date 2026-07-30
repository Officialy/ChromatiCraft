package reika.chromaticraft.client;

import java.util.List;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.dye26.BlockDyeLeaf;
import reika.chromaticraft.block.dye26.BlockDyeSapling;
import reika.chromaticraft.block.dye26.BlockRainbowLeaf;
import reika.chromaticraft.registry.ChromaBlocks;

/** Block tint sources used by the original tint-indexed crystal outline texture. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ChromaBlockColors {
    private ChromaBlockColors() {}

    private static final BlockTintSource CAVE_CRYSTAL = new BlockTintSource() {
        @Override public int color(BlockState state) { return tint(state); }
        @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) { return tint(state); }
        private int tint(BlockState state) {
            BlockCaveCrystal crystal = (BlockCaveCrystal)state.getBlock();
            return 0xFF000000 | crystal.getTintColor(crystal.getCrystalElement().ordinal());
        }
    };

    private static final BlockTintSource DYE_TREE = new BlockTintSource() {
        @Override public int color(BlockState state) { return tint(state); }
        @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) { return tint(state); }
        private int tint(BlockState state) {
            if (state.getBlock() instanceof BlockDyeLeaf leaf) return leaf.getTintColor();
            return ((BlockDyeSapling)state.getBlock()).getTintColor();
        }
    };

    private static final BlockTintSource RAINBOW_LEAF = new BlockTintSource() {
        @Override public int color(BlockState state) { return BlockRainbowLeaf.getTintColor(BlockPos.ZERO); }
        @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return BlockRainbowLeaf.getTintColor(pos);
        }
    };
    /**
     * Vanilla grass/foliage blocks whose tint the Luminous Cliffs biome shifts per position and
     * altitude. V33a did this by overriding {@code BiomeGenBase.getBiomeGrassColor(x,y,z)}, which
     * has no 26.2 equivalent — the modern {@code ColorResolver} never sees Y. Wrapping the tint
     * source is the hook that does; see {@link LuminousCliffsColors}.
     */
    private static final net.minecraft.world.level.block.Block[] CLIFF_TINTED_VANILLA = {
        Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.POTTED_FERN, Blocks.BUSH,
        Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.PINK_PETALS, Blocks.WILDFLOWERS,
        Blocks.SUGAR_CANE, Blocks.LEAF_LITTER,
        Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES, Blocks.VINE,
    };

    /**
     * Drops the cached Luminous Cliffs biome identities when the client leaves a level, so a world
     * switch cannot leave the tint wrappers comparing against a stale registry.
     */
    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LuminousCliffsColors.onLevelChanged(null);
    }

    /** Builds the cached, blend-radius-aware tint cache the cliff tint wrappers probe. */
    @SubscribeEvent
    public static void registerColorResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(LuminousCliffsColors.CLIFF_PRESENCE);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(CAVE_CRYSTAL), ChromaBlocks.CAVE_CRYSTALS.stream().map(h -> h.get()).toArray(net.minecraft.world.level.block.Block[]::new));
        net.minecraft.world.level.block.Block[] dyeBlocks = java.util.stream.Stream.concat(
                ChromaBlocks.DYE_LEAVES.stream(), ChromaBlocks.DYE_SAPLINGS.stream())
                .map(h -> (net.minecraft.world.level.block.Block)h.get())
                .toArray(net.minecraft.world.level.block.Block[]::new);
        event.register(List.of(DYE_TREE), dyeBlocks);
        event.register(List.of(RAINBOW_LEAF), ChromaBlocks.RAINBOW_LEAVES.get());
        registerLuminousCliffsTints(event);
    }

    /**
     * Re-registers the vanilla grass/foliage tint sources wrapped in the Luminous Cliffs shift.
     * The wrapper delegates for every position outside the biome, so world-wide behaviour is
     * unchanged; only the layer count and identity of the sources differ.
     */
    private static void registerLuminousCliffsTints(RegisterColorHandlersEvent.BlockTintSources event) {
        BlockColors colors = event.getBlockColors();
        for (net.minecraft.world.level.block.Block block : CLIFF_TINTED_VANILLA) {
            List<BlockTintSource> existing = colors.getTintSources(block.defaultBlockState());
            if (existing.isEmpty())
                continue;
            event.register(existing.stream().map(LuminousCliffsColors::wrapTerrain).toList(), block);
        }
    }
}