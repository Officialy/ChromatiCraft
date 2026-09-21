package reika.chromaticraft.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.auxiliary.recipemanagers.AbilityRituals;
import reika.chromaticraft.auxiliary.ability.AbilityHelper;
import reika.chromaticraft.auxiliary.ability.AbilityEffects;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * V33a {@code Chromabilities}: the thirty-nine powers a player can ritual into existence.
 *
 * <p><b>Data only.</b> Upstream's enum runs to 744 lines because every constant carries its own
 * behaviour, and every one of those switches calls into the 1,847-line {@code AbilityHelper}, which
 * is not ported. What is here is everything that <em>describes</em> an ability rather than performs
 * it: identity, tick shape, power levels, mod gate, ritual cost. That is the whole of what the guide
 * book reads, which is why its ability and ritual pages do not have to wait for the engine.
 *
 * <p>The behaviour methods below are present and honest about being inert. They are not stubs
 * standing in for missing knowledge — they are the seam the engine attaches at, each marked
 * CHROMA-PORT so none of them can be mistaken for finished work.
 */
public enum Chromabilities implements Ability {

	REACH(null, true),
	MAGNET(Phase.END, false),
	SONIC(null, true),
	SHIFT(null, false),
	HEAL(null, false),
	SHIELD(Phase.START, false),
	FIREBALL(null, false),
	COMMUNICATE(Phase.START, false),
	HEALTH(null, true),
	PYLON(null, false),
	LIGHTNING(null, false),
	LIFEPOINT(null, false, "BLOODMAGIC"),
	DEATHPROOF(null, false),
	HOTBAR(null, true),
	SHOCKWAVE(null, true),
	TELEPORT(null, true),
	LEECH(null, false),
	FLOAT(Phase.END, true),
	SPAWNERSEE(null, true),
	BREADCRUMB(null, true),
	RANGEDBOOST(null, false),
	DIMPING(null, false),
	DASH(Phase.END, false),
	LASER(null, true),
	FIRERAIN(Phase.START, true),
	KEEPINV(null, false),
	ORECLIP(Phase.START, true),
	DOUBLECRAFT(null, true),
	GROWAURA(Phase.END, true),
	RECHARGE(null, false),
	MEINV(null, false, "APPENG"),
	MOBSEEK(null, true),
	BEEALYZE(null, true),
	NUKER(Phase.START, false),
	LIGHTCAST(null, false),
	JUMP(null, false),
	SUPERBUILD(null, false),
	CHESTCLEAR(Phase.END, false),
	MOBBAIT(null, false);

	private final boolean tickBased;
	private final Phase tickPhase;
	private final boolean actOnClient;
	/** V33a holds a DragonAPI {@code ModList}; the port keeps the mod id, as that enum is not ported. */
	private final String dependency;

	public static final int MAX_REACH = 128;

	public static final Chromabilities[] abilities = values();
	private static final Map<String, Ability> externalAbilities = new LinkedHashMap<>();

	Chromabilities(Phase tick, boolean client) {
		this(tick, client, null);
	}

	Chromabilities(Phase tick, boolean client, String mod) {
		tickBased = tick != null;
		tickPhase = tick;
		actOnClient = client;
		dependency = mod;
	}

	@Override
	public String getID() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public String getDisplayName() {
		// CHROMA-PORT: V33a scrambles the name until the player holds the ability's lexicon fragment
		// (deobfuscateIf). The fragment check is ported but the obfuscated font type is not, so the
		// name currently reads plainly; restore the scramble with the font work.
		return Component.translatable("chromability." + this.getID()).getString();
	}

	@Override
	public String getDescription() {
		return Component.translatable("chromability." + this.getID() + ".desc").getString();
	}

