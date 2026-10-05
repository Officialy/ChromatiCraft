/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.client.render.ColoredAltarEffects;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.CubeRotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** V33a's half-height, fullbright altar, including density-dependent discharge and collapse. */
public final class BlockColoredAltar extends CrystalTypeBlock implements EntityBlock {
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,8,16);
    public BlockColoredAltar(Properties properties,CrystalElement element) { super(properties,element); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new TileEntityColoredAltar(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return type==ChromaBlockEntities.COLORED_ALTAR.get()?(world,pos,block,entity)->((TileEntityColoredAltar)entity).tick():null;
    }
    @Override protected void onBroken(ServerLevel level,BlockPos pos) {
        for (BlockPos point:BlockPos.betweenClosed(pos.offset(-9,-3,-9),pos.offset(9,3,9)))
            if (level.getBlockEntity(point) instanceof TileEntityColoredAltar other) other.calcNeighborDensity();
    }
    public static final class TileEntityColoredAltar extends BlockEntity {
        public static final int NUMBER_CUBES=12;
        public final CubeRotation[] cubeRotations=new CubeRotation[NUMBER_CUBES];
        public CrystalElement renderColor=CrystalElement.WHITE;
        private final Random random=new Random();
        private final int tickOffset=random.nextInt(360);
        private int ticks,neighborAreaCheckCount=-1;
        private float neighborAreaCheckValue;
        private final List<BlockPos> otherAltars=new ArrayList<>();
        public TileEntityColoredAltar(BlockPos pos,BlockState state) {
            super(ChromaBlockEntities.COLORED_ALTAR.get(),pos,state);
            for (int i=0;i<NUMBER_CUBES;i++) cubeRotations[i]=new CubeRotation().randomize(random);
        }
        public CrystalElement getColor() { return getBlockState().getBlock() instanceof BlockColoredAltar altar?altar.getCrystalElement():renderColor; }
        public int getTicksExisted() { return ticks; }
        public int getRenderTick() { return ticks+tickOffset; }
        private void tick() {
            ticks++; if (level.isClientSide()) { ColoredAltarEffects.particles(this); return; }
            if (neighborAreaCheckCount<0 || ticks%80==0) calcNeighborDensity();
            float density=neighborAreaCheckValue/neighborAreaCheckCount,collapse=Math.min(density,density*density*27);
            if (otherAltars.isEmpty() || density<=.0125F/25 || random.nextFloat()>=density) return;
            CrystalElement element=getColor(); Vec3 center=Vec3.atCenterOf(worldPosition);
            if (random.nextFloat()<density) {
                var player=level.getNearestPlayer(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),12,false);
                if (player!=null) {
                    reika.chromaticraft.auxiliary.PylonDamage.doPylonAttack(element,player,player.getHealth()/4);
                    Vec3 delta=player.position().subtract(center); double length=Math.max(1.0e-7,Math.hypot(delta.x,delta.z));
                    player.setDeltaMovement(player.getDeltaMovement().add(delta.x*1.5/length,.125+random.nextDouble()*.0625,delta.z*1.5/length)); player.syncVelocity=true;
                    reika.chromaticraft.magic.CrystalPotionController.instance.applyEffectFromColor(100,0,player,element,false,false,true);
                    ColoredAltarEffects.send((ServerLevel)level,worldPosition,player.position().add(0,-.25,0),element,"shock");
                    ChromaSounds.MONUMENTRAY.playSoundAtBlock(this,1,(float)reika.chromaticraft.auxiliary.CrystalMusicManager.instance.getDingPitchScale(element));
                }
            } else {
                BlockPos other=otherAltars.get(random.nextInt(otherAltars.size()));
                ColoredAltarEffects.send((ServerLevel)level,worldPosition,Vec3.atCenterOf(other),element,"bolt");
                ChromaSounds.MONUMENTRAY.playSoundAtBlock(this,1,(float)reika.chromaticraft.auxiliary.CrystalMusicManager.instance.getDingPitchScale(element));
            }
            if (density>.0006F && random.nextFloat()<collapse) {
                ColoredAltarEffects.send((ServerLevel)level,worldPosition,center,element,"break");
                ChromaSounds.POWERDOWN.playSoundAtBlock(this,1,1);
                level.levelEvent(2001,worldPosition,Block.getId(getBlockState()));
                level.removeBlock(worldPosition,false);
                for (int i=0,n=1+random.nextInt(4);i<n;i++) drop(ChromaItems.CRYSTAL_POWDER.get());
                if (random.nextBoolean()) drop(Items.COBBLESTONE);
                for (var point:otherAltars) if (level.getBlockEntity(point) instanceof TileEntityColoredAltar other) other.calcNeighborDensity();
            }
        }
        private void drop(Item item) { level.addFreshEntity(new ItemEntity(level,worldPosition.getX()+random.nextDouble(),worldPosition.getY()+random.nextDouble(),worldPosition.getZ()+random.nextDouble(),new ItemStack(item))); }
        private void calcNeighborDensity() {
            otherAltars.clear(); neighborAreaCheckCount=0; neighborAreaCheckValue=0;
            for (int x=-9;x<=9;x++) for (int z=-9;z<=9;z++) for (int y=-3;y<=3;y++) {
                if (x==0 && y==0 && z==0) continue; neighborAreaCheckCount++;
                BlockPos point=worldPosition.offset(x,y,z);
                if (level.getBlockState(point).getBlock() instanceof BlockColoredAltar) {
                    otherAltars.add(point); float weight=Math.clamp(4-(Math.abs(x)+Math.abs(y)+Math.abs(z))/6,1,4); neighborAreaCheckValue+=weight*weight;
                }
            }
        }
    }
}
