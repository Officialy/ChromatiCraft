package reika.chromaticraft.registry;

/**
 * The 13 V33a {@code ChromaItems.CLUSTER} metadata variants, split into stable 26.2 item
 * identities. Declaration order is the original metadata order used by {@code ChromaStacks} and
 * the casting recipe graph.
 */
public enum ChromaClusterItems {

	RED_GROUP("red_crystal_group", "Red Crystal Group"),
	GREEN_GROUP("green_crystal_group", "Green Crystal Group"),
	ORANGE_GROUP("orange_crystal_group", "Orange Crystal Group"),
	WHITE_GROUP("white_crystal_group", "White Crystal Group"),
	PRIMARY_BUNCH("primary_crystal_bunch", "Primary Crystal Bunch"),
	SECONDARY_BUNCH("secondary_crystal_bunch", "Secondary Crystal Bunch"),
	TERTIARY_BUNCH("tertiary_crystal_bunch", "Tertiary Crystal Bunch"),
	QUATERNARY_BUNCH("quaternary_crystal_bunch", "Quaternary Crystal Bunch"),
	PRIMARY_CLUSTER("primary_crystal_cluster", "Primary Crystal Cluster"),
	SECONDARY_CLUSTER("secondary_crystal_cluster", "Secondary Crystal Cluster"),
	CRYSTAL_CORE("crystal_core", "Crystal Core"),
	CRYSTAL_STAR("crystal_star", "Crystal Star"),
	MULTI_CRYSTAL("multi_crystal", "Multi Crystal");

	public static final ChromaClusterItems[] list = values();

	private final String registryName;
	private final String displayName;

	ChromaClusterItems(String registryName, String displayName) {
		this.registryName = registryName;
		this.displayName = displayName;
	}

	public String registryName() {
		return registryName;
	}

	public String displayName() {
		return displayName;
	}
}