	@Override
	public Identifier getTexture(boolean gray) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"textures/ability/" + this.getID() + (gray ? "_g" : "") + ".png");
	}

	@Override
	public boolean isTickBased() {
		return tickBased;
	}

	@Override
	public Phase getTickPhase() {
		return tickPhase;
	}

	@Override
	public boolean actOnClient() {
		return actOnClient;
	}

	/** The mod id this ability needs, or null if it is unconditional. */
	public String getModDependency() {
		return dependency;
	}

	/** V33a: HOTBAR is disabled outright, and a mod-gated ability is absent when its mod is not loaded. */
	public boolean isDummiedOut() {
		return this == HOTBAR
				|| (dependency != null && !net.neoforged.fml.ModList.get().isLoaded(dependency));
	}

	/** V33a costsPerTick: the ambient powers, which pay continuously rather than on use. */
	@Override
	public boolean costsPerTick() {
		return switch (this) {
			case BEEALYZE -> true;
			case BREADCRUMB -> true;
			case DASH -> true;
			case DEATHPROOF -> true;
			case DOUBLECRAFT -> true;
			case FIRERAIN -> true;
			case GROWAURA -> true;
			case HEALTH -> true;
			case KEEPINV -> true;
			case LEECH -> true;
			case MEINV -> true;
			case MOBSEEK -> true;
			case NUKER -> true;
			case ORECLIP -> true;
			case PYLON -> true;
			case RANGEDBOOST -> true;
			case REACH -> true;
			case RECHARGE -> true;
			case SPAWNERSEE -> true;
			default -> false;
		};
	}

	/** V33a getMaxPower: how many levels a trigger-type ability can be fired at. */
	@Override
	public int getMaxPower() {
		return switch (this) {
			case BREADCRUMB -> 12;
			case FIREBALL -> 8;
			case GROWAURA -> 3;
			case HEAL -> 4;
			case HEALTH -> 50;
			case JUMP -> 8;
			case LIGHTNING -> 2;
			case MAGNET -> 1;
			case REACH -> AbilityHelper.REACH_SCALE.length - 1;
			case SHIFT -> 24;
			case SONIC -> 12;
			default -> 0;
		};
	}

	/**
	 * V33a isPureEventDriven: an ability with no ambient half at all. The ones returning false have a
	 * trigger that sets them up and then keep working, which is why the distinction exists.
	 */
	@Override
	public boolean isPureEventDriven() {
		return switch (this) {
			case DIMPING -> true;
			case FIREBALL -> true;
			case HEAL -> true;
			case HOTBAR -> true;
			case JUMP -> true;
			case LASER -> true;
			case LIGHTCAST -> true;
			case LIGHTNING -> true;
			case MOBBAIT -> true;
			case SHOCKWAVE -> true;
			case SONIC -> true;
			case TELEPORT -> true;
			default -> false;
		};
	}

	/** The elemental aura this ability's ritual consumes. */
	public ElementTagCompound getRitualCost() {
		return AbilityRituals.instance.getAura(this);
	}

	public static Ability getAbility(String id) {
		for (Chromabilities a : abilities) {
			if (!a.isDummiedOut() && a.getID().equals(id))
				return a;
		}
		return externalAbilities.get(id);
	}

	public static synchronized void addAbility(Ability ability) {
		String id = ability.getID();
		if (id == null || id.isEmpty() || id.equals("null") || id.equals(" ")
				|| id.equals("all") || id.equals("none"))
			throw new IllegalArgumentException("Invalid ability ID " + id);
		if (getAbility(id) != null)
			throw new IllegalArgumentException("Ability ID " + id + " already registered");
		externalAbilities.put(id, ability);
	}

	/** Every ability the current mod set actually offers. */
	public static List<Ability> getAbilities() {
		List<Ability> li = new ArrayList<>();
		for (Chromabilities a : abilities) {
			if (!a.isDummiedOut())
				li.add(a);
		}
		li.addAll(externalAbilities.values());
		return li;
	}

	/** Maximum selectable V33a power after constraining it to the player's current lumen buffer. */
	public static int maxPower(Player player, Ability ability) {
		int base = ability.getMaxPower();
		if (player.getAbilities().instabuild || base <= 0)
			return base;
		int level = base;
		ElementTagCompound perLevel = AbilityHelper.instance.getElementsFor(ability).scale(0.01F);
		for (reika.chromaticraft.registry.CrystalElement element : perLevel.elementSet())
			level = Math.min(level, PlayerElementBuffer.instance.getPlayerContent(player, element)
					/ perLevel.getValue(element));
		return Math.max(1, level);
	}

	public boolean enabledOn(Player player) {
		return ChromaAbilityData.enabledOn(player, this);
	}

	public boolean playerHasAbility(Player player) {
		return ChromaAbilityData.hasAbility(player, this);
	}

	public void setToPlayer(Player player, boolean enabled) {
		ChromaAbilityData.setToPlayer(player, enabled, this);
	}

	public void give(Player player) {
		ChromaAbilityData.give(player, this);
	}

	public void removeFromPlayer(Player player) {
		ChromaAbilityData.removeFromPlayer(player, this);
	}

	/**
	 * Server-authoritative V33a ability activation: pay the correctly scaled lumen cost, toggle the
	 * persisted state, apply the two parameterized ambient settings, and fire event-driven powers.
	 */
	public static boolean triggerAbility(Player player, Ability ability, int level) {
		return triggerAbility(player, ability, level, 1);
	}

	public static boolean triggerAbility(Player player, Ability ability, int level, float costScale) {
		if (player.level().isClientSide() || ability == null || !ChromaAbilityData.hasAbility(player, ability))
			return false;
		if (!ability.isAvailableToPlayer(player) || !ability.canPlayerExecuteAt(player))
			return false;
		if (level < 0 || level > ability.getMaxPower() || !Float.isFinite(costScale) || costScale < 0)
			return false;

		ElementTagCompound use = AbilityHelper.instance.getUsageElementsFor(ability, player).scale(costScale);
		if (ability == HEALTH)
			use.scale(10 * (1 + level));
		else if (ability == SHIFT)
			use.scale(10);
		else if (ability == LIGHTNING)
			use.scale(10 * (1 + level * level));
		else if (ability == BREADCRUMB)
			use.scale(5 * (1 + level * 4));
		else if (ability == LIFEPOINT)
			use.scale(5);
		else if (ability == DIMPING)
			use.scale(125);
		else if (ability == LASER)
			use.scale(800);
		else if (ability == LIGHTCAST)
			use.scale(20);
		else if (ability == JUMP)
			use.scale(1 + level);

		if (!PlayerElementBuffer.instance.playerHas(player, use))
			return false;

		ProgressStage.ABILITY.stepPlayerTo(player);
		boolean wasEnabled = ChromaAbilityData.enabledOn(player, ability) || ability.isPureEventDriven();
		ChromaAbilityData.setToPlayer(player, !wasEnabled, ability);
		if (wasEnabled)
			ability.onRemoveFromPlayer(player);

		if (ability == MAGNET)
			AbilityHelper.instance.setNoClippingMagnet(player, !wasEnabled && level > 0);
		if (ability == GROWAURA)
			AbilityHelper.instance.setGrowAuraState(player, !wasEnabled ? level : 0);

		if (ability.isTickBased())
			return true;
		if (!ability.trigger(player, level))
			return false;
		PlayerElementBuffer.instance.removeFromPlayer(player, use);
		return true;
	}

	@Override
	public boolean canPlayerExecuteAt(Player player) {
		return true;
	}

	// ---------------------------------------------------------------------------------------------
	// The engine seam. Everything below needs AbilityHelper, which is not ported; see TODO.md.
	// ---------------------------------------------------------------------------------------------

	/**
	 * Whether the player has met the prerequisites to ritual this ability. The progression gates are
	 * ported even though the abilities themselves are not, so this answers honestly rather than
	 * refusing everything.
	 */
	@Override
	public boolean isAvailableToPlayer(Player player) {
		return !this.isDummiedOut()
				&& reika.chromaticraft.auxiliary.ability.AbilityHelper.instance
						.playerCanGetAbility(this, player);
	}

	/** What this ability draws from the player's buffer each tick while it runs. */
	public ElementTagCompound getTickCost(Player player) {
		return reika.chromaticraft.auxiliary.ability.AbilityHelper.instance.getTickCost(this, player);
	}

	@Override
	public void apply(Player player) {
		switch (this) {
			case MAGNET -> AbilityEffects.attractItemsAndExperience(player, 24,
					AbilityHelper.instance.isMagnetNoClip(player));
			case SHIELD -> AbilityEffects.stopArrows(player);
			case COMMUNICATE -> AbilityEffects.deAggroMobs(player);
			default -> {
				// Remaining ambient powers are admitted with their dependency clusters rather than
				// replaced by approximate effects here.
			}
		}
	}

	@Override
	public boolean trigger(Player player, int level) {
		return switch (this) {
			case HEAL -> {
				AbilityEffects.heal(player, level);
				yield true;
			}
			case FIREBALL -> AbilityEffects.launchFireball(player, level);
			case HEALTH -> {
				AbilityEffects.setPlayerMaxHealth(player, this.enabledOn(player) ? level : 0);
				yield true;
			}
			case LIGHTNING -> AbilityEffects.spawnLightning(player, level);
			case SHOCKWAVE -> {
				AbilityEffects.shockwave(player);
				yield true;
			}
			case JUMP -> AbilityEffects.jump(player, level);
			default -> false;
		};
	}

	@Override
	public void onRemoveFromPlayer(Player player) {
		switch (this) {
			case HEALTH -> AbilityEffects.setPlayerMaxHealth(player, 0);
			case MAGNET -> AbilityHelper.instance.setNoClippingMagnet(player, false);
			case GROWAURA -> AbilityHelper.instance.setGrowAuraState(player, 0);
			default -> {
			}
		}
	}

	@Override
	public boolean isFunctioningOn(Player player) {
		return this != COMMUNICATE || AbilityHelper.instance.isPeaceActive(player);
	}

	/**
	 * CHROMA-PORT: the per-level blurb. Upstream's text interpolates AbilityHelper's tuning constants
	 * (SONIC_EXPLO_FACTOR and its siblings), so it cannot be written faithfully without the engine.
	 */
	@Override
	public String getPowerDesc(int level) {
		return switch (this) {
			case SONIC -> level + "m radius\nMax explosion resist "
					+ String.format(Locale.ROOT, "%.0f", 12F * level);
			case SHIFT -> level + "m shift";
			case HEAL -> String.format(Locale.ROOT, "%.1f hearts", level / 2F);
			case MAGNET -> level > 0 ? "Phasing" : "Solid";
			case REACH -> AbilityHelper.REACH_SCALE[Math.clamp(level, 0,
					AbilityHelper.REACH_SCALE.length - 1)] + "m";
			case JUMP, FIREBALL, HEALTH, LIGHTNING, BREADCRUMB -> "Level " + level;
			default -> null;
		};
	}
}
