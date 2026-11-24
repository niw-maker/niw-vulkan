package br.niw.vulkan.mixin;

import br.niw.vulkan.texturas.FilaAnimacao;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mistura dos quadros de texturas animadas: mesma conta, lendo e gravando direto na memória da imagem. */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$InterpolationData")
public class TexturaInterpolaMixin {
    @Shadow @Final private NativeImage[] activeFrame;
    @Unique private SpriteContents niw$dono;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void niw$guarda(SpriteContents dono, CallbackInfo ci) {
        niw$dono = dono;
    }

    @Inject(method = "uploadInterpolatedFrame", at = @At("HEAD"), cancellable = true)
    private void niw$mistura(int x, int y, SpriteContents.Ticker t, CallbackInfo ci) {
        SpriteContents s = niw$dono;
        SpriteContents.AnimatedTexture a = t.animationInfo;
        List<SpriteContents.FrameInfo> quadros = a.frames;
        SpriteContents.FrameInfo q = quadros.get(t.frame);
        int i = q.index, j = quadros.get((t.frame + 1) % quadros.size()).index;
        if (i == j) return;
        double d = 1.0 - (double) t.subFrame / (double) q.time, e = 1.0 - d;
        for (int k = 0; k < activeFrame.length; k++) {
            int l = s.width >> k, c = s.height >> k;
            if (l < 1 || c < 1) continue;
            NativeImage de = s.byMipLevel[k], para = activeFrame[k];
            int x1 = a.getFrameX(i) * s.width >> k, y1 = a.getFrameY(i) * s.height >> k;
            int x2 = a.getFrameX(j) * s.width >> k, y2 = a.getFrameY(j) * s.height >> k;
            if (de.pixels == 0 || para.pixels == 0 || de.format() != NativeImage.Format.RGBA || para.format() != NativeImage.Format.RGBA
                || Math.max(x1, x2) + l > de.getWidth() || Math.max(y1, y2) + c > de.getHeight() || l > para.getWidth() || c > para.getHeight()) return;
        }
        FilaAnimacao.faz(() -> niw$calcula(s, a, i, j, d, e), () -> s.upload(x, y, 0, 0, activeFrame));
        ci.cancel();
    }

    @Unique
    private void niw$calcula(SpriteContents s, SpriteContents.AnimatedTexture a, int i, int j, double d, double e) {
        for (int k = 0; k < activeFrame.length; k++) {
            int l = s.width >> k, c = s.height >> k;
            if (l < 1 || c < 1) continue;
            NativeImage de = s.byMipLevel[k], para = activeFrame[k];
            long w = de.getWidth() * 4L;
            long p1 = de.pixels + (a.getFrameY(i) * s.height >> k) * w + (a.getFrameX(i) * s.width >> k) * 4L;
            long p2 = de.pixels + (a.getFrameY(j) * s.height >> k) * w + (a.getFrameX(j) * s.width >> k) * 4L;
            long o = para.pixels, wo = para.getWidth() * 4L;
            for (int r = 0; r < c; r++, p1 += w, p2 += w, o += wo) {
                for (long b = 0, fim = l * 4L; b < fim; b += 4) {
                    int m = MemoryUtil.memGetInt(p1 + b), n = MemoryUtil.memGetInt(p2 + b);
                    int hi = (int) (d * (m >> 16 & 255) + e * (n >> 16 & 255)), me = (int) (d * (m >> 8 & 255) + e * (n >> 8 & 255));
                    MemoryUtil.memPutInt(o + b, m & 0xFF000000 | hi << 16 | me << 8 | (int) (d * (m & 255) + e * (n & 255)));
                }
            }
        }
    }
}
