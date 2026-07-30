/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.entity.EntityLumaBurst;
import reika.chromaticraft.entity.EntityPylonOverloadShock;

/** Entity registrations accepted into the active 26.2 port slice. */
public final class ChromaEntityTypes {

	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
			DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<EntityType<?>, EntityType<EntityPylonOverloadShock>> PYLON_OVERLOAD =
			register("pylon_overload", () -> EntityType.Builder
					.<EntityPylonOverloadShock>of(EntityPylonOverloadShock::new, MobCategory.MISC)
					.sized(0.25F, 0.25F).clientTrackingRange(128).updateInterval(1));

	/** V33a "Luma Fog" (Luminous Cliffs wandering light-cloud); see EntityGlowCloud for the full
	 *  behaviour and its CHROMA-PORT-marked gaps. */
	public static final DeferredHolder<EntityType<?>, EntityType<EntityGlowCloud>> GLOW_CLOUD =
			register("glow_cloud", () -> EntityType.Builder
					.<EntityGlowCloud>of(EntityGlowCloud::new, MobCategory.CREATURE)
					.sized(0.25F, 0.25F).clientTrackingRange(80).updateInterval(1));

	/** V33a "Luma Burst" (gravity-puzzle/burst-emitter projectile); registration only for now — its
	 *  V33a spawner (BlockGravityTile) is still pristine 1.7.10. See EntityLumaBurst. */
	public static final DeferredHolder<EntityType<?>, EntityType<EntityLumaBurst>> LUMA_BURST =
			register("luma_burst", () -> EntityType.Builder
					.<EntityLumaBurst>of(EntityLumaBurst::new, MobCategory.MISC)
					.sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1));

	private ChromaEntityTypes() {}

	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
			String name, Supplier<EntityType.Builder<T>> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name));
		return ENTITY_TYPES.register(name, () -> builder.get().build(key));
	}
}
