package better_bundles;

import better_bundles.inventory.BundleInteractionLogic;
import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleInteractionLogicTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void tryInsert_mergesIntoExistingStackBeforeUsingNewSlot() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventoryAccess.write(
				bundle,
				BundleInventory.empty().set(0, new ItemStack(Items.DIRT, 1))
		);

		ItemStack source = new ItemStack(Items.DIRT, 5);
		BundleInteractionLogic.InsertResult result = BundleInteractionLogic.tryInsert(bundle, source);

		assertTrue(result.inserted());
		assertTrue(result.ejected().isEmpty());
		assertEquals(0, source.getCount(), "All dirt should move into the bundle");

		BundleInventory inventory = BundleInventoryAccess.read(bundle);
		assertEquals(Items.DIRT, inventory.get(0).getItem());
		assertEquals(6, inventory.get(0).getCount());
		assertTrue(inventory.get(1).isEmpty());
	}

	@Test
	void tryInsertOne_mergesSingleItemIntoExistingStack() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventoryAccess.write(
				bundle,
				BundleInventory.empty().set(0, new ItemStack(Items.DIRT, 2))
		);

		ItemStack source = new ItemStack(Items.DIRT, 4);
		BundleInteractionLogic.InsertResult result = BundleInteractionLogic.tryInsertOne(bundle, source);

		assertTrue(result.inserted());
		assertEquals(3, source.getCount());
		assertEquals(3, BundleInventoryAccess.read(bundle).get(0).getCount());
	}

	@Test
	void tryPickStack_removesEntireOccupiedSlot() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventoryAccess.write(
				bundle,
				BundleInventory.empty().set(0, new ItemStack(Items.DIRT, 5))
		);

		Optional<ItemStack> picked = BundleInteractionLogic.tryPickStack(bundle);

		assertTrue(picked.isPresent());
		assertEquals(5, picked.get().getCount());
		assertTrue(BundleInventoryAccess.read(bundle).get(0).isEmpty());
	}

	@Test
	void tryPickOne_removesSingleItem() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventoryAccess.write(
				bundle,
				BundleInventory.empty().set(0, new ItemStack(Items.DIRT, 5))
		);

		Optional<ItemStack> picked = BundleInteractionLogic.tryPickOne(bundle);

		assertTrue(picked.isPresent());
		assertEquals(1, picked.get().getCount());
		assertEquals(4, BundleInventoryAccess.read(bundle).get(0).getCount());
	}

	@Test
	void tryInsert_fillsUpToMaxStackSizeOnEmptyBundle() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		ItemStack source = new ItemStack(Items.DIRT, 10);

		BundleInteractionLogic.InsertResult result = BundleInteractionLogic.tryInsert(bundle, source);

		assertTrue(result.inserted());
		assertEquals(0, source.getCount());
		assertEquals(64, BundleInventoryAccess.read(bundle).get(0).getMaxStackSize());
		assertEquals(10, BundleInventoryAccess.read(bundle).get(0).getCount());
	}
}
