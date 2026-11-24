package br.niw.vulkan.mixin;

import br.niw.vulkan.inicio.DriverVulkan;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class JanelaMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void niw$abriu(CallbackInfo ci) {
        DriverVulkan.abriu();
    }
}
