package br.niw.vulkan.terreno;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.system.MemoryUtil;

/** Vértices de uma camada (sólido, recortado, translúcido...) de todas as seções de uma região, num buffer só. */
final class Faixa {
    static final int PASSO = 32;
    private static int sequencial, quadsSequenciais;

    final Regiao regiao;
    private int vao, vbo, ibo;
    private Alocador verts, indices;
    // montagem da chamada múltipla: preenchida pelo Desenhista quando a lista visível muda
    long contagens, deslocamentos, bases;
    int nDraws, cabem;
    boolean marcada;

    Faixa(Regiao r) {
        regiao = r;
    }

    int pegaVerts(int n) {
        if (verts == null) {
            vbo = GlStateManager._glGenBuffers();
            verts = new Alocador(Math.max(n, 4096));
            GlStateManager._glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, vbo);
            GL15C.nglBufferData(GL31C.GL_COPY_WRITE_BUFFER, (long) verts.capacidade * PASSO, 0L, GL15C.GL_STATIC_DRAW);
        }
        int onde = verts.pega(n);
        if (onde < 0) {
            int nova = Math.max(verts.capacidade + n, verts.capacidade + verts.capacidade / 2);
            vbo = copia(vbo, (long) verts.capacidade * PASSO, (long) nova * PASSO);
            verts.cresce(nova);
            soltaVao();
            onde = verts.pega(n);
        }
        return onde;
    }

    int pegaIndices(int n) {
        if (indices == null) {
            ibo = GlStateManager._glGenBuffers();
            indices = new Alocador(Math.max(n, 4096));
            GlStateManager._glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, ibo);
            GL15C.nglBufferData(GL31C.GL_COPY_WRITE_BUFFER, indices.capacidade * 4L, 0L, GL15C.GL_STATIC_DRAW);
            soltaVao();
        }
        int onde = indices.pega(n);
        if (onde < 0) {
            int nova = Math.max(indices.capacidade + n, indices.capacidade + indices.capacidade / 2);
            ibo = copia(ibo, indices.capacidade * 4L, nova * 4L);
            indices.cresce(nova);
            soltaVao();
            onde = indices.pega(n);
        }
        return onde;
    }

    void envia(boolean deIndices, long onde, long dados, long bytes) {
        GlStateManager._glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, deIndices ? ibo : vbo);
        GL15C.nglBufferSubData(GL31C.GL_COPY_WRITE_BUFFER, onde, bytes, dados);
    }

    void devolve(int v, int nv, int i, int ni) {
        if (nv > 0) verts.devolve(v, nv);
        if (ni > 0) indices.devolve(i, ni);
    }

    boolean vazia() {
        return (verts == null || verts.vazio()) && (indices == null || indices.vazio());
    }

    boolean temIndices() {
        return indices != null;
    }

    void liga() {
        if (vao == 0) {
            vao = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            DefaultVertexFormat.BLOCK.setupBufferState();
            GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ibo != 0 ? ibo : sequencial);
        } else {
            GlStateManager._glBindVertexArray(vao);
        }
    }

    /** Índices 0,1,2,2,3,0... comuns a todas as faixas sem ordem própria. */
    static void garanteSequencial(int quads) {
        if (quads <= quadsSequenciais) return;
        int q = Math.max(Math.max(quads, 32768), quadsSequenciais * 2);
        if (sequencial == 0) sequencial = GlStateManager._glGenBuffers();
        ByteBuffer b = MemoryUtil.memAlloc(q * 24);
        long p = MemoryUtil.memAddress(b);
        for (int i = 0; i < q; i++, p += 24) {
            int v = i * 4;
            MemoryUtil.memPutInt(p, v);
            MemoryUtil.memPutInt(p + 4, v + 1);
            MemoryUtil.memPutInt(p + 8, v + 2);
            MemoryUtil.memPutInt(p + 12, v + 2);
            MemoryUtil.memPutInt(p + 16, v + 3);
            MemoryUtil.memPutInt(p + 20, v);
        }
        GlStateManager._glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, sequencial);
        GL15C.glBufferData(GL31C.GL_COPY_WRITE_BUFFER, b, GL15C.GL_STATIC_DRAW);
        MemoryUtil.memFree(b);
        quadsSequenciais = q;
    }

    void junta(int contagem, long deslocamento, int base) {
        if (nDraws == cabem) {
            cabem = Math.max(64, cabem * 2);
            contagens = MemoryUtil.nmemReallocChecked(contagens, cabem * 4L);
            deslocamentos = MemoryUtil.nmemReallocChecked(deslocamentos, cabem * 8L);
            bases = MemoryUtil.nmemReallocChecked(bases, cabem * 4L);
        }
        MemoryUtil.memPutInt(contagens + nDraws * 4L, contagem);
        MemoryUtil.memPutAddress(deslocamentos + nDraws * 8L, deslocamento);
        MemoryUtil.memPutInt(bases + nDraws * 4L, base);
        nDraws++;
    }

    private static int copia(int velho, long bytes, long novos) {
        int novo = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL31C.GL_COPY_READ_BUFFER, velho);
        GlStateManager._glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, novo);
        GL15C.nglBufferData(GL31C.GL_COPY_WRITE_BUFFER, novos, 0L, GL15C.GL_STATIC_DRAW);
        GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, 0, 0, bytes);
        GlStateManager._glDeleteBuffers(velho);
        return novo;
    }

    private void soltaVao() {
        if (vao != 0) GlStateManager._glDeleteVertexArrays(vao);
        vao = 0;
    }

    void apaga() {
        soltaVao();
        if (vbo != 0) GlStateManager._glDeleteBuffers(vbo);
        if (ibo != 0) GlStateManager._glDeleteBuffers(ibo);
        vbo = ibo = 0;
        verts = indices = null;
        MemoryUtil.nmemFree(contagens);
        MemoryUtil.nmemFree(deslocamentos);
        MemoryUtil.nmemFree(bases);
        contagens = deslocamentos = bases = 0;
        cabem = nDraws = 0;
    }
}
