package br.niw.vulkan.mixin;

import br.niw.vulkan.mundo.Superficie;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkStatus.class)
public class GeracaoSuperficieMixin {
    @Redirect(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkStatus$GenerationTask;doWork(Lnet/minecraft/world/level/chunk/ChunkStatus;Ljava/util/concurrent/Executor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/server/level/ThreadedLevelLightEngine;Ljava/util/function/Function;Ljava/util/List;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> niw$etapa(ChunkStatus.GenerationTask t, ChunkStatus s, Executor fila,
        ServerLevel w, ChunkGenerator g, StructureTemplateManager m, ThreadedLevelLightEngine l,
        Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> f, List<ChunkAccess> vizinhos, ChunkAccess c) {
        if (!Superficie.pode(s, g, vizinhos)) return t.doWork(s, fila, w, g, m, l, f, vizinhos, c);
        return Superficie.fora(() -> t.doWork(s, fila, w, g, m, l, f, vizinhos, c), fila);
    }
}
