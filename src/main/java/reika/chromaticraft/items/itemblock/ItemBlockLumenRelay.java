package reika.chromaticraft.items.itemblock;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import reika.chromaticraft.block.relay.BlockLumenRelay;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/** Separate-id replacement for V33a's metadata-valued relay BlockItem. */
public final class ItemBlockLumenRelay extends BlockItem {

	private final BlockLumenRelay relay;

	public ItemBlockLumenRelay(BlockLumenRelay block, Item.Properties properties) {
		super(block, properties);
		relay = block;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal(relay.isMultichromic()
				? "Conducts all elements."
				: "Conducts " + relay.getElement().displayName + '.'));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return context.getPlayer() != null && ReikaPlayerAPI.isFake(context.getPlayer())
				? InteractionResult.FAIL : super.useOn(context);
	}
}
