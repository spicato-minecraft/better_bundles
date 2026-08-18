package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import better_bundles.inventory.BundleInventoryAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenInputMixin {

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void better_bundles$updateHoverOnContainerTick(CallbackInfo ci) {
		double[] mouse = BundleHoverController.currentMousePosition();
		updateHoverController(mouse[0], mouse[1]);
	}

	@Inject(method = "getHoveredSlot", at = @At("RETURN"), cancellable = true)
	private void better_bundles$suppressHoveredSlotUnderInnerOverlay(
			double mouseX,
			double mouseY,
			CallbackInfoReturnable<Slot> cir
	) {
		BundleHoverController controller = BundleHoverController.getInstance();
		if (controller.getActive().isEmpty()) {
			return;
		}

		if (controller.isPointInOverlay(mouseX, mouseY)) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "onMouseClickAction", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overlayOnMouseClickAction(Slot slot, ClickType clickType, CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		BundleHoverController controller = BundleHoverController.getInstance();
		var active = controller.getActive();
		if (active.isEmpty()) {
			return;
		}

		double[] mouse = BundleHoverController.currentMousePosition();
		OptionalInt innerSlot = active.get().hitInnerSlot(mouse[0], mouse[1]);
		if (innerSlot.isEmpty()) {
			return;
		}

		if (controller.handleOverlayClickType(screen, active.get(), innerSlot.getAsInt(), clickType)) {
			ci.cancel();
		}
	}

	@Inject(method = "showTooltipWithItemInHand", at = @At("HEAD"), cancellable = true)
	private void better_bundles$suppressVanillaBundleHandTooltip(
			ItemStack stack,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (BundleInventoryAccess.isBundleItem(stack)) {
			cir.setReturnValue(false);
		}
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

		if (!screen.getMenu().getCarried().isEmpty()) {
			if (controller.handleOverlayMouseRelease(screen, event)) {
				cir.setReturnValue(true);
			}
			return;
		}

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
		if (!screen.getMenu().getCarried().isEmpty()) {
			return;
		}

		if (BundleHoverController.getInstance().shouldBlockOverlayMouse(screen, event)) {
			cir.setReturnValue(true);
		}
	}

	private void updateHoverController(double mouseX, double mouseY) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		BundleHoverController.getInstance().update(screen, mouseX, mouseY);
	}
}
