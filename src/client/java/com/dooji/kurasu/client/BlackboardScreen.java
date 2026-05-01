package com.dooji.kurasu.client;

import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.block.BlackboardBlock;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import com.dooji.kurasu.item.DrawData;
import com.dooji.kurasu.network.SaveBlackboardPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlackboardScreen extends DrawScreen {
	private final BlockPos blockPos;

	private BlackboardScreen(BlockPos blockPos, DrawData data) {
		super(Mode.BLACKBOARD, BlackboardBlockEntity.DRAW_WIDTH, BlackboardBlockEntity.DRAW_HEIGHT, data.pixels());
		this.blockPos = blockPos;
	}

	public static boolean tryOpenFromLook() {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
			return false;
		}

		if (KurasuItems.getChalkColor(minecraft.player.getMainHandItem()) != null) {
			return false;
		}

		BlockState state = minecraft.level.getBlockState(hitResult.getBlockPos());

		if (!(state.getBlock() instanceof BlackboardBlock blackboard)) {
			return false;
		}

		BlockPos blockPos = blackboard.getAnchorPos(hitResult.getBlockPos(), state);
		DrawData data;

		if (minecraft.level.getBlockEntity(blockPos) instanceof BlackboardBlockEntity blockEntity) {
			data = blockEntity.getDrawData();
		} else {
			data = new DrawData(BlackboardBlockEntity.DRAW_WIDTH, BlackboardBlockEntity.DRAW_HEIGHT, new int[BlackboardBlockEntity.DRAW_WIDTH * BlackboardBlockEntity.DRAW_HEIGHT]);
		}

		minecraft.setScreen(new BlackboardScreen(blockPos, data));
		return true;
	}

	@Override
	protected void save(int width, int height, int[] pixels) {
		ClientPlayNetworking.send(new SaveBlackboardPayload(this.blockPos, pixels));
	}
}
