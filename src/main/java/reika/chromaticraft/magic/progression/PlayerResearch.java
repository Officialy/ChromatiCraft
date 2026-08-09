package reika.chromaticraft.magic.progression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/** Server-authoritative V33a information-fragment ownership and selection rules. */
public final class PlayerResearch {

	private static final String FRAGMENTS_TAG = "fragments";
	private static final Map<String, Map<String, Integer>> PRIORITIES = Map.of(
			"entry", Map.of("FRAGMENT", -1),
			"runecraft", Map.of("CRAFTING", 10, "CASTING1", 5, "NETHERKEY", 20),
			"multicraft", Map.of("CASTING2", 10),
			"pyloncraft", Map.of("CASTING3", 10),
			"networking", Map.of("REPEATER", 100, "COMPOUND", 10,
					"REPEATERSTRUCT", 25, "COMPOUNDSTRUCT", 2),
			"energy", Map.of("ENERGY", 100, "SELFCHARGE", 50, "TRANSMISSION", 20));
	private static final Map<String, String> HARD_DEPENDENCIES = Map.of(
			"TELEGATELOCK", "GATE", "TRANSMISSION", "ENERGY", "RELAY", "ENERGY");

	private PlayerResearch() {}

	public static Set<String> fragments(Player player) {
		CompoundTag research = researchTag(player);
		LinkedHashSet<String> pages = new LinkedHashSet<>();
		for (Tag tag : research.getListOrEmpty(FRAGMENTS_TAG)) {
			tag.asString().map(LexiconCatalog::byId).filter(entry -> entry != null && entry.obtainable())
					.map(LexiconCatalog.Entry::id).ifPresent(pages::add);
		}
		writeFragments(research, pages);
		return Collections.unmodifiableSet(pages);
	}

	public static boolean hasFragment(Player player, LexiconCatalog.Entry page) {
		return page.readableWithoutFragment() || fragments(player).contains(page.id());
	}

	public static boolean giveFragment(Player player, LexiconCatalog.Entry page, boolean notify) {
		if (player.level().isClientSide() || page == null || !page.obtainable() || hasFragment(player, page))
			return false;
		LinkedHashSet<String> pages = new LinkedHashSet<>(fragments(player));
		if (!pages.add(page.id()))
			return false;
		writeFragments(researchTag(player), pages);
		sync(player);
		if (notify)
			player.sendOverlayMessage(page.section().title().copy().append(": ").append(page.title()));
		checkForUpgrade(player);
		return true;
	}

	public static boolean removeFragment(Player player, LexiconCatalog.Entry page) {
		if (player.level().isClientSide() || page == null || page.alwaysPresent())
			return false;
		LinkedHashSet<String> pages = new LinkedHashSet<>(fragments(player));
		if (!pages.remove(page.id()))
			return false;
		writeFragments(researchTag(player), pages);
		sync(player);
		return true;
	}

	public static List<LexiconCatalog.Entry> nextResearch(Player player) {
		ResearchLevel level = ResearchProgress.getLevel(player);
		List<LexiconCatalog.Entry> priority = priorityCandidates(player, level);
		if (!priority.isEmpty())
			return priority;
		ArrayList<LexiconCatalog.Entry> candidates = new ArrayList<>();
		for (LexiconCatalog.Entry page : LexiconCatalog.obtainablePages()) {
			if (hasFragment(player, page) || page.level() == null || !level.isAtLeast(page.level()))
				continue;
			if (page.canPlayerProgressTo(player) && hasDependency(player, page))
				candidates.add(page);
		}
		return List.copyOf(candidates);
	}

	public static LexiconCatalog.Entry randomNextResearch(Player player, RandomSource random) {
		ResearchLevel level = ResearchProgress.getLevel(player);
		Map<String, Integer> weights = PRIORITIES.getOrDefault(level.name().toLowerCase(), Map.of());
		List<LexiconCatalog.Entry> priorities = priorityCandidates(player, level);
		if (!priorities.isEmpty()) {
			for (LexiconCatalog.Entry page : priorities) {
				if (weights.getOrDefault(page.id(), 0) < 0)
					return page;
			}
			int total = priorities.stream().mapToInt(page -> Math.max(0, weights.getOrDefault(page.id(), 0))).sum();
			if (total > 0) {
				int pick = random.nextInt(total);
				for (LexiconCatalog.Entry page : priorities) {
					pick -= Math.max(0, weights.getOrDefault(page.id(), 0));
					if (pick < 0)
						return page;
				}
			}
		}
		List<LexiconCatalog.Entry> pages = nextResearch(player);
		return pages.isEmpty() ? null : pages.get(random.nextInt(pages.size()));
	}

	private static List<LexiconCatalog.Entry> priorityCandidates(Player player, ResearchLevel level) {
		Map<String, Integer> weights = PRIORITIES.getOrDefault(level.name().toLowerCase(), Map.of());
		ArrayList<LexiconCatalog.Entry> pages = new ArrayList<>();
		for (String id : weights.keySet()) {
			LexiconCatalog.Entry page = LexiconCatalog.byId(id);
			if (page != null && !hasFragment(player, page) && page.canPlayerProgressTo(player)
					&& hasDependency(player, page))
				pages.add(page);
		}
		return pages;
	}

	private static boolean hasDependency(Player player, LexiconCatalog.Entry page) {
		String dependency = HARD_DEPENDENCIES.get(page.id());
		return dependency == null || hasFragment(player, LexiconCatalog.byId(dependency));
	}

	private static void checkForUpgrade(Player player) {
		ResearchLevel level = ResearchProgress.getLevel(player);
		if (level == ResearchLevel.CTM)
			return;
		for (LexiconCatalog.Entry page : LexiconCatalog.obtainablePages()) {
			if (page.level() == level && isTierGate(page, level) && !hasFragment(player, page))
				return;
		}
		ResearchLevel next = level.post();
		if (next.canProgressTo(player))
			ResearchProgress.stepTo(player, next, true);
	}

	private static boolean isTierGate(LexiconCatalog.Entry page, ResearchLevel level) {
		if (Set.of("TINKERTOOLS", "TRAPFLOOR", "VOIDESSENCE").contains(page.id()))
			return false;
		for (ProgressStage stage : page.requiredProgress()) {
			if (!stage.isGating(level))
				return false;
		}
		return page.section() != LexiconCatalog.Section.STRUCTURES
				|| !Set.of("PYLON", "CAVERN", "BURROW", "OCEAN", "DESERT", "SNOW", "DATATOWER")
						.contains(page.id());
	}

	private static CompoundTag researchTag(Player player) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(player);
		CompoundTag research = NBTCompat.getCompound(root, ResearchProgress.ROOT_TAG);
		root.put(ResearchProgress.ROOT_TAG, research);
		return research;
	}

	private static void writeFragments(CompoundTag research, Collection<String> pages) {
		ListTag list = new ListTag();
		for (String page : pages)
			list.add(StringTag.valueOf(page));
		research.put(FRAGMENTS_TAG, list);
	}

	private static void sync(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			try {
				ReikaPlayerAPI.syncCustomData(serverPlayer);
			}
			catch (Exception ignored) {
				// A mock GameTest player has no connection; persistent server state is already authoritative.
			}
		}
	}
}
