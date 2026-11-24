package br.niw.vulkan.mundo;

import br.niw.vulkan.inicio.Chaves;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

/**
 * A etapa de superfície só escreve no próprio chunk e o resultado depende só da posição; no original ela roda na fila
 * única da geração. Se alguma regra de outro mod não aguentar, volta para a fila e refaz o chunk.
 */
public final class Superficie {
    private static volatile boolean ligada = Chaves.ligada("geracao");

    private Superficie() { }

    public static boolean pode(ChunkStatus s, ChunkGenerator g, List<ChunkAccess> vizinhos) {
        if (!ligada || s != ChunkStatus.SURFACE || g.getClass() != NoiseBasedChunkGenerator.class) return false;
        // mundos antigos com mistura de bordas mexem nos vizinhos: ficam na fila
        for (ChunkAccess c : vizinhos) if (c.isOldNoiseGeneration()) return false;
        return true;
    }

    public static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> fora(
        Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> etapa, Executor fila) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return etapa.get();
            } catch (Throwable t) {
                if (ligada) Chaves.LOG.error("a superfície em paralelo falhou; a geração volta para a fila original", t);
                ligada = false;
                return null;
            }
        }, Util.backgroundExecutor()).thenCompose(f -> f != null ? f : CompletableFuture.supplyAsync(etapa, fila).thenCompose(x -> x));
    }
}
