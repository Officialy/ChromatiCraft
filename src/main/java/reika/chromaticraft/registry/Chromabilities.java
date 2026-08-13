package reika.chromaticraft.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.auxiliary.recipemanagers.AbilityRituals;
import reika.chromaticraft.magic.ElementTagCompound;

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
			case REACH -> 8;
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
			if (a.getID().equals(id))
				return a;
		}
		return null;
	}

	/** Every ability the current mod set actually offers. */
	public static List<Ability> getAbilities() {
		List<Ability> li = new ArrayList<>();
		for (Chromabilities a : abilities) {
			if (!a.isDummiedOut())
				li.add(a);
		}
		return li;
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

	/** CHROMA-PORT: the ambient half, driven by AbilityHelper's tick handlers. */
	@Override
	public void apply(Player player) {}

	/** CHROMA-PORT: the trigger half. Returns false, which upstream reads as "did not fire". */
	@Override
	public boolean trigger(Player player, int level) {
		return false;
	}

	/** CHROMA-PORT: cleanup when an ability leaves a player. */
	@Override
	public void onRemoveFromPlayer(Player player) {}

	/** CHROMA-PORT: whether the ability is doing anything right now, which gates its energy drain. */
	@Override
	public boolean isFunctioningOn(Player player) {
		return false;
	}

	/**
	 * CHROMA-PORT: the per-level blurb. Upstream's text interpolates AbilityHelper's tuning constants
	 * (SONIC_EXPLO_FACTOR and its siblings), so it cannot be written faithfully without the engine.
	 */
	@Override
	public String getPowerDesc(int level) {
		return String.valueOf(level);
	}
}
