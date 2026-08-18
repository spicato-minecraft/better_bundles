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
 * Bundle hover overlay: leather panel chrome plus inner-slot item icons.
 */
public final class BundleOverlayRenderer {

	private static final int DEFAULT_MENU_SLOT_SIZE = 18;
	private static final int SLOT_HIGHLIGHT_INSET = 4;
	private static final int SLOT_HIGHLIGHT_SIZE = 24;
	private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace(
			"container/slot_highlight_back"
	);
	private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace(
			"container/slot_highlight_front"
	);
	/** Fine-tune 16×16 item icons within 18×18 overlay slots. */
	private static final int ITEM_RENDER_OFFSET_X = -1;
	private static final int ITEM_RENDER_OFFSET_Y = -1;

	private BundleOverlayRenderer() {
	}

	public static void renderOverlay(
			GuiGraphics graphics,
			Font font,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop,
			double mouseX,
			double mouseY
	) {
		BundleOverlayPanelChrome.renderStorageBackground(graphics, overlay, guiLeft, guiTop);
		// Anchor patch overlaps the top inner slots in DL layout; draw highlights and items after it.
		BundleOverlayPanelChrome.renderAnchorPatch(graphics, overlay, guiLeft, guiTop);
		renderInnerSlotContent(graphics, font, overlay, guiLeft, guiTop, mouseX, mouseY);
		BundleOverlayPanelChrome.renderBundleItem(graphics, font, overlay, guiLeft, guiTop);
	}

	private static void renderInnerSlotContent(
			GuiGraphics graphics,
			Font font,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop,
			double mouseX,
			double mouseY
	) {
		renderHoveredInnerSlotHighlight(graphics, overlay, guiLeft, guiTop, mouseX, mouseY, SLOT_HIGHLIGHT_BACK_SPRITE);
		renderInnerItems(graphics, font, overlay, guiLeft, guiTop);
		renderHoveredInnerSlotHighlight(graphics, overlay, guiLeft, guiTop, mouseX, mouseY, SLOT_HIGHLIGHT_FRONT_SPRITE);
	}

	private static void renderHoveredInnerSlotHighlight(
			GuiGraphics graphics,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop,
			double mouseX,
			double mouseY,
			Identifier sprite
	) {
		overlay.hitInnerSlot(mouseX, mouseY).ifPresent(index -> {
			BundleOverlayPlacement.SlotRect local = toGuiLocal(overlay.overlayRects()[index], guiLeft, guiTop);
			graphics.blitSprite(
					RenderPipelines.GUI_TEXTURED,
					sprite,
					local.x() - SLOT_HIGHLIGHT_INSET,
					local.y() - SLOT_HIGHLIGHT_INSET,
					SLOT_HIGHLIGHT_SIZE,
					SLOT_HIGHLIGHT_SIZE
			);
		});
	}

	private static void renderInnerItems(
			GuiGraphics graphics,
			Font font,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		BundleInventory inventory = BundleInventoryAccess.read(overlay.bundleStack());
		BundleOverlayPlacement.SlotRect[] rects = overlay.overlayRects();

		for (int index = 0; index < rects.length; index++) {
			renderItemIfPresent(graphics, font, inventory, toGuiLocal(rects[index], guiLeft, guiTop), index);
		}
	}

	private static BundleOverlayPlacement.SlotRect toGuiLocal(
			BundleOverlayPlacement.SlotRect rect,
			int guiLeft,
			int guiTop
	) {
		return new BundleOverlayPlacement.SlotRect(
				rect.x() - guiLeft,
				rect.y() - guiTop,
				rect.w(),
				rect.h()
		);
	}

	private static void renderItemIfPresent(
			GuiGraphics graphics,
			Font font,
			BundleInventory inventory,
			BundleOverlayPlacement.SlotRect rect,
			int index
	) {
		ItemStack stack = inventory.get(index);
		if (stack.isEmpty()) {
			return;
		}

		int itemX = itemRenderX(rect);
		int itemY = itemRenderY(rect);
		int seed = rect.x() + rect.y();
		graphics.renderItem(stack, itemX, itemY, seed);
		graphics.renderItemDecorations(font, stack, itemX, itemY);
	}

	static int itemRenderX(BundleOverlayPlacement.SlotRect rect) {
		return rect.x() + (rect.w() - 16) / 2 + ITEM_RENDER_OFFSET_X;
	}

	static int itemRenderY(BundleOverlayPlacement.SlotRect rect) {
		return rect.y() + (rect.h() - 16) / 2 + ITEM_RENDER_OFFSET_Y;
	}

	public static void renderItemInSlot(
			GuiGraphics graphics,
			Font font,
			ItemStack stack,
			BundleOverlayPlacement.SlotRect rect
	) {
		int itemX = itemRenderX(rect);
		int itemY = itemRenderY(rect);
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

	private static boolean intersects(BundleOverlayPlacement.SlotRect a, BundleOverlayPlacement.SlotRect b) {
		return a.x() < b.right()
				&& a.right() > b.x()
				&& a.y() < b.bottom()
				&& a.bottom() > b.y();
	}
}
