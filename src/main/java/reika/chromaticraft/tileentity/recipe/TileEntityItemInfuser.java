package reika.chromaticraft.tileentity.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** V33a Raw Crystal -> Iridescent Crystal Shard machine, including overflow and DOUBLECRAFT. */
public final class TileEntityItemInfuser extends TileEntityAuraInfuser {

	private static final String EXTRA_DROP = "requiredExtra";

	public TileEntityItemInfuser(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ITEM_INFUSER.get(), pos, state);
	}

	@Override public ChromaTiles getTile() { return ChromaTiles.INFUSER; }
	@Override protected ChromaStructures getStructure() { return ChromaStructures.INFUSION; }

	@Override
	protected void collectFocusCrystalLocations(FilledBlockArray array) {
		BlockPos origin = this.getBlockPos();
		for (BlockPos cell : array.keySet()) {
			if (cell.getY() == origin.getY() - 1 && cell.distManhattan(origin) > 2
					&& array.getBlockAt(cell.getX(), cell.getY(), cell.getZ())
							== ChromaBlocks.crystallineStone(StoneTypes.BRICKS).get())
				focusCrystalSpots.add(cell.above());
		}
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && stack.is(ChromaItems.CRAFTING.get(ChromaCraftingItems.RAW_CRYSTAL).get());
	}
	@Override protected boolean isReady() { return this.canPlaceItem(0, inv.get(0)); }

	@Override
	protected void onCraft() {
		int count = inv.get(0).getCount();
		Player player = this.getCraftingPlayer();
		if (player != null && ChromaAbilityData.hasDoubleCraft(player)) count *= 2;
		ItemStack result = ChromaItems.craftingStack(ChromaCraftingItems.IRIDESCENT_CRYSTAL);
		int extra = Math.max(0, count - result.getMaxStackSize());
		result.setCount(Math.min(count, result.getMaxStackSize()));
		if (extra > 0) {
			CompoundTag tag = new CompoundTag();
			tag.putInt(EXTRA_DROP, extra);
			result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
		inv.set(0, result);
		if (player != null) ProgressStage.INFUSE.stepPlayerTo(player);
	}

	@Override
	protected ItemEntity dropItem() {
		ItemEntity entity = super.dropItem();
		if (entity == null) return null;
		ItemStack stack = entity.getItem();
		if (!stack.is(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())) return entity;
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		if (tag == null) return entity;
		int extra = tag.getIntOr(EXTRA_DROP, 0);
		if (extra <= 0) return entity;
		tag.remove(EXTRA_DROP);
		ReikaItemHelper.setStackTag(stack, tag);
		ItemStack second = stack.copyWithCount(extra);
		ItemEntity overflow = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), second);
		entity.level().addFreshEntity(overflow);
		return entity;
	}

	@Override protected void spawnCraftingParticles(Level world, BlockPos pos) {
		ChromaParticle.spawnItemInfuserCrafting(world, pos, this.getTicksExisted(), world.getRandom());
	}
	@Override protected void spawnCompletionParticles(Level world, BlockPos pos) {
		ChromaParticle.spawnItemInfuserCompletion(world, pos, world.getRandom());
	}

	/** Focused test seam: completes through the production craft/consume/progression path. */
	public void completeCraftForTest() { this.craft(); }
}
