package com.dooji.kurasu;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kurasu implements ModInitializer {
	public static final String MOD_ID = "kurasu";
	public static final String STICKY_NOTE_ACCESSORY_ID = MOD_ID + ":sticky_note";
	public static final String BOOK_1_ACCESSORY_ID = MOD_ID + ":book_1";
	public static final String BLACKBOARD_DRAW_ID = MOD_ID + ":blackboard_draw";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		KurasuBlocks.init();
		KurasuItems.init();
		KurasuBlockEntityTypes.init();
		KurasuCreativeTabs.init();
		KurasuNetworking.init();
	}
}
