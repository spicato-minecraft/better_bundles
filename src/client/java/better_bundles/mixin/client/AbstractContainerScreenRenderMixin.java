package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import better_bundles.client.BundleOverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenRenderMixin {

	@Shadow
	protected net.minecraft.client.gui.Font font;

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderContents(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
					shift = At.Shift.AFTER
			)
	)
	private void better_bundles$renderBundleOverlay(
			GuiGraphics graphics,
			int mouseX,
			int mouseY,
			float delta,
			CallbackInfo ci
	) {
		BundleHoverController.getInstance().getActive().ifPresent(overlay ->
				BundleOverlayRenderer.render(graphics, font, overlay)
		);
	}
}
