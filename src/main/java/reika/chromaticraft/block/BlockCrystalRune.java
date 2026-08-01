/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017
 ******************************************************************************/
package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;

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

    /**
     * V33a {@code onBlockPlacedBy}: a rune placed touching crystalline stone, within six blocks of a
     * Casting Table, reports itself to that table. That is the only path to
     * {@link reika.chromaticraft.magic.progression.ProgressStage#RUNEUSE}, which in turn is what
     * unlocks tier-2 (auxiliary-stand) casting — so this hook is what makes the stands usable.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(placer instanceof Player player))
            return;
        if (!hasAdjacentCrystallineStone(level, pos))
            return;
        TileEntityCastingTable table = findNearbyTable(level, pos);
        if (table != null)
            table.onAddRune(player);
    }

    /** V33a checks the 26 surrounding positions, corners included, for crystalline stone. */
    private static boolean hasAdjacentCrystallineStone(Level level, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (check.equals(pos))
                continue;
            if (level.getBlockState(check).getBlock() instanceof BlockCrystallineStone)
                return true;
        }
        return false;
    }

    /** V33a {@code ReikaWorldHelper.findNearBlock(..., 6, TABLE)}. */
    private static TileEntityCastingTable findNearbyTable(Level level, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-SEARCH_RANGE, -SEARCH_RANGE, -SEARCH_RANGE),
                pos.offset(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE))) {
            if (level.getBlockEntity(check) instanceof TileEntityCastingTable table)
                return table;
        }
        return null;
    }

    private static final int SEARCH_RANGE = 6;
}