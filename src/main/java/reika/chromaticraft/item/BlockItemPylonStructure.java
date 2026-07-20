package reika.chromaticraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockPylonStructure;

/**
 * BlockItem for one crystalline-stone ({@link BlockPylonStructure}) variant. Crystalline stone is one
 * block with the {@link BlockPylonStructure#TYPE} property (0..15) and one of these per variant, each
 * placing its own {@code TYPE} state — the one-block-many-items convention (see GeoStrata's
 * {@code BlockItemLavaRock}). Parameterised by variant rather than 16 subclasses.
 */
public class BlockItemPylonStructure extends BlockItem {

	private final int variant;
	private final String descriptionId;

	public BlockItemPylonStructure(Block block, int variant, Item.Properties props) {
		super(block, props);
		this.variant = variant;
		// A BlockItem's name comes from its block's single description id; give each variant its own
		// translation key so the 16 items show their distinct names (Crystalline Stone Beam, Column, …).
		this.descriptionId = "block.chromaticraft.pylon_structure_"
				+ BlockPylonStructure.StoneTypes.list[variant].name().toLowerCase(java.util.Locale.ENGLISH);
	}

	@Override
	protected BlockState getPlacementState(BlockPlaceContext ctx) {
		BlockState base = super.getPlacementState(ctx);
		return base == null ? null : base.setValue(BlockPylonStructure.TYPE, this.variant);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.descriptionId);
	}
}
