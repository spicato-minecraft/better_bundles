package better_bundles.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class BundleStorageGameTest {

	@GameTest(maxTicks = 100)
	public void bundleStorageSmokeTest(GameTestHelper context) {
		context.succeed();
	}
}
