package reika.chromaticraft.auxiliary.ability;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.auxiliary.recipemanagers.AbilityRituals;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.Chromabilities;

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
public final class AbilityHelper {

	public static final AbilityHelper instance = new AbilityHelper();

	/**
	 * V33a scales an ability's ritual aura down by this to get its running cost. The ritual is what
	 * you pay once; this is what it draws from your buffer while it works.
	 */
	private static final float TICK_COST_SCALE = 0.0008F;

	private final Map<Chromabilities, Set<ProgressStage>> progressMap = new EnumMap<>(Chromabilities.class);

	private AbilityHelper() {
		this.addProgress();
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

	/**
	 * V33a playerCanGetAbility: an ability with no listed stages is open from the start; otherwise
	 * every one of its stages must be met.
	 */
	public boolean playerCanGetAbility(Ability a, Player player) {
		for (ProgressStage stage : this.getProgressFor(a)) {
			if (!stage.isPlayerAtStage(player))
				return false;
		}
		return true;
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

	/** V33a getTickCost: only the abilities that pay continuously have a per-tick draw. */
	public ElementTagCompound getTickCost(Ability a, Player player) {
		return a.isTickBased() || a.costsPerTick()
				? this.getUsageElementsFor(a, player)
				: new ElementTagCompound();
	}
}
