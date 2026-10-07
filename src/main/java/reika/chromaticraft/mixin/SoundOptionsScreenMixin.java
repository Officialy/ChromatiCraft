package reika.chromaticraft.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;

import reika.chromaticraft.client.sound.ChromaMusicVolume;

/** Client-only: the ChromatiCraft Music slider joins the per-category volume grid on Music &amp; Sounds. */
@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin {

	@Inject(method = "getAllSoundOptionsExceptMaster()[Lnet/minecraft/client/OptionInstance;", at = @At("RETURN"),
			cancellable = true)
	private void chromaticraft$addMusicSlider(CallbackInfoReturnable<OptionInstance<?>[]> callback) {
		OptionInstance<?>[] options = callback.getReturnValue();
		OptionInstance<?>[] extended = Arrays.copyOf(options, options.length + 1);
		extended[options.length] = ChromaMusicVolume.option();
		callback.setReturnValue(extended);
	}
}
