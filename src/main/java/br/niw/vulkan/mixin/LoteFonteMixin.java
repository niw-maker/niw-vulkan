package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.Lotes;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiBufferSource.BufferSource.class)
public class LoteFonteMixin {
    @Shadow @Final protected Map<RenderType, BufferBuilder> fixedBuffers;

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void niw$pede(RenderType t, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!Lotes.ligado(this)) return;
        BufferBuilder b = Lotes.pede(t, fixedBuffers.containsKey(t));
        if (b != null) cir.setReturnValue(b);
    }

    @Inject(method = "endLastBatch", at = @At("HEAD"))
    private void niw$fimUltimo(CallbackInfo ci) {
        if (Lotes.ligado(this)) Lotes.descarrega();
    }

    @Inject(method = "endBatch()V", at = @At("HEAD"))
    private void niw$fimTudo(CallbackInfo ci) {
        if (Lotes.ligado(this)) Lotes.descarrega();
    }

    @Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"))
    private void niw$fimTipo(RenderType t, CallbackInfo ci) {
        if (Lotes.ligado(this)) Lotes.descarrega(t);
    }
}
