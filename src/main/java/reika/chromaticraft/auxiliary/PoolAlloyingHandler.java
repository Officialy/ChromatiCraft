package reika.chromaticraft.auxiliary;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingInput;
import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingRecipe;
import reika.chromaticraft.block.BlockChromaFluid;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaRecipeTypes;

/** Server-authoritative V33a dropped-item Pool Recipe runtime. */
public final class PoolAlloyingHandler {
	private static final String ACTIVE_TAG = "chromaticraft_pool_alloying";

	private PoolAlloyingHandler() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(PoolAlloyingHandler::onItemTick);
	}

	private static void onItemTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof ItemEntity catalyst)
				|| !(catalyst.level() instanceof ServerLevel level) || !catalyst.isAlive()) return;
		boolean timingRoll = level.getRandom().nextInt(5) == 0;
		if (!timingRoll && !catalyst.getPersistentData().getBooleanOr(ACTIVE_TAG, false)) return;
		RecipeHolder<PoolAlloyingRecipe> holder = findRecipe(catalyst);
		if (holder == null) return;
		catalyst.getPersistentData().putBoolean(ACTIVE_TAG, true);
		PoolAlloyingRecipe recipe = holder.value();
		ChromaNetwork.sendPoolAlloyingFx(level, catalyst);
		if (timingRoll && catalyst.tickCount >= recipe.minimumDuration() && catalyst.tickCount > 20) {
			BlockChromaFluid.TileEntityChroma pool = pool(catalyst);
			int speed = BlockChromaFluid.getSpeedMultiplier(pool.getEtherCount());
			int elapsed = catalyst.tickCount - recipe.minimumDuration();
			if (level.getRandom().nextInt(Math.max(1, 20 / speed)) == 0
					&& (elapsed >= 600 / speed
							|| level.getRandom().nextInt(Math.max(1, (600 - elapsed) / speed)) == 0))
				complete(catalyst, recipe, pool);
		}
	}

	public static RecipeHolder<PoolAlloyingRecipe> findRecipe(ItemEntity catalyst) {
		if (!(catalyst.level() instanceof ServerLevel level) || !(catalyst.getOwner() instanceof Player player)
				|| !ProgressStage.ALLOY.playerHasPrerequisites(player) || pool(catalyst) == null) return null;
		List<ItemEntity> entities = entitiesInCell(level, catalyst.blockPosition());
		PoolAlloyingInput input = new PoolAlloyingInput(catalyst.getItem(),
				entities.stream().map(ItemEntity::getItem).toList());
		for (RecipeHolder<PoolAlloyingRecipe> holder : level.getServer().getRecipeManager()
				.recipeMap().byType(ChromaRecipeTypes.POOL_ALLOYING.get())) {
			PoolAlloyingRecipe recipe = holder.value();
			if (recipe.playerHasProgress(player) && recipe.matches(input, level)) return holder;
		}
		return null;
	}

	/** Atomic completion shared by runtime and the focused GameTest. */
	public static boolean complete(ItemEntity catalyst, PoolAlloyingRecipe recipe,
			BlockChromaFluid.TileEntityChroma pool) {
		if (!(catalyst.level() instanceof ServerLevel level) || !(catalyst.getOwner() instanceof Player owner)
				|| pool == null) return false;
		List<ItemEntity> entities = entitiesInCell(level, catalyst.blockPosition());
		PoolAlloyingInput input = new PoolAlloyingInput(catalyst.getItem(),
				entities.stream().map(ItemEntity::getItem).toList());
		if (!ProgressStage.ALLOY.playerHasPrerequisites(owner) || !recipe.playerHasProgress(owner)
				|| !recipe.matches(input, level)) return false;
		BlockPos pos = catalyst.blockPosition();
		double outputX = catalyst.getX();
		double outputY = catalyst.getY();
		double outputZ = catalyst.getZ();

		for (PoolAlloyingRecipe.CountedIngredient required : recipe.ingredients()) {
			int remaining = required.count();
			for (ItemEntity entity : entities) {
				ItemStack stack = entity.getItem();
				if (remaining > 0 && required.ingredient().test(stack)) {
					int consumed = Math.min(remaining, stack.getCount());
					stack.shrink(consumed);
					remaining -= consumed;
					if (stack.isEmpty()) entity.discard();
					else entity.setItem(stack);
				}
			}
		}
		ItemStack catalystStack = catalyst.getItem();
		catalystStack.shrink(1);
		if (catalystStack.isEmpty()) catalyst.discard();
		else catalyst.setItem(catalystStack);

		int outputMultiplier = recipe.allowDoubling()
				&& level.getRandom().nextFloat() < BlockChromaFluid.getDoublingChance(pool.getEtherCount()) ? 2 : 1;
		if (ChromaAbilityData.hasDoubleCraft(owner)) outputMultiplier *= 2;
		ItemStack result = recipe.result();
		int total = result.getCount() * outputMultiplier;
		for (int i = 0; i < total; i++) {
			ItemEntity output = new ItemEntity(level, outputX, outputY, outputZ,
					result.copyWithCount(1));
			output.setUnlimitedLifetime();
			output.setThrower(owner);
			level.addFreshEntity(output);
		}
		pool.clear();
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		ProgressStage.ALLOY.stepPlayerTo(owner);
		return true;
	}

	private static BlockChromaFluid.TileEntityChroma pool(ItemEntity entity) {
		return entity.level().getBlockEntity(entity.blockPosition()) instanceof BlockChromaFluid.TileEntityChroma pool
				? pool : null;
	}

	private static List<ItemEntity> entitiesInCell(ServerLevel level, BlockPos pos) {
		return new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos)));
	}
}
