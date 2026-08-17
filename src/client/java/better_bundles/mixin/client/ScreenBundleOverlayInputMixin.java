package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes container-screen mouse input through the bundle overlay on {@link Screen},
 * where {@code mouseClicked} / {@code mouseReleased} live in 1.21.11.
 */
@Mixin(Screen.class)
public abstract class ScreenBundleOverlayInputMixin {

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overlayMouseClicked(
			MouseButtonEvent event,
			boolean doubled,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (!((Object) this instanceof AbstractContainerScreen<?> screen)) {
			return;
		}

		if (BundleHoverController.getInstance().handleOverlayMouseClick(screen, event, doubled)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overlayMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof AbstractContainerScreen<?> screen)) {
			return;
		}

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
		if (!((Object) this instanceof AbstractContainerScreen<?> screen)) {
			return;
		}

		if (BundleHoverController.getInstance().shouldBlockOverlayMouse(screen, event)) {
			cir.setReturnValue(true);
		}
	}
}
