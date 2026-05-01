package com.dooji.kurasu.client;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.block.SafeBlock;
import com.dooji.kurasu.block.entity.SafeBlockEntity;
import com.dooji.kurasu.network.SubmitSafeActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;

public class SafeCodeScreen extends Screen {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/lockpick_safe_ui.png");

	private final BlockPos blockPos;
	private final boolean open;
	private Mode mode;
	private String code = "";
	private int scale;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int buttonWidth;
	private int buttonHeight;
	private int wideButtonWidth;

	private SafeCodeScreen(BlockPos blockPos, Mode mode, boolean open) {
		super(Component.empty());
		this.blockPos = blockPos;
		this.mode = mode;
		this.open = open;
	}

	public static boolean tryOpenFromLook() {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null
			|| minecraft.level == null
			|| minecraft.screen != null
			|| !minecraft.player.getMainHandItem().isEmpty()
			|| !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
			return false;
		}

		BlockPos blockPos = hitResult.getBlockPos();
		if (!(minecraft.level.getBlockState(blockPos).getBlock() instanceof SafeBlock)) {
			return false;
		}

		if (!(minecraft.level.getBlockEntity(blockPos) instanceof SafeBlockEntity blockEntity)) {
			return false;
		}

		if (!blockEntity.hasCode()) {
			minecraft.setScreen(new SafeCodeScreen(blockPos, Mode.SET_CODE, false));
			return true;
		}

		if (blockEntity.isStructureLocked() && !blockEntity.isStructureOpen()) {
			minecraft.setScreen(new SafeCodeScreen(blockPos, Mode.ENTER_CODE, false));
			return true;
		}

		if (!blockEntity.isStructureLocked() && minecraft.options.keyShift.isDown()) {
			minecraft.setScreen(new SafeCodeScreen(blockPos, Mode.CONTROL, blockEntity.isStructureOpen()));
			return true;
		}

