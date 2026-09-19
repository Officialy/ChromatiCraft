package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.chromaticraft.api.RitualAPI;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * V33a {@code AbilityRituals}: what each Chromability costs to ritual into existence, in elemental
 * aura.
 *
 * <p>These numbers are the guide book's ritual page: the wheel is the proportions between an
 * ability's elements, and the energy bar scales against {@link #getMaxAbilityTotalCost}. They are
 * also the aura the altar consumes on completion. The native keys are validated against the
 * Chromabilities enum; externally registered abilities retain their API-provided IDs.
 *
 * <p>The V33a table-location set and {@link RitualAPI} query are restored. The query only examines
 * already-loaded chunks, avoiding a synchronous distant-chunk request while testing whether a
 * player is inside an ongoing ritual.
 */
public final class AbilityRituals implements RitualAPI {

	public static final AbilityRituals instance = new AbilityRituals();

	/** V33a AbilityRitual: every ritual runs for the same 980 ticks. */
	public static final int DURATION = 980;

	private final Map<String, ElementTagCompound> auras = new LinkedHashMap<>();
	private static final Set<WorldLocation> tables = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private int maxCost;
	private int maxTotalCost;

	private AbilityRituals() {
		this.addRituals();
	}

	private void addRituals() {
		ritual("REACH", e(CrystalElement.LIME, 20000), e(CrystalElement.GRAY, 10000), e(CrystalElement.PURPLE, 5000));
		ritual("MAGNET", e(CrystalElement.LIME, 5000), e(CrystalElement.WHITE, 5000));
		ritual("SONIC", e(CrystalElement.BLACK, 5000), e(CrystalElement.YELLOW, 5000));
		ritual("SHIFT", e(CrystalElement.LIME, 25000), e(CrystalElement.YELLOW, 50000), e(CrystalElement.BROWN, 10000));
		ritual("HEAL", e(CrystalElement.MAGENTA, 50000), e(CrystalElement.LIGHTBLUE, 10000));
		ritual("SHIELD", e(CrystalElement.RED, 20000));
		ritual("FIREBALL", e(CrystalElement.ORANGE, 10000), e(CrystalElement.PINK, 4000));
		ritual("COMMUNICATE", e(CrystalElement.BLACK, 40000), e(CrystalElement.RED, 10000),
				e(CrystalElement.PINK, 12000), e(CrystalElement.LIGHTGRAY, 8000));
		ritual("HEALTH", e(CrystalElement.MAGENTA, 50000), e(CrystalElement.PURPLE, 25000));
		ritual("PYLON", e(CrystalElement.BLACK, 2000), e(CrystalElement.YELLOW, 5000), e(CrystalElement.RED, 25000));
		ritual("LIGHTNING", e(CrystalElement.BLACK, 5000), e(CrystalElement.YELLOW, 40000), e(CrystalElement.PINK, 10000), e(CrystalElement.ORANGE, 2000));
		ritual("LIFEPOINT", e(CrystalElement.MAGENTA, 25000), e(CrystalElement.BLACK, 5000), e(CrystalElement.RED, 5000));
		ritual("DEATHPROOF", e(CrystalElement.BLACK, 10000), e(CrystalElement.WHITE, 10000), e(CrystalElement.PINK, 5000), e(CrystalElement.PURPLE, 15000), e(CrystalElement.RED, 5000));
		ritual("HOTBAR", e(CrystalElement.LIME, 20000), e(CrystalElement.GRAY, 5000), e(CrystalElement.PURPLE, 25000));
		ritual("SHOCKWAVE", e(CrystalElement.RED, 20000), e(CrystalElement.PINK, 25000), e(CrystalElement.YELLOW, 10000));
		ritual("LEECH", e(CrystalElement.PINK, 10000), e(CrystalElement.MAGENTA, 15000));
		ritual("TELEPORT", e(CrystalElement.LIME, 100000), e(CrystalElement.BLACK, 75000), e(CrystalElement.PURPLE, 25000), e(CrystalElement.GRAY, 40000));
		ritual("FLOAT", e(CrystalElement.LIME, 5000), e(CrystalElement.BLACK, 2000), e(CrystalElement.CYAN, 25000));
		ritual("SPAWNERSEE", e(CrystalElement.BLUE, 100000), e(CrystalElement.BLACK, 20000), e(CrystalElement.PINK, 25000), e(CrystalElement.LIGHTGRAY, 25000));
		ritual("BREADCRUMB", e(CrystalElement.BLUE, 40000), e(CrystalElement.LIGHTBLUE, 10000), e(CrystalElement.LIME, 25000), e(CrystalElement.LIGHTGRAY, 5000));
		ritual("DIMPING", e(CrystalElement.BLUE, 40000), e(CrystalElement.BLACK, 100000), e(CrystalElement.LIME, 75000));
		ritual("RANGEDBOOST", e(CrystalElement.PINK, 25000), e(CrystalElement.PURPLE, 20000), e(CrystalElement.BLACK, 10000), e(CrystalElement.LIME, 15000));
		ritual("DASH", e(CrystalElement.LIGHTBLUE, 5000), e(CrystalElement.BLACK, 2000), e(CrystalElement.LIME, 10000));
		ritual("LASER", e(CrystalElement.PINK, 360000), e(CrystalElement.BLUE, 270000), e(CrystalElement.BLACK, 90000), e(CrystalElement.YELLOW, 720000));
		ritual("FIRERAIN", e(CrystalElement.PINK, 270000), e(CrystalElement.BLACK, 120000), e(CrystalElement.ORANGE, 480000), e(CrystalElement.CYAN, 60000));
		ritual("KEEPINV", e(CrystalElement.LIGHTGRAY, 30000), e(CrystalElement.BLACK, 20000), e(CrystalElement.PINK, 60000), e(CrystalElement.RED, 120000), e(CrystalElement.WHITE, 40000), e(CrystalElement.MAGENTA, 80000));
		ritual("ORECLIP", e(CrystalElement.BLACK, 90000), e(CrystalElement.BLUE, 90000), e(CrystalElement.LIME, 600000), e(CrystalElement.LIGHTGRAY, 90000), e(CrystalElement.PURPLE, 270000), e(CrystalElement.BROWN, 360000), e(CrystalElement.CYAN, 90000));
		ritual("DOUBLECRAFT", e(CrystalElement.BLACK, 180000), e(CrystalElement.GRAY, 450000), e(CrystalElement.PURPLE, 900000), e(CrystalElement.BROWN, 240000), e(CrystalElement.LIGHTBLUE, 60000));
		ritual("RECHARGE", e(CrystalElement.BLACK, 90000), e(CrystalElement.YELLOW, 120000), e(CrystalElement.LIME, 120000), e(CrystalElement.PURPLE, 90000), e(CrystalElement.WHITE, 40000));
		ritual("GROWAURA", e(CrystalElement.BLACK, 120000), e(CrystalElement.MAGENTA, 270000), e(CrystalElement.GREEN, 180000), e(CrystalElement.RED, 120000), e(CrystalElement.GRAY, 60000));
		ritual("MEINV", e(CrystalElement.BLACK, 120000), e(CrystalElement.LIGHTGRAY, 180000), e(CrystalElement.LIME, 180000), e(CrystalElement.BROWN, 120000), e(CrystalElement.WHITE, 240000));
		ritual("MOBSEEK", e(CrystalElement.BLACK, 90000), e(CrystalElement.BLUE, 60000), e(CrystalElement.LIME, 400000), e(CrystalElement.LIGHTGRAY, 60000), e(CrystalElement.PURPLE, 120000));
		ritual("BEEALYZE", e(CrystalElement.GREEN, 60000), e(CrystalElement.BLUE, 20000), e(CrystalElement.MAGENTA, 60000), e(CrystalElement.PURPLE, 10000));
		ritual("LIGHTCAST", e(CrystalElement.BLUE, 100000), e(CrystalElement.BROWN, 20000), e(CrystalElement.GRAY, 5000));
		ritual("NUKER", e(CrystalElement.BROWN, 200000), e(CrystalElement.LIGHTBLUE, 60000), e(CrystalElement.LIME, 60000), e(CrystalElement.GRAY, 60000), e(CrystalElement.YELLOW, 120000), e(CrystalElement.BLACK, 40000));
		ritual("JUMP", e(CrystalElement.LIME, 20000), e(CrystalElement.LIGHTBLUE, 5000));
		ritual("SUPERBUILD", e(CrystalElement.LIGHTBLUE, 24000), e(CrystalElement.GRAY, 6000), e(CrystalElement.PURPLE, 6000));
		ritual("CHESTCLEAR", e(CrystalElement.LIME, 9000), e(CrystalElement.LIGHTGRAY, 1000), e(CrystalElement.WHITE, 3000));
		ritual("MOBBAIT", e(CrystalElement.LIGHTGRAY, 12000), e(CrystalElement.RED, 9000), e(CrystalElement.PINK, 3000));
	}

