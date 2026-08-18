package better_bundles;

import better_bundles.client.BundleOverlayPanelLayout;
import better_bundles.client.BundleOverlayPlacement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleOverlayPanelLayoutTest {

	@Test
	void frameWrapsStorageSlotsAndTouchesAnchor() {
		BundleOverlayPlacement.SlotRect[] slots = new BundleOverlayPlacement.SlotRect[] {
				new BundleOverlayPlacement.SlotRect(10, 10, 18, 18),
				new BundleOverlayPlacement.SlotRect(28, 10, 18, 18),
				new BundleOverlayPlacement.SlotRect(10, 28, 18, 18),
				new BundleOverlayPlacement.SlotRect(28, 28, 18, 18),
		};
		BundleOverlayPlacement.SlotRect anchor = new BundleOverlayPlacement.SlotRect(64, 28, 18, 18);

		BundleOverlayPanelLayout.Frame frame = BundleOverlayPanelLayout.compute(slots, anchor);

		assertContains(frame.storageOuter(), slots[0]);
		assertContains(frame.storageOuter(), slots[3]);
		assertContains(frame.anchorFrame(), anchor);
		assertTrue(frame.connector().w() > 0);
	}

	private static void assertContains(BundleOverlayPlacement.SlotRect outer, BundleOverlayPlacement.SlotRect inner) {
		assertTrue(outer.x() <= inner.x());
		assertTrue(outer.y() <= inner.y());
		assertTrue(outer.right() >= inner.right());
		assertTrue(outer.bottom() >= inner.bottom());
	}
}
