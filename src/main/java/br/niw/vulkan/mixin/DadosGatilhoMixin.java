package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.NbtLembrado;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleCriterionTrigger.class)
public class DadosGatilhoMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void niw$abre(ServerPlayer p, Predicate<?> t, CallbackInfo ci) {
        NbtLembrado.abre(true);
    }

    /** A partir daqui recompensas rodam e os dados podem mudar. */
    @Inject(method = "trigger", at = {@At(value = "INVOKE", target = "Lnet/minecraft/advancements/CriterionTrigger$Listener;run(Lnet/minecraft/server/PlayerAdvancements;)V"), @At("RETURN")})
    private void niw$fecha(ServerPlayer p, Predicate<?> t, CallbackInfo ci) {
        NbtLembrado.abre(false);
    }
}
