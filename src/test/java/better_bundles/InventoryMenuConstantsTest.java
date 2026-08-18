package better_bundles;

import net.minecraft.world.inventory.InventoryMenu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryMenuConstantsTest {

	@Test
	void inventoryMenuSlotRangesMatchExpectations() {
		assertEquals(9, InventoryMenu.INV_SLOT_START);
		assertEquals(36, InventoryMenu.INV_SLOT_END);
		assertEquals(36, InventoryMenu.USE_ROW_SLOT_START);
		assertEquals(45, InventoryMenu.USE_ROW_SLOT_END);
		assertEquals(45, InventoryMenu.SHIELD_SLOT);
	}
}
