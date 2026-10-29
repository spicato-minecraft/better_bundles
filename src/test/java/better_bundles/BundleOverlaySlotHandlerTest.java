package better_bundles;

import better_bundles.inventory.BundleInventoryAccess;
import better_bundles.network.BundleSlotActionPayload;
import better_bundles.network.BundleSlotActionPayload.Action;
import better_bundles.server.BundleOverlaySlotHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BundleOverlaySlotHandlerTest {

	private static final int SYNC_ID = 7;
	private static final int BUNDLE_SLOT = 12;
	private static final int PLAYER_SLOT = 20;

	private AbstractContainerMenu menu;
	private Player player;
	private Inventory playerInventory;
	private SimpleContainer bundleContainer;
	private Slot bundleMenuSlot;
	private Slot playerMenuSlot;
	private ItemStack carriedStack;

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@BeforeEach
	void setUp() {
		menu = mock(AbstractContainerMenu.class);
		player = mock(Player.class);
		bundleContainer = new SimpleContainer(1);
		bundleContainer.setItem(0, new ItemStack(Items.BUNDLE));
		playerInventory = new Inventory(player, new EntityEquipment());
		playerMenuSlot = new Slot(playerInventory, 0, 8, 26);
		bundleMenuSlot = new Slot(bundleContainer, 0, 8, 8);
		carriedStack = ItemStack.EMPTY;

		when(player.getInventory()).thenReturn(playerInventory);
		when(menu.getStateId()).thenReturn(SYNC_ID);
		when(menu.stillValid(player)).thenReturn(true);
		when(menu.isValidSlotIndex(BUNDLE_SLOT)).thenReturn(true);
		when(menu.isValidSlotIndex(PLAYER_SLOT)).thenReturn(true);
		when(menu.getSlot(BUNDLE_SLOT)).thenReturn(bundleMenuSlot);
		when(menu.getSlot(PLAYER_SLOT)).thenReturn(playerMenuSlot);
		NonNullList<Slot> slots = NonNullList.create();
		slots.add(bundleMenuSlot);
		slots.add(playerMenuSlot);
		setMenuSlots(menu, slots);

		when(menu.getCarried()).thenAnswer(invocation -> carriedStack);
		doAnswer(invocation -> {
			carriedStack = invocation.getArgument(0);
			return null;
		}).when(menu).setCarried(any(ItemStack.class));
	}

	private ItemStack bundleStack() {
		return bundleContainer.getItem(0);
	}

	@Test
	void pickup_movesInnerItemToCarried() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(1, new ItemStack(Items.IRON_INGOT, 3))
		);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PICKUP, 1, BundleSlotActionPayload.MOUSE_BUTTON_PRIMARY)
		));
		assertTrue(carriedStack.is(Items.IRON_INGOT));
		assertEquals(3, carriedStack.getCount());
		assertTrue(BundleInventoryAccess.read(bundleStack()).get(1).isEmpty());
		verify(menu).broadcastChanges();
	}

	@Test
	void pickupSecondary_splitsStackInHalf() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.IRON_INGOT, 5))
		);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PICKUP, 0, BundleSlotActionPayload.MOUSE_BUTTON_SECONDARY)
		));
		assertEquals(3, carriedStack.getCount());
		assertEquals(2, BundleInventoryAccess.read(bundleStack()).get(0).getCount());
	}

	@Test
	void placeSecondary_movesOneItemFromCarried() {
		carriedStack = new ItemStack(Items.GOLD_INGOT, 5);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PLACE, 2, BundleSlotActionPayload.MOUSE_BUTTON_SECONDARY)
		));
		assertEquals(4, carriedStack.getCount());
		assertEquals(1, BundleInventoryAccess.read(bundleStack()).get(2).getCount());
	}

	@Test
	void leftClick_placesFullStack() {
		carriedStack = new ItemStack(Items.GOLD_INGOT, 16);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PLACE, 2, BundleSlotActionPayload.containerButton(BundleSlotActionPayload.EVENT_BUTTON_LEFT))
		));
		assertTrue(carriedStack.isEmpty());
		assertEquals(16, BundleInventoryAccess.read(bundleStack()).get(2).getCount());
	}

	@Test
	void leftClick_picksUpFullStack() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.IRON_INGOT, 10))
		);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PICKUP, 0, BundleSlotActionPayload.containerButton(BundleSlotActionPayload.EVENT_BUTTON_LEFT))
		));
		assertEquals(10, carriedStack.getCount());
		assertTrue(BundleInventoryAccess.read(bundleStack()).get(0).isEmpty());
	}

	@Test
	void rightClick_placesOneItem() {
		carriedStack = new ItemStack(Items.GOLD_INGOT, 16);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PLACE, 2, BundleSlotActionPayload.containerButton(BundleSlotActionPayload.EVENT_BUTTON_RIGHT))
		));
		assertEquals(15, carriedStack.getCount());
		assertEquals(1, BundleInventoryAccess.read(bundleStack()).get(2).getCount());
	}

	@Test
	void rightClick_picksUpHalfStack() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.IRON_INGOT, 10))
		);

		assertTrue(BundleOverlaySlotHandler.handle(
				menu,
				player,
				payload(Action.PICKUP, 0, BundleSlotActionPayload.containerButton(BundleSlotActionPayload.EVENT_BUTTON_RIGHT))
		));
		assertEquals(5, carriedStack.getCount());
		assertEquals(5, BundleInventoryAccess.read(bundleStack()).get(0).getCount());
	}

	@Test
	void place_movesCarriedIntoEmptyInnerSlot() {
		carriedStack = new ItemStack(Items.GOLD_INGOT, 5);

		assertTrue(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 2)));
		assertTrue(carriedStack.isEmpty());
		assertEquals(Items.GOLD_INGOT, BundleInventoryAccess.read(bundleStack()).get(2).getItem());
		assertEquals(5, BundleInventoryAccess.read(bundleStack()).get(2).getCount());
	}

	@Test
	void swap_exchangesCarriedAndInnerStacks() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.EMERALD, 2))
		);
		carriedStack = new ItemStack(Items.DIAMOND, 1);

		assertTrue(BundleOverlaySlotHandler.handle(menu, player, payload(Action.SWAP, 0)));
		assertTrue(carriedStack.is(Items.EMERALD));
		assertEquals(2, carriedStack.getCount());
		assertEquals(Items.DIAMOND, BundleInventoryAccess.read(bundleStack()).get(0).getItem());
	}

	@Test
	void merge_combinesMatchingStacks() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.IRON_INGOT, 32))
		);
		carriedStack = new ItemStack(Items.IRON_INGOT, 16);

		assertTrue(BundleOverlaySlotHandler.handle(menu, player, payload(Action.MERGE, 0)));
		assertTrue(carriedStack.isEmpty());
		assertEquals(48, BundleInventoryAccess.read(bundleStack()).get(0).getCount());
	}

	@Test
	void shiftMoveOut_movesInnerStackToPlayerInventory() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(2, new ItemStack(Items.COBBLESTONE, 10))
		);
		playerInventory.setItem(0, ItemStack.EMPTY);

		assertTrue(BundleOverlaySlotHandler.handle(menu, player, payload(Action.SHIFT_MOVE, 2)));
		assertTrue(BundleInventoryAccess.read(bundleStack()).get(2).isEmpty());
		assertTrue(playerInventory.getItem(0).is(Items.COBBLESTONE));
		assertEquals(10, playerInventory.getItem(0).getCount());
	}

	@Test
	void shiftMoveIn_movesPlayerStackIntoFirstEmptyInnerSlot() {
		playerInventory.setItem(0, new ItemStack(Items.OAK_LOG, 8));

		BundleSlotActionPayload shiftIn = new BundleSlotActionPayload(
			SYNC_ID,
			BUNDLE_SLOT,
			0,
			Action.SHIFT_MOVE,
			SYNC_ID,
			PLAYER_SLOT,
			BundleSlotActionPayload.MOUSE_BUTTON_PRIMARY
		);

		assertTrue(BundleOverlaySlotHandler.handle(menu, player, shiftIn));
		assertTrue(playerInventory.getItem(0).isEmpty());
		assertEquals(Items.OAK_LOG, BundleInventoryAccess.read(bundleStack()).get(0).getItem());
		assertEquals(8, BundleInventoryAccess.read(bundleStack()).get(0).getCount());
	}

	@Test
	void rejectsStaleScreenSyncId() {
		when(menu.getStateId()).thenReturn(SYNC_ID + 1);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 0)));
	}

	@Test
	void rejectsStaleCarriedRevision() {
		BundleSlotActionPayload staleRevision = new BundleSlotActionPayload(
			SYNC_ID,
			BUNDLE_SLOT,
			0,
			Action.PLACE,
			SYNC_ID - 1,
			BundleSlotActionPayload.NO_SOURCE_SLOT,
			BundleSlotActionPayload.MOUSE_BUTTON_PRIMARY
		);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, staleRevision));
	}

	@Test
	void rejectsInvalidContainerSlot() {
		when(menu.isValidSlotIndex(BUNDLE_SLOT)).thenReturn(false);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 0)));
	}

	@Test
	void rejectsNonBundleContainerSlot() {
		bundleContainer.setItem(0, new ItemStack(Items.COBBLESTONE));

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 0)));
	}

	@Test
	void rejectsInvalidInnerSlot() {
		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 4)));
		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, -1)));
	}

	@Test
	void rejectsInvalidMenu() {
		when(menu.stillValid(player)).thenReturn(false);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 0)));
	}

	@Test
	void place_rejectsNestedBundle() {
		carriedStack = new ItemStack(Items.BUNDLE);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 0)));
	}

	@Test
	void pickup_rejectsWhenCarriedIsOccupied() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.APPLE))
		);
		carriedStack = new ItemStack(Items.BREAD);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PICKUP, 0)));
	}

	@Test
	void place_rejectsWhenInnerSlotOccupied() {
		BundleInventoryAccess.write(
			bundleStack(),
			BundleInventoryAccess.read(bundleStack()).set(0, new ItemStack(Items.APPLE))
		);
		carriedStack = new ItemStack(Items.BREAD);

		assertFalse(BundleOverlaySlotHandler.handle(menu, player, payload(Action.PLACE, 0)));
	}

	private static BundleSlotActionPayload payload(Action action, int innerSlot) {
		return payload(action, innerSlot, BundleSlotActionPayload.MOUSE_BUTTON_PRIMARY);
	}

	private static BundleSlotActionPayload payload(Action action, int innerSlot, int mouseButton) {
		return new BundleSlotActionPayload(
			SYNC_ID,
			BUNDLE_SLOT,
			innerSlot,
			action,
			SYNC_ID,
			BundleSlotActionPayload.NO_SOURCE_SLOT,
			mouseButton
		);
	}

	private static void setMenuSlots(AbstractContainerMenu menu, NonNullList<Slot> slots) {
		try {
			Field slotsField = AbstractContainerMenu.class.getDeclaredField("slots");
			slotsField.setAccessible(true);
			slotsField.set(menu, slots);
		} catch (ReflectiveOperationException exception) {
			throw new RuntimeException(exception);
		}
	}
}
