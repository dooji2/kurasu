package com.dooji.kurasu.mixin;

import com.dooji.kurasu.block.LockerBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
	private void onInteractOn(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		if (hand != InteractionHand.MAIN_HAND) {
			return;
		}

		InteractionResult result = LockerBlock.tryShoveSelectedPlayer((Player) (Object) this, entity);

		if (result != InteractionResult.PASS) {
			cir.setReturnValue(result);
		}
	}

	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void onWantsToStopRiding(CallbackInfoReturnable<Boolean> cir) {
		Player player = (Player) (Object) this;

		if (LockerBlock.isLockerSeat(player.getVehicle())) {
			cir.setReturnValue(false);
		}
	}
}
