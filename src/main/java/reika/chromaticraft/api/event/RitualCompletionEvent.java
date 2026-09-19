package reika.chromaticraft.api.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fired after an altar grants its chosen ability to the ritual player. */
public final class RitualCompletionEvent extends PlayerEvent {

	public final String abilityID;

	public RitualCompletionEvent(Player player, String id) {
		super(player);
		abilityID = id;
	}
}
