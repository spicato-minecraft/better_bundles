package better_bundles;

import better_bundles.component.BetterBundleComponents;
import better_bundles.network.BundleSlotActionPayload;
import better_bundles.server.BundleOverlaySlotHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BetterBundles implements ModInitializer {
	public static final String MOD_ID = "better_bundles";

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BetterBundleComponents.initialize();
		registerNetworking();
		LOGGER.info("Better Bundles initialized");
	}

	private static void registerNetworking() {
		PayloadTypeRegistry.serverboundPlay().register(BundleSlotActionPayload.TYPE, BundleSlotActionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(BundleSlotActionPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				if (!BundleOverlaySlotHandler.handle(context.player(), payload)) {
					LOGGER.debug(
						"Ignoring bundle slot action: player={}, screenSyncId={}, slot={}, inner={}, action={}",
						context.player().getGameProfile().name(),
						payload.screenSyncId(),
						payload.containerSlotId(),
						payload.innerSlot(),
						payload.action()
					);
				}
			});
		});
	}
}
