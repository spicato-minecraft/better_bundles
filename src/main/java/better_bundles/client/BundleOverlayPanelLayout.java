package better_bundles.client;

/**
 * Pure layout for the bundle overlay chrome: storage panel, connector, and anchor tab.
 */
public final class BundleOverlayPanelLayout {

	public static final int BORDER = 3;
	public static final int PADDING = 2;

	private BundleOverlayPanelLayout() {
	}

	public record Frame(
			BundleOverlayPlacement.SlotRect storageOuter,
			BundleOverlayPlacement.SlotRect connector,
			BundleOverlayPlacement.SlotRect anchorFrame
	) {
	}

	public static Frame compute(
			BundleOverlayPlacement.SlotRect[] slots,
			BundleOverlayPlacement.SlotRect anchor
	) {
		BundleOverlayPlacement.SlotRect storageInner = boundingBox(slots);
		BundleOverlayPlacement.SlotRect storageOuter = expand(storageInner, PADDING + BORDER);
		BundleOverlayPlacement.SlotRect anchorFrame = expand(anchor, BORDER);
		BundleOverlayPlacement.SlotRect connector = verticalConnectorBelow(storageOuter, anchorFrame);
		return new Frame(storageOuter, connector, anchorFrame);
	}

	private static BundleOverlayPlacement.SlotRect verticalConnectorBelow(
			BundleOverlayPlacement.SlotRect storageOuter,
			BundleOverlayPlacement.SlotRect anchorFrame
	) {
		int left = overlapStart(storageOuter.x(), storageOuter.right(), anchorFrame.x(), anchorFrame.right());
		int right = overlapEnd(storageOuter.x(), storageOuter.right(), anchorFrame.x(), anchorFrame.right());
		if (left >= right) {
			left = anchorFrame.x();
			right = anchorFrame.right();
		}

		int top = anchorFrame.bottom() - BORDER;
		int height = storageOuter.y() - top + BORDER;
		return new BundleOverlayPlacement.SlotRect(left, top, right - left, Math.max(0, height));
	}

	private static int overlapStart(int aStart, int aEnd, int bStart, int bEnd) {
		return Math.max(aStart, bStart);
	}

	private static int overlapEnd(int aStart, int aEnd, int bStart, int bEnd) {
		return Math.min(aEnd, bEnd);
	}

	private static BundleOverlayPlacement.SlotRect expand(BundleOverlayPlacement.SlotRect rect, int margin) {
		return new BundleOverlayPlacement.SlotRect(
				rect.x() - margin,
				rect.y() - margin,
				rect.w() + margin * 2,
				rect.h() + margin * 2
		);
	}

	private static BundleOverlayPlacement.SlotRect boundingBox(BundleOverlayPlacement.SlotRect[] rects) {
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
