package reika.chromaticraft.registry;

/** All ten V33a DIMGEN resource variants; order is the original metadata/sprite offset. */
public enum ChromaDimensionItems {
    MIASMA("miasmatic_essence", "Miasmatic Essence"),
    FLOATSTONE("floatstone", "Floatstone"),
    AQUA("aqua_dust", "Aqua Dust"),
    LIFEWATER("restorative_gel", "Restorative Gel"),
    LATTICE("ortho_crystals", "Ortho Crystals"),
    GEMSTONE("geode_gem", "Geode Gem"),
    CRYSTALLEAF("crystal_leaves", "Crystal Leaves"),
    OCEANSTONE("oceanstone", "Oceanstone"),
    CLIFFGLASS("aura_glass_shard", "Aura Glass Shard"),
    GLOWCAVE("glow_cave_dust", "Lumen Essence");

    private final String id;
    private final String displayName;
    ChromaDimensionItems(String id, String displayName) { this.id = id; this.displayName = displayName; }
    public String registryName() { return id; }
    public String displayName() { return displayName; }
}
