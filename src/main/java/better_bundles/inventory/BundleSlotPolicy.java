package better_bundles.inventory;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Rules for which items may be stored inside a bundle inner slot.
 */
public final class BundleSlotPolicy {

	private BundleSlotPolicy() {
	}

	public static boolean canInsert(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		if (stack.is(Items.BUNDLE)) {
			return false;
		}

		return !isShulkerBox(stack);
	}

	private static boolean isShulkerBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem
			&& blockItem.getBlock() instanceof ShulkerBoxBlock;
	}
}
