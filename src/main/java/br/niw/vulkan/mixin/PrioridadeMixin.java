package br.niw.vulkan.mixin;

import java.util.concurrent.ForkJoinWorkerThread;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** As threads de mundo usam todos os núcleos; um degrau abaixo, o desenho e o driver passam na frente quando precisam. */
@Mixin(Util.class)
public class PrioridadeMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ForkJoinWorkerThread;setName(Ljava/lang/String;)V", remap = false))
    private static void niw$nome(ForkJoinWorkerThread t, String nome) {
        t.setName(nome);
        t.setPriority(Thread.NORM_PRIORITY - 1);
    }
}
