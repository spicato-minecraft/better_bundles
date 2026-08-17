package better_bundles.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ModLoadsGameTest {

	@GameTest(maxTicks = 100)
	public void modLoadsInGameTestWorld(GameTestHelper context) {
		context.succeed();
	}
}
