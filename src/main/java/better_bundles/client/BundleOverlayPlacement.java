package better_bundles.client;

/**
 * Pure placement math for the locked DL bundle hover overlay. No Minecraft client imports —
 * lives in the main source set so unit tests can run offline.
 */
public final class BundleOverlayPlacement {

	private BundleOverlayPlacement() {
	}

	public record SlotRect(int x, int y, int w, int h) {
		public int right() {
			return x + w;
		}

		public int bottom() {
			return y + h;
		}
	}

	public record PlacementInput(int slotPitchW, int slotPitchH) {
	}

	public record PlacementResult(
			SlotRect slot0,
			SlotRect slot1,
			SlotRect slot2,
			SlotRect slot3
	) {
		public SlotRect[] slots() {
			return new SlotRect[] { slot0, slot1, slot2, slot3 };
		}
	}

	public static PlacementResult compute(PlacementInput input) {
		SlotRect[] rects = computeSlotRects(input.slotPitchW(), input.slotPitchH());
		return new PlacementResult(rects[0], rects[1], rects[2], rects[3]);
	}

	/**
	 * Inner slot layout relative to the bundle anchor: 2×2 grid opening downward.
	 */
	static SlotRect[] computeSlotRects(int pitchW, int pitchH) {
		return new SlotRect[] {
				new SlotRect(0, pitchH, pitchW, pitchH),
				new SlotRect(pitchW, pitchH, pitchW, pitchH),
				new SlotRect(0, 2 * pitchH, pitchW, pitchH),
				new SlotRect(pitchW, 2 * pitchH, pitchW, pitchH),
		};
	}
}
