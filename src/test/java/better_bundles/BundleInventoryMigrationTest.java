package better_bundles;

import better_bundles.component.BetterBundleComponents;
import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleInventoryMigrationTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void migrate_copiesVanillaContentsIntoSlotsInOrder() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleContents.Mutable mutable = new BundleContents.Mutable(BundleContents.EMPTY);
		mutable.tryInsert(new ItemStack(Items.IRON_INGOT, 3));
		mutable.tryInsert(new ItemStack(Items.GOLD_INGOT, 1));
		mutable.tryInsert(new ItemStack(Items.EMERALD, 2));
		mutable.tryInsert(new ItemStack(Items.DIAMOND, 1));
		mutable.tryInsert(new ItemStack(Items.COAL, 16));
		BundleContents vanillaContents = mutable.toImmutable();
		bundle.set(DataComponents.BUNDLE_CONTENTS, vanillaContents);

		BundleInventory inventory = BundleInventoryAccess.read(bundle);

		int slot = 0;
		for (ItemStack item : vanillaContents.itemsCopy()) {
			if (slot >= BundleInventory.SLOT_COUNT) {
				break;
			}

			assertEquals(item.getItem(), inventory.get(slot).getItem());
			assertEquals(item.getCount(), inventory.get(slot).getCount());
			slot++;
		}

		assertFalse(bundle.has(DataComponents.BUNDLE_CONTENTS));
		assertTrue(bundle.get(BetterBundleComponents.BETTER_BUNDLE_CONTENTS).migrated());
	}

	@Test
	void migrate_emptyBundle_setsMigratedComponentAndClearsVanilla() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);

		BundleInventory inventory = BundleInventoryAccess.read(bundle);

		assertTrue(inventory.get(0).isEmpty());
		assertNotNull(bundle.get(BetterBundleComponents.BETTER_BUNDLE_CONTENTS));
		assertTrue(bundle.get(BetterBundleComponents.BETTER_BUNDLE_CONTENTS).migrated());
		assertFalse(bundle.has(DataComponents.BUNDLE_CONTENTS));
	}

	@Test
	void writeAndRead_roundTripPreservesInventory() {
		ItemStack bundle = new ItemStack(Items.BLUE_BUNDLE);
		BundleInventory original = BundleInventory.empty()
			.set(0, new ItemStack(Items.APPLE, 4))
			.set(2, new ItemStack(Items.BREAD, 1));

		BundleInventoryAccess.write(bundle, original);
		BundleInventory restored = BundleInventoryAccess.read(bundle);

		assertEquals(Items.APPLE, restored.get(0).getItem());
		assertEquals(4, restored.get(0).getCount());
		assertTrue(restored.get(1).isEmpty());
		assertEquals(Items.BREAD, restored.get(2).getItem());
		assertTrue(restored.get(3).isEmpty());
		assertFalse(bundle.has(DataComponents.BUNDLE_CONTENTS));
	}
}
