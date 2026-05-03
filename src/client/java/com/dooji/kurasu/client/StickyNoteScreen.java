package com.dooji.kurasu.client;

import com.dooji.kurasu.item.DrawData;
import com.dooji.kurasu.network.SaveStickyNotePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class StickyNoteScreen extends DrawScreen {
	private static final int BASE_SIZE = DrawData.DEFAULT_SIZE;
	private final ItemStack stack;

	public StickyNoteScreen(ItemStack stack) {
		this(stack, DrawData.fromStack(stack));
	}

	private StickyNoteScreen(ItemStack stack, DrawData data) {
		super(Mode.STICKY_NOTE, BASE_SIZE, BASE_SIZE, data);
		this.stack = stack;
	}

	@Override
	protected void save(int width, int height, int[] pixels) {
		DrawData.set(this.stack, new DrawData(width, height, pixels));
		ClientPlayNetworking.send(new SaveStickyNotePayload(width, height, pixels));
	}
}
