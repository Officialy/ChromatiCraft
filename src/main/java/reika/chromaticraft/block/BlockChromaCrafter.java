package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityChromaCrafter;

/** Source CHROMACRAFTER: automation input/output and bucket access, with owner authorization. */
public final class BlockChromaCrafter extends BlockChromaticTile {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FORMED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("formed");
    public BlockChromaCrafter(Properties properties) {
        super(properties, ChromaTiles.CHROMACRAFTER);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(FORMED);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityChromaCrafter crafter)
                || (!crafter.isPlayerAccessible(player) || !crafter.isOwnedByPlayer(player))) return InteractionResult.PASS;
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        return FluidUtil.interactWithFluidHandler(player, hand, pos, crafter.fluidHandler(hit.getDirection()), null)
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
