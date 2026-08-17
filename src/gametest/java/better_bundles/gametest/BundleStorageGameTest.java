package better_bundles.gametest;

import better_bundles.inventory.BundleInteractionLogic;
import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BundleStorageGameTest {

	@GameTest(maxTicks = 100)
	public void rightClickInsert_fillsSlotZeroFirst(GameTestHelper context) {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		ItemStack dirt = new ItemStack(Items.DIRT);
		ItemStack stone = new ItemStack(Items.COBBLESTONE);

		BundleInteractionLogic.InsertResult first = BundleInteractionLogic.tryInsertOne(bundle, dirt);
		context.assertTrue(first.inserted(), "First insert should succeed");
		context.assertTrue(first.ejected().isEmpty(), "Empty bundle should not eject");

		BundleInteractionLogic.InsertResult second = BundleInteractionLogic.tryInsertOne(bundle, stone);
		context.assertTrue(second.inserted(), "Second insert should succeed");

		BundleInventory inventory = BundleInventoryAccess.read(bundle);
		context.assertValueEqual(Items.DIRT, inventory.get(0).getItem(), "Slot 0 should hold first inserted item");
		context.assertValueEqual(Items.COBBLESTONE, inventory.get(1).getItem(), "Slot 1 should hold second inserted item");
		context.assertTrue(inventory.get(2).isEmpty(), "Slot 2 should remain empty");
		context.assertTrue(inventory.get(3).isEmpty(), "Slot 3 should remain empty");

		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void rightClickPick_removesSlotZeroFirst(GameTestHelper context) {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventory seeded = BundleInventory.empty()
			.set(0, new ItemStack(Items.DIRT, 3))
			.set(2, new ItemStack(Items.COBBLESTONE));
		BundleInventoryAccess.write(bundle, seeded);

		ItemStack firstPick = BundleInteractionLogic.tryPickOne(bundle).orElse(ItemStack.EMPTY);
		context.assertValueEqual(Items.DIRT, firstPick.getItem(), "First pick should come from slot 0");
		context.assertValueEqual(1, firstPick.getCount(), "Pick should remove one item at a time");

		BundleInventory afterFirstPick = BundleInventoryAccess.read(bundle);
		context.assertValueEqual(Items.DIRT, afterFirstPick.get(0).getItem(), "Slot 0 should still hold remaining dirt");
		context.assertValueEqual(2, afterFirstPick.get(0).getCount(), "Slot 0 dirt count should decrease by one");
		context.assertValueEqual(Items.COBBLESTONE, afterFirstPick.get(2).getItem(), "Slot 2 should be untouched after first pick");

		for (int i = 0; i < 2; i++) {
			BundleInteractionLogic.tryPickOne(bundle);
		}

		ItemStack thirdPick = BundleInteractionLogic.tryPickOne(bundle).orElse(ItemStack.EMPTY);
		context.assertValueEqual(Items.COBBLESTONE, thirdPick.getItem(), "After slot 0 is empty, pick should come from slot 2");
		context.assertTrue(BundleInventoryAccess.read(bundle).get(0).isEmpty(), "Slot 0 should be empty");
		context.assertTrue(BundleInventoryAccess.read(bundle).get(2).isEmpty(), "Slot 2 should be empty after pick");

		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bundleInBundle_isRejected(GameTestHelper context) {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		ItemStack nestedBundle = new ItemStack(Items.BUNDLE);

		BundleInteractionLogic.InsertResult result = BundleInteractionLogic.tryInsertOne(bundle, nestedBundle);
		context.assertFalse(result.inserted(), "Nested bundle insert should be rejected");
		context.assertTrue(result.ejected().isEmpty(), "Rejected insert should not eject");
		context.assertValueEqual(1, nestedBundle.getCount(), "Rejected source stack should be unchanged");

		BundleInventory inventory = BundleInventoryAccess.read(bundle);
		context.assertTrue(inventory.firstOccupied().isEmpty(), "Bundle inner inventory should remain empty");

		context.succeed();
	}
}
