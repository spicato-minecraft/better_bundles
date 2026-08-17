package better_bundles;

import better_bundles.network.BundleSlotActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class BetterBundlesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry.playC2S().register(BundleSlotActionPayload.TYPE, BundleSlotActionPayload.CODEC);
	}
}
