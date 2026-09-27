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
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityLumenTurret;

/**
 * V33a {@code LumenTurretDamage}, now the data-driven {@code chromaticraft:lumen_turret} damage type: magic (witches
 * resist it), credited to the turret's placer -- or, with the non-player upgrade or the placer's Communication ability,
 * to the placer's fake player -- so the death reads "got too close to <placer>'s Lumen Turret".
 */
public final class LumenTurretDamage {

	public static final ResourceKey<DamageType> KEY = ResourceKey.create(Registries.DAMAGE_TYPE,
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "lumen_turret"));

	/** The death-message id, {@code death.attack.<id>}. */
	public static final String MESSAGE_ID = "chromaticraft.lumen_turret";

	private LumenTurretDamage() {}

	/** V33a {@code new LumenTurretDamage(te, fake)}. */
	public static DamageSource source(TileEntityLumenTurret te, boolean fake) {
		Level level = te.getLevel();
		Player player = fake ? te.getFakePlacer() : te.getPlacer();
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(KEY), player, player);
	}

	/** Stands in for V33a's {@code src instanceof LumenTurretDamage}. */
	public static boolean isTurretDamage(DamageSource src) {
		return src.is(KEY);
	}

	public static void bootstrap(BootstrapContext<DamageType> context) {
		// 1.7.10 DamageSource hunger 0.3 (only armour-bypassing and absolute damage zeroed it).
		context.register(KEY, new DamageType(MESSAGE_ID, DamageScaling.NEVER, 0.3F));
	}

}
