package reika.chromaticraft.client.sound;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import reika.chromaticraft.ChromaClientConfig;
import reika.chromaticraft.registry.ChromaSounds;

/**
 * V33a's "ChromatiCraft Music" sound category ({@code CHROMA_MUSIC}, added through DragonAPI's
 * {@code addSoundCategory}). 26.3's {@code SoundSource} is a closed enum, so the category is a slider of our own on
 * the Music &amp; Sounds screen ({@code SoundOptionsScreenMixin}) and the Proxima tracks take their volume from it
 * times Master instead of from the vanilla Music slider ({@code SoundEngineChromaMusicMixin}).
 */
public final class ChromaMusicVolume {

	private static @Nullable Set<Identifier> tracks;
	private static @Nullable OptionInstance<Double> option;

	private ChromaMusicVolume() {}

	public static float volume() {
		try {
			return (float)Mth.clamp(ChromaClientConfig.MUSIC_VOLUME.get(), 0, 1);
		}
		catch (IllegalStateException notLoadedYet) {
			return 1;
		}
	}

	/** Whether a playing sound is one of ChromatiCraft's music tracks. */
	public static boolean isChromaMusic(@Nullable Identifier sound) {
		if (sound == null || !sound.getNamespace().equals(reika.chromaticraft.ChromatiCraft.MODID))
			return false;
		Set<Identifier> set = tracks;
		if (set == null) {
			set = new HashSet<>();
			for (ChromaSounds s : ChromaSounds.values())
				if (s.isProximaTrack())
					set.add(s.getSoundEvent().location());
			tracks = set;
		}
		return set.contains(sound);
	}

	/** The engine volume of a ChromatiCraft track: its own volume, the ChromatiCraft Music slider and Master. */
	public static float engineVolume(float instanceVolume, Options options, float sourceGain) {
		return Mth.clamp(instanceVolume, 0, 1) * volume()
				* Mth.clamp(options.getSoundSourceVolume(SoundSource.MASTER), 0, 1) * sourceGain;
	}

	/** The slider, labelled with V33a's {@code soundCategory.chroma_music}. */
	public static OptionInstance<Double> option() {
		if (option == null)
			option = new OptionInstance<>("soundCategory.chroma_music", OptionInstance.noTooltip(),
					(caption, value) -> value == 0
							? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
							: Component.translatable("options.percent_value", caption, (int)(value * 100)),
					OptionInstance.UnitDouble.INSTANCE, (double)volume(), value -> {
						ChromaClientConfig.MUSIC_VOLUME.set(value);
						ChromaClientConfig.MUSIC_VOLUME.save();
						// The tracks play in the MUSIC source; refreshing it re-applies our volume to them.
						Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MUSIC);
					});
		return option;
	}
}
