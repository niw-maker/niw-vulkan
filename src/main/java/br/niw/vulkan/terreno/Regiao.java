package br.niw.vulkan.terreno;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

/** Bloco de 8x4x8 seções (128x64x128 blocos) desenhado junto. */
final class Regiao {
    /** Um conjunto por montador de chunks: mods que mostram duas dimensões ao mesmo tempo não se misturam. */
    static final class Conjunto {
        private final Long2ObjectOpenHashMap<Regiao> todas = new Long2ObjectOpenHashMap<>();
        final Tabela tabela = new Tabela();

        Regiao de(int bx, int by, int bz) {
            int x = cantoXZ(bx), y = cantoY(by), z = cantoXZ(bz);
            long k = ((x >> 7) & 0x1FFFFFL) << 42 | ((z >> 7) & 0x1FFFFFL) << 21 | ((y >> 6) & 0x1FFFFFL);
            Regiao r = todas.get(k);
            if (r == null) todas.put(k, r = new Regiao(this, x, y, z, k));
            return r;
        }
    }

    final int x, y, z;
    private final long chave;
    private final Conjunto conjunto;
    private final Map<RenderType, Faixa> faixas = new IdentityHashMap<>();

    private Regiao(Conjunto c, int x, int y, int z, long chave) {
        conjunto = c;
        this.x = x;
        this.y = y;
        this.z = z;
        this.chave = chave;
    }

    static int cantoXZ(int b) { return b & ~127; }
    static int cantoY(int b) { return b & ~63; }

    Faixa faixa(RenderType t) {
        return faixas.computeIfAbsent(t, k -> new Faixa(this));
    }

    void talvezApaga() {
        for (Faixa f : faixas.values()) if (!f.vazia()) return;
        faixas.values().forEach(Faixa::apaga);
        faixas.clear();
        conjunto.todas.remove(chave, this);
    }
}
