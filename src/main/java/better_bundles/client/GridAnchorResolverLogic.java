package better_bundles.client;

import java.util.List;
import java.util.OptionalInt;

/**
 * Pure grid classification and pitch math for bundle overlay anchoring.
 * Lives in the main source set so unit tests can run without a Minecraft client.
 */
public final class GridAnchorResolverLogic {

	private GridAnchorResolverLogic() {
	}

	public static final int PLAYER_TAIL_SLOT_COUNT = 36;
	public static final int PLAYER_MAIN_SLOT_COUNT = 27;
	public static final int PLAYER_HOTBAR_SLOT_COUNT = 9;
	public static final int STANDARD_GRID_WIDTH = 9;

	public static final int INV_MENU_CRAFT_RESULT = 0;
	public static final int INV_MENU_CRAFT_START = 1;
	public static final int INV_MENU_CRAFT_END = 5;
	public static final int INV_MENU_ARMOR_START = 5;
	public static final int INV_MENU_ARMOR_END = 9;
	public static final int INV_MENU_MAIN_START = 9;
	public static final int INV_MENU_MAIN_END = 36;
	public static final int INV_MENU_HOTBAR_START = 36;
	public static final int INV_MENU_HOTBAR_END = 45;
	public static final int INV_MENU_OFFHAND = 45;

	public enum HostGridKind {
		CONTAINER,
		MAIN,
		HOTBAR
	}

	public enum MenuLayout {
		PLAYER_INVENTORY,
		STANDARD_CONTAINER
	}

	public record IntSlotPos(int x, int y) {
	}

	public record SlotClassification(
			boolean supported,
			HostGridKind hostGrid,
			int gridWidth,
			int gridHeight,
			int slotIndexInGrid
	) {
		public static final SlotClassification UNSUPPORTED = new SlotClassification(false, null, 0, 0, 0);
	}

	public record GridDimensions(int width, int height) {
	}

	public static boolean isExcludedInventoryMenuSlot(int menuSlotIndex) {
		return menuSlotIndex == INV_MENU_CRAFT_RESULT
				|| (menuSlotIndex >= INV_MENU_CRAFT_START && menuSlotIndex < INV_MENU_CRAFT_END)
				|| (menuSlotIndex >= INV_MENU_ARMOR_START && menuSlotIndex < INV_MENU_ARMOR_END)
				|| menuSlotIndex == INV_MENU_OFFHAND;
	}

	public static SlotClassification classifyInventoryMenuSlot(int menuSlotIndex) {
		if (isExcludedInventoryMenuSlot(menuSlotIndex)) {
			return SlotClassification.UNSUPPORTED;
		}

		if (menuSlotIndex >= INV_MENU_MAIN_START && menuSlotIndex < INV_MENU_MAIN_END) {
			return new SlotClassification(
					true,
					HostGridKind.MAIN,
					STANDARD_GRID_WIDTH,
					PLAYER_MAIN_SLOT_COUNT / STANDARD_GRID_WIDTH,
					menuSlotIndex - INV_MENU_MAIN_START
			);
		}

		if (menuSlotIndex >= INV_MENU_HOTBAR_START && menuSlotIndex < INV_MENU_HOTBAR_END) {
			return new SlotClassification(
					true,
					HostGridKind.HOTBAR,
					STANDARD_GRID_WIDTH,
					1,
					menuSlotIndex - INV_MENU_HOTBAR_START
			);
		}

		return SlotClassification.UNSUPPORTED;
	}

	public static int standardContainerSlotCount(int menuSlotCount) {
		return Math.max(0, menuSlotCount - PLAYER_TAIL_SLOT_COUNT);
	}

	public static SlotClassification classifyStandardContainerMenuSlot(int menuSlotIndex, int containerSlotCount) {
		if (containerSlotCount < 0 || menuSlotIndex < 0) {
			return SlotClassification.UNSUPPORTED;
		}

		int playerStart = containerSlotCount;
		int mainStart = playerStart;
		int mainEnd = mainStart + PLAYER_MAIN_SLOT_COUNT;
		int hotbarStart = mainEnd;
		int hotbarEnd = hotbarStart + PLAYER_HOTBAR_SLOT_COUNT;

		if (menuSlotIndex >= mainStart && menuSlotIndex < mainEnd) {
			return new SlotClassification(
					true,
					HostGridKind.MAIN,
					STANDARD_GRID_WIDTH,
					PLAYER_MAIN_SLOT_COUNT / STANDARD_GRID_WIDTH,
					menuSlotIndex - mainStart
			);
		}

		if (menuSlotIndex >= hotbarStart && menuSlotIndex < hotbarEnd) {
			return new SlotClassification(
					true,
					HostGridKind.HOTBAR,
					STANDARD_GRID_WIDTH,
					1,
					menuSlotIndex - hotbarStart
			);
		}

		if (menuSlotIndex < containerSlotCount) {
			if (containerSlotCount % STANDARD_GRID_WIDTH == 0) {
				int rows = containerSlotCount / STANDARD_GRID_WIDTH;
				return new SlotClassification(
						true,
						HostGridKind.CONTAINER,
						STANDARD_GRID_WIDTH,
						rows,
						menuSlotIndex
				);
			}

			// Hopper and other non-9-wide container grids; dimensions refined from live slot positions.
			return new SlotClassification(
					true,
					HostGridKind.CONTAINER,
					containerSlotCount,
					1,
					menuSlotIndex
			);
		}

		return SlotClassification.UNSUPPORTED;
	}

