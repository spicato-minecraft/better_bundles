package better_bundles.network;

import better_bundles.BetterBundles;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet for server-authoritative edits to a bundle inner slot overlay.
 */
public record BundleSlotActionPayload(
	int screenSyncId,
	int containerSlotId,
	int innerSlot,
	Action action,
	int carriedRevision,
	int sourceSlotId,
	int mouseButton
) implements CustomPacketPayload {

	public static final int NO_SOURCE_SLOT = -1;
	public static final int MOUSE_BUTTON_PRIMARY = 0;
	public static final int MOUSE_BUTTON_SECONDARY = 1;

	/**
	 * Mouse-event button for a left click. Container slots still use {@link #MOUSE_BUTTON_PRIMARY}.
	 * Matches {@code AbstractContainerScreen.getContainerClickButton}.
	 */
	public static final int EVENT_BUTTON_LEFT = 1;

	/**
	 * Mouse-event button for a right click. Container slots still use {@link #MOUSE_BUTTON_SECONDARY}.
	 */
	public static final int EVENT_BUTTON_RIGHT = 3;

	/**
	 * Converts a screen mouse-event button into the container click button used by overlay slot actions.
	 * Left click places or picks up the whole stack; right click places one item or picks up half.
	 */
	public static int containerButton(int eventButton) {
		return switch (eventButton) {
			case EVENT_BUTTON_LEFT -> MOUSE_BUTTON_PRIMARY;
			case EVENT_BUTTON_RIGHT -> MOUSE_BUTTON_SECONDARY;
			default -> eventButton;
		};
	}

	public static final Identifier ID = Identifier.fromNamespaceAndPath(BetterBundles.MOD_ID, "bundle_slot_action");
	public static final CustomPacketPayload.Type<BundleSlotActionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, BundleSlotActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		BundleSlotActionPayload::screenSyncId,
		ByteBufCodecs.VAR_INT,
		BundleSlotActionPayload::containerSlotId,
		ByteBufCodecs.VAR_INT,
		BundleSlotActionPayload::innerSlot,
		ByteBufCodecs.VAR_INT,
		payload -> payload.action().ordinal(),
		ByteBufCodecs.VAR_INT,
		BundleSlotActionPayload::carriedRevision,
		ByteBufCodecs.VAR_INT,
		BundleSlotActionPayload::sourceSlotId,
		ByteBufCodecs.VAR_INT,
		BundleSlotActionPayload::mouseButton,
		BundleSlotActionPayload::fromWire
	);

	private static BundleSlotActionPayload fromWire(
		int screenSyncId,
		int containerSlotId,
		int innerSlot,
		int actionOrdinal,
		int carriedRevision,
		int sourceSlotId,
		int mouseButton
	) {
		return new BundleSlotActionPayload(
			screenSyncId,
			containerSlotId,
			innerSlot,
			decodeAction(actionOrdinal),
			carriedRevision,
			sourceSlotId,
			mouseButton
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public enum Action {
		PLACE,
		PICKUP,
		SWAP,
		/** Shift-click out of inner slot, or shift-click into bundle from {@link #sourceSlotId}. */
		SHIFT_MOVE,
		/** Double-click merge between carried stack and inner slot. */
		MERGE
	}

	private static Action decodeAction(int id) {
		Action[] values = Action.values();
		return id >= 0 && id < values.length ? values[id] : Action.PLACE;
	}
}
