package better_bundles;

import better_bundles.component.BetterBundleComponents;
import better_bundles.component.BetterBundleContentsComponent;
import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import com.mojang.serialization.JsonOps;
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
		BundleContents.Mutable mutable = new BundleContents.Mutable();
		mutable.tryInsert(new ItemStack(Items.IRON_INGOT, 3));
		mutable.tryInsert(new ItemStack(Items.GOLD_INGOT, 1));
		mutable.tryInsert(new ItemStack(Items.EMERALD, 2));
		mutable.tryInsert(new ItemStack(Items.DIAMOND, 1));
		mutable.tryInsert(new ItemStack(Items.COAL, 16));
		BundleContents vanillaContents = mutable.toImmutable();
		bundle.set(DataComponents.BUNDLE_CONTENTS, vanillaContents);

		BundleInventory inventory = BundleInventoryAccess.read(bundle);

		int slot = 0;
		for (ItemStack item : vanillaContents.itemCopies().toList()) {
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
		ItemStack bundle = new ItemStack(Items.DYED_BUNDLE.blue());
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

	@Test
	void itemStackCopy_preservesInnerInventory() {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		BundleInventory original = BundleInventory.empty()
				.set(0, new ItemStack(Items.DIRT, 7))
				.set(3, new ItemStack(Items.STONE, 1));
		BundleInventoryAccess.write(bundle, original);

		BundleInventory restored = BundleInventoryAccess.read(bundle.copy());

		assertEquals(Items.DIRT, restored.get(0).getItem());
		assertEquals(7, restored.get(0).getCount());
		assertEquals(Items.STONE, restored.get(3).getItem());
	}

	@Test
	void componentCodec_roundTripPreservesInventory() {
		BetterBundleContentsComponent original = BetterBundleContentsComponent.fromInventory(
				BundleInventory.empty()
						.set(0, new ItemStack(Items.IRON_INGOT, 2))
						.set(1, new ItemStack(Items.GOLD_INGOT, 1))
						.set(2, new ItemStack(Items.COAL, 1))
						.set(3, new ItemStack(Items.DIRT, 1)),
				true
		);

		BetterBundleContentsComponent decoded = BetterBundleContentsComponent.CODEC
				.decode(JsonOps.INSTANCE, BetterBundleContentsComponent.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow())
				.getOrThrow()
				.getFirst();

		assertEquals(original.toInventory().get(0).getItem(), decoded.toInventory().get(0).getItem());
		assertEquals(original.toInventory().get(0).getCount(), decoded.toInventory().get(0).getCount());
		assertEquals(original.toInventory().get(1).getItem(), decoded.toInventory().get(1).getItem());
		assertEquals(original.toInventory().get(2).getItem(), decoded.toInventory().get(2).getItem());
		assertEquals(original.toInventory().get(3).getItem(), decoded.toInventory().get(3).getItem());
		assertTrue(decoded.migrated());
	}

	@Test
	void componentCodec_encodesPartialOccupancy() {
		BetterBundleContentsComponent original = BetterBundleContentsComponent.fromInventory(
				BundleInventory.empty().set(0, new ItemStack(Items.SPRUCE_PLANKS, 1)),
				true
		);

		BetterBundleContentsComponent.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();

		BetterBundleContentsComponent decoded = BetterBundleContentsComponent.CODEC
				.decode(JsonOps.INSTANCE, BetterBundleContentsComponent.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow())
				.getOrThrow()
				.getFirst();

		assertEquals(Items.SPRUCE_PLANKS, decoded.toInventory().get(0).getItem());
		assertTrue(decoded.toInventory().get(1).isEmpty());
		assertTrue(decoded.toInventory().get(2).isEmpty());
		assertTrue(decoded.toInventory().get(3).isEmpty());
	}
}