	public static SlotClassification classifyMenuSlot(MenuLayout layout, int menuSlotIndex, int containerSlotCount) {
		return switch (layout) {
			case PLAYER_INVENTORY -> classifyInventoryMenuSlot(menuSlotIndex);
			case STANDARD_CONTAINER -> classifyStandardContainerMenuSlot(menuSlotIndex, containerSlotCount);
		};
	}

	public static int menuSlotIndexForGridSlot(
			MenuLayout layout,
			SlotClassification classification,
			int containerSlotCount,
			int slotIndexInGrid
	) {
		return switch (layout) {
			case PLAYER_INVENTORY -> switch (classification.hostGrid()) {
				case MAIN -> INV_MENU_MAIN_START + slotIndexInGrid;
				case HOTBAR -> INV_MENU_HOTBAR_START + slotIndexInGrid;
				case CONTAINER -> slotIndexInGrid;
			};
			case STANDARD_CONTAINER -> switch (classification.hostGrid()) {
				case MAIN -> containerSlotCount + slotIndexInGrid;
				case HOTBAR -> containerSlotCount + PLAYER_MAIN_SLOT_COUNT + slotIndexInGrid;
				case CONTAINER -> slotIndexInGrid;
			};
		};
	}

	public static OptionalInt deriveHorizontalPitch(IntSlotPos current, IntSlotPos rightNeighbor) {
		int delta = rightNeighbor.x() - current.x();
		return delta > 0 ? OptionalInt.of(delta) : OptionalInt.empty();
	}

	public static OptionalInt deriveVerticalPitch(IntSlotPos current, IntSlotPos belowNeighbor) {
		int delta = belowNeighbor.y() - current.y();
		return delta > 0 ? OptionalInt.of(delta) : OptionalInt.empty();
	}

	public static int derivePitchW(
			SlotClassification classification,
			IntSlotPos current,
			IntSlotPos horizontalNeighbor,
			int fallback
	) {
		if (classification.slotIndexInGrid() % classification.gridWidth() < classification.gridWidth() - 1) {
			return deriveHorizontalPitch(current, horizontalNeighbor).orElse(fallback);
		}

		return fallback;
	}

	public static int derivePitchH(
			SlotClassification classification,
			IntSlotPos current,
			IntSlotPos verticalNeighbor,
			int fallback
	) {
		int maxRowStart = (classification.gridHeight() - 1) * classification.gridWidth();
		if (classification.slotIndexInGrid() < maxRowStart) {
			return deriveVerticalPitch(current, verticalNeighbor).orElse(fallback);
		}

		return fallback;
	}

	public static GridDimensions deriveContainerGridDimensions(List<IntSlotPos> containerSlots) {
		if (containerSlots.isEmpty()) {
			return new GridDimensions(0, 0);
		}

		int minY = containerSlots.stream().mapToInt(IntSlotPos::y).min().orElse(0);
		int pitchY = 0;
		for (IntSlotPos pos : containerSlots) {
			if (pos.y() > minY) {
				pitchY = pos.y() - minY;
				break;
			}
		}

		int rowTolerance = pitchY > 0 ? Math.max(1, pitchY / 2) : 1;
		int rows = 1;
		int lastRowY = minY;
		for (IntSlotPos pos : containerSlots) {
			if (pos.y() - lastRowY > rowTolerance) {
				rows++;
				lastRowY = pos.y();
			}
		}

		int width = (int) containerSlots.stream()
				.filter(pos -> Math.abs(pos.y() - minY) <= rowTolerance)
				.count();

		if (width <= 0) {
			width = STANDARD_GRID_WIDTH;
		}

		return new GridDimensions(width, rows);
	}

	public static BundleOverlayPlacement.PlacementInput toPlacementInput(
			SlotClassification classification,
			int slotPitchW,
			int slotPitchH
	) {
		return new BundleOverlayPlacement.PlacementInput(slotPitchW, slotPitchH);
	}

	public static BundleOverlayPlacement.SlotRect toScreenRect(
			BundleOverlayPlacement.SlotRect relativeRect,
			int anchorScreenX,
			int anchorScreenY
	) {
		return new BundleOverlayPlacement.SlotRect(
				relativeRect.x() + anchorScreenX,
				relativeRect.y() + anchorScreenY,
				relativeRect.w(),
				relativeRect.h()
		);
	}

	public static BundleOverlayPlacement.SlotRect[] toScreenRects(
			BundleOverlayPlacement.PlacementResult result,
			int anchorScreenX,
			int anchorScreenY
	) {
		BundleOverlayPlacement.SlotRect[] screenRects = new BundleOverlayPlacement.SlotRect[4];
		BundleOverlayPlacement.SlotRect[] relativeRects = result.slots();

		for (int i = 0; i < relativeRects.length; i++) {
			screenRects[i] = toScreenRect(relativeRects[i], anchorScreenX, anchorScreenY);
		}

		return screenRects;
	}
}
