package reika.chromaticraft.auxiliary.ability;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.api.AbilityAPI;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.auxiliary.recipemanagers.AbilityRituals;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code AbilityHelper}, data half: what an ability costs to run and what a player must have
 * done before they may ritual it.
 *
 * <p>Upstream's class is 1,847 lines across 138 imports because it also <em>performs</em> all
 * thirty-nine abilities — flight, reach, ore sight, the lot — and reaches into most of the mod to do
 * it. None of that is here. What is here is the part with no dependencies beyond progression and the
 * ritual costs, and it is the part that makes {@link Chromabilities#isAvailableToPlayer} able to
 * answer honestly instead of always saying no.
 *
 * <p>CHROMA-PORT: the effects themselves, and the tick and event handlers that drive them, are their
 * own slice. Until they land an ability can be shown, gated and priced, but not used.
 */
public final class AbilityHelper implements AbilityAPI {

	public static final AbilityHelper instance = new AbilityHelper();
	public static final int[] REACH_SCALE = {8, 12, 16, 32, 64, 128, 192};

	/**
	 * V33a scales an ability's ritual aura down by this to get its running cost. The ritual is what
	 * you pay once; this is what it draws from your buffer while it works.
	 */
	private static final float TICK_COST_SCALE = 0.0008F;
	private static final String LYING_TAG = "lastCommunicateLie";
	private static final int LYING_DURATION = 1200;

	private final Map<Chromabilities, Set<ProgressStage>> progressMap = new EnumMap<>(Chromabilities.class);
	private final Map<Chromabilities, Set<CrystalElement>> structureMap = new EnumMap<>(Chromabilities.class);
	private final Set<UUID> noClippingMagnet = new HashSet<>();
	private final Map<UUID, Integer> growAura = new java.util.HashMap<>();

	private AbilityHelper() {
		this.addProgress();
		for (Map.Entry<Chromabilities, Set<ProgressStage>> entry : progressMap.entrySet()) {
			if (entry.getValue().contains(ProgressStage.STRUCTCOMPLETE)) {
				Set<CrystalElement> colors = new LinkedHashSet<>();
				colors.addAll(AbilityRituals.instance.getAura(entry.getKey()).elementSet());
				structureMap.put(entry.getKey(), colors);
			}
		}
	}

	@Override
	public void addAbility(Ability ability) {
		Chromabilities.addAbility(ability);
	}

	@Override
	public boolean playerHasAbility(Player player, Ability ability) {
		return ChromaAbilityData.hasAbility(player, ability);
	}

	private void addProgress() {
		gate(Chromabilities.FIREBALL, ProgressStage.NETHER);
		gate(Chromabilities.PYLON, ProgressStage.SHOCK);
		gate(Chromabilities.DEATHPROOF, ProgressStage.DIE);
		gate(Chromabilities.TELEPORT, ProgressStage.STRUCTCOMPLETE);
		gate(Chromabilities.SPAWNERSEE, ProgressStage.DIMENSION);
		gate(Chromabilities.SPAWNERSEE, ProgressStage.BREAKSPAWNER);
		gate(Chromabilities.DIMPING, ProgressStage.DIMENSION);
		gate(Chromabilities.COMMUNICATE, ProgressStage.KILLMOB);
		gate(Chromabilities.RANGEDBOOST, ProgressStage.KILLMOB);
		gate(Chromabilities.LEECH, ProgressStage.KILLMOB);
		gate(Chromabilities.LASER, ProgressStage.TURBOCHARGE);
		gate(Chromabilities.FIRERAIN, ProgressStage.NETHER);
		gate(Chromabilities.FIRERAIN, ProgressStage.CTM);
		gate(Chromabilities.KEEPINV, ProgressStage.DIMENSION);
		gate(Chromabilities.ORECLIP, ProgressStage.CTM);
		gate(Chromabilities.DOUBLECRAFT, ProgressStage.CTM);
		gate(Chromabilities.RECHARGE, ProgressStage.STRUCTCOMPLETE);
		gate(Chromabilities.GROWAURA, ProgressStage.RAINBOWLEAF);
		gate(Chromabilities.MEINV, ProgressStage.DIMENSION);
		gate(Chromabilities.MOBSEEK, ProgressStage.STRUCTCOMPLETE);
		gate(Chromabilities.BEEALYZE, ProgressStage.HIVE);
		gate(Chromabilities.BEEALYZE, ProgressStage.LINK);
		gate(Chromabilities.NUKER, ProgressStage.STRUCTCOMPLETE);
		gate(Chromabilities.LIGHTCAST, ProgressStage.BEDROCK);
		gate(Chromabilities.CHESTCLEAR, ProgressStage.TWILIGHT);
		gate(Chromabilities.MOBBAIT, ProgressStage.KILLMOB);
	}

	private void gate(Chromabilities ability, ProgressStage stage) {
		progressMap.computeIfAbsent(ability, k -> new LinkedHashSet<>()).add(stage);
	}

	/** The stages a player must have reached before this ability can be obtained. */
	public Collection<ProgressStage> getProgressFor(Ability a) {
		Set<ProgressStage> set = a instanceof Chromabilities c ? progressMap.get(c) : null;
		return set == null ? Set.of() : Set.copyOf(set);
	}

	/** V33a requires every pylon colour used by a structure-gated ritual to have been recovered. */
	public Collection<CrystalElement> getRequiredStructuresFor(Ability ability) {
		Set<CrystalElement> set = ability instanceof Chromabilities nativeAbility
				? structureMap.get(nativeAbility) : null;
		return set == null ? Set.of() : Set.copyOf(set);
	}

	/**
	 * V33a playerCanGetAbility: an ability with no listed stages is open from the start; otherwise
	 * every one of its stages must be met.
	 */
	public boolean playerCanGetAbility(Ability a, Player player) {
		for (ProgressStage stage : this.getProgressFor(a)) {
			if (!stage.isPlayerAtStage(player))
				return false;
		}
		for (CrystalElement element : this.getRequiredStructuresFor(a)) {
			if (!ProgressionManager.instance.hasPlayerCompletedStructureColor(player, element))
				return false;
		}
		return true;
	}

	public boolean isMagnetNoClip(Player player) {
		return noClippingMagnet.contains(player.getUUID());
	}

	public void setNoClippingMagnet(Player player, boolean enabled) {
		if (enabled)
			noClippingMagnet.add(player.getUUID());
		else
			noClippingMagnet.remove(player.getUUID());
	}

	public int getGrowAuraState(Player player) {
		return growAura.getOrDefault(player.getUUID(), 0);
	}

	public void setGrowAuraState(Player player, int level) {
		if (level > 0)
			growAura.put(player.getUUID(), level);
		else
			growAura.remove(player.getUUID());
	}

	public boolean isPeaceActive(Player player) {
		return player.level().getGameTime()
				- player.getPersistentData().getLongOr(LYING_TAG, 0) > LYING_DURATION;
	}

	/**
	 * V33a getUsageElementsFor: the per-tick draw, which is the ritual aura scaled down and then
	 * adjusted per ability. The three named multipliers are upstream's own tuning; the progression
	 * scaling makes a late-game ability cost more to run, not less, because by then the player's buffer
	 * is orders of magnitude larger.
	 */
	public ElementTagCompound getUsageElementsFor(Ability a, Player player) {
		ElementTagCompound tag = AbilityRituals.instance.getAura(a).scale(TICK_COST_SCALE);
		if (a == Chromabilities.FIRERAIN)
			tag.scale(12.5F);
		else if (a == Chromabilities.ORECLIP)
			tag.scale(20);
		else if (a == Chromabilities.DOUBLECRAFT)
			tag.scale(7.5F);
		else if (this.getProgressFor(a).contains(ProgressStage.CTM))
			tag.scale(5);
		else if (this.getProgressFor(a).contains(ProgressStage.DIMENSION))
			tag.scale(2.5F);
		// CHROMA-PORT: upstream then applies power(0.75) if the player holds an active Efficiency
		// Crystal. ItemEfficiencyCrystal is not ported, so no discount applies yet.
		return tag;
	}

	public ElementTagCompound getElementsFor(Ability ability) {
		return AbilityRituals.instance.getAura(ability);
	}

	/** V33a getTickCost: only the abilities that pay continuously have a per-tick draw. */
	public ElementTagCompound getTickCost(Ability a, Player player) {
		return a.isTickBased() || a.costsPerTick()
				? this.getUsageElementsFor(a, player)
				: new ElementTagCompound();
	}
}
