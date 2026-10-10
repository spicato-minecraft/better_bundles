package better_bundles.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Loads every client mixin target on the client thread. A renamed class fails as soon as it is initialized.
 */
public class ClientMixinApplySweepGameTest implements FabricClientGameTest {
	private static final String[] TARGETS = {
			"net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
			"net.minecraft.client.gui.screens.Screen",
			"net.minecraft.world.item.BundleItem"
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		context.computeOnClient(client -> {
			for (String name : TARGETS) {
				try {
					Class.forName(name);
				} catch (ClassNotFoundException e) {
					throw new AssertionError("Mixin target failed to load: " + name, e);
				}
			}
			return null;
		});
	}
}
