package better_bundles.client;

import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Read-only 2×2 bundle inner-grid overlay: slot backgrounds plus item icons.
 */
public final class BundleOverlayRenderer {

	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int DEFAULT_MENU_SLOT_SIZE = 18;

	private BundleOverlayRenderer() {
	}

	public static void render(
			GuiGraphics graphics,
			Font font,
			BundleHoverController.ActiveOverlay overlay
	) {
		BundleInventory inventory = BundleInventoryAccess.read(overlay.bundleStack());
		BundleOverlayPlacement.SlotRect[] rects = overlay.overlayRects();

		for (int index = 0; index < BundleInventory.SLOT_COUNT; index++) {
			BundleOverlayPlacement.SlotRect rect = rects[index];
			renderSlotBackground(graphics, rect);
			ItemStack stack = inventory.get(index);
			if (!stack.isEmpty()) {
				renderItem(graphics, font, stack, rect);
			}
		}
	}

	private static void renderSlotBackground(GuiGraphics graphics, BundleOverlayPlacement.SlotRect rect) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, rect.x(), rect.y(), rect.w(), rect.h());
	}

	private static void renderItem(
			GuiGraphics graphics,
			Font font,
			ItemStack stack,
			BundleOverlayPlacement.SlotRect rect
	) {
		int itemX = rect.x() + (rect.w() - 16) / 2;
		int itemY = rect.y() + (rect.h() - 16) / 2;
		int seed = rect.x() + rect.y();
		graphics.renderItem(stack, itemX, itemY, seed);
		graphics.renderItemDecorations(font, stack, itemX, itemY);
	}

	public static BundleOverlayPlacement.SlotRect boundingBox(BundleOverlayPlacement.SlotRect[] overlayRects) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (BundleOverlayPlacement.SlotRect rect : overlayRects) {
			minX = Math.min(minX, rect.x());
			minY = Math.min(minY, rect.y());
			maxX = Math.max(maxX, rect.right());
			maxY = Math.max(maxY, rect.bottom());
		}

		return new BundleOverlayPlacement.SlotRect(minX, minY, maxX - minX, maxY - minY);
	}

	/**
	 * Menu slot indices whose on-screen bounds intersect the overlay bounding box.
	 * Used by click-blocking logic in a later phase.
	 */
	public static Set<Integer> computeObscuredSlotIndices(
			AbstractContainerScreen<?> screen,
			BundleOverlayPlacement.SlotRect overlayBounds
	) {
		int originX = ((AbstractContainerScreenAccessor) screen).better_bundles$getLeftPos();
		int originY = ((AbstractContainerScreenAccessor) screen).better_bundles$getTopPos();
		Set<Integer> obscured = new HashSet<>();

		for (Slot slot : screen.getMenu().slots) {
			if (!slot.isActive()) {
				continue;
			}

			BundleOverlayPlacement.SlotRect slotRect = new BundleOverlayPlacement.SlotRect(
					originX + slot.x,
					originY + slot.y,
					DEFAULT_MENU_SLOT_SIZE,
					DEFAULT_MENU_SLOT_SIZE
			);

			if (intersects(slotRect, overlayBounds)) {
				obscured.add(slot.index);
			}
		}

		return obscured;
	}

	public static Set<Integer> computeObscuredSlotIndices(
			AbstractContainerScreen<?> screen,
			BundleOverlayPlacement.SlotRect[] overlayRects
	) {
		return computeObscuredSlotIndices(screen, boundingBox(overlayRects));
	}

	/**
	 * True when a screen-space point lies inside the overlay bounding box (inner slots only).
	 */
	public static boolean containsOverlayBounds(BundleOverlayPlacement.SlotRect[] overlayRects, double mouseX, double mouseY) {
		BundleOverlayPlacement.SlotRect bounds = boundingBox(overlayRects);
		return mouseX >= bounds.x()
				&& mouseX < bounds.right()
				&& mouseY >= bounds.y()
				&& mouseY < bounds.bottom();
	}

	private static boolean intersects(BundleOverlayPlacement.SlotRect a, BundleOverlayPlacement.SlotRect b) {
		return a.x() < b.right()
				&& a.right() > b.x()
				&& a.y() < b.bottom()
				&& a.bottom() > b.y();
	}
}
