/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.tileentity.TileEntityCrystalPortal;
import reika.chromaticraft.world.dimension.ChromaTeleporter;
import reika.chromaticraft.world.dimension.CheatingPreventionSystem;
import reika.chromaticraft.world.dimension.DimensionTuningManager;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/**
 * V33a {@code BlockChromaPortal}: the Portal Rift, the nine-block pad at the bottom of the authored
 * portal structure that carries a qualified player to Proxima.
 *
 * <p>V33a distinguished the two rifts by metadata: metadata 15 targets the Overworld and validates
 * unconditionally, everything else targets Proxima and must match the whole multiblock. Those are
 * two different pieces of content, so this class backs two registry identities —
 * {@code chromaticraft:portal_rift} and {@code chromaticraft:return_portal_rift} — rather than a
 * property on one. The return form has no producer upstream either (nothing in V33a places metadata
 * 15; it is reachable only through commands/creative), and this port deliberately does not invent one.
 *
 * <p>Everything the rift does happens on entity contact. There is no collision box, no model and no
 * item interaction other than the Elemental Manipulator's dismantle.
 */
public class BlockChromaPortal extends Block implements EntityBlock, Portal {

	private final MapCodec<BlockChromaPortal> codec = MapCodec.unit(this);

	/** V33a metadata 15: destination is the Overworld and the structure check is skipped. */
	private final boolean returnPortal;

	public BlockChromaPortal(BlockBehaviour.Properties properties, boolean returnPortal) {
		super(properties);
		this.returnPortal = returnPortal;
	}

	public boolean isReturnPortal() {
		return returnPortal;
	}

	@Override
	public MapCodec<? extends BlockChromaPortal> codec() {
		return codec;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		// V33a hasTileEntity(int meta) returns an unconditional true — its "meta == 1" restriction is
		// commented out — so all nine pad blocks carry an entity and only the centre one works.
		return new TileEntityCrystalPortal(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		// Both halves are required: updateEntity() runs BlockEntityBase's lifecycle (first tick, sync,
		// callbacks) and updateEntity(level, pos) is this tile's own body. Calling only the first
		// leaves the rift permanently uncharged.
		return (world, pos, blockState, be) -> {
			if (be instanceof TileEntityCrystalPortal portal) {
				portal.updateEntity();
				portal.updateEntity(world, pos);
			}
		};
	}

	/** V33a getCollisionBoundingBoxFromPool returns null: you fall straight into the rift. */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}

