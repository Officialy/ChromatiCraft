package reika.chromaticraft.api;

import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.api.abilityapi.Ability;

public interface AbilityAPI {

	void addAbility(Ability ability);

	boolean playerHasAbility(Player player, Ability ability);
}
