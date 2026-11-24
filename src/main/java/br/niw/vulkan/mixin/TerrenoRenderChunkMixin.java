package br.niw.vulkan.mixin;

import br.niw.vulkan.terreno.BufferArena;
import br.niw.vulkan.terreno.Desenhista;
import br.niw.vulkan.terreno.DonoDeRegioes;
import br.niw.vulkan.terreno.Visao;
import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public class TerrenoRenderChunkMixin {
    @Shadow @Final private Map<RenderType, VertexBuffer> buffers;

    @Unique private Object niw$conjunto;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void niw$troca(ChunkRenderDispatcher fora, int indice, int x, int y, int z, CallbackInfo ci) {
        Object conjunto = ((DonoDeRegioes) fora).niw$conjunto();
        niw$conjunto = conjunto;
        BufferArena.registra(conjunto, (ChunkRenderDispatcher.RenderChunk) (Object) this);
        buffers.replaceAll((t, vb) -> {
            vb.close();
            return new BufferArena((ChunkRenderDispatcher.RenderChunk) (Object) this, conjunto, t);
        });
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void niw$solta(CallbackInfo ci) {
        if (buffers != null) for (VertexBuffer vb : buffers.values()) if (vb instanceof BufferArena a) a.solta();
        if (niw$conjunto != null) BufferArena.publicou(niw$conjunto, (ChunkRenderDispatcher.RenderChunk) (Object) this);
        Desenhista.mudou();
        Desenhista.sujou();
    }

    @Inject(method = "setDirty", at = @At("TAIL"))
    private void niw$sujou(boolean doJogador, CallbackInfo ci) {
        Desenhista.sujou();
    }

    @Inject(method = "setOrigin", at = @At("TAIL"))
    private void niw$moveu(int x, int y, int z, CallbackInfo ci) {
        Visao.moveu();
    }
}
