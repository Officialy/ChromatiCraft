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

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;

/**
 * ChromatiCraft item data components. Everything else still rides {@code CUSTOM_DATA}; the Adjacency Upgrade tier is
 * its own component because the casting table matches a recipe's centre by component, and V33a's
 * {@code isValidCentralNBT} compared the tier while ignoring the stored {@code energy} a used upgrade carries.
 */
public final class ChromaDataComponents {

	public static final DeferredRegister.DataComponents COMPONENTS =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ChromatiCraft.MODID);

	/** V33a's Adjacency Upgrade {@code tier} stack tag, 0 to {@code MAX_TIER-1}. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ADJACENCY_TIER =
			COMPONENTS.registerComponentType("adjacency_tier", b -> b
					.persistent(Codec.intRange(0, 7))
					.networkSynchronized(ByteBufCodecs.VAR_INT));

	private ChromaDataComponents() {}
}
