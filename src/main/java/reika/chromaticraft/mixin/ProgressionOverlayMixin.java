package reika.chromaticraft.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.chromaticraft.client.gui.ProgressionOverlay;

/** HUD layers finish before screens, toasts and F3; progression notes must follow all of them. */
@Mixin(Gui.class)
public abstract class ProgressionOverlayMixin {

	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private GuiRenderState guiRenderState;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void chromaticraft$renderProgressionForeground(DeltaTracker deltaTracker,
			boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo callback) {
		if (shouldRenderLevel && resourcesLoaded) {
			// Use an independent pose/scissor state so screens cannot clip or transform the notes.
			ProgressionOverlay.renderForeground(new GuiGraphicsExtractor(minecraft, guiRenderState, 0, 0));
		}
	}
}
