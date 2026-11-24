package br.niw.vulkan.mixin;

import br.niw.vulkan.texturas.AtlasDireto;
import br.niw.vulkan.texturas.FilaAnimacao;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TexturaAtlasMixin extends AbstractTexture {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"))
    private void niw$abre(CallbackInfo ci) {
        FilaAnimacao.abre();
    }

    @Inject(method = "cycleAnimationFrames", at = @At("RETURN"))
    private void niw$fecha(CallbackInfo ci) {
        bind();
        FilaAnimacao.fecha();
    }

    @Inject(method = "upload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlas;clearTextureData()V", shift = At.Shift.AFTER))
    private void niw$abreAtlas(SpriteLoader.Preparations p, CallbackInfo ci) {
        AtlasDireto.abre(p.width(), p.height(), p.mipLevel());
    }

    @Inject(method = "upload", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;", ordinal = 0, remap = false))
    private void niw$fechaAtlas(SpriteLoader.Preparations p, CallbackInfo ci) {
        bind();
        AtlasDireto.fecha();
    }
}
