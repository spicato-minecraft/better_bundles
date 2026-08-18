package better_bundles;

import better_bundles.client.BundleOverlayPlacement;
import better_bundles.client.BundleOverlayPlacement.PlacementInput;
import better_bundles.client.BundleOverlayPlacement.PlacementResult;
import better_bundles.client.BundleOverlayPlacement.SlotRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BundleOverlayPlacementTest {

	private static final int PITCH = 18;

	@Test
	void lockedSlotGridOpensBelowAnchor() {
		PlacementResult main = compute(PITCH, PITCH);
		assertSlotGrid(main, 0, PITCH);
	}

	@Test
	void innerSlotOrderIsLeftToRightTopToBottom() {
		PlacementResult result = compute(PITCH, PITCH);

		assertEquals(new SlotRect(0, PITCH, PITCH, PITCH), result.slot0());
		assertEquals(new SlotRect(PITCH, PITCH, PITCH, PITCH), result.slot1());
		assertEquals(new SlotRect(0, 2 * PITCH, PITCH, PITCH), result.slot2());
		assertEquals(new SlotRect(PITCH, 2 * PITCH, PITCH, PITCH), result.slot3());
	}

	private static PlacementResult compute(int pitchW, int pitchH) {
		return BundleOverlayPlacement.compute(new PlacementInput(pitchW, pitchH));
	}

	private static void assertSlotGrid(PlacementResult result, int originX, int originY) {
		assertEquals(new SlotRect(originX, originY, PITCH, PITCH), result.slot0());
		assertEquals(new SlotRect(originX + PITCH, originY, PITCH, PITCH), result.slot1());
		assertEquals(new SlotRect(originX, originY + PITCH, PITCH, PITCH), result.slot2());
		assertEquals(new SlotRect(originX + PITCH, originY + PITCH, PITCH, PITCH), result.slot3());
	}
}
