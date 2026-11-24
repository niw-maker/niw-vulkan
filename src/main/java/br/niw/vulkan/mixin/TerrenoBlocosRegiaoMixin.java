package br.niw.vulkan.mixin;

import br.niw.vulkan.terreno.RegiaoLembrada;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cada bloco da seção é consultado dezenas de vezes na montagem; a segunda vez em diante sai de um vetor. */
@Mixin(RenderChunkRegion.class)
public class TerrenoBlocosRegiaoMixin implements RegiaoLembrada {
    @Shadow @Final private int centerX;
    @Shadow @Final private int centerZ;
    @Shadow @Final protected RenderChunk[][] chunks;
    @Unique private int niw$x, niw$y, niw$z;
    @Unique private BlockState[] niw$vistos;

    @Override
    public void niw$lembra(int x, int y, int z) {
        niw$x = x - 1;
        niw$y = y - 1;
        niw$z = z - 1;
        niw$vistos = new BlockState[20 * 20 * 20];
    }

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void niw$bloco(BlockPos p, CallbackInfoReturnable<BlockState> cir) {
        BlockState s = niw$acha(p);
        if (s != null) cir.setReturnValue(s);
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void niw$fluido(BlockPos p, CallbackInfoReturnable<FluidState> cir) {
        BlockState s = niw$acha(p);
        if (s != null) cir.setReturnValue(s.getFluidState());
    }

    @Unique
    private BlockState niw$acha(BlockPos p) {
        BlockState[] v = niw$vistos;
        if (v == null) return null;
        int x = p.getX() - niw$x, y = p.getY() - niw$y, z = p.getZ() - niw$z;
        if ((x | y | z) < 0 || x >= 20 || y >= 20 || z >= 20) return null;
        int i = (y * 20 + z) * 20 + x;
        BlockState s = v[i];
        if (s == null) {
            s = chunks[SectionPos.blockToSectionCoord(p.getX()) - centerX][SectionPos.blockToSectionCoord(p.getZ()) - centerZ].getBlockState(p);
            v[i] = s;
        }
        return s;
    }
}
