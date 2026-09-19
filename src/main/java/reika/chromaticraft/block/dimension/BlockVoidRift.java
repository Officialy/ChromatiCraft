package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.dimension.TileEntityVoidRift;
import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a's Void Rift, registered once per crystal element.
 *
 * <p>The seam a fissure opens at its floor. Upstream's block carries its colour in metadata; here each
 * element is its own registry identity, as with every other colour-carrying block in this port.
 *
 * <p>It is doubly gated against being mined: {@code getPlayerRelativeBlockHardness} returns -1 — which
 * 26.2 expresses as a destroy progress of zero — unless the player is both past {@code ProgressStage.CTM}
 * <em>and</em> sufficiently tuned to the dimension for {@code DECOHARVEST}. Its blast resistance of
 * 900,000 makes the other route in equally hopeless. It is deliberately not unbreakable: upstream's
 * {@code setBlockUnbreakable()} is commented out and the hardness of 10 left in its place.
 *
 * <h2>What it looks like</h2>
 *
 * <p>A plain cube, and that is faithful. {@code VoidRiftRenderer}'s whole aura pass is commented out in
 * V33a — the live path is one {@code renderStandardBlockWithAmbientOcclusion} call — so the block wears
 * {@code dimgen/voidrift} on its top and Stone Shielding on every other face, exactly as
 * {@code getIcon} says.
 *
	 * <p>The coloured aura walls use V33a's shipped fallback copy of its remotely sourced full-size
	 * atlas. The exact frame UVs, neighbour seams and mixed-colour joins are preserved by the BER.
 */
public class BlockVoidRift extends BaseEntityBlock {

	public static final MapCodec<BlockVoidRift> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					propertiesCodec(),
					CrystalElement.CODEC.fieldOf("element").forGetter(BlockVoidRift::getElement)
			).apply(instance, BlockVoidRift::new));

	private final CrystalElement element;

	public BlockVoidRift(BlockBehaviour.Properties properties, CrystalElement element) {
		super(properties);
		this.element = element;
	}

	public CrystalElement getElement() {
		return element;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityVoidRift(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			@Nullable Orientation orientation, boolean movedByPiston) {
		if (level.getBlockEntity(pos) instanceof TileEntityVoidRift rift)
			rift.clearNeighbourCache();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (level.getBlockEntity(pos.relative(direction)) instanceof TileEntityVoidRift adjacent)
				adjacent.clearNeighbourCache();
		}
	}

	/**
	 * V33a getPlayerRelativeBlockHardness: both gates, in upstream's own order. Expressed as destroy
	 * progress rather than on a break event so it also covers explosions and other mods' miners.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (!DimensionTuningManager.TuningThresholds.DECOHARVEST.isSufficientlyTuned(player))
			return 0;
		return ProgressStage.CTM.isPlayerAtStage(player)
				? super.getDestroyProgress(state, player, level, pos) : 0;
	}
}
