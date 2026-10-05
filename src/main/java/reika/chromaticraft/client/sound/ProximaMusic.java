package reika.chromaticraft.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.Music;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.MonumentRitualEffects;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaSounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** V33a's shuffled Proxima soundtrack, using the 26.3 situational music selector. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ProximaMusic {

    private static final List<ChromaSounds> FRESH = new ArrayList<>();
    private static ChromaSounds selected;
    private static boolean played;

    private ProximaMusic() {}

    @SubscribeEvent
    public static void select(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null
                || !mc.level.dimension().equals(ChromaDimensions.PROXIMA)) {
            // On departure, stop the dimension track before the Overworld chooses its own music.
            if (selected != null) event.overrideMusic(null);
            selected = null;
            played = false;
            FRESH.clear();
            return;
        }
        if (MonumentRitualEffects.isRunning()) {
            selected = null;
            played = false;
            event.overrideMusic(null);
            return;
        }

        boolean currentIsSelected = selected != null && event.getPlayingMusic() != null
                && selected.getSoundEvent().location().equals(event.getPlayingMusic().getIdentifier());
        if (played && !currentIsSelected) {
            // The previous recording finished; the music manager supplies V33a's 300-1200 tick pause.
            selected = null;
            played = false;
        }
        if (selected == null) selected = next(mc);
        if (selected != null) {
            played = currentIsSelected;
            event.overrideMusic(new Music(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(selected.getSoundEvent()),
                    300, 1200, true));
        }
    }

    private static ChromaSounds next(Minecraft mc) {
        if (FRESH.isEmpty()) {
            for (ChromaSounds sound : ChromaSounds.values())
                if (sound.isProximaTrack()) FRESH.add(sound);
            Collections.shuffle(FRESH);
        }
        while (!FRESH.isEmpty()) {
            ChromaSounds sound = FRESH.removeFirst();
            if ((sound != ChromaSounds.PROXIMA_ETHEREAL_CONTACT
                    && sound != ChromaSounds.PROXIMA_POLYCHROMACY)
                    || ProgressStage.CTM.isPlayerAtStage(mc.player))
                return sound;
        }
        return null;
    }
}
