package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.PylonDamage;

/**
 * V33a {@code PylonDamage}'s behaviour as damage-type tags: unblockable (bypasses armour), absolute (no
 * Resistance, no Protection -- bypasses effects), magic (witches resist it), and with no attacker, no knockback.
 * Creative players stay immune, as V33a's {@code canHarmInCreative} was false.
 */
public final class ChromaDamageTypeTagsProvider extends TagsProvider<DamageType> {

	public ChromaDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, Registries.DAMAGE_TYPE, lookup, ChromatiCraft.MODID);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		for (ResourceKey<DamageType> key : PylonDamage.keys()) {
			tag(PylonDamage.TAG).add(key);
			tag(DamageTypeTags.BYPASSES_ARMOR).add(key);
			tag(DamageTypeTags.BYPASSES_EFFECTS).add(key);
			tag(DamageTypeTags.WITCH_RESISTANT_TO).add(key);
			tag(DamageTypeTags.NO_KNOCKBACK).add(key);
		}
	}
}
