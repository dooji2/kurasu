package com.dooji.kurasu.client;

import com.dooji.kurasu.item.DrawData;
import com.dooji.kurasu.network.SaveStickyNotePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class StickyNoteScreen extends DrawScreen {
	private final ItemStack stack;

	public StickyNoteScreen(ItemStack stack) {
		this(stack, DrawData.fromStack(stack));
	}

	private StickyNoteScreen(ItemStack stack, DrawData data) {
		super(Mode.STICKY_NOTE, data.width(), data.height(), data.pixels());
		this.stack = stack;
	}

	@Override
	protected void save(int width, int height, int[] pixels) {
		DrawData.set(this.stack, new DrawData(width, height, pixels));
		ClientPlayNetworking.send(new SaveStickyNotePayload(width, height, pixels));
	}
}
