/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.level.ReikaBlockHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a Subterranean Route Finder: shortest open route to the sky, or, while sneaking, the deepest reachable
 * point within 400 horizontal blocks without climbing above the start. Work and waypoint placement are spread
 * across server ticks; only already loaded terrain is searched. Air-only waypoints retain original spacing/links.
 */
public class ItemCaveExitFinder extends Item {
    public ItemCaveExitFinder(Properties properties) { super(properties); }

    static boolean isPassable(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.is(ChromaBlocks.HOVER.get()) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_BARS) || state.is(ChromaBlocks.CHROMA_TRAIL.get())
                || ReikaBlockHelper.isLiquid(state) || ReikaWorldHelper.softBlocks(world, pos) || ReikaBlockHelper.isLeaf(world, pos);
    }

    @Override public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (world instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
            if (!player.isShiftKeyDown() && level.canSeeSky(player.blockPosition())) return InteractionResult.SUCCESS;
            CaveRouteJobs.start(level, serverPlayer, player.isShiftKeyDown());
        }
        return InteractionResult.SUCCESS;
    }
}
