package com.dooji.kurasu.client;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.block.SafeBlock;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.network.FinishLockpickPayload;
import java.util.Random;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;

public class LockpickScreen extends Screen {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/lockpick_safe_ui.png");

	private final BlockPos blockPos;
	private final float driftStrength;
	private final float controlStrength;
	private final float progressGain;
	private final float progressLoss;
	private final float sweetSpot;
	private final Random random = new Random();
	private float lockOffsetX;
	private float lockOffsetY;
	private float lockVelocityX;
	private float lockVelocityY;
	private float biasX;
	private float biasY;
	private float progress;
	private int biasTicks;
	private int scale;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int circleX;
	private int circleY;
	private int circleSize;
	private int circleCenterX;
	private int circleCenterY;
	private int markerSize;
	private int markerTravel;
	private double cursorX;
	private double cursorY;
	private boolean holding;
	private boolean finished;

	private LockpickScreen(BlockPos blockPos, boolean hard) {
		super(Component.empty());
		this.blockPos = blockPos;
		this.driftStrength = hard ? 0.013f : 0.010f;
		this.controlStrength = hard ? 0.019f : 0.024f;
		this.progressGain = hard ? 0.016f : 0.022f;
		this.progressLoss = hard ? 0.026f : 0.020f;
		this.sweetSpot = hard ? 0.48f : 0.58f;
	}

	public static boolean tryOpenFromLook() {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null
			|| minecraft.level == null
			|| minecraft.screen != null
			|| minecraft.player.getMainHandItem().getItem() != KurasuItems.LOCKPICK
			|| !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
			return false;
		}

		BlockPos blockPos = hitResult.getBlockPos();
		if (!(minecraft.level.getBlockEntity(blockPos) instanceof LockerBlockEntity blockEntity)) {
			return false;
		}

		if (!blockEntity.isStructureLocked() || blockEntity.isStructureOpen()) {
			return false;
		}

