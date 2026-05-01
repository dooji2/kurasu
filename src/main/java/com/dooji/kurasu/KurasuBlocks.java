package com.dooji.kurasu;

import com.dooji.kurasu.block.BlackboardBlock;
import com.dooji.kurasu.block.ChairBlock;
import com.dooji.kurasu.block.DeskBlock;
import com.dooji.kurasu.block.LockerBlock;
import com.dooji.kurasu.block.SafeBlock;
import com.dooji.kurasu.block.SimpleHorizontalBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class KurasuBlocks {
	private static final Identifier LOCKER_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "locker");
	private static final Identifier SAFE_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "safe");
	private static final Identifier BLACKBOARD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "blackboard");
	private static final Identifier CONCRETE_WALL_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "concrete_wall");
	private static final Identifier CONCRETE_WALL_BOTTOM_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "concrete_wall_bottom");
	private static final Identifier CHAIR_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "chair");
	private static final Identifier DESK_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "desk");
	private static final ResourceKey<Block> LOCKER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, LOCKER_ID);
	private static final ResourceKey<Block> SAFE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, SAFE_ID);
	private static final ResourceKey<Block> BLACKBOARD_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, BLACKBOARD_ID);
	private static final ResourceKey<Block> CONCRETE_WALL_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, CONCRETE_WALL_ID);
	private static final ResourceKey<Block> CONCRETE_WALL_BOTTOM_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, CONCRETE_WALL_BOTTOM_ID);
	private static final ResourceKey<Block> CHAIR_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, CHAIR_ID);
	private static final ResourceKey<Block> DESK_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, DESK_ID);
	private static final ResourceKey<Item> LOCKER_ITEM_KEY = ResourceKey.create(Registries.ITEM, LOCKER_ID);
	private static final ResourceKey<Item> SAFE_ITEM_KEY = ResourceKey.create(Registries.ITEM, SAFE_ID);
	private static final ResourceKey<Item> BLACKBOARD_ITEM_KEY = ResourceKey.create(Registries.ITEM, BLACKBOARD_ID);
	private static final ResourceKey<Item> CONCRETE_WALL_ITEM_KEY = ResourceKey.create(Registries.ITEM, CONCRETE_WALL_ID);
	private static final ResourceKey<Item> CONCRETE_WALL_BOTTOM_ITEM_KEY = ResourceKey.create(Registries.ITEM, CONCRETE_WALL_BOTTOM_ID);
	private static final ResourceKey<Item> CHAIR_ITEM_KEY = ResourceKey.create(Registries.ITEM, CHAIR_ID);
	private static final ResourceKey<Item> DESK_ITEM_KEY = ResourceKey.create(Registries.ITEM, DESK_ID);

	public static final LockerBlock LOCKER = registerBlock(LOCKER_ID, new LockerBlock(BlockBehaviour.Properties.of().setId(LOCKER_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final SafeBlock SAFE = registerBlock(SAFE_ID, new SafeBlock(BlockBehaviour.Properties.of().setId(SAFE_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final BlackboardBlock BLACKBOARD = registerBlock(BLACKBOARD_ID, new BlackboardBlock(BlockBehaviour.Properties.of().setId(BLACKBOARD_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final Block CONCRETE_WALL = registerBlock(CONCRETE_WALL_ID, new SimpleHorizontalBlock(BlockBehaviour.Properties.of().setId(CONCRETE_WALL_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final Block CONCRETE_WALL_BOTTOM = registerBlock(CONCRETE_WALL_BOTTOM_ID, new SimpleHorizontalBlock(BlockBehaviour.Properties.of().setId(CONCRETE_WALL_BOTTOM_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final ChairBlock CHAIR = registerBlock(CHAIR_ID, new ChairBlock(BlockBehaviour.Properties.of().setId(CHAIR_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final DeskBlock DESK = registerBlock(DESK_ID, new DeskBlock(BlockBehaviour.Properties.of().setId(DESK_BLOCK_KEY).strength(2.0f).noOcclusion()));
	public static final Item LOCKER_ITEM = registerItem(LOCKER_ID, new BlockItem(LOCKER, new Item.Properties().setId(LOCKER_ITEM_KEY)));
	public static final Item SAFE_ITEM = registerItem(SAFE_ID, new BlockItem(SAFE, new Item.Properties().setId(SAFE_ITEM_KEY)));
	public static final Item BLACKBOARD_ITEM = registerItem(BLACKBOARD_ID, new BlockItem(BLACKBOARD, new Item.Properties().setId(BLACKBOARD_ITEM_KEY)));
	public static final Item CONCRETE_WALL_ITEM = registerItem(CONCRETE_WALL_ID, new BlockItem(CONCRETE_WALL, new Item.Properties().setId(CONCRETE_WALL_ITEM_KEY)));
	public static final Item CONCRETE_WALL_BOTTOM_ITEM = registerItem(CONCRETE_WALL_BOTTOM_ID, new BlockItem(CONCRETE_WALL_BOTTOM, new Item.Properties().setId(CONCRETE_WALL_BOTTOM_ITEM_KEY)));
	public static final Item CHAIR_ITEM = registerItem(CHAIR_ID, new BlockItem(CHAIR, new Item.Properties().setId(CHAIR_ITEM_KEY)));
	public static final Item DESK_ITEM = registerItem(DESK_ID, new BlockItem(DESK, new Item.Properties().setId(DESK_ITEM_KEY)));

	public static void init() {
	}

	private static <T extends Block> T registerBlock(Identifier id, T block) {
		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	private static <T extends Item> T registerItem(Identifier id, T item) {
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}
}
