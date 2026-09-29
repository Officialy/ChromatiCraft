package reika.chromaticraft.tileentity.transport;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.EnumMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

import reika.chromaticraft.base.tileentity.TileEntityAreaDistributor;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;

/**
 * V33a Fluid Radiator: receives fluid on any face and immediately distributes it to accessible tanks below it.
 * It keeps the original per-face target probing and input exclusion. NeoForge's caller transaction is carried into
 * each destination insert, so a simulated or rolled-back upstream transfer does not consume fluid downstream.
 */
public class TileEntityFluidDistributor extends TileEntityAreaDistributor {
	private static final Set<Class<?>> BLACKLIST = new HashSet<>();
	private final EnumMap<Direction, ResourceHandler<FluidResource>> inputs = new EnumMap<>(Direction.class);
	private final TrailJournal trails = new TrailJournal();

	public TileEntityFluidDistributor(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.FLUID_DISTRIBUTOR.get(), pos, state);
		for (Direction side : Direction.values())
			inputs.put(side, new InputHandler(side));
	}

	public ResourceHandler<FluidResource> fluidHandler(Direction side) {
		return inputs.get(side != null ? side : Direction.DOWN);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.FLUIDDISTRIBUTOR;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (world.getRandom().nextInt(3) == 0) {
			double x = pos.getX() + world.getRandom().nextDouble();
			double y = pos.getY() + world.getRandom().nextDouble();
			double z = pos.getZ() + world.getRandom().nextDouble();
			world.addParticle(ParticleTypes.ENCHANT, x, y, z,
					(world.getRandom().nextDouble() - 0.5) * 0.06,
					(world.getRandom().nextDouble() - 0.5) * 0.06,
					(world.getRandom().nextDouble() - 0.5) * 0.06);
		}
	}

	@Override
	protected boolean isValidTarget(BlockEntity te) {
		if (te == null || te == this || te.getBlockPos().getY() >= worldPosition.getY())
			return false;
		// The original explicitly excluded World Rifts. Its port will join the active source set later.
		if (te.getClass().getName().equals("reika.chromaticraft.tileentity.transport.TileEntityRift"))
			return false;
		Class<?> type = te.getClass();
		if (BLACKLIST.contains(type))
			return false;
		String name = type.getName().toLowerCase(Locale.ROOT);
		if (name.contains("conduit") || name.contains("duct") || name.contains("cable") || name.contains("pipe")
				|| name.contains("tesseract") || name.contains("hypercube")) {
			BLACKLIST.add(type);
			return false;
		}
		Level world = te.getLevel();
		if (world == null || !world.isLoaded(te.getBlockPos()))
			return false;
		for (Direction side : Direction.values()) {
			if (world.getCapability(Capabilities.Fluid.BLOCK, te.getBlockPos(), side) != null)
				return true;
		}
		return false;
	}

	private int distribute(FluidResource fluid, int amount, Direction input, TransactionContext transaction) {
		if (level == null || level.isClientSide() || this.hasRedstoneSignal() || fluid.isEmpty() || amount <= 0)
			return 0;
		addInput(worldPosition.relative(input));
		int accepted = 0;
		Iterator<BlockPos> targets = this.getTargets();
		while (targets.hasNext() && accepted < amount) {
			BlockPos target = targets.next();
			if (!level.isLoaded(target))
				continue;
			BlockEntity te = level.getBlockEntity(target);
			if (!this.isValidTarget(te)) {
				targets.remove();
				continue;
			}
			int moved = 0;
			for (Direction side : Direction.values()) {
				ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, target, side);
				if (handler == null)
					continue;
				int inserted = handler.insert(fluid, amount - accepted, transaction);
				accepted += inserted;
				moved += inserted;
				if (accepted >= amount)
					break;
			}
			if (moved > 0)
				trails.add(target, moved, transaction);
		}
		return accepted;
	}

	private void sendFluidTrail(ServerLevel world, BlockPos target, int amount) {
		if (world.getRandom().nextInt(3) != 0)
			return;
		for (int i = 0; i <= 8; i++) {
			double t = i / 8D;
			double x = worldPosition.getX() + 0.5 + (target.getX() - worldPosition.getX()) * t;
			double y = worldPosition.getY() + 0.5 + (target.getY() - worldPosition.getY()) * t + Math.sin(t * Math.PI);
			double z = worldPosition.getZ() + 0.5 + (target.getZ() - worldPosition.getZ()) * t;
			world.sendParticles(ParticleTypes.SPLASH, x, y, z, Math.min(4, 1 + amount / 1000), 0.04, 0.04, 0.04, 0.01);
		}
	}

	private record Trail(BlockPos target, int amount) {}

	/** Visual effects only become visible if the caller commits the fluid transfer. */
	private final class TrailJournal extends SnapshotJournal<ArrayList<Trail>> {
		private ArrayList<Trail> pending = new ArrayList<>();

		private void add(BlockPos target, int amount, TransactionContext transaction) {
			this.updateSnapshots(transaction);
			pending.add(new Trail(target.immutable(), amount));
		}

		@Override protected ArrayList<Trail> createSnapshot() { return new ArrayList<>(pending); }
		@Override protected void revertToSnapshot(ArrayList<Trail> snapshot) { pending = snapshot; }
		@Override protected void onRootCommit(ArrayList<Trail> originalState) {
			if (level instanceof ServerLevel server) {
				for (Trail trail : pending)
					sendFluidTrail(server, trail.target(), trail.amount());
			}
			pending.clear();
		}
	}

	private final class InputHandler implements ResourceHandler<FluidResource> {
		private final Direction side;

		private InputHandler(Direction side) {
			this.side = side;
		}

		@Override public int size() { return 1; }
		@Override public FluidResource getResource(int index) { return FluidResource.EMPTY; }
		@Override public long getAmountAsLong(int index) { return 0; }
		@Override public long getCapacityAsLong(int index, FluidResource resource) {
			return index == 0 && this.isValid(index, resource) ? Integer.MAX_VALUE : 0;
		}
		@Override public boolean isValid(int index, FluidResource resource) {
			return index == 0 && !resource.isEmpty();
		}
		@Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			return index == 0 ? distribute(resource, amount, side, transaction) : 0;
		}
		@Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			return 0;
		}
	}
}
