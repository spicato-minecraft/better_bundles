package better_bundles.client;

import better_bundles.client.GridAnchorResolverLogic.HostGridKind;
import better_bundles.client.GridAnchorResolverLogic.IntSlotPos;
import better_bundles.client.GridAnchorResolverLogic.MenuLayout;
import better_bundles.client.GridAnchorResolverLogic.SlotClassification;
import better_bundles.mixin.client.AbstractContainerScreenAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves live container-screen slot geometry into {@link BundleOverlayPlacement} inputs.
 */
public final class GridAnchorResolver {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Temporary dev toggle; prefer {@link Logger#isDebugEnabled()} in call sites. */
	public static final boolean DEBUG_PLACEMENT_RECTS = false;

	private static final int DEFAULT_SLOT_PITCH = 18;

	private GridAnchorResolver() {
	}

	public record ResolvedAnchor(
			BundleOverlayPlacement.SlotRect slotRect,
			HostGridKind hostGrid,
			int gridWidth,
			int gridHeight,
			int slotIndexInGrid,
			int slotPitchW,
			int slotPitchH
	) {
	}

	public record ResolvedPlacement(
			ResolvedAnchor anchor,
			BundleOverlayPlacement.PlacementResult placement,
			BundleOverlayPlacement.SlotRect[] overlayScreenRects
	) {
	}

	public static Optional<ResolvedAnchor> resolve(AbstractContainerScreen<?> screen, Slot slot) {
		if (screen == null || slot == null) {
			return Optional.empty();
		}

		AbstractContainerMenu menu = screen.getMenu();
		MenuLayout layout = detectMenuLayout(screen, menu);
		if (layout == null) {
			return Optional.empty();
		}

		int containerSlotCount = containerSlotCount(menu, layout);
		SlotClassification classification = GridAnchorResolverLogic.classifyMenuSlot(
				layout,
				slot.index,
				containerSlotCount
		);

		if (!classification.supported()) {
			return Optional.empty();
		}

		if (classification.hostGrid() == HostGridKind.CONTAINER && layout == MenuLayout.STANDARD_CONTAINER) {
			classification = refineContainerClassification(menu, containerSlotCount, classification);
			if (!classification.supported()) {
				return Optional.empty();
			}
		}

		int bundleScreenX = screenOriginX(screen) + slot.x;
		int bundleScreenY = screenOriginY(screen) + slot.y;
		int[] pitch = derivePitch(menu, layout, classification, containerSlotCount, slot);
		BundleOverlayPlacement.SlotRect slotRect = new BundleOverlayPlacement.SlotRect(
				bundleScreenX,
				bundleScreenY,
				pitch[0],
				pitch[1]
		);

		ResolvedAnchor anchor = new ResolvedAnchor(
				slotRect,
				classification.hostGrid(),
				classification.gridWidth(),
				classification.gridHeight(),
				classification.slotIndexInGrid(),
				pitch[0],
				pitch[1]
		);

		logDebugAnchor(screen, slot, anchor);
		return Optional.of(anchor);
	}

	public static Optional<ResolvedPlacement> resolvePlacement(
			AbstractContainerScreen<?> screen,
			Slot slot,
			int viewportWidth,
			int viewportHeight
	) {
		return resolve(screen, slot).map(anchor -> {
			SlotClassification classification = new SlotClassification(
					true,
					anchor.hostGrid(),
					anchor.gridWidth(),
					anchor.gridHeight(),
					anchor.slotIndexInGrid()
			);

			BundleOverlayPlacement.PlacementInput input = GridAnchorResolverLogic.toPlacementInput(
					classification,
					anchor.slotPitchW(),
					anchor.slotPitchH(),
					viewportWidth,
					viewportHeight,
					anchor.slotRect().x(),
					anchor.slotRect().y()
			);

			BundleOverlayPlacement.PlacementResult placement = BundleOverlayPlacement.compute(input);
			BundleOverlayPlacement.SlotRect[] overlayScreenRects = GridAnchorResolverLogic.toScreenRects(
					placement,
					anchor.slotRect().x(),
					anchor.slotRect().y()
			);

			ResolvedPlacement resolved = new ResolvedPlacement(anchor, placement, overlayScreenRects);
			logDebugPlacement(resolved);
			return resolved;
		});
	}

	private static MenuLayout detectMenuLayout(AbstractContainerScreen<?> screen, AbstractContainerMenu menu) {
		if (screen instanceof InventoryScreen || menu instanceof InventoryMenu) {
			return MenuLayout.PLAYER_INVENTORY;
		}

		if (screen instanceof ContainerScreen || menu instanceof ChestMenu) {
			return MenuLayout.STANDARD_CONTAINER;
		}

		if (menu.slots.size() >= GridAnchorResolverLogic.PLAYER_TAIL_SLOT_COUNT
				&& hasStandardPlayerInventoryTail(menu)) {
			return MenuLayout.STANDARD_CONTAINER;
		}

		return null;
	}

	private static boolean hasStandardPlayerInventoryTail(AbstractContainerMenu menu) {
		int size = menu.slots.size();
		int hotbarStart = size - GridAnchorResolverLogic.PLAYER_HOTBAR_SLOT_COUNT;
		int mainStart = hotbarStart - GridAnchorResolverLogic.PLAYER_MAIN_SLOT_COUNT;

		if (mainStart < 0) {
			return false;
		}

		Slot firstMain = menu.slots.get(mainStart);
		Slot firstHotbar = menu.slots.get(hotbarStart);
		return firstHotbar.y - firstMain.y >= DEFAULT_SLOT_PITCH;
	}

