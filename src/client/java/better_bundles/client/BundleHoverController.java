package better_bundles.client;

import better_bundles.client.GridAnchorResolver.ResolvedAnchor;
import better_bundles.client.GridAnchorResolver.ResolvedPlacement;
import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.mixin.client.AbstractContainerScreenAccessor;
import better_bundles.network.BundleSlotActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Client-side hover state machine for the bundle overlay panel.
 */
public final class BundleHoverController {

	private static final BundleHoverController INSTANCE = new BundleHoverController();
	private static final int MENU_SLOT_HIT_SIZE = 18;

	private enum State {
		IDLE,
		PANEL_OPEN
	}

	private State state = State.IDLE;
	private ActiveOverlay active;

	private BundleHoverController() {
	}

	public static BundleHoverController getInstance() {
		return INSTANCE;
	}

	public record ActiveOverlay(
			Slot bundleSlot,
			ResolvedPlacement placement,
			ItemStack bundleStack,
			BundleOverlayPlacement.SlotRect[] overlayRects
	) {
		public OptionalInt hitInnerSlot(double mouseX, double mouseY) {
			for (int index = 0; index < overlayRects.length; index++) {
				if (containsPoint(overlayRects[index], mouseX, mouseY)) {
					return OptionalInt.of(index);
				}
			}
			return OptionalInt.empty();
		}

		public boolean containsPoint(double mouseX, double mouseY) {
			if (containsPoint(placement.anchor().slotRect(), mouseX, mouseY)) {
				return true;
			}

			for (BundleOverlayPlacement.SlotRect rect : overlayRects) {
				if (containsPoint(rect, mouseX, mouseY)) {
					return true;
				}
			}
			return false;
		}

		public boolean containsOverlayRect(double mouseX, double mouseY) {
			for (BundleOverlayPlacement.SlotRect rect : overlayRects) {
				if (containsPoint(rect, mouseX, mouseY)) {
					return true;
				}
			}
			return false;
		}

		private static boolean containsPoint(BundleOverlayPlacement.SlotRect rect, double mouseX, double mouseY) {
			return mouseX >= rect.x()
					&& mouseX < rect.right()
					&& mouseY >= rect.y()
					&& mouseY < rect.bottom();
		}
	}

	public Optional<ActiveOverlay> getActive() {
		return Optional.ofNullable(active);
	}

	public boolean isPointInOverlay(double mouseX, double mouseY) {
		ActiveOverlay current = active;
		return current != null && current.containsOverlayRect(mouseX, mouseY);
	}

	public void update(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		if (screen == null) {
			close();
			return;
		}

		if (state == State.PANEL_OPEN) {
			maintainPanelOpen(screen, mouseX, mouseY);
		} else {
			tryOpenPanel(screen, mouseX, mouseY);
		}
	}

	public void close() {
		state = State.IDLE;
		active = null;
	}

	public boolean handleOverlayMouseClick(AbstractContainerScreen<?> screen, MouseButtonEvent event, boolean doubled) {
		Optional<ActiveOverlay> current = getActive();
		if (current.isEmpty()) {
			return false;
		}

		ActiveOverlay overlay = current.get();
		double mouseX = event.x();
		double mouseY = event.y();

		if (hasShiftDown() && !overlay.containsOverlayRect(mouseX, mouseY)) {
			return handleShiftClickIntoBundle(screen, overlay, mouseX, mouseY);
		}

		OptionalInt innerSlot = overlay.hitInnerSlot(mouseX, mouseY);
		if (innerSlot.isPresent()) {
			if (hasShiftDown()) {
				BundleOverlayNetworking.sendAction(
						screen,
						overlay,
						innerSlot.getAsInt(),
						BundleSlotActionPayload.Action.SHIFT_MOVE
				);
			} else if (doubled) {
				sendResolvedAction(screen, overlay, innerSlot.getAsInt(), true);
			} else {
				sendResolvedAction(screen, overlay, innerSlot.getAsInt(), false);
			}
			return true;
		}

		return shouldBlockUnderlyingSlot(screen, overlay, mouseX, mouseY);
	}

