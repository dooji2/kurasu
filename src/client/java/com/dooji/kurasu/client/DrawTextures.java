package com.dooji.kurasu.client;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.item.DrawData;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class DrawTextures {
	private static final Map<Integer, Identifier> BLACKBOARD_TEXTURES = new HashMap<>();
	private static final Map<Integer, Identifier> STICKY_NOTE_TEXTURES = new HashMap<>();
	private static NativeImage blackboardBaseImage;
	private static NativeImage stickyNoteBaseImage;

	public static Identifier getBlackboardTexture(DrawData data) {
		int key = getTextureKey(data);
		Identifier existing = BLACKBOARD_TEXTURES.get(key);

		if (existing != null) {
			return existing;
		}

		Identifier id = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "draw/blackboard/" + Integer.toUnsignedString(key, 16));
		DynamicTexture texture = new DynamicTexture(id::toString, buildImage(blackboardBaseImage, data));
		Minecraft.getInstance().getTextureManager().register(id, texture);
		BLACKBOARD_TEXTURES.put(key, id);
		return id;
	}

	public static Identifier getStickyNoteTexture(DrawData data) {
		int key = getTextureKey(data);
		Identifier existing = STICKY_NOTE_TEXTURES.get(key);

		if (existing != null) {
			return existing;
		}

		Identifier id = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "draw/sticky_note/" + Integer.toUnsignedString(key, 16));
		DynamicTexture texture = new DynamicTexture(id::toString, buildImage(stickyNoteBaseImage, data));
		Minecraft.getInstance().getTextureManager().register(id, texture);
		STICKY_NOTE_TEXTURES.put(key, id);
		return id;
	}

	private static int getTextureKey(DrawData data) {
		return Objects.hash(data.width(), data.height(), Arrays.hashCode(data.pixels()));
	}

	private static NativeImage buildImage(NativeImage baseImage, DrawData data) {
		int width = data.width();
		int height = data.height();
		NativeImage image = new NativeImage(width, height, true);

		if (baseImage != null && baseImage.getWidth() == width && baseImage.getHeight() == height) {
			image.copyFrom(baseImage);
		}

		int[] pixels = data.pixels();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = pixels[x + y * width];

				if (ARGB.alpha(pixel) != 0) {
					image.setPixel(x, y, pixel);
				}
			}
		}

		return image;
	}

	public static void setBlackboardBase(NativeImage image) {
		if (blackboardBaseImage != null) {
			blackboardBaseImage.close();
		}

		blackboardBaseImage = image;
	}

	public static void setStickyNoteBase(NativeImage image) {
		if (stickyNoteBaseImage != null) {
			stickyNoteBaseImage.close();
		}

		stickyNoteBaseImage = image;
	}

	public static void clear() {
		Minecraft minecraft = Minecraft.getInstance();

		for (Identifier id : BLACKBOARD_TEXTURES.values()) {
			minecraft.getTextureManager().release(id);
		}

		for (Identifier id : STICKY_NOTE_TEXTURES.values()) {
			minecraft.getTextureManager().release(id);
		}

		BLACKBOARD_TEXTURES.clear();
		STICKY_NOTE_TEXTURES.clear();

		if (blackboardBaseImage != null) {
			blackboardBaseImage.close();
			blackboardBaseImage = null;
		}

		if (stickyNoteBaseImage != null) {
			stickyNoteBaseImage.close();
			stickyNoteBaseImage = null;
		}
	}
}
