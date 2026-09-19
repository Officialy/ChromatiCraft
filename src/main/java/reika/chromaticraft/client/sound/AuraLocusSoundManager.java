package reika.chromaticraft.client.sound;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;

import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;

/** Maintains one loaded Aura Locus loop per block entity, including while initially inaudible. */
public final class AuraLocusSoundManager {

	private record Key(ClientLevel level, BlockPos pos) {}

	private static final Map<Key, AuraLocusSoundInstance> ACTIVE = new HashMap<>();

	private AuraLocusSoundManager() {}

	public static void tick(TileEntityAuraPoint locus) {
		if (!(locus.getLevel() instanceof ClientLevel level))
			return;
		if (level.getGameTime() % 600 == 0)
			ACTIVE.values().removeIf(AuraLocusSoundInstance::isStopped);
		Key key = new Key(level, locus.getBlockPos().immutable());
		AuraLocusSoundInstance current = ACTIVE.get(key);
		SoundManager sounds = Minecraft.getInstance().getSoundManager();
		if (current != null && !current.isStopped() && sounds.isActive(current))
			return;
		AuraLocusSoundInstance next = new AuraLocusSoundInstance(locus);
		ACTIVE.put(key, next);
		sounds.play(next);
	}
}
