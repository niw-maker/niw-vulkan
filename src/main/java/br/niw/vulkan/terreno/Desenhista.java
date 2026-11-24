package br.niw.vulkan.terreno;

import br.niw.vulkan.mixin.TerrenoInfoAcesso;
import com.mojang.blaze3d.shaders.Uniform;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;

/** Desenha uma camada inteira com uma chamada múltipla por região, no lugar de uma chamada por seção. */
public final class Desenhista {
    private static final AtomicInteger VERSAO = new AtomicInteger(), SUJOS = new AtomicInteger(), VISAO = new AtomicInteger();

    private final Visao visao;
    private int versao = -1, camX, camY, camZ;
    private Object origem;
    private final List<List<Faixa>> ordem = new ArrayList<>();
    private final List<List<BufferArena>> avulsos = new ArrayList<>();
    /** Posições, na lista visível, das seções com camada translúcida. */
    private int[] translucidas = new int[64];
    private int nTranslucidas, camadaTranslucida = -1;
    private long[] chaves = new long[64];
    private final ObjectArrayList<Object> comBlocos = new ObjectArrayList<>();

    public Desenhista(Visao v) {
        visao = v;
        for (int i = 0; i < Tabela.CAMADAS; i++) {
            ordem.add(new ArrayList<>());
            avulsos.add(new ArrayList<>());
        }
    }

    /** Algo mudou (lista visível, conteúdo ou posição): as listas são refeitas no próximo desenho. */
    public static void mudou() {
        VERSAO.incrementAndGet();
    }

    /** Alguma seção ficou marcada para remontar. */
    public static void sujou() {
        SUJOS.incrementAndGet();
    }

    public static int versaoSujos() {
        return SUJOS.get();
    }

    public static void mudouVisao() {
        VISAO.incrementAndGet();
        mudou();
    }

    public static int versaoVisao() {
        return VISAO.get();
    }

    public void desenha(RenderType tipo, List<?> visiveis, Object dono, double cx, double cy, double cz, ShaderInstance sh) {
        int camada = tipo.getChunkLayerId();
        if (camada < 0 || camada >= Tabela.CAMADAS) return;
        Tabela t = ((Regiao.Conjunto) ((DonoDeRegioes) dono).niw$conjunto()).tabela;
        atualiza(visiveis, t);
        if (tipo == RenderType.translucent()) {
            int sx = (int) Math.floor(cx) >> 4, sy = (int) Math.floor(cy) >> 4, sz = (int) Math.floor(cz) >> 4;
            if (camadaTranslucida != camada || camX != sx || camY != sy || camZ != sz) {
                camadaTranslucida = camada;
                camX = sx;
                camY = sy;
                camZ = sz;
                ordena(t, camada);
            }
        }
        Uniform u = sh.CHUNK_OFFSET;
        List<Faixa> l = ordem.get(camada);
        for (int k = 0, n = l.size(); k < n; k++) {
            Faixa f = l.get(k);
            if (u != null) {
                u.set((float) (f.regiao.x - cx), (float) (f.regiao.y - cy), (float) (f.regiao.z - cz));
                u.upload();
            }
            f.liga();
            GL32C.nglMultiDrawElementsBaseVertex(GL11C.GL_TRIANGLES, f.contagens, GL11C.GL_UNSIGNED_INT, f.deslocamentos, f.nDraws, f.bases);
        }
        for (BufferArena a : avulsos.get(camada)) {
            if (u != null) {
                u.set((float) ((a.secX() << 4) - cx), (float) ((a.secY() << 4) - cy), (float) ((a.secZ() << 4) - cz));
                u.upload();
            }
            a.bind();
            a.draw();
        }
    }

    private void atualiza(List<?> visiveis, Tabela t) {
        int v = VERSAO.get();
        if (versao == v && origem == visiveis && visao.nVisiveis == visiveis.size()) return;
        versao = v;
        origem = visiveis;
        visao.confere(visiveis);
        monta(visiveis, t);
    }