	public boolean handleOverlayMouseRelease(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
		Optional<ActiveOverlay> current = getActive();
		if (current.isEmpty()) {
			return false;
		}

		ActiveOverlay overlay = current.get();
		double mouseX = event.x();
		double mouseY = event.y();

		OptionalInt innerSlot = overlay.hitInnerSlot(mouseX, mouseY);
		if (innerSlot.isPresent() && !screen.getMenu().getCarried().isEmpty()) {
			sendResolvedAction(screen, overlay, innerSlot.getAsInt(), false);
			return true;
		}

		return shouldBlockUnderlyingSlot(screen, overlay, mouseX, mouseY);
	}

	public boolean shouldBlockOverlayMouse(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
		Optional<ActiveOverlay> current = getActive();
		if (current.isEmpty()) {
			return false;
		}

		ActiveOverlay overlay = current.get();
		double mouseX = event.x();
		double mouseY = event.y();

		if (shouldBlockUnderlyingSlot(screen, overlay, mouseX, mouseY)) {
			return true;
		}

		// Block only inner overlay slots, not the full bounding box — clicks outside pass to EMI/vanilla.
		return overlay.hitInnerSlot(mouseX, mouseY).isPresent();
	}

	private void tryOpenPanel(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		findHoveredBundleSlot(screen, mouseX, mouseY).ifPresent(slot -> openPanel(screen, slot));
	}

	private void maintainPanelOpen(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		ActiveOverlay current = active;
		if (current == null) {
			close();
			return;
		}

		if (!isBundleStillInSlot(current)) {
			close();
			return;
		}

		ActiveOverlay refreshed = refreshOverlay(screen, current);
		if (refreshed == null) {
			close();
			return;
		}
		active = refreshed;

		if (!active.containsPoint(mouseX, mouseY)) {
			close();
			return;
		}

		findHoveredBundleSlot(screen, mouseX, mouseY).ifPresent(slot -> {
			if (slot != active.bundleSlot()) {
				openPanel(screen, slot);
			}
		});
	}

	private void openPanel(AbstractContainerScreen<?> screen, Slot slot) {
		Optional<ResolvedPlacement> placement = GridAnchorResolver.resolvePlacement(
				screen,
				slot,
				screen.width,
				screen.height
		);

		if (placement.isEmpty()) {
			close();
			return;
		}

		active = new ActiveOverlay(
				slot,
				placement.get(),
				slot.getItem().copy(),
				placement.get().overlayScreenRects()
		);
		state = State.PANEL_OPEN;
	}

	private ActiveOverlay refreshOverlay(AbstractContainerScreen<?> screen, ActiveOverlay overlay) {
		Optional<ResolvedPlacement> placement = GridAnchorResolver.resolvePlacement(
				screen,
				overlay.bundleSlot(),
				screen.width,
				screen.height
		);

		if (placement.isEmpty()) {
			return null;
		}

		return new ActiveOverlay(
				overlay.bundleSlot(),
				placement.get(),
				overlay.bundleSlot().getItem().copy(),
				placement.get().overlayScreenRects()
		);
	}

	private static boolean isBundleStillInSlot(ActiveOverlay overlay) {
		ItemStack stack = overlay.bundleSlot().getItem();
		return !stack.isEmpty() && BundleInventoryAccess.isBundleItem(stack);
	}