	private static int containerSlotCount(AbstractContainerMenu menu, MenuLayout layout) {
		if (layout == MenuLayout.PLAYER_INVENTORY) {
			return 0;
		}

		if (menu instanceof ChestMenu chestMenu) {
			return chestMenu.getRowCount() * GridAnchorResolverLogic.STANDARD_GRID_WIDTH;
		}

		return GridAnchorResolverLogic.standardContainerSlotCount(menu.slots.size());
	}

	private static SlotClassification refineContainerClassification(
			AbstractContainerMenu menu,
			int containerSlotCount,
			SlotClassification classification
	) {
		if (containerSlotCount % GridAnchorResolverLogic.STANDARD_GRID_WIDTH == 0) {
			return classification;
		}

		List<IntSlotPos> containerPositions = new ArrayList<>(containerSlotCount);
		for (int i = 0; i < containerSlotCount; i++) {
			Slot containerSlot = menu.slots.get(i);
			containerPositions.add(new IntSlotPos(containerSlot.x, containerSlot.y));
		}

		GridAnchorResolverLogic.GridDimensions dimensions = GridAnchorResolverLogic.deriveContainerGridDimensions(
				containerPositions
		);

		if (dimensions.width() <= 0 || dimensions.height() <= 0) {
			return SlotClassification.UNSUPPORTED;
		}

		return new SlotClassification(
				true,
				HostGridKind.CONTAINER,
				dimensions.width(),
				dimensions.height(),
				classification.slotIndexInGrid()
		);
	}

	private static int[] derivePitch(
			AbstractContainerMenu menu,
			MenuLayout layout,
			SlotClassification classification,
			int containerSlotCount,
			Slot currentSlot
	) {
		IntSlotPos current = new IntSlotPos(currentSlot.x, currentSlot.y);
		int fallbackW = DEFAULT_SLOT_PITCH;
		int fallbackH = DEFAULT_SLOT_PITCH;

		int horizontalIndex = classification.slotIndexInGrid() + 1;
		if (classification.slotIndexInGrid() % classification.gridWidth() < classification.gridWidth() - 1) {
			int menuIndex = GridAnchorResolverLogic.menuSlotIndexForGridSlot(
					layout,
					classification,
					containerSlotCount,
					horizontalIndex
			);
			Slot neighbor = menu.slots.get(menuIndex);
			fallbackW = GridAnchorResolverLogic.derivePitchW(
					classification,
					current,
					new IntSlotPos(neighbor.x, neighbor.y),
					DEFAULT_SLOT_PITCH
			);
		}

		int verticalIndex = classification.slotIndexInGrid() + classification.gridWidth();
		int maxRowStart = (classification.gridHeight() - 1) * classification.gridWidth();
		if (classification.slotIndexInGrid() < maxRowStart) {
			int menuIndex = GridAnchorResolverLogic.menuSlotIndexForGridSlot(
					layout,
					classification,
					containerSlotCount,
					verticalIndex
			);
			Slot neighbor = menu.slots.get(menuIndex);
			fallbackH = GridAnchorResolverLogic.derivePitchH(
					classification,
					current,
					new IntSlotPos(neighbor.x, neighbor.y),
					DEFAULT_SLOT_PITCH
			);
		}

		if (classification.hostGrid() == HostGridKind.HOTBAR) {
			fallbackH = fallbackW;
		}

		return new int[] { fallbackW, fallbackH };
	}

	private static int screenOriginX(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).better_bundles$getLeftPos();
	}

	private static int screenOriginY(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).better_bundles$getTopPos();
	}

	private static void logDebugAnchor(AbstractContainerScreen<?> screen, Slot slot, ResolvedAnchor anchor) {
		if (!DEBUG_PLACEMENT_RECTS && !LOGGER.isDebugEnabled()) {
			return;
		}

		LOGGER.debug(
				"Grid anchor {} slot#{} -> rect=({},{} {}x{}), grid={}x{} idx={} pitch={}x{} screen={}",
				anchor.hostGrid(),
				slot.index,
				anchor.slotRect().x(),
				anchor.slotRect().y(),
				anchor.slotRect().w(),
				anchor.slotRect().h(),
				anchor.gridWidth(),
				anchor.gridHeight(),
				anchor.slotIndexInGrid(),
				anchor.slotPitchW(),
				anchor.slotPitchH(),
				screen.getClass().getSimpleName()
		);
	}

	private static void logDebugPlacement(ResolvedPlacement resolved) {
		if (!DEBUG_PLACEMENT_RECTS && !LOGGER.isDebugEnabled()) {
			return;
		}

		BundleOverlayPlacement.PlacementResult placement = resolved.placement();
		LOGGER.debug(
				"Overlay placement dir=({},{}), flipped=({},{}), screenRects=[({},{}), ({},{}), ({},{}), ({},{})]",
				placement.dirX(),
				placement.dirY(),
				placement.flippedX(),
				placement.flippedY(),
				resolved.overlayScreenRects()[0].x(),
				resolved.overlayScreenRects()[0].y(),
				resolved.overlayScreenRects()[1].x(),
				resolved.overlayScreenRects()[1].y(),
				resolved.overlayScreenRects()[2].x(),
				resolved.overlayScreenRects()[2].y(),
				resolved.overlayScreenRects()[3].x(),
				resolved.overlayScreenRects()[3].y()
		);
	}
}
