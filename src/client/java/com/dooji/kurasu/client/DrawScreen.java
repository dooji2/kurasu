package com.dooji.kurasu.client;

import com.dooji.kurasu.Kurasu;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public abstract class DrawScreen extends Screen {
	private static final Identifier DRAW_SCREEN_TEXTURE = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/sticky_note.png");
	private static final Identifier BRUSH_ICON = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/icons/brush.png");
	private static final Identifier ERASER_ICON = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/icons/eraser.png");
	private static final Identifier UP_ICON = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/icons/up.png");
	private static final Identifier DOWN_ICON = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "textures/gui/icons/down.png");
	private static final int MAX_ZOOM = 24;
	private static final int[] BLACKBOARD_COLORS = {0xFFFFFFFF};
	private static final int[] STICKY_NOTE_COLORS = {
		0xFFFFFFFF,
		0xFF1F1F1F,
		0xFFF6C7D6,
		0xFFF9E0A8,
		0xFFC9E8C8,
		0xFFC7DDF6
	};

	private final int canvasWidth;
	private final int canvasHeight;
	private final int[] colors;
	private final int[] pixels;
	private final NativeImage canvasImage;
	private final DynamicTexture canvasTexture;
	private final Identifier canvasTextureId;
	private int color;
	private boolean eraseMode;
	private boolean painting;
	private boolean panning;
	private boolean dirty;
	private boolean modified;
	private boolean previewVisible;
	private int guiX;
	private int guiY;
	private int toolScroll;
	private int brushSize = 1;
	private int zoom = 1;
	private double viewX;
	private double viewY;
	private int lastPaintX = -1;
	private int lastPaintY = -1;

	protected DrawScreen(Mode mode, int canvasWidth, int canvasHeight, int[] pixels) {
		super(Component.empty());
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		this.colors = switch (mode) {
			case BLACKBOARD -> BLACKBOARD_COLORS.clone();
			case STICKY_NOTE -> STICKY_NOTE_COLORS.clone();
		};
		this.pixels = Arrays.copyOf(pixels, canvasWidth * canvasHeight);
		this.color = this.colors[Math.min(2, this.colors.length - 1)];
		this.canvasTextureId = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "screen/draw/" + Integer.toHexString(System.identityHashCode(this)));
		this.canvasImage = new NativeImage(canvasWidth, canvasHeight, true);
		this.canvasTexture = new DynamicTexture(this.canvasTextureId::toString, this.canvasImage);

		for (int y = 0; y < this.canvasHeight; y++) {
			for (int x = 0; x < this.canvasWidth; x++) {
				this.canvasImage.setPixel(x, y, this.displayPixel(x, y));
			}
		}

		this.dirty = true;
		Minecraft.getInstance().getTextureManager().register(this.canvasTextureId, this.canvasTexture);
	}

	@Override
	protected void init() {
		this.guiX = (this.width - 250) / 2;
		this.guiY = (this.height - 180) / 2;
		if (this.guiX < 12) {
			this.guiX = 12;
		}

		if (this.guiY < 12) {
			this.guiY = 12;
		}

		this.zoom = Math.min(MAX_ZOOM, Math.max(218 / this.canvasWidth, 164 / this.canvasHeight) + 1);
		this.viewX = (this.canvasWidth - this.sourceWidth()) * 0.5;
		this.viewY = (this.canvasHeight - this.sourceHeight()) * 0.5;
		this.clampView();
		this.toolScroll = this.clampToolScroll(this.toolScroll);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			int x = this.guiX + 8;
			int top = this.guiY + 8;
			int bottom = this.guiY + 165;

			if (this.isInside(event.x(), event.y(), x, top, 10, 7)) {
				this.toolScroll = this.clampToolScroll(this.toolScroll - 1);
				return true;
			}

			if (this.isInside(event.x(), event.y(), x, bottom, 10, 7)) {
				this.toolScroll = this.clampToolScroll(this.toolScroll + 1);
				return true;
			}

			int listTop = top + 7;
			if (this.isInside(event.x(), event.y(), x, listTop, 10, bottom - listTop)) {
				int index = this.toolScroll + (int) ((event.y() - listTop) / 10);

				if (index < this.colors.length + 2) {
					if (index == 0) {
						this.eraseMode = false;
					} else if (index == 1) {
						this.eraseMode = true;
					} else {
						this.color = this.colors[index - 2];
						this.eraseMode = false;
					}

					return true;
				}
			}

			if (this.isInside(event.x(), event.y(), this.canvasDrawLeft(), this.canvasDrawTop(), this.canvasDrawWidth(), this.canvasDrawHeight())) {
				this.painting = true;
				this.lastPaintX = -1;
				this.lastPaintY = -1;
				this.paintAt(event.x(), event.y());
				return true;
			}
			}

			if (event.button() == 1 && this.isInside(event.x(), event.y(), this.canvasDrawLeft(), this.canvasDrawTop(), this.canvasDrawWidth(), this.canvasDrawHeight())) {
				this.panning = true;
				return true;
			}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.painting) {
			this.paintAt(event.x(), event.y());
			return true;
		}

		if (this.panning) {
			this.viewX -= dragX / this.zoom;
			this.viewY -= dragY / this.zoom;
			this.clampView();
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && this.painting) {
			this.painting = false;
			this.lastPaintX = -1;
			this.lastPaintY = -1;
			return true;
		}

		if (event.button() == 1 && this.panning) {
			this.panning = false;
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0.0) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}

		if (InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
			this.brushSize = Mth.clamp(this.brushSize + (scrollY > 0.0 ? 1 : -1), 1, 8);
			return true;
		}

		if (this.isInside(mouseX, mouseY, this.canvasDrawLeft(), this.canvasDrawTop(), this.canvasDrawWidth(), this.canvasDrawHeight())) {
			int newZoom = Mth.clamp(this.zoom + (scrollY > 0.0 ? 1 : -1), 1, MAX_ZOOM);
			if (newZoom != this.zoom) {
				int drawLeft = this.canvasDrawLeft();
				int drawTop = this.canvasDrawTop();
				double pixelX = this.sourceX() + (mouseX - drawLeft) / this.zoom;
				double pixelY = this.sourceY() + (mouseY - drawTop) / this.zoom;
				this.zoom = newZoom;
				this.viewX = pixelX - (mouseX - this.canvasDrawLeft()) / this.zoom;
				this.viewY = pixelY - (mouseY - this.canvasDrawTop()) / this.zoom;
				this.clampView();
			}

			return true;
		}

		int toolX = this.guiX + 8;
		int toolY = this.guiY + 8;
		if (this.isInside(mouseX, mouseY, toolX, toolY, 10, 163)) {
			this.toolScroll = this.clampToolScroll(this.toolScroll + (scrollY > 0.0 ? -1 : 1));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gfx, mouseX, mouseY, partialTick);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX, this.guiY, 0, 0, 25, 9, 25, 9, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX + 25, this.guiY, 25, 0, 217, 9, 217, 9, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX + 242, this.guiY, 242, 0, 8, 9, 8, 9, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX, this.guiY + 9, 0, 9, 25, 163, 25, 163, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX + 25, this.guiY + 9, 25, 9, 217, 163, 217, 163, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX + 242, this.guiY + 9, 242, 9, 8, 163, 8, 163, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX, this.guiY + 172, 0, 172, 25, 8, 25, 8, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX + 25, this.guiY + 172, 25, 172, 217, 8, 217, 8, 260, 180);
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, this.guiX + 242, this.guiY + 172, 242, 172, 8, 8, 8, 8, 260, 180);

		boolean hadPreview = this.previewVisible;
		boolean previewVisible = false;
		if (this.isInside(mouseX, mouseY, this.canvasDrawLeft(), this.canvasDrawTop(), this.canvasDrawWidth(), this.canvasDrawHeight())) {
			int centerX = this.mouseToCanvasX(mouseX) - this.brushSize / 2;
			int centerY = this.mouseToCanvasY(mouseY) - this.brushSize / 2;

			// idk how to make the preview pixel perfect so imma do this for now
			for (int y = 0; y < this.brushSize; y++) {
				for (int x = 0; x < this.brushSize; x++) {
					int pixelX = centerX + x;
					int pixelY = centerY + y;

					if (pixelX < 0 || pixelY < 0 || pixelX >= this.canvasWidth || pixelY >= this.canvasHeight) {
						continue;
					}

					this.canvasImage.setPixel(pixelX, pixelY, this.eraseMode ? this.emptyPixelColor(pixelX, pixelY) : this.color);
					previewVisible = true;
				}
			}
		}

		if (this.dirty || previewVisible || hadPreview) {
			this.canvasTexture.upload();
			this.dirty = false;
		}

		gfx.blit(
			RenderPipelines.GUI_TEXTURED,
			this.canvasTextureId,
			this.canvasDrawLeft(),
			this.canvasDrawTop(),
			(float) this.sourceX(),
			(float) this.sourceY(),
			this.canvasDrawWidth(),
			this.canvasDrawHeight(),
			this.sourceWidth(),
			this.sourceHeight(),
			this.canvasWidth,
			this.canvasHeight
		);

		if (previewVisible) {
			int centerX = this.mouseToCanvasX(mouseX) - this.brushSize / 2;
			int centerY = this.mouseToCanvasY(mouseY) - this.brushSize / 2;

			for (int y = 0; y < this.brushSize; y++) {
				for (int x = 0; x < this.brushSize; x++) {
					int pixelX = centerX + x;
					int pixelY = centerY + y;

					if (pixelX < 0 || pixelY < 0 || pixelX >= this.canvasWidth || pixelY >= this.canvasHeight) {
						continue;
					}

					this.canvasImage.setPixel(pixelX, pixelY, this.displayPixel(pixelX, pixelY));
				}
			}
		}

		this.previewVisible = previewVisible;
		int x = this.guiX + 8;
		int top = this.guiY + 8;
		int bottom = this.guiY + 165;

		this.drawArrowButton(gfx, x, top, this.isInside(mouseX, mouseY, x, top, 10, 7), UP_ICON);
		this.drawArrowButton(gfx, x, bottom, this.isInside(mouseX, mouseY, x, bottom, 10, 7), DOWN_ICON);

		int first = this.toolScroll;
		int visible = 15;
		int last = Math.min(this.colors.length + 2, first + visible);
		int listTop = top + 7;

		for (int i = first; i < last; i++) {
			int y = listTop + (i - first) * 10;
			boolean hovered = this.isInside(mouseX, mouseY, x, y, 10, 10);
			boolean selected;
			if (i == 0) {
				selected = !this.eraseMode;
			} else if (i == 1) {
				selected = this.eraseMode;
			} else {
				selected = !this.eraseMode && this.color == this.colors[i - 2];
			}

			gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, x, y, 250, (hovered || selected) ? 17 : 0, 10, 10, 10, 10, 260, 180);

			if (i == 0) {
				this.drawIcon(gfx, BRUSH_ICON, x, y, 10, 8, 8);
			} else if (i == 1) {
				this.drawIcon(gfx, ERASER_ICON, x, y, 10, 8, 8);
			} else {
				gfx.fill(x + 2, y + 2, x + 8, y + 8, this.colors[i - 2]);
			}
		}
	}

	@Override
	public void onClose() {
		this.painting = false;
		this.panning = false;

		if (this.modified) {
			this.save(this.canvasWidth, this.canvasHeight, this.pixels);
		}

		super.onClose();
	}

	@Override
	public void removed() {
		Minecraft.getInstance().getTextureManager().release(this.canvasTextureId);
		super.removed();
	}

	protected abstract void save(int width, int height, int[] pixels);

	private void drawArrowButton(GuiGraphicsExtractor gfx, int x, int y, boolean hovered, Identifier icon) {
		gfx.blit(RenderPipelines.GUI_TEXTURED, DRAW_SCREEN_TEXTURE, x, y, 250, hovered ? 27 : 10, 10, 7, 10, 7, 260, 180);
		this.drawIcon(gfx, icon, x, y, 7, 8, 5);
	}

	private void drawIcon(GuiGraphicsExtractor gfx, Identifier texture, int x, int y, int buttonHeight, int width, int height) {
		int drawX = x + (10 - width) / 2;
		int drawY = y + (buttonHeight - height) / 2;
		gfx.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, 0, 0, width, height, width, height, width, height);
	}

	private int clampToolScroll(int value) {
		return Mth.clamp(value, 0, Math.max(0, this.colors.length + 2 - 15));
	}

	private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	private void paintAt(double mouseX, double mouseY) {
		int x = this.mouseToCanvasX(mouseX);
		int y = this.mouseToCanvasY(mouseY);

		if (this.lastPaintX < 0) {
			this.paintBrush(x, y);
		} else {
			this.paintLine(this.lastPaintX, this.lastPaintY, x, y);
		}

		this.lastPaintX = x;
		this.lastPaintY = y;
	}

	private void paintLine(int startX, int startY, int endX, int endY) {
		int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));

		for (int i = 0; i <= steps; i++) {
			float t = steps == 0 ? 0.0f : (float) i / steps;
			int x = Math.round(Mth.lerp(t, startX, endX));
			int y = Math.round(Mth.lerp(t, startY, endY));
			this.paintBrush(x, y);
		}
	}

	private void paintBrush(int centerX, int centerY) {
		int startX = centerX - this.brushSize / 2;
		int startY = centerY - this.brushSize / 2;

		for (int y = 0; y < this.brushSize; y++) {
			for (int x = 0; x < this.brushSize; x++) {
				this.paintPixel(startX + x, startY + y);
			}
		}
	}

	private void paintPixel(int x, int y) {
		if (x < 0 || y < 0 || x >= this.canvasWidth || y >= this.canvasHeight) {
			return;
		}

		int index = x + y * this.canvasWidth;
		int pixel = this.eraseMode ? 0 : this.color;
		if (this.pixels[index] == pixel) {
			return;
		}

		this.pixels[index] = pixel;
		this.canvasImage.setPixel(x, y, this.displayPixel(x, y));
		this.dirty = true;
		this.modified = true;
	}

	private int displayPixel(int x, int y) {
		int pixel = this.pixels[x + y * this.canvasWidth];
		return ARGB.alpha(pixel) == 0 ? this.emptyPixelColor(x, y) : pixel;
	}

	private int emptyPixelColor(int x, int y) {
		return (x / 16 + y / 16) % 2 == 0 ? 0xFF808080 : 0xFFC0C0C0;
	}

	private int canvasDrawLeft() {
		return this.guiX + 24 + (218 - this.canvasDrawWidth()) / 2;
	}

	private int canvasDrawTop() {
		return this.guiY + 8 + (164 - this.canvasDrawHeight()) / 2;
	}

	private int canvasDrawWidth() {
		return Math.min(218, this.canvasWidth * this.zoom);
	}

	private int canvasDrawHeight() {
		return Math.min(164, this.canvasHeight * this.zoom);
	}

	private double sourceX() {
		return this.canvasWidth * this.zoom <= 218 ? 0.0 : this.viewX;
	}

	private double sourceY() {
		return this.canvasHeight * this.zoom <= 164 ? 0.0 : this.viewY;
	}

	private int sourceWidth() {
		if (this.canvasWidth * this.zoom <= 218) {
			return this.canvasWidth;
		}

		return Mth.ceil(218.0f / this.zoom);
	}

	private int sourceHeight() {
		if (this.canvasHeight * this.zoom <= 164) {
			return this.canvasHeight;
		}

		return Mth.ceil(164.0f / this.zoom);
	}

	private void clampView() {
		this.viewX = Mth.clamp(this.viewX, 0.0, this.canvasWidth - this.sourceWidth());
		this.viewY = Mth.clamp(this.viewY, 0.0, this.canvasHeight - this.sourceHeight());
	}

	private int mouseToCanvasX(double mouseX) {
		double pixel = this.sourceX() + (mouseX - this.canvasDrawLeft()) / this.zoom;
		return Mth.clamp(Mth.floor(pixel), 0, this.canvasWidth - 1);
	}

	private int mouseToCanvasY(double mouseY) {
		double pixel = this.sourceY() + (mouseY - this.canvasDrawTop()) / this.zoom;
		return Mth.clamp(Mth.floor(pixel), 0, this.canvasHeight - 1);
	}

	protected enum Mode {
		BLACKBOARD,
		STICKY_NOTE
	}
}
