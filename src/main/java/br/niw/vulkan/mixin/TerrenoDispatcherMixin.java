package br.niw.vulkan.mixin;

import br.niw.vulkan.terreno.BufferArena;
import br.niw.vulkan.terreno.DonoDeRegioes;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderDispatcher.class)
public class TerrenoDispatcherMixin implements DonoDeRegioes {
    @Shadow @Final private Queue<Runnable> toUpload;
    @Unique private Object niw$conjunto;

    @Override
    public Object niw$conjunto() {
        if (niw$conjunto == null) niw$conjunto = BufferArena.novoConjunto();
        return niw$conjunto;
    }

    @Inject(method = "uploadChunkLayer", at = @At("HEAD"), cancellable = true)
    private void niw$agenda(BufferBuilder.RenderedBuffer rb, VertexBuffer vb, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (vb instanceof BufferArena a) cir.setReturnValue(a.agenda(rb, toUpload::add));
    }
}
