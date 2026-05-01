package com.dooji.kurasu.item;

import net.minecraft.world.item.Item;

public class ChalkItem extends Item {
	private final int color;

	public ChalkItem(Properties properties, int color) {
		super(properties);
		this.color = color;
	}

	public int color() {
		return this.color;
	}
}
