package better_bundles.mixin.client;

import better_bundles.client.BundleHoverController;
import better_bundles.client.BundleOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Queue inner-slot tooltips immediately before deferred tooltip drawing.
 * {@code AbstractContainerScreen#renderTooltip} HEAD-cancel skips TAIL injectors,
 * so this must not live on that method's TAIL.
 */
@Mixin(Screen.class)
public abstract class ScreenDeferredTooltipMixin {

	@Inject(
			method = "renderWithTooltipAndSubtitles",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;renderDeferredElements()V"
			)
	)
	private void better_bundles$queueInnerOverlayTooltip(
			GuiGraphics graphics,
			int mouseX,
			int mouseY,
			float delta,
			CallbackInfo ci
	) {
		if (!((Object) this instanceof AbstractContainerScreen<?> screen)) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) {
			return;
		}

		BundleHoverController.getInstance().getActive().ifPresent(overlay ->
				BundleOverlayRenderer.queueInnerSlotTooltip(
						graphics,
						minecraft.font,
						screen,
						overlay,
						mouseX,
						mouseY
				)
		);
	}
}
