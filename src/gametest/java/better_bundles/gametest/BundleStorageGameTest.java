package better_bundles.gametest;

import better_bundles.inventory.BundleInteractionLogic;
import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

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

	@GameTest(maxTicks = 100)
	public void bundleInChestSlot_preservesInnerInventory(GameTestHelper context) {
		BlockPos chestPos = new BlockPos(1, 2, 1);
		context.setBlock(chestPos, Blocks.CHEST);

		ChestBlockEntity chest = context.getBlockEntity(chestPos, ChestBlockEntity.class);
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventory seeded = BundleInventory.empty()
				.set(0, new ItemStack(Items.DIRT, 5))
				.set(2, new ItemStack(Items.COBBLESTONE, 3));
		BundleInventoryAccess.write(bundle, seeded);

		chest.setItem(0, bundle);
		BundleInventory fromChest = BundleInventoryAccess.read(chest.getItem(0));

		context.assertValueEqual(Items.DIRT, fromChest.get(0).getItem(), "Chest bundle slot 0 should hold dirt");
		context.assertValueEqual(5, fromChest.get(0).getCount(), "Chest bundle dirt count should persist");
		context.assertValueEqual(Items.COBBLESTONE, fromChest.get(2).getItem(), "Chest bundle slot 2 should hold cobblestone");
		context.assertValueEqual(3, fromChest.get(2).getCount(), "Chest bundle cobblestone count should persist");

		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bundleContents_persistOnDroppedItem(GameTestHelper context) {
		ServerLevel level = context.getLevel();
		BlockPos pos = new BlockPos(1, 2, 1);

		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventory seeded = BundleInventory.empty().set(1, new ItemStack(Items.EMERALD, 4));
		BundleInventoryAccess.write(bundle, seeded);

		ItemEntity entity = new ItemEntity(
				level,
				pos.getX() + 0.5,
				pos.getY(),
				pos.getZ() + 0.5,
				bundle
		);
		level.addFreshEntity(entity);

		BundleInventory dropped = BundleInventoryAccess.read(entity.getItem());
		context.assertValueEqual(Items.EMERALD, dropped.get(1).getItem(), "Dropped bundle should keep emerald in slot 1");
		context.assertValueEqual(4, dropped.get(1).getCount(), "Dropped bundle emerald count should persist");

		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bundleContents_survivePlayerDeathDrop(GameTestHelper context) {
		ServerLevel level = context.getLevel();
		BlockPos pos = new BlockPos(1, 2, 1);

		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventory seeded = BundleInventory.empty().set(0, new ItemStack(Items.DIAMOND, 2));
		BundleInventoryAccess.write(bundle, seeded);

		ItemStack deathDrop = bundle.copy();
		BundleInventory removed = BundleInventoryAccess.read(deathDrop);
		context.assertValueEqual(Items.DIAMOND, removed.get(0).getItem(), "Inventory removal should keep inner diamond stack");
		context.assertValueEqual(2, removed.get(0).getCount(), "Inventory removal should keep inner diamond count");

		ItemEntity entity = new ItemEntity(
				level,
				pos.getX() + 0.5,
				pos.getY(),
				pos.getZ() + 0.5,
				deathDrop
		);
		level.addFreshEntity(entity);

		BundleInventory onGround = BundleInventoryAccess.read(entity.getItem());
		context.assertValueEqual(Items.DIAMOND, onGround.get(0).getItem(), "Death drop item entity should keep inner diamond stack");
		context.assertValueEqual(2, onGround.get(0).getCount(), "Death drop item entity should keep inner diamond count");

		context.succeed();
	}
}
