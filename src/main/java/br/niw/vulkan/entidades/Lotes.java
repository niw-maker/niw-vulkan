package br.niw.vulkan.entidades;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Durante o desenho de entidades e blocos-entidade do mundo, junta o que usa o mesmo tipo de desenho.
 * No original, cada troca de tipo (vaca, ovelha, vaca...) vira um envio e uma chamada de vídeo separados.
 */
public final class Lotes {
    private static final ArrayDeque<BufferBuilder> LIVRES = new ArrayDeque<>();
    private static final Map<RenderType, BufferBuilder> PENDENTES = new IdentityHashMap<>();
    private static final List<RenderType> OPACOS = new ArrayList<>(), TRANSLUCIDOS = new ArrayList<>();
    private static final Matrix4f VISAO = new Matrix4f(), PROJECAO = new Matrix4f();
    private static MultiBufferSource.BufferSource fonte;
    private static boolean descarregando;

    private Lotes() { }

    public static void comeca(MultiBufferSource.BufferSource s) {
        descarrega();
        fonte = s;
        VISAO.set(RenderSystem.getModelViewMatrix());
        PROJECAO.set(RenderSystem.getProjectionMatrix());
    }

    public static void acaba() {
        descarrega();
        fonte = null;
    }

    public static boolean ligado(Object s) {
        return s == fonte && !descarregando;
    }

    /** Devolve o buffer do lote, ou null para seguir o caminho original. */
    public static BufferBuilder pede(RenderType t, boolean fixo) {
        if (fixo) return null;
        byte b = Tipos.de(t);
        if (b >= Tipos.NAO) {
            if (b == Tipos.NAO_E_MISTURA) descarrega();
            return null;
        }
        if (!estadoComum()) {
            descarrega();
            return null;
        }
        BufferBuilder bb = PENDENTES.get(t);
        if (bb == null) {
            if (PENDENTES.size() >= 96) descarrega();
            bb = LIVRES.poll();
            if (bb == null) bb = new BufferBuilder(16384);
            bb.begin(t.mode(), t.format());
            PENDENTES.put(t, bb);
            (b == Tipos.OPACO ? OPACOS : TRANSLUCIDOS).add(t);
        }
        return bb;
    }

    private static boolean estadoComum() {
        float[] c = RenderSystem.getShaderColor();
        return c[0] == 1 && c[1] == 1 && c[2] == 1 && c[3] == 1 && VISAO.equals(RenderSystem.getModelViewMatrix())
            && PROJECAO.equals(RenderSystem.getProjectionMatrix());
    }

    /** Alguém pediu o fim deste tipo: sai agora, como no original. */
    public static void descarrega(RenderType t) {
        if (PENDENTES.isEmpty() || !PENDENTES.containsKey(t)) return;
        if (TRANSLUCIDOS.contains(t)) {
            descarrega();
        } else {
            OPACOS.remove(t);
            descarregando = true;
            try {
                solta(t);
            } finally {
                descarregando = false;
            }
        }
    }

    public static void descarrega() {
        if (PENDENTES.isEmpty() || descarregando) return;
        descarregando = true;
        try {
            for (int i = 0; i < OPACOS.size(); i++) solta(OPACOS.get(i));
            for (int i = 0; i < TRANSLUCIDOS.size(); i++) solta(TRANSLUCIDOS.get(i));
        } finally {
            OPACOS.clear();
            TRANSLUCIDOS.clear();
            PENDENTES.clear();
            descarregando = false;
        }
    }

    private static void solta(RenderType t) {
        BufferBuilder bb = PENDENTES.remove(t);
        if (bb == null) return;
        // Se um mod mexeu na matriz global e pediu o fim, o que é nosso sai com a matriz de quando foi montado.
        Matrix4f mv = RenderSystem.getModelViewMatrix();
        boolean outra = !VISAO.equals(mv);
        Matrix4f guarda = outra ? new Matrix4f(mv) : null;
        if (outra) mv.set(VISAO);
        t.end(bb, RenderSystem.getVertexSorting());
        if (outra) mv.set(guarda);
        LIVRES.push(bb);
    }
}
