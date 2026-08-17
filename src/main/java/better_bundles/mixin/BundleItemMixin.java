package better_bundles.mixin;

import better_bundles.inventory.BundleInteractionLogic;
import better_bundles.inventory.BundleInventoryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.Level;

import java.util.Optional;

@Mixin(BundleItem.class)
public abstract class BundleItemMixin {

	@Shadow
	private static void playInsertSound(Entity entity) {
	}

	@Shadow
	private static void playInsertFailSound(Entity entity) {
	}

	@Shadow
	private static void playRemoveOneSound(Entity entity) {
	}

	@Shadow
	private void broadcastChangesOnContainerMenu(Player player) {
	}

	@Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overrideStackedOnOther(
		ItemStack bundleStack,
		Slot slot,
		ClickAction action,
		Player player,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!BundleInventoryAccess.isBundleItem(bundleStack)) {
			return;
		}

		ItemStack slotStack = slot.getItem();

		if (action == ClickAction.PRIMARY && !slotStack.isEmpty()) {
			BundleInteractionLogic.InsertResult result = BundleInteractionLogic.tryInsertOne(bundleStack, slotStack);
			if (result.inserted()) {
				if (!result.ejected().isEmpty()) {
					ItemStack overflow = slot.safeInsert(result.ejected());
					if (!overflow.isEmpty()) {
						BundleInteractionLogic.tryInsertOne(bundleStack, overflow);
					}
				}
				playInsertSound(player);
			} else {
				playInsertFailSound(player);
			}
			broadcastChangesOnContainerMenu(player);
			cir.setReturnValue(true);
			return;
		}

		if (action == ClickAction.SECONDARY && slotStack.isEmpty()) {
			Optional<ItemStack> picked = BundleInteractionLogic.tryPickOne(bundleStack);
			if (picked.isPresent()) {
				ItemStack overflow = slot.safeInsert(picked.get());
				if (!overflow.isEmpty()) {
					BundleInteractionLogic.tryInsertOne(bundleStack, overflow);
				}
				playRemoveOneSound(player);
			}
			broadcastChangesOnContainerMenu(player);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
	private void better_bundles$overrideOtherStackedOnMe(
		ItemStack bundleStack,
		ItemStack otherStack,
		Slot slot,
		ClickAction action,
		Player player,
		SlotAccess cursorAccess,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!BundleInventoryAccess.isBundleItem(bundleStack)) {
			return;
		}

		if (action == ClickAction.PRIMARY && otherStack.isEmpty()) {
			cir.setReturnValue(false);
			cir.cancel();
			return;
		}

		if (action == ClickAction.PRIMARY && !otherStack.isEmpty()) {
			if (!slot.allowModification(player)) {
				playInsertFailSound(player);
				broadcastChangesOnContainerMenu(player);
				cir.setReturnValue(true);
				return;
			}

			BundleInteractionLogic.InsertResult result = BundleInteractionLogic.tryInsertOne(bundleStack, otherStack);
			if (result.inserted()) {
				if (!result.ejected().isEmpty()) {
					if (otherStack.isEmpty()) {
						cursorAccess.set(result.ejected());
					} else if (!player.getInventory().add(result.ejected())) {
						player.drop(result.ejected(), false);
					}
				}
				playInsertSound(player);
			} else {
				playInsertFailSound(player);
			}
			broadcastChangesOnContainerMenu(player);
			cir.setReturnValue(true);
			return;
		}

		if (action == ClickAction.SECONDARY && otherStack.isEmpty()) {
			if (slot.allowModification(player)) {
				Optional<ItemStack> picked = BundleInteractionLogic.tryPickOne(bundleStack);
				if (picked.isPresent()) {
					playRemoveOneSound(player);
					cursorAccess.set(picked.get());
				}
			}
			broadcastChangesOnContainerMenu(player);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "dropContent(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
	private void better_bundles$dropContent(
		ItemStack bundleStack,
		Player player,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!BundleInventoryAccess.isBundleItem(bundleStack)) {
			return;
		}

		Optional<ItemStack> picked = BundleInteractionLogic.tryPickOne(bundleStack);
		if (picked.isEmpty()) {
			cir.setReturnValue(false);
			cir.cancel();
			return;
		}

		playRemoveOneSound(player);
		player.drop(picked.get(), true);
		cir.setReturnValue(true);
		cir.cancel();
	}
}
