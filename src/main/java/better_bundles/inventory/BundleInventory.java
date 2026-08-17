package better_bundles.inventory;

import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

/**
 * Immutable four-slot bundle storage (2×2 inner grid).
 * <p>
 * Vanilla right-click pick/insert order is mapped across slots 0 → 3:
 * pick removes from the lowest occupied index; insert fills the lowest empty index.
 * When every slot is full, insert wraps to slot 0 — the same position vanilla would
 * cycle to when replacing the front of a single logical stack.
 */
public record BundleInventory(ItemStack slot0, ItemStack slot1, ItemStack slot2, ItemStack slot3) {

	public static final int SLOT_COUNT = 4;

	public BundleInventory {
		slot0 = normalize(slot0);
		slot1 = normalize(slot1);
		slot2 = normalize(slot2);
		slot3 = normalize(slot3);
	}

	public static BundleInventory empty() {
		return new BundleInventory(
			ItemStack.EMPTY,
			ItemStack.EMPTY,
			ItemStack.EMPTY,
			ItemStack.EMPTY
		);
	}

	public ItemStack get(int index) {
		return switch (index) {
			case 0 -> slot0;
			case 1 -> slot1;
			case 2 -> slot2;
			case 3 -> slot3;
			default -> throw new IndexOutOfBoundsException("Bundle slot index out of bounds: " + index);
		};
	}

	public BundleInventory set(int index, ItemStack stack) {
		ItemStack normalized = normalize(stack);
		return switch (index) {
			case 0 -> new BundleInventory(normalized, slot1, slot2, slot3);
			case 1 -> new BundleInventory(slot0, normalized, slot2, slot3);
			case 2 -> new BundleInventory(slot0, slot1, normalized, slot3);
			case 3 -> new BundleInventory(slot0, slot1, slot2, normalized);
			default -> throw new IndexOutOfBoundsException("Bundle slot index out of bounds: " + index);
		};
	}

	public OptionalInt firstEmpty() {
		for (int index = 0; index < SLOT_COUNT; index++) {
			if (get(index).isEmpty()) {
				return OptionalInt.of(index);
			}
		}
		return OptionalInt.empty();
	}

	public OptionalInt firstOccupied() {
		for (int index = 0; index < SLOT_COUNT; index++) {
			if (!get(index).isEmpty()) {
				return OptionalInt.of(index);
			}
		}
		return OptionalInt.empty();
	}

	/**
	 * Slot index for vanilla right-click pick: lowest occupied slot (0 first).
	 */
	public OptionalInt pickSlot() {
		return firstOccupied();
	}

	/**
	 * Slot index for vanilla right-click insert: lowest empty slot (0 first).
	 * When full, wraps to slot 0 so callers can eject the front item before inserting.
	 */
	public int insertSlot() {
		return firstEmpty().orElse(0);
	}

	private static ItemStack normalize(ItemStack stack) {
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
	}
}