	/** Reads better than a builder for a table this shape; see {@link #ritual}. */
	private static Map.Entry<CrystalElement, Integer> e(CrystalElement element, int amount) {
		return Map.entry(element, amount);
	}

	@SafeVarargs
	private void ritual(String abilityId, Map.Entry<CrystalElement, Integer>... costs) {
		Chromabilities.valueOf(abilityId);
		ElementTagCompound tag = new ElementTagCompound();
		int max = 0;
		int total = 0;
		for (Map.Entry<CrystalElement, Integer> cost : costs) {
			tag.addTag(cost.getKey(), cost.getValue());
			max = Math.max(max, cost.getValue());
			total += cost.getValue();
		}
		if (auras.putIfAbsent(abilityId, tag) != null)
			throw new IllegalArgumentException("Ritual already registered for " + abilityId);
		maxCost = Math.max(maxCost, max);
		maxTotalCost = Math.max(maxTotalCost, total);
	}

	public boolean hasRitual(Ability a) {
		return a != null && this.hasRitual(a.getID());
	}

	/** For callers that have an ability's identity but not yet its enum constant. */
	public boolean hasRitual(String abilityId) {
		return auras.containsKey(key(abilityId));
	}

	public ElementTagCompound getAura(Ability a) {
		return a == null ? new ElementTagCompound() : this.getAura(a.getID());
	}

