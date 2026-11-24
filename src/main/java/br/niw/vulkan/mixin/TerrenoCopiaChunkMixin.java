package br.niw.vulkan.mixin;

import br.niw.vulkan.terreno.CopiaSobDemanda;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * O original copia as 24 seções de cada uma das 9 colunas, na thread de desenho, a cada remontagem.
 * Aqui só são copiadas as três alturas que a montagem usa; fora delas a leitura vai direto ao chunk.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.RenderChunk")
public class TerrenoCopiaChunkMixin implements CopiaSobDemanda {
    @Shadow @Final private List<PalettedContainer<BlockState>> sections;
    @Shadow @Final private LevelChunk wrapped;
    @Unique private int niw$feitas;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/PalettedContainer;copy()Lnet/minecraft/world/level/chunk/PalettedContainer;"))
    private PalettedContainer<BlockState> niw$adia(PalettedContainer<BlockState> c) {
        return c;
    }

    @Override
    public void niw$copia(int yMin, int yMax) {
        if (sections == null) return;
        int de = Math.max(0, wrapped.getSectionIndex(yMin)), ate = Math.min(sections.size() - 1, wrapped.getSectionIndex(yMax));
        for (int i = de; i <= ate; i++) {
            if (i < 32 && (niw$feitas & 1 << i) != 0) continue;
            PalettedContainer<BlockState> c = sections.get(i);
            if (c != null) sections.set(i, c.copy());
            if (i < 32) niw$feitas |= 1 << i;
        }
    }
}
