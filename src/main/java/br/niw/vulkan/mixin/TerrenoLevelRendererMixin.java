package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.Oclusao;
import br.niw.vulkan.inicio.Chaves;
import br.niw.vulkan.terreno.BufferArena;
import br.niw.vulkan.terreno.Desenhista;
import br.niw.vulkan.terreno.DonoDeRegioes;
import br.niw.vulkan.terreno.Visao;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class TerrenoLevelRendererMixin {
    @Shadow @Final private ObjectArrayList<?> renderChunksInFrustum;
    @Shadow @Final private AtomicReference<?> renderChunkStorage;
    @Shadow @Final private Minecraft minecraft;
    @Unique private final Visao niw$visao = new Visao();
    @Unique private final Desenhista niw$desenhista = new Desenhista(niw$visao);
    @Unique private final boolean niw$oclusao = Chaves.ligada("oclusao");
    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    @Shadow private ViewArea viewArea;
    @Shadow private ClientLevel level;
    @Shadow private Frustum capturedFrustum;

    /** Troca só o laço do teste: mesma lista, mesma ordem, mesma conta. */
    @Inject(method = "applyFrustum", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;get()Ljava/lang/Object;", remap = false), cancellable = true)
    private void niw$visao(Frustum f, CallbackInfo ci) {
        niw$visao.aplica(f, ((TerrenoArmazemAcesso) renderChunkStorage.get()).niw$chunks(), renderChunksInFrustum);
        Desenhista.mudouVisao();
        minecraft.getProfiler().pop();
        ci.cancel();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    private void niw$vaiEntidades(CallbackInfo ci) {
        if (!niw$oclusao || viewArea == null || this != (Object) minecraft.levelRenderer || capturedFrustum != null) return;
        TerrenoGradeAcesso g = (TerrenoGradeAcesso) viewArea;
        niw$visao.grade(g.niw$gx(), g.niw$gy(), g.niw$gz(), level.getMinSection());
        Oclusao.visao = niw$visao;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", ordinal = 0))
    private void niw$foiEntidades(CallbackInfo ci) {
        Oclusao.visao = null;
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void niw$garante(CallbackInfo ci) {
        Oclusao.visao = null;
    }

    @Inject(method = "addRecentlyCompiledChunk", at = @At("HEAD"))
    private void niw$mudouChunk(ChunkRenderDispatcher.RenderChunk c, CallbackInfo ci) {
        // quem publica é o montador desta seção, que pode ser o de outra dimensão mostrada por um mod
        BufferArena.publicou(((DonoDeRegioes) chunkRenderDispatcher).niw$conjunto(), c);
        Desenhista.mudou();
    }

    @Inject(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V", shift = At.Shift.AFTER))
    private void niw$desenha(RenderType tipo, PoseStack ps, double x, double y, double z, Matrix4f proj, CallbackInfo ci) {
        niw$desenhista.desenha(tipo, renderChunksInFrustum, chunkRenderDispatcher, x, y, z, RenderSystem.getShader());
    }

    /** O laço original, de uma chamada por seção, fica sem nada para percorrer. */
    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;listIterator(I)Lit/unimi/dsi/fastutil/objects/ObjectListIterator;", remap = false))
    private ObjectListIterator<?> niw$vazio(ObjectArrayList<?> lista, int i) {
        return ObjectLists.emptyList().listIterator();
    }

    /** O laço de blocos-entidade passa só pelas seções que têm algum (as outras não fariam nada). */
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;", remap = false))
    private ObjectListIterator<?> niw$soComBlocos(ObjectArrayList<?> lista) {
        if (lista != renderChunksInFrustum) return lista.iterator();
        return niw$desenhista.comBlocos(lista, chunkRenderDispatcher).iterator();
    }
}
