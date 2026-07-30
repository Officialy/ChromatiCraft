package reika.chromaticraft.client.sound;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;

import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/** Maintains exactly one immediately-started ambient loop for each loaded, assembled pylon. */
public final class PylonSoundManager {

    private record Key(ClientLevel level, BlockPos pos) {}

    private static final Map<Key, PylonSoundInstance> ACTIVE = new HashMap<>();

    private PylonSoundManager() {}

    public static void tick(TileEntityCrystalPylon pylon) {
        if (!(pylon.getLevel() instanceof ClientLevel level) || !pylon.hasStructure())
            return;
        if (level.getGameTime() % 600 == 0)
            ACTIVE.values().removeIf(PylonSoundInstance::isStopped);
        Key key = new Key(level, pylon.getBlockPos().immutable());
        PylonSoundInstance current = ACTIVE.get(key);
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        if (current != null && !current.isStopped() && sounds.isActive(current))
            return;
        PylonSoundInstance next = new PylonSoundInstance(pylon);
        ACTIVE.put(key, next);
        sounds.play(next);
    }
}