package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.VazioNoTick;
import java.util.function.BiConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.threetag.palladium.client.model.animation.PalladiumAnimation;
import net.threetag.palladium.client.model.animation.PalladiumAnimationRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Separa as consultas com e sem o movimento das pernas: a resposta de uma não serve para a outra. */
@Pseudo
@Mixin(value = PalladiumAnimationRegistry.class, remap = false)
public class PalladiumRegistroMixin {
    @Inject(method = "forEach(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/HumanoidModel;Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$FirstPersonContext;FLjava/util/function/BiConsumer;)V", at = @At("HEAD"), remap = false, require = 0)
    private static void niw$simples(AbstractClientPlayer p, HumanoidModel<?> m, PalladiumAnimation.FirstPersonContext c, float f, BiConsumer<?, ?> b, CallbackInfo ci) {
        VazioNoTick.modo = 0;
    }

    @Inject(method = "forEach(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/HumanoidModel;Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$FirstPersonContext;FLjava/util/function/BiConsumer;FFFFF)V", at = @At("HEAD"), remap = false, require = 0)
    private static void niw$completo(AbstractClientPlayer p, HumanoidModel<?> m, PalladiumAnimation.FirstPersonContext c, float f, BiConsumer<?, ?> b, float g, float h, float i, float j, float k, CallbackInfo ci) {
        VazioNoTick.modo = 1;
    }
}
