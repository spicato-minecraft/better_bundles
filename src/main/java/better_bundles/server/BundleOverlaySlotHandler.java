package better_bundles.server;

import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.inventory.BundleSlotPolicy;
import better_bundles.network.BundleSlotActionPayload;
import better_bundles.network.BundleSlotActionPayload.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Applies validated C2S bundle overlay slot actions on the server.
 */
public final class BundleOverlaySlotHandler {

	private BundleOverlaySlotHandler() {
	}

	public static boolean handle(ServerPlayer player, BundleSlotActionPayload payload) {
		return handle(player.containerMenu, player, payload);
	}

	public static boolean handle(AbstractContainerMenu menu, Player player, BundleSlotActionPayload payload) {
		if (!validate(menu, player, payload)) {
			return false;
		}

		Slot slot = menu.getSlot(payload.containerSlotId());
		ItemStack bundleStack = slot.getItem();
		BundleInventory inventory = BundleInventoryAccess.read(bundleStack);
		ItemStack carried = menu.getCarried();

		ApplyResult result = switch (payload.action()) {
			case PLACE -> applyPlace(inventory, carried, payload.innerSlot());
			case PICKUP -> applyPickup(inventory, carried, payload.innerSlot());
			case SWAP -> applySwap(inventory, carried, payload.innerSlot());
			case SHIFT_MOVE -> null;
		};

		if (result == null) {
			return false;
		}

		BundleInventoryAccess.write(bundleStack, result.inventory());
		slot.set(bundleStack);
		menu.setCarried(result.carried());
		slot.setChanged();
		menu.slotsChanged(slot.container);
		menu.broadcastChanges();
		return true;
	}

	private static boolean validate(AbstractContainerMenu menu, Player player, BundleSlotActionPayload payload) {
		if (!menu.stillValid(player)) {
			return false;
		}

		if (payload.screenSyncId() != menu.getStateId()) {
			return false;
		}

		if (payload.carriedRevision() != menu.getStateId()) {
			return false;
		}

		if (!menu.isValidSlotIndex(payload.containerSlotId())) {
			return false;
		}

		if (!isValidInnerSlot(payload.innerSlot())) {
			return false;
		}

		ItemStack bundleStack = menu.getSlot(payload.containerSlotId()).getItem();
		return BundleInventoryAccess.isBundleItem(bundleStack);
	}

	private static boolean isValidInnerSlot(int innerSlot) {
		return innerSlot >= 0 && innerSlot < BundleInventory.SLOT_COUNT;
	}

	private static ApplyResult applyPlace(BundleInventory inventory, ItemStack carried, int innerSlot) {
		if (carried.isEmpty() || !inventory.get(innerSlot).isEmpty()) {
			return null;
		}

		if (!BundleSlotPolicy.canInsert(carried)) {
			return null;
		}

		return new ApplyResult(inventory.set(innerSlot, carried.copy()), ItemStack.EMPTY);
	}

	private static ApplyResult applyPickup(BundleInventory inventory, ItemStack carried, int innerSlot) {
		ItemStack innerStack = inventory.get(innerSlot);
		if (!carried.isEmpty() || innerStack.isEmpty()) {
			return null;
		}

		return new ApplyResult(inventory.set(innerSlot, ItemStack.EMPTY), innerStack.copy());
	}

	private static ApplyResult applySwap(BundleInventory inventory, ItemStack carried, int innerSlot) {
		ItemStack innerStack = inventory.get(innerSlot);
		if (carried.isEmpty() || innerStack.isEmpty()) {
			return null;
		}

		if (!BundleSlotPolicy.canInsert(carried)) {
			return null;
		}

		return new ApplyResult(inventory.set(innerSlot, carried.copy()), innerStack.copy());
	}

	private record ApplyResult(BundleInventory inventory, ItemStack carried) {
	}
}
