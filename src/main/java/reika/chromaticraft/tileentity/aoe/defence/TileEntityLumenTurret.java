/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.aoe.defence;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import reika.chromaticraft.auxiliary.LumenTurretDamage;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.RayTracer;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.interfaces.entity.TameHostile;
import reika.dragonapi.libraries.ReikaEntityHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * V33a {@code TileEntityLumenTurret}: every 5 ticks, with a view of the sky, it strikes each hostile mob within 6
 * blocks (2 vertically) it can see from its tip -- 4 damage, magic, credited to its placer -- then ignores that mob
 * for 10 ticks (5 for endermen and creepers), resetting its invulnerability and making it forget its target. Rune
 * blocks stacked up to four deep beneath it (or a multichromic crystalline stone, which counts as all of them) add
 * upgrades: green passive mobs, purple other players, lime range, pink damage, light blue fire rate, blue no sky
 * needed, orange fire, light grey a non-player damage source.
 */
public class TileEntityLumenTurret extends TileEntityChromaticBase {

	private final HashMap<UUID, Integer> attackCooldown = new HashMap<>();

	private final StepTimer attackTimer = new StepTimer(5);

	private static final RayTracer tracer = new RayTracer(0, 0, 0, 0, 0, 0);

	public static final int MAX_UPGRADES = 4;

