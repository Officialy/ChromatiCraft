package reika.chromaticraft.client.sound;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;

import reika.chromaticraft.tileentity.TileEntityCrystalPortal;

/** Maintains one continuous ambient loop for each loaded, formed Portal Rift centre. */
public final class PortalSoundManager {

    private record Key(ClientLevel level, BlockPos pos) {}

    private static final Map<Key, PortalSoundInstance> ACTIVE = new HashMap<>();

    private PortalSoundManager() {}

    public static void tick(TileEntityCrystalPortal portal) {
        if (!(portal.getLevel() instanceof ClientLevel level)
                || !portal.isPadCentre() || !portal.isComplete())
            return;
        if (level.getGameTime() % 600 == 0)
            ACTIVE.values().removeIf(PortalSoundInstance::isStopped);
        Key key = new Key(level, portal.getBlockPos().immutable());
        PortalSoundInstance current = ACTIVE.get(key);
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        if (current != null && !current.isStopped() && sounds.isActive(current))
            return;
        PortalSoundInstance next = new PortalSoundInstance(portal);
        ACTIVE.put(key, next);
        sounds.play(next);
    }
}
