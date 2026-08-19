package reika.chromaticraft.tileentity.aoe;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.base.tileentity.TileEntityLocusPoint;
import reika.dragonapi.libraries.ReikaEntityHelper;

/**
 * V33a {@code TileEntityAuraPoint}: what the monument becomes once its ritual completes.
 *
 * <p>It is a standing area effect keyed to the player who made it. Hostile things inside its reach are
 * struck; friendly things are healed; both radii, and the looting the strikes carry, grow with the
 * point's own age rather than with anything the player does afterwards. That growth is the whole
 * character of it — a fresh point defends eight blocks, an old one ninety-six — so the age formulas are
 * carried exactly:
 *
 * <ul>
 * <li>attack radius {@code min(8 + age/16, 96)}</li>
 * <li>heal radius {@code min(4 + age/64, 32)}</li>
 * <li>looting {@code min(6, age/288000)} — one level per four hours, capped at six after a day</li>
 * </ul>
 *
 * <p>Both sweeps use a column of the full world height rather than a sphere, which is upstream's own
 * shape and matters: a point defends the sky above it as readily as the ground around it.
 *
 * <p>Players are neutral until they make themselves hostile — {@link #markHostile} is called when one
 * attacks the owner — and the PVP toggle turns that off wholesale, clearing the hostile set with it so
 * flipping it back does not resurrect old grudges.
 *
 * <h2>What is deferred</h2>
 *
 * <p>Upstream's strike goes through {@code ChromaAux.doPylonAttack}, which is not ported. The damage it
 * computes is carried here exactly and applied the way the ported pylon applies its own strike; what is
 * not yet carried is that helper's taper and progress-granting flags, and the looting level is computed
 * and held rather than reaching the drop roll. The aura kills what it should, for the right amount.
 */
public class TileEntityAuraPoint extends TileEntityLocusPoint {

	/** V33a hue drift: the point's colour wanders rather than sitting on one value. */
	private int hue;
	private float saturation;
	private int hueTarget;
	private float saturationTarget;

	private final Set<UUID> hostilePlayers = new HashSet<>();
	private boolean doPVP = true;

	public TileEntityAuraPoint(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.AURA_POINT.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.AURAPOINT;
	}

	@Override
	public int getRenderColor() {
		// Full brightness, as upstream: only the hue and saturation drift.
		return java.awt.Color.HSBtoRGB(hue / 360F, saturation, 1) & 0xFFFFFF;
	}

	/**
	 * V33a's is a call to super plus a commented-out point removal; the locus cache eviction the super
	 * does is the whole of the live behaviour.
	 */
	@Override
	public void breakBlock() {}

	public void togglePVP() {
		doPVP = !doPVP;
		// Cleared with the toggle: flipping PVP back on must not resurrect old grudges.
		hostilePlayers.clear();
		this.syncAllData(false);
	}

	public boolean doPvP() {
		return doPVP;
	}

	/** V33a: an aura point is the owner's, but anyone may stand in it. */
	@Override
	public boolean onlyAllowOwnersToMine() {
		return true;
	}

	@Override
	public boolean onlyAllowOwnersToUse() {
		return false;
	}

