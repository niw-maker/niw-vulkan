package br.niw.vulkan.texturas;

import br.niw.vulkan.Nucleos;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryUtil;

/** Monta o atlas na memória, em faixas e com vários núcleos, e envia poucas vezes em vez de uma por textura e nível. */
public final class AtlasDireto {
    private record Pedaco(int nivel, int x, int y, int w, int h, long de, long passo) { }

    private static final List<Pedaco> PEDACOS = new ArrayList<>();
    private static boolean aberto;
    private static int largura, altura, niveis;
    private static long vistos;

    private AtlasDireto() { }

    public static void abre(int w, int h, int mips) {
        PEDACOS.clear();
        largura = w;
        altura = h;
        niveis = mips;
        vistos = 0;
        aberto = true;
    }

    /** Guarda para depois; false = quem chamou envia agora, pelo caminho original. */
    public static boolean guarda(NativeImage im, int nivel, int x, int y, int px, int py, int w, int h) {
        if (!aberto || nivel > niveis || im.pixels == 0 || im.format() != NativeImage.Format.RGBA || w <= 0 || h <= 0 || x < 0 || y < 0) return false;
        if (x + w > largura >> nivel || y + h > altura >> nivel || px < 0 || py < 0 || px + w > im.getWidth() || py + h > im.getHeight()) return false;
        // o primeiro de cada nível vai pelo caminho original, que deixa os filtros da textura como o jogo quer
        boolean primeiro = (vistos & 1L << nivel) == 0;
        vistos |= 1L << nivel;
        long passo = im.getWidth() * 4L;
        PEDACOS.add(new Pedaco(nivel, x, y, w, h, im.pixels + py * passo + px * 4L, passo));
        return !primeiro;
    }

    public static void fecha() {
        aberto = false;
        if (PEDACOS.isEmpty()) return;
        try {
            for (int n = 0; n <= niveis; n++) nivel(n);
        } finally {
            PEDACOS.clear();
        }
    }

    private static void nivel(int n) {
        int w = largura >> n, h = altura >> n;
        List<Pedaco> meus = new ArrayList<>();
        for (Pedaco p : PEDACOS) if (p.nivel == n) meus.add(p);
        if (meus.isEmpty() || w <= 0 || h <= 0) return;
        int linhas = Math.max(16, Math.min(h, (24 << 20) / (w * 4)));
        long buf = MemoryUtil.nmemAllocChecked((long) w * linhas * 4);
        try {
            for (int y0 = 0; y0 < h; y0 += linhas) {
                int y1 = Math.min(h, y0 + linhas), ini = y0;
                List<Pedaco> aqui = new ArrayList<>();
                for (Pedaco p : meus) if (p.y < y1 && p.y + p.h > y0) aqui.add(p);
                if (aqui.isEmpty()) continue;
                MemoryUtil.memSet(buf, 0, (long) w * (y1 - y0) * 4);
                List<Runnable> tarefas = new ArrayList<>();
                int grupo = Math.max(64, aqui.size() / 32);
                for (int g = 0; g < aqui.size(); g += grupo) {
                    List<Pedaco> parte = aqui.subList(g, Math.min(aqui.size(), g + grupo));
                    tarefas.add(() -> {
                        for (Pedaco p : parte)
                            for (int y = Math.max(p.y, ini), f = Math.min(p.y + p.h, y1); y < f; y++)
                                MemoryUtil.memCopy(p.de + (y - p.y) * p.passo, buf + ((long) (y - ini) * w + p.x) * 4, p.w * 4L);
                    });
                }
                Nucleos.todas(tarefas);
                GlStateManager._pixelStore(GL11C.GL_UNPACK_ROW_LENGTH, 0);
                GlStateManager._pixelStore(GL11C.GL_UNPACK_SKIP_PIXELS, 0);
                GlStateManager._pixelStore(GL11C.GL_UNPACK_SKIP_ROWS, 0);
                GlStateManager._pixelStore(GL11C.GL_UNPACK_ALIGNMENT, 4);
                GlStateManager._texSubImage2D(GL11C.GL_TEXTURE_2D, n, 0, y0, w, y1 - y0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, buf);
            }
        } finally {
            MemoryUtil.nmemFree(buf);
        }
    }
}
