package reika.chromaticraft.block.dye26;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;

/**
 * V33a {@code BlockDyeFlower}: the Rainbow Forest's flowers, one registered block per former metadata
 * colour.
 *
 * <p>Upstream is a plain {@code BlockFlower} with one greyscale texture ({@code dye/flower}) coloured per
 * dye. In the world that colour is the dye's {@code getJavaColor().brighter()}; as an item it is the
 * matching crystal colour, because {@code ItemBlockDyeTypes} overrides the item tint for every dye block
 * except the dye block and the leaves. Both answers are kept.
 *
 * <p>It is a Plains plant, so it stands on grass, dirt (1.7.10's dirt metadata covered coarse dirt and
 * podzol) or farmland. The damaged-forest branch of {@code colorMultiplier} is unreachable: V33a's
 * {@code BiomeRainbowForest.isDamaged} always returns false.
 */
public final class BlockDyeFlower extends VegetationBlock {

	/** V33a BlockBush default bounds: {@code f = 0.2}, so 0.3-0.7 wide and 0.6 tall. */
	private static final VoxelShape SHAPE = Block.box(4.8, 0, 4.8, 11.2, 9.6, 11.2);

	private final CrystalElement element;


	public BlockDyeFlower(BlockBehaviour.Properties properties, CrystalElement element) {
		super(properties);
		this.element = element;
	}

	public CrystalElement getElement() {
		return element;
	}


	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Forge 1.7.10's {@code EnumPlantType.Plains}: grass, dirt or farmland. */
	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return isPlainsSoil(state);
	}

	public static boolean isPlainsSoil(BlockState state) {
		return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
				|| state.is(Blocks.PODZOL) || state.is(Blocks.FARMLAND);
	}

	/** V33a {@code colorMultiplier}: the dye colour brightened. */
	public int getTintColor() {
		return 0xFF000000 | ReikaDyeHelper.dyes[element.ordinal()].getJavaColor().brighter().getRGB();
	}
}
