package reika.chromaticraft.registry;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import reika.chromaticraft.ChromatiCraft;

/**
 * Item tags for the crystal shard family.
 *
 * <p>Plain and boosted per-colour tags are deliberately disjoint, matching V33a's exact metadata
 * ingredients. A recipe which genuinely accepts either form uses {@link #CRYSTAL_SHARDS}; recipes
 * such as Raw Crystal which named metadata 0-15 use {@link #PLAIN_CRYSTAL_SHARDS}.
 */
public final class ChromaItemTags {

	/** Every crystal shard, plain or boosted, in every colour. */
	public static final TagKey<Item> CRYSTAL_SHARDS = create("crystal_shards");

	/**
	 * Only the plain shards. V33a's wildcard-looking recipes actually name
	 * {@code SHARD.getStackOfMetadata(0..15)}, which is an exact metadata match and so never accepts
	 * a boosted shard (metadata 16-31); this is the tag for "any shard, but not the upgraded form".
	 */
	public static final TagKey<Item> PLAIN_CRYSTAL_SHARDS = create("plain_crystal_shards");

	/** Only the boosted shards, for recipes that genuinely require the upgraded form. */
	public static final TagKey<Item> BOOSTED_CRYSTAL_SHARDS = create("boosted_crystal_shards");
	/** All seventeen former RELAY metadata values, including the multichromic relay itself. */
	public static final TagKey<Item> LUMEN_RELAYS = create("lumen_relays");

	private static final Map<CrystalElement, TagKey<Item>> BY_COLOUR = byColour("crystal_shards/");
	private static final Map<CrystalElement, TagKey<Item>> BOOSTED_BY_COLOUR =
			byColour("boosted_crystal_shards/");

	/** Plain shard of one specific colour. */
	public static TagKey<Item> crystalShards(CrystalElement element) {
		return BY_COLOUR.get(element);
	}

	/** The boosted shard of one specific colour. */
	public static TagKey<Item> boostedCrystalShards(CrystalElement element) {
		return BOOSTED_BY_COLOUR.get(element);
	}

	private static Map<CrystalElement, TagKey<Item>> byColour(String prefix) {
		Map<CrystalElement, TagKey<Item>> map = new EnumMap<>(CrystalElement.class);
		// getEnglishName, not the enum constant: the registry ids use vanilla's dye spelling
		// (light_gray / light_blue), and the tag paths must line up with the items they contain.
		for (CrystalElement element : CrystalElement.elements)
			map.put(element, create(prefix + element.getEnglishName()));
		return map;
	}

	private static TagKey<Item> create(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
	}

	private ChromaItemTags() {}
}
