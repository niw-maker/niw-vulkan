package br.niw.vulkan.entidades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Dados da entidade já convertidos, válidos só enquanto um mesmo gatilho testa as suas condições (nada muda no meio). */
public final class NbtLembrado {
    private static final ThreadLocal<NbtLembrado> MEU = ThreadLocal.withInitial(NbtLembrado::new);
    private boolean aberto;
    private Entity entidade;
    private CompoundTag nbt;

    private NbtLembrado() { }

    public static void abre(boolean sim) {
        NbtLembrado m = MEU.get();
        m.aberto = sim;
        m.entidade = null;
        m.nbt = null;
    }

    public static CompoundTag de(Entity e) {
        NbtLembrado m = MEU.get();
        return m.aberto && m.entidade == e ? m.nbt : null;
    }

    public static void guarda(Entity e, CompoundTag t) {
        NbtLembrado m = MEU.get();
        if (!m.aberto) return;
        m.entidade = e;
        m.nbt = t;
    }
}
