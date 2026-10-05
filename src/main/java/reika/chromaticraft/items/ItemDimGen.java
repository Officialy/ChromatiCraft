/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import reika.chromaticraft.auxiliary.interfaces.TieredItem;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDimensionItems;
import reika.chromaticraft.registry.ProximaDecoTypes;

/** V33a DIMGEN resources; only Miasmatic Essence places its corresponding decoration. */
public class ItemDimGen extends Item implements TieredItem {
    private final ChromaDimensionItems type;
    public ItemDimGen(Properties properties, ChromaDimensionItems type) {
        super(properties);
        this.type = type;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (type != ChromaDimensionItems.MIASMA) return super.useOn(context);
        var pos = context.getClickedPos().relative(context.getClickedFace());
        var player = context.getPlayer();
        if (!context.getLevel().isEmptyBlock(pos) || (player != null
                && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())))
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            context.getLevel().setBlockAndUpdate(pos, ChromaBlocks.deco(ProximaDecoTypes.MIASMA).get().defaultBlockState());
            if (player == null || !player.isCreative()) context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override public ProgressStage getDiscoveryTier(net.minecraft.world.item.ItemStack stack) { return ProgressStage.DIMENSION; }
    @Override public boolean isTiered(net.minecraft.world.item.ItemStack stack) { return true; }
}
