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

/** Crystal lamp: a fixed-colour, decorative full-bright crystal with no potion effects. */
public class BlockCrystalLamp extends CrystalBlock {

    private final CrystalElement element;

    public BlockCrystalLamp(BlockBehaviour.Properties props, CrystalElement element) {
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
        return false;
    }

    @Override
    public boolean performEffect(CrystalElement e) {
        return false;
    }

    @Override
    public int getRange() {
        return 3;
    }

    @Override
    public int getDuration(CrystalElement e) {
        return 200;
    }

    @Override
    public int getPotionLevel(CrystalElement e) {
        return 0;
    }

    @Override
    public boolean renderBase() {
        return true;
    }
}