package com.dooji.kurasu.client;

import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.client.render.LockerRaycast;
import com.dooji.kurasu.client.render.ObjModels;
import com.dooji.kurasu.network.DrawBlackboardPixelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class ChalkDrawingClient {
	private static BlockPos lastBlockPos;
	private static int lastPixelX = -1;
	private static int lastPixelY = -1;

	private ChalkDrawingClient() {
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
			resetStroke();
			return;
		}

		if (KurasuItems.getChalkColor(minecraft.player.getMainHandItem()) == null) {
			resetStroke();
			return;
		}

		if (!minecraft.options.keyUse.isDown()) {
			resetStroke();
			return;
		}

		drawFromLook(minecraft);
	}

	public static boolean handleUse() {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
			return false;
		}

		if (KurasuItems.getChalkColor(minecraft.player.getMainHandItem()) == null) {
			return false;
		}

		return drawFromLook(minecraft);
	}

	private static boolean drawFromLook(Minecraft minecraft) {
		LockerRaycast.Hit hit = LockerRaycast.raycastPlayerView(minecraft.player, 1.0f, LockerRaycast.AccessoryHitMode.NONE);

		if (hit == null || hit.isAccessoryHit() || !"board_surface".equals(hit.partName())) {
			lastBlockPos = null;
			lastPixelX = -1;
			lastPixelY = -1;
			return false;
		}

		int[] pixel = ObjModels.blackboardPixel(hit.localPosition());
		if (pixel == null) {
			lastBlockPos = null;
			lastPixelX = -1;
			lastPixelY = -1;
			return false;
		}

		int pixelX = pixel[0];
		int pixelY = pixel[1];

		if (pixelX == lastPixelX && pixelY == lastPixelY && hit.blockPos().equals(lastBlockPos)) {
			return true;
		}

		if (hit.blockPos().equals(lastBlockPos) && lastPixelX >= 0 && lastPixelY >= 0) {
			sendLine(hit.blockPos(), lastPixelX, lastPixelY, pixelX, pixelY);
		} else {
			ClientPlayNetworking.send(new DrawBlackboardPixelPayload(hit.blockPos(), pixelX, pixelY));
		}

		lastBlockPos = hit.blockPos();
		lastPixelX = pixelX;
		lastPixelY = pixelY;
		return true;
	}

	private static void sendLine(BlockPos blockPos, int startX, int startY, int endX, int endY) {
		int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));

		if (steps <= 0) {
			ClientPlayNetworking.send(new DrawBlackboardPixelPayload(blockPos, endX, endY));
			return;
		}

		for (int step = 1; step <= steps; step++) {
			int x = Math.round(Mth.lerp((float) step / steps, startX, endX));
			int y = Math.round(Mth.lerp((float) step / steps, startY, endY));
			ClientPlayNetworking.send(new DrawBlackboardPixelPayload(blockPos, x, y));
		}
	}

	private static void resetStroke() {
		lastBlockPos = null;
		lastPixelX = -1;
		lastPixelY = -1;
	}
}
