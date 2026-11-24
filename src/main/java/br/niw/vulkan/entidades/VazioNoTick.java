package br.niw.vulkan.entidades;

import net.minecraft.world.entity.Entity;

/** Lembra, por jogador e contexto, em que tick a resposta foi vazia. Só na thread de desenho. */
public final class VazioNoTick {
    /** 0 = consulta sem movimento de pernas, 1 = com. */
    public static int modo;
    private final long[] quando = new long[4 * 2 * 4];

    private static long chave(Entity e) {
        return (long) e.getId() << 32 | e.tickCount & 0xFFFFFFFFL;
    }

    private int lugar(Entity e, int contexto) {
        return (e.getId() & 3) * 8 + modo * 4 + (contexto & 3);
    }

    public boolean vazia(Entity e, int contexto) {
        return quando[lugar(e, contexto)] == chave(e);
    }

    public void anota(Entity e, int contexto) {
        quando[lugar(e, contexto)] = chave(e);
    }
}
