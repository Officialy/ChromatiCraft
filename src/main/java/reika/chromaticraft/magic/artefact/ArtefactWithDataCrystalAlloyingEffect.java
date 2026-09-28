package reika.chromaticraft.magic.artefact;

import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.auxiliary.recipemanagers.poolrecipes.AlloyingEffect;
import reika.chromaticraft.entity.EntityDataCrystal;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.auxiliary.trackers.TickScheduler;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.event.ScheduledTickEvent;
import reika.dragonapi.instantiable.event.scheduledtickevent.DelayedAttack;
import reika.dragonapi.instantiable.event.scheduledtickevent.DelayedKnockback;

/**
 * V33a: the Artifact Distribution Map's Pool Recipe, where a Memory Crystal and an Unknown Artefact are
 * alloyed together. The crystal turns orange and bathes the screen in a shifting amber glow; every so often
 * the pool throws a shockwave that, twelve ticks later, knocks back and hurts everything within twelve
 * blocks; and when the map forms, the pool goes up in a fiery explosion that the map and its catalyst survive.
 *
 * <p>The client half (particles, the burst sound and shockwave, the GLOW/SHOCK shaders) is
 * {@code ArtealloyEffects}; the orange crystal is {@link EntityDataCrystal#isArtealloy()}.
 *
 * <p>V33a gated bursts on {@code fireTick == 0}, but only the client's burst handler ever set that field. In
 * singleplayer the static was shared with the integrated server, so bursts were at least 50 ticks apart; on a
 * dedicated server the gate never closed. The singleplayer spacing is kept as a server-side cooldown. Likewise
 * the shared static was ticked by both sides in singleplayer; only the server ticks it here.
 */
public class ArtefactWithDataCrystalAlloyingEffect implements AlloyingEffect {

	public static final ArtefactWithDataCrystalAlloyingEffect instance = new ArtefactWithDataCrystalAlloyingEffect();

	/** Marks every item that took part, as V33a's {@code getEntityData().setBoolean("artealloy", true)}. */
	public static final String TAG = "artealloy";

	private static final int BURST_COOLDOWN = 50;

	private int tick = 0;
	private int burstCooldown = 0;

	private final Random rand = new Random();

	private ArtefactWithDataCrystalAlloyingEffect() {

	}

	@Override
	public void initialize(ItemEntity ei) {
		rand.setSeed(ei.getId());
		rand.nextBoolean();
		rand.nextBoolean();
		mark(ei);
		tick = 0;
		AABB box = new AABB(ei.blockPosition());
		for (ItemEntity ei2 : ei.level().getEntitiesOfClass(ItemEntity.class, box)) {
			mark(ei2);
		}
	}

	private static void mark(ItemEntity ei) {
		ei.getPersistentData().putBoolean(TAG, true);
		if (ei instanceof EntityDataCrystal crystal)
			crystal.setArtealloy(true);
	}

	@Override
	public void doEffect(ItemEntity ei) {
		if (!(ei.level() instanceof ServerLevel level))
			return;
		if (tick % 45 == 0)
			ChromaSounds.ARTEALLOY.playSoundNoAttenuation(level, ei.blockPosition(), 2, 1, 96);
		if (burstCooldown > 0)
			burstCooldown--;
		if (burstCooldown == 0 && tick > 10 && rand.nextInt(90) == 0) {
			List<LivingEntity> li = level.getEntitiesOfClass(LivingEntity.class, ei.getBoundingBox().inflate(12));
			DecimalPosition from = new DecimalPosition(ei);
			for (LivingEntity e : li) {
				TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(new DelayedKnockback(e, from, 5, 0.5)), 12);
				TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(new DelayedAttack(e, level.damageSources().magic(), 2)), 12);
			}
			ChromaNetwork.sendArtealloyBurst(level, ei);
			burstCooldown = BURST_COOLDOWN;
		}
		tick++;
	}

	@Override
	public void onFinish(ItemEntity ei, ItemEntity result) {
		Level world = ei.level();
		BlockPos c = ei.blockPosition();
		if (world.getBlockState(c).is(ChromaBlocks.CHROMA.get()))
			world.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
		ei.setPermanentlyInvulnerable(true);
		result.setPermanentlyInvulnerable(true);
		// newExplosion(ei, ..., 6, flaming, smoking): fire, and blocks broken regardless of mob griefing.
		world.explode(ei, ei.getX(), ei.getY(), ei.getZ(), 6, true, Level.ExplosionInteraction.TNT);
	}

}
