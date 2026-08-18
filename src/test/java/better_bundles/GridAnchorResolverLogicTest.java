package better_bundles;

import better_bundles.client.BundleOverlayPlacement;
import better_bundles.client.GridAnchorResolverLogic;
import better_bundles.client.GridAnchorResolverLogic.HostGridKind;
import better_bundles.client.GridAnchorResolverLogic.IntSlotPos;
import better_bundles.client.GridAnchorResolverLogic.MenuLayout;
import better_bundles.client.GridAnchorResolverLogic.SlotClassification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridAnchorResolverLogicTest {

	private static final int PITCH = 18;

	@Test
	void inventoryMenuMainGridSlotNineIsSupported() {
		SlotClassification classification = GridAnchorResolverLogic.classifyInventoryMenuSlot(9);

		assertTrue(classification.supported());
		assertEquals(HostGridKind.MAIN, classification.hostGrid());
		assertEquals(9, classification.gridWidth());
		assertEquals(3, classification.gridHeight());
		assertEquals(0, classification.slotIndexInGrid());
	}

	@Test
	void inventoryMenuHotbarSlotFortyFourIsSupported() {
		SlotClassification classification = GridAnchorResolverLogic.classifyInventoryMenuSlot(44);

		assertTrue(classification.supported());
		assertEquals(HostGridKind.HOTBAR, classification.hostGrid());
		assertEquals(8, classification.slotIndexInGrid());
	}

	@Test
	void inventoryMenuExcludesCraftArmorAndOffhand() {
		assertFalse(GridAnchorResolverLogic.classifyInventoryMenuSlot(0).supported());
		assertFalse(GridAnchorResolverLogic.classifyInventoryMenuSlot(2).supported());
		assertFalse(GridAnchorResolverLogic.classifyInventoryMenuSlot(6).supported());
		assertFalse(GridAnchorResolverLogic.classifyInventoryMenuSlot(45).supported());
	}

	@Test
	void chestMenuContainerMainAndHotbarRanges() {
		int containerSlots = 27;

		SlotClassification container = GridAnchorResolverLogic.classifyStandardContainerMenuSlot(10, containerSlots);
		assertTrue(container.supported());
		assertEquals(HostGridKind.CONTAINER, container.hostGrid());
		assertEquals(10, container.slotIndexInGrid());

		SlotClassification main = GridAnchorResolverLogic.classifyStandardContainerMenuSlot(27, containerSlots);
		assertTrue(main.supported());
		assertEquals(HostGridKind.MAIN, main.hostGrid());
		assertEquals(0, main.slotIndexInGrid());

		SlotClassification hotbar = GridAnchorResolverLogic.classifyStandardContainerMenuSlot(54, containerSlots);
		assertTrue(hotbar.supported());
		assertEquals(HostGridKind.HOTBAR, hotbar.hostGrid());
		assertEquals(0, hotbar.slotIndexInGrid());
	}

	@Test
	void menuSlotIndexRoundTripForPlayerInventory() {
		SlotClassification main = GridAnchorResolverLogic.classifyInventoryMenuSlot(13);
		assertEquals(
				13,
				GridAnchorResolverLogic.menuSlotIndexForGridSlot(
						MenuLayout.PLAYER_INVENTORY,
						main,
						0,
						main.slotIndexInGrid()
				)
		);

		SlotClassification hotbar = GridAnchorResolverLogic.classifyInventoryMenuSlot(40);
		assertEquals(
				40,
				GridAnchorResolverLogic.menuSlotIndexForGridSlot(
						MenuLayout.PLAYER_INVENTORY,
						hotbar,
						0,
						hotbar.slotIndexInGrid()
				)
		);
	}

	@Test
	void derivePitchFromLiveSlotPositions() {
		IntSlotPos current = new IntSlotPos(8, 84);
		IntSlotPos right = new IntSlotPos(26, 84);
		IntSlotPos below = new IntSlotPos(8, 102);

		assertEquals(OptionalInt.of(PITCH), GridAnchorResolverLogic.deriveHorizontalPitch(current, right));
		assertEquals(OptionalInt.of(PITCH), GridAnchorResolverLogic.deriveVerticalPitch(current, below));
	}

	@Test
	void derivePitchUsesNeighborsWithinGrid() {
		SlotClassification classification = new SlotClassification(true, HostGridKind.MAIN, 9, 3, 4);
		IntSlotPos current = new IntSlotPos(80, 60);
		IntSlotPos right = new IntSlotPos(98, 60);
		IntSlotPos below = new IntSlotPos(80, 78);

		assertEquals(PITCH, GridAnchorResolverLogic.derivePitchW(classification, current, right, PITCH));
		assertEquals(PITCH, GridAnchorResolverLogic.derivePitchH(classification, current, below, PITCH));
	}

	@Test
	void deriveContainerGridDimensionsFromPositions() {
		List<IntSlotPos> chestSlots = List.of(
				new IntSlotPos(8, 18),
				new IntSlotPos(26, 18),
				new IntSlotPos(44, 18),
				new IntSlotPos(8, 36),
				new IntSlotPos(26, 36)
		);

		GridAnchorResolverLogic.GridDimensions dimensions = GridAnchorResolverLogic.deriveContainerGridDimensions(chestSlots);

		assertEquals(3, dimensions.width());
		assertEquals(2, dimensions.height());
	}

	@Test
	void toPlacementInputUsesSlotPitch() {
		SlotClassification hotbar = GridAnchorResolverLogic.classifyInventoryMenuSlot(36);
		BundleOverlayPlacement.PlacementInput input = GridAnchorResolverLogic.toPlacementInput(hotbar, PITCH, PITCH);

		assertEquals(PITCH, input.slotPitchW());
		assertEquals(PITCH, input.slotPitchH());
	}

	@Test
	void toScreenRectsTranslatesRelativePlacement() {
		SlotClassification main = GridAnchorResolverLogic.classifyInventoryMenuSlot(13);
		BundleOverlayPlacement.PlacementInput input = GridAnchorResolverLogic.toPlacementInput(main, PITCH, PITCH);
		BundleOverlayPlacement.PlacementResult placement = BundleOverlayPlacement.compute(input);
		BundleOverlayPlacement.SlotRect[] screenRects = GridAnchorResolverLogic.toScreenRects(placement, 80, 60);

		assertEquals(new BundleOverlayPlacement.SlotRect(80, 78, PITCH, PITCH), screenRects[0]);
		assertEquals(new BundleOverlayPlacement.SlotRect(98, 78, PITCH, PITCH), screenRects[1]);
		assertEquals(new BundleOverlayPlacement.SlotRect(80, 96, PITCH, PITCH), screenRects[2]);
		assertEquals(new BundleOverlayPlacement.SlotRect(98, 96, PITCH, PITCH), screenRects[3]);
	}

	@Test
	void standardContainerSlotCountUsesPlayerTail() {
		assertEquals(27, GridAnchorResolverLogic.standardContainerSlotCount(63));
		assertEquals(5, GridAnchorResolverLogic.standardContainerSlotCount(41));
		assertEquals(0, GridAnchorResolverLogic.standardContainerSlotCount(36));
	}

	@Test
	void hopperContainerSlotsAreSupported() {
		int containerSlots = 5;

		SlotClassification middle = GridAnchorResolverLogic.classifyStandardContainerMenuSlot(2, containerSlots);
		assertTrue(middle.supported());
		assertEquals(HostGridKind.CONTAINER, middle.hostGrid());
		assertEquals(5, middle.gridWidth());
		assertEquals(1, middle.gridHeight());
		assertEquals(2, middle.slotIndexInGrid());
	}
}
