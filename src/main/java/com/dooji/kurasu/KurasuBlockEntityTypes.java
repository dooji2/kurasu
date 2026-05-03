package com.dooji.kurasu;

import com.dooji.kurasu.block.entity.AccessoryBlockEntity;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.block.entity.SafeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class KurasuBlockEntityTypes {
	public static final BlockEntityType<LockerBlockEntity> LOCKER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "locker"),
		FabricBlockEntityTypeBuilder.create(LockerBlockEntity::new, KurasuBlocks.LOCKER, KurasuBlocks.LOCKER_1).build()
	);
	public static final BlockEntityType<SafeBlockEntity> SAFE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "safe"),
		FabricBlockEntityTypeBuilder.create(SafeBlockEntity::new, KurasuBlocks.SAFE).build()
	);
	public static final BlockEntityType<BlackboardBlockEntity> BLACKBOARD = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "blackboard"),
		FabricBlockEntityTypeBuilder.create(BlackboardBlockEntity::new, KurasuBlocks.BLACKBOARD, KurasuBlocks.BLACKBOARD_1).build()
	);
	public static final BlockEntityType<AccessoryBlockEntity> CHAIR = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "chair"),
		FabricBlockEntityTypeBuilder.create(KurasuBlockEntityTypes::createChair, KurasuBlocks.CHAIR, KurasuBlocks.CHAIR_1).build()
	);
	public static final BlockEntityType<AccessoryBlockEntity> DESK = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "desk"),
		FabricBlockEntityTypeBuilder.create(KurasuBlockEntityTypes::createDesk, KurasuBlocks.DESK, KurasuBlocks.DESK_1).build()
	);

	public static void init() {
	}

	private static AccessoryBlockEntity createChair(BlockPos pos, BlockState state) {
		return new AccessoryBlockEntity(CHAIR, pos, state);
	}

	private static AccessoryBlockEntity createDesk(BlockPos pos, BlockState state) {
		return new AccessoryBlockEntity(DESK, pos, state);
	}
}
