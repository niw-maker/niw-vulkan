package br.niw.vulkan.mixin;

import br.niw.vulkan.mundo.Biomas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeManager.class)
public class MundoBiomaMixin {
    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;
    @Shadow @Final private long biomeZoomSeed;

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void niw$rapido(BlockPos p, CallbackInfoReturnable<Holder<Biome>> cir) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        int c = Biomas.canto(biomeZoomSeed, x, y, z);
        cir.setReturnValue(noiseBiomeSource.getNoiseBiome((x - 2 >> 2) + (c >> 2), (y - 2 >> 2) + (c >> 1 & 1), (z - 2 >> 2) + (c & 1)));
    }
}
