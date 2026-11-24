package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.Lotes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LoteLevelRendererMixin {
    @Shadow @Final private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    private void niw$comeca(PoseStack a, float b, long c, boolean d, Camera e, GameRenderer f, LightTexture g, Matrix4f h, CallbackInfo ci) {
        Lotes.comeca(renderBuffers.bufferSource());
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void niw$acaba(PoseStack a, float b, long c, boolean d, Camera e, GameRenderer f, LightTexture g, Matrix4f h, CallbackInfo ci) {
        Lotes.acaba();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void niw$garante(PoseStack a, float b, long c, boolean d, Camera e, GameRenderer f, LightTexture g, Matrix4f h, CallbackInfo ci) {
        Lotes.acaba();
    }
}
