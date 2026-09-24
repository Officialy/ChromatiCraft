/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.network.ChromaNetwork;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker.BreakerCallback;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker.ProgressiveBreaker;
import reika.dragonapi.instantiable.event.BlockTickEvent;
import reika.dragonapi.instantiable.event.BlockTickEvent.UpdateFlags;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.registry.ReikaCropHelper;
import reika.dragonapi.modregistry.ModCropList;

/**
 * V33a Fertility Seeds, shaken loose from Glow Roots.
 *
 * <p>Seven potencies, V33a's metadata 0-6, each now its own registered item. Dropped on the ground, a seed
 * waits {@link #INITIAL_DELAY} ticks and then, for ten ticks, force-ticks {@code (2 + 2*potency) * count}
 * random grass, mycelium, sapling or crop blocks within two blocks sideways and one up or down, before
 * vanishing. Used on dirt, it spreads grass over up to three connected blocks of dirt under light-passing
 * cover; used on anything else, it works as bone meal.
 *
 * <p>Seeds fall through leaves (see {@code ChromaFertilitySeedEvents}) so roots under a canopy still
 * drop them to the ground, and a seed that has woken up cannot be picked back up.
 */
public class ItemFertilitySeed extends Item implements BreakerCallback {

	public static final int INITIAL_DELAY = 40;
	public static final int POTENCIES = 7;

	private final int potency;

	public ItemFertilitySeed(int potency, Properties properties) {
		super(properties.stacksTo(8));
		this.potency = potency;
	}

	public int getPotency() {
		return potency;
	}

	/** V33a {@code getRandomAmbientDropMeta}: potency {@code i} weighted {@code 125 - 20i}. */
	public static int getRandomAmbientDropPotency(RandomSource rand) {
		int total = 0;
		for (int i = 0; i < POTENCIES; i++)
			total += 125 - i * 20;
		int pick = rand.nextInt(total);
		for (int i = 0; i < POTENCIES; i++) {
			pick -= 125 - i * 20;
			if (pick < 0)
				return i;
		}
		return POTENCIES - 1;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		ItemStack is = context.getItemInHand();
		Player ep = context.getPlayer();
		if (world.getBlockState(pos).is(Blocks.DIRT)) {
			if (!world.isClientSide()) {
				ProgressiveBreaker br = ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(world, pos, 3);
				br.drops = false;
				br.tickRate = 2;
				br.player = ep;
				br.hungerFactor = 0;
				br.causeUpdates = false;
				br.call = this;
				if (ep == null || !ep.getAbilities().instabuild)
					is.shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
		if (BoneMealItem.growCrop(is, world, pos)) {
			if (!world.isClientSide())
				world.levelEvent(1505, pos, 15);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity ei) {
		int age = ei.getAge();
		if (age < INITIAL_DELAY) {
			// vanilla behaviour until the seed wakes
		}
		else if (ei.level() instanceof ServerLevel world && age < INITIAL_DELAY + 10) {
			int x0 = Mth.floor(ei.getX());
			int y0 = Mth.floor(ei.getY());
			int z0 = Mth.floor(ei.getZ());
			if (world.hasChunksAt(x0 - 3, y0 - 3, z0 - 3, x0 + 3, y0 + 3, z0 + 3)) {
				Random rand = new Random(world.getRandom().nextLong());
				int ticks = (2 + 2 * potency) * stack.getCount();
				for (int i = 0; i < ticks; i++) {
					BlockPos pos = new BlockPos(ReikaRandomHelper.getRandomPlusMinus(x0, 2, rand),
							ReikaRandomHelper.getRandomPlusMinus(y0, 1, rand),
							ReikaRandomHelper.getRandomPlusMinus(z0, 2, rand));
					BlockState b = world.getBlockState(pos);
					if (this.canTickBlock(world, pos, b)) {
						BlockTickEvent.fire(b, world, pos, UpdateFlags.FORCED);
						ChromaNetwork.sendFertilitySeed(world, pos);
					}
				}
			}
		}
		else if (!ei.level().isClientSide() && age >= INITIAL_DELAY + 10) {
			ei.discard();
			return true;
		}
		return false;
	}

	private boolean canTickBlock(Level world, BlockPos pos, BlockState b) {
		Block block = b.getBlock();
		return block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM || block instanceof SaplingBlock
				|| ReikaCropHelper.isCrop(block) || ModCropList.isModCrop(world, pos, b);
	}

	@Override
	public boolean canBreak(ProgressiveBreaker b, Level world, BlockPos pos, Block id) {
		return id == Blocks.DIRT && world.getBlockState(pos.above()).getLightDampening() <= 2;
	}

	@Override
	public void onPreBreak(ProgressiveBreaker b, Level world, BlockPos pos, Block id) {

	}

	@Override
	public void onPostBreak(ProgressiveBreaker b, Level world, BlockPos pos, Block id) {
		world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
	}

	@Override
	public void onFinish(ProgressiveBreaker b) {

	}

	/** V33a's leaf-passthrough target: every leaf block, which the tag covers along with dye and glow leaves. */
	public static boolean passesThrough(BlockState state) {
		return state.is(BlockTags.LEAVES);
	}
}
