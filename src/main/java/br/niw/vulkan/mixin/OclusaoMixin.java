package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.Oclusao;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class OclusaoMixin {
    @Shadow public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T e);

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void niw$escondida(E e, Frustum f, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Oclusao.visao != null && cir.getReturnValueZ() && Oclusao.esconde(e, getRenderer(e))) cir.setReturnValue(false);
    }
}
