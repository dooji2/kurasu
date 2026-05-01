package com.dooji.kurasu.item;

import java.util.Arrays;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class DrawData {
	public static final int DEFAULT_SIZE = 64;
	private static final DrawData EMPTY = new DrawData(DEFAULT_SIZE, DEFAULT_SIZE, new int[DEFAULT_SIZE * DEFAULT_SIZE]);

	private final int width;
	private final int height;
	private final int[] pixels;

	public DrawData(int width, int height, int[] pixels) {
		this.width = width > 0 ? width : DEFAULT_SIZE;
		this.height = height > 0 ? height : DEFAULT_SIZE;
		this.pixels = Arrays.copyOf(pixels, this.width * this.height);
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}

	public int[] pixels() {
		return this.pixels.clone();
	}

	public boolean isBlank() {
		return !this.hasPixels();
	}

	public boolean hasPixels() {
		for (int pixel : this.pixels) {
			if (pixel != 0) {
				return true;
			}
		}

		return false;
	}

	public boolean samePixels(DrawData other) {
		return other != null
			&& this.width == other.width
			&& this.height == other.height
			&& Arrays.equals(this.pixels, other.pixels);
	}

	public DrawData normalized(int width, int height) {
		int normalizedWidth = width > 0 ? width : DEFAULT_SIZE;
		int normalizedHeight = height > 0 ? height : DEFAULT_SIZE;

		if (this.width == normalizedWidth && this.height == normalizedHeight) {
			return this;
		}

		int[] normalizedPixels = new int[normalizedWidth * normalizedHeight];
		int copyWidth = Math.min(this.width, normalizedWidth);
		int copyHeight = Math.min(this.height, normalizedHeight);

		for (int y = 0; y < copyHeight; y++) {
			System.arraycopy(this.pixels, y * this.width, normalizedPixels, y * normalizedWidth, copyWidth);
		}

		return new DrawData(normalizedWidth, normalizedHeight, normalizedPixels);
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("width", this.width);
		tag.putInt("height", this.height);
		tag.putIntArray("pixels", this.pixels);
		return tag;
	}

	public void write(ValueOutput output) {
		output.putInt("width", this.width);
		output.putInt("height", this.height);
		output.putIntArray("pixels", this.pixels);
	}

	public static DrawData fromTag(CompoundTag tag) {
		int width = tag.getInt("width").orElseThrow();
		int height = tag.getInt("height").orElseThrow();
		return new DrawData(width, height, tag.getIntArray("pixels").orElseThrow());
	}

	public static DrawData read(ValueInput input) {
		int width = input.getInt("width").orElseThrow();
		int height = input.getInt("height").orElseThrow();
		return new DrawData(width, height, input.getIntArray("pixels").orElseThrow());
	}

	public static DrawData fromStack(ItemStack stack) {
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag drawTag = customData.copyTag().getCompoundOrEmpty("draw");
		return drawTag.isEmpty() ? EMPTY : fromTag(drawTag);
	}

	public static DrawData empty() {
		return EMPTY;
	}

	public static void set(ItemStack stack, DrawData data) {
		if (fromStack(stack).samePixels(data)) {
			return;
		}

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			if (data.isBlank()) {
				tag.remove("draw");
				return;
			}

			tag.put("draw", data.toTag());
		});
	}
}
