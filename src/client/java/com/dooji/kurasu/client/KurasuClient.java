package com.dooji.kurasu.client;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.client.render.LockerBlockEntityRenderer;
import com.dooji.kurasu.client.render.ObjModels;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class KurasuClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ObjModels.init();
		AccessoryPlacementClient.init();
		BlockEntityRenderers.register(KurasuBlockEntityTypes.LOCKER, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.SAFE, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.BLACKBOARD, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.CHAIR, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.DESK, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
	}
}
