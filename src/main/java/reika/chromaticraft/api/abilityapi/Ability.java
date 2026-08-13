package reika.chromaticraft.api.abilityapi;

import net.minecraft.world.entity.player.Player;

/**
 * V33a {@code AbilityAPI.Ability}: the contract a Chromability implements, and the extension point
 * other mods add their own powers through.
 *
 * <p>Nested inside {@code AbilityAPI} upstream; the port flattens it into its own file because
 * {@code Chromabilities} refers to it by the package name {@code api.abilityapi}, which is the
 * convention the rest of the ported API surface follows.
 *
 * <p>Every implementation is expected to be a singleton — upstream suggests an enum, which is what
 * {@code Chromabilities} is.
 */
public interface Ability {

	/** The string ID of the ability. Must be unique, and is conventionally lowercase. */
	String getID();

	/**
	 * The image for this ability, 50x50. V33a returns a path relative to
	 * {@code getTextureReferenceClass}; the port returns a resource location instead, because 26.2
	 * addresses textures by namespace rather than by a class's package.
	 *
	 * @param gray whether to use the greyscale variant, shown when the player cannot use the ability
	 */
	net.minecraft.resources.Identifier getTexture(boolean gray);

	/** The maximum power level. Only meaningful for trigger-type abilities. */
	int getMaxPower();

	/**
	 * For trigger-type abilities such as lightning: performs the ability.
	 *
	 * @return whether it actually fired
	 */
	boolean trigger(Player player, int level);

	/** For ambient abilities such as magnet mode: performs one tick of it. */
	void apply(Player player);

	String getDisplayName();

	/** The text shown in the lexicon and on some screens. Long descriptions will overrun. */
	String getDescription();

	/** Called when the ability leaves a player, whether manually, from running dry, or on death. */
	void onRemoveFromPlayer(Player player);

	/** Whether the player may attempt to obtain this ability's fragment yet. */
	boolean isAvailableToPlayer(Player player);

	/** Whether the player may perform it right now. */
	boolean canPlayerExecuteAt(Player player);

	/** Ambient, like magnet mode, as opposed to trigger-based, like lightning. */
	boolean isTickBased();

	/** Only meaningful for tick-based abilities; null otherwise. */
	Phase getTickPhase();

	/**
	 * Which half of the tick an ambient ability runs in. V33a uses Forge's
	 * {@code TickEvent.Phase}; NeoForge 26.2 replaced that enum with separate {@code Pre}/{@code Post}
	 * event classes, so the distinction is carried here instead of being dropped — an ability that has
	 * to act before the world ticks is not interchangeable with one that acts after.
	 */
	enum Phase {
		START,
		END
	}

	/**
	 * Purely trigger-driven. Some abilities have a trigger that sets them up and an ambient part as
	 * well — upstream points at {@code SHIFT} as the example — and those return false.
	 */
	boolean isPureEventDriven();

	/** Whether the ability does its work client-side. */
	boolean actOnClient();

	/** Whether the energy cost is per tick rather than on trigger or toggle. Usually true. */
	boolean costsPerTick();

	/**
	 * Whether the ability is currently doing anything, regardless of being enabled. This is what
	 * decides whether energy is consumed, so an enabled-but-idle ability is free.
	 */
	boolean isFunctioningOn(Player player);

	/** The "what this power level does" blurb shown when selecting a level to fire at. */
	String getPowerDesc(int level);
}
