package com.dooji.kurasu.client.mixin;

import com.dooji.kurasu.client.AccessoryPlacementClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void onScroll(long windowPointer, double horizontalOffset, double verticalOffset, CallbackInfo ci) {
		if (AccessoryPlacementClient.handleScroll(verticalOffset)) {
			ci.cancel();
		}
	}

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void onTurnPlayer(CallbackInfo ci) {
		if (AccessoryPlacementClient.isPlacing()) {
			ci.cancel();
		}
	}
}
