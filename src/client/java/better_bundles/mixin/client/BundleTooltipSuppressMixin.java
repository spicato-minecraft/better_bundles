package better_bundles.mixin.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BundleItem.class)
public abstract class BundleTooltipSuppressMixin {

	@Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
	private void better_bundles$suppressVanillaBundleTooltip(
			ItemStack stack,
			CallbackInfoReturnable<Optional<TooltipComponent>> cir
	) {
		cir.setReturnValue(Optional.empty());
	}
}
