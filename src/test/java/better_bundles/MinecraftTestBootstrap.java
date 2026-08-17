package better_bundles;

import better_bundles.component.BetterBundleComponents;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Initializes Minecraft registries for Fabric Loader JUnit unit tests.
 */
public final class MinecraftTestBootstrap {

	private static boolean initialized;

	private MinecraftTestBootstrap() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BetterBundleComponents.initialize();
		initialized = true;
	}
}
