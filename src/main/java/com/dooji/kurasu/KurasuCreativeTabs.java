package com.dooji.kurasu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

class KurasuCreativeTabs {
	private static final ResourceKey<CreativeModeTab> KURASU = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "kurasu"));

	public static void init() {
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			KURASU,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.kurasu.kurasu"))
				.icon(() -> new ItemStack(KurasuBlocks.LOCKER_ITEM))
				.displayItems((parameters, output) -> {
					output.accept(KurasuBlocks.LOCKER_ITEM);
					output.accept(KurasuBlocks.SAFE_ITEM);
					output.accept(KurasuBlocks.BLACKBOARD_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_BOTTOM_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_BOTTOM_LEFT_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_BOTTOM_RIGHT_ITEM);
					output.accept(KurasuBlocks.CHAIR_ITEM);
					output.accept(KurasuBlocks.DESK_ITEM);
					output.accept(KurasuItems.KEY);
					output.accept(KurasuItems.LOCKPICK);
					output.accept(KurasuItems.CHALK);
					output.accept(KurasuItems.STICKY_NOTE);
					output.accept(KurasuItems.BOOK_1);
				})
				.build()
		);
	}
}
