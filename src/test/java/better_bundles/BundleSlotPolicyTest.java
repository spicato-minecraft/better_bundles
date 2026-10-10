package better_bundles;

import better_bundles.inventory.BundleSlotPolicy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleSlotPolicyTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void bundleInBundle_isBlocked() {
		assertFalse(BundleSlotPolicy.canInsert(new ItemStack(Items.BUNDLE)));
	}

	@Test
	void shulkerInBundle_isBlocked() {
		assertFalse(BundleSlotPolicy.canInsert(new ItemStack(Items.SHULKER_BOX)));
		assertFalse(BundleSlotPolicy.canInsert(new ItemStack(Items.DYED_SHULKER_BOX.red())));
	}

	@Test
	void regularItems_areAllowed() {
		assertTrue(BundleSlotPolicy.canInsert(new ItemStack(Items.IRON_INGOT)));
		assertTrue(BundleSlotPolicy.canInsert(new ItemStack(Items.DIAMOND_PICKAXE)));
	}

	@Test
	void emptyStack_isRejected() {
		assertFalse(BundleSlotPolicy.canInsert(ItemStack.EMPTY));
	}
}
