package br.niw.vulkan.mixin;

import br.niw.vulkan.texturas.AtlasDireto;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteContents.class)
public class TexturaEnvioMixin {
    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;upload(IIIIIIIZZ)V"))
    private void niw$envia(NativeImage im, int nivel, int x, int y, int px, int py, int w, int h, boolean mip, boolean fecha) {
        if (fecha || !AtlasDireto.guarda(im, nivel, x, y, px, py, w, h)) im.upload(nivel, x, y, px, py, w, h, mip, fecha);
    }
}
