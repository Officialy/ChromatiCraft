/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;

import java.util.Locale;

/**
 * V33a {@code PylonDamage}: the energy damage of crystal pylons and everything that borrows it, one kind per
 * element plus an uncoloured one ({@code ChromatiCraft.pylonDamage[0..16]}). In 26.2 these are data-driven damage
 * types, {@code chromaticraft:pylon_<element>} and {@code chromaticraft:pylon}.
 *
 * <p>V33a's source was unblockable, absolute (no Resistance, no Protection), magic, never difficulty-scaled, and
 * unable to hurt creative players, with no attacking entity; the tags in {@code ChromaDamageTypeTagsProvider}
 * carry that, and each element has its own death message ({@code death.attack.chromaticraft.pylon.*}).
 */
public final class PylonDamage {

	private static final ResourceKey<DamageType>[] KEYS = createKeys();

	/** Every pylon damage type; stands in for V33a's {@code src instanceof PylonDamage}. */
	public static final net.minecraft.tags.TagKey<DamageType> TAG = net.minecraft.tags.TagKey.create(
			Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon"));

	public static boolean isPylonDamage(DamageSource src) {
		return src.is(TAG);
	}

	private PylonDamage() {}

	@SuppressWarnings("unchecked")
	private static ResourceKey<DamageType>[] createKeys() {
		ResourceKey<DamageType>[] keys = new ResourceKey[CrystalElement.elements.length + 1];
		for (CrystalElement e : CrystalElement.elements)
			keys[e.ordinal()] = ResourceKey.create(Registries.DAMAGE_TYPE,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_" + e.getEnglishName()));
		keys[CrystalElement.elements.length] = ResourceKey.create(Registries.DAMAGE_TYPE,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon"));
		return keys;
	}

	/** The damage type for an element, or the uncoloured one for null. */
	public static ResourceKey<DamageType> key(CrystalElement color) {
		return KEYS[color == null ? CrystalElement.elements.length : color.ordinal()];
	}

	public static ResourceKey<DamageType>[] keys() {
		return KEYS.clone();
	}

	/** The death-message id, {@code death.attack.<id>}. */
	public static String messageId(CrystalElement color) {
		return "chromaticraft.pylon" + (color == null ? "" : "." + color.name().toLowerCase(Locale.ROOT));
	}

	public static DamageSource source(Level level, CrystalElement color) {
		return level.damageSources().source(key(color));
	}

	public static void bootstrap(BootstrapContext<DamageType> context) {
		for (CrystalElement e : CrystalElement.elements)
			context.register(key(e), new DamageType(messageId(e), DamageScaling.NEVER, 0));
		context.register(key(null), new DamageType(messageId(null), DamageScaling.NEVER, 0));
	}

	/** V33a {@code ChromaAux.doPylonAttack(color, e, amt)}: no taper for new players, no progression. */
	public static void doPylonAttack(CrystalElement color, LivingEntity e, float amt) {
		doPylonAttack(color, e, amt, false, false);
	}

	/**
	 * V33a {@code ChromaAux.doPylonAttack}. A player struck with {@code progress} learns of pylon shocks
	 * (SHOCK); with {@code taperNew} a player alive under 30 s takes half a heart, rising by half a heart every
	 * 5 s to 2.5 hearts at 50 s. The strike then lands for the full, untapered amount whatever reduced it -- the
	 * health is set down to what it would be, and a strike that should kill does.
	 *
	 * <p>V33a also drained a Blood Magic soul network and pierced its bound armour; that mod has no 26.2 port.
	 * Its {@code looting} argument was never read.
	 */
	public static void doPylonAttack(CrystalElement color, LivingEntity e, float amt, boolean taperNew, boolean progress) {
		if (!(e.level() instanceof ServerLevel level))
			return;
		final float originalAmt = amt;
		if (e instanceof Player ep) {
			if (progress)
				ProgressStage.SHOCK.stepPlayerTo(ep);
			if (taperNew) {
				if (e.tickCount < 600)
					amt = 1;
				else if (e.tickCount <= 1000)
					amt = 1 + (e.tickCount - 600) / 100;
			}
		}
		float last = e.getHealth();
		DamageSource src = source(level, color);
		e.hurtServer(level, src, amt);
		if (e.getHealth() > Math.max(0, last - originalAmt)) {
			if (originalAmt >= last) {
				e.setHealth(0.1F);
				e.hurtServer(level, src, Float.MAX_VALUE);
			}
			else {
				e.setHealth(last - originalAmt);
			}
		}
	}

}
