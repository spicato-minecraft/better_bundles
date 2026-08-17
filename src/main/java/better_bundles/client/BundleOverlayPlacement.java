package better_bundles.client;

/**
 * Pure placement math for the 2×2 bundle hover overlay. No Minecraft client imports —
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

	/**
	 * @param gridWidth       host grid columns
	 * @param gridHeight      host grid rows
	 * @param slotIndex       index within the host grid
	 * @param slotPitchW      slot width in pixels
	 * @param slotPitchH      slot height in pixels
	 * @param viewportWidth   screen viewport width
	 * @param viewportHeight  screen viewport height
	 * @param hotbarGrid      {@code true} when the host grid is the hotbar row
	 * @param bundleScreenX   hovered bundle slot screen X (for clip fallback)
	 * @param bundleScreenY   hovered bundle slot screen Y (for clip fallback)
	 */
	public record PlacementInput(
			int gridWidth,
			int gridHeight,
			int slotIndex,
			int slotPitchW,
			int slotPitchH,
			int viewportWidth,
			int viewportHeight,
			boolean hotbarGrid,
			int bundleScreenX,
			int bundleScreenY
	) {
	}

	public record PlacementResult(
			SlotRect slot0,
			SlotRect slot1,
			SlotRect slot2,
			SlotRect slot3,
			int dirX,
			int dirY,
			boolean flippedX,
			boolean flippedY
	) {
		public SlotRect[] slots() {
			return new SlotRect[] { slot0, slot1, slot2, slot3 };
		}
	}

	/** Extended overlay canvas margin in slot units (each side). */
	public static final int EXTENDED_CANVAS_MARGIN_SLOTS = 2;

	public static PlacementResult compute(PlacementInput input) {
		int col = input.slotIndex() % input.gridWidth();
		int row = input.slotIndex() / input.gridWidth();

		int dirX = resolveDirX(col, input.gridWidth());
		int dirY = resolveDirY(row, input.gridHeight(), input.hotbarGrid());

		boolean flippedX = false;
		boolean flippedY = false;

		SlotRect[] rects = computeSlotRects(dirX, dirY, input.slotPitchW(), input.slotPitchH());
		Bounds bounds = boundsOf(rects, input.bundleScreenX(), input.bundleScreenY());

		if (bounds.minX() < 0 || bounds.maxX() > input.viewportWidth()) {
			dirX = -dirX;
			flippedX = true;
			rects = computeSlotRects(dirX, dirY, input.slotPitchW(), input.slotPitchH());
			bounds = boundsOf(rects, input.bundleScreenX(), input.bundleScreenY());
		}

		if (bounds.minY() < 0 || bounds.maxY() > input.viewportHeight()) {
			dirY = -dirY;
			flippedY = true;
			rects = computeSlotRects(dirX, dirY, input.slotPitchW(), input.slotPitchH());
		}

		return new PlacementResult(
				rects[0],
				rects[1],
				rects[2],
				rects[3],
				dirX,
				dirY,
				flippedX,
				flippedY
		);
	}

	/**
	 * Extended canvas bounds relative to the host grid origin (top-left slot), including
	 * {@link #EXTENDED_CANVAS_MARGIN_SLOTS} slot margin on every side.
	 */
	public static SlotRect extendedCanvasBounds(int gridWidth, int gridHeight, int pitchW, int pitchH) {
		int marginW = EXTENDED_CANVAS_MARGIN_SLOTS * pitchW;
		int marginH = EXTENDED_CANVAS_MARGIN_SLOTS * pitchH;
		return new SlotRect(
				-marginW,
				-marginH,
				gridWidth * pitchW + 2 * marginW,
				gridHeight * pitchH + 2 * marginH
		);
	}

	static int resolveDirX(int col, int gridWidth) {
		int centerCol = (gridWidth - 1) / 2;
		return col <= centerCol ? -1 : 1;
	}

	static int resolveDirY(int row, int gridHeight, boolean hotbarGrid) {
		if (hotbarGrid) {
			return -1;
		}
		int centerRow = (gridHeight - 1) / 2;
		return row <= centerRow ? -1 : 1;
	}

	static SlotRect[] computeSlotRects(int dirX, int dirY, int pitchW, int pitchH) {
		int originX = dirX < 0 ? -2 * pitchW : pitchW;
		int originY = dirY < 0 ? -2 * pitchH : pitchH;

		return new SlotRect[] {
				new SlotRect(originX, originY, pitchW, pitchH),
				new SlotRect(originX + pitchW, originY, pitchW, pitchH),
				new SlotRect(originX, originY + pitchH, pitchW, pitchH),
				new SlotRect(originX + pitchW, originY + pitchH, pitchW, pitchH),
		};
	}

	private record Bounds(int minX, int minY, int maxX, int maxY) {
	}

	private static Bounds boundsOf(SlotRect[] rects, int offsetX, int offsetY) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (SlotRect rect : rects) {
			minX = Math.min(minX, rect.x() + offsetX);
			minY = Math.min(minY, rect.y() + offsetY);
			maxX = Math.max(maxX, rect.right() + offsetX);
			maxY = Math.max(maxY, rect.bottom() + offsetY);
		}

		return new Bounds(minX, minY, maxX, maxY);
	}
}
