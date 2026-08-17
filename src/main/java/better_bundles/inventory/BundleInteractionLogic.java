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

		ItemStack picked = slotStack.copyWithCount(1);
		ItemStack remaining = slotStack.copyWithCount(slotStack.getCount() - 1);
		BundleInventoryAccess.write(
			bundleStack,
			inventory.set(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining)
		);
		return Optional.of(picked);
	}

	/**
	 * Inserts one item from {@code sourceStack} into the lowest empty inner slot.
	 * When every slot is full, ejects the occupant of slot 0 before inserting.
	 *
	 * @param sourceStack stack to take one item from; count is decremented on success
	 */
	public static InsertResult tryInsertOne(ItemStack bundleStack, ItemStack sourceStack) {
		if (sourceStack.isEmpty()) {
			return InsertResult.failed();
		}

		if (!BundleSlotPolicy.canInsert(sourceStack)) {
			return InsertResult.failed();
		}

		BundleInventory inventory = BundleInventoryAccess.read(bundleStack);
		int targetSlot = inventory.insertSlot();
		ItemStack ejected = ItemStack.EMPTY;

		if (!inventory.get(targetSlot).isEmpty()) {
			ejected = inventory.get(targetSlot).copy();
			inventory = inventory.set(targetSlot, ItemStack.EMPTY);
		}

		inventory = inventory.set(targetSlot, new ItemStack(sourceStack.getItem(), 1));
		BundleInventoryAccess.write(bundleStack, inventory);
		sourceStack.shrink(1);

		if (ejected.isEmpty()) {
			return InsertResult.success();
		}
		return InsertResult.successWithEject(ejected);
	}
}
