package com.dooji.kurasu.client.mixin;

import com.dooji.kurasu.block.LockerBlock;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public class LivingEntityRendererMixin {
	@Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
	private static void onExtractHumanoidRenderState(LivingEntity entity, HumanoidRenderState renderState, float partialTick, ItemModelResolver itemModelResolver, CallbackInfo ci) {
		if (LockerBlock.isLockerSeat(entity.getVehicle())) {
			renderState.isPassenger = false;
		}
	}
}