		return false;
	}

	@Override
	protected void init() {
		this.scale = 3;
		while (this.scale > 1 && (47 * this.scale > this.width - 24 || 70 * this.scale > this.height - 24)) {
			this.scale--;
		}

		this.panelWidth = 47 * this.scale;
		this.panelHeight = 70 * this.scale;
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;
		this.buttonWidth = 11 * this.scale;
		this.buttonHeight = 10 * this.scale;
		this.wideButtonWidth = 37 * this.scale;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gfx, mouseX, mouseY, partialTick);
		this.extractTransparentBackground(gfx);
		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.panelX, this.panelY, 95, 0, this.panelWidth, this.panelHeight, 47, 70, 142, 70);

		if (this.mode == Mode.CONTROL) {
			this.drawControlButtons(gfx, mouseX, mouseY);
			return;
		}

		this.drawInput(gfx);
		this.drawKeypad(gfx, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}

		if (this.mode == Mode.CONTROL) {
			if (this.clickControlButton(event.x(), event.y())) {
				return true;
			}

			return super.mouseClicked(event, doubleClick);
		}

		if (this.clickKeypadButton(event.x(), event.y())) {
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void drawInput(GuiGraphicsExtractor gfx) {
		int x = this.panelX + 7 * this.scale;
		int y = this.panelY + 7 * this.scale;
		int width = 33 * this.scale;
		int height = 8 * this.scale;

		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 102, 7, width, height, 33, 8, 142, 70);
		gfx.centeredText(this.font, this.code, x + width / 2, y + (height - 8) / 2, 0xFFFFFFFF);
	}

	private void drawKeypad(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
		for (int i = 0; i < 9; i++) {
			int column = i % 3;
			int row = i / 3;
			this.drawButton(gfx, mouseX, mouseY, this.buttonX(column), this.buttonY(row), this.buttonWidth, Component.literal(Integer.toString(i + 1)));
		}

		this.drawButton(gfx, mouseX, mouseY, this.buttonX(0), this.buttonY(3), this.buttonWidth, Component.translatable("gui.kurasu.clear"));
		this.drawButton(gfx, mouseX, mouseY, this.buttonX(1), this.buttonY(3), this.buttonWidth, Component.literal("0"));
		this.drawButton(gfx, mouseX, mouseY, this.buttonX(2), this.buttonY(3), this.buttonWidth, Component.translatable("gui.kurasu.confirm"));
	}

	private void drawControlButtons(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
		Component openLabel = this.open ? Component.translatable("gui.kurasu.close") : Component.translatable("gui.kurasu.open");
		this.drawButton(gfx, mouseX, mouseY, this.buttonX(0), this.buttonY(0), this.wideButtonWidth, openLabel);
		this.drawButton(gfx, mouseX, mouseY, this.buttonX(0), this.buttonY(1), this.wideButtonWidth, Component.translatable("gui.kurasu.lock"));
		this.drawButton(gfx, mouseX, mouseY, this.buttonX(0), this.buttonY(2), this.wideButtonWidth, Component.translatable("gui.kurasu.change_code"));
	}

	private void drawButton(GuiGraphicsExtractor gfx, int mouseX, int mouseY, int x, int y, int width, Component label) {
		boolean hovered = this.isInside(mouseX, mouseY, x, y, width, this.buttonHeight);
		int sourceX = hovered ? 67 : 100;
		int sourceY = hovered ? 24 : 19;
		gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, sourceX, sourceY, width, this.buttonHeight, 11, 10, 142, 70);
		gfx.centeredText(this.font, label, x + width / 2, y + (this.buttonHeight - 8) / 2, 0xFFFFFFFF);
	}

	private boolean clickControlButton(double mouseX, double mouseY) {
		if (this.isInside(mouseX, mouseY, this.buttonX(0), this.buttonY(0), this.wideButtonWidth, this.buttonHeight)) {
			ClientPlayNetworking.send(new SubmitSafeActionPayload(this.blockPos, SubmitSafeActionPayload.ACTION_TOGGLE_OPEN, ""));
			this.onClose();
			return true;
		}

		if (this.isInside(mouseX, mouseY, this.buttonX(0), this.buttonY(1), this.wideButtonWidth, this.buttonHeight)) {
			ClientPlayNetworking.send(new SubmitSafeActionPayload(this.blockPos, SubmitSafeActionPayload.ACTION_LOCK, ""));
			this.onClose();
			return true;
		}

		if (this.isInside(mouseX, mouseY, this.buttonX(0), this.buttonY(2), this.wideButtonWidth, this.buttonHeight)) {
			this.mode = Mode.CHANGE_CODE;
			this.code = "";
			return true;
		}

		return false;
	}

	private boolean clickKeypadButton(double mouseX, double mouseY) {
		for (int i = 0; i < 9; i++) {
			int column = i % 3;
			int row = i / 3;
			if (this.isInside(mouseX, mouseY, this.buttonX(column), this.buttonY(row), this.buttonWidth, this.buttonHeight)) {
				this.appendDigit((char) ('1' + i));
				return true;
			}
		}

		if (this.isInside(mouseX, mouseY, this.buttonX(0), this.buttonY(3), this.buttonWidth, this.buttonHeight)) {
			this.code = "";
			return true;
		}

		if (this.isInside(mouseX, mouseY, this.buttonX(1), this.buttonY(3), this.buttonWidth, this.buttonHeight)) {
			this.appendDigit('0');
			return true;
		}

		if (this.isInside(mouseX, mouseY, this.buttonX(2), this.buttonY(3), this.buttonWidth, this.buttonHeight)) {
			this.submitCode();
			return true;
		}

		return false;
	}

	private void appendDigit(char digit) {
		if (this.code.length() < 4) {
			this.code += digit;
		}
	}

	private void submitCode() {
		if (this.mode == Mode.CONTROL || this.code.length() != 4) {
			return;
		}

		int action = this.mode == Mode.ENTER_CODE ? SubmitSafeActionPayload.ACTION_ENTER_CODE : SubmitSafeActionPayload.ACTION_SET_CODE;
		ClientPlayNetworking.send(new SubmitSafeActionPayload(this.blockPos, action, this.code));
		this.onClose();
	}

	private int buttonX(int column) {
		return this.panelX + (5 + column * 13) * this.scale;
	}

	private int buttonY(int row) {
		return this.panelY + (19 + row * 12) * this.scale;
	}

	private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	private enum Mode {
		SET_CODE,
		ENTER_CODE,
		CHANGE_CODE,
		CONTROL
	}
}
