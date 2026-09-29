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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

import reika.chromaticraft.block.BlockCrystalPlant;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant.Modifier;

/**
 * V33a {@code ItemCrystalSeeds} ("Crystal Seeds"), one item per element where V33a used {@code meta % 16}.
 *
 * <p>V33a packed the plant modifiers into the metadata above the colour ({@link Modifier#flag}: 16 Impure, 32
 * Enriched, 64 Primal) and marked decorative seeds with an NBT {@code sterile} flag. Both now live in
 * {@code CUSTOM_DATA} under {@value #FLAGS_KEY} and {@value #STERILE_KEY}, written only when set, so a plain
 * pure seed has no data at all and every source of one stacks with every other.
 */
public class ItemCrystalSeeds extends Item {

	public static final String FLAGS_KEY = "flags";
	public static final String STERILE_KEY = "sterile";

	private final CrystalElement element;

	public ItemCrystalSeeds(CrystalElement element, Item.Properties properties) {
		super(properties);
		this.element = element;
	}

	public CrystalElement getElement() {
		return element;
	}

	/** A seed of {@code element} carrying V33a modifier bits {@code flags} and, if asked, sterility. */
	public static ItemStack stack(CrystalElement element, int flags, boolean sterile, int count) {
		ItemStack is = new ItemStack(ChromaItems.CRYSTAL_SEEDS.get(element).get(), count);
		if (flags != 0 || sterile) {
			CompoundTag tag = new CompoundTag();
			if (flags != 0)
				tag.putInt(FLAGS_KEY, flags);
			if (sterile)
				tag.putBoolean(STERILE_KEY, true);
			is.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
		return is;
	}

	/** V33a {@code is.getItemDamage() & ~15}: the modifier bits. */
	public static int getFlags(ItemStack is) {
		return is.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(FLAGS_KEY, 0);
	}

	/** V33a {@code stackTagCompound.getBoolean("sterile")}. */
	public static boolean isSterile(ItemStack is) {
		return is.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(STERILE_KEY, false);
	}

	/** V33a {@code isMetaInCreative}: plain, then each single modifier V33a showed (Primal was hidden). */
	public static boolean showsInCreative(Modifier m) {
		return m.showsInCreative();
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		// V33a onItemUse: plant into the clicked block if it is soft, else into the block on the clicked side.
		if (!world.getBlockState(pos).canBeReplaced())
			pos = pos.relative(context.getClickedFace());
		if (!world.getBlockState(pos).canBeReplaced() || !BlockCrystalPlant.canGrowOn(world.getBlockState(pos.below())))
			return InteractionResult.PASS;
		ItemStack item = context.getItemInHand();
		Player player = context.getPlayer();
		if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), item))
			return InteractionResult.PASS;
		if (!world.isClientSide()) {
			world.setBlock(pos, ChromaBlocks.crystalPlant(element).get().defaultBlockState(), 3);
			if (world.getBlockEntity(pos) instanceof TileEntityCrystalPlant te)
				te.setStates(item);
			// DragonAPI playPlaceSound(Blocks.grass): the grass step sound's place variant, as block placement plays it.
			SoundType sound = SoundType.GRASS;
			world.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
					(sound.getVolume()+1.0F)/2.0F, sound.getPitch()*0.8F);
		}
		item.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		int flags = getFlags(is);
		for (int i = 0; i < Modifier.list.length; i++) {
			Modifier m = Modifier.list[i];
			if (m.present(flags)) {
				li.accept(Component.translatable(m.translationKey()));
			}
		}
		if (isSterile(is)) {
			li.accept(Component.translatable("item.chromaticraft.crystal_seeds.sterile"));
		}
	}

	/** V33a {@code getAllVariants}: the plain seed and one per modifier. */
	public static Collection<ItemStack> getAllVariants(CrystalElement e) {
		Collection<ItemStack> ret = new ArrayList<>();
		ret.add(stack(e, 0, false, 1));
		for (Modifier m : Modifier.list)
			ret.add(stack(e, m.flag, false, 1));
		return ret;
	}

	/**
	 * V33a creative listing: its metadata order put all sixteen plain seeds first, then each shown modifier's
	 * sixteen.
	 */
	public static List<ItemStack> creativeStacks() {
		List<ItemStack> ret = new ArrayList<>();
		for (CrystalElement e : CrystalElement.elements)
			ret.add(stack(e, 0, false, 1));
		for (Modifier m : Modifier.list) {
			if (!showsInCreative(m))
				continue;
			for (CrystalElement e : CrystalElement.elements)
				ret.add(stack(e, m.flag, false, 1));
		}
		return ret;
	}

}
