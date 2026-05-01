package com.dooji.kurasu.client;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.client.render.LockerBlockEntityRenderer;
import com.dooji.kurasu.client.render.ObjModels;
import com.dooji.kurasu.client.render.ObjSpecialModelRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class KurasuClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ObjSpecialModelRenderer.init();
		ObjModels.init();
		AccessoryPlacementClient.init();
		ClientTickEvents.END_CLIENT_TICK.register(ChalkDrawingClient::tick);
		BlockEntityRenderers.register(KurasuBlockEntityTypes.LOCKER, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.SAFE, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.BLACKBOARD, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.CHAIR, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
		BlockEntityRenderers.register(KurasuBlockEntityTypes.DESK, context -> new LockerBlockEntityRenderer<>(blockEntity -> ObjModels.getSurfaceMesh(blockEntity.getBlockState())));
	}
}
