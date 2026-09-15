package reika.chromaticraft.api;

import java.util.Map;

import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;

public interface RitualAPI {

	boolean isPlayerUndergoingRitual(Player player);

	void addRitual(Ability ability, Map<? extends CrystalElementProxy, Integer> elements);
}
