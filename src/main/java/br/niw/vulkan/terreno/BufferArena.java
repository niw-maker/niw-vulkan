package br.niw.vulkan.terreno;

import br.niw.vulkan.mixin.TerrenoVertexBufferAcesso;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

/**
 * Buffer de uma camada de uma seção que mora dentro do buffer da região. Quem chama bind()/draw() por fora
 * (outros mods) continua funcionando. Formatos de vértice diferentes do original caem no VertexBuffer comum.
 */
public final class BufferArena extends VertexBuffer {
    private final ChunkRenderDispatcher.RenderChunk dona;
    private final Regiao.Conjunto conjunto;
    final RenderType tipo;
    private final boolean indicesProprios;
    Faixa faixa;
    private int v, nv, i, ni;
    int contagem;
    boolean simples;
    private boolean fechado;

    public BufferArena(ChunkRenderDispatcher.RenderChunk dona, Object conjunto, RenderType tipo) {
        super(VertexBuffer.Usage.STATIC);
        this.dona = dona;
        this.conjunto = (Regiao.Conjunto) conjunto;
        this.tipo = tipo;
        this.indicesProprios = tipo == RenderType.translucent();
        // os três objetos GL que o construtor original criou não são usados no modo normal
        TerrenoVertexBufferAcesso a = (TerrenoVertexBufferAcesso) (Object) this;
        GlStateManager._glDeleteBuffers(a.niw$vbo());
        GlStateManager._glDeleteBuffers(a.niw$ibo());
        GlStateManager._glDeleteVertexArrays(a.niw$vao());
        a.niw$vbo(0);
        a.niw$ibo(0);
        a.niw$vao(0);
    }

    public static Object novoConjunto() {
        return new Regiao.Conjunto();
    }

    public static void registra(Object conjunto, ChunkRenderDispatcher.RenderChunk c) {
        ((Regiao.Conjunto) conjunto).tabela.registra(c);
    }

    public static void publicou(Object conjunto, ChunkRenderDispatcher.RenderChunk c) {
        ((Regiao.Conjunto) conjunto).tabela.publicou(c);
    }

    private static boolean serve(BufferBuilder.DrawState e, boolean indicesProprios) {
        return e.format() == DefaultVertexFormat.BLOCK && e.mode() == VertexFormat.Mode.QUADS && (indicesProprios || e.sequentialIndex());
    }

    /** Thread de trabalho: soma a posição dentro da região e só deixa a cópia para a thread de desenho. */
    public CompletableFuture<Void> agenda(BufferBuilder.RenderedBuffer rb, Executor fila) {
        int ox = dona.getOrigin().getX(), oy = dona.getOrigin().getY(), oz = dona.getOrigin().getZ();
        boolean ok = serve(rb.drawState(), indicesProprios);
        if (ok && !rb.drawState().indexOnly()) soma(rb, ox, oy, oz);
        return CompletableFuture.runAsync(() -> {
            BlockPos o = dona.getOrigin();
            if (fechado || o.getX() != ox || o.getY() != oy || o.getZ() != oz) rb.release();
            else if (ok) poe(rb, ox, oy, oz);
            else comum(rb);
        }, fila);
    }

    private static void soma(BufferBuilder.RenderedBuffer rb, int ox, int oy, int oz) {
        float dx = ox - Regiao.cantoXZ(ox), dy = oy - Regiao.cantoY(oy), dz = oz - Regiao.cantoXZ(oz);
        ByteBuffer b = rb.vertexBuffer();
        long p = MemoryUtil.memAddress(b), fim = p + (long) rb.drawState().vertexCount() * Faixa.PASSO;
        for (; p < fim; p += Faixa.PASSO) {
            MemoryUtil.memPutFloat(p, MemoryUtil.memGetFloat(p) + dx);
            MemoryUtil.memPutFloat(p + 4, MemoryUtil.memGetFloat(p + 4) + dy);
            MemoryUtil.memPutFloat(p + 8, MemoryUtil.memGetFloat(p + 8) + dz);
        }
    }

    private void poe(BufferBuilder.RenderedBuffer rb, int ox, int oy, int oz) {
        try {
            BufferBuilder.DrawState e = rb.drawState();
            if (e.indexOnly()) {
                // uma reordenação atrasada, de uma montagem que já não vale, apontaria para vértices de outra seção
                if (faixa != null && !simples && e.indexCount() == nv / 4 * 6) poeIndices(rb, e);
                anota();
                return;
            }
            solta();
            simples = false;
            faixa = conjunto.de(ox, oy, oz).faixa(tipo);
            nv = e.vertexCount();
            v = faixa.pegaVerts(nv);
            faixa.envia(false, (long) v * Faixa.PASSO, MemoryUtil.memAddress(rb.vertexBuffer()), (long) nv * Faixa.PASSO);
            contagem = e.indexCount();
            if (indicesProprios) poeIndices(rb, e);
            else Faixa.garanteSequencial(nv / 4 + 1);
            anota();
        } finally {
            rb.release();
        }
    }

