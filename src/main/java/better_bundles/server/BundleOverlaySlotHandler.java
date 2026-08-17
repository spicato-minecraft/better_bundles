package better_bundles.server;

import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.inventory.BundleSlotPolicy;
import better_bundles.network.BundleSlotActionPayload;
import better_bundles.network.BundleSlotActionPayload.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
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

		Slot bundleContainerSlot = menu.getSlot(payload.containerSlotId());
		ItemStack bundleStack = bundleContainerSlot.getItem();
		BundleInventory inventory = BundleInventoryAccess.read(bundleStack);
		ItemStack carried = menu.getCarried();

		ApplyResult result = switch (payload.action()) {
			case PLACE -> applyPlace(inventory, carried, payload.innerSlot());
			case PICKUP -> applyPickup(inventory, carried, payload.innerSlot());
			case SWAP -> applySwap(inventory, carried, payload.innerSlot());
			case SHIFT_MOVE -> applyShiftMove(menu, player, inventory, payload);
			case MERGE -> applyMerge(inventory, carried, payload.innerSlot());
		};

		if (result == null) {
			return false;
		}

		BundleInventoryAccess.write(bundleStack, result.inventory());
		bundleContainerSlot.set(bundleStack);
		menu.setCarried(result.carried());
		bundleContainerSlot.setChanged();
		menu.slotsChanged(bundleContainerSlot.container);
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

		if (payload.action() != Action.SHIFT_MOVE || payload.sourceSlotId() < 0) {
			if (!isValidInnerSlot(payload.innerSlot())) {
				return false;
			}
		}

		if (payload.sourceSlotId() != BundleSlotActionPayload.NO_SOURCE_SLOT
				&& !menu.isValidSlotIndex(payload.sourceSlotId())) {
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

	private static ApplyResult applyMerge(BundleInventory inventory, ItemStack carried, int innerSlot) {
		ItemStack innerStack = inventory.get(innerSlot);
		if (carried.isEmpty() || innerStack.isEmpty()) {
			return null;
		}

		if (!ItemStack.isSameItemSameComponents(carried, innerStack)) {
			return null;
		}

		int maxStack = innerStack.getMaxStackSize();
		int space = maxStack - innerStack.getCount();
		if (space <= 0) {
			return null;
		}

		int toMerge = Math.min(space, carried.getCount());
		ItemStack mergedInner = innerStack.copy();
		mergedInner.grow(toMerge);

		ItemStack newCarried = carried.copy();
		newCarried.shrink(toMerge);
		if (newCarried.isEmpty()) {
			newCarried = ItemStack.EMPTY;
		}

		return new ApplyResult(inventory.set(innerSlot, mergedInner), newCarried);
	}

	private static ApplyResult applyShiftMove(
			AbstractContainerMenu menu,
			Player player,
			BundleInventory inventory,
			BundleSlotActionPayload payload
	) {
		if (payload.sourceSlotId() >= 0) {
			return applyShiftMoveIntoBundle(menu, inventory, payload.containerSlotId(), payload.sourceSlotId());
		}

		return applyShiftMoveOutOfBundle(menu, player, inventory, payload.innerSlot());
	}

	private static ApplyResult applyShiftMoveOutOfBundle(
			AbstractContainerMenu menu,
			Player player,
			BundleInventory inventory,
			int innerSlot
	) {
		ItemStack innerStack = inventory.get(innerSlot);
		if (innerStack.isEmpty()) {
			return null;
		}

		ItemStack remaining = moveStackIntoPlayerInventory(menu, player, innerStack.copy());
		if (ItemStack.matches(remaining, innerStack)) {
			return null;
		}

		BundleInventory updated = remaining.isEmpty()
				? inventory.set(innerSlot, ItemStack.EMPTY)
				: inventory.set(innerSlot, remaining);
		return new ApplyResult(updated, menu.getCarried());
	}

	private static ApplyResult applyShiftMoveIntoBundle(
			AbstractContainerMenu menu,
			BundleInventory inventory,
			int bundleContainerSlotId,
			int sourceSlotId
	) {
		if (sourceSlotId == bundleContainerSlotId) {
			return null;
		}

		Slot sourceSlot = menu.getSlot(sourceSlotId);
		ItemStack sourceStack = sourceSlot.getItem();
		if (sourceStack.isEmpty()) {
			return null;
		}

		if (!BundleSlotPolicy.canInsert(sourceStack)) {
			return null;
		}

		int targetInner = inventory.insertSlot();
		if (!inventory.get(targetInner).isEmpty() && inventory.firstEmpty().isEmpty()) {
			return null;
		}

		if (!inventory.get(targetInner).isEmpty()) {
			targetInner = inventory.firstEmpty().orElse(-1);
			if (targetInner < 0) {
				return null;
			}
		}

		ItemStack toInsert = sourceStack.copy();
		toInsert.setCount(Math.min(toInsert.getCount(), toInsert.getMaxStackSize()));

		sourceStack.shrink(toInsert.getCount());
		sourceSlot.setChanged();

		return new ApplyResult(inventory.set(targetInner, toInsert), menu.getCarried());
	}

	private static ItemStack moveStackIntoPlayerInventory(AbstractContainerMenu menu, Player player, ItemStack stack) {
		ItemStack remaining = stack.copy();

		for (Slot slot : menu.slots) {
			if (!isPlayerInventorySlot(slot, player)) {
				continue;
			}

			ItemStack slotStack = slot.getItem();
			if (!slotStack.isEmpty()
					&& ItemStack.isSameItemSameComponents(slotStack, remaining)
					&& slotStack.getCount() < slotStack.getMaxStackSize()) {
				int transferable = Math.min(
						remaining.getCount(),
						slotStack.getMaxStackSize() - slotStack.getCount()
				);
				if (transferable <= 0) {
					continue;
				}

				slotStack.grow(transferable);
				remaining.shrink(transferable);
				slot.setChanged();
				if (remaining.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}

		for (Slot slot : menu.slots) {
			if (!isPlayerInventorySlot(slot, player)) {
				continue;
			}

			if (slot.getItem().isEmpty() && slot.mayPlace(remaining)) {
				slot.set(remaining.copy());
				return ItemStack.EMPTY;
			}
		}

		return remaining;
	}

	private static boolean isPlayerInventorySlot(Slot slot, Player player) {
		return slot.container instanceof Inventory inventory && inventory == player.getInventory();
	}

	private record ApplyResult(BundleInventory inventory, ItemStack carried) {
	}
}
