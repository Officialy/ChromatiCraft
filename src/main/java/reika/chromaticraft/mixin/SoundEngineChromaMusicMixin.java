package reika.chromaticraft.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;

import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;

import reika.chromaticraft.client.sound.ChromaMusicVolume;

/**
 * Client-only: ChromatiCraft's music takes its volume from the ChromatiCraft Music slider times Master, not from
 * the vanilla Music slider (V33a's separate {@code CHROMA_MUSIC} category). The music manager's fade gain still
 * applies. A track starts at the engine's ordinary volume; refreshing the MUSIC source right after it starts
 * re-applies ours through {@code calculateVolume(SoundInstance)}.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineChromaMusicMixin {

	@Shadow @Final private Options options;
	@Shadow @Final private Object2FloatMap<SoundSource> gainBySource;

	@Shadow public abstract void refreshCategoryVolume(SoundSource source);

	@Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("HEAD"),
			cancellable = true)
	private void chromaticraft$musicVolume(SoundInstance instance, CallbackInfoReturnable<Float> callback) {
		if (ChromaMusicVolume.isChromaMusic(instance.getIdentifier()))
			callback.setReturnValue(ChromaMusicVolume.engineVolume(instance.getVolume(), options,
					gainBySource.getFloat(instance.getSource())));
	}

	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
			at = @At("RETURN"))
	private void chromaticraft$applyMusicVolume(SoundInstance instance, CallbackInfoReturnable<?> callback) {
		if (ChromaMusicVolume.isChromaMusic(instance.getIdentifier()))
			refreshCategoryVolume(instance.getSource());
	}
}
