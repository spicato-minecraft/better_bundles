package better_bundles;

import better_bundles.client.BundleOverlayPlacement;
import better_bundles.client.BundleOverlayPlacement.PlacementInput;
import better_bundles.client.BundleOverlayPlacement.PlacementResult;
import better_bundles.client.BundleOverlayPlacement.SlotRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleOverlayPlacementTest {

	private static final int PITCH = 18;
	private static final int VIEW_W = 400;
	private static final int VIEW_H = 300;

	@Test
	void upperLeftQuadrantOpensUpAndLeft() {
		PlacementResult result = computeMainGrid(0, 80, 60);

		assertEquals(-1, result.dirX());
		assertEquals(-1, result.dirY());
		assertFalse(result.flippedX());
		assertFalse(result.flippedY());
		assertSlotGrid(result, -2 * PITCH, -2 * PITCH);
	}

	@Test
	void upperRightQuadrantOpensUpAndRight() {
		PlacementResult result = computeMainGrid(8, 80, 60);

		assertEquals(1, result.dirX());
		assertEquals(-1, result.dirY());
		assertSlotGrid(result, PITCH, -2 * PITCH);
	}

	@Test
	void lowerLeftQuadrantOpensDownAndLeft() {
		PlacementResult result = computeMainGrid(18, 80, 60);

		assertEquals(-1, result.dirX());
		assertEquals(1, result.dirY());
		assertSlotGrid(result, -2 * PITCH, PITCH);
	}

	@Test
	void lowerRightQuadrantOpensDownAndRight() {
		PlacementResult result = computeMainGrid(26, 80, 60);

		assertEquals(1, result.dirX());
		assertEquals(1, result.dirY());
		assertSlotGrid(result, PITCH, PITCH);
	}

	@Test
	void centerColumnSlotFourOpensLeft() {
		PlacementResult result = computeMainGrid(4, 80, 60);

		assertEquals(-1, result.dirX());
		assertEquals(-1, result.dirY());
	}

	@Test
	void centerColumnSlotFiveOpensRight() {
		PlacementResult result = computeMainGrid(5, 80, 60);

		assertEquals(1, result.dirX());
		assertEquals(-1, result.dirY());
	}

	@Test
	void hotbarSlotFourOpensUpAndLeft() {
		PlacementResult result = computeHotbar(4, 80, 200);

		assertEquals(-1, result.dirX());
		assertEquals(-1, result.dirY());
		assertSlotGrid(result, -2 * PITCH, -2 * PITCH);
	}

	@Test
	void hotbarSlotFiveOpensUpAndRight() {
		PlacementResult result = computeHotbar(5, 80, 200);

		assertEquals(1, result.dirX());
		assertEquals(-1, result.dirY());
		assertSlotGrid(result, PITCH, -2 * PITCH);
	}

	@Test
	void hotbarAlwaysOpensUpward() {
		for (int slot = 0; slot < 9; slot++) {
			PlacementResult result = computeHotbar(slot, 80, 200);
			assertEquals(-1, result.dirY(), "slot " + slot);
		}
	}

	@Test
	void extendedCanvasHasTwoSlotMargin() {
		SlotRect canvas = BundleOverlayPlacement.extendedCanvasBounds(9, 3, PITCH, PITCH);

		assertEquals(-2 * PITCH, canvas.x());
		assertEquals(-2 * PITCH, canvas.y());
		assertEquals(9 * PITCH + 4 * PITCH, canvas.w());
		assertEquals(3 * PITCH + 4 * PITCH, canvas.h());
	}

	@Test
	void clipFallbackFlipsHorizontalWhenOverlayExitsLeftEdge() {
		PlacementInput input = new PlacementInput(
				9, 3, 0, PITCH, PITCH, VIEW_W, VIEW_H, false, 0, 80
		);
		PlacementResult result = BundleOverlayPlacement.compute(input);

		assertTrue(result.flippedX());
		assertEquals(1, result.dirX());
		assertSlotGrid(result, PITCH, -2 * PITCH);
	}

	@Test
	void clipFallbackFlipsVerticalWhenOverlayExitsTopEdge() {
		PlacementInput input = new PlacementInput(
				9, 3, 4, PITCH, PITCH, VIEW_W, VIEW_H, false, 80, 0
		);
		PlacementResult result = BundleOverlayPlacement.compute(input);

		assertTrue(result.flippedY());
		assertEquals(-1, result.dirX());
		assertEquals(1, result.dirY());
		assertSlotGrid(result, -2 * PITCH, PITCH);
	}

	@Test
	void clipFallbackFlipsHorizontalWhenOverlayExitsRightEdge() {
		PlacementInput input = new PlacementInput(
				9, 3, 8, PITCH, PITCH, VIEW_W, VIEW_H, false, VIEW_W - PITCH, 80
		);
		PlacementResult result = BundleOverlayPlacement.compute(input);

		assertTrue(result.flippedX());
		assertEquals(-1, result.dirX());
		assertSlotGrid(result, -2 * PITCH, -2 * PITCH);
	}

	@Test
	void clipFallbackFlipsVerticalWhenOverlayExitsBottomEdge() {
		// Slot 22 (center column, bottom row) defaults to down-left; near bottom edge flips to up-left.
		PlacementInput input = new PlacementInput(
				9, 3, 22, PITCH, PITCH, VIEW_W, VIEW_H, false, 80, VIEW_H - PITCH
		);
		PlacementResult result = BundleOverlayPlacement.compute(input);

		assertTrue(result.flippedY());
		assertEquals(-1, result.dirX());
		assertEquals(-1, result.dirY());
		assertSlotGrid(result, -2 * PITCH, -2 * PITCH);
	}

	@Test
	void innerSlotOrderIsLeftToRightTopToBottom() {
		PlacementResult result = computeMainGrid(13, 80, 60);

		assertEquals(new SlotRect(-36, -36, PITCH, PITCH), result.slot0());
		assertEquals(new SlotRect(-18, -36, PITCH, PITCH), result.slot1());
		assertEquals(new SlotRect(-36, -18, PITCH, PITCH), result.slot2());
		assertEquals(new SlotRect(-18, -18, PITCH, PITCH), result.slot3());
	}

	private static PlacementResult computeMainGrid(int slotIndex, int bundleX, int bundleY) {
		PlacementInput input = new PlacementInput(
				9, 3, slotIndex, PITCH, PITCH, VIEW_W, VIEW_H, false, bundleX, bundleY
		);
		return BundleOverlayPlacement.compute(input);
	}

	private static PlacementResult computeHotbar(int slotIndex, int bundleX, int bundleY) {
		PlacementInput input = new PlacementInput(
				9, 1, slotIndex, PITCH, PITCH, VIEW_W, VIEW_H, true, bundleX, bundleY
		);
		return BundleOverlayPlacement.compute(input);
	}

	private static void assertSlotGrid(PlacementResult result, int originX, int originY) {
		assertEquals(new SlotRect(originX, originY, PITCH, PITCH), result.slot0());
		assertEquals(new SlotRect(originX + PITCH, originY, PITCH, PITCH), result.slot1());
		assertEquals(new SlotRect(originX, originY + PITCH, PITCH, PITCH), result.slot2());
		assertEquals(new SlotRect(originX + PITCH, originY + PITCH, PITCH, PITCH), result.slot3());
	}
}
