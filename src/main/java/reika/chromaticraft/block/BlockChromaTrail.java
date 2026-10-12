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

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import reika.chromaticraft.auxiliary.HoldingChecks;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;
import reika.dragonapi.instantiable.math.Spline.BasicVariablePoint;
import reika.dragonapi.instantiable.math.Spline.SplineType;

import java.util.ArrayList;
import java.util.List;

/**
 * V33a {@code BlockChromaTrail}: one waypoint of a Subterranean Route Finder's trail -- invisible, collisionless, a small
 * selection cube at the centre -- each remembering its neighbours, so the whole route can be found from any point and
 * cleared by using a Manipulator on it. Its tile draws the route as blurs gliding along a spline through the waypoints,
 * tinted by height like ethereal light, with a haze around each point while a Manipulator is held.
 */
public class BlockChromaTrail extends Block implements EntityBlock {

	private static final VoxelShape SHAPE = box(6, 6, 6, 10, 10, 10);

	public BlockChromaTrail(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileChromaTrail(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() && type == ChromaBlockEntities.CHROMA_TRAIL.get()
				? (lvl, pos, st, te) -> ((TileChromaTrail)te).clientTick() : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player ep,
			InteractionHand hand, BlockHitResult hit) {
		if (HoldingChecks.MANIPULATOR.isHolding(ep)) {
			if (world.getBlockEntity(pos) instanceof TileChromaTrail te) {
				for (BlockPos loc : te.getFullPath()) {
					world.setBlock(loc, Blocks.AIR.defaultBlockState(), 3);
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public static class TileChromaTrail extends BlockEntity {

		private BlockPos nextLocation;
		private BlockPos prevLocation;

		private int pathIndex;
		private List<DecimalPosition> overallPath;

		public TileChromaTrail(BlockPos pos, BlockState state) {
			super(ChromaBlockEntities.CHROMA_TRAIL.get(), pos, state);
		}

		private void clientTick() {
			ChromaParticle.spawnChromaTrail(this.getLevel(), this.getBlockPos(), this);
		}

		public void setData(BlockPos prev, BlockPos next) {
			prevLocation = prev;
			nextLocation = next;
			overallPath = null;
			this.setChanged();
			if (this.getLevel() != null)
				this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
		}

		/** This point's stretch of the spline through the whole route, computed once. */
		public List<DecimalPosition> getSplineSegment() {
			if (overallPath == null)
				overallPath = this.calcSpline();
			return overallPath;
		}

		public int getPathIndex() {
			return pathIndex;
		}

		private List<DecimalPosition> calcSpline() {
			ArrayList<BlockPos> path = this.getFullPath();
			pathIndex = path.indexOf(this.getBlockPos());
			Spline s = new Spline(SplineType.CENTRIPETAL);
			for (BlockPos c : path) {
				s.addPoint(new BasicVariablePoint(new DecimalPosition(c.getX()+0.5, c.getY()+0.5, c.getZ()+0.5), 0.75, 0.0625));
			}
			List<DecimalPosition> all = s.get(128, false);
			int size = all.size()/path.size();
			int idx = pathIndex*size;
			return all.subList(idx, idx+size);
		}

        /** Broken/unloaded links end the trail; overlapping routes must never loop forever. */
        public ArrayList<BlockPos> getFullPath() {
            java.util.ArrayDeque<BlockPos> path = new java.util.ArrayDeque<>();
            java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
            path.add(getBlockPos()); seen.add(getBlockPos());
            if (getLevel() == null) return new ArrayList<>(path);
            collectPath(prevLocation, true, path, seen);
            collectPath(nextLocation, false, path, seen);
            return new ArrayList<>(path);
        }
        private void collectPath(BlockPos pos, boolean backwards, java.util.ArrayDeque<BlockPos> path,
                java.util.Set<BlockPos> seen) {
            while (pos != null && seen.add(pos) && getLevel().hasChunkAt(pos)) {
                if (!(getLevel().getBlockEntity(pos) instanceof TileChromaTrail trail)) break;
                if (backwards) path.addFirst(pos); else path.addLast(pos);
                pos = backwards ? trail.prevLocation : trail.nextLocation;
            }
        }

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			if (nextLocation != null)
				output.store("next", BlockPos.CODEC, nextLocation);
			if (prevLocation != null)
				output.store("prev", BlockPos.CODEC, prevLocation);
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			nextLocation = input.read("next", BlockPos.CODEC).orElse(null);
			prevLocation = input.read("prev", BlockPos.CODEC).orElse(null);
			overallPath = null;
		}

		@Override
		public Packet<ClientGamePacketListener> getUpdatePacket() {
			return ClientboundBlockEntityDataPacket.create(this);
		}

		@Override
		public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
			return this.saveCustomOnly(registries);
		}

	}

}
