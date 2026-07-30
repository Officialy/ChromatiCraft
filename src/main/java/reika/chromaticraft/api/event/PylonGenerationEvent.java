/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.neoforged.bus.api.Event;

import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;

/** Fired after a pylon and its NBT-backed monument are successfully generated. */
public class PylonGenerationEvent extends Event {

    /** Whether the structure is damaged and thus inactive until repaired. */
    public final boolean isBroken;
    public final CrystalElementProxy color;
    public final WorldGenLevel world;
    public final BlockPos position;
    public final RandomSource random;
    public final int x;
    public final int y;
    public final int z;

    public PylonGenerationEvent(WorldGenLevel world, BlockPos pos, RandomSource random,
            boolean broken, CrystalElementProxy color) {
        this.world = world;
        position = pos.immutable();
        this.random = random;
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        isBroken = broken;
        this.color = color;
    }
}
