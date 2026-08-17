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
	int sourceSlotId
) implements CustomPacketPayload {

	public static final int NO_SOURCE_SLOT = -1;

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
		BundleSlotActionPayload::fromWire
	);

	private static BundleSlotActionPayload fromWire(
		int screenSyncId,
		int containerSlotId,
		int innerSlot,
		int actionOrdinal,
		int carriedRevision,
		int sourceSlotId
	) {
		return new BundleSlotActionPayload(
			screenSyncId,
			containerSlotId,
			innerSlot,
			decodeAction(actionOrdinal),
			carriedRevision,
			sourceSlotId
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
