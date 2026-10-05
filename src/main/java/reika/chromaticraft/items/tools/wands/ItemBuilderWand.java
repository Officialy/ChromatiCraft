/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017
 * All rights reserved.
 * Distribution requires the owner's explicit prior permission.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.auxiliary.trackers.KeyWatcher.Key;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.libraries.ReikaDirectionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** V33a Construction Star: extend a connected face with the source state, using original control masks. */
public final class ItemBuilderWand extends ItemWandBase {
    public ItemBuilderWand(Properties properties) {
        super(properties);
        addEnergyCost(CrystalElement.BROWN, 10);
        addEnergyCost(CrystalElement.BLACK, 5);
        addEnergyCost(CrystalElement.WHITE, 10);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !sufficientEnergy(player)) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos source = context.getClickedPos();
        BlockState state = level.getBlockState(source);
        var positions = getCoordinatesFor(level, source, context.getClickedFace(), player);
        positions.sort(Comparator.comparingDouble(pos -> pos.distSqr(source)));
        boolean placed = false;
        for (BlockPos pos : positions) {
            if (!place(level, pos, state, player, stack, context.getClickedFace())) break;
            placed = true;
        }
        if (!placed) return InteractionResult.PASS;
        var sound = state.getSoundType(level, source, player);
        level.playSound(null, source, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume()+1)/2F, sound.getPitch()*0.8F);
        return InteractionResult.SUCCESS;
    }

    public static int getRange(Player player) { return canUseBoostedEffect(player) ? 12 : 6; }

    public static ArrayList<BlockPos> getCoordinatesFor(Level level, BlockPos source, Direction face, Player player) {
        var right = ReikaDirectionHelper.getPerpendicularDirections(face);
        Direction d1 = right.get(0), d2 = right.get(1);
        BlockState state = level.getBlockState(source);
        int range = getRange(player);
        int rx = BuildControl.COLUMN.isInUse(player) ? 0 : range;
        int ry = BuildControl.FLAT.isInUse(player) ? 0 : range;
        int rz = BuildControl.COLUMN.isInUse(player) ? 0 : range;
        BlockArray base = new BlockArray();
        base.maxDepth = 2*range;
        base.recursiveAddMultipleWithBounds(level, source.getX(), source.getY(), source.getZ(), Set.of(new BlockKey(state)),
                source.getX()-rx, source.getY()-ry, source.getZ()-rz,
                source.getX()+rx, source.getY()+ry, source.getZ()+rz);
        BlockPos center = source.relative(face);
        ArrayList<BlockPos> positions = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        // Retain V33a's axis masks (including the first X bound using r instead of rx).
        for (int x = -d1.getStepX()*rx-d2.getStepX()*range; x <= d1.getStepX()*rx+d2.getStepX()*rx; x++)
            for (int y = -d1.getStepY()*ry-d2.getStepY()*ry; y <= d1.getStepY()*ry+d2.getStepY()*ry; y++)
                for (int z = -d1.getStepZ()*rz-d2.getStepZ()*rz; z <= d1.getStepZ()*rz+d2.getStepZ()*rz; z++) {
                    BlockPos support = source.offset(x,y,z), destination = center.offset(x,y,z);
                    if (!level.isOutsideBuildHeight(destination) && level.getBlockState(support) == state
                            && base.hasBlock(support) && level.getBlockState(destination).canBeReplaced()
                            && clearSight(level, destination, center, player)) {
                        positions.add(destination);
                        minX = Math.min(minX, destination.getX()); maxX = Math.max(maxX, destination.getX());
                        minZ = Math.min(minZ, destination.getZ()); maxZ = Math.max(maxZ, destination.getZ());
                    }
                }
        if (BuildControl.FRAME.isInUse(player) || BuildControl.CENTER.isInUse(player)) {
            var copy = new HashSet<>(positions);
            positions.removeIf(pos -> {
                int neighbours = 0;
                for (int x = -d1.getStepX()-d2.getStepX(); x <= d1.getStepX()+d2.getStepX(); x++)
                    for (int y = -d1.getStepY()-d2.getStepY(); y <= d1.getStepY()+d2.getStepY(); y++)
                        for (int z = -d1.getStepZ()-d2.getStepZ(); z <= d1.getStepZ()+d2.getStepZ(); z++)
                            if (copy.contains(pos.offset(x,y,z))) neighbours++;
                return neighbours > 6 ? BuildControl.FRAME.isInUse(player) : BuildControl.CENTER.isInUse(player);
            });
        }
        if (BuildControl.WALLS.isInUse(player)) {
            int lx = minX, ux = maxX, lz = minZ, uz = maxZ;
            positions.removeIf(pos -> face.getStepX() == 0 && pos.getX() != lx && pos.getX() != ux
                    || face.getStepZ() == 0 && pos.getZ() != lz && pos.getZ() != uz);
        }
        return positions;
    }

    private static boolean clearSight(Level level, BlockPos from, BlockPos to, Player player) {
        if (from.equals(to)) return true;
        return level.clip(new ClipContext(Vec3.atCenterOf(from), Vec3.atCenterOf(to),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private boolean place(Level level, BlockPos pos, BlockState state, Player player, ItemStack wand, Direction face) {
        if (!sufficientEnergy(player) || !player.mayUseItemAt(pos, face, wand)
                || !level.getBlockState(pos).canBeReplaced()) return false;
        var item = state.getBlock().asItem();
        if (item == Items.AIR) return false;
        ItemStack material = ItemStack.EMPTY;
        if (!player.getAbilities().instabuild) {
            for (var carried : player.getInventory()) if (carried.is(item)) { material = carried; break; }
            if (material.isEmpty()) return false;
        }
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, 3)) return false;
        if (EventHooks.onBlockPlace(player, snapshot, face)) {
            snapshot.restore();
            return false;
        }
        state.getBlock().setPlacedBy(level, pos, state, player, new ItemStack(item));
        if (!player.getAbilities().instabuild) {
            material.shrink(1);
            player.getInventory().setChanged();
            drainPlayer(player);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> lines, TooltipFlag flag) {
        if (net.minecraft.client.Minecraft.getInstance().hasControlDown())
            for (BuildControl control : BuildControl.values())
                lines.accept(Component.translatable("item.chromaticraft.construction_star.control",
                        control.key.name(), Component.translatable("item.chromaticraft.construction_star."+control.name().toLowerCase(java.util.Locale.ROOT))));
        else lines.accept(Component.translatable("item.chromaticraft.construction_star.hold_ctrl",
                Component.literal("CTRL").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, lines, flag);
    }

    public enum BuildControl {
        COLUMN(Key.LCTRL), FLAT(Key.LSHIFT), FRAME(Key.TAB), CENTER(Key.TILDE), WALLS(Key.END);
        private final Key key;
        BuildControl(Key key) { this.key = key; }
        public boolean isInUse(Player player) {
            return this == FLAT ? player.isShiftKeyDown() : KeyWatcher.instance.isKeyDown(player, key);
        }
    }
}

