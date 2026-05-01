package com.dooji.kurasu.item;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public class KeyItem extends Item {
	private static final String LOCK_ID_TAG = "lock_id";

	public KeyItem(Properties properties) {
		super(properties);
	}

	public static String getLockId(ItemStack stack) {
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		return customData.copyTag().getStringOr(LOCK_ID_TAG, "");
	}

	public static void setLockId(ItemStack stack, String lockId) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			if (lockId.isBlank()) {
				tag.remove(LOCK_ID_TAG);
				return;
			}

			tag.putString(LOCK_ID_TAG, lockId);
		});
	}

	@Override
	public Component getName(ItemStack stack) {
		return getLockId(stack).isBlank() ? super.getName(stack) : Component.translatable("item.kurasu.bound_key");
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return !getLockId(stack).isBlank();
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
		String lockId = getLockId(stack);

		if (lockId.isBlank()) {
			tooltip.accept(Component.translatable("item.kurasu.key.blank"));
			return;
		}

		tooltip.accept(Component.translatable("item.kurasu.key.bound", lockId.length() <= 6 ? lockId : lockId.substring(lockId.length() - 6)));
	}
}
