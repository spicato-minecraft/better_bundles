package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenInputMixin {

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void better_bundles$updateHoverOnContainerTick(CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getWindow() == null) {
			return;
		}

		double scaleX = (double) minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
		double scaleY = (double) minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
		double mouseX = minecraft.mouseHandler.xpos() * scaleX;
		double mouseY = minecraft.mouseHandler.ypos() * scaleY;
		updateHoverController(mouseX, mouseY);
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void better_bundles$closeHoverOnRemoved(CallbackInfo ci) {
		BundleHoverController.getInstance().close();
	}

	private void updateHoverController(double mouseX, double mouseY) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		BundleHoverController.getInstance().update(screen, mouseX, mouseY);
	}
}
