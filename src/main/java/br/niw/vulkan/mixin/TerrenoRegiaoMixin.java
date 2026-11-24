package br.niw.vulkan.mixin;

import br.niw.vulkan.terreno.CopiaSobDemanda;
import br.niw.vulkan.terreno.RegiaoLembrada;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderRegionCache.class)
public class TerrenoRegiaoMixin {
    @Inject(method = "createRegion", at = @At("RETURN"))
    private void niw$copia(Level w, BlockPos a, BlockPos b, int margem, CallbackInfoReturnable<RenderChunkRegion> cir) {
        RenderChunkRegion r = cir.getReturnValue();
        if (r == null) return;
        // as consultas fora da folga seguem pelo caminho original
        if (b.getX() - a.getX() == 17 && b.getY() - a.getY() == 17 && b.getZ() - a.getZ() == 17) ((RegiaoLembrada) r).niw$lembra(a.getX(), a.getY(), a.getZ());
        for (Object[] linha : ((TerrenoRegiaoAcesso) r).niw$chunks())
            for (Object c : linha) if (c instanceof CopiaSobDemanda s) s.niw$copia(Math.min(a.getY(), b.getY()), Math.max(a.getY(), b.getY()));
    }
}
