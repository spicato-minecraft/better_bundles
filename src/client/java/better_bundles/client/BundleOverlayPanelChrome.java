package better_bundles.client;

import better_bundles.inventory.BundleInventoryAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the pre-baked bundle overlay sprite for the locked DL layout.
 */
public final class BundleOverlayPanelChrome {

	private BundleOverlayPanelChrome() {
	}

	public static void renderStorageBackground(
			GuiGraphics graphics,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		blitSprite(graphics, overlay, guiLeft, guiTop);
	}

	public static void renderAnchorPatch(
			GuiGraphics graphics,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		blitAnchorPatch(graphics, overlay, guiLeft, guiTop);
	}

	public static void renderBundleItem(
			GuiGraphics graphics,
			Font font,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		renderBundleItemOnAnchor(graphics, font, overlay, guiLeft, guiTop);
	}

	private static void blitSprite(
			GuiGraphics graphics,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		BundleOverlaySpriteLayout.SpriteSpec spec = resolveSpriteSpec(overlay);
		Identifier texture = textureFor(spec);
		BundleOverlayPlacement.SlotRect drawRect = spriteDrawRect(spec, overlay, guiLeft, guiTop);

		blitSpriteRegion(
				graphics,
				texture,
				drawRect.x(),
				drawRect.y(),
				0,
				0,
				drawRect.w(),
				drawRect.h(),
				spec.width(),
				spec.height()
		);
	}

	private static void blitAnchorPatch(
			GuiGraphics graphics,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		BundleOverlaySpriteLayout.SpriteSpec spec = resolveSpriteSpec(overlay);
		Identifier texture = textureFor(spec);
		BundleOverlayPanelLayout.Frame frame = resolveFrame(overlay);
		BundleOverlayPlacement.SlotRect patch = union(
				toGuiLocal(frame.connector(), guiLeft, guiTop),
				toGuiLocal(frame.anchorFrame(), guiLeft, guiTop)
		);
		BundleOverlayPlacement.SlotRect drawRect = spriteDrawRect(spec, overlay, guiLeft, guiTop);

		int srcX = patch.x() - drawRect.x();
		int srcY = patch.y() - drawRect.y();
		blitSpriteRegion(
				graphics,
				texture,
				patch.x(),
				patch.y(),
				srcX,
				srcY,
				patch.w(),
				patch.h(),
				spec.width(),
				spec.height()
		);
	}

	private static void blitSpriteRegion(
			GuiGraphics graphics,
			Identifier texture,
			int destX,
			int destY,
			int srcX,
			int srcY,
			int width,
			int height,
			int textureWidth,
			int textureHeight
	) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				texture,
				destX,
				destY,
				srcX,
				srcY,
				width,
				height,
				width,
				height,
				textureWidth,
				textureHeight
		);
	}

	private static void renderBundleItemOnAnchor(
			GuiGraphics graphics,
			Font font,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		ItemStack stack = overlay.bundleStack();
		if (stack.isEmpty()) {
			return;
		}

		BundleOverlayPlacement.SlotRect anchor = toGuiLocal(
				overlay.placement().anchor().slotRect(),
				guiLeft,
				guiTop
		);
		BundleOverlayRenderer.renderItemInSlot(graphics, font, stack, anchor);
	}

	private static BundleOverlaySpriteLayout.SpriteSpec resolveSpriteSpec(
			BundleHoverController.ActiveOverlay overlay
	) {
		return BundleOverlaySpriteLayout.spec(
				overlay.placement().anchor().slotPitchW(),
				overlay.placement().anchor().slotPitchH()
		);
	}

	private static BundleOverlayPlacement.SlotRect spriteDrawRect(
			BundleOverlaySpriteLayout.SpriteSpec spec,
			BundleHoverController.ActiveOverlay overlay,
			int guiLeft,
			int guiTop
	) {
		BundleOverlayPlacement.SlotRect anchor = overlay.placement().anchor().slotRect();
		return new BundleOverlayPlacement.SlotRect(
				anchor.x() - spec.anchorPixelX() - guiLeft,
				anchor.y() - spec.anchorPixelY() - guiTop,
				spec.width(),
				spec.height()
		);
	}

	private static Identifier textureFor(BundleOverlaySpriteLayout.SpriteSpec spec) {
		return Identifier.fromNamespaceAndPath("better_bundles", spec.texturePath());
	}

	private static BundleOverlayPanelLayout.Frame resolveFrame(BundleHoverController.ActiveOverlay overlay) {
		return BundleOverlayPanelLayout.compute(
				overlay.overlayRects(),
				overlay.placement().anchor().slotRect()
		);
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

	private static BundleOverlayPlacement.SlotRect union(
			BundleOverlayPlacement.SlotRect first,
			BundleOverlayPlacement.SlotRect second
	) {
		if (first.w() <= 0 || first.h() <= 0) {
			return second;
		}
		if (second.w() <= 0 || second.h() <= 0) {
			return first;
		}

		int minX = Math.min(first.x(), second.x());
		int minY = Math.min(first.y(), second.y());
		int maxX = Math.max(first.right(), second.right());
		int maxY = Math.max(first.bottom(), second.bottom());
		return new BundleOverlayPlacement.SlotRect(minX, minY, maxX - minX, maxY - minY);
	}
}
