package com.dooji.kurasu.client;

import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.block.BlackboardBlock;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import com.dooji.kurasu.item.DrawData;
import com.dooji.kurasu.network.SaveBlackboardPayload;
import com.dooji.kurasu.network.ToggleOperatorLockPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlackboardScreen extends DrawScreen {
	private final BlockPos blockPos;
	private Button lockButton;

	private BlackboardScreen(BlockPos blockPos, DrawData data) {
		super(Mode.BLACKBOARD, BlackboardBlockEntity.DRAW_WIDTH, BlackboardBlockEntity.DRAW_HEIGHT, data);
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
		ClientPlayNetworking.send(new SaveBlackboardPayload(this.blockPos, width, height, pixels));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && this.isOperatorLocked() && !this.isOperator()) {
			int guiX = Math.max(12, (this.width - 250) / 2);
			int guiY = Math.max(12, (this.height - 196) / 2);

			if (
				(event.x() >= guiX + 8 && event.x() < guiX + 18 && event.y() >= guiY + 8 && event.y() < guiY + 172)
				|| (event.x() >= guiX + 24 && event.x() < guiX + 242 && event.y() >= guiY + 8 && event.y() < guiY + 172)
			) {
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void init() {
		super.init();
		int x = (this.width - 70) / 2;
		int y = Math.max(0, Math.max(12, (this.height - 196) / 2) - 10);
		this.lockButton = this.addRenderableWidget(
			Button.builder(Component.translatable(this.isOperatorLocked() ? "gui.kurasu.op_unlock" : "gui.kurasu.op_lock"), button -> ClientPlayNetworking.send(new ToggleOperatorLockPayload(this.blockPos)))
				.bounds(x, y, 70, 10)
				.build()
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		if (this.lockButton != null) {
			this.lockButton.setMessage(Component.translatable(this.isOperatorLocked() ? "gui.kurasu.op_unlock" : "gui.kurasu.op_lock"));
			this.lockButton.active = this.isOperator();
			this.lockButton.setX((this.width - 70) / 2);
			this.lockButton.setY(Math.max(0, Math.max(12, (this.height - 196) / 2) - 10));
		}

		super.extractRenderState(gfx, mouseX, mouseY, partialTick);
	}

	private boolean isOperator() {
		return this.minecraft != null
			&& this.minecraft.player != null
			&& this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private boolean isOperatorLocked() {
		return this.minecraft != null
			&& this.minecraft.level != null
			&& this.minecraft.level.getBlockEntity(this.blockPos) instanceof BlackboardBlockEntity blockEntity
			&& blockEntity.isOperatorLocked();
	}
}
