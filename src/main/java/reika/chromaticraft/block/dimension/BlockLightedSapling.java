package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * V33a {@code BlockLightedSapling}: the sapling a Glowing Leaf drops, and the only way to plant a
 * glowing tree by hand.
 *
 * <p>It emits light 9, three less than the canopy it grows into, and survives on the same soils any
 * sapling does. Placement, survival, light and drops are complete — this is what the Glow Cave and
 * Dungeon chest injections in {@code ChromaChestLoot} were waiting on.
 *
 * <p>What it does not do yet is grow. Upstream's {@code func_149878_d} picks one of
 * {@code WorldGenLightedTree.TreeGen}'s layouts at random, and that generator is not ported, so this
 * extends {@link VegetationBlock} rather than vanilla's {@code BushBlock}: the latter is
 * {@code BonemealableBlock}, and inheriting that would advertise a growth this cannot perform. When
 * the tree generator lands, moving to a bonemealable base and driving it is the whole change.
 */
public class BlockLightedSapling extends VegetationBlock {

	private final MapCodec<BlockLightedSapling> codec = MapCodec.unit(this);

	public BlockLightedSapling(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends BlockLightedSapling> codec() {
		return codec;
	}
}
