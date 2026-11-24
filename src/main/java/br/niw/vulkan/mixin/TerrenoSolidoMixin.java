package br.niw.vulkan.mixin;

import net.minecraft.util.RandomSource;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Criar um sorteador novo a cada face disputa o mesmo contador entre todas as threads de montagem. */
@Mixin(value = ForgeHooksClient.class, remap = false)
public class TerrenoSolidoMixin {
    @Unique private static final ThreadLocal<RandomSource> niw$SORTE = ThreadLocal.withInitial(RandomSource::create);

    @Redirect(method = "isBlockInSolidLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;", remap = true), remap = false)
    private static RandomSource niw$sorte() {
        return niw$SORTE.get();
    }
}