	private static Optional<Slot> findHoveredBundleSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		return findHoveredSlot(screen, mouseX, mouseY).filter(slot -> isSupportedBundleSlot(screen, slot));
	}

	private static boolean isSupportedBundleSlot(AbstractContainerScreen<?> screen, Slot slot) {
		return BundleInventoryAccess.isBundleItem(slot.getItem()) && GridAnchorResolver.resolve(screen, slot).isPresent();
	}

	private static Optional<Slot> findHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		int originX = ((AbstractContainerScreenAccessor) screen).better_bundles$getLeftPos();
		int originY = ((AbstractContainerScreenAccessor) screen).better_bundles$getTopPos();

		for (Slot slot : screen.getMenu().slots) {
			if (!slot.isActive()) {
				continue;
			}

			int hitW = MENU_SLOT_HIT_SIZE;
			int hitH = MENU_SLOT_HIT_SIZE;
			Optional<ResolvedAnchor> anchor = GridAnchorResolver.resolve(screen, slot);
			if (anchor.isPresent()) {
				hitW = anchor.get().slotPitchW();
				hitH = anchor.get().slotPitchH();
			}

			int slotX = originX + slot.x;
			int slotY = originY + slot.y;
			if (mouseX >= slotX && mouseX < slotX + hitW
					&& mouseY >= slotY && mouseY < slotY + hitH) {
				return Optional.of(slot);
			}
		}

		return Optional.empty();
	}

	private static boolean handleShiftClickIntoBundle(
			AbstractContainerScreen<?> screen,
			ActiveOverlay overlay,
			double mouseX,
			double mouseY
	) {
		Optional<Slot> hovered = findHoveredSlot(screen, mouseX, mouseY);
		if (hovered.isEmpty()) {
			return false;
		}

		Slot slot = hovered.get();
		if (slot == overlay.bundleSlot() || slot.getItem().isEmpty()) {
			return false;
		}

		if (overlay.containsOverlayRect(mouseX, mouseY)) {
			return false;
		}

		var inventory = BundleInventoryAccess.read(overlay.bundleStack());
		int targetInner = inventory.firstEmpty().orElse(-1);
		if (targetInner < 0) {
			return false;
		}

		BundleOverlayNetworking.sendAction(
				screen,
				overlay,
				targetInner,
				BundleSlotActionPayload.Action.SHIFT_MOVE,
				slot.index
		);
		return true;
	}

	private static boolean shouldBlockUnderlyingSlot(
			AbstractContainerScreen<?> screen,
			ActiveOverlay overlay,
			double mouseX,
			double mouseY
	) {
		if (overlay.containsOverlayRect(mouseX, mouseY)) {
			return true;
		}

		Optional<Slot> hovered = findHoveredSlot(screen, mouseX, mouseY);
		if (hovered.isEmpty()) {
			return false;
		}

		Slot slot = hovered.get();
		if (slot == overlay.bundleSlot()) {
			return false;
		}

		Set<Integer> obscured = BundleOverlayRenderer.computeObscuredSlotIndices(screen, overlay.overlayRects());
		return obscured.contains(slot.index);
	}

	private static void sendResolvedAction(
			AbstractContainerScreen<?> screen,
			ActiveOverlay overlay,
			int innerSlot,
			boolean doubled
	) {
		AbstractContainerMenu menu = screen.getMenu();
		ItemStack carried = menu.getCarried();
		ItemStack inner = BundleInventoryAccess.read(overlay.bundleStack()).get(innerSlot);

		BundleSlotActionPayload.Action action;
		if (doubled
				&& !carried.isEmpty()
				&& !inner.isEmpty()
				&& ItemStack.isSameItemSameComponents(carried, inner)) {
			action = BundleSlotActionPayload.Action.MERGE;
		} else {
			action = resolveAction(carried, inner);
		}

		if (action != null) {
			BundleOverlayNetworking.sendAction(screen, overlay, innerSlot, action);
		}
	}

	private static BundleSlotActionPayload.Action resolveAction(ItemStack carried, ItemStack inner) {
		if (carried.isEmpty() && !inner.isEmpty()) {
			return BundleSlotActionPayload.Action.PICKUP;
		}

		if (!carried.isEmpty() && inner.isEmpty()) {
			return BundleSlotActionPayload.Action.PLACE;
		}

		if (!carried.isEmpty() && !inner.isEmpty()) {
			return BundleSlotActionPayload.Action.SWAP;
		}

		return null;
	}

	private static boolean hasShiftDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.hasShiftDown();
	}
}
