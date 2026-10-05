package reika.chromaticraft.magic.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import reika.chromaticraft.ChromatiCraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dependency-free identity catalog for every V33a Chromic Lexicon entry.
 *
 * <p>The original {@code ChromaResearch} enum eagerly constructed icons from every machine, block,
 * tool, ability, structure and optional integration in the mod. That loading model is not viable
 * while those registries are being moved to 26.2, but the stable fragment names and their ordering
 * must not change: they are persistent data in lexicons and player research. The bundled catalog is
 * mechanically derived from the complete V33a enum and deliberately contains all 322 entries, not
 * merely the currently registered content. Content renderers bind to {@link Entry#sourceType()} and
 * {@link Entry#sourceId()} as their corresponding modern registries land.
 */
public final class LexiconCatalog {

	private static final String RESOURCE = "/assets/chromaticraft/lexicon/catalog.tsv";
	private static final List<Entry> ENTRIES;
	private static final Map<String, Entry> BY_ID;
	private static final EnumMap<Section, List<Entry>> BY_SECTION;

	static {
		LinkedHashMap<String, Entry> byId = new LinkedHashMap<>();
		EnumMap<Section, ArrayList<Entry>> bySection = new EnumMap<>(Section.class);
		for (Section section : Section.values())
			bySection.put(section, new ArrayList<>());

		try (InputStream stream = LexiconCatalog.class.getResourceAsStream(RESOURCE)) {
			if (stream == null)
				throw new IllegalStateException("Missing Chromic Lexicon catalog " + RESOURCE);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				int ordinal = 0;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.charAt(0) == '#')
						continue;
					String[] fields = line.split("\\t", -1);
					if (fields.length != 8)
						throw new IllegalStateException("Malformed lexicon catalog row: " + line);
					String id = fields[0];
					Section section = Section.valueOf(fields[1].toUpperCase(Locale.ROOT));
					ResearchLevel level = fields[2].isEmpty() ? null
							: ResearchLevel.valueOf(fields[2].toUpperCase(Locale.ROOT));
					boolean parent = fields[3].equals("parent");
					Entry entry = new Entry(id, ordinal++, section, level, parent, fields[4], fields[5], fields[6], fields[7]);
					if (byId.put(id, entry) != null)
						throw new IllegalStateException("Duplicate Chromic Lexicon page id " + id);
					bySection.get(section).add(entry);
				}
			}
		}
		catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}

		if (byId.size() != 322)
			throw new IllegalStateException("V33a Chromic Lexicon catalog must contain 322 entries, found " + byId.size());
		ENTRIES = List.copyOf(byId.values());
		BY_ID = Collections.unmodifiableMap(byId);
		BY_SECTION = new EnumMap<>(Section.class);
		for (Map.Entry<Section, ArrayList<Entry>> entry : bySection.entrySet())
			BY_SECTION.put(entry.getKey(), List.copyOf(entry.getValue()));
	}

	private LexiconCatalog() {}

	public static List<Entry> entries() {
		return ENTRIES;
	}

	public static List<Entry> pages() {
		return ENTRIES.stream().filter(entry -> !entry.parent()).toList();
	}

	/**
	 * V33a {@code isDummiedOut} hard retirements: AUGMENT, VILLAGEREPAIR and AISHUTDOWN (the last also
	 * an incomplete machine: a modelled block with no renderer). APIRECIPES was dummied whenever no
	 * external mod had registered casting recipes; the port has no such API yet, so it is always empty.
	 */
	private static final java.util.Set<String> RETIRED = java.util.Set.of(
			"AUGMENT", "VILLAGEREPAIR", "AISHUTDOWN", "APIRECIPES");

	/**
	 * V33a dependencies by page: {@code ChromaResearch.getDependency} plus the {@code ModList}/{@code
	 * PowerTypes} prerequisite of each page's machine ({@code ChromaTiles}) or item ({@code ChromaItems}).
	 */
	private static final Map<String, reika.dragonapi.interfaces.registry.Dependency> DEPENDENCIES = Map.ofEntries(
			Map.entry("BEES", reika.dragonapi.ModList.FORESTRY),
			Map.entry("BEESTORAGE", reika.dragonapi.ModList.FORESTRY),
			Map.entry("BEEFRAME", reika.dragonapi.ModList.FORESTRY),
			Map.entry("ALVEARY", reika.dragonapi.ModList.FORESTRY),
			Map.entry("TINKERTOOLS", reika.dragonapi.ModList.TINKERER),
			Map.entry("SMELTERY", reika.dragonapi.ModList.TINKERER),
			Map.entry("RFDISTRIB", reika.dragonapi.modregistry.PowerTypes.RF),
			Map.entry("BALLLIGHTNING", reika.chromaticraft.registry.ChromaOptions.BALLLIGHTNING),
			Map.entry("NODENET", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("ASPECT", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("ASPECTJAR", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("FLUXMAKER", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("WARP", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("MANIPFOCUS", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("ABILITYFOCUS", reika.dragonapi.ModList.THAUMCRAFT),
			Map.entry("MYSTPAGE", reika.dragonapi.ModList.MYSTCRAFT),
			Map.entry("BOOKDECOMP", reika.dragonapi.ModList.MYSTCRAFT),
			Map.entry("DEATHFOG", reika.dragonapi.ModList.VOIDMONSTER),
			Map.entry("VOIDTRAP", reika.dragonapi.ModList.VOIDMONSTER),
			Map.entry("VOIDTRAPSTRUCT", reika.dragonapi.ModList.VOIDMONSTER),
            Map.entry("VOIDTRAPSTRUCTN", reika.dragonapi.ModList.VOIDMONSTER),
			Map.entry("VOIDESSENCE", reika.dragonapi.ModList.VOIDMONSTER),
			Map.entry("MEDISTRIB", reika.dragonapi.ModList.APPENG),
			Map.entry("PATTERNS", reika.dragonapi.ModList.APPENG),
			Map.entry("CRYSTALCELL", reika.dragonapi.ModList.APPENG),
			Map.entry("VOIDCELL", reika.dragonapi.ModList.APPENG),
			Map.entry("MULTIPATTERN", reika.dragonapi.ModList.APPENG),
			Map.entry("MANABOOSTER", reika.dragonapi.ModList.BOTANIA),
			Map.entry("LANDMARK", reika.dragonapi.ModList.BUILDCRAFT));

	public static List<Entry> obtainablePages() {
		return ENTRIES.stream().filter(Entry::obtainable).toList();
	}

	public static List<Entry> entries(Section section) {
		return BY_SECTION.get(section);
	}

	public static Entry byId(String id) {
		return id == null ? null : BY_ID.get(id.toUpperCase(Locale.ROOT));
	}

	public enum Section {
		INFO("Introduction"),
		MACHINES("Constructs"),
		BLOCKS("Other Blocks"),
		TOOLS("Tools"),
		RESOURCES("Resources"),
		ABILITIES("Abilities"),
		STRUCTURES("Structures");

		private final String fallbackTitle;

		Section(String fallbackTitle) {
			this.fallbackTitle = fallbackTitle;
		}

		public Component title() {
			return Component.translatableWithFallback(
					"lexicon." + ChromatiCraft.MODID + ".section." + name().toLowerCase(Locale.ROOT), fallbackTitle);
		}
	}

	public record Entry(String id, int ordinal, Section section, ResearchLevel level, boolean parent,
			String sourceType, String sourceId, String progressSpec, String exactTitle) {

		public Entry {
			if (id.isBlank() || !id.equals(id.toUpperCase(Locale.ROOT)))
				throw new IllegalArgumentException("Lexicon page ids must retain their V33a uppercase identity: " + id);
			if (sourceType.isBlank() || sourceId.isBlank())
				throw new IllegalArgumentException("Lexicon source binding cannot be blank for " + id);
			if (exactTitle.isBlank())
				throw new IllegalArgumentException("Lexicon title cannot be blank for " + id);
		}

		public boolean alwaysPresent() {
			return id.equals("START") || id.equals("LEXICON");
		}

		/** PACKCHANGES was obtainable data, but V33a made it readable even without its fragment. */
		public boolean readableWithoutFragment() {
			return alwaysPresent() || id.equals("PACKCHANGES");
		}

		public boolean obtainable() {
			return !parent && !alwaysPresent() && !isDummiedOut();
		}

		/**
		 * V33a {@code ChromaResearch.isDummiedOut}: pages for content that cannot exist in this game.
		 * They are neither obtainable (V33a {@code getAllObtainableFragments}) nor tier-gating
		 * ({@code isGating}). Without this, DEATHFOG (a RUNECRAFT page whose VOIDMONSTER stage is inactive
		 * without the Void Monster mod) could never be obtained and froze research at RUNECRAFT.
		 *
		 * <p>The source consults the page's machine/item/ability dependency, a per-page dependency, a few
		 * hard-coded retirements, and whether every required stage is active. PACKCHANGES (dummied when no
		 * pack modification exists) keeps its existing readable-without-fragment handling.
		 */
		public boolean isDummiedOut() {
			if (RETIRED.contains(id))
				return true;
			reika.dragonapi.interfaces.registry.Dependency dependency = DEPENDENCIES.get(id);
			if (dependency != null && !dependency.isLoaded())
				return true;
			for (ProgressStage stage : requiredProgress()) {
				if (!stage.active)
					return true;
			}
			return false;
		}

		public List<ProgressStage> requiredProgress() {
			if (progressSpec.isEmpty())
				return List.of();
			ArrayList<ProgressStage> stages = new ArrayList<>();
			for (String token : progressSpec.split(",")) {
				if (token.charAt(0) == '@') {
					ProgressStage target = ProgressStage.valueOf(token.substring(1));
					Collections.addAll(stages, ProgressionManager.instance.getPrereqsArray(target));
				}
				else {
					stages.add(ProgressStage.valueOf(token));
				}
			}
			return List.copyOf(stages);
		}

		public boolean canPlayerProgressTo(Player player) {
			for (ProgressStage stage : requiredProgress()) {
				if (!stage.isPlayerAtStage(player))
					return false;
			}
			return true;
		}

		public boolean requiresProgress(ProgressStage stage) {
			return requiredProgress().contains(stage);
		}

		public Component title() {
			return Component.translatableWithFallback(
					"lexicon." + ChromatiCraft.MODID + ".page." + id.toLowerCase(Locale.ROOT), exactTitle);
		}

		/** XML file used by the original description pipeline. */
		public String descriptionResource() {
			return switch (section) {
				case INFO -> "info";
				case MACHINES -> "machines";
				case BLOCKS -> "blocks";
				case TOOLS -> "tools";
				case RESOURCES -> "resource";
				case ABILITIES -> "abilities";
				case STRUCTURES -> "structure";
			};
		}

		/** Node name inside the V33a XML resource. */
		public String descriptionNode() {
			return switch (section) {
				case MACHINES, ABILITIES -> sourceId;
				case INFO, BLOCKS, TOOLS, RESOURCES, STRUCTURES -> id.toLowerCase(Locale.ROOT);
			};
		}
	}

}
