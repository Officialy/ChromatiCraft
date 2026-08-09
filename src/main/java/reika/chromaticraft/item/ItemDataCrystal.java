package reika.chromaticraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import reika.chromaticraft.auxiliary.recipemanagers.InscriptionRecipes;
import reika.chromaticraft.entity.EntityDataCrystal;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** V33a Memory Crystal: owner-bound lore key and sustained-click block inscription tool. */
public final class ItemDataCrystal extends Item {

	public ItemDataCrystal(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		if (tag == null || level.getGameTime() - tag.getLongOr("last", 0) < 10) return;
		int carve = tag.getIntOr("carve", 0);
		if (carve > 1) {
			tag.putInt("carve", carve - 1);
			ReikaItemHelper.setStackTag(stack, tag);
		}
		else if (carve == 1) {
			ReikaItemHelper.setStackTag(stack, null);
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;
		ItemStack stack = context.getItemInHand();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (!ProgressStage.TOWER.isPlayerAtStage(player)) {
			ReikaItemHelper.setStackTag(stack, null);
			return InteractionResult.SUCCESS;
		}
		var recipe = InscriptionRecipes.instance.getInscriptionRecipe(level.getBlockState(pos));
		if (recipe == null) {
			ReikaItemHelper.setStackTag(stack, null);
			return InteractionResult.SUCCESS;
		}
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		if (tag == null) tag = new CompoundTag();
		if (tag.contains("recipe") && tag.getIntOr("recipe", -1) != recipe.referenceIndex()) {
			ReikaItemHelper.setStackTag(stack, null);
			return InteractionResult.SUCCESS;
		}
		boolean same = tag.getIntOr("x", Integer.MIN_VALUE) == pos.getX()
				&& tag.getIntOr("y", Integer.MIN_VALUE) == pos.getY()
				&& tag.getIntOr("z", Integer.MIN_VALUE) == pos.getZ();
		if (!same) {
			tag.putInt("recipe", recipe.referenceIndex());
			tag.putInt("x", pos.getX());
			tag.putInt("y", pos.getY());
			tag.putInt("z", pos.getZ());
			tag.putInt("carve", 0);
			ChromaSounds.INSCRIBE.playSound(level, pos, 1, 1);
		}
		else {
			int carve = tag.getIntOr("carve", 0) + 1;
			if (carve >= recipe.duration()) {
				recipe.place(server, pos, player);
				ChromaNetwork.sendInscription(server, pos, recipe.referenceIndex());
				ReikaItemHelper.setStackTag(stack, null);
				return InteractionResult.SUCCESS;
			}
			tag.putInt("carve", carve);
			if (carve % 5 == 0) ChromaSounds.INSCRIBE.playSound(level, pos, 1, 1);
		}
		tag.putLong("last", level.getGameTime());
		ReikaItemHelper.setStackTag(stack, tag);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer)
			ChromaNetwork.openLorePuzzle(serverPlayer);
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hasCustomEntity(ItemStack stack) {
		return true;
	}

	@Override
	public Entity createEntity(Level level, Entity location, ItemStack stack) {
		EntityDataCrystal crystal = new EntityDataCrystal(level, location.getX(), location.getY(), location.getZ(), stack.copy());
		crystal.setDeltaMovement(location.getDeltaMovement());
		crystal.setPickUpDelay(40);
		if (location instanceof ItemEntity item && item.getOwner() != null)
			crystal.setThrower(item.getOwner());
		return crystal;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		return tag != null && !tag.getBooleanOr("tooltip", false) && tag.getIntOr("carve", 0) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		if (tag == null) return 0;
		var recipe = InscriptionRecipes.instance.getRecipeByID(tag.getIntOr("recipe", -1));
		return recipe == null ? 0 : Math.clamp(Math.round(13F * tag.getIntOr("carve", 0) / recipe.duration()), 0, 13);
	}
}
