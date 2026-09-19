package reika.chromaticraft.items.tools;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import reika.chromaticraft.ChromatiCraft;

/**
 * V33a's reusable End Crystal mover. The old item encoded its captured-crystal count in item damage;
 * 26.2 custom data carries that mutable payload while the item keeps one stable registry identity.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ItemEnderCrystal extends Item {

	private static final String CRYSTALS_TAG = "ender_crystals";

	public ItemEnderCrystal(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		if (stored(stack) <= 0)
			return InteractionResult.FAIL;
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockState clickedState = level.getBlockState(clicked);
		BlockPos base = clickedState.canBeReplaced() && clickedState.getFluidState().isEmpty()
				? clicked : clicked.relative(context.getClickedFace());
		Player player = context.getPlayer();
		if (player == null || !player.mayUseItemAt(base, context.getClickedFace(), stack)
				|| !hasSpace(level, base))
			return InteractionResult.FAIL;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		level.setBlockAndUpdate(base, Blocks.BEDROCK.defaultBlockState());
		level.setBlockAndUpdate(base.above(), Blocks.FIRE.defaultBlockState());
		EndCrystal crystal = new EndCrystal(level, base.getX() + 0.5, base.getY() + 1,
				base.getZ() + 0.5);
		level.addFreshEntity(crystal);
		if (!player.getAbilities().instabuild)
			setStored(stack, stored(stack) - 1);
		level.playSound(null, base, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1, 0.55F);
		return InteractionResult.SUCCESS;
	}

	private static boolean hasSpace(Level level, BlockPos base) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				for (int dy = 0; dy < 3; dy++) {
					BlockState state = level.getBlockState(base.offset(dx, dy, dz));
					if (!state.getFluidState().isEmpty() || (!state.isAir() && !state.canBeReplaced()))
						return false;
				}
		return true;
	}

	/** Captures any vanilla End Crystal, including the eight used by the Portal Rift structure. */
	@net.neoforged.bus.api.SubscribeEvent
	public static void captureCrystal(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof EndCrystal crystal))
			return;
		ItemStack stack = event.getEntity().getItemInHand(event.getHand());
		if (!(stack.getItem() instanceof ItemEnderCrystal))
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		if (!(crystal.level() instanceof ServerLevel level))
			return;
		setStored(stack, stored(stack) + 1);
		BlockPos base = crystal.blockPosition().below();
		crystal.discard();
		if (level.getBlockState(base).is(Blocks.BEDROCK))
			level.removeBlock(base, false);
		level.playSound(null, base, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1, 0.55F);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		int count = stored(stack);
		tooltip.accept(count > 0 ? Component.translatable("item.chromaticraft.ender_crystal_mover.filled", count)
				: Component.translatable("item.chromaticraft.ender_crystal_mover.empty"));
	}

	public static int stored(ItemStack stack) {
		return Math.max(0, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getIntOr(CRYSTALS_TAG, 0));
	}

	/** The second V33a creative subtype: a mover preloaded with one crystal. */
	public static ItemStack filledCreativeStack(Item item) {
		ItemStack stack = new ItemStack(item);
		setStored(stack, 1);
		return stack;
	}

	private static void setStored(ItemStack stack, int count) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			if (count > 0) tag.putInt(CRYSTALS_TAG, count);
			else tag.remove(CRYSTALS_TAG);
		});
	}
}
