package reika.chromaticraft.auxiliary.ability;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.Chromabilities;

/** Direct 26.2 implementations of V33a Chromability effects with no intermediary legacy API. */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class AbilityEffects {

	private static final Identifier HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "ability_health");

	private AbilityEffects() {}

	/** V33a Leech heals ten percent of the incoming attack amount dealt by its owning player. */
	@SubscribeEvent
	public static void leechDamage(LivingDamageEvent.Post event) {
		if (event.getSource().getEntity() instanceof Player player
				&& Chromabilities.LEECH.enabledOn(player))
			player.heal(event.getOriginalDamage() * 0.1F);
	}

	public static void heal(Player player, int health) {
		player.heal(health);
	}

	public static boolean jump(Player player, int power) {
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x,
				motion.y + power / 2D * (1 + player.getRandom().nextDouble()), motion.z);
		player.hurtMarked = true;
		player.resetFallDistance();
		ChromaSounds.RIFT.playSound(player, 1, 2);
		return true;
	}

	public static void setPlayerMaxHealth(Player player, int value) {
		AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
		if (attribute == null)
			return;
		attribute.removeModifier(HEALTH_MODIFIER);
		if (value > 0) {
			attribute.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, value / 10F,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
	}

	public static boolean launchFireball(Player player, int charge) {
		if (!(player.level() instanceof ServerLevel level))
			return false;
		Vec3 look = player.getLookAngle();
		LargeFireball fireball = new LargeFireball(level, player, look, charge);
		fireball.snapTo(player.getX() + look.x * 2, player.getEyeY() + look.y * 2,
				player.getZ() + look.z * 2, player.getYRot(), player.getXRot());
		level.addFreshEntity(fireball);
		level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GHAST_SHOOT,
				net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
		return true;
	}

	public static boolean spawnLightning(Player player, int power) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level))
			return false;
		HitResult hit = player.pick(128, 1, false);
		if (!(hit instanceof BlockHitResult blockHit))
			return false;
		BlockPos pos = blockHit.getBlockPos();
		if (!level.canSeeSky(pos.above()) || !level.mayInteract(serverPlayer, pos)) {
			ChromaSounds.ERROR.playSound(player, 1, 1);
			return false;
		}
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null)
			return false;
		bolt.snapTo(Vec3.atBottomCenterOf(pos.above()));
		bolt.setCause(serverPlayer);
		level.addFreshEntity(bolt);
		if (power > 0) {
			float strength = power == 1 ? 4 : 6;
			level.explode(player, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5,
					strength, power == 1, Level.ExplosionInteraction.TNT);
		}
		return true;
	}

	public static void attractItemsAndExperience(Player player, double range, boolean noClip) {
		Vec3 target = player.position().add(0, 1.5, 0);
		AABB box = player.getBoundingBox().inflate(range);
		for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, box)) {
			if (!item.isAlive())
				continue;
			pull(player, item, target, 1, 0.125);
			item.setExtendedLifetime();
			item.noPhysics = noClip;
			item.getPersistentData().putString("cc_magnetized", player.getStringUUID());
		}
		for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, box)) {
			if (!orb.isAlive())
				continue;
			pull(player, orb, target, 2, 0.1);
			orb.noPhysics = noClip;
			orb.getPersistentData().putString("cc_magnetized", player.getStringUUID());
		}
	}

	private static void pull(Player player, Entity entity, Vec3 target, double horizontalDivisor,
			double lift) {
		Vec3 delta = target.subtract(entity.position());
		double distance = Math.max(0.25, delta.length());
		if (delta.horizontalDistance() < 1) {
			if (entity instanceof ItemEntity item)
				item.playerTouch(player);
			else if (entity instanceof ExperienceOrb orb)
				orb.playerTouch(player);
			return;
		}
		Vec3 motion = entity.getDeltaMovement();
		entity.setDeltaMovement(
				Mth.clamp(motion.x + delta.x / distance / distance / horizontalDivisor, -0.75, 0.75),
				Mth.clamp(motion.y + delta.y / distance / distance / 2 +
						(entity.getY() < target.y ? lift : 0), -0.75, 0.75),
				Mth.clamp(motion.z + delta.z / distance / distance / horizontalDivisor, -0.75, 0.75));
		entity.hurtMarked = true;
	}

	public static void stopArrows(Player player) {
		if (player.level().isClientSide())
			return;
		List<AbstractArrow> arrows = player.level().getEntitiesOfClass(AbstractArrow.class,
				player.getBoundingBox().inflate(6, 4, 6));
		for (AbstractArrow arrow : arrows) {
			Entity owner = arrow.getOwner();
			if (owner == player || owner instanceof Player
					&& player.level() instanceof ServerLevel serverLevel && !serverLevel.isPvpAllowed())
				continue;
			arrow.setDeltaMovement(arrow.getDeltaMovement().scale(-0.1));
			arrow.setYRot(arrow.getYRot() + 180);
			arrow.yRotO += 180;
			arrow.hurtMarked = true;
		}
	}

	public static void deAggroMobs(Player player) {
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class,
				player.getBoundingBox().inflate(12))) {
			if (mob instanceof EnderMan || mob instanceof ZombifiedPiglin)
				continue;
			if (mob.getTarget() == player)
				mob.setTarget(null);
			if (mob instanceof Creeper creeper && creeper.getTarget() != player)
				creeper.setSwellDir(-1);
		}
	}

	public static void shockwave(Player player) {
		if (player.level().isClientSide())
			return;
		ChromaSounds.SHOCKWAVE.playSound(player, 1, 1);
		for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(16, 4, 16))) {
			if (target == player || target.distanceToSqr(player) > 16 * 16)
				continue;
			Vec3 away = target.position().subtract(player.position());
			double horizontal = Math.max(0.001, away.horizontalDistance());
			target.push(away.x / horizontal * 4, 0.5, away.z / horizontal * 4);
			target.hurtMarked = true;
		}
	}
}
