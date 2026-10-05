package reika.chromaticraft.api;

import net.minecraft.world.entity.player.Player;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.abilityapi.Ability;

import java.util.Map;

public interface RitualAPI {

	boolean isPlayerUndergoingRitual(Player player);

	void addRitual(Ability ability, Map<? extends CrystalElementProxy, Integer> elements);
}
