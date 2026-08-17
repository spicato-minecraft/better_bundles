package better_bundles;

import better_bundles.inventory.BundleInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleInventoryTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void pickSlot_returnsLowestOccupiedIndex() {
		BundleInventory inventory = BundleInventory.empty()
			.set(2, new ItemStack(Items.DIAMOND))
			.set(0, new ItemStack(Items.IRON_INGOT));

		assertEquals(OptionalInt.of(0), inventory.pickSlot());
		assertEquals(OptionalInt.of(0), inventory.firstOccupied());
	}

	@Test
	void pickSlot_isEmptyWhenInventoryIsEmpty() {
		BundleInventory inventory = BundleInventory.empty();

		assertTrue(inventory.pickSlot().isEmpty());
		assertTrue(inventory.firstOccupied().isEmpty());
	}

	@Test
	void insertSlot_fillsLowestEmptySlotInOrder() {
		BundleInventory inventory = BundleInventory.empty();

		assertEquals(0, inventory.insertSlot());
		assertEquals(OptionalInt.of(0), inventory.firstEmpty());

		inventory = inventory.set(0, new ItemStack(Items.COBBLESTONE));
		assertEquals(1, inventory.insertSlot());
		assertEquals(OptionalInt.of(1), inventory.firstEmpty());

		inventory = inventory.set(1, new ItemStack(Items.STONE));
		assertEquals(2, inventory.insertSlot());

		inventory = inventory.set(2, new ItemStack(Items.DIRT));
		assertEquals(3, inventory.insertSlot());
	}

	@Test
	void insertSlot_wrapsToSlotZeroWhenFull() {
		BundleInventory inventory = BundleInventory.empty()
			.set(0, new ItemStack(Items.COBBLESTONE))
			.set(1, new ItemStack(Items.STONE))
			.set(2, new ItemStack(Items.DIRT))
			.set(3, new ItemStack(Items.GRAVEL));

		assertTrue(inventory.firstEmpty().isEmpty());
		assertEquals(0, inventory.insertSlot());
	}

	@Test
	void set_isImmutable() {
		BundleInventory original = BundleInventory.empty();
		BundleInventory updated = original.set(1, new ItemStack(Items.APPLE));

		assertTrue(original.get(1).isEmpty());
		assertEquals(Items.APPLE, updated.get(1).getItem());
		assertNotSame(original, updated);
	}

	@Test
	void get_returnsConfiguredSlotContents() {
		ItemStack stack = new ItemStack(Items.EMERALD, 16);
		BundleInventory inventory = BundleInventory.empty().set(3, stack);

		assertEquals(Items.EMERALD, inventory.get(3).getItem());
		assertEquals(16, inventory.get(3).getCount());
	}
}
