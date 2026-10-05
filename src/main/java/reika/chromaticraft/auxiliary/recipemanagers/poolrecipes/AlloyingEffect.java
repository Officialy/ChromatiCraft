/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.poolrecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.entity.item.ItemEntity;
import reika.chromaticraft.magic.artefact.ArtefactWithDataCrystalAlloyingEffect;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * V33a {@code PoolRecipes.AlloyingEffect}: extra behaviour a Pool Recipe runs while it alloys. A recipe names
 * its effects by id in its {@code effects} list.
 *
 * <p>In V33a the pool loop ran on both sides and each effect branched on {@code worldObj.isRemote}. Here the
 * server drives these hooks, and each tick's {@code PoolAlloyingFx} packet carries the recipe's effect ids so
 * the client can run the matching visuals (see {@code ClientPayloadHandlers.poolAlloyingFx}).
 */
public interface AlloyingEffect {

	/** Once, when the catalyst first starts alloying. */
	void initialize(ItemEntity ei);

	/** Every server tick while the catalyst is alloying. */
	void doEffect(ItemEntity ei);

	/** Once per output item when the recipe completes. */
	void onFinish(ItemEntity ei, ItemEntity result);

	/** The effects a recipe can name, by id. */
	final class Registry {

		private static final Map<String, AlloyingEffect> EFFECTS = new LinkedHashMap<>();
		private static final Map<AlloyingEffect, String> IDS = new LinkedHashMap<>();

		public static final String ARTEFACT_DATA_CRYSTAL = "artefact_data_crystal";

		static {
			register(ARTEFACT_DATA_CRYSTAL, ArtefactWithDataCrystalAlloyingEffect.instance);
		}

		private Registry() {}

		private static void register(String id, AlloyingEffect effect) {
			EFFECTS.put(id, effect);
			IDS.put(effect, id);
		}

		public static AlloyingEffect get(String id) {
			return EFFECTS.get(id);
		}

		public static String getId(AlloyingEffect effect) {
			return IDS.get(effect);
		}

		/** Unknown ids fail when the recipe loads rather than being silently dropped. */
		public static final Codec<AlloyingEffect> CODEC = Codec.STRING.comapFlatMap(id -> {
			AlloyingEffect effect = EFFECTS.get(id);
			return effect != null ? DataResult.success(effect)
					: DataResult.error(() -> "Unknown pool alloying effect '" + id + "'");
		}, Registry::getId);

	}

}
