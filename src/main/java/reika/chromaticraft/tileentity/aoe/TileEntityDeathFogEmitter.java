/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.aoe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.base.tileentity.InventoriedChromaticBase;
import reika.chromaticraft.entity.EntityDeathFog;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

public final class TileEntityDeathFogEmitter extends InventoriedChromaticBase {
    public TileEntityDeathFogEmitter(BlockPos pos,BlockState state) { super(ChromaBlockEntities.DEATH_FOG_EMITTER.get(),pos,state); }
    @Override public ChromaTiles getTile() { return ChromaTiles.DEATHFOG; }
    @Override public void updateEntity(Level world,BlockPos pos) {
        if (world.isClientSide() || !world.hasChunksAt(pos.offset(-2,0,-2),pos.offset(2,0,2))) return;
        boolean enhanced=inv.get(0).is(ChromaItems.VOID_MONSTER_ESSENCE.get());
        if (enhanced && rand.nextInt(600)==0) { inv.get(0).shrink(1); setChanged(); }
        double yaw=rand.nextDouble()*360,pitch=rand.nextDouble()*360;
        while (pitch>30 && rand.nextInt(5)>0) pitch=rand.nextDouble()*360;
        double[] v=ReikaPhysicsHelper.polarToCartesian(.0625+rand.nextDouble()*.1875,pitch,yaw);
        world.addFreshEntity(new EntityDeathFog(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,v[0],v[1],v[2],enhanced));
    }
    @Override protected void animateWithTick(Level world,BlockPos pos) { }
    @Override public int getSizeInventory() { return 1; }
    @Override public int getMaxStackSize() { return 32; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return stack.is(ChromaItems.VOID_MONSTER_ESSENCE.get()); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return false; }
}
