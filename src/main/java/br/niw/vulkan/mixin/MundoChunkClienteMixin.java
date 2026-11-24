package br.niw.vulkan.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O servidor lembra os últimos chunks consultados; o cliente não, e cada leitura de bloco passa por aqui. Só vale na
 * thread do jogo e é esquecido sempre que um chunk chega, sai ou a área de visão muda.
 */
@Mixin(ClientChunkCache.class)
public class MundoChunkClienteMixin {
    @Unique private final Thread niw$dona = Thread.currentThread();
    @Unique private final LevelChunk[] niw$chunks = new LevelChunk[16];
    @Unique private final int[] niw$x = new int[16], niw$z = new int[16];

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("HEAD"), cancellable = true)
    private void niw$lembra(int x, int z, ChunkStatus s, boolean carrega, CallbackInfoReturnable<LevelChunk> cir) {
        int i = x & 3 | (z & 3) << 2;
        if (niw$x[i] != x || niw$z[i] != z || Thread.currentThread() != niw$dona) return;
        LevelChunk c = niw$chunks[i];
        if (c != null) cir.setReturnValue(c);
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("RETURN"))
    private void niw$guarda(int x, int z, ChunkStatus s, boolean carrega, CallbackInfoReturnable<LevelChunk> cir) {
        LevelChunk c = cir.getReturnValue();
        // o chunk vazio de fora da área não está na posição pedida e não é guardado
        if (c == null || c.getPos().x != x || c.getPos().z != z || Thread.currentThread() != niw$dona || ((Object) c).getClass() != LevelChunk.class) return;
        int i = x & 3 | (z & 3) << 2;
        niw$x[i] = x;
        niw$z[i] = z;
        niw$chunks[i] = c;
    }

    @Inject(method = {"drop", "updateViewCenter", "updateViewRadius"}, at = {@At("HEAD"), @At("RETURN")})
    private void niw$esquece(CallbackInfo ci) {
        java.util.Arrays.fill(niw$chunks, null);
    }

    @Inject(method = "replaceWithPacketData", at = {@At("HEAD"), @At("RETURN")})
    private void niw$esqueceNovo(CallbackInfoReturnable<LevelChunk> cir) {
        java.util.Arrays.fill(niw$chunks, null);
    }
}
