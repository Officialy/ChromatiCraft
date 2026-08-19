package reika.chromaticraft.client;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import reika.chromaticraft.registry.CrystalElement;

/**
 * Item-side counterparts of the block tint sources in {@link ChromaBlockColors}. In 26.x an item's
 * tint is data-driven: the item model JSON declares its tint sources and the codecs are registered
 * from {@code ChromaBlockColors.registerItemColors}, so a tint that varies per stack has to be its
 * own registered type rather than a colour baked into datagen.
 */
public final class ChromaItemTints {

	private ChromaItemTints() {}

	/**
	 * The Dimension Core's colour, read from the stack's own {@code color} tag — the same tag
	 * {@code TileEntityDimensionCore.setDataFromItemStackTag} reads when the block is placed, so a
	 * core looks in the hand exactly as it will look in the ring.
	 *
	 * <p>V33a never needed this: its core is drawn entirely by {@code RenderDimensionCore} and has no
	 * item icon of its own. The sixteen cores are only distinguishable in an inventory because of it.
	 */
	public record DimensionCoreTint() implements ItemTintSource {

		public static final MapCodec<DimensionCoreTint> CODEC = MapCodec.unit(new DimensionCoreTint());

		@Override
		public int calculate(ItemStack stack, ClientLevel level, LivingEntity holder) {
			var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			CrystalElement element = tag == null ? CrystalElement.WHITE
					: CrystalElement.elements[tag.getIntOr("color", CrystalElement.WHITE.ordinal())];
			return 0xFF000000 | element.getColor();
		}

		@Override
		public MapCodec<DimensionCoreTint> type() {
			return CODEC;
		}
	}
}
