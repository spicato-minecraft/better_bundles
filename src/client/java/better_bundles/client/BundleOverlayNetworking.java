package better_bundles.client;

import better_bundles.inventory.CreativeInventoryMenuSlots;
import better_bundles.network.BundleSlotActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.OptionalInt;

/**
 * Sends server-authoritative bundle overlay slot actions from the client.
 */
public final class BundleOverlayNetworking {

	private BundleOverlayNetworking() {
	}

	public static boolean sendAction(
			AbstractContainerScreen<?> screen,
			BundleHoverController.ActiveOverlay overlay,
			int innerSlot,
			BundleSlotActionPayload.Action action,
			int mouseButton
	) {
		return sendAction(
				screen,
				overlay,
				innerSlot,
				action,
				BundleSlotActionPayload.NO_SOURCE_SLOT,
				mouseButton
		);
	}

	public static boolean sendAction(
			AbstractContainerScreen<?> screen,
			BundleHoverController.ActiveOverlay overlay,
			int innerSlot,
			BundleSlotActionPayload.Action action,
			int sourceSlotId,
			int mouseButton
	) {
		if (screen == null || overlay == null) {
			return false;
		}

		if (!ClientPlayNetworking.canSend(BundleSlotActionPayload.TYPE)) {
			return false;
		}

		int containerSlotId = resolveContainerSlotId(screen, overlay.bundleSlot());
		if (containerSlotId < 0) {
			return false;
		}

		int remappedSource = sourceSlotId;
		if (sourceSlotId != BundleSlotActionPayload.NO_SOURCE_SLOT) {
			remappedSource = resolveSourceSlotId(screen, sourceSlotId);
			if (remappedSource < 0) {
				return false;
			}
		}

		int stateId = resolveStateId(screen);
		ClientPlayNetworking.send(new BundleSlotActionPayload(
				stateId,
				containerSlotId,
				innerSlot,
				action,
				stateId,
				remappedSource,
				mouseButton
		));
		return true;
	}

	private static int resolveContainerSlotId(AbstractContainerScreen<?> screen, Slot bundleSlot) {
		if (!(screen instanceof CreativeModeInventoryScreen)) {
			return bundleSlot.index;
		}

		OptionalInt mapped = CreativeInventoryMenuSlots.toInventoryMenuIndex(bundleSlot);
		return mapped.orElse(-1);
	}

	private static int resolveSourceSlotId(AbstractContainerScreen<?> screen, int sourceSlotId) {
		if (!(screen instanceof CreativeModeInventoryScreen)) {
			return sourceSlotId;
		}

		Slot source = screen.getMenu().getSlot(sourceSlotId);
		OptionalInt mapped = CreativeInventoryMenuSlots.toInventoryMenuIndex(source);
		return mapped.orElse(-1);
	}

	private static int resolveStateId(AbstractContainerScreen<?> screen) {
		if (screen instanceof CreativeModeInventoryScreen) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				return player.inventoryMenu.getStateId();
			}
		}

		AbstractContainerMenu menu = screen.getMenu();
		return menu.getStateId();
	}
}
