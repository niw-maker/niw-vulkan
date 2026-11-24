package br.niw.vulkan.terreno;

import java.util.Arrays;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

/**
 * Resumo contíguo do que cada seção tem para desenhar, atualizado só quando a seção muda. A lista do quadro é montada
 * lendo estes vetores, sem passar pelos objetos espalhados pela memória.
 */
final class Tabela {
    static final int CAMADAS = 8;
    static final short TEM_BLOCOS = 1 << 12;
    /** Marca de seção que usa o VertexBuffer comum (formato de outro mod). */
    static final Faixa AVULSA = new Faixa(null);

    private ChunkRenderDispatcher.RenderChunk[] dono = new ChunkRenderDispatcher.RenderChunk[0];
    short[] mascara = new short[0];
    final Faixa[][] faixa = new Faixa[CAMADAS][0];
    final int[][] contagem = new int[CAMADAS][0], base = new int[CAMADAS][0], desloc = new int[CAMADAS][0];

    synchronized void registra(ChunkRenderDispatcher.RenderChunk c) {
        int i = c.index;
        if (i >= dono.length) {
            int n = Math.max(i + 1, dono.length * 2);
            dono = Arrays.copyOf(dono, n);
            mascara = Arrays.copyOf(mascara, n);
            for (int k = 0; k < CAMADAS; k++) {
                faixa[k] = Arrays.copyOf(faixa[k], n);
                contagem[k] = Arrays.copyOf(contagem[k], n);
                base[k] = Arrays.copyOf(base[k], n);
                desloc[k] = Arrays.copyOf(desloc[k], n);
            }
        }
        dono[i] = c;
        mascara[i] = 0;
        for (int k = 0; k < CAMADAS; k++) faixa[k][i] = null;
    }

    synchronized void slot(ChunkRenderDispatcher.RenderChunk c, int camada, Faixa f, int cont, int b, int d) {
        int i = c.index;
        if (dono[i] != c) return;
        faixa[camada][i] = f;
        contagem[camada][i] = cont;
        base[camada][i] = b;
        desloc[camada][i] = d;
    }

    /** O resultado publicado da seção mudou. Pode vir de qualquer thread. */
    synchronized void publicou(ChunkRenderDispatcher.RenderChunk c) {
        int i = c.index;
        if (i >= dono.length || dono[i] != c) return;
        ChunkRenderDispatcher.CompiledChunk cc = c.getCompiledChunk();
        int m = cc.getRenderableBlockEntities().isEmpty() ? 0 : TEM_BLOCOS;
        for (RenderType t : cc.hasBlocks) {
            int k = t.getChunkLayerId();
            if (k >= 0 && k < CAMADAS) m |= 1 << k;
        }
        mascara[i] = (short) m;
    }
}
