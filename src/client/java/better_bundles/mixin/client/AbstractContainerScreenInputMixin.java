package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenInputMixin {

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void better_bundles$updateHoverOnContainerTick(CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getWindow() == null) {
			return;
		}

		double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
		double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
		updateHoverController(mouseX, mouseY);
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void better_bundles$closeHoverOnRemoved(CallbackInfo ci) {
		BundleHoverController.getInstance().close();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overlayMouseClicked(
			MouseButtonEvent event,
			boolean doubled,
			CallbackInfoReturnable<Boolean> cir
	) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (BundleHoverController.getInstance().handleOverlayMouseClick(screen, event, doubled)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overlayMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		BundleHoverController controller = BundleHoverController.getInstance();
		if (controller.handleOverlayMouseRelease(screen, event)) {
			cir.setReturnValue(true);
		} else if (controller.shouldBlockOverlayMouse(screen, event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overlayMouseDragged(
			MouseButtonEvent event,
			double dragX,
			double dragY,
			CallbackInfoReturnable<Boolean> cir
	) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (BundleHoverController.getInstance().shouldBlockOverlayMouse(screen, event)) {
			cir.setReturnValue(true);
		}
	}

	private void updateHoverController(double mouseX, double mouseY) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		BundleHoverController.getInstance().update(screen, mouseX, mouseY);
	}
}
