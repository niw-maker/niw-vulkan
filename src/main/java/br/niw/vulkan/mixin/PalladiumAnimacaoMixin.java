package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.VazioNoTick;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.threetag.palladium.client.model.animation.PalladiumAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cada animação em JavaScript é perguntada várias vezes por quadro e quase sempre não tem nada a fazer. O que ela
 * consulta (poder ligado, temporizador) só muda por tick: se respondeu vazio, fica vazia até o próximo tick.
 */
@Pseudo
@Mixin(targets = "net.threetag.palladium.compat.kubejs.RegisterAnimationsEventJS$AnimationImpl", remap = false)
public class PalladiumAnimacaoMixin {
    @Unique private final VazioNoTick niw$vazio = new VazioNoTick();

    @Inject(method = "animate", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void niw$pula(PalladiumAnimation.Builder b, AbstractClientPlayer p, HumanoidModel<?> m, PalladiumAnimation.FirstPersonContext c, float parcial, CallbackInfo ci) {
        if (niw$vazio.vazia(p, c.ordinal())) ci.cancel();
    }

    @Inject(method = "animate", at = @At("RETURN"), remap = false, require = 0)
    private void niw$anota(PalladiumAnimation.Builder b, AbstractClientPlayer p, HumanoidModel<?> m, PalladiumAnimation.FirstPersonContext c, float parcial, CallbackInfo ci) {
        // bem no começo do tick um temporizador que acabou de ligar ainda vale zero: não conta
        if (parcial > 0.05f && b.getAnimationData().isEmpty()) niw$vazio.anota(p, c.ordinal());
    }
}
