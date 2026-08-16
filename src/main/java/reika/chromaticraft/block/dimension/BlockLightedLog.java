package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * V33a {@code BlockLightedLog}: the trunk of Proxima's glowing trees.
 *
 * <p>Upstream extends vanilla's log, so it keeps the axis a log is placed along and behaves like wood
 * for tools and fuel. Its own contribution is the glow: it draws vanilla oak log in pass 0 and
 * {@code dimgen/glowlog-light} over it in pass 1. That overlay is the same deferred second-pass
 * rendering the decoration blocks are waiting on, so the block currently draws upstream's own pass-0
 * base without it. Unlike the leaves, the log emits no light of its own — the glow is the canopy's.
 */
public class BlockLightedLog extends RotatedPillarBlock {

	private final MapCodec<BlockLightedLog> codec = MapCodec.unit(this);

	public BlockLightedLog(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends BlockLightedLog> codec() {
		return codec;
	}
}
