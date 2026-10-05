package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * V33a {@code BlockSparkle}: an ordinary terrain block with glinting white motes on its faces, one
 * registered block per former metadata type.
 *
 * <p>Each type stands in for a vanilla "proxy" block and borrows everything from it: its texture, its
 * hardness and blast resistance, and its mining speed for a given tool. It does not borrow the proxy's
 * need for a correct tool. Upstream's material was {@code Material.ground}, which never needs one, so a
 * Sparkling Obsidian mined by hand is as slow as obsidian mined by hand but still drops. The two types
 * whose proxies fall (sand and gravel) fall too, on the same two-tick delay upstream scheduled.
 *
 * <p>Sparkling Stone drops Sparkling Cobblestone, as stone drops cobblestone. Smelting takes Sparkling
 * Clay to Sparkling Bricks and Sparkling Cobblestone back to Sparkling Stone.
 */
public final class BlockSparkle extends Block implements Fallable {

	/** V33a {@code BlockSparkle.BlockTypes}, in upstream's metadata order. */
	public enum Type {
		DIRT(Blocks.DIRT),
		SAND(Blocks.SAND),
		GRAVEL(Blocks.GRAVEL),
		COBBLE(Blocks.COBBLESTONE),
		STONE(Blocks.STONE),
		CLAY(Blocks.CLAY),
		BRICKS(Blocks.BRICKS),
		OBSIDIAN(Blocks.OBSIDIAN);

		public static final Type[] list = values();

		private final Block proxy;

		Type(Block proxy) {
			this.proxy = proxy;
		}

		public Block getBlockProxy() {
			return proxy;
		}

		/** V33a {@code canFall}: the proxy is a falling block. */
		public boolean canFall() {
			return proxy instanceof FallingBlock;
		}

		/** V33a {@code damageDropped}: stone breaks to cobblestone, everything else to itself. */
		public Type getDropType() {
			return this == STONE ? COBBLE : this;
		}

		public String registryName() {
			return "sparkle_" + name().toLowerCase(Locale.ROOT);
		}

		/** V33a {@code BlockTypes.getByProxy}. */
		public static Type getByProxy(Block block) {
			for (Type type : list)
				if (type.proxy == block)
					return type;
			return null;
		}
	}

	/** V33a {@code tickRate}. */
	private static final int FALL_DELAY = 2;

	private final Type type;


	public BlockSparkle(BlockBehaviour.Properties properties, Type type) {
		super(properties);
		this.type = type;
	}

	public Type getType() {
		return type;
	}


	/**
	 * V33a {@code getPlayerRelativeBlockHardness}: the proxy's own answer, so tool choice and the
	 * no-correct-tool penalty are the proxy's.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return type.getBlockProxy().defaultBlockState().getDestroyProgress(player, level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (type.canFall())
			level.scheduleTick(pos, this, FALL_DELAY);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (type.canFall())
			ticks.scheduleTick(pos, this, FALL_DELAY);
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	/** V33a {@code updateTick}, through vanilla's own falling-block rules. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (type.canFall() && FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY())
			FallingBlockEntity.fall(level, pos, state);
	}
}
