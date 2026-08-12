package reika.chromaticraft.item;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** UUID-bearing key for Chroma Doors; V33a's metadata mode bit is modern CUSTOM_DATA. */
public final class ItemDoorKey extends Item {

	private static final String UUID_TAG = "uid";
	private static final String AUTO_TAG = "auto";

	public ItemDoorKey(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof TileEntityChromaDoor door))
			return InteractionResult.PASS;
		ItemStack stack = context.getItemInHand();
		UUID id = getUID(stack);
		if (!level.isClientSide()) {
			if (player.isShiftKeyDown())
				door.bindUUID(player, id, isAutoOpen(stack));
			else if (door.canOpen(id)) {
				door.openClick();
				if (door.getBlockState().getValue(BlockChromaDoor.CONSUME_KEY))
					stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Right-clicking air toggles the source's auto-open binding bit. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) setAutoOpen(stack, !isAutoOpen(stack));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("ID: " + getUID(stack)).withStyle(ChatFormatting.GRAY));
		if (isAutoOpen(stack))
			tooltip.accept(Component.literal("Sets doors to auto-open").withStyle(ChatFormatting.AQUA));
	}

	public UUID getUID(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getOrCreateStackTag(stack);
		if (tag.contains(UUID_TAG)) {
			try { return UUID.fromString(tag.getString(UUID_TAG).orElse("")); }
			catch (IllegalArgumentException ignored) { }
		}
		UUID id = UUID.randomUUID();
		setID(stack, id);
		return id;
	}

	public void setID(ItemStack stack, UUID id) {
		ReikaItemHelper.updateStackTag(stack, tag -> tag.putString(UUID_TAG, id.toString()));
	}

	public boolean isAutoOpen(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		return tag != null && tag.getBooleanOr(AUTO_TAG, false);
	}

	public void setAutoOpen(ItemStack stack, boolean automatic) {
		ReikaItemHelper.updateStackTag(stack, tag -> tag.putBoolean(AUTO_TAG, automatic));
	}
}