	/**
	 * V33a onEntityCollidedWithBlock ran off the block's full cube even though it has no collision
	 * box, so contact is tested against the whole block.
	 */
	@Override
	protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			Entity entity) {
		return Shapes.block();
	}

	/** V33a isOpaqueCube/renderAsNormalBlock false with getRenderType() == -1: the BER draws it all. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	/**
	 * V33a onBlockActivated: the Elemental Manipulator dismantles the whole pad. It first puts out
	 * any fire in the 5x5 at the rift's own level, then flood-fills every connected Portal Rift
	 * within 16/8/16, clears each one and drops one Portal Rift item per removed block at the block
	 * that was clicked, and finally plays the rift tear-down and power-down cues.
	 *
	 * <p>{@code ownedBy} treats a portal with no recorded placer as owned by everyone, so a rift that
	 * was never placed by a player can be dismantled by anyone.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().is(ChromaItems.MANIPULATOR.get()))
			return InteractionResult.SUCCESS; // V33a returns true unconditionally: no block placement.
		if (level.isClientSide())
			return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof TileEntityCrystalPortal te) || !te.ownedBy(player))
			return InteractionResult.SUCCESS;

		for (int i = -2; i <= 2; i++) {
			for (int k = -2; k <= 2; k++) {
				BlockPos fire = pos.offset(i, 0, k);
				if (level.getBlockState(fire).is(Blocks.FIRE))
					level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
			}
		}

		for (BlockPos connected : this.collectPad(level, pos)) {
			level.setBlockAndUpdate(connected, Blocks.AIR.defaultBlockState());
			popResource(level, pos, new ItemStack(this));
		}
		ChromaSounds.RIFT.playSoundAtBlock(level, pos);
		ChromaSounds.POWERDOWN.playSoundAtBlock(level, pos);
		return InteractionResult.SUCCESS;
	}

	/** V33a BlockArray.recursiveAddWithBounds(world, x, y, z, this, +-16, +-8, +-16). */
	private Set<BlockPos> collectPad(Level level, BlockPos origin) {
		Set<BlockPos> found = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		found.add(origin);
		while (!queue.isEmpty()) {
			BlockPos current = queue.removeFirst();
			for (Direction dir : Direction.values()) {
				BlockPos next = current.relative(dir);
				if (Math.abs(next.getX() - origin.getX()) > 16 || Math.abs(next.getY() - origin.getY()) > 8
						|| Math.abs(next.getZ() - origin.getZ()) > 16)
					continue;
				if (found.contains(next) || !level.getBlockState(next).is(this))
					continue;
				found.add(next);
				queue.addLast(next);
			}
		}
		return found;
	}

	/**
	 * V33a onEntityCollidedWithBlock. Contact anywhere on the pad walks inward to the centre first:
	 * for each horizontal direction whose neighbour is not a rift, the block on the opposite side is
	 * — so the search steps away from the pad's edge — and the whole decision runs there instead. The
	 * source guarded the walk with a static visited set; a per-call set is the same guard without a
	 * field shared between worlds and threads.
	 */
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean precise) {
		BlockPos centre = walkInward(level, pos, new HashSet<>());
		if (level.isClientSide() || !(level.getBlockEntity(centre) instanceof TileEntityCrystalPortal te))
			return;
		if (entity instanceof Player player) {
			if (te.isComplete() && te.canPlayerUse(player))
				entity.setAsInsidePortal(this, centre);
			else
				denyEntity(level, entity);
		}
		else if (entity instanceof ItemEntity item) {
			ItemStack is = item.getItem();
			if (is.is(ChromaItems.TIERED.get(ChromaTieredItems.PROXIMAL_ESSENCE).get())
					|| is.is(ChromaItems.TIERED.get(ChromaTieredItems.PURE_PROXIMAL_ESSENCE).get())) {
				te.addTuningEnergy(is);
				item.discard();
			}
			else {
				denyEntity(level, entity);
			}
		}
		else {
			denyEntity(level, entity);
		}
	}

	private static BlockPos walkInward(Level level, BlockPos pos, Set<BlockPos> visited) {
		visited.add(pos);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(dir)).getBlock() instanceof BlockChromaPortal)
				continue;
			BlockPos opposite = pos.relative(dir.getOpposite());
			if (level.getBlockState(opposite).getBlock() instanceof BlockChromaPortal
					&& !visited.contains(opposite))
				return walkInward(level, opposite, visited);
		}
		return pos;
	}

	/**
	 * V33a denyEntity: anything the rift refuses is thrown 1.5 blocks per tick straight up with a
	 * random horizontal nudge, and has its fall distance forced to at least 500 so the landing is
	 * lethal. That is the anti-abuse mechanism, not a cosmetic bounce.
	 */
	private static void denyEntity(Level level, Entity e) {
		Vec3 motion = e.getDeltaMovement();
		e.setDeltaMovement(motion.x + (level.getRandom().nextDouble() * 2 - 1) * 0.25, 1.5,
				motion.z + (level.getRandom().nextDouble() * 2 - 1) * 0.25);
		e.fallDistance = Math.max(e.fallDistance, 500);
		e.hurtMarked = true;
		ChromaSounds.POWERDOWN.playSound(e);
	}

	/** V33a teleported on contact; the vanilla portal processor supplies the loop guard for free. */
	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		return 0;
	}

	@Override
	public Portal.Transition getLocalTransition() {
		return Portal.Transition.CONFUSION;
	}

	/**
	 * V33a teleportPlayer: pre-join bookkeeping, tune the player by the rift's stored tuning, hand
	 * them to {@link ChromaTeleporter} for the destination, then grant {@link ProgressStage#DIMENSION}
	 * and play whichever arrival cue applies. Sixty percent of the stored tuning is spent per trip.
	 */
	@Nullable
	@Override
	public TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity,
			BlockPos portalEntryPos) {
		if (!(entity instanceof ServerPlayer player))
			return null;
		if (!(currentLevel.getBlockEntity(portalEntryPos) instanceof TileEntityCrystalPortal te))
			return null;
		if (!te.isComplete() || !te.canPlayerUse(player))
			return null;
		ServerLevel target = currentLevel.getServer().getLevel(this.getTargetDimension());
		if (target == null)
			return null;

		CheatingPreventionSystem.instance.preJoin(player);
		DimensionTuningManager.instance.tunePlayer(player, te.consumeTuningForTrip());
		return ChromaTeleporter.arrivalTransition(target, player, transported -> {
			if (!(transported instanceof ServerPlayer arrived))
				return;
			// The first arrival is announced to the whole server; later ones only play locally, plus
			// the source's global DIMSOUND cue.
			if (ProgressStage.DIMENSION.stepPlayerTo(arrived))
				ChromaSounds.GOTODIM.broadcast(arrived.level().getServer(), 1, 1);
			else
				ChromaSounds.GOTODIM.playSound(arrived, 1, 1);
			CheatingPreventionSystem.instance.postJoin(arrived);
		});
	}

	/** V33a getTargetDimension: metadata 15 goes home, everything else goes to Proxima. */
	public ResourceKey<Level> getTargetDimension() {
		return returnPortal ? Level.OVERWORLD : ChromaDimensions.PROXIMA;
	}

	/**
	 * V33a isPortalFunctional: the background generators must have finished deciding Proxima's global
	 * layout, and the dimension itself has to be loadable at all.
	 */
	public static boolean isPortalFunctional(Level level) {
		return ProximaGenerators.areGeneratorsReady() && isDimensionLoadable(level);
	}

	public static boolean areGeneratorsReady(Level level) {
		return ProximaGenerators.areGeneratorsReady();
	}

	/**
	 * V33a {@code ChromatiCraft.isDimensionLoadable()} answered whether the configured dimension id
	 * was actually available. The 26.2 equivalent is whether the data-driven level exists on this
	 * server, which also covers a datapack having removed it.
	 */
	public static boolean isDimensionLoadable(Level level) {
		if (level == null || level.getServer() == null)
			return false;
		MinecraftServer server = level.getServer();
		return server.getLevel(ChromaDimensions.PROXIMA) != null;
	}

	/**
	 * V33a getPortalPosition: a 1-9 numpad code for where this block sits in its pad, 5 being the
	 * middle. It compares against the portal block identity only, so a pad mixing both rift types
	 * still reads as one pad — which is the upstream behaviour, since metadata was ignored here.
	 */
	public static int getPortalPosition(BlockGetter world, BlockPos pos) {
		boolean west = isPortal(world, pos.west());
		boolean east = isPortal(world, pos.east());
		boolean north = isPortal(world, pos.north());
		boolean south = isPortal(world, pos.south());
		if (!west)
			return !north ? 7 : !south ? 1 : 4;
		if (!east)
			return !north ? 9 : !south ? 3 : 6;
		return !north ? 8 : !south ? 2 : 5;
	}

	/**
	 * V33a isFull9x9, which despite its name checks the 3x3 at the same height. Renamed here so the
	 * next reader does not "fix" it into a 9x9 scan.
	 */
	public static boolean isFullPad(BlockGetter world, BlockPos pos) {
		for (int i = -1; i <= 1; i++)
			for (int k = -1; k <= 1; k++)
				if (!isPortal(world, pos.offset(i, 0, k)))
					return false;
		return true;
	}

	private static boolean isPortal(BlockGetter world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockChromaPortal;
	}

	/** Both rift identities, for the shared block entity type and for tag/model datagen. */
	public static Block[] allPortalBlocks() {
		return new Block[] { ChromaBlocks.PORTAL.get(), ChromaBlocks.RETURN_PORTAL.get() };
	}
}