		minecraft.setScreen(new LockpickScreen(blockPos, minecraft.level.getBlockState(blockPos).getBlock() instanceof SafeBlock));
		return true;
	}

	@Override
	protected void init() {
		this.scale = 3;
		while (this.scale > 1 && (67 * this.scale > this.width - 24 || 55 * this.scale > this.height - 24)) {
			this.scale--;
		}

		this.panelWidth = 67 * this.scale;
		this.panelHeight = 55 * this.scale;
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;
		this.circleSize = 30 * this.scale;
		this.circleX = this.panelX + 18 * this.scale;
		this.circleY = this.panelY + 7 * this.scale;
		this.circleCenterX = this.circleX + this.circleSize / 2;
		this.circleCenterY = this.circleY + this.circleSize / 2;
		this.markerSize = 24 * this.scale;
		this.markerTravel = (this.circleSize - this.markerSize) / 2;
		this.pickNewBias();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.finished) {
			return;
		}

		float inputX = 0.0f;
		float inputY = 0.0f;

		if (this.holding) {
			double dx = this.cursorX - this.circleCenterX;
			double dy = this.cursorY - this.circleCenterY;
			double distance = Math.sqrt(dx * dx + dy * dy);
			double radius = this.circleSize * 0.5;

			if (distance > radius) {
				dx *= radius / distance;
				dy *= radius / distance;
			}

			inputX = (float) (dx / radius);
			inputY = (float) (dy / radius);
		}

		if (--this.biasTicks <= 0) {
			this.pickNewBias();
		}

		this.lockVelocityX += this.biasX * this.driftStrength;
		this.lockVelocityY += this.biasY * this.driftStrength;
		this.lockVelocityX += inputX * this.controlStrength;
		this.lockVelocityY += inputY * this.controlStrength;
		this.lockVelocityX *= 0.90f;
		this.lockVelocityY *= 0.90f;
		this.lockOffsetX += this.lockVelocityX;
		this.lockOffsetY += this.lockVelocityY;

		float distance = Mth.sqrt(this.lockOffsetX * this.lockOffsetX + this.lockOffsetY * this.lockOffsetY);
		if (distance > 1.12f) {
			float clamp = 1.12f / distance;
			this.lockOffsetX *= clamp;
			this.lockOffsetY *= clamp;
			this.lockVelocityX *= 0.5f;
			this.lockVelocityY *= 0.5f;
			distance = 1.12f;
		}

		if (distance <= this.sweetSpot) {
			this.progress = Mth.clamp(this.progress + this.progressGain, 0.0f, 1.0f);
		} else {
			this.progress = Mth.clamp(this.progress - this.progressLoss, 0.0f, 1.0f);
		}

		if (distance >= 1.0f) {
			this.finish(false);
		} else if (this.progress >= 1.0f) {
			this.finish(true);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gfx, mouseX, mouseY, partialTick);
		this.cursorX = mouseX;
		this.cursorY = mouseY;
		this.extractTransparentBackground(gfx);
		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.panelX, this.panelY, 0, 0, this.panelWidth, this.panelHeight, 67, 55, 142, 70);
		this.drawHint(gfx);
		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.circleX, this.circleY, 18, 7, this.circleSize, this.circleSize, 30, 30, 142, 70);
		this.drawProgressRing(gfx);
		this.drawMarker(gfx);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && this.isInsideCircle(event.x(), event.y())) {
			this.holding = true;
			this.cursorX = event.x();
			this.cursorY = event.y();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.holding) {
			this.cursorX = event.x();
			this.cursorY = event.y();
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && this.holding) {
			this.holding = false;
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		if (!this.finished) {
			this.finish(false);
			return;
		}

		super.onClose();
	}

	private void drawHint(GuiGraphicsExtractor gfx) {
		int x = this.panelX + 7 * this.scale;
		int y = this.panelY + 41 * this.scale;
		int width = 53 * this.scale;
		int height = 8 * this.scale;

		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 7, 41, width, height, 53, 8, 142, 70);
		gfx.centeredText(this.font, Component.translatable("gui.kurasu.lockpick_hint").getString(), x + width / 2, y + (height - 8) / 2, 0xFFFFFFFF);
	}

	private void drawProgressRing(GuiGraphicsExtractor gfx) {
		if (this.progress <= 0.0f) {
			return;
		}

		int ringX = this.circleX + this.scale;
		int ringY = this.circleY + this.scale;
		double filledAngle = this.progress * Math.PI * 2.0;
		for (int localY = 0; localY < 28; localY++) {
			for (int localX = 0; localX < 28; localX++) {
				double dx = localX + 0.5 - 14.0;
				double dy = localY + 0.5 - 14.0;
				double angle = Math.atan2(dy, dx) + Math.PI * 0.5;
				if (angle < 0.0) {
					angle += Math.PI * 2.0;
				}

				if (angle > filledAngle) {
					continue;
				}

				gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, ringX + localX * this.scale, ringY + localY * this.scale, 67 + localX, 34 + localY, this.scale, this.scale, 1, 1, 142, 70);
			}
		}
	}

	private void drawMarker(GuiGraphicsExtractor gfx) {
		int x = this.circleX + (this.circleSize - this.markerSize) / 2 + Math.round(this.lockOffsetX * this.markerTravel);
		int y = this.circleY + (this.circleSize - this.markerSize) / 2 + Math.round(this.lockOffsetY * this.markerTravel);
		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 67, 0, this.markerSize, this.markerSize, 24, 24, 142, 70);
	}

	private boolean isInsideCircle(double mouseX, double mouseY) {
		double dx = mouseX - this.circleCenterX;
		double dy = mouseY - this.circleCenterY;
		double radius = this.circleSize * 0.5;
		return dx * dx + dy * dy <= radius * radius;
	}

	private void finish(boolean success) {
		if (this.finished) {
			return;
		}

		this.finished = true;
		ClientPlayNetworking.send(new FinishLockpickPayload(this.blockPos, success));

		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}

	private void pickNewBias() {
		float angle = this.random.nextFloat() * (float) (Math.PI * 2.0);
		float strength = 0.45f + this.random.nextFloat() * 0.45f;
		this.biasX = Mth.cos(angle) * strength;
		this.biasY = Mth.sin(angle) * strength;
		this.biasTicks = 14 + this.random.nextInt(18);
	}
}
