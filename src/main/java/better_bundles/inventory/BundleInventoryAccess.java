package better_bundles.inventory;

import better_bundles.component.BetterBundleComponents;
import better_bundles.component.BetterBundleContentsComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Public read/write API for bundle inner inventory stored on {@link ItemStack}s.
 * <p>
 * First access on an unmigrated bundle copies vanilla {@link net.minecraft.world.item.component.BundleContents}
 * into the mod component and clears the vanilla component.
 */
public final class BundleInventoryAccess {

	private BundleInventoryAccess() {
	}

	public static boolean isBundleItem(ItemStack stack) {
		return BundleInventoryMigration.isBundleItem(stack);
	}

	public static BundleInventory read(ItemStack stack) {
		if (!isBundleItem(stack)) {
			return BundleInventory.empty();
		}

		if (!isMigrated(stack)) {
			BundleInventoryMigration.migrate(stack);
		}

		BetterBundleContentsComponent component = stack.get(BetterBundleComponents.BETTER_BUNDLE_CONTENTS);
		return component != null ? component.toInventory() : BundleInventory.empty();
	}

	public static void write(ItemStack stack, BundleInventory inventory) {
		if (!isBundleItem(stack)) {
			return;
		}

		stack.set(
			BetterBundleComponents.BETTER_BUNDLE_CONTENTS,
			BetterBundleContentsComponent.fromInventory(inventory, true)
		);
		stack.remove(DataComponents.BUNDLE_CONTENTS);
	}

	private static boolean isMigrated(ItemStack stack) {
		BetterBundleContentsComponent component = stack.get(BetterBundleComponents.BETTER_BUNDLE_CONTENTS);
		return component != null && component.migrated();
	}
}
