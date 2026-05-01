package com.dooji.kurasu.client.mixin;

import com.dooji.kurasu.client.AccessoryPlacementClient;
import com.dooji.kurasu.client.BlackboardScreen;
import com.dooji.kurasu.client.ChalkDrawingClient;
import com.dooji.kurasu.client.LockpickScreen;
import com.dooji.kurasu.client.SafeCodeScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void onStartUseItem(CallbackInfo ci) {
		if (AccessoryPlacementClient.handleUse() || ChalkDrawingClient.handleUse() || LockpickScreen.tryOpenFromLook() || SafeCodeScreen.tryOpenFromLook() || BlackboardScreen.tryOpenFromLook()) {
			ci.cancel();
		}
	}
}
