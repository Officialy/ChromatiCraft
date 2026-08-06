package reika.chromaticraft.block.worldgen26;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.magic.WarpNetwork;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.tileentity.aoe.TileEntityWarpNode;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

/**
 * V33a {@code BlockWarpNode}: a node you fall through, not one you click a destination on.
 *
 * <p>It has no collision and no model — the whole block is an invisible trigger. Walking into it
 * takes your current horizontal velocity, converts it to a bearing, and asks {@link WarpNetwork} for
 * the peer nearest that bearing within {@value #ANGLE_TOLERANCE} degrees. So you steer a jump by
 * choosing which direction you enter from, and a node with no peer on that heading simply does
 * nothing.
 *
 * <p>A node must first be opened with the Elemental Manipulator, which is what registers it in the
 * network. Until the LINK stage, arriving hurts: the chance of taking 1-8 magic damage falls as the
 * player progresses, from 5-in-6 down to 1-in-2 and then to nothing.
 */
public class BlockWarpNode extends Block implements EntityBlock {

	public static final double ANGLE_TOLERANCE = 15;

	/** V33a: 200 ticks between jumps, tracked per player rather than per node. */
	private static final long COOLDOWN = 200;
	private static final String COOLDOWN_TAG = "lastWarpNode";

	private final MapCodec<BlockWarpNode> codec = MapCodec.unit(this);

	public BlockWarpNode(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends BlockWarpNode> codec() {
		return codec;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityWarpNode(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
			Level level, BlockState state, BlockEntityType<T> type) {
		return null; // V33a canUpdate() returns false.
	}

	/** V33a getCollisionBoundingBoxFromPool returns null: you walk straight into it. */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}

	/** V33a onBlockActivated: only the Manipulator opens a node. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		// CHROMA-PORT: V33a gates this on HoldingChecks.MANIPULATOR, which is not in the compile
		// slice yet; the check it performs is exactly "is this player holding an Elemental
		// Manipulator", which is what is inlined here until that class lands.
		if (level.isClientSide() || !player.getMainHandItem().is(ChromaItems.MANIPULATOR.get()))
			return InteractionResult.PASS;
		if (level.getBlockEntity(pos) instanceof TileEntityWarpNode node) {
			node.open();
			ProgressStage.WARPNODE.stepPlayerTo(player);
			// CHROMA-PORT: V33a also plays ChromaSounds.USE at the node here.
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** V33a onEntityCollidedWithBlock: entering the node is what triggers the jump. */
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (!(entity instanceof ServerPlayer player))
			return;
		long last = player.getPersistentData().getLongOr(COOLDOWN_TAG, 0);
		if (level.getGameTime() - last <= COOLDOWN)
			return;
		double vx = player.getX() - player.xo;
		double vz = player.getZ() - player.zo;
		if (vx == 0 && vz == 0)
			return;
		double angle = ReikaPhysicsHelper.cartesianToPolar(vx, 0, vz)[2];
		WorldLocation target = WarpNetwork.instance.getLink(
				new WorldLocation(level, pos.getX(), pos.getY(), pos.getZ()), angle, ANGLE_TOLERANCE);
		if (target == null)
			return;
		this.teleportPlayer(player, target);
		player.getPersistentData().putLong(COOLDOWN_TAG, level.getGameTime());
	}

	private void teleportPlayer(ServerPlayer player, WorldLocation target) {
		player.teleportTo(target.pos.getX() + 0.5, target.pos.getY() + 0.5, target.pos.getZ() + 0.5);
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(
				motion.x + (player.getRandom().nextDouble() * 1.5 - 0.75),
				motion.y + (player.getRandom().nextDouble() * 1.5 - 0.75),
				motion.z + (player.getRandom().nextDouble() * 1.5 - 0.75));
		player.hurtMarked = true;
		// V33a: the ride is rough until you have LINK, and steadily less so as you progress.
		if (!player.isCreative() && !ProgressStage.LINK.isPlayerAtStage(player)) {
			int odds = ProgressStage.USEENERGY.isPlayerAtStage(player) ? 2
					: ProgressStage.ALLCOLORS.isPlayerAtStage(player) ? 3 : 6;
			if (player.getRandom().nextInt(odds) > 0)
				player.hurt(player.damageSources().magic(), 1 + player.getRandom().nextInt(8));
		}
		ProgressStage.WARPNODE.stepPlayerTo(player);
	}
}
