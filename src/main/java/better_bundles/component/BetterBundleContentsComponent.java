package better_bundles.component;

import better_bundles.inventory.BundleInventory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * Persistent four-slot bundle storage plus migration flag, stored on bundle {@link ItemStack}s.
 */
public record BetterBundleContentsComponent(
	ItemStack slot0,
	ItemStack slot1,
	ItemStack slot2,
	ItemStack slot3,
	boolean migrated
) {

	private static final Codec<ItemStack> SLOT_CODEC = ItemStack.OPTIONAL_CODEC;

	public static final Codec<BetterBundleContentsComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		SLOT_CODEC.fieldOf("slot0").forGetter(BetterBundleContentsComponent::slot0),
		SLOT_CODEC.fieldOf("slot1").forGetter(BetterBundleContentsComponent::slot1),
		SLOT_CODEC.fieldOf("slot2").forGetter(BetterBundleContentsComponent::slot2),
		SLOT_CODEC.fieldOf("slot3").forGetter(BetterBundleContentsComponent::slot3),
		Codec.BOOL.fieldOf("migrated").forGetter(BetterBundleContentsComponent::migrated)
	).apply(instance, BetterBundleContentsComponent::new));

	public BetterBundleContentsComponent {
		slot0 = normalizeSlot(slot0);
		slot1 = normalizeSlot(slot1);
		slot2 = normalizeSlot(slot2);
		slot3 = normalizeSlot(slot3);
	}

	public BundleInventory toInventory() {
		return new BundleInventory(slot0, slot1, slot2, slot3);
	}

	public static BetterBundleContentsComponent fromInventory(BundleInventory inventory, boolean migrated) {
		return new BetterBundleContentsComponent(
			inventory.slot0(),
			inventory.slot1(),
			inventory.slot2(),
			inventory.slot3(),
			migrated
		);
	}

	private static ItemStack normalizeSlot(ItemStack stack) {
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
	}
}
