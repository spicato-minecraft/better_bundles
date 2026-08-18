package better_bundles;

import better_bundles.client.BundleOverlaySpriteLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleOverlaySpriteLayoutTest {

	@Test
	void dlSpriteAnchorMatchesLockedLayout() {
		BundleOverlaySpriteLayout.SpriteSpec spec = BundleOverlaySpriteLayout.spec();

		assertEquals(46, spec.width());
		assertEquals(62, spec.height());
		assertEquals(6, spec.anchorPixelX());
		assertEquals(4, spec.anchorPixelY());
	}

	@Test
	void lockedSpriteSpecUsesDownLeftTexture() {
		BundleOverlaySpriteLayout.SpriteSpec spec = BundleOverlaySpriteLayout.spec();

		assertEquals("dl", spec.textureId());
		assertEquals("textures/gui/bundle_overlay/dl.png", spec.texturePath());
		assertTrue(spec.anchorPixelX() >= 0);
		assertTrue(spec.anchorPixelY() >= 0);
		assertEquals(4, spec.innerSlotPixels().length);
	}
}
