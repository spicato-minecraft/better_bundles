package better_bundles.client;

import better_bundles.client.GridAnchorResolver.ResolvedPlacement;
import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.mixin.client.AbstractContainerScreenAccessor;
import better_bundles.network.BundleSlotActionPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Client-side hover state machine for the bundle overlay panel.
 */
public final class BundleHoverController {

	private static final Logger LOGGER = LogUtils.getLogger();
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

	public boolean handleOverlayMouseClick(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
		return handleOverlayMouse(screen, event, true);
	}

	public boolean shouldBlockOverlayMouse(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
		return handleOverlayMouse(screen, event, false);
	}

	private boolean handleOverlayMouse(AbstractContainerScreen<?> screen, MouseButtonEvent event, boolean stubOnInnerHit) {
		Optional<ActiveOverlay> current = getActive();
		if (current.isEmpty()) {
			return false;
		}

		ActiveOverlay overlay = current.get();
		double mouseX = event.x();
		double mouseY = event.y();

		OptionalInt innerSlot = overlay.hitInnerSlot(mouseX, mouseY);
		if (innerSlot.isPresent()) {
			if (stubOnInnerHit) {
				sendInnerSlotActionStub(screen, overlay, innerSlot.getAsInt(), event);
			}
			return true;
		}

		return shouldBlockUnderlyingSlot(screen, overlay, mouseX, mouseY);
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

		active = refreshStack(current);

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

	private ActiveOverlay refreshStack(ActiveOverlay overlay) {
		return new ActiveOverlay(
				overlay.bundleSlot(),
				overlay.placement(),
				overlay.bundleSlot().getItem().copy(),
				overlay.overlayRects()
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

			int slotX = originX + slot.x;
			int slotY = originY + slot.y;
			if (mouseX >= slotX && mouseX < slotX + MENU_SLOT_HIT_SIZE
					&& mouseY >= slotY && mouseY < slotY + MENU_SLOT_HIT_SIZE) {
				return Optional.of(slot);
			}
		}

		return Optional.empty();
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

	private static void sendInnerSlotActionStub(
			AbstractContainerScreen<?> screen,
			ActiveOverlay overlay,
			int innerSlot,
			MouseButtonEvent event
	) {
		BundleSlotActionPayload.Action action = stubAction(screen);
		BundleSlotActionPayload payload = new BundleSlotActionPayload(
				screen.getMenu().containerId,
				overlay.bundleSlot().index,
				innerSlot,
				action,
				0
		);

		// TODO Phase 5: ClientPlayNetworking.send(BundleSlotActionPayload.TYPE, payload)
		LOGGER.debug(
				"Stub C2S bundle slot action: syncId={}, containerSlot={}, innerSlot={}, action={}, button={}",
				payload.screenSyncId(),
				payload.containerSlotId(),
				payload.innerSlot(),
				payload.action(),
				event.button()
		);
	}

	private static BundleSlotActionPayload.Action stubAction(AbstractContainerScreen<?> screen) {
		return screen.getMenu().getCarried().isEmpty()
				? BundleSlotActionPayload.Action.PICKUP
				: BundleSlotActionPayload.Action.PLACE;
	}
}
