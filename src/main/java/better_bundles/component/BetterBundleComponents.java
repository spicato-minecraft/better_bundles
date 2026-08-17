package better_bundles.component;

import better_bundles.BetterBundles;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class BetterBundleComponents {

	public static final DataComponentType<BetterBundleContentsComponent> BETTER_BUNDLE_CONTENTS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(BetterBundles.MOD_ID, "bundle_contents"),
		DataComponentType.<BetterBundleContentsComponent>builder()
			.persistent(BetterBundleContentsComponent.CODEC)
			.build()
	);

	private BetterBundleComponents() {
	}

	public static void initialize() {
		BetterBundles.LOGGER.info("Registered Better Bundles data components");
	}
}
