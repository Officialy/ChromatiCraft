/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017
 ******************************************************************************/
package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.registry.CrystalElement;

/** A crystal rune whose colour is part of its registry identity, not mutable block state. */
public final class BlockCrystalRune extends Block {

    private final CrystalElement element;

    public BlockCrystalRune(BlockBehaviour.Properties props, CrystalElement element) {
        super(props);
        this.element = element;
    }

    public CrystalElement getColor() {
        return element;
    }

    public static CrystalElement getColor(BlockState state) {
        if (state.getBlock() instanceof BlockCrystalRune rune)
            return rune.element;
        throw new IllegalArgumentException("Not a crystal rune state: " + state);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }
}