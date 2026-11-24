package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.SemTravaDeLeitura;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SynchedEntityData.class)
public class DadosEntidadeMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/concurrent/locks/ReentrantReadWriteLock", remap = false))
    private ReentrantReadWriteLock niw$semTrava() {
        return new SemTravaDeLeitura();
    }
}
