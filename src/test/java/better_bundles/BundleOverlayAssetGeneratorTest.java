package better_bundles;

import better_bundles.client.BundleOverlayPlacement;
import better_bundles.client.BundleOverlaySpriteLayout;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual dev helper to regenerate the placeholder {@code dl.png} overlay sprite.
 *
 * Run:
 * {@code ./gradlew test --tests better_bundles.BundleOverlayAssetGeneratorTest.generatePlaceholderOverlayAsset}
 */
class BundleOverlayAssetGeneratorTest {

	private static final Path OUTPUT = Path.of(
			"src/client/resources/assets/better_bundles/textures/gui/bundle_overlay"
	);

	@Test
	@Disabled("Manual asset generation only — run: ./gradlew test --tests better_bundles.BundleOverlayAssetGeneratorTest.generatePlaceholderOverlayAsset")
	void generatePlaceholderOverlayAsset() throws Exception {
		Files.createDirectories(OUTPUT);
		writeSprite();
	}

	private static void writeSprite() throws Exception {
		BundleOverlaySpriteLayout.SpriteSpec spec = BundleOverlaySpriteLayout.spec();
		BufferedImage image = new BufferedImage(spec.width(), spec.height(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(198, 166, 120, 255));
		graphics.fillRect(0, 0, spec.width(), spec.height());
		graphics.setColor(new Color(61, 40, 20, 255));
		graphics.setStroke(new BasicStroke(1));
		graphics.drawRect(0, 0, spec.width() - 1, spec.height() - 1);

		for (BundleOverlayPlacement.SlotRect slot : spec.innerSlotPixels()) {
			graphics.setColor(new Color(158, 120, 72, 255));
			graphics.fillRect(slot.x(), slot.y(), slot.w(), slot.h());
			graphics.setColor(new Color(61, 40, 20, 255));
			graphics.drawRect(slot.x(), slot.y(), slot.w() - 1, slot.h() - 1);
		}

		graphics.setColor(new Color(158, 120, 72, 255));
		graphics.fillRect(spec.anchorPixelX(), spec.anchorPixelY(), 18, 18);
		graphics.setColor(new Color(61, 40, 20, 255));
		graphics.drawLine(
				spec.anchorPixelX(),
				spec.anchorPixelY(),
				spec.anchorPixelX() + 17,
				spec.anchorPixelY()
		);
		graphics.drawLine(
				spec.anchorPixelX(),
				spec.anchorPixelY(),
				spec.anchorPixelX(),
				spec.anchorPixelY() + 17
		);
		graphics.drawLine(
				spec.anchorPixelX() + 17,
				spec.anchorPixelY(),
				spec.anchorPixelX() + 17,
				spec.anchorPixelY() + 17
		);
		graphics.drawLine(
				spec.anchorPixelX(),
				spec.anchorPixelY() + 17,
				spec.anchorPixelX() + 17,
				spec.anchorPixelY() + 17
		);

		graphics.dispose();
		ImageIO.write(image, "png", OUTPUT.resolve(spec.textureId() + ".png").toFile());
	}
}
