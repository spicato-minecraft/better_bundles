package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import better_bundles.client.BundleOverlayRenderer;
import better_bundles.inventory.BundleInventoryAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenRenderMixin {

	@Inject(
			method = "extractContents",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
					shift = At.Shift.AFTER
			)
	)
	private void better_bundles$renderBundleOverlay(
			GuiGraphicsExtractor graphics,
			int mouseX,
			int mouseY,
			float delta,
			CallbackInfo ci
	) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) {
			return;
		}

		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor pos = (AbstractContainerScreenAccessor) screen;
		BundleHoverController.getInstance().getActive().ifPresent(overlay ->
				BundleOverlayRenderer.renderOverlay(
						graphics,
						minecraft.font,
						overlay,
						pos.better_bundles$getLeftPos(),
						pos.better_bundles$getTopPos(),
						mouseX,
						mouseY
				)
		);
	}

	@Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
	private void better_bundles$suppressVanillaBundleSlotTooltip(
			GuiGraphicsExtractor graphics,
			int mouseX,
			int mouseY,
			CallbackInfo ci
	) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		BundleHoverController controller = BundleHoverController.getInstance();
		var overlay = controller.getActive();
		if (overlay.isPresent() && overlay.get().hitInnerSlot(mouseX, mouseY).isPresent()) {
			ci.cancel();
			return;
		}

		Slot hovered = ((AbstractContainerScreenAccessor) screen).better_bundles$getHoveredSlot();
		if (hovered != null && BundleInventoryAccess.isBundleItem(hovered.getItem())) {
			ci.cancel();
		}
	}
}
