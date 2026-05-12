package com.dooji.kurasu.client;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity;
import com.dooji.kurasu.client.render.LockerRaycast;
import com.dooji.kurasu.client.render.LockerRaycast.AccessoryHitMode;
import com.dooji.kurasu.network.PickUpAccessoryPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class AccessoryPlacementClient {
	private static final float MIN_SCALE = 0.5f;
	private static final float MAX_SCALE = 2.5f;
	private static final float SCALE_STEP = 0.1f;

	private static BlockPos activeBlockPos;
	private static AccessoryBlockEntity.PlacedAccessory activeAccessory;
	private static boolean waitingForUseRelease;
	private static double lastMouseX;

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (activeAccessory == null) {
				if (waitingForUseRelease && !client.options.keyUse.isDown()) {
					waitingForUseRelease = false;
				}

				return;
			}

			if (client.player == null || client.level == null || client.screen != null) {
				cancelPlacement();
				return;
			}

			if (KurasuItems.getAccessoryId(client.player.getMainHandItem()) == null) {
				cancelPlacement();
				return;
			}

			double mouseX = client.mouseHandler.xpos();
			activeAccessory = activeAccessory.withRotationAndScale(activeAccessory.rotation() + (float) ((mouseX - lastMouseX) * 0.0125f), activeAccessory.scale());
			lastMouseX = mouseX;

			double reach = client.player.blockInteractionRange() + 4.0;

			if (client.player.distanceToSqr(activeBlockPos.getX() + 0.5, activeBlockPos.getY() + 0.5, activeBlockPos.getZ() + 0.5) > reach * reach) {
				cancelPlacement();
			}

			if (waitingForUseRelease && !client.options.keyUse.isDown()) {
				waitingForUseRelease = false;
			}
		});
	}

	public static boolean handleUse() {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
			return false;
		}

		if (waitingForUseRelease) {
			return true;
		}

		if (activeAccessory != null) {
			ClientPlayNetworking.send(activeAccessory.toPayload(activeBlockPos));
			cancelPlacement();
			waitingForUseRelease = true;
			return true;
		}

		ItemStack heldItem = minecraft.player.getMainHandItem();

		if (heldItem.getItem() == KurasuItems.OP_TOOL) {
			return false;
		}

		String accessoryId = KurasuItems.getAccessoryId(heldItem);

		if (accessoryId == null) {
			LockerRaycast.Hit pickupHit = LockerRaycast.raycastPlayerView(minecraft.player, 1.0f, AccessoryHitMode.ALL);

			if (pickupHit == null || !pickupHit.isAccessoryHit()) {
				return false;
			}

			ClientPlayNetworking.send(new PickUpAccessoryPayload(pickupHit.blockPos(), pickupHit.accessoryIndex()));
			waitingForUseRelease = true;
			return true;
		}

		AccessoryHitMode hitMode = AccessoryHitMode.NONE;
		if (Kurasu.BOOK_1_ACCESSORY_ID.equals(accessoryId)) {
			hitMode = AccessoryHitMode.STACKABLE_ONLY;
		}

		LockerRaycast.Hit hit = LockerRaycast.raycastPlayerView(minecraft.player, 1.0f, hitMode);

		if (hit == null) {
			if (Kurasu.STICKY_NOTE_ACCESSORY_ID.equals(accessoryId)) {
				minecraft.setScreen(new StickyNoteScreen(heldItem));
				waitingForUseRelease = true;
				return true;
			}

			return false;
		}

		activeBlockPos = hit.blockPos();
		activeAccessory = hit.toPlacedAccessory(accessoryId, heldItem.copyWithCount(1));
		waitingForUseRelease = true;
		lastMouseX = minecraft.mouseHandler.xpos();
		return true;
	}

	public static boolean handleScroll(double scrollDelta) {
		if (activeAccessory == null || scrollDelta == 0.0) {
			return false;
		}

		float scale = Mth.clamp(activeAccessory.scale() + (float) Math.signum(scrollDelta) * SCALE_STEP, MIN_SCALE, MAX_SCALE);
		activeAccessory = activeAccessory.withRotationAndScale(activeAccessory.rotation(), scale);
		return true;
	}

	public static boolean handleEscape() {
		if (activeAccessory == null) {
			return false;
		}

		cancelPlacement();
		return true;
	}

	public static boolean isPlacing() {
		return activeAccessory != null;
	}

	public static AccessoryBlockEntity.PlacedAccessory getPreview(BlockPos blockPos) {
		if (activeAccessory == null || !blockPos.equals(activeBlockPos)) {
			return null;
		}

		return activeAccessory;
	}

	private static void cancelPlacement() {
		activeBlockPos = null;
		activeAccessory = null;
	}
}
