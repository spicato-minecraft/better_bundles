package better_bundles.client;

import better_bundles.network.BundleSlotActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Sends server-authoritative bundle overlay slot actions from the client.
 */
public final class BundleOverlayNetworking {

	private BundleOverlayNetworking() {
	}

	public static void sendAction(
			AbstractContainerScreen<?> screen,
			BundleHoverController.ActiveOverlay overlay,
			int innerSlot,
			BundleSlotActionPayload.Action action
	) {
		sendAction(screen, overlay, innerSlot, action, BundleSlotActionPayload.NO_SOURCE_SLOT);
	}

	public static void sendAction(
			AbstractContainerScreen<?> screen,
			BundleHoverController.ActiveOverlay overlay,
			int innerSlot,
			BundleSlotActionPayload.Action action,
			int sourceSlotId
	) {
		if (screen == null || overlay == null) {
			return;
		}

		if (!ClientPlayNetworking.canSend(BundleSlotActionPayload.TYPE)) {
			return;
		}

		AbstractContainerMenu menu = screen.getMenu();
		ClientPlayNetworking.send(new BundleSlotActionPayload(
				menu.getStateId(),
				overlay.bundleSlot().index,
				innerSlot,
				action,
				menu.getStateId(),
				sourceSlotId
		));
	}
}
