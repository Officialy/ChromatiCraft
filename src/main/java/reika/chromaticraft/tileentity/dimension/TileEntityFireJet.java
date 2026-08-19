package reika.chromaticraft.tileentity.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a's {@code DimensionDecoTile} as the Fire Jet uses it: a countdown, and the flame it throws while
 * that countdown runs.
 *
 * <p>{@code activate()} sets a hundred to thirteen hundred ticks — one to about four minutes, and the
 * spread is wide on purpose so a field of jets never falls into step — and plays the vanilla ignite
 * sound. The block re-arms it every four hundred ticks or so, meaning a jet is lit far more often than
 * it is dark.
 *
 * <p>The countdown is persisted, so a jet that was burning when the world saved is still burning when
 * it loads rather than restarting its cycle.
 */
public class TileEntityFireJet extends TileEntityChromaticBase {

	private int tick;

	public TileEntityFireJet(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.FIRE_JET.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.FIREJET;
	}

	/** V33a activate: a fresh countdown, a sync, and the ignite. */
	public void activate() {
		Level world = this.getLevel();
		if (world == null || world.isClientSide())
			return;
		tick = 100 + world.getRandom().nextInt(1200);
		this.syncAllData(false);
		world.playSound(null, this.getBlockPos(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
				1, 1);
	}

	public boolean isLit() {
		return tick > 0;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (tick <= 0)
			return;
		// The countdown is the server's; the client runs its own copy off the synced value so the
		// flame does not wait on a packet per tick.
		tick--;
	}

	/**
	 * V33a spawnParticles for FIREJET: one rising blur every other tick, hue-cycling with position and
	 * time — unless a crystal block sits underneath, in which case the jet burns that element's colour.
	 * That is the detail worth keeping: a jet over a crystal is how the pools read as coloured.
	 */
	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (tick > 0)
			ChromaParticle.spawnFireJet(world, pos);
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);
		NBT.putInt("tick", tick);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);
		tick = NBT.getIntOr("tick", 0);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.putInt("tick", tick);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		tick = NBT.getIntOr("tick", 0);
	}
}
