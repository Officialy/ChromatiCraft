package reika.chromaticraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.registry.CrystalElement;

/**
 * BlockItem for one {@link BlockCrystalRune} colour. One block + the {@link BlockCrystalRune#COLOR}
 * property + one of these per {@link CrystalElement} (one-block-many-items, per crystalline stone's
 * {@code BlockItemPylonStructure}). Parameterised by colour; each places its own COLOR state and shows
 * its own name.
 */
public class BlockItemCrystalRune extends BlockItem {

	private final int color;
	private final String descriptionId;

	public BlockItemCrystalRune(Block block, int color, Item.Properties props) {
		super(block, props);
		this.color = color;
		this.descriptionId = "block.chromaticraft.rune_"
				+ CrystalElement.elements[color].name().toLowerCase(java.util.Locale.ENGLISH);
	}

	@Override
	protected BlockState getPlacementState(BlockPlaceContext ctx) {
		BlockState base = super.getPlacementState(ctx);
		return base == null ? null : base.setValue(BlockCrystalRune.COLOR, this.color);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.descriptionId);
	}
}
