/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block.crystal;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.registry.CrystalElement;

/** Super crystal: a fixed-colour, large-range, high-level effect crystal. */
public class BlockSuperCrystal extends CrystalBlock {

    private final CrystalElement element;

    public BlockSuperCrystal(BlockBehaviour.Properties props, CrystalElement element) {
        super(props);
        this.element = element;
    }

    @Override
    public CrystalElement getCrystalElement(BlockState state) {
        return element;
    }

    @Override
    public boolean shouldMakeNoise() {
        return true;
    }

    @Override
    public boolean shouldGiveEffects(CrystalElement e) {
        return true;
    }

    @Override
    public boolean performEffect(CrystalElement e) {
        return true;
    }

    @Override
    public int getRange() {
        return 12;
    }

    @Override
    public int getDuration(CrystalElement e) {
        return 6000;
    }

    @Override
    public int getPotionLevel(CrystalElement e) {
        return 2;
    }

    @Override
    public boolean renderBase() {
        return true;
    }
}