package better_bundles.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

import java.util.OptionalInt;

/**
 * Maps creative-screen player storage slots onto survival {@link InventoryMenu}
 * indices. Catalog/search slots are not mapped and must not open the overlay.
 */
public final class CreativeInventoryMenuSlots {

	private CreativeInventoryMenuSlots() {
	}

	public static OptionalInt toInventoryMenuIndex(Slot slot) {
		if (slot == null || !(slot.container instanceof Inventory)) {
			return OptionalInt.empty();
		}

		int containerSlot = slot.getContainerSlot();
		int menuIndex = slot.index;

		if (menuIndex >= 45 && menuIndex < 54 && containerSlot >= 0 && containerSlot < 9) {
			return OptionalInt.of(InventoryMenu.USE_ROW_SLOT_START + containerSlot);
		}

		boolean playerStorage = (containerSlot >= InventoryMenu.INV_SLOT_START && containerSlot < InventoryMenu.INV_SLOT_END)
				|| (containerSlot >= InventoryMenu.USE_ROW_SLOT_START && containerSlot < InventoryMenu.USE_ROW_SLOT_END);
		if (menuIndex == containerSlot && playerStorage) {
			return OptionalInt.of(containerSlot);
		}

		return OptionalInt.empty();
	}
}