    /** Seções visíveis que têm blocos-entidade, na ordem da lista. */
    public ObjectArrayList<Object> comBlocos(List<?> visiveis, Object dono) {
        atualiza(visiveis, ((Regiao.Conjunto) ((DonoDeRegioes) dono).niw$conjunto()).tabela);
        return comBlocos;
    }

    private void monta(List<?> visiveis, Tabela t) {
        for (int i = 0; i < Tabela.CAMADAS; i++) {
            ordem.get(i).clear();
            avulsos.get(i).clear();
        }
        comBlocos.clear();
        nTranslucidas = 0;
        camadaTranslucida = -1;
        int trans = RenderType.translucent().getChunkLayerId();
        short[] mascara = t.mascara;
        int n = visao.nVisiveis;
        for (int k = 0; k < n; k++) {
            int i = visao.vIndice[k];
            if (i >= mascara.length) continue;
            int m = mascara[i];
            if (m == 0) continue;
            if ((m & Tabela.TEM_BLOCOS) != 0) {
                comBlocos.add(visiveis.get(k));
                m &= ~Tabela.TEM_BLOCOS;
            }
            while (m != 0) {
                int c = Integer.numberOfTrailingZeros(m);
                m &= m - 1;
                Faixa f = t.faixa[c][i];
                if (f == null) continue;
                if (f == Tabela.AVULSA) {
                    avulsa(c, visiveis.get(k));
                } else if (c == trans) {
                    if (nTranslucidas == translucidas.length) {
                        translucidas = Arrays.copyOf(translucidas, nTranslucidas * 2);
                        chaves = new long[translucidas.length];
                    }
                    translucidas[nTranslucidas++] = k;
                } else {
                    junta(ordem.get(c), f, t, c, i);
                }
            }
        }
        for (List<Faixa> l : ordem) for (int k = 0; k < l.size(); k++) l.get(k).marcada = false;
    }

    private void avulsa(int camada, Object info) {
        ChunkRenderDispatcher.RenderChunk c = ((TerrenoInfoAcesso) info).niw$chunk();
        for (RenderType t : RenderType.chunkBufferLayers())
            if (t.getChunkLayerId() == camada && c.getBuffer(t) instanceof BufferArena a) avulsos.get(camada).add(a);
    }

    /**
     * De trás para a frente: numa grade, a soma das distâncias por eixo é uma ordem de pintura válida tanto entre regiões
     * quanto entre as seções de uma região.
     */
    private void ordena(Tabela t, int camada) {
        List<Faixa> l = ordem.get(camada);
        l.clear();
        int rx = camX >> 3, ry = camY >> 2, rz = camZ >> 3;
        for (int q = 0; q < nTranslucidas; q++) {
            int k = translucidas[q];
            int x = visao.vx[k] >> 4, y = visao.vy[k] >> 4, z = visao.vz[k] >> 4;
            long dr = Math.abs((x >> 3) - rx) + Math.abs((y >> 2) - ry) + Math.abs((z >> 3) - rz);
            long id = ((x >> 3) & 0xFF) << 16 | ((y >> 2) & 0xFF) << 8 | ((z >> 3) & 0xFF);
            long ds = Math.abs(x - camX) + Math.abs(y - camY) + Math.abs(z - camZ);
            chaves[q] = (0xFFL - Math.min(dr, 0xFF)) << 55 | id << 31 | (0x7FFL - Math.min(ds, 0x7FF)) << 20 | k & 0xFFFFF;
        }
        Arrays.sort(chaves, 0, nTranslucidas);
        for (int q = 0; q < nTranslucidas; q++) {
            int i = visao.vIndice[(int) (chaves[q] & 0xFFFFF)];
            Faixa f = t.faixa[camada][i];
            if (f != null && f != Tabela.AVULSA) junta(l, f, t, camada, i);
        }
        for (int k = 0; k < l.size(); k++) l.get(k).marcada = false;
    }

    private static void junta(List<Faixa> l, Faixa f, Tabela t, int c, int i) {
        if (t.contagem[c][i] == 0) return;
        if (!f.marcada) {
            f.marcada = true;
            f.nDraws = 0;
            l.add(f);
        }
        f.junta(t.contagem[c][i], t.desloc[c][i] * 4L, t.base[c][i]);
    }
}
