package com.dooji.kurasu;

import com.dooji.kurasu.block.LockerBlock;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.block.entity.SafeBlockEntity;
import com.dooji.kurasu.item.DrawData;
import com.dooji.kurasu.network.DrawBlackboardPixelPayload;
import com.dooji.kurasu.network.FinishLockpickPayload;
import com.dooji.kurasu.network.PlaceAccessoryPayload;
import com.dooji.kurasu.network.PickUpAccessoryPayload;
import com.dooji.kurasu.network.SaveBlackboardPayload;
import com.dooji.kurasu.network.SaveStickyNotePayload;
import com.dooji.kurasu.network.SubmitSafeActionPayload;
import com.dooji.kurasu.network.ToggleOperatorLockPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class KurasuNetworking {
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(DrawBlackboardPixelPayload.TYPE, DrawBlackboardPixelPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(FinishLockpickPayload.TYPE, FinishLockpickPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PlaceAccessoryPayload.TYPE, PlaceAccessoryPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PickUpAccessoryPayload.TYPE, PickUpAccessoryPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SaveBlackboardPayload.TYPE, SaveBlackboardPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SaveStickyNotePayload.TYPE, SaveStickyNotePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SubmitSafeActionPayload.TYPE, SubmitSafeActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ToggleOperatorLockPayload.TYPE, ToggleOperatorLockPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DrawBlackboardPixelPayload.TYPE, (payload, context) -> context.server().execute(() -> drawBlackboardPixel(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(FinishLockpickPayload.TYPE, (payload, context) -> context.server().execute(() -> finishLockpick(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(PlaceAccessoryPayload.TYPE, (payload, context) -> context.server().execute(() -> placeAccessory(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(PickUpAccessoryPayload.TYPE, (payload, context) -> context.server().execute(() -> pickUpAccessory(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(SaveBlackboardPayload.TYPE, (payload, context) -> context.server().execute(() -> saveBlackboard(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(SaveStickyNotePayload.TYPE, (payload, context) -> context.server().execute(() -> saveStickyNote(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(SubmitSafeActionPayload.TYPE, (payload, context) -> context.server().execute(() -> submitSafeAction(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(ToggleOperatorLockPayload.TYPE, (payload, context) -> context.server().execute(() -> toggleOperatorLock(context.player(), payload)));
	}

	private static void finishLockpick(ServerPlayer player, FinishLockpickPayload payload) {
		if (player.getMainHandItem().getItem() != KurasuItems.LOCKPICK || tooFar(player, payload.blockPos(), 1.0)) {
			return;
		}

		if (!(player.level().getBlockEntity(payload.blockPos()) instanceof LockerBlockEntity locker)) {
			return;
		}

		if (!canUseLockedLocker(player, payload.blockPos(), locker)) {
			return;
		}

		if (!locker.isStructureLocked() || locker.isStructureOpen()) {
			return;
		}

		if (payload.success()) {
			locker.pickStructureOpen();
			player.sendOverlayMessage(Component.translatable("message.kurasu.lockpick_success"));
		} else {
			player.sendOverlayMessage(Component.translatable("message.kurasu.lockpick_failed"));
		}

		if (!player.getAbilities().instabuild) {
			player.getMainHandItem().hurtAndBreak(1, player.level(), player, item -> {
			});
		}
	}

	private static void drawBlackboardPixel(ServerPlayer player, DrawBlackboardPixelPayload payload) {
		Integer color = KurasuItems.getChalkColor(player.getMainHandItem());

		if (color == null || tooFar(player, payload.blockPos(), 4.0)) {
			return;
		}

		if (player.level().getBlockEntity(payload.blockPos()) instanceof BlackboardBlockEntity blackboard) {
			if (!canUseOperatorLocked(player, blackboard.isOperatorLocked(), false)) {
				return;
			}

			blackboard.setPixel(payload.x(), payload.y(), color);
		}
	}

	private static void placeAccessory(ServerPlayer player, PlaceAccessoryPayload payload) {
		String heldAccessory = KurasuItems.getAccessoryId(player.getMainHandItem());

		if (heldAccessory == null || !heldAccessory.equals(payload.accessoryId()) || tooFar(player, payload.blockPos(), 4.0)) {
			return;
		}

		if (!(player.level().getBlockEntity(payload.blockPos()) instanceof AccessoryBlockEntity accessoryBlock)) {
			return;
		}

		if (!canModifyAccessories(player, payload.blockPos(), accessoryBlock)) {
			return;
		}

		if (!accessoryBlock.addAccessory(AccessoryBlockEntity.PlacedAccessory.fromPayload(payload, player.getMainHandItem().copyWithCount(1)))) {
			return;
		}

		if (!player.getAbilities().instabuild) {
			player.getMainHandItem().consume(1, player);
		}
	}

	private static void pickUpAccessory(ServerPlayer player, PickUpAccessoryPayload payload) {
		if (tooFar(player, payload.blockPos(), 4.0)) {
			return;
		}

		if (!(player.level().getBlockEntity(payload.blockPos()) instanceof AccessoryBlockEntity accessoryBlock)) {
			return;
		}

		if (!canModifyAccessories(player, payload.blockPos(), accessoryBlock)) {
			return;
		}

		AccessoryBlockEntity.PlacedAccessory removed = accessoryBlock.removeAccessory(payload.accessoryIndex());
		if (removed == null) {
			return;
		}

		ItemStack stack = removed.itemStack();
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	private static void saveBlackboard(ServerPlayer player, SaveBlackboardPayload payload) {
		if (tooFar(player, payload.blockPos(), 4.0)) {
			return;
		}

		if (player.level().getBlockEntity(payload.blockPos()) instanceof BlackboardBlockEntity blackboard) {
			if (!canUseOperatorLocked(player, blackboard.isOperatorLocked(), false)) {
				return;
			}

			blackboard.setDrawData(new DrawData(payload.width(), payload.height(), payload.pixels()));
		}
	}

	private static void saveStickyNote(ServerPlayer player, SaveStickyNotePayload payload) {
		if (player.getMainHandItem().getItem() == KurasuItems.STICKY_NOTE) {
			DrawData.set(player.getMainHandItem(), new DrawData(payload.width(), payload.height(), payload.pixels()));
		}
	}

	private static void submitSafeAction(ServerPlayer player, SubmitSafeActionPayload payload) {
		if (tooFar(player, payload.blockPos(), 1.0)) {
			return;
		}

		if (!(player.level().getBlockEntity(payload.blockPos()) instanceof SafeBlockEntity safe)) {
			return;
		}

		if (!canUseOperatorLocked(player, safe.isOperatorLocked(), true)) {
			return;
		}

		switch (payload.action()) {
			case SubmitSafeActionPayload.ACTION_SET_CODE -> setSafeCode(player, safe, payload.code());
			case SubmitSafeActionPayload.ACTION_ENTER_CODE -> enterSafeCode(player, safe, payload.code());
			case SubmitSafeActionPayload.ACTION_TOGGLE_OPEN -> toggleSafeOpen(player, safe);
			case SubmitSafeActionPayload.ACTION_LOCK -> lockSafe(player, safe);
			default -> {
			}
		}
	}

	private static void setSafeCode(ServerPlayer player, SafeBlockEntity safe, String code) {
		if (!canUseOperatorLocked(player, safe.isOperatorLocked(), true)) {
			return;
		}

		if (!isValidSafeCode(code)) {
			return;
		}

		safe.setCode(code);
		safe.setStructureState(false, true, "", false);
		player.sendOverlayMessage(Component.translatable("message.kurasu.safe_code_set"));
	}

	private static void enterSafeCode(ServerPlayer player, SafeBlockEntity safe, String code) {
		if (!safe.hasCode()) {
			return;
		}

		if (!safe.matchesCode(code)) {
			player.sendOverlayMessage(Component.translatable("message.kurasu.wrong_code"));
			return;
		}

		safe.setStructureState(true, false, "", false);
		player.sendOverlayMessage(Component.translatable("message.kurasu.safe_unlocked"));
	}

	private static void toggleSafeOpen(ServerPlayer player, SafeBlockEntity safe) {
		if (safe.isStructureOpen()) {
			safe.setStructureState(false, true, "", false);
			player.sendOverlayMessage(Component.translatable("message.kurasu.safe_locked"));
		} else if (!safe.isStructureLocked()) {
			safe.setStructureState(true, false, "", false);
		}
	}

	private static void lockSafe(ServerPlayer player, SafeBlockEntity safe) {
		if (!canUseOperatorLocked(player, safe.isOperatorLocked(), true)) {
			return;
		}

		if (safe.hasCode()) {
			safe.setStructureState(false, true, "", false);
			player.sendOverlayMessage(Component.translatable("message.kurasu.safe_locked"));
		}
	}

	private static void toggleOperatorLock(ServerPlayer player, ToggleOperatorLockPayload payload) {
		if (!isOperator(player) || tooFar(player, payload.blockPos(), 4.0)) {
			return;
		}

		if (player.level().getBlockEntity(payload.blockPos()) instanceof AccessoryBlockEntity blockEntity) {
			blockEntity.setOperatorLocked(!blockEntity.isOperatorLocked());
			player.sendOverlayMessage(Component.translatable(blockEntity.isOperatorLocked() ? "message.kurasu.op_locked" : "message.kurasu.op_unlocked"));
		}
	}

	private static boolean tooFar(ServerPlayer player, BlockPos pos, double extraReach) {
		double reach = player.blockInteractionRange() + extraReach;
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > reach * reach;
	}

	private static boolean canModifyAccessories(ServerPlayer player, BlockPos blockPos, AccessoryBlockEntity accessoryBlock) {
		return canUseOperatorLocked(player, accessoryBlock.isOperatorLocked(), true)
			&& !player.blockActionRestricted(player.level(), blockPos, player.gameMode())
			&& player.mayUseItemAt(blockPos, Direction.UP, player.getMainHandItem());
	}

	private static boolean canUseOperatorLocked(ServerPlayer player, boolean operatorLocked, boolean notify) {
		if (!operatorLocked || isOperator(player)) {
			return true;
		}

		if (notify) {
			player.sendOverlayMessage(Component.translatable("message.kurasu.op_only"));
		}

		return false;
	}

	private static boolean canUseLockedLocker(ServerPlayer player, BlockPos blockPos, LockerBlockEntity locker) {
		if (!locker.isOperatorLocked() || isOperator(player) || isInsideLocker(player, blockPos)) {
			return true;
		}

		player.sendOverlayMessage(Component.translatable("message.kurasu.op_only"));
		return false;
	}

	private static boolean isInsideLocker(ServerPlayer player, BlockPos blockPos) {
		Entity vehicle = player.getVehicle();
		return LockerBlock.isLockerSeat(vehicle) && BlockPos.containing(vehicle.position()).equals(blockPos);
	}

	private static boolean isOperator(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		return server != null && (server.getPlayerList().isOp(player.nameAndId()) || server.isSingleplayerOwner(player.nameAndId()));
	}

	private static boolean isValidSafeCode(String code) {
		if (code.length() != 4) {
			return false;
		}

		for (int i = 0; i < code.length(); i++) {
			if (!Character.isDigit(code.charAt(i))) {
				return false;
			}
		}

		return true;
	}
}
