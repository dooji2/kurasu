package com.dooji.kurasu;

import com.dooji.kurasu.item.ChalkItem;
import com.dooji.kurasu.item.KeyItem;
import java.util.ArrayList;
import java.util.List;
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
	private static final Identifier ERASER_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "eraser");
	private static final ChalkVariantDefinition[] CHALK_VARIANT_DEFINITIONS = {
		new ChalkVariantDefinition("chalk", 0xFFF0F0F0),
		new ChalkVariantDefinition("chalk_orange", 0xFFEB8844),
		new ChalkVariantDefinition("chalk_magenta", 0xFFC354CD),
		new ChalkVariantDefinition("chalk_light_blue", 0xFF6689D3),
		new ChalkVariantDefinition("chalk_yellow", 0xFFDECF2A),
		new ChalkVariantDefinition("chalk_lime", 0xFF41CD34),
		new ChalkVariantDefinition("chalk_pink", 0xFFD88198),
		new ChalkVariantDefinition("chalk_gray", 0xFF434343),
		new ChalkVariantDefinition("chalk_light_gray", 0xFFABABAB),
		new ChalkVariantDefinition("chalk_cyan", 0xFF287697),
		new ChalkVariantDefinition("chalk_purple", 0xFF7B2FBE),
		new ChalkVariantDefinition("chalk_blue", 0xFF253192),
		new ChalkVariantDefinition("chalk_brown", 0xFF51301A),
		new ChalkVariantDefinition("chalk_green", 0xFF3B511A),
		new ChalkVariantDefinition("chalk_red", 0xFFB3312C),
		new ChalkVariantDefinition("chalk_black", 0xFF1E1B1B),
		new ChalkVariantDefinition("chalk_soft_yellow", 0xFFF8D96B),
		new ChalkVariantDefinition("chalk_amber", 0xFFFFBF00),
		new ChalkVariantDefinition("chalk_peach", 0xFFFFB347),
		new ChalkVariantDefinition("chalk_salmon", 0xFFFF7A7A),
		new ChalkVariantDefinition("chalk_blush", 0xFFFF99C8),
		new ChalkVariantDefinition("chalk_mint", 0xFFC6F08C),
		new ChalkVariantDefinition("chalk_light_green", 0xFF7EE787),
		new ChalkVariantDefinition("chalk_aqua", 0xFF7FE7E7),
		new ChalkVariantDefinition("chalk_sky", 0xFF89CFF0),
		new ChalkVariantDefinition("chalk_periwinkle", 0xFF7AA2FF),
		new ChalkVariantDefinition("chalk_lavender", 0xFFC099FF),
		new ChalkVariantDefinition("chalk_tan", 0xFFA97142)
	};
	private static final ResourceKey<Item> STICKY_NOTE_ITEM_KEY = ResourceKey.create(Registries.ITEM, STICKY_NOTE_ID);
	private static final ResourceKey<Item> BOOK_1_ITEM_KEY = ResourceKey.create(Registries.ITEM, BOOK_1_ID);
	private static final ResourceKey<Item> KEY_ITEM_KEY = ResourceKey.create(Registries.ITEM, KEY_ID);
	private static final ResourceKey<Item> LOCKPICK_ITEM_KEY = ResourceKey.create(Registries.ITEM, LOCKPICK_ID);
	private static final ResourceKey<Item> ERASER_ITEM_KEY = ResourceKey.create(Registries.ITEM, ERASER_ID);

	public static final Item STICKY_NOTE = registerItem(STICKY_NOTE_ID, new Item(new Item.Properties().setId(STICKY_NOTE_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item BOOK_1 = registerItem(BOOK_1_ID, new WritableBookItem(new Item.Properties().setId(BOOK_1_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1).component(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY)));
	public static final Item KEY = registerItem(KEY_ID, new KeyItem(new Item.Properties().setId(KEY_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1)));
	public static final Item LOCKPICK = registerItem(LOCKPICK_ID, new Item(new Item.Properties().setId(LOCKPICK_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1).durability(24)));
	private static final List<Item> CHALK_VARIANTS = registerChalkVariants();
	public static final Item CHALK = CHALK_VARIANTS.getFirst();
	public static final Item ERASER = registerItem(ERASER_ID, new ChalkItem(new Item.Properties().setId(ERASER_ITEM_KEY).useItemDescriptionPrefix().stacksTo(1), 0));

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

	public static List<Item> getChalkVariants() {
		return CHALK_VARIANTS;
	}

	private static <T extends Item> T registerItem(Identifier id, T item) {
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static List<Item> registerChalkVariants() {
		List<Item> variants = new ArrayList<>();

		for (ChalkVariantDefinition variant : CHALK_VARIANT_DEFINITIONS) {
			Identifier id = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, variant.path());
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
			variants.add(registerItem(id, new ChalkItem(new Item.Properties().setId(itemKey).useItemDescriptionPrefix().stacksTo(1), variant.color())));
		}

		return List.copyOf(variants);
	}

	private record ChalkVariantDefinition(String path, int color) {
	}
}
