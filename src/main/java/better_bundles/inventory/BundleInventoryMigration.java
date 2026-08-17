package better_bundles.inventory;

import better_bundles.component.BetterBundleComponents;
import better_bundles.component.BetterBundleContentsComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

/**
 * One-time migration from vanilla {@link BundleContents} into {@link BundleInventory}.
 */
public final class BundleInventoryMigration {

	private BundleInventoryMigration() {
	}

	public static boolean isBundleItem(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof BundleItem;
	}

	public static BundleInventory fromVanillaContents(BundleContents contents) {
		if (contents == null || contents.isEmpty()) {
			return BundleInventory.empty();
		}

		BundleInventory inventory = BundleInventory.empty();
		int slot = 0;

		for (ItemStack item : contents.itemsCopy()) {
			if (slot >= BundleInventory.SLOT_COUNT) {
				break;
			}

			inventory = inventory.set(slot, item.copy());
			slot++;
		}

		return inventory;
	}

	public static void migrate(ItemStack stack) {
		if (!isBundleItem(stack)) {
			return;
		}

		BundleInventory inventory = fromVanillaContents(
			stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
		);
		stack.set(
			BetterBundleComponents.BETTER_BUNDLE_CONTENTS,
			BetterBundleContentsComponent.fromInventory(inventory, true)
		);
		stack.remove(DataComponents.BUNDLE_CONTENTS);
	}
}