	public void markHostile(Player ep) {
		hostilePlayers.add(ep.getUUID());
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide())
			return;
		this.playSounds(world, pos);
		// V33a's cadence: the attack sweep is far more frequent than the heal.
		if (rand.nextInt(20) == 0)
			this.killEntities(world, pos);
		if (rand.nextInt(160) == 0)
			this.healFriendly(world, pos);
	}

	/** V33a: twice as often inside Proxima as outside it. */
	private void playSounds(Level world, BlockPos pos) {
		int n = world.dimension() == reika.chromaticraft.registry.ChromaDimensions.PROXIMA ? 2 : 1;
		if (this.getTicksExisted() % (244 / n) == 0)
			ChromaSounds.AURALOCUS.playSoundAtBlock(world, pos, 2, n);
	}

	private int getAttackRange() {
		return (int)Math.min(8 + this.getBlockEntityAge() / 16, 96);
	}

	private int getHealRange() {
		return (int)Math.min(4 + this.getBlockEntityAge() / 64, 32);
	}

	/** V33a: one looting level per four hours of the point's life, capped at six. */
	public int getLootingLevel() {
		return (int)Math.min(6, this.getBlockEntityAge() / 288000);
	}

	/** A full-height column, as upstream sweeps: the aura defends the sky as well as the ground. */
	private AABB column(BlockPos pos, int r) {
		return new AABB(pos.getX() - r, this.getLevel().getMinY(), pos.getZ() - r,
				pos.getX() + 1 + r, this.getLevel().getMaxY(), pos.getZ() + 1 + r);
	}

	private void killEntities(Level world, BlockPos pos) {
		boolean struck = false;
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class,
				this.column(pos, this.getAttackRange()))) {
			if (this.shouldAttack(e)) {
				this.attack(e);
				struck = true;
			}
		}
		if (struck) {
			ChromaSounds.DISCHARGE.playSoundAtBlock(world, pos);
			Player owner = this.getPlacer();
			if (owner != null && owner.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
					< 32 * 32)
				ChromaSounds.DISCHARGE.playSound(owner, 0.125F, 0.75F);
		}
	}

	/**
	 * V33a getAttackDamage: ten flat for a player, otherwise half the target's current health with a
	 * floor of four — so it scales into bosses instead of tickling them.
	 */
	private float getAttackDamage(LivingEntity e) {
		if (e instanceof Player)
			return 10;
		return Math.max(4, e.getHealth() / 2);
	}

	private void attack(LivingEntity e) {
		if (!(this.getLevel() instanceof ServerLevel server))
			return;
		// Deferred: ChromaAux.doPylonAttack(null, e, dmg, false, getLootingLevel(), false). The damage
		// is upstream's; the taper and progress flags, and routing the looting level into the drop
		// roll, arrive with that helper.
		e.hurtServer(server, server.damageSources().magic(), this.getAttackDamage(e));
		ChromaSounds.DISCHARGE.playSound(e, 0.5F, 1);
	}

	private boolean shouldAttack(LivingEntity e) {
		if (e.getHealth() <= 0 || !e.isAlive())
			return false;
		if (e instanceof Player ep) {
			// A player is neutral until they make themselves hostile, and never a target with PVP off.
			return doPVP && !this.isOwnedByPlayer(ep) && hostilePlayers.contains(ep.getUUID());
		}
		return ReikaEntityHelper.isHostile(e);
	}

	private void healFriendly(Level world, BlockPos pos) {
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class,
				this.column(pos, this.getHealRange())))
			if (this.shouldHeal(e)) {
				e.heal(e.getMaxHealth());
				ChromaSounds.CAST.playSound(e, 0.5F, 1);
			}
	}

	private boolean shouldHeal(LivingEntity e) {
		if (e.getHealth() >= e.getMaxHealth())
			return false;
		if (e instanceof Player ep)
			return this.isOwnedByPlayer(ep) || !hostilePlayers.contains(ep.getUUID());
		return e instanceof Animal;
	}

	/**
	 * V33a updateColors: the hue walks one degree a tick towards a target it re-rolls on arrival, going
	 * the short way round the wheel. The saturation eases towards its own target at a thirty-secondth
	 * of an eighth per tick, which is what makes the colour drift rather than flicker.
	 */
	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		float ds = saturationTarget - saturation;
		int dh = hueTarget - hue;
		if (dh == 0 && Math.abs(ds) < 0.03125F) {
			hueTarget = rand.nextInt(360);
			saturationTarget = rand.nextFloat() * rand.nextFloat();
		}
		saturation += 0.03125F * 0.125F * Math.signum(ds);
		int step = (int)Math.signum(dh);
		// Half a turn or more away, walking backwards is the shorter path.
		hue += Math.abs(dh) >= 180 ? -step : step;
		if (hue < 0 || hue >= 360)
			hue = (hue % 360 + 360) % 360;
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);
		NBT.putBoolean("pvp", doPVP);
		net.minecraft.nbt.ListTag li = new net.minecraft.nbt.ListTag();
		for (UUID id : hostilePlayers)
			li.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
		NBT.put("hostile", li);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);
		doPVP = NBT.getBooleanOr("pvp", true);
		hostilePlayers.clear();
		net.minecraft.nbt.ListTag li = NBT.getListOrEmpty("hostile");
		for (int i = 0; i < li.size(); i++)
			li.getString(i).ifPresent(id -> hostilePlayers.add(UUID.fromString(id)));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.putBoolean("pvp", doPVP);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		doPVP = NBT.getBooleanOr("pvp", true);
	}
}
