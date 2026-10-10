package better_bundles.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Loads the common mixin target so a renamed class fails here instead of in a player session.
 */
public class ServerMixinApplySweepGameTest {

	@GameTest(maxTicks = 20)
	public void loadsBundleItemMixinTarget(GameTestHelper context) {
		load("net.minecraft.world.item.BundleItem");
		context.succeed();
	}

	private static void load(String name) {
		try {
			Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new AssertionError("Mixin target failed to load: " + name, e);
		}
	}
}
