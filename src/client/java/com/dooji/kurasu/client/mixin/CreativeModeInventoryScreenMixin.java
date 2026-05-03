package com.dooji.kurasu.client.mixin;

import com.dooji.kurasu.KurasuCreativeTabs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@Redirect(
		method = "extractTabButton",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;II)V"
		)
	)
	private void renderKurasuTabIcon(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y, GuiGraphicsExtractor ignored, int mouseX, int mouseY, CreativeModeTab tab) {
		gfx.item(KurasuCreativeTabs.isKurasuTab(tab) ? KurasuCreativeTabs.createTabIcon() : stack, x, y);
	}
}
