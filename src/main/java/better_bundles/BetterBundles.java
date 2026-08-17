package better_bundles;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BetterBundles implements ModInitializer {
	public static final String MOD_ID = "better_bundles";

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Better Bundles initialized");
	}
}
