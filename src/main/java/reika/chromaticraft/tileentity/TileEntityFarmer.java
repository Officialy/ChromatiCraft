/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity;

import java.util.Collection;
import java.util.EnumMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.ComplexAOE;
import reika.chromaticraft.base.tileentity.TileEntityRelayPowered;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.interfaces.registry.CropType;
import reika.dragonapi.libraries.registry.ReikaCropHelper;
import reika.dragonapi.modregistry.ModCropList;

public final class TileEntityFarmer extends TileEntityRelayPowered implements ComplexAOE {

    private static final EnumMap<Direction, java.util.List<WeightedPosition>> POSITIONS = createPositions();

    public TileEntityFarmer(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.FARMER.get(), pos, state);
    }

    private static EnumMap<Direction, java.util.List<WeightedPosition>> createPositions() {
        var positions = new EnumMap<Direction, java.util.List<WeightedPosition>>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var entries = new java.util.ArrayList<WeightedPosition>();
            Direction left = direction.getCounterClockWise();
            for (int distance = 0; distance < 16; distance++) {
                for (int spread = -distance; spread <= distance; spread++) {
                    double weight = 100;
                    if (Math.abs(spread) > 2 && Math.abs(spread) >= distance / 2)
                        weight -= Math.abs(spread) / 3D * 10;
                    if (distance > 8) weight *= 1 - (distance - 8) / 8D;
                    if (weight > 0)
                        entries.add(new WeightedPosition(new Coordinate(
                                distance * direction.getStepX() + spread * left.getStepX(), 0,
                                distance * direction.getStepZ() + spread * left.getStepZ()), weight));
                }
            }
            positions.put(direction, java.util.List.copyOf(entries));
        }
        return positions;
    }

    @Override
    public void updateEntity(Level world, BlockPos pos) {
        super.updateEntity(world, pos);
        if (!(world instanceof ServerLevel server) || this.hasRedstoneSignal()
                || this.getEnergy(CrystalElement.GREEN) < 200) return;
        int attempts = Math.max(1, this.getEnergy(CrystalElement.GREEN) / 2500);
        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos target = this.getRandomPosition(server);
            if (target != null && this.operateAt(server, target, true)) {
                reika.chromaticraft.network.ChromaNetwork.sendFarmerHarvest(server, pos, target);
                break;
            }
        }
    }

    private boolean operateAt(ServerLevel world, BlockPos target, boolean allowRelays) {
        if (!world.hasChunkAt(target)) return false;
        if (allowRelays && world.getBlockEntity(target) instanceof TileEntityFunctionRelay relay) {
            Coordinate next = relay.getRandomCoordinate();
            return next != null && this.operateAt(world,
                    new BlockPos(next.xCoord, next.yCoord, next.zCoord), false);
        }
        BlockState state = world.getBlockState(target);
        Block block = state.getBlock();
        int fortune = this.getEnergy(CrystalElement.PURPLE) / 1000;
        java.util.List<ItemStack> drops;
        if ((block == Blocks.CACTUS || block == Blocks.SUGAR_CANE)
                && world.getBlockState(target.below()).is(block)) {
            drops = this.blockDrops(world, target, state, fortune);
            world.removeBlock(target, false);
        }
        else {
            CropType crop = ReikaCropHelper.getCrop(block);
            if (crop == null) crop = ModCropList.getModCrop(world, target, state);
            if (crop == null || !crop.isRipe(world, target)) return false;
            drops = crop.getDrops(world, target, fortune);
            if (fortune < 3)
                reika.dragonapi.interfaces.registry.CropType.CropMethods.removeOneSeed(crop, drops);
            crop.setHarvested(world, target);
        }
        for (ItemStack drop : drops) Block.popResource(world, target, drop);
        reika.dragonapi.libraries.io.ReikaSoundHelper.playBreakSound(world, target, block);
        this.drainEnergy(CrystalElement.GREEN, 200);
        this.drainEnergy(CrystalElement.PURPLE, 50);
        return true;
    }

    private java.util.List<ItemStack> blockDrops(ServerLevel world, BlockPos target, BlockState state, int fortune) {
        ItemStack tool = new ItemStack(net.minecraft.world.item.Items.IRON_HOE);
        if (fortune > 0)
            tool.enchant(world.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), fortune);
        return Block.getDrops(state, world, target, world.getBlockEntity(target), this.getPlacer(), tool);
    }

    private BlockPos getRandomPosition(ServerLevel world) {
        var positions = POSITIONS.get(this.getFacing());
        double total = positions.stream().mapToDouble(WeightedPosition::weight).sum();
        double choice = rand.nextDouble() * total;
        Coordinate selected = positions.getLast().position();
        for (WeightedPosition entry : positions) {
            choice -= entry.weight();
            if (choice < 0) {
                selected = entry.position();
                break;
            }
        }
        BlockPos target = worldPosition.offset(selected.xCoord, 0, selected.zCoord);
        if (!world.hasChunkAt(target)) return null;
        while (target.getY() > world.getMinY() && world.isEmptyBlock(target))
            target = target.below();
        if (this.isSubmerged(world)
                && target.distManhattan(worldPosition) >= 5) return null;
        return target;
    }

    private boolean isSubmerged(Level world) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(direction);
            BlockState state = world.getBlockState(adjacent);
            if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) continue;
            if (!state.isSolidRender() && !state.isCollisionShapeFullBlock(world, adjacent))
                return false;
        }
        return true;
    }

    @Override public int getMaxStorage(CrystalElement element) {
        return switch (element) {
            case GREEN -> 10000;
            case PURPLE -> 5000;
            default -> 0;
        };
    }

    @Override public ChromaTiles getTile() { return ChromaTiles.FARMER; }

    @Override
    protected void animateWithTick(Level world, BlockPos pos) {
    }

    @Override
    protected boolean canReceiveFrom(CrystalElement element, Direction direction) {
        return this.isAcceptingColor(element);
    }

    @Override
    public ElementTagCompound getRequiredEnergy() {
        ElementTagCompound required = new ElementTagCompound();
        required.addTag(CrystalElement.GREEN, this.getRemainingSpace(CrystalElement.GREEN));
        required.addTag(CrystalElement.PURPLE, this.getRemainingSpace(CrystalElement.PURPLE));
        return required;
    }

    @Override public boolean isAcceptingColor(CrystalElement element) {
        return element == CrystalElement.GREEN || element == CrystalElement.PURPLE;
    }

    public Direction getFacing() {
        return getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public Collection<Coordinate> getPossibleRelativePositions() {
        return POSITIONS.get(Direction.SOUTH).stream().map(WeightedPosition::position).toList();
    }

    @Override
    public double getNormalizedWeight(Coordinate position) {
        var positions = POSITIONS.get(Direction.SOUTH);
        double maximum = positions.stream().mapToDouble(WeightedPosition::weight).max().orElseThrow();
        return positions.stream().filter(entry -> entry.position().equals(position))
                .mapToDouble(WeightedPosition::weight).sum() / maximum;
    }

    private record WeightedPosition(Coordinate position, double weight) {}
}
