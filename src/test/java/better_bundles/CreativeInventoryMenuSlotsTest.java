package better_bundles;

import better_bundles.inventory.CreativeInventoryMenuSlots;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CreativeInventoryMenuSlotsTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void mapsCreativeSearchHotbarOntoInventoryMenuHotbar() {
		Inventory inventory = new Inventory(mock(Player.class), new EntityEquipment());
		Slot lastHotbar = new Slot(inventory, 8, 0, 0);
		lastHotbar.index = 53;

		assertEquals(
				InventoryMenu.USE_ROW_SLOT_START + 8,
				CreativeInventoryMenuSlots.toInventoryMenuIndex(lastHotbar).orElseThrow()
		);
	}

	@Test
	void mapsCreativeInventoryTabMainSlotsOneToOne() {
		Inventory inventory = new Inventory(mock(Player.class), new EntityEquipment());
		Slot main = new Slot(inventory, 20, 0, 0);
		main.index = 20;

		assertEquals(20, CreativeInventoryMenuSlots.toInventoryMenuIndex(main).orElseThrow());
	}

	@Test
	void mapsCreativeInventoryTabHotbarSlotWrapper() {
		Inventory inventory = new Inventory(mock(Player.class), new EntityEquipment());
		Slot hotbar = new Slot(inventory, 36, 0, 0);
		hotbar.index = 36;

		assertEquals(36, CreativeInventoryMenuSlots.toInventoryMenuIndex(hotbar).orElseThrow());
	}

	@Test
	void rejectsCreativeCatalogSlots() {
		SimpleContainer catalog = new SimpleContainer(45);
		catalog.setItem(0, new ItemStack(Items.BUNDLE));
		Slot catalogSlot = new Slot(catalog, 0, 0, 0);
		catalogSlot.index = 0;

		assertTrue(CreativeInventoryMenuSlots.toInventoryMenuIndex(catalogSlot).isEmpty());
	}

	@Test
	void rejectsArmorLikeCreativeSlots() {
		Inventory inventory = new Inventory(mock(Player.class), new EntityEquipment());
		Slot armor = new Slot(inventory, 5, 0, 0);
		armor.index = 5;

		assertTrue(CreativeInventoryMenuSlots.toInventoryMenuIndex(armor).isEmpty());
	}
}
