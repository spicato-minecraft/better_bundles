package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenInputMixin {

	@Shadow
	protected Minecraft minecraft;

	@Inject(method = "mouseMoved", at = @At("HEAD"))
	private void better_bundles$updateHoverOnMouseMoved(double mouseX, double mouseY, CallbackInfo ci) {
		updateHoverController(mouseX, mouseY);
	}

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void better_bundles$updateHoverOnContainerTick(CallbackInfo ci) {
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

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void better_bundles$blockOverlayMouseClicked(
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
	private void better_bundles$blockOverlayMouseReleased(
			MouseButtonEvent event,
			CallbackInfoReturnable<Boolean> cir
	) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (BundleHoverController.getInstance().handleOverlayMouseRelease(screen, event)) {
			cir.setReturnValue(true);
		} else if (BundleHoverController.getInstance().shouldBlockOverlayMouse(screen, event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void better_bundles$blockOverlayMouseDragged(
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