    private void poeIndices(BufferBuilder.RenderedBuffer rb, BufferBuilder.DrawState e) {
        int n = e.indexCount();
        if (n != ni) {
            if (ni > 0) faixa.devolve(0, 0, i, ni);
            ni = n;
            i = faixa.pegaIndices(n);
        }
        contagem = n;
        long t = MemoryUtil.nmemAllocChecked(n * 4L);
        if (e.sequentialIndex()) {
            for (int q = 0, b = 0; q + 5 < n; q += 6, b += 4) {
                long p = t + q * 4L;
                MemoryUtil.memPutInt(p, b);
                MemoryUtil.memPutInt(p + 4, b + 1);
                MemoryUtil.memPutInt(p + 8, b + 2);
                MemoryUtil.memPutInt(p + 12, b + 2);
                MemoryUtil.memPutInt(p + 16, b + 3);
                MemoryUtil.memPutInt(p + 20, b);
            }
        } else {
            long o = MemoryUtil.memAddress(rb.indexBuffer());
            if (e.indexType() == VertexFormat.IndexType.INT) MemoryUtil.memCopy(o, t, n * 4L);
            else for (int k = 0; k < n; k++) MemoryUtil.memPutInt(t + k * 4L, MemoryUtil.memGetShort(o + k * 2L) & 0xFFFF);
        }
        faixa.envia(true, i * 4L, t, n * 4L);
        MemoryUtil.nmemFree(t);
    }

    /** Formato desconhecido (mod que troca o formato dos blocos): usa o caminho original para esta seção. */
    private void comum(BufferBuilder.RenderedBuffer rb) {
        solta();
        TerrenoVertexBufferAcesso a = (TerrenoVertexBufferAcesso) (Object) this;
        if (a.niw$vao() == 0) {
            a.niw$vbo(GlStateManager._glGenBuffers());
            a.niw$ibo(GlStateManager._glGenBuffers());
            a.niw$vao(GlStateManager._glGenVertexArrays());
        }
        simples = true;
        bind();
        super.upload(rb);
        VertexBuffer.unbind();
        anota();
    }

    private void anota() {
        conjunto.tabela.slot(dona, tipo.getChunkLayerId(), simples ? Tabela.AVULSA : faixa, contagem, v, indicesProprios ? i : 0);
        Desenhista.mudou();
    }

    long deslocamento() {
        return indicesProprios ? i * 4L : 0L;
    }

    int base() {
        return v;
    }

    int secX() { return dona.getOrigin().getX() >> 4; }
    int secY() { return dona.getOrigin().getY() >> 4; }
    int secZ() { return dona.getOrigin().getZ() >> 4; }

    public void solta() {
        if (faixa == null) {
            if (simples) {
                simples = false;
                anota();
            }
            return;
        }
        faixa.devolve(v, nv, i, ni);
        Regiao r = faixa.regiao;
        faixa = null;
        v = nv = i = ni = contagem = 0;
        r.talvezApaga();
        anota();
    }

    @Override
    public void upload(BufferBuilder.RenderedBuffer rb) {
        if (fechado) {
            rb.release();
            return;
        }
        BlockPos o = dona.getOrigin();
        if (serve(rb.drawState(), indicesProprios)) {
            if (!rb.drawState().indexOnly()) soma(rb, o.getX(), o.getY(), o.getZ());
            poe(rb, o.getX(), o.getY(), o.getZ());
        } else {
            comum(rb);
        }
    }

    @Override
    public void bind() {
        if (simples) {
            super.bind();
            return;
        }
        BufferUploader.invalidate();
        if (faixa != null) faixa.liga();
    }

    @Override
    public void draw() {
        if (simples) {
            super.draw();
            return;
        }
        if (faixa == null || contagem == 0) return;
        ShaderInstance s = RenderSystem.getShader();
        Uniform u = s == null ? null : s.CHUNK_OFFSET;
        float x = 0, y = 0, z = 0;
        if (u != null) {
            FloatBuffer f = u.getFloatBuffer();
            x = f.get(0);
            y = f.get(1);
            z = f.get(2);
            BlockPos o = dona.getOrigin();
            u.set(x - (o.getX() - faixa.regiao.x), y - (o.getY() - faixa.regiao.y), z - (o.getZ() - faixa.regiao.z));
            u.upload();
        }
        GL32C.glDrawElementsBaseVertex(GL11C.GL_TRIANGLES, contagem, GL11C.GL_UNSIGNED_INT, deslocamento(), v);
        if (u != null) u.set(x, y, z);
    }

    @Override
    public VertexFormat getFormat() {
        return simples ? super.getFormat() : DefaultVertexFormat.BLOCK;
    }

    @Override
    public void close() {
        if (fechado) return;
        fechado = true;
        solta();
        super.close();
    }
}