	public TileEntityLumenTurret(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LUMEN_TURRET.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.TURRET;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!world.isClientSide()) {
			attackTimer.update();
			if (attackTimer.checkCap()) {
				if (world.canSeeSky(pos.above()) || TurretUpgrades.SKY.check(this))
					this.attackEntities(world, pos);
			}

			this.decrementTicks();
		}
		else {
			this.doParticles(world, pos);
		}
	}

	/** V33a: each tick one random upgrade, if fitted, sends up a blur of its colour. */
	private void doParticles(Level world, BlockPos pos) {
		TurretUpgrades u = TurretUpgrades.list[rand.nextInt(TurretUpgrades.list.length)];
		if (u.check(this)) {
			float g = -(float)(0.0625+(rand.nextDouble()*2-1)*0.03125);
			float s = (float)(1.25+(rand.nextDouble()*2-1)*0.25);
			int l = 10+rand.nextInt(30);
			ChromaParticle.spawnTurretUpgradeGlow(world, pos.getX()+0.5, pos.getY()+0.625, pos.getZ()+0.5, u.color.color(), g, s, l);
		}
	}

	private void decrementTicks() {
		if (!attackCooldown.isEmpty()) {
			HashSet<UUID> rem = new HashSet<>();
			for (UUID id : attackCooldown.keySet()) {
				int time = attackCooldown.get(id);
				if (time > 1) {
					attackCooldown.put(id, time-1);
				}
				else {
					rem.add(id);
				}
			}
			for (UUID id : rem) {
				attackCooldown.remove(id);
			}
		}
	}

	private void attackEntities(Level world, BlockPos pos) {
		List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, this.getBox(pos));
		for (LivingEntity e : li) {
			if (this.shouldAttack(e)) {
				this.tryAttackEntity(world, pos, e);
			}
		}
	}

	private boolean shouldAttack(LivingEntity e) {
		if (e instanceof EntityBallLightning)
			return false;
		if (e instanceof TameHostile || (e instanceof TamableAnimal t && t.isTame()))
			return false;
		if (ReikaEntityHelper.isHostile(e))
			return true;
		if (e instanceof Player ep)
			return !this.isOwnedByPlayer(ep) && this.isHostile(ep) && TurretUpgrades.PLAYERS.check(this);
		return TurretUpgrades.PASSIVE.check(this);
	}

	/** V33a never judged a player hostile, so even with the purple upgrade players are spared. */
	private boolean isHostile(Player e) {
		return false;
	}

	private void tryAttackEntity(Level world, BlockPos pos, LivingEntity e) {
		if (!attackCooldown.containsKey(e.getUUID())) {
			if (this.canSeeEntity(world, pos, e)) {
				this.doAttackEntity(world, pos, e);
			}
		}
	}

	/** Line of sight from the tip to the entity's feet, middle or top. */
	private boolean canSeeEntity(Level world, BlockPos pos, Entity e) {
		int n = 2;
		for (int i = 0; i <= n; i++) {
			tracer.setOrigins(pos.getX()+0.5, pos.getY()+0.5625, pos.getZ()+0.5, e.getX(), e.getY()+e.getBbHeight()*i/n, e.getZ());
			if (tracer.isClearLineOfSight(world))
				return true;
		}
		return false;
	}

	private void doAttackEntity(Level world, BlockPos pos, LivingEntity e) {
		attackCooldown.put(e.getUUID(), this.getAttackCooldown(e));
		Player placer = this.getPlacer();
		boolean fake = TurretUpgrades.NONPLAYER.check(this) || (placer != null && Chromabilities.COMMUNICATE.enabledOn(placer));
		if (world instanceof ServerLevel level)
			e.hurtServer(level, LumenTurretDamage.source(this, fake), this.getAttackDamage(e));
		if (e instanceof PathfinderMob em) {
			em.setTarget(null); //de-aggro
		}
		e.setInvulnerableTime(0);
		if (TurretUpgrades.FIRE.check(this))
			e.igniteForSeconds(2);
		ChromaSounds.DISCHARGE.playSound(world, pos, 0.25F, 1.5F);
		if (world instanceof ServerLevel level)
			ChromaNetwork.sendTurretAttack(level, pos, e.getId());
	}

	/** V33a TURRETATTACK: 2-5 pink flares shoot from the tip toward the struck entity's middle. */
	public void doAttackParticles(int entityID) {
		Entity e = level.getEntity(entityID);
		if (e == null)
			return;
		BlockPos pos = this.getBlockPos();
		ChromaParticle.spawnTurretAttack(level, pos.getX()+0.5, pos.getY()+0.5625, pos.getZ()+0.5,
				e.getX(), e.getY()+e.getBbHeight()/2, e.getZ());
	}

	private float getAttackDamage(LivingEntity e) {
		return TurretUpgrades.DAMAGE.check(this) ? 7 : 4;
	}

	private int getAttackCooldown(LivingEntity e) {
		if (TurretUpgrades.FIRERATE.check(this))
			return e instanceof Enderman || e instanceof Creeper ? 2 : 5;
		return e instanceof Enderman || e instanceof Creeper ? 5 : 10;
	}

	private AABB getBox(BlockPos pos) {
		boolean flag = TurretUpgrades.RANGE.check(this);
		int r = flag ? 10 : 6;
		int r2 = flag ? 4 : 2;
		return new AABB(pos).inflate(r, r2, r);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	public static String getUpgradesListString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TurretUpgrades.list.length; i++) {
			TurretUpgrades t = TurretUpgrades.list[i];
			sb.append(t.color.displayName+": "+t.getLexiconString());
			if (i < TurretUpgrades.list.length-1)
				sb.append("\n");
		}
		return sb.toString();
	}

	public enum TurretUpgrades {
		PASSIVE(CrystalElement.GREEN),
		PLAYERS(CrystalElement.PURPLE),
		RANGE(CrystalElement.LIME),
		DAMAGE(CrystalElement.PINK),
		FIRERATE(CrystalElement.LIGHTBLUE),
		SKY(CrystalElement.BLUE),
		FIRE(CrystalElement.ORANGE),
		NONPLAYER(CrystalElement.LIGHTGRAY);

		public final CrystalElement color;

		private static final TurretUpgrades[] list = values();

		TurretUpgrades(CrystalElement e) {
			color = e;
		}

		public String getLexiconString() {
			return switch(this) {
				case DAMAGE -> "Increase damage per hit";
				case FIRE -> "Set fire to hit mobs";
				case FIRERATE -> "Increase fire rate";
				case NONPLAYER -> "Obfuscate damage source as non-player";
				case PASSIVE -> "Enable targeting of passive creatures";
				case PLAYERS -> "Enable targeting of players";
				case RANGE -> "Increase range";
				case SKY -> "Function without sky view";
			};
		}

		/**
		 * A multichromic crystalline stone directly beneath counts as every upgrade; otherwise the runes stacked
		 * directly beneath, up to four deep. (V33a's loop never re-read the block as it descended, so beneath a rune
		 * it read the metadata of whatever lay up to four deep; 26.2 blocks have no metadata, and the stack of runes
		 * it was written for behaves the same.)
		 */
		public boolean check(TileEntityLumenTurret te) {
			Level world = te.getLevel();
			BlockPos base = te.getBlockPos();
			int y = base.getY()-1;
			BlockPos below = base.atY(y);
			if (world.getBlockState(below).is(ChromaBlocks.crystallineStone(reika.chromaticraft.block.BlockCrystallineStone.StoneTypes.MULTICHROMIC).get()))
				return true;
			while (y > world.getMinY() && y >= base.getY()-MAX_UPGRADES) {
				BlockPos at = base.atY(y);
				if (!(world.getBlockState(at).getBlock() instanceof reika.chromaticraft.block.BlockCrystalRune rune))
					break;
				if (rune.getColor() == color)
					return true;
				y--;
			}
			return false;
		}
	}

}
