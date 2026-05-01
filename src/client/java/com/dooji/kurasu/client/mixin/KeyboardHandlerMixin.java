package com.dooji.kurasu.client.mixin;

import com.dooji.kurasu.client.AccessoryPlacementClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void onEscPress(long windowPointer, int action, KeyEvent event, CallbackInfo ci) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS && AccessoryPlacementClient.handleEscape()) {
			ci.cancel();
		}
	}
}
