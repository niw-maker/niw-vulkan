package br.niw.vulkan.terreno;

import br.niw.vulkan.mixin.TerrenoFrustumAcesso;
import br.niw.vulkan.mixin.TerrenoInfoAcesso;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import org.joml.FrustumIntersection;

/** Posições das seções em vetores contíguos: o teste de visibilidade deixa de passear pela memória atrás de cada objeto. */
public final class Visao {
    private static volatile int versaoPos;
    private Object origem;
    private int tamanho = -1, versao = -1;
    private Object[] infos = new Object[0];
    private int[] px = new int[0], py = new int[0], pz = new int[0], indice = new int[0];
    // resultado do último teste, na ordem da lista visível
    int nVisiveis;
    int[] vIndice = new int[0], vx = new int[0], vy = new int[0], vz = new int[0];

    // seções que o jogo alcançou a partir da câmera, por índice
    private int[] carimbo = new int[0];
    private int vez, gx, gy, gz, secaoMinima;

    public void grade(int x, int y, int z, int minima) {
        gx = x;
        gy = y;
        gz = z;
        secaoMinima = minima;
    }

    /** Se a seção pode ser vista. Na dúvida, sim. */
    public boolean alcanca(int x, int y, int z) {
        int j = y - secaoMinima;
        if (tamanho <= 0 || j < 0 || j >= gy) return true;
        int i = (Math.floorMod(z, gz) * gy + j) * gx + Math.floorMod(x, gx);
        return i < carimbo.length && carimbo[i] == vez;
    }

    /** Uma seção mudou de lugar (o jogador andou). */
    public static void moveu() {
        versaoPos++;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void aplica(Frustum fr, Collection<?> chunks, ObjectArrayList visiveis) {
        if (origem != chunks || tamanho != chunks.size() || versao != versaoPos) refaz(chunks);
        TerrenoFrustumAcesso f = (TerrenoFrustumAcesso) fr;
        FrustumIntersection fi = f.niw$intersecao();
        double cx = f.niw$x(), cy = f.niw$y(), cz = f.niw$z();
        garante(tamanho);
        int n = 0;
        for (int i = 0; i < tamanho; i++) {
            double x = px[i], y = py[i], z = pz[i];
            if (fi.testAab((float) (x - cx), (float) (y - cy), (float) (z - cz), (float) (x + 16 - cx), (float) (y + 16 - cy), (float) (z + 16 - cz))) {
                visiveis.add(infos[i]);
                vIndice[n] = indice[i];
                vx[n] = px[i];
                vy[n] = py[i];
                vz[n++] = pz[i];
            }
        }
        nVisiveis = n;
    }

    /** Se outro mod mexeu na lista visível, relê tudo dela. */
    void confere(List<?> visiveis) {
        if (visiveis.size() == nVisiveis) return;
        garante(visiveis.size());
        nVisiveis = visiveis.size();
        for (int i = 0; i < nVisiveis; i++) {
            ChunkRenderDispatcher.RenderChunk c = ((TerrenoInfoAcesso) visiveis.get(i)).niw$chunk();
            vIndice[i] = c.index;
            vx[i] = c.getOrigin().getX();
            vy[i] = c.getOrigin().getY();
            vz[i] = c.getOrigin().getZ();
        }
    }

    private void garante(int n) {
        if (vIndice.length >= n) return;
        vIndice = new int[n + 1024];
        vx = new int[n + 1024];
        vy = new int[n + 1024];
        vz = new int[n + 1024];
    }

    private void refaz(Collection<?> chunks) {
        versao = versaoPos;
        origem = chunks;
        int n = chunks.size();
        if (infos.length < n) {
            infos = new Object[n + 1024];
            px = new int[n + 1024];
            py = new int[n + 1024];
            pz = new int[n + 1024];
            indice = new int[n + 1024];
        }
        int i = 0;
        vez++;
        for (Object o : chunks) {
            if (i == n) break;
            ChunkRenderDispatcher.RenderChunk c = ((TerrenoInfoAcesso) o).niw$chunk();
            BlockPos p = c.getOrigin();
            infos[i] = o;
            px[i] = p.getX();
            py[i] = p.getY();
            pz[i] = p.getZ();
            indice[i] = c.index;
            if (c.index >= carimbo.length) carimbo = Arrays.copyOf(carimbo, Math.max(c.index + 1, carimbo.length * 2));
            carimbo[c.index] = vez;
            i++;
        }
        for (int k = i; k < tamanho && k < infos.length; k++) infos[k] = null;
        tamanho = i;
    }
}
