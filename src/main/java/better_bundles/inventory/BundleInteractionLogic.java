package better_bundles.inventory;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Server-side pick/insert semantics for bundle right-click interactions.
 * Mutates bundle stacks exclusively through {@link BundleInventoryAccess#write}.
 */
public final class BundleInteractionLogic {

	private BundleInteractionLogic() {
	}

	public record InsertResult(boolean inserted, ItemStack ejected) {
		public static InsertResult failed() {
			return new InsertResult(false, ItemStack.EMPTY);
		}

		public static InsertResult success() {
			return new InsertResult(true, ItemStack.EMPTY);
		}

		public static InsertResult successWithEject(ItemStack ejected) {
			return new InsertResult(true, ejected);
		}
	}

	/**
	 * Removes one item from the lowest occupied inner slot (slot 0 first).
	 *
	 * @return the picked stack (count 1), or empty if the bundle has no contents
	 */
	public static Optional<ItemStack> tryPickOne(ItemStack bundleStack) {
		return tryPickFromSlot(bundleStack, 1);
	}

	/**
	 * Removes the entire stack from the lowest occupied inner slot (slot 0 first).
	 * Used for bundle right-click extraction in inventory.
	 */
	public static Optional<ItemStack> tryPickStack(ItemStack bundleStack) {
		return tryPickFromSlot(bundleStack, Integer.MAX_VALUE);
	}

	private static Optional<ItemStack> tryPickFromSlot(ItemStack bundleStack, int maxCount) {
		BundleInventory inventory = BundleInventoryAccess.read(bundleStack);
		OptionalInt pickIndex = inventory.pickSlot();
		if (pickIndex.isEmpty()) {
			return Optional.empty();
		}

		int slot = pickIndex.getAsInt();
		ItemStack slotStack = inventory.get(slot);
		if (slotStack.isEmpty()) {
			return Optional.empty();
		}

		int pickCount = Math.min(maxCount, slotStack.getCount());
		ItemStack picked = slotStack.copyWithCount(pickCount);
		ItemStack remaining = slotStack.copyWithCount(slotStack.getCount() - pickCount);
		BundleInventoryAccess.write(
				bundleStack,
				inventory.set(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining)
		);
		return Optional.of(picked);
	}

	/**
	 * Inserts as much of {@code sourceStack} as possible, merging into matching inner stacks first.
	 * When every slot is full and the item type is new to the bundle, ejects slot 0 before inserting.
	 */
	public static InsertResult tryInsert(ItemStack bundleStack, ItemStack sourceStack) {
		if (sourceStack.isEmpty() || !BundleSlotPolicy.canInsert(sourceStack)) {
			return InsertResult.failed();
		}

		BundleInventory inventory = BundleInventoryAccess.read(bundleStack);
		int remaining = sourceStack.getCount();
		ItemStack ejected = ItemStack.EMPTY;

		for (int slot = 0; slot < BundleInventory.SLOT_COUNT && remaining > 0; slot++) {
			ItemStack existing = inventory.get(slot);
			if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, sourceStack)) {
				continue;
			}

			int space = existing.getMaxStackSize() - existing.getCount();
			if (space <= 0) {
				continue;
			}

			int added = Math.min(space, remaining);
			inventory = inventory.set(slot, existing.copyWithCount(existing.getCount() + added));
			remaining -= added;
		}

		while (remaining > 0) {
			OptionalInt emptySlot = inventory.firstEmpty();
			if (emptySlot.isEmpty()) {
				break;
			}

			int slot = emptySlot.getAsInt();
			int added = Math.min(remaining, sourceStack.getMaxStackSize());
			inventory = inventory.set(slot, sourceStack.copyWithCount(added));
			remaining -= added;
		}

		if (remaining > 0 && inventory.firstEmpty().isEmpty() && !hasMatchingStack(inventory, sourceStack)) {
			int targetSlot = 0;
			if (!inventory.get(targetSlot).isEmpty()) {
				ejected = inventory.get(targetSlot).copy();
			}

			int added = Math.min(remaining, sourceStack.getMaxStackSize());
			inventory = inventory.set(targetSlot, sourceStack.copyWithCount(added));
			remaining -= added;
		}

		int inserted = sourceStack.getCount() - remaining;
		if (inserted <= 0) {
			return InsertResult.failed();
		}

		BundleInventoryAccess.write(bundleStack, inventory);
		sourceStack.shrink(inserted);

		if (ejected.isEmpty()) {
			return InsertResult.success();
		}
		return InsertResult.successWithEject(ejected);
	}

	/**
	 * Inserts a single item from {@code sourceStack}, preferring merge into an existing matching stack.
	 */
	public static InsertResult tryInsertOne(ItemStack bundleStack, ItemStack sourceStack) {
		if (sourceStack.isEmpty()) {
			return InsertResult.failed();
		}

		if (sourceStack.getCount() == 1) {
			return tryInsert(bundleStack, sourceStack);
		}

		ItemStack single = sourceStack.copyWithCount(1);
		InsertResult result = tryInsert(bundleStack, single);
		if (result.inserted()) {
			sourceStack.shrink(1);
		}
		return result;
	}

	private static boolean hasMatchingStack(BundleInventory inventory, ItemStack sourceStack) {
		for (int slot = 0; slot < BundleInventory.SLOT_COUNT; slot++) {
			ItemStack existing = inventory.get(slot);
			if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, sourceStack)) {
				return true;
			}
		}
		return false;
	}
}
