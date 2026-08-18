package better_bundles.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-baked bundle overlay sprite for the locked DL layout.
 */
public final class BundleOverlaySpriteLayout {

	private static final int DEFAULT_PITCH = 18;
	private static final int ANCHOR_DRAW_OFFSET_X = 1;
	private static final int ANCHOR_DRAW_OFFSET_Y = 1;
	private static final String TEXTURE_ID = "dl";

	private BundleOverlaySpriteLayout() {
	}

	public static SpriteSpec spec() {
		return spec(DEFAULT_PITCH, DEFAULT_PITCH);
	}

	public static SpriteSpec spec(int pitchW, int pitchH) {
		BundleOverlayPlacement.SlotRect anchor = new BundleOverlayPlacement.SlotRect(0, 0, pitchW, pitchH);
		BundleOverlayPlacement.SlotRect[] relativeSlots = BundleOverlayPlacement.computeSlotRects(pitchW, pitchH);
		BundleOverlayPlacement.SlotRect[] screenSlots = new BundleOverlayPlacement.SlotRect[relativeSlots.length];
		for (int i = 0; i < relativeSlots.length; i++) {
			screenSlots[i] = GridAnchorResolverLogic.toScreenRect(relativeSlots[i], 0, 0);
		}

		BundleOverlayPanelLayout.Frame frame = BundleOverlayPanelLayout.compute(screenSlots, anchor);
		BundleOverlayPlacement.SlotRect bounds = union(
				frame.storageOuter(),
				frame.connector(),
				frame.anchorFrame()
		);

		int anchorPixelX = -bounds.x() + ANCHOR_DRAW_OFFSET_X;
		int anchorPixelY = -bounds.y() + ANCHOR_DRAW_OFFSET_Y;
		BundleOverlayPlacement.SlotRect[] innerSlotPixels = new BundleOverlayPlacement.SlotRect[screenSlots.length];
		for (int i = 0; i < screenSlots.length; i++) {
			innerSlotPixels[i] = new BundleOverlayPlacement.SlotRect(
					screenSlots[i].x() - bounds.x(),
					screenSlots[i].y() - bounds.y(),
					screenSlots[i].w(),
					screenSlots[i].h()
			);
		}

		return new SpriteSpec(
				TEXTURE_ID,
				bounds.w(),
				bounds.h(),
				anchorPixelX,
				anchorPixelY,
				innerSlotPixels
		);
	}

	public record SpriteSpec(
			String textureId,
			int width,
			int height,
			int anchorPixelX,
			int anchorPixelY,
			BundleOverlayPlacement.SlotRect[] innerSlotPixels
	) {
		public String texturePath() {
			return "textures/gui/bundle_overlay/" + textureId + ".png";
		}
	}

	private static BundleOverlayPlacement.SlotRect union(
			BundleOverlayPlacement.SlotRect first,
			BundleOverlayPlacement.SlotRect second,
			BundleOverlayPlacement.SlotRect third
	) {
		List<BundleOverlayPlacement.SlotRect> rects = new ArrayList<>();
		rects.add(first);
		if (second.w() > 0 && second.h() > 0) {
			rects.add(second);
		}
		rects.add(third);

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (BundleOverlayPlacement.SlotRect rect : rects) {
			minX = Math.min(minX, rect.x());
			minY = Math.min(minY, rect.y());
			maxX = Math.max(maxX, rect.right());
			maxY = Math.max(maxY, rect.bottom());
		}

		return new BundleOverlayPlacement.SlotRect(minX, minY, maxX - minX, maxY - minY);
	}
}
