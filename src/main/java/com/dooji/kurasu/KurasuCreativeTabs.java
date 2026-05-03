package com.dooji.kurasu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class KurasuCreativeTabs {
	public static final ResourceKey<CreativeModeTab> KURASU = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "kurasu"));
	private static final Item[] TAB_ICONS = {
		KurasuBlocks.LOCKER_ITEM,
		KurasuBlocks.BLACKBOARD_ITEM,
		KurasuBlocks.CHAIR_ITEM,
		KurasuBlocks.DESK_ITEM,
		KurasuItems.CHALK,
		KurasuItems.STICKY_NOTE
	};

	public static void init() {
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			KURASU,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.kurasu.kurasu"))
				.icon(() -> new ItemStack(KurasuBlocks.LOCKER_ITEM))
				.displayItems((parameters, output) -> {
					output.accept(KurasuBlocks.LOCKER_ITEM);
					output.accept(KurasuBlocks.LOCKER_1_ITEM);
					output.accept(KurasuBlocks.SAFE_ITEM);
					output.accept(KurasuBlocks.BLACKBOARD_ITEM);
					output.accept(KurasuBlocks.BLACKBOARD_1_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_BOTTOM_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_BOTTOM_LEFT_ITEM);
					output.accept(KurasuBlocks.CONCRETE_WALL_BOTTOM_RIGHT_ITEM);
					output.accept(KurasuBlocks.CHAIR_ITEM);
					output.accept(KurasuBlocks.CHAIR_1_ITEM);
					output.accept(KurasuBlocks.DESK_ITEM);
					output.accept(KurasuBlocks.DESK_1_ITEM);
					output.accept(KurasuItems.KEY);
					output.accept(KurasuItems.LOCKPICK);
					output.accept(KurasuItems.STICKY_NOTE);
					output.accept(KurasuItems.BOOK_1);
					for (Item item : KurasuItems.getChalkVariants()) {
						output.accept(item);
					}
					output.accept(KurasuItems.ERASER);
				})
				.build()
		);
	}

	public static ItemStack createTabIcon() {
		long index = (System.currentTimeMillis() / 1200L) % TAB_ICONS.length;
		return new ItemStack(TAB_ICONS[(int) index]);
	}

	public static boolean isKurasuTab(CreativeModeTab tab) {
		return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).filter(KURASU::equals).isPresent();
	}
}
