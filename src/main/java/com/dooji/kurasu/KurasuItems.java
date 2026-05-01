package com.dooji.kurasu;

import com.dooji.kurasu.item.ChalkItem;
import com.dooji.kurasu.item.KeyItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.component.WritableBookContent;

public class KurasuItems {
	private static final Identifier STICKY_NOTE_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "sticky_note");
	private static final Identifier BOOK_1_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "book_1");
	private static final Identifier KEY_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "key");
	private static final Identifier LOCKPICK_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "lockpick");
	private static final Identifier CHALK_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "chalk");
	private static final ResourceKey<Item> STICKY_NOTE_ITEM_KEY = ResourceKey.create(Registries.ITEM, STICKY_NOTE_ID);
	private static final ResourceKey<Item> BOOK_1_ITEM_KEY = ResourceKey.create(Registries.ITEM, BOOK_1_ID);
	private static final ResourceKey<Item> KEY_ITEM_KEY = ResourceKey.create(Registries.ITEM, KEY_ID);
	private static final ResourceKey<Item> LOCKPICK_ITEM_KEY = ResourceKey.create(Registries.ITEM, LOCKPICK_ID);
	private static final ResourceKey<Item> CHALK_ITEM_KEY = ResourceKey.create(Registries.ITEM, CHALK_ID);

	public static final Item STICKY_NOTE = registerItem(STICKY_NOTE_ID, new Item(new Item.Properties().setId(STICKY_NOTE_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item BOOK_1 = registerItem(BOOK_1_ID, new WritableBookItem(new Item.Properties().setId(BOOK_1_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1).component(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY)));
	public static final Item KEY = registerItem(KEY_ID, new KeyItem(new Item.Properties().setId(KEY_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1)));
	public static final Item LOCKPICK = registerItem(LOCKPICK_ID, new Item(new Item.Properties().setId(LOCKPICK_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1).durability(24)));
	public static final Item CHALK = registerItem(CHALK_ID, new ChalkItem(new Item.Properties().setId(CHALK_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1), 0xFFFFFFFF));

	public static void init() {
	}

	public static String getAccessoryId(ItemStack itemStack) {
		if (itemStack.getItem() == STICKY_NOTE) {
			return Kurasu.STICKY_NOTE_ACCESSORY_ID;
		}

		if (itemStack.getItem() == BOOK_1) {
			return Kurasu.BOOK_1_ACCESSORY_ID;
		}

		return null;
	}

	public static Integer getChalkColor(ItemStack itemStack) {
		if (itemStack.getItem() instanceof ChalkItem chalkItem) {
			return chalkItem.color();
		}

		return null;
	}

	private static <T extends Item> T registerItem(Identifier id, T item) {
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}
}