	public ElementTagCompound getAura(String abilityId) {
		ElementTagCompound tag = auras.get(key(abilityId));
		return tag == null ? new ElementTagCompound() : tag.copy();
	}

	public int getDuration(Ability a) {
		return this.hasRitual(a) ? DURATION : 0;
	}

	/** V33a addRitual(Ability, Map): the extension point other mods register their own costs through. */
	@Override
	public void addRitual(Ability a, Map<? extends CrystalElementProxy, Integer> elements) {
		String id = key(a.getID());
		if (auras.containsKey(id))
			throw new IllegalArgumentException("Ritual already registered for " + id);
		ElementTagCompound tag = new ElementTagCompound();
		int max = 0;
		int total = 0;
		for (Map.Entry<? extends CrystalElementProxy, Integer> cost : elements.entrySet()) {
			if (!(cost.getKey() instanceof CrystalElement element))
				throw new IllegalArgumentException("Unknown ritual element " + cost.getKey());
			tag.addTag(element, cost.getValue());
			max = Math.max(max, cost.getValue());
			total += cost.getValue();
		}
		auras.put(id, tag);
		maxCost = Math.max(maxCost, max);
		maxTotalCost = Math.max(maxTotalCost, total);
	}

	public static void addTable(TileEntityRitualTable table) {
		if (table.getLevel() != null) tables.add(new WorldLocation(table));
	}

	public static void removeTable(TileEntityRitualTable table) {
		if (table.getLevel() != null) tables.remove(new WorldLocation(table));
	}

	@Override
	public boolean isPlayerUndergoingRitual(Player player) {
		Level level = player.level();
		for (WorldLocation location : tables) {
			if (!location.getDimension().equals(level.dimension())) continue;
			boolean loaded = level instanceof ServerLevel server
					? server.getChunkSource().getChunkNow(location.pos.getX() >> 4,
							location.pos.getZ() >> 4) != null
					: level.hasChunkAt(location.pos);
			if (!loaded) continue;
			if (level.getBlockEntity(location.pos) instanceof TileEntityRitualTable table
					&& table.isActive() && table.isPlayerUsing(player)) return true;
		}
		return false;
	}

	/** The largest single-element cost of any ability. */
	public int getMaxAbilityCost() {
		return maxCost;
	}

	/** The largest total cost of any ability; the ritual page's energy bar is a fraction of this. */
	public int getMaxAbilityTotalCost() {
		return maxTotalCost;
	}

	public Map<String, ElementTagCompound> allRituals() {
		Map<String, ElementTagCompound> result = new LinkedHashMap<>();
		auras.forEach((id, tag) -> result.put(id, tag.copy()));
		return result;
	}

	/**
	 * Upstream IDs are the enum constant's own name; accepting either case means a caller can pass
	 * {@code getID()} without the port having to guess which convention that method settles on.
	 */
	private static String key(String abilityId) {
		return abilityId == null ? "" : abilityId.toUpperCase(java.util.Locale.ROOT);
	}
}
