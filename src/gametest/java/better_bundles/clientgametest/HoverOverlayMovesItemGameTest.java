package better_bundles.clientgametest;

import better_bundles.client.BundleHoverController;
import better_bundles.client.BundleOverlayPlacement;
import better_bundles.inventory.BundleInventory;
import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.mixin.client.AbstractContainerScreenAccessor;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Opens the survival inventory, hovers a bundle, and clicks a carried item into the overlay.
 */
public class HoverOverlayMovesItemGameTest implements FabricClientGameTest {
	private static final int BUNDLE_INVENTORY_SLOT = 9;
	private static final int EMERALD_INVENTORY_SLOT = 17;
	private static final int LEFT_MOUSE_BUTTON = 0;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			hoverOverlayMovesItem(context, world);
		}
	}

	private static void hoverOverlayMovesItem(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getConnection().waitForChunksDownload();
		context.getInput().resizeWindow(1280, 720);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.SURVIVAL);
			player.getInventory().clearContent();
			player.getInventory().setItem(BUNDLE_INVENTORY_SLOT, new ItemStack(Items.BUNDLE));
			player.getInventory().setItem(EMERALD_INVENTORY_SLOT, new ItemStack(Items.EMERALD));
		});
		context.waitFor(client -> client.player != null
				&& client.gameMode != null
				&& client.gameMode.getPlayerMode().isSurvival()
				&& client.player.getInventory().getItem(BUNDLE_INVENTORY_SLOT).is(Items.BUNDLE)
				&& client.player.getInventory().getItem(EMERALD_INVENTORY_SLOT).is(Items.EMERALD));

		context.runOnClient(client -> BundleHoverController.getInstance().close());
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTick();

		clickSlot(context, Items.EMERALD);
		waitUntil(context, client -> client.gui.screen() instanceof InventoryScreen screen
				&& screen.getMenu().getCarried().is(Items.EMERALD), "Emerald was not picked up onto the cursor");

		moveCursorToSlot(context, Items.BUNDLE);
		waitUntil(context, HoverOverlayMovesItemGameTest::overlayShowsFourSlots, overlayMissMessage(context));
		context.runOnClient(client -> assertFourOverlaySlots(requireOverlay()));

		moveCursorToOverlaySlot(context, 0);
		context.getInput().pressMouse(LEFT_MOUSE_BUTTON);

		int bundleMenuSlot = context.computeOnClient(client -> {
			if (!(client.gui.screen() instanceof InventoryScreen screen)) {
				throw new AssertionError("Survival inventory closed before the overlay click");
			}
			return findSlotHolding(screen, Items.BUNDLE).index;
		});
		String problem = "Bundle slot 0 did not receive the emerald";
		for (int tick = 0; tick < 100; tick++) {
			problem = world.getServer().computeOnServer(server -> bundleSlotProblem(server, bundleMenuSlot));
			if (problem == null) {
				break;
			}
			context.waitTick();
		}
		if (problem != null) {
			throw new AssertionError(problem);
		}
	}

	private static boolean overlayShowsFourSlots(Minecraft client) {
		var active = BundleHoverController.getInstance().getActive();
		return active.isPresent() && active.get().overlayRects().length == BundleInventory.SLOT_COUNT;
	}

	private static String overlayMissMessage(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			double[] mouse = BundleHoverController.currentMousePosition();
			return "Bundle overlay did not open with four slots. mouse=" + mouse[0] + "," + mouse[1]
					+ " screen=" + (client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName());
		});
	}

	private static void assertFourOverlaySlots(BundleHoverController.ActiveOverlay overlay) {
		BundleOverlayPlacement.SlotRect[] rects = overlay.overlayRects();
		if (rects.length != BundleInventory.SLOT_COUNT) {
			throw new AssertionError("Overlay represented " + rects.length + " slots");
		}
		for (int index = 0; index < rects.length; index++) {
			BundleOverlayPlacement.SlotRect rect = rects[index];
			double x = rect.x() + rect.w() / 2.0;
			double y = rect.y() + rect.h() / 2.0;
			int hit = overlay.hitInnerSlot(x, y).orElse(-1);
			if (hit != index) {
				throw new AssertionError("Overlay slot " + index + " hit-tested as " + hit);
			}
		}
	}

	private static String bundleSlotProblem(net.minecraft.server.MinecraftServer server, int bundleMenuSlot) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		if (!player.containerMenu.isValidSlotIndex(bundleMenuSlot)) {
			return "Server menu has no slot " + bundleMenuSlot;
		}
		ItemStack bundle = player.containerMenu.getSlot(bundleMenuSlot).getItem();
		if (!bundle.is(Items.BUNDLE)) {
			return "Server slot " + bundleMenuSlot + " is " + bundle;
		}
		BundleInventory inventory = BundleInventoryAccess.read(bundle);
		ItemStack placed = inventory.get(0);
		if (!placed.is(Items.EMERALD) || placed.getCount() != 1) {
			return "Bundle slot 0 is " + placed.getCount() + " " + placed.getItem()
					+ "; carried=" + player.containerMenu.getCarried();
		}
		for (int slot = 1; slot < BundleInventory.SLOT_COUNT; slot++) {
			if (!inventory.get(slot).isEmpty()) {
				return "Bundle slot " + slot + " should stay empty but holds " + inventory.get(slot);
			}
		}
		if (!player.containerMenu.getCarried().isEmpty()) {
			return "Cursor should be empty after placing the emerald, but holds " + player.containerMenu.getCarried();
		}
		return null;
	}

	private static void clickSlot(ClientGameTestContext context, Item item) {
		moveCursorToSlot(context, item);
		context.getInput().pressMouse(LEFT_MOUSE_BUTTON);
	}

	private static void moveCursorToSlot(ClientGameTestContext context, Item item) {
		double[] gui = context.computeOnClient(client -> {
			if (!(client.gui.screen() instanceof InventoryScreen screen)) {
				throw new AssertionError("Expected the survival inventory");
			}
			return slotCenter(screen, findSlotHolding(screen, item));
		});
		moveCursorToGui(context, gui[0], gui[1]);
	}

	private static void moveCursorToOverlaySlot(ClientGameTestContext context, int innerSlot) {
		double[] gui = context.computeOnClient(client -> {
			BundleHoverController.ActiveOverlay overlay = requireOverlay();
			BundleOverlayPlacement.SlotRect rect = overlay.overlayRects()[innerSlot];
			return new double[] { rect.x() + rect.w() / 2.0, rect.y() + rect.h() / 2.0 };
		});
		moveCursorToGui(context, gui[0], gui[1]);
	}

	private static void moveCursorToGui(ClientGameTestContext context, double guiX, double guiY) {
		double[] windowPos = context.computeOnClient(client -> toWindowCoordinates(client, guiX, guiY));
		context.getInput().setCursorPos(windowPos[0], windowPos[1]);
	}

	private static double[] toWindowCoordinates(Minecraft client, double guiX, double guiY) {
		Window window = client.getWindow();
		int scaledWidth = window.getGuiScaledWidth();
		int scaledHeight = window.getGuiScaledHeight();
		if (scaledWidth <= 0 || scaledHeight <= 0) {
			throw new AssertionError("GUI scale is not ready");
		}
		return new double[] {
				guiX * window.getScreenWidth() / scaledWidth,
				guiY * window.getScreenHeight() / scaledHeight
		};
	}

	private static double[] slotCenter(InventoryScreen screen, Slot slot) {
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
		return new double[] {
				accessor.better_bundles$getLeftPos() + slot.x + 8.0,
				accessor.better_bundles$getTopPos() + slot.y + 8.0
		};
	}

	private static Slot findSlotHolding(InventoryScreen screen, Item item) {
		for (Slot slot : screen.getMenu().slots) {
			if (slot.getItem().is(item)) {
				return slot;
			}
		}
		throw new AssertionError("Survival inventory has no " + item);
	}

	private static BundleHoverController.ActiveOverlay requireOverlay() {
		return BundleHoverController.getInstance().getActive()
				.orElseThrow(() -> new AssertionError("Bundle overlay is not open"));
	}

	private static void waitUntil(ClientGameTestContext context, java.util.function.Predicate<Minecraft> predicate, String message) {
		for (int tick = 0; tick < 40; tick++) {
			if (context.computeOnClient(predicate::test)) {
				return;
			}
			context.waitTick();
		}
		throw new AssertionError(message);
	}
}
